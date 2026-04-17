//DecksViewModel.kt
package com.example.myapplication.presentation.decks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.domain.model.Deck
import com.example.myapplication.domain.repository.DeckRepository
import com.example.myapplication.domain.repository.UserRepository
import com.example.myapplication.data.remote.api.FlashcardApi
import com.example.myapplication.data.remote.api.ShareApi
import com.example.myapplication.data.remote.dto.ViolationNotice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class DecksUiState(
    val isLoading: Boolean = true,
    val decks: List<Deck> = emptyList(),
    val error: String? = null,
    val violations: List<ViolationNotice> = emptyList(),
    val showViolationDialog: Boolean = false
)

@HiltViewModel
class DecksViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val deckRepository: DeckRepository,
    private val flashcardApi: FlashcardApi,
    private val shareApi: ShareApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(DecksUiState())
    val uiState: StateFlow<DecksUiState> = _uiState.asStateFlow()

    init {
        loadDecks()
        checkViolations()
    }

    private fun loadDecks() {
        viewModelScope.launch {
            val userId = userRepository.getCurrentUserId()
            if (userId != null) {
                deckRepository.syncDecks(userId)
                deckRepository.observeDecksByUser(userId)
                    .catch { e ->
                        _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
                    }
                    .collect { decksList ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                decks = decksList,
                                error = null
                            )
                        }
                    }
            } else {
                 _uiState.update { it.copy(isLoading = false, error = "User not logged in") }
            }
        }
    }

    /** Re-sync decks from server (called when screen becomes visible) */
    fun refresh() {
        viewModelScope.launch {
            val userId = userRepository.getCurrentUserId() ?: return@launch
            try {
                deckRepository.syncDecks(userId)
            } catch (_: Exception) {
                // Offline — keep local data
            }
        }
    }

    private fun checkViolations() {
        viewModelScope.launch {
            try {
                val violations = flashcardApi.getViolations()
                if (violations.isNotEmpty()) {
                    _uiState.update { it.copy(violations = violations, showViolationDialog = true) }
                }
            } catch (_: Exception) {
                // Offline or error — skip silently
            }
        }
    }

    fun dismissViolationDialog() {
        _uiState.update { it.copy(showViolationDialog = false) }
    }

    fun createDeck(name: String, description: String, coverImageUrl: String? = null) {
        viewModelScope.launch {
            val userId = userRepository.getCurrentUserId() ?: return@launch
            try {
                deckRepository.createDeck(
                    Deck(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        name = name,
                        description = description.ifBlank { null },
                        coverImageUrl = coverImageUrl
                    )
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun deleteDeck(deckId: String) {
        viewModelScope.launch {
            try {
                deckRepository.deleteDeck(deckId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun renameDeck(deckId: String, newName: String) {
        viewModelScope.launch {
            try {
                val deck = deckRepository.getDeckById(deckId) ?: return@launch
                deckRepository.updateDeck(deck.copy(name = newName))
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun leaveDeck(deckId: String) {
        viewModelScope.launch {
            try {
                shareApi.leaveDeck(deckId)
                // Remove from local Room DB
                deckRepository.deleteDeck(deckId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }
}
