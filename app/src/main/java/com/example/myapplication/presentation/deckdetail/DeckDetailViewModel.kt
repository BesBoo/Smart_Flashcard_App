//DeckDetailViewModel.kt
package com.example.myapplication.presentation.deckdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.domain.model.Deck
import com.example.myapplication.domain.model.Flashcard
import com.example.myapplication.domain.repository.DeckRepository
import com.example.myapplication.domain.repository.FlashcardRepository
import com.example.myapplication.domain.repository.UserRepository
import com.example.myapplication.domain.repository.AdminRepository
import com.example.myapplication.data.remote.api.FlashcardApi
import com.example.myapplication.data.remote.dto.LinkGoogleSheetRequest
import com.example.myapplication.util.ExcelImportHelper
import android.content.Context
import android.net.Uri
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeckDetailUiState(
    val isLoading: Boolean = true,
    val deck: Deck? = null,
    val flashcards: List<Flashcard> = emptyList(),
    val dueCards: List<Flashcard> = emptyList(),
    val searchQuery: String = "",
    val error: String? = null,
    val isImporting: Boolean = false,
    val importResult: String? = null,
    // Google Sheet sync
    val sheetUrl: String = "",
    val isSheetLinked: Boolean = false,
    val isSyncing: Boolean = false,
    val syncResult: String? = null,
    val showSheetDialog: Boolean = false
) {
    val totalCards: Int get() = flashcards.size
    val dueCount: Int get() = dueCards.size
    val newCount: Int get() = flashcards.count { it.isNew }
    val masteredCount: Int get() = flashcards.count { it.sm2.repetition >= 3 && it.sm2.easeFactor >= 2.5 }
    val filteredFlashcards: List<Flashcard> get() {
        if (searchQuery.isBlank()) return flashcards
        return flashcards.filter {
            it.frontText.contains(searchQuery, ignoreCase = true) ||
            it.backText.contains(searchQuery, ignoreCase = true)
        }
    }
}

@HiltViewModel
class DeckDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val deckRepository: DeckRepository,
    private val flashcardRepository: FlashcardRepository,
    private val userRepository: UserRepository,
    private val adminRepository: AdminRepository,
    private val flashcardApi: FlashcardApi,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val deckId: String = savedStateHandle["deckId"] ?: ""

    private val _uiState = MutableStateFlow(DeckDetailUiState())
    val uiState: StateFlow<DeckDetailUiState> = _uiState.asStateFlow()

    init {
        loadDeckDetail()
        refreshFromServer()
    }

    /** Sync flashcards from server so edits by shared-deck members are visible */
    fun refreshFromServer() {
        viewModelScope.launch {
            try {
                val userId = userRepository.getCurrentUserId() ?: return@launch
                deckRepository.syncFlashcardsForDeck(deckId, userId)
            } catch (_: Exception) {
                // Offline — ignore
            }
        }
    }

    private fun loadDeckDetail() {
        viewModelScope.launch {
            combine(
                deckRepository.observeDeckById(deckId),
                flashcardRepository.observeFlashcardsByDeck(deckId)
            ) { deck, cards ->
                Triple(deck, cards, cards.filter { it.isDue })
            }
            .catch { e ->
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
            .collect { (deck, cards, dueCards) ->
                _uiState.update { current ->
                    // On first load (isLoading=true), initialize sheet state from Room
                    // Otherwise preserve the sheet state set by link/unlink/sync actions
                    val sheetLinked = if (current.isLoading) {
                        !deck?.googleSheetUrl.isNullOrBlank()
                    } else {
                        current.isSheetLinked
                    }
                    val sheetUrl = if (current.isLoading) {
                        deck?.googleSheetUrl ?: ""
                    } else {
                        current.sheetUrl
                    }

                    current.copy(
                        isLoading = false,
                        deck = deck,
                        flashcards = cards,
                        dueCards = dueCards,
                        isSheetLinked = sheetLinked,
                        sheetUrl = sheetUrl
                    )
                }
            }
        }
    }

    fun deleteFlashcard(cardId: String) {
        viewModelScope.launch {
            try {
                flashcardRepository.deleteFlashcard(cardId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun updateSearch(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun deleteDeck() {
        viewModelScope.launch {
            try {
                deckRepository.deleteDeck(deckId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    /** Import flashcards from an Excel file */
    fun importCardsFromExcel(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, importResult = null, error = null) }
            try {
                val userId = userRepository.getCurrentUserId()
                if (userId == null) {
                    _uiState.update { it.copy(isImporting = false, error = "Bạn chưa đăng nhập") }
                    return@launch
                }

                val result = ExcelImportHelper.parseExcelFile(context, uri)

                if (result.cards.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            importResult = "Không tìm thấy dữ liệu hợp lệ trong file Excel"
                        )
                    }
                    return@launch
                }

                // Create flashcards in batch
                var createdCount = 0
                for (row in result.cards) {
                    try {
                        flashcardRepository.createFlashcard(
                            Flashcard(
                                id = java.util.UUID.randomUUID().toString(),
                                userId = userId,
                                deckId = deckId,
                                frontText = row.frontText,
                                backText = row.backText,
                                exampleText = row.exampleText
                            )
                        )
                        createdCount++
                    } catch (_: Exception) {
                        // Skip failed cards
                    }
                }

                _uiState.update {
                    it.copy(
                        isImporting = false,
                        importResult = "✅ Đã nhập $createdCount/${result.cards.size} thẻ" +
                            if (result.skippedRows > 0) " (bỏ qua ${result.skippedRows} dòng trống)" else ""
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        error = "Lỗi đọc file Excel: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun clearImportResult() {
        _uiState.update { it.copy(importResult = null) }
    }

    // ══════════════════════════════════════════════════════════
    //  GOOGLE SHEET SYNC
    // ══════════════════════════════════════════════════════════

    fun showLinkSheetDialog() {
        _uiState.update { it.copy(showSheetDialog = true) }
    }

    fun dismissSheetDialog() {
        _uiState.update { it.copy(showSheetDialog = false) }
    }

    fun updateSheetUrl(url: String) {
        _uiState.update { it.copy(sheetUrl = url) }
    }

    /** Link a Google Sheet URL to this deck */
    fun linkGoogleSheet() {
        val url = _uiState.value.sheetUrl.trim()
        if (url.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, error = null) }
            try {
                flashcardApi.linkGoogleSheet(deckId, LinkGoogleSheetRequest(url))
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        isSheetLinked = true,
                        showSheetDialog = false,
                        syncResult = "✅ Đã liên kết Google Sheet thành công!"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSyncing = false, error = "Lỗi liên kết: ${e.localizedMessage}")
                }
            }
        }
    }

    /** Unlink Google Sheet from this deck */
    fun unlinkGoogleSheet() {
        viewModelScope.launch {
            try {
                flashcardApi.unlinkGoogleSheet(deckId)
                _uiState.update {
                    it.copy(
                        isSheetLinked = false,
                        sheetUrl = "",
                        syncResult = "Đã hủy liên kết Google Sheet"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    /** Sync cards from linked Google Sheet */
    fun syncGoogleSheet() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, syncResult = null, error = null) }
            try {
                val result = flashcardApi.syncGoogleSheet(deckId)
                // Refresh local data
                val userId = userRepository.getCurrentUserId()
                if (userId != null) {
                    deckRepository.syncFlashcardsForDeck(deckId, userId)
                }
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        syncResult = "✅ Thêm ${result.added} thẻ mới" +
                            if (result.skipped > 0) ", cập nhật ${result.skipped} thẻ" else ""
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        error = "Lỗi đồng bộ: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun clearSyncResult() {
        _uiState.update { it.copy(syncResult = null) }
    }

    fun submitReport(deckId: String, reason: String) {
        viewModelScope.launch {
            try {
                adminRepository.submitReport("Deck", deckId, reason)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Lỗi gửi báo cáo: ${e.localizedMessage}") }
            }
        }
    }
}
