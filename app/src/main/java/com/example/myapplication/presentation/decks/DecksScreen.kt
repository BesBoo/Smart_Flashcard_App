//DecksScreen.kt
package com.example.myapplication.presentation.decks

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myapplication.domain.model.Deck
import com.example.myapplication.presentation.components.DeckCard
import com.example.myapplication.data.remote.dto.ViolationNotice
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background

@Composable
fun DecksScreen(
    modifier: Modifier = Modifier,
    viewModel: DecksViewModel = hiltViewModel(),
    onDeckClick: (String) -> Unit = {},
    onCreateDeckClick: () -> Unit = {},
    onJoinDeckClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val cs = MaterialTheme.colorScheme
    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var deckToRename by remember { mutableStateOf<Deck?>(null) }
    var deckToDelete by remember { mutableStateOf<Deck?>(null) }
    var deckToLeave by remember { mutableStateOf<Deck?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Join deck button
                FloatingActionButton(
                    onClick = onJoinDeckClick,
                    containerColor = cs.secondaryContainer,
                    contentColor = cs.onSecondaryContainer,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(imageVector = Icons.Default.GroupAdd, contentDescription = "Nhập mã chia sẻ")
                }
                // Create deck button
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = cs.primary,
                    contentColor = cs.onPrimary,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Tạo bộ thẻ mới")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Thư viện của bạn", color = cs.onSurface, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("${uiState.decks.size} bộ", color = cs.onSurfaceVariant, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Tìm kiếm bộ thẻ...", color = cs.onSurfaceVariant) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = cs.onSurfaceVariant) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = cs.primary,
                    unfocusedBorderColor = cs.outlineVariant,
                    focusedContainerColor = cs.surfaceContainer,
                    unfocusedContainerColor = cs.surfaceContainer,
                    focusedTextColor = cs.onSurface,
                    unfocusedTextColor = cs.onSurface,
                    cursorColor = cs.primary
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = cs.primary)
                }
            } else if (uiState.decks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Chưa có bộ thẻ nào", color = cs.onSurfaceVariant, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Bấm nút + để tạo bộ thẻ đầu tiên!", color = cs.onSurfaceVariant, fontSize = 14.sp)
                    }
                }
            } else {
                val filteredDecks = uiState.decks.filter {
                    it.name.contains(searchQuery, ignoreCase = true)
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(1),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredDecks) { deck ->
                        DeckCard(
                            deck = deck,
                            onClick = { onDeckClick(deck.id) },
                            onRenameClick = { deckToRename = deck },
                            onDeleteClick = { deckToDelete = deck },
                            onLeaveClick = { deckToLeave = deck }
                        )
                    }
                }
            }
        }

        // Create Deck Dialog
        if (showCreateDialog) {
            CreateDeckDialog(
                onDismiss = { showCreateDialog = false },
                onCreate = { name, desc, coverUri ->
                    viewModel.createDeck(name, desc, coverUri?.toString())
                    showCreateDialog = false
                }
            )
        }

        // Rename Deck Dialog
        deckToRename?.let { deck ->
            RenameDeckDialog(
                currentName = deck.name,
                onDismiss = { deckToRename = null },
                onRename = { newName ->
                    viewModel.renameDeck(deck.id, newName)
                    deckToRename = null
                }
            )
        }

        // Delete Confirmation Dialog
        deckToDelete?.let { deck ->
            DeleteDeckConfirmDialog(
                deckName = deck.name,
                onDismiss = { deckToDelete = null },
                onConfirm = {
                    viewModel.deleteDeck(deck.id)
                    deckToDelete = null
                }
            )
        }

        // Leave Deck Confirmation Dialog
        deckToLeave?.let { deck ->
            AlertDialog(
                onDismissRequest = { deckToLeave = null },
                containerColor = cs.surface,
                icon = { Icon(Icons.Default.Warning, null, tint = cs.error) },
                title = { Text("Rời khỏi bộ thẻ?", fontWeight = FontWeight.Bold, color = cs.onSurface) },
                text = {
                    Text(
                        "Bạn sẽ không thể truy cập bộ thẻ \"${deck.name}\" nữa. Bạn có thể tham gia lại nếu có mã chia sẻ.",
                        color = cs.onSurfaceVariant
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.leaveDeck(deck.id)
                            deckToLeave = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = cs.error)
                    ) {
                        Text("Rời khỏi", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deckToLeave = null }) {
                        Text("Hủy")
                    }
                }
            )
        }

        // Violation Notification Dialog
        if (uiState.showViolationDialog && uiState.violations.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissViolationDialog() },
                containerColor = cs.surface,
                icon = { Icon(Icons.Default.Warning, null, tint = cs.error) },
                title = {
                    Text(
                        "Thông báo vi phạm nội dung",
                        fontWeight = FontWeight.Bold,
                        color = cs.error
                    )
                },
                text = {
                    Column {
                        uiState.violations.forEach { v ->
                            Text(
                                "• Bộ thẻ \"${v.deckName}\" đã bị xóa do vi phạm: ${v.reason}",
                                color = cs.onSurfaceVariant,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Vui lòng tuân thủ quy định cộng đồng.",
                            color = cs.onSurfaceVariant,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.dismissViolationDialog() },
                        colors = ButtonDefaults.buttonColors(containerColor = cs.primary)
                    ) {
                        Text("Đã hiểu")
                    }
                }
            )
        }
    }
}

@Composable
fun CreateDeckDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, description: String, coverImageUri: Uri?) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var coverImageUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { coverImageUri = it }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Tạo bộ thẻ mới", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; showError = false },
                    label = { Text("Tên bộ thẻ (*)") },
                    isError = showError && name.isBlank(),
                    supportingText = if (showError && name.isBlank()) { { Text("Không được để trống") } } else null,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = cs.primary, cursorColor = cs.primary),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Mô tả (Tùy chọn)") },
                    minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = cs.primary, cursorColor = cs.primary),
                    modifier = Modifier.fillMaxWidth()
                )

                // ── Cover Image Picker (CARD-01/03) ──
                Text("Ảnh bìa (tùy chọn)", fontSize = 13.sp, color = cs.onSurfaceVariant)
                if (coverImageUri != null) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(coverImageUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Ảnh bìa",
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Icon(
                            Icons.Default.Close,
                            "Xóa ảnh",
                            tint = Color.White,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(24.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                .clickable { coverImageUri = null }
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(cs.surfaceContainer)
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AddPhotoAlternate,
                                null,
                                tint = cs.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.padding(horizontal = 4.dp))
                            Text("Chọn ảnh bìa", color = cs.onSurfaceVariant, fontSize = 13.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onCreate(name, description, coverImageUri) else showError = true }) {
                Text("Tạo", color = cs.primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy", color = cs.onSurfaceVariant) }
        },
        containerColor = cs.surface
    )
}

@Composable
fun RenameDeckDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onRename: (newName: String) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    var newName by remember { mutableStateOf(currentName) }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Đổi tên bộ thẻ", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it; showError = false },
                label = { Text("Tên mới") },
                isError = showError && newName.isBlank(),
                supportingText = if (showError && newName.isBlank()) { { Text("Không được để trống") } } else null,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = cs.primary, cursorColor = cs.primary),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { if (newName.isNotBlank()) onRename(newName.trim()) else showError = true }) {
                Text("Lưu", color = cs.primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy", color = cs.onSurfaceVariant) }
        },
        containerColor = cs.surface
    )
}

@Composable
fun DeleteDeckConfirmDialog(
    deckName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Xóa bộ thẻ", fontWeight = FontWeight.Bold) },
        text = {
            Text(
                text = "Bạn có chắc chắn muốn xóa bộ thẻ \"$deckName\"? Tất cả các thẻ trong bộ này cũng sẽ bị xóa. Hành động này không thể hoàn tác.",
                color = cs.onSurfaceVariant,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Xóa", color = cs.error, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy", color = cs.onSurfaceVariant) }
        },
        containerColor = cs.surface
    )
}
