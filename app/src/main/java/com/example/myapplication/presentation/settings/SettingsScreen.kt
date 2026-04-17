package com.example.myapplication.presentation.settings

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myapplication.ui.theme.ThemeManager

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
    onLogout: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val cs = MaterialTheme.colorScheme
    val context = LocalContext.current
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()
    var showEmailDialog by remember { mutableStateOf(false) }
    // Permission launcher for Android 13+ notifications
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.toggleReminder(true)
    }

    LaunchedEffect(uiState.loggedOut) {
        if (uiState.loggedOut) onLogout()
    }

    if (uiState.isLoading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = cs.primary)
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Cài đặt", color = cs.onSurface, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }

        // User Profile Card
        item {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer)
            ) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(56.dp).clip(CircleShape).background(cs.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (uiState.user?.displayName?.take(1) ?: "U").uppercase(),
                            color = cs.onPrimary, fontWeight = FontWeight.Bold, fontSize = 24.sp
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            uiState.user?.displayName ?: "Người dùng",
                            color = cs.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 18.sp
                        )
                        Text(
                            uiState.user?.email ?: "",
                            color = cs.onSurfaceVariant, fontSize = 13.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            uiState.user?.subscriptionTier?.name ?: "FREE",
                            color = cs.primary, fontWeight = FontWeight.Bold, fontSize = 11.sp,
                            modifier = Modifier
                                .background(cs.primary.copy(alpha = 0.1f), CircleShape)
                                .padding(horizontal = 10.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // Appearance Section
        item {
            SectionTitle("Giao diện")
            SettingsCard {
                SettingsToggleItem(Icons.Default.DarkMode, "Chế độ tối", isDarkMode) {
                    ThemeManager.setDarkMode(context, it)
                }
                SettingsHDivider()
                SettingsItem(Icons.Default.Language, "Ngôn ngữ", "Tiếng Việt") {}
            }
        }

        // Study Settings Section (STUDY-11)
        item {
            SectionTitle("Học tập")
            SettingsCard {
                SettingsSliderItem(
                    icon = Icons.Default.Speed,
                    title = "Thẻ mới mỗi ngày",
                    value = uiState.newCardsPerDay,
                    range = 5f..1000f,
                    onValueChange = { viewModel.updateNewCardsPerDay(it.toInt()) }
                )
                SettingsHDivider()
                SettingsSliderItem(
                    icon = Icons.Default.Speed,
                    title = "Thẻ ôn tập mỗi ngày",
                    value = uiState.reviewCardsPerDay,
                    range = 10f..5000f,
                    onValueChange = { viewModel.updateReviewCardsPerDay(it.toInt()) }
                )
                SettingsHDivider()
                SettingsToggleItem(Icons.Default.Notifications, "Nhắc nhở ôn tập", uiState.reminderEnabled) { enabled ->
                    if (enabled) {
                        // Check notification permission on Android 13+
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                            != PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.toggleReminder(true)
                        }
                    } else {
                        viewModel.toggleReminder(false)
                    }
                }
                if (uiState.reminderEnabled) {
                    SettingsHDivider()
                    SettingsItem(
                        Icons.Default.AccessTime,
                        "Giờ nhắc nhở",
                        String.format("%02d:%02d", uiState.reminderHour, uiState.reminderMinute)
                    ) {
                        TimePickerDialog(
                            context,
                            { _, h, m -> viewModel.updateReminderTime(h, m) },
                            uiState.reminderHour,
                            uiState.reminderMinute,
                            true
                        ).show()
                    }
                }
            }
        }

        // Account Section
        item {
            SectionTitle("Tài khoản")
            SettingsCard {
                SettingsItem(Icons.Default.Email, "Thay đổi email", uiState.user?.email) {
                    showEmailDialog = true
                }
            }
        }

        // About Section
        item {
            SectionTitle("Khác")
            SettingsCard {
                SettingsItem(Icons.Default.Security, "Bảo mật", null) {}
                SettingsHDivider()
                SettingsItem(Icons.AutoMirrored.Filled.HelpOutline, "Trợ giúp", null) {}
                SettingsHDivider()
                SettingsItem(Icons.Default.Info, "Phiên bản", "1.0.0") {}
            }
        }

        // Logout
        item {
            Card(
                Modifier.fillMaxWidth().clickable { viewModel.logout() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer)
            ) {
                Row(
                    Modifier.padding(20.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, null, tint = cs.error, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Đăng xuất", color = cs.error, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }
        }

        item { Spacer(Modifier.height(20.dp)) }
    }

    // ── Change Email Dialog ──
    if (showEmailDialog) {
        var newEmail by remember { mutableStateOf(uiState.user?.email ?: "") }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                showEmailDialog = false
                viewModel.clearEmailResult()
            },
            containerColor = cs.surface,
            icon = {
                Icon(Icons.Default.Email, null, tint = cs.primary, modifier = Modifier.size(28.dp))
            },
            title = {
                Text("Thay đổi email", fontWeight = FontWeight.Bold, color = cs.onSurface)
            },
            text = {
                Column {
                    androidx.compose.material3.OutlinedTextField(
                        value = newEmail,
                        onValueChange = { newEmail = it },
                        label = { Text("Email mới") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = cs.primary,
                            cursorColor = cs.primary,
                            focusedTextColor = cs.onSurface,
                            unfocusedTextColor = cs.onSurface
                        )
                    )
                    if (uiState.emailUpdateError != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(uiState.emailUpdateError!!, color = cs.error, fontSize = 13.sp)
                    }
                    if (uiState.emailUpdateResult != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(uiState.emailUpdateResult!!, color = cs.primary, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = { viewModel.updateEmail(newEmail) },
                    enabled = !uiState.isUpdatingEmail && newEmail.isNotBlank(),
                    shape = RoundedCornerShape(10.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = cs.primary)
                ) {
                    if (uiState.isUpdatingEmail) {
                        CircularProgressIndicator(
                            Modifier.size(16.dp),
                            color = cs.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Lưu", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showEmailDialog = false
                    viewModel.clearEmailResult()
                }) {
                    Text("Hủy", color = cs.onSurfaceVariant)
                }
            }
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    val cs = MaterialTheme.colorScheme
    Text(title, color = cs.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 4.dp, top = 4.dp))
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer)
    ) {
        Column(Modifier.padding(vertical = 4.dp)) { content() }
    }
}

@Composable
private fun SettingsItem(icon: ImageVector, title: String, value: String?, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = cs.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(title, color = cs.onSurface, fontSize = 15.sp, modifier = Modifier.weight(1f))
        if (value != null) {
            Text(value, color = cs.onSurfaceVariant, fontSize = 13.sp)
        }
    }
}

@Composable
private fun SettingsToggleItem(icon: ImageVector, title: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = cs.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(title, color = cs.onSurface, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedTrackColor = cs.primary)
        )
    }
}

@Composable
private fun SettingsHDivider() {
    val cs = MaterialTheme.colorScheme
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 56.dp),
        color = cs.outlineVariant.copy(alpha = 0.3f)
    )
}

@Composable
private fun SettingsSliderItem(
    icon: ImageVector,
    title: String,
    value: Int,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = cs.primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(14.dp))
            Text(title, color = cs.onSurface, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Text("$value", color = cs.primary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.padding(start = 36.dp),
            colors = SliderDefaults.colors(
                thumbColor = cs.primary,
                activeTrackColor = cs.primary
            )
        )
    }
}
