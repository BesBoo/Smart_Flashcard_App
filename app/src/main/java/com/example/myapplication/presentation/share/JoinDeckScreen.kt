package com.example.myapplication.presentation.share

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myapplication.ui.theme.GradientEnd
import com.example.myapplication.ui.theme.GradientStart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinDeckScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: ShareViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val cs = MaterialTheme.colorScheme
    var code by remember { mutableStateOf("") }

    Scaffold(
        containerColor = cs.background,
        topBar = {
            TopAppBar(
                title = { Text("Nhập mã chia sẻ", fontWeight = FontWeight.Bold, color = cs.onSurface) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại", tint = cs.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = cs.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            // Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        brush = Brush.linearGradient(listOf(GradientStart, GradientEnd)),
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Group, null, tint = Color.White, modifier = Modifier.size(40.dp))
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "Nhập mã 6 ký tự để tham gia bộ thẻ được chia sẻ",
                color = cs.onSurfaceVariant, fontSize = 14.sp, textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            // Code input
            OutlinedTextField(
                value = code,
                onValueChange = {
                    if (it.length <= 6) {
                        code = it.uppercase().filter { c -> c.isLetterOrDigit() }
                    }
                },
                placeholder = { Text("VD: ABC123", color = cs.onSurfaceVariant.copy(alpha = 0.5f), fontFamily = FontFamily.Monospace) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 6.sp,
                    textAlign = TextAlign.Center,
                    color = cs.onSurface
                ),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = cs.primary,
                    unfocusedBorderColor = cs.outlineVariant,
                    cursorColor = cs.primary
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = { if (code.length == 6) viewModel.previewDeck(code) }
                )
            )

            Spacer(Modifier.height(16.dp))

            // Preview button
            Button(
                onClick = { viewModel.previewDeck(code) },
                enabled = code.length == 6 && !uiState.isLoading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = cs.primary)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = cs.onPrimary, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Tìm bộ thẻ", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            // Error
            uiState.error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = cs.error, fontSize = 13.sp, textAlign = TextAlign.Center)
            }

            // Preview result
            uiState.preview?.let { preview ->
                Spacer(Modifier.height(24.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Style, null, tint = cs.primary, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(preview.deckName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = cs.onSurface)
                        }

                        if (!preview.description.isNullOrBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(preview.description, fontSize = 13.sp, color = cs.onSurfaceVariant)
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(preview.ownerName, fontSize = 13.sp, color = cs.onSurfaceVariant)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.School, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("${preview.cardCount} thẻ", fontSize = 13.sp, color = cs.onSurfaceVariant)
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = { viewModel.joinDeck(code) },
                            enabled = !uiState.isLoading,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = cs.primary)
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(color = cs.onPrimary, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Tham gia bộ thẻ", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Success message
            uiState.joinResult?.let {
                Spacer(Modifier.height(20.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20).copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Đã tham gia thành công!", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF4CAF50))
                            Text("Bộ thẻ \"${it.deckName}\" đã được thêm vào thư viện.", fontSize = 12.sp, color = cs.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
