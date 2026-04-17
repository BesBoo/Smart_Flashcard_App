package com.example.myapplication.presentation.share

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myapplication.data.remote.dto.SubscriberInfo

@Composable
fun ShareDeckDialog(
    deckId: String,
    isShared: Boolean,
    onDismiss: () -> Unit,
    viewModel: ShareViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme

    LaunchedEffect(deckId) {
        if (isShared) {
            viewModel.loadShareInfo(deckId)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Share, null, tint = cs.primary, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("Chia sẻ bộ thẻ", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (uiState.isLoading) {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = cs.primary, modifier = Modifier.size(32.dp))
                    }
                } else if (uiState.shareInfo != null) {
                    // ── Share code display ──
                    val info = uiState.shareInfo!!
                    Text("Mã chia sẻ", color = cs.onSurfaceVariant, fontSize = 13.sp)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = cs.primaryContainer.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = info.shareCode,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = cs.primary,
                                letterSpacing = 4.sp
                            )
                            IconButton(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Share Code", info.shareCode))
                                Toast.makeText(context, "Đã sao chép mã!", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.ContentCopy, "Sao chép", tint = cs.primary)
                            }
                        }
                    }

                    // Permission info
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (info.defaultPermission == "edit") Icons.Default.Edit else Icons.Default.Visibility,
                            null, tint = cs.onSurfaceVariant, modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Quyền mặc định: ${if (info.defaultPermission == "edit") "Chỉnh sửa" else "Chỉ xem"}",
                            color = cs.onSurfaceVariant, fontSize = 13.sp
                        )
                    }

                    // Subscribers list
                    if (info.subscribers.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Người tham gia (${info.subscribers.size})",
                            color = cs.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold
                        )
                        LazyColumn(
                            modifier = Modifier.height((info.subscribers.size * 56).coerceAtMost(200).dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(info.subscribers) { sub ->
                                SubscriberRow(
                                    subscriber = sub,
                                    onTogglePermission = {
                                        val newPerm = if (sub.permission == "read") "edit" else "read"
                                        viewModel.updatePermission(deckId, sub.userId, newPerm)
                                    },
                                    onKick = { viewModel.kickSubscriber(deckId, sub.userId) }
                                )
                            }
                        }
                    }

                    // Stop sharing button
                    Spacer(Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { viewModel.stopSharing(deckId); onDismiss() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = cs.error)
                    ) {
                        Icon(Icons.Default.Lock, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Hủy chia sẻ", fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    // ── Not shared yet — show create share ──
                    Text(
                        "Tạo mã chia sẻ để người khác có thể truy cập bộ thẻ này. Người nhận sẽ luôn thấy nội dung mới nhất khi bạn cập nhật.",
                        color = cs.onSurfaceVariant, fontSize = 13.sp
                    )

                    Button(
                        onClick = { viewModel.createShare(deckId) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = cs.primary)
                    ) {
                        Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Tạo mã chia sẻ", fontWeight = FontWeight.Bold)
                    }
                }

                // Error message
                uiState.error?.let {
                    Text(it, color = cs.error, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng", color = cs.primary, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = cs.surface,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun SubscriberRow(
    subscriber: SubscriberInfo,
    onTogglePermission: () -> Unit,
    onKick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar placeholder
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape)
                .background(cs.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, null, tint = cs.primary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(subscriber.displayName, fontSize = 14.sp, color = cs.onSurface, fontWeight = FontWeight.Medium)
            Text(
                if (subscriber.permission == "edit") "Chỉnh sửa" else "Chỉ xem",
                fontSize = 12.sp, color = cs.onSurfaceVariant
            )
        }
        // Toggle permission
        IconButton(onClick = onTogglePermission, modifier = Modifier.size(28.dp)) {
            Icon(
                if (subscriber.permission == "read") Icons.Default.Edit else Icons.Default.Visibility,
                "Đổi quyền",
                tint = cs.primary,
                modifier = Modifier.size(16.dp)
            )
        }
        // Kick
        IconButton(onClick = onKick, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Delete, "Xóa", tint = cs.error.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
        }
    }
}
