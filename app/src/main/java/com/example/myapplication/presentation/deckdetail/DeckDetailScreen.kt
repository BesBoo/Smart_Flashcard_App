//DeckDetailScreen.kt
package com.example.myapplication.presentation.deckdetail

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myapplication.domain.model.Flashcard
import com.example.myapplication.ui.theme.GradientEnd
import com.example.myapplication.ui.theme.GradientStart
import com.example.myapplication.ui.theme.QualityAgain
import com.example.myapplication.ui.theme.QualityGood
import com.example.myapplication.presentation.share.ShareDeckDialog
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckDetailScreen(
    modifier: Modifier = Modifier,
    viewModel: DeckDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onStartStudy: (String) -> Unit = {},
    onAddCard: (String) -> Unit = {},
    onEditCard: (String, String) -> Unit = { _, _ -> },
    onAiGenerate: (String) -> Unit = {},
    onQuiz: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val cs = MaterialTheme.colorScheme
    var showShareDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var reportReason by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val deck = uiState.deck
    val isOwner = deck?.isOwner != false
    val canEdit = isOwner || deck?.permission == "edit"

    // Excel file picker
    val excelPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importCardsFromExcel(it) }
    }

    Scaffold(
        modifier = modifier,
        containerColor = cs.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.deck?.name ?: "Chi tiết bộ thẻ",
                        color = cs.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại", tint = cs.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = cs.background),
                actions = {
                    // Report button (for non-owner shared decks)
                    if (!isOwner) {
                        IconButton(onClick = { showReportDialog = true }) {
                            Icon(
                                Icons.Default.Flag,
                                "Báo cáo",
                                tint = cs.error.copy(alpha = 0.7f)
                            )
                        }
                    }
                    // Share button (only for owner)
                    if (isOwner) {
                        IconButton(onClick = { showShareDialog = true }) {
                            Icon(
                                Icons.Default.Share,
                                "Chia sẻ",
                                tint = if (deck?.isShared == true) cs.primary else cs.onSurfaceVariant
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (canEdit) {
                FloatingActionButton(
                    onClick = { uiState.deck?.id?.let { onAddCard(it) } },
                    containerColor = cs.primary,
                    contentColor = cs.onPrimary,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Thêm thẻ")
                }
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = cs.primary)
            }
            return@Scaffold
        }

        @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refreshFromServer(showIndicator = true) },
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            // Study Card Header
            item {
                StudyHeaderCard(
                    dueCount = uiState.dueCount,
                    totalCards = uiState.totalCards,
                    newCount = uiState.newCount,
                    masteredCount = uiState.masteredCount,
                    onStartStudy = { uiState.deck?.id?.let { onStartStudy(it) } },
                    onAiGenerate = { uiState.deck?.id?.let { onAiGenerate(it) } },
                    onQuiz = { uiState.deck?.id?.let { onQuiz(it) } }
                )
            }

            // Section title
            item {
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Danh sách thẻ", color = cs.onSurface, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text("${uiState.totalCards} thẻ", color = cs.onSurfaceVariant, fontSize = 14.sp)
                }
            }

            // Search bar (CARD-09)
            item {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.updateSearch(it) },
                    placeholder = { Text("Tìm kiếm thẻ...", color = cs.onSurfaceVariant) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = cs.onSurfaceVariant) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = cs.primary,
                        unfocusedBorderColor = cs.outlineVariant,
                        focusedTextColor = cs.onSurface,
                        unfocusedTextColor = cs.onSurface,
                        cursorColor = cs.primary
                    )
                )
            }

            // Empty state
            if (uiState.flashcards.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.School, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("Chưa có thẻ nào", color = cs.onSurfaceVariant, fontSize = 16.sp)
                            Text("Bấm + để tạo thẻ hoặc nhập từ Excel!", color = cs.onSurfaceVariant, fontSize = 14.sp)
                        }
                    }
                }
            }

            // ── Import Excel button ──
            if (canEdit) {
                item {
                    // Import result message
                    if (uiState.importResult != null) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = cs.primaryContainer.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    uiState.importResult!!,
                                    color = cs.onSurface,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.clearImportResult() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Text("✕", color = cs.onSurfaceVariant, fontSize = 14.sp)
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            excelPickerLauncher.launch(
                                arrayOf(
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", // .xlsx
                                    "application/vnd.ms-excel", // .xls
                                    "application/octet-stream"  // fallback
                                )
                            )
                        },
                        enabled = !uiState.isImporting,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, cs.outline.copy(alpha = 0.3f))
                    ) {
                        if (uiState.isImporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = cs.primary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("Đang nhập...", color = cs.onSurface, fontSize = 14.sp)
                        } else {
                            Icon(
                                Icons.Default.UploadFile,
                                null,
                                tint = cs.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Nhập từ file Excel",
                                color = cs.onSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // ── Google Sheet Sync ──
            if (canEdit) {
                item {
                    // Sync result message
                    if (uiState.syncResult != null) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = cs.tertiaryContainer.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    uiState.syncResult!!,
                                    color = cs.onSurface,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.clearSyncResult() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Text("✕", color = cs.onSurfaceVariant, fontSize = 14.sp)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Link / Unlink button
                        OutlinedButton(
                            onClick = {
                                if (uiState.isSheetLinked) viewModel.unlinkGoogleSheet()
                                else viewModel.showLinkSheetDialog()
                            },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.dp,
                                if (uiState.isSheetLinked) cs.error.copy(alpha = 0.3f)
                                else cs.outline.copy(alpha = 0.3f)
                            )
                        ) {
                            Icon(
                                if (uiState.isSheetLinked) Icons.Default.LinkOff
                                else Icons.Default.Link,
                                null,
                                tint = if (uiState.isSheetLinked) cs.error else cs.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (uiState.isSheetLinked) "Hủy liên kết"
                                else "Liên kết Google Sheet",
                                color = cs.onSurface,
                                fontSize = 13.sp
                            )
                        }

                        // Sync button (only when linked)
                        if (uiState.isSheetLinked) {
                            Button(
                                onClick = { viewModel.syncGoogleSheet() },
                                enabled = !uiState.isSyncing,
                                modifier = Modifier.weight(1f).height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = cs.primary
                                )
                            ) {
                                if (uiState.isSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = cs.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Sync,
                                        null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (uiState.isSyncing) "Đang đồng bộ..." else "Đồng bộ",
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            // Flashcard list (filtered by search)
            items(uiState.filteredFlashcards, key = { it.id }) { card ->
                FlashcardListItem(
                    card = card,
                    canEdit = canEdit,
                    onEdit = { onEditCard(card.deckId, card.id) },
                    onDelete = { viewModel.deleteFlashcard(card.id) }
                )
            }

            // Bottom spacing for FAB
            item { Spacer(Modifier.height(72.dp)) }
        }
        }
    }

    // ShareDeckDialog
    if (showShareDialog && deck != null) {
        ShareDeckDialog(
            deckId = deck.id,
            isShared = deck.isShared,
            onDismiss = { showShareDialog = false }
        )
    }

    // Report Dialog
    if (showReportDialog && deck != null) {
        AlertDialog(
            onDismissRequest = {
                showReportDialog = false
                reportReason = ""
            },
            containerColor = cs.surface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Flag, null, tint = cs.error, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Báo cáo vi phạm", fontWeight = FontWeight.Bold, color = cs.onSurface)
                }
            },
            text = {
                Column {
                    Text(
                        "Bộ thẻ: ${deck.name}",
                        color = cs.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = reportReason,
                        onValueChange = { reportReason = it },
                        placeholder = { Text("Mô tả lý do báo cáo...", color = cs.onSurfaceVariant.copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = cs.error,
                            unfocusedBorderColor = cs.outlineVariant,
                            focusedTextColor = cs.onSurface,
                            unfocusedTextColor = cs.onSurface,
                            cursorColor = cs.error
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (reportReason.isNotBlank()) {
                            viewModel.submitReport(deck.id, reportReason)
                            showReportDialog = false
                            reportReason = ""
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Đã gửi báo cáo. Cảm ơn bạn!")
                            }
                        }
                    },
                    enabled = reportReason.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = cs.error)
                ) {
                    Text("Gửi báo cáo", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showReportDialog = false
                    reportReason = ""
                }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Snackbar
    Box(Modifier.fillMaxSize()) {
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // Link Google Sheet Dialog
    if (uiState.showSheetDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissSheetDialog() },
            containerColor = cs.surface,
            title = {
                Text(
                    "Liên kết Google Sheet",
                    fontWeight = FontWeight.Bold,
                    color = cs.onSurface
                )
            },
            text = {
                Column {
                    Text(
                        "Dán link Google Sheet của bạn vào đây.\nCấu trúc: Cột A = Mặt trước, B = Mặt sau, C = Ví dụ",
                        color = cs.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = uiState.sheetUrl,
                        onValueChange = { viewModel.updateSheetUrl(it) },
                        placeholder = {
                            Text(
                                "https://docs.google.com/spreadsheets/d/...",
                                color = cs.onSurfaceVariant.copy(alpha = 0.5f),
                                fontSize = 13.sp
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = cs.primary,
                            unfocusedBorderColor = cs.outlineVariant,
                            focusedTextColor = cs.onSurface,
                            unfocusedTextColor = cs.onSurface,
                            cursorColor = cs.primary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.linkGoogleSheet() },
                    enabled = uiState.sheetUrl.isNotBlank() && !uiState.isSyncing,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = cs.primary)
                ) {
                    if (uiState.isSyncing) {
                        CircularProgressIndicator(
                            Modifier.size(16.dp),
                            color = cs.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Liên kết", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissSheetDialog() }) {
                    Text("Đóng", color = cs.onSurfaceVariant)
                }
            }
        )
    }
}

@Composable
private fun StudyHeaderCard(
    dueCount: Int,
    totalCards: Int,
    newCount: Int,
    masteredCount: Int,
    onStartStudy: () -> Unit,
    onAiGenerate: () -> Unit = {},
    onQuiz: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(GradientStart, GradientEnd)))
                .padding(20.dp)
        ) {
            Column {
                Text("Sẵn sàng ôn tập!", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                val studyTotal = newCount + dueCount
                Text(
                    text = if (studyTotal > 0) "$studyTotal thẻ cần ôn tập" else "Không có thẻ cần ôn!",
                    color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))

                // Stats row
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    MiniStat("Mới", "$newCount", Color.White.copy(alpha = 0.8f))
                    MiniStat("Ôn tập", "$dueCount", Color.White.copy(alpha = 0.9f))
                    MiniStat("Thành thạo", "$masteredCount", QualityGood)
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = onStartStudy,
                    enabled = dueCount > 0 || totalCards > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Bắt đầu học", fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onAiGenerate,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("AI Tạo thẻ", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(
                        onClick = onQuiz,
                        enabled = totalCards >= 2,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.Quiz, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Quiz", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
    }
}

@Composable
private fun FlashcardListItem(
    card: Flashcard,
    canEdit: Boolean = true,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status indicator
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            card.isNew -> MaterialTheme.colorScheme.secondary
                            card.isDue -> QualityAgain
                            else -> QualityGood
                        }
                    )
            )
            Spacer(Modifier.width(12.dp))

            // Card content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = card.frontText,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = card.backText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Action buttons (only if canEdit)
            if (canEdit) {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, "Sửa", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, "Xóa", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
