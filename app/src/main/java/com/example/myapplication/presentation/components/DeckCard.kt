package com.example.myapplication.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.GroupWork
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.domain.model.Deck
import com.example.myapplication.ui.theme.DeckCardShape

@Composable
fun DeckCard(
    deck: Deck,
    onClick: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    onMoreClick: () -> Unit = {},
    onLeaveClick: () -> Unit = {}
) {
    val cs = MaterialTheme.colorScheme
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth().clickable { onClick() },
        shape = DeckCardShape,
        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Icon + Title
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape)
                            .background(
                                if (deck.isOwner) cs.primary.copy(alpha = 0.1f)
                                else cs.tertiary.copy(alpha = 0.1f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (!deck.isOwner) Icons.Default.GroupWork else Icons.Default.Style,
                            null,
                            tint = if (deck.isOwner) cs.primary else cs.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = deck.name,
                            color = cs.onSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        // Show owner name for subscribed decks
                        if (!deck.isOwner && !deck.ownerName.isNullOrBlank()) {
                            Text(
                                text = "Được chia sẻ bởi ${deck.ownerName}",
                                color = cs.tertiary,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Share indicator for owned shared decks
                if (deck.isOwner && deck.isShared) {
                    Icon(
                        Icons.Default.Share,
                        "Đang chia sẻ",
                        tint = cs.primary.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp).padding(end = 4.dp)
                    )
                }

                // More options (only for owner)
                if (deck.isOwner) {
                    Box {
                        IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.MoreVert, "Options", tint = cs.onSurfaceVariant)
                        }

                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Edit, null, tint = cs.primary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Đổi tên", color = cs.onSurface)
                                    }
                                },
                                onClick = { showMenu = false; onRenameClick() }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Delete, null, tint = cs.error, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Xóa bộ thẻ", color = cs.error)
                                    }
                                },
                                onClick = { showMenu = false; onDeleteClick() }
                            )
                        }
                    }
                } else {
                    // Menu for subscriber (leave option)
                    Box {
                        IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.MoreVert, "Options", tint = cs.onSurfaceVariant)
                        }

                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.ExitToApp, null, tint = cs.error, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Rời khỏi deck", color = cs.error)
                                    }
                                },
                                onClick = { showMenu = false; onLeaveClick() }
                            )
                        }
                    }
                    // Permission badge
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (deck.permission == "edit") cs.primary.copy(alpha = 0.1f) else cs.outlineVariant.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (deck.permission == "edit") "Chỉnh sửa" else "Chỉ xem",
                            fontSize = 11.sp,
                            color = if (deck.permission == "edit") cs.primary else cs.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (!deck.description.isNullOrBlank()) {
                Text(
                    text = deck.description,
                    color = cs.onSurfaceVariant,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(12.dp))
            } else {
                Spacer(modifier = Modifier.height(4.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                CardCountBadge(count = deck.dueCount, label = "Ôn tập", color = cs.onSurface)
                Spacer(modifier = Modifier.width(8.dp))
                CardCountBadge(count = deck.cardCount, label = "Tổng", color = cs.onSurface)
            }
        }
    }
}

@Composable
private fun CardCountBadge(count: Int, label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), shape = CircleShape)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text("$count", color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, color = color.copy(alpha = 0.8f), fontSize = 11.sp)
    }
}
