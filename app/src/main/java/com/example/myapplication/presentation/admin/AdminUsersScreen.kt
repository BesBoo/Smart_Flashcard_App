package com.example.myapplication.presentation.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myapplication.data.remote.dto.AdminUserDto

// Confirm action data class
private data class ConfirmAction(
    val title: String,
    val message: String,
    val onConfirm: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersScreen(
    modifier: Modifier = Modifier,
    viewModel: AdminUsersViewModel = hiltViewModel(),
    onLogout: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val cs = MaterialTheme.colorScheme
    var selectedUser by remember { mutableStateOf<AdminUserDto?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }
    var confirmAction by remember { mutableStateOf<ConfirmAction?>(null) }

    // Force logout when admin demotes themselves
    LaunchedEffect(uiState.selfDemoted) {
        if (uiState.selfDemoted) {
            snackbarHostState.showSnackbar("Bạn đã hạ quyền của chính mình. Đang đăng xuất...")
            onLogout()
        }
    }

    LaunchedEffect(uiState.actionMessage) {
        uiState.actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearActionMessage()
        }
    }

    // ADMIN-15: Confirm Dialog
    confirmAction?.let { action ->
        AlertDialog(
            onDismissRequest = { confirmAction = null },
            title = { Text(action.title) },
            text = { Text(action.message) },
            confirmButton = {
                Button(
                    onClick = {
                        action.onConfirm()
                        confirmAction = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = cs.error)
                ) {
                    Text("Xác nhận", color = cs.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmAction = null }) {
                    Text("Hủy")
                }
            },
            containerColor = cs.surface
        )
    }

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Text("Quản lý người dùng", color = cs.onSurface, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("${uiState.users.size} người dùng", color = cs.onSurfaceVariant, fontSize = 13.sp)
            Spacer(Modifier.height(16.dp))

            // Search bar (ADMIN-10)
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.search(it) },
                placeholder = { Text("Tìm email hoặc tên...", color = cs.onSurfaceVariant) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = cs.onSurfaceVariant) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = cs.primary,
                    unfocusedBorderColor = cs.outlineVariant,
                    focusedContainerColor = cs.surfaceContainer,
                    unfocusedContainerColor = cs.surfaceContainer,
                    focusedTextColor = cs.onSurface,
                    unfocusedTextColor = cs.onSurface,
                    cursorColor = cs.primary
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            Spacer(Modifier.height(16.dp))

            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = cs.primary)
                    }
                }
                uiState.error != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(uiState.error!!, color = cs.onSurfaceVariant, fontSize = 14.sp)
                    }
                }
                uiState.users.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Không tìm thấy người dùng", color = cs.onSurfaceVariant, fontSize = 14.sp)
                    }
                }
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(uiState.users, key = { it.id }) { user ->
                            UserRow(user) { selectedUser = user }
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }

    if (selectedUser != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedUser = null },
            sheetState = sheetState,
            containerColor = cs.surface
        ) {
            UserActionSheet(
                user = selectedUser!!,
                onBan = { userId, ban ->
                    selectedUser = null
                    confirmAction = ConfirmAction(
                        title = if (ban) "Khóa tài khoản?" else "Mở khóa?",
                        message = if (ban) "Người dùng sẽ không thể đăng nhập." else "Người dùng sẽ được đăng nhập trở lại.",
                        onConfirm = { viewModel.banUser(userId, ban) }
                    )
                },
                onChangeRole = { userId, newRole ->
                    selectedUser = null
                    confirmAction = ConfirmAction(
                        title = "Đổi quyền thành $newRole?",
                        message = "Người dùng sẽ có quyền ${if (newRole == "admin") "quản trị viên" else "thường"}.",
                        onConfirm = { viewModel.changeRole(userId, newRole) }
                    )
                },
                onClose = { selectedUser = null }
            )
        }
    }
}

@Composable
private fun UserRow(user: AdminUserDto, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(CircleShape)
                    .background(if (user.role == "admin") cs.primary else cs.secondary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.displayName.take(1).uppercase(),
                    color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(user.displayName, color = cs.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(user.email, color = cs.onSurfaceVariant, fontSize = 13.sp)
                Text("${user.deckCount} decks • ${user.flashcardCount} cards", color = cs.onSurfaceVariant, fontSize = 12.sp)
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    user.role.uppercase(),
                    color = if (user.role == "admin") cs.primary else cs.onSurface,
                    fontWeight = FontWeight.Bold, fontSize = 11.sp,
                    modifier = Modifier.background(cs.primary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (user.isActive) "Active" else "Banned",
                    color = if (user.isActive) cs.tertiary else cs.error,
                    fontWeight = FontWeight.Medium, fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun UserActionSheet(
    user: AdminUserDto,
    onBan: (String, Boolean) -> Unit,
    onChangeRole: (String, String) -> Unit,
    onClose: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(24.dp).padding(bottom = 24.dp)) {
        Text("Tài khoản: ${user.displayName}", color = cs.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(user.email, color = cs.onSurfaceVariant, fontSize = 14.sp)
        Spacer(Modifier.height(28.dp))

        // ADMIN-12/13: Ban/Unban
        Button(
            onClick = { onBan(user.id, user.isActive) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (user.isActive) cs.error.copy(alpha = 0.85f) else cs.tertiary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Block, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (user.isActive) "Khóa tài khoản" else "Mở khóa tài khoản", fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(12.dp))

        // ADMIN-14: Change role
        Button(
            onClick = {
                val newRole = if (user.role == "admin") "user" else "admin"
                onChangeRole(user.id, newRole)
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = cs.surfaceContainer),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Security, null, tint = cs.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                if (user.role == "admin") "Hạ quyền xuống User" else "Nâng quyền lên Admin",
                color = cs.primary, fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text("Đóng", color = cs.onSurfaceVariant)
        }
    }
}
