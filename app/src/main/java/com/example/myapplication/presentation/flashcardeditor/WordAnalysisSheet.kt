package com.example.myapplication.presentation.flashcardeditor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.domain.repository.WordAnalysisResult
import com.example.myapplication.domain.repository.WordSenseItem

/**
 * Bottom sheet that displays the polysemy analysis result:
 * main sense, related variants, other meanings, word variants.
 */
@Composable
fun WordAnalysisSheet(
    result: WordAnalysisResult,
    onDismiss: () -> Unit
) {
    val cs = MaterialTheme.colorScheme

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Header ──
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MenuBook, null, tint = cs.primary, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            "Phân tích từ: \"${result.lemma}\"",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = cs.onSurface
                        )
                        Text(
                            "Từ loại phát hiện: ${posLabel(result.detectedPOS)}",
                            fontSize = 13.sp,
                            color = cs.onSurfaceVariant
                        )
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Đóng", color = cs.primary)
                }
            }
        }

        // ── Main Sense ──
        item {
            Text(
                "★ Nghĩa chính",
                fontWeight = FontWeight.SemiBold,
                color = cs.primary,
                fontSize = 15.sp
            )
        }
        item {
            SenseCard(sense = result.mainSense, isMain = true)
        }

        // ── Related Variants ──
        if (result.relatedVariants.isNotEmpty()) {
            item {
                HorizontalDivider(color = cs.outlineVariant.copy(alpha = 0.3f))
            }
            item {
                Text(
                    "Các biến thể liên quan",
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onSurface,
                    fontSize = 15.sp
                )
            }
            items(result.relatedVariants) { sense ->
                SenseCard(sense = sense, isMain = false)
            }
        }

        // ── Other Meanings (homonyms) ──
        if (result.otherMeanings.isNotEmpty()) {
            item {
                HorizontalDivider(color = cs.outlineVariant.copy(alpha = 0.3f))
            }
            item {
                Text(
                    "Nghĩa khác (đồng âm)",
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onSurfaceVariant,
                    fontSize = 15.sp
                )
            }
            items(result.otherMeanings) { sense ->
                SenseCard(sense = sense, isMain = false)
            }
        }

        // ── Word Variants ──
        if (result.wordVariants.isNotEmpty()) {
            item {
                HorizontalDivider(color = cs.outlineVariant.copy(alpha = 0.3f))
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Translate, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Biến thể hình thái",
                        fontWeight = FontWeight.SemiBold,
                        color = cs.onSurface,
                        fontSize = 15.sp
                    )
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        result.wordVariants.forEach { v ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    v.variant,
                                    color = cs.onSurface,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                                Text(
                                    variantTypeLabel(v.type),
                                    color = cs.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    fontStyle = FontStyle.Italic
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom padding
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun SenseCard(sense: WordSenseItem, isMain: Boolean) {
    val cs = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isMain) cs.primaryContainer.copy(alpha = 0.3f) else cs.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // POS badge + Definition
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isMain) cs.primary else cs.secondary)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        posLabel(sense.partOfSpeech),
                        color = cs.onPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    sense.definitionEn,
                    color = cs.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                // Similarity badge
                if (sense.similarityScore > 0) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                when {
                                    sense.similarityScore >= 80 -> cs.primary.copy(alpha = 0.15f)
                                    sense.similarityScore >= 40 -> cs.tertiary.copy(alpha = 0.15f)
                                    else -> cs.onSurfaceVariant.copy(alpha = 0.1f)
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "${sense.similarityScore}%",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                sense.similarityScore >= 80 -> cs.primary
                                sense.similarityScore >= 40 -> cs.tertiary
                                else -> cs.onSurfaceVariant
                            }
                        )
                    }
                }
            }

            // Vietnamese translation
            if (!sense.definitionVi.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "→ ${sense.definitionVi}",
                    color = cs.primary.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Example
            if (!sense.example.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "💡 ${sense.example}",
                    color = cs.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic,
                    maxLines = 2
                )
            }

            // Homonym cluster label
            if (!sense.homonymCluster.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Nhóm: ${sense.homonymCluster}",
                    color = cs.onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

private fun posLabel(pos: String): String = when (pos.lowercase()) {
    "noun" -> "Danh từ"
    "verb" -> "Động từ"
    "adjective", "adj" -> "Tính từ"
    "adverb", "adv" -> "Trạng từ"
    "preposition" -> "Giới từ"
    "pronoun" -> "Đại từ"
    "conjunction" -> "Liên từ"
    else -> pos
}

private fun variantTypeLabel(type: String): String = when (type.lowercase()) {
    "past_tense" -> "quá khứ"
    "past_participle" -> "quá khứ phân từ"
    "present_participle" -> "hiện tại phân từ"
    "plural" -> "số nhiều"
    "third_person" -> "ngôi 3 số ít"
    "comparative" -> "so sánh hơn"
    "superlative" -> "so sánh nhất"
    "gerund" -> "danh động từ"
    else -> type
}
