package com.example.myapplication.util

import android.content.Context
import android.net.Uri
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * Parsed row from an Excel file.
 * Columns: A=frontText, B=backText, C=example, D=IPA
 */
data class ExcelCardRow(
    val frontText: String,
    val backText: String,
    val exampleText: String?,
    val pronunciationIpa: String?
)

/**
 * Result of an Excel import operation.
 */
data class ExcelImportResult(
    val cards: List<ExcelCardRow>,
    val skippedRows: Int,
    val totalRows: Int
)

/**
 * Lightweight XLSX parser using only built-in Android APIs (ZipInputStream + XmlPullParser).
 * No external library needed — .xlsx is a ZIP containing XML files.
 *
 * Expected format:
 *   Column A: Front text (question / vocabulary)
 *   Column B: Back text (answer / meaning)
 *   Column C: Example (optional)
 *   Column D: IPA pronunciation (optional)
 *
 * Rows where both A and B are empty are skipped.
 */
object ExcelImportHelper {

    fun parseExcelFile(context: Context, uri: Uri): ExcelImportResult {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Không thể mở file")

        return try {
            parseXlsx(inputStream)
        } catch (e: Exception) {
            throw IllegalStateException("Lỗi đọc file Excel: ${e.message}", e)
        } finally {
            inputStream.close()
        }
    }

    private fun parseXlsx(inputStream: InputStream): ExcelImportResult {
        // Step 1: Read shared strings table (strings are stored separately in xlsx)
        val sharedStrings = mutableListOf<String>()
        // Step 2: Read sheet data
        var sheetXml: ByteArray? = null

        // First pass: extract shared strings and sheet1
        val zipBytes = inputStream.readBytes()

        // Parse shared strings
        ZipInputStream(zipBytes.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                when {
                    entry.name == "xl/sharedStrings.xml" -> {
                        parseSharedStrings(zip, sharedStrings)
                    }
                    entry.name == "xl/worksheets/sheet1.xml" -> {
                        sheetXml = zip.readBytes()
                    }
                }
                entry = zip.nextEntry
            }
        }

        if (sheetXml == null) {
            throw IllegalStateException("Không tìm thấy sheet trong file Excel")
        }

        // Parse sheet data
        return parseSheet(sheetXml!!.inputStream(), sharedStrings)
    }

    private fun parseSharedStrings(input: InputStream, strings: MutableList<String>) {
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(input, "UTF-8")

        var inT = false
        val sb = StringBuilder()

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "t") {
                        inT = true
                        sb.clear()
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inT) sb.append(parser.text)
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "t") {
                        inT = false
                    } else if (parser.name == "si") {
                        strings.add(sb.toString())
                        sb.clear()
                    }
                }
            }
            eventType = parser.next()
        }
    }

    private fun parseSheet(input: InputStream, sharedStrings: List<String>): ExcelImportResult {
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(input, "UTF-8")

        val cards = mutableListOf<ExcelCardRow>()
        var totalRows = 0
        var skippedRows = 0

        // Current row data
        var currentRow = mutableMapOf<Int, String>() // column index -> value
        var currentCellRef = ""
        var cellType = ""
        var inValue = false
        val valueSb = StringBuilder()

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "row" -> {
                            currentRow = mutableMapOf()
                        }
                        "c" -> {
                            currentCellRef = parser.getAttributeValue(null, "r") ?: ""
                            cellType = parser.getAttributeValue(null, "t") ?: ""
                        }
                        "v" -> {
                            inValue = true
                            valueSb.clear()
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inValue) valueSb.append(parser.text)
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "v" -> {
                            inValue = false
                            val rawValue = valueSb.toString()
                            val colIndex = getColumnIndex(currentCellRef)

                            val cellValue = if (cellType == "s") {
                                // Shared string reference
                                val idx = rawValue.toIntOrNull() ?: 0
                                sharedStrings.getOrElse(idx) { "" }
                            } else {
                                rawValue
                            }

                            if (colIndex in 0..3) {
                                currentRow[colIndex] = cellValue
                            }
                        }
                        "row" -> {
                            totalRows++
                            val front = currentRow[0]?.trim() ?: ""
                            val back = currentRow[1]?.trim() ?: ""
                            val example = currentRow[2]?.trim() ?: ""
                            val ipa = currentRow[3]?.trim() ?: ""

                            if (front.isBlank() && back.isBlank()) {
                                skippedRows++
                            } else {
                                cards.add(
                                    ExcelCardRow(
                                        frontText = front,
                                        backText = back,
                                        exampleText = example.ifBlank { null },
                                        pronunciationIpa = ipa.ifBlank { null }
                                    )
                                )
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        return ExcelImportResult(cards, skippedRows, totalRows)
    }

    /**
     * Convert Excel cell reference (e.g. "A1", "B3", "C12") to column index (0, 1, 2).
     */
    private fun getColumnIndex(cellRef: String): Int {
        var col = 0
        for (ch in cellRef) {
            if (ch.isLetter()) {
                col = col * 26 + (ch.uppercaseChar() - 'A' + 1)
            } else break
        }
        return col - 1 // 0-based
    }
}
