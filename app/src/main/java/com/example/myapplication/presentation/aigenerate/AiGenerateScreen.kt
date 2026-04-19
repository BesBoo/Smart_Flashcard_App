package com.example.myapplication.presentation.aigenerate

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myapplication.ui.theme.GradientEnd
import com.example.myapplication.ui.theme.GradientStart
import com.example.myapplication.ui.theme.QualityGood

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun AiGenerateScreen(
    modifier: Modifier = Modifier,
    viewModel: AiGenerateViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val cs = MaterialTheme.colorScheme
    val context = LocalContext.current

    // Local text state for Vietnamese IME compatibility
    var localInputText by remember { mutableStateOf(uiState.inputText) }
    val lastVmInputText = remember { mutableStateOf(uiState.inputText) }
    if (uiState.inputText != lastVmInputText.value) {
        lastVmInputText.value = uiState.inputText
        localInputText = uiState.inputText
    }
    // Debounced sync to ViewModel
    LaunchedEffect(Unit) {
        snapshotFlow { localInputText }
            .debounce(500L)
            .collect { viewModel.updateInputText(it) }
    }

    // PDF file picker
    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = getFileName(context, it) ?: "file.pdf"
            viewModel.selectFile(it, fileName, InputMode.PDF)
        }
    }

    // DOCX file picker
    val docxLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = getFileName(context, it) ?: "file.docx"
            viewModel.selectFile(it, fileName, InputMode.DOCX)
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = cs.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Tạo thẻ bằng AI",
                        color = cs.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại", tint = cs.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = cs.background)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Deck info + AI usage badge (AI-15) ──
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    uiState.deck?.let { deck ->
                        Text(
                            text = "Bộ thẻ: ${deck.name}",
                            color = cs.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                    // Remaining AI usage badge
                    uiState.remainingAiUsage?.let { remaining ->
                        Box(
                            modifier = Modifier
                                .background(
                                    if (remaining > 5) cs.primary.copy(alpha = 0.1f)
                                    else cs.error.copy(alpha = 0.1f),
                                    CircleShape
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Còn $remaining/${uiState.maxAiUsage} lượt",
                                color = if (remaining > 5) cs.primary else cs.error,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // ── Upload file section ──
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "\uD83D\uDCCE Tải lên tài liệu",
                            color = cs.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Chọn file PDF hoặc DOCX để tự động tạo flashcard hoặc trích xuất từ vựng.",
                            color = cs.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { pdfLauncher.launch(arrayOf("application/pdf")) },
                                modifier = Modifier.weight(1f),
                                enabled = !uiState.isGenerating,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, null, modifier = Modifier.size(18.dp), tint = cs.error)
                                Spacer(Modifier.width(6.dp))
                                Text("Chọn file PDF", fontSize = 13.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    docxLauncher.launch(arrayOf(
                                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                        "application/msword"
                                    ))
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !uiState.isGenerating,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Description, null, modifier = Modifier.size(18.dp), tint = cs.primary)
                                Spacer(Modifier.width(6.dp))
                                Text("Chọn file DOCX", fontSize = 13.sp)
                            }
                        }

                        // Show selected file
                        if (uiState.selectedFileName != null) {
                            Spacer(Modifier.height(8.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = cs.primary.copy(alpha = 0.1f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (uiState.inputMode == InputMode.PDF) Icons.Default.PictureAsPdf else Icons.Default.Description,
                                        null,
                                        tint = cs.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = uiState.selectedFileName!!,
                                        color = cs.onSurface,
                                        fontSize = 13.sp,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    IconButton(
                                        onClick = { viewModel.clearFile() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, "Xóa", tint = cs.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Generation mode selector (only visible when file is selected) ──
            if (uiState.selectedFileUri != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "⚙\uFE0F Chế độ xử lý",
                                color = cs.onSurface,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                            Spacer(Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Extract Vocab chip
                                FilterChip(
                                    selected = uiState.generationMode == GenerationMode.EXTRACT_VOCAB,
                                    onClick = { viewModel.setGenerationMode(GenerationMode.EXTRACT_VOCAB) },
                                    label = { Text("\uD83D\uDCD6 Trích xuất từ vựng", fontSize = 13.sp) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = cs.primary.copy(alpha = 0.2f),
                                        selectedLabelColor = cs.primary
                                    )
                                )

                                // Flashcards chip
                                FilterChip(
                                    selected = uiState.generationMode == GenerationMode.FLASHCARDS,
                                    onClick = { viewModel.setGenerationMode(GenerationMode.FLASHCARDS) },
                                    label = { Text("\uD83D\uDCDD Tạo flashcard", fontSize = 13.sp) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = cs.primary.copy(alpha = 0.2f),
                                        selectedLabelColor = cs.primary
                                    )
                                )
                            }

                            // Target language (only for vocab extraction)
                            AnimatedVisibility(
                                visible = uiState.generationMode == GenerationMode.EXTRACT_VOCAB,
                                enter = expandVertically(),
                                exit = shrinkVertically()
                            ) {
                                Column {
                                    Spacer(Modifier.height(12.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Translate, null,
                                            tint = cs.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "Dịch nghĩa sang:",
                                            color = cs.onSurfaceVariant,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        LanguageChip("\uD83C\uDDFB\uD83C\uDDF3 Tiếng Việt", "vi", uiState.targetLanguage) {
                                            viewModel.setTargetLanguage("vi")
                                        }
                                        LanguageChip("\uD83C\uDDEC\uD83C\uDDE7 English", "en", uiState.targetLanguage) {
                                            viewModel.setTargetLanguage("en")
                                        }
                                        LanguageChip("\uD83C\uDDEF\uD83C\uDDF5 日本語", "ja", uiState.targetLanguage) {
                                            viewModel.setTargetLanguage("ja")
                                        }
                                    }
                                }
                            }

                            // Description of selected mode
                            Spacer(Modifier.height(8.dp))
                            val desc = if (uiState.generationMode == GenerationMode.EXTRACT_VOCAB) {
                                "AI sẽ đọc đoạn văn → lọc từ vựng quan trọng → tạo nghĩa và ví dụ từ ngữ cảnh gốc."
                            } else {
                                "AI sẽ đọc nội dung → tạo flashcard dạng câu hỏi/khái niệm."
                            }
                            Text(text = desc, color = cs.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 12.sp)
                        }
                    }
                }
            }

            // ── OR divider ──
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.weight(1f).height(1.dp).background(cs.onSurfaceVariant.copy(alpha = 0.3f)))
                    Text(
                        text = "  HOẶC  ",
                        color = cs.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Box(Modifier.weight(1f).height(1.dp).background(cs.onSurfaceVariant.copy(alpha = 0.3f)))
                }
            }

            // ── Text input section ──
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "\uD83D\uDCDD Nhập nội dung",
                            color = cs.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Dán đoạn văn, ghi chú, hoặc nội dung bài học. Hệ thôngs sẽ tự động tạo flashcard cho bạn.",
                            color = cs.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = localInputText,
                            onValueChange = { localInputText = it },
                            placeholder = { Text("Dán nội dung vào đây...", color = cs.onSurfaceVariant) },
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !uiState.isGenerating && uiState.selectedFileUri == null,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = cs.primary,
                                unfocusedBorderColor = cs.onSurfaceVariant.copy(alpha = 0.3f),
                                focusedTextColor = cs.onSurface,
                                unfocusedTextColor = cs.onSurface,
                                cursorColor = cs.primary
                            )
                        )
                    }
                }
            }

            // ── Generate button ──
            item {
                val canGenerate = when (uiState.inputMode) {
                    InputMode.TEXT -> uiState.inputText.isNotBlank()
                    InputMode.PDF, InputMode.DOCX -> uiState.selectedFileUri != null
                }

                Button(
                    onClick = { viewModel.generateDrafts() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = canGenerate && !uiState.isGenerating,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = cs.primary,
                        disabledContainerColor = cs.primary.copy(alpha = 0.3f)
                    )
                ) {
                    if (uiState.isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Đang xử lý...", color = Color.White)
                    } else {
                        Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        val label = when {
                            uiState.inputMode == InputMode.TEXT -> "Tạo Flashcard tự động"
                            uiState.generationMode == GenerationMode.EXTRACT_VOCAB -> "Trích xuất từ vựng"
                            uiState.inputMode == InputMode.PDF -> "Tạo từ PDF"
                            else -> "Tạo từ DOCX"
                        }
                        Text(label, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ── Error ──
            if (uiState.error != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = cs.error.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "❌ ${uiState.error}",
                            color = cs.error,
                            modifier = Modifier.padding(16.dp),
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // ── Success message ──
            if (uiState.isSaved) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = QualityGood.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = QualityGood, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Đã lưu thẻ thành công!",
                                color = QualityGood,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // ── Draft review cards (AI-04/05/VOCAB-06) ──
            if (uiState.drafts.isNotEmpty() && !uiState.isSaved) {
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Bản nháp (${uiState.selectedCount}/${uiState.drafts.size} đã chọn)",
                            color = cs.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Chỉnh sửa trước khi lưu",
                            color = cs.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }

                itemsIndexed(uiState.drafts) { index, draft ->
                    DraftCardItem(
                        index = index,
                        draft = draft,
                        onToggle = { viewModel.toggleDraftSelection(index) },
                        onEdit = { front, back, example -> viewModel.editDraft(index, front, back, example) },
                        onRemove = { viewModel.removeDraft(index) }
                    )
                }

                // Save selected button
                item {
                    Button(
                        onClick = { viewModel.saveSelectedDrafts() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = uiState.selectedCount > 0,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = QualityGood,
                            disabledContainerColor = QualityGood.copy(alpha = 0.3f)
                        )
                    ) {
                        Icon(Icons.Default.Save, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Lưu ${uiState.selectedCount} thẻ đã chọn",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Bottom spacing
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

/** Language selection chip */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageChip(
    label: String,
    langCode: String,
    selected: String,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val isSelected = langCode == selected
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (isSelected) Modifier.background(cs.primary.copy(alpha = 0.15f))
                else Modifier.background(Color.Transparent)
            )
            .border(
                width = 1.dp,
                color = if (isSelected) cs.primary else cs.onSurfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) cs.primary else cs.onSurfaceVariant
        )
    }
}

/** Helper to get file name from Uri */
private fun getFileName(context: android.content.Context, uri: Uri): String? {
    var name: String? = null
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst() && nameIndex >= 0) {
            name = cursor.getString(nameIndex)
        }
    }
    return name
}

/** Draft card with checkbox, inline edit, and delete (AI-04/05/VOCAB-06) */
@Composable
private fun DraftCardItem(
    index: Int,
    draft: DraftCard,
    onToggle: () -> Unit,
    onEdit: (String, String, String) -> Unit,
    onRemove: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    var isEditing by remember { mutableStateOf(false) }
    var editFront by remember(draft.flashcard.frontText) { mutableStateOf(draft.flashcard.frontText) }
    var editBack by remember(draft.flashcard.backText) { mutableStateOf(draft.flashcard.backText) }
    var editExample by remember(draft.flashcard.exampleText) { mutableStateOf(draft.flashcard.exampleText ?: "") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (draft.isSelected) cs.surfaceContainer else cs.surfaceContainer.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = draft.isSelected,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(checkedColor = cs.primary)
                )

                Box(
                    Modifier.size(28.dp).clip(CircleShape)
                        .background(Brush.linearGradient(listOf(GradientStart, GradientEnd))),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${index + 1}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = draft.flashcard.frontText,
                    color = if (draft.isSelected) cs.onSurface else cs.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = { isEditing = !isEditing }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, "Sửa", tint = cs.primary, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, "Xóa", tint = cs.error, modifier = Modifier.size(18.dp))
                }
            }

            if (!isEditing) {
                Spacer(Modifier.height(4.dp))
                Text(draft.flashcard.backText, color = cs.onSurfaceVariant, fontSize = 14.sp, maxLines = 3)
                if (!draft.flashcard.exampleText.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text("\uD83D\uDCA1 ${draft.flashcard.exampleText}", color = cs.primary.copy(alpha = 0.8f), fontSize = 13.sp, maxLines = 3)
                }
            } else {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = editFront,
                    onValueChange = { editFront = it },
                    label = { Text("Mặt trước") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = cs.primary,
                        focusedTextColor = cs.onSurface,
                        unfocusedTextColor = cs.onSurface,
                        cursorColor = cs.primary
                    )
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = editBack,
                    onValueChange = { editBack = it },
                    label = { Text("Mặt sau") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = cs.primary,
                        focusedTextColor = cs.onSurface,
                        unfocusedTextColor = cs.onSurface,
                        cursorColor = cs.primary
                    )
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = editExample,
                    onValueChange = { editExample = it },
                    label = { Text("Ví dụ") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = cs.primary,
                        focusedTextColor = cs.onSurface,
                        unfocusedTextColor = cs.onSurface,
                        cursorColor = cs.primary
                    )
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        onEdit(editFront, editBack, editExample)
                        isEditing = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = cs.primary)
                ) {
                    Text("Xong", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
