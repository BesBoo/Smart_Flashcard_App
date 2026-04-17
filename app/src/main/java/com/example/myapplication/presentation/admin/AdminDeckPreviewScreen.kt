package com.example.myapplication.presentation.admin

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.remote.dto.AdminCardPreview
import com.example.myapplication.data.remote.dto.AdminDeckPreviewResponse
import com.example.myapplication.domain.repository.AdminRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── ViewModel ──

data class AdminDeckPreviewUiState(
    val isLoading: Boolean = true,
    val preview: AdminDeckPreviewResponse? = null,
    val error: String? = null
)

@HiltViewModel
class AdminDeckPreviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val deckId: String = savedStateHandle["deckId"] ?: ""

    private val _uiState = MutableStateFlow(AdminDeckPreviewUiState())
    val uiState: StateFlow<AdminDeckPreviewUiState> = _uiState.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val preview = adminRepository.fetchDeckPreview(deckId)
                _uiState.update { it.copy(isLoading = false, preview = preview) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage ?: "Lỗi tải deck") }
            }
        }
    }
}

// ── Screen ──

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDeckPreviewScreen(
    onNavigateBack: () -> Unit,
    viewModel: AdminDeckPreviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val cs = MaterialTheme.colorScheme

    Scaffold(
        containerColor = cs.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        uiState.preview?.deckName ?: "Xem nội dung deck",
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = cs.background)
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = cs.primary)
                }
            }
            uiState.error != null -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(uiState.error!!, color = cs.error, fontSize = 14.sp)
                }
            }
            uiState.preview != null -> {
                val preview = uiState.preview!!
                LazyColumn(
                    Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ── Deck Info Header ──
                    item {
                        Spacer(Modifier.height(4.dp))
                        Card(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    preview.deckName,
                                    color = cs.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                                if (!preview.description.isNullOrBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(preview.description, color = cs.onSurfaceVariant, fontSize = 13.sp)
                                }
                                Spacer(Modifier.height(12.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Person, null, tint = cs.primary, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Chủ sở hữu: ${preview.ownerEmail}", color = cs.onSurfaceVariant, fontSize = 13.sp)
                                }
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Style, null, tint = cs.tertiary, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("${preview.totalCards} thẻ", color = cs.onSurfaceVariant, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    // ── Section Title ──
                    item {
                        Text("Danh sách thẻ", color = cs.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }

                    // ── Card List ──
                    if (preview.cards.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(vertical = 30.dp), contentAlignment = Alignment.Center) {
                                Text("Deck này không có thẻ nào.", color = cs.onSurfaceVariant, fontSize = 14.sp)
                            }
                        }
                    } else {
                        itemsIndexed(preview.cards) { index, card ->
                            PreviewCardRow(index = index + 1, card = card)
                        }
                    }

                    item { Spacer(Modifier.height(20.dp)) }
                }
            }
        }
    }
}

@Composable
private fun PreviewCardRow(index: Int, card: AdminCardPreview) {
    val cs = MaterialTheme.colorScheme

    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer)
    ) {
        Row(Modifier.padding(14.dp)) {
            // Number badge
            Box(
                Modifier.size(32.dp).clip(CircleShape)
                    .background(cs.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text("$index", color = cs.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    card.frontText,
                    color = cs.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    card.backText,
                    color = cs.onSurfaceVariant,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!card.exampleText.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "→ ${card.exampleText}",
                        color = cs.tertiary,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
