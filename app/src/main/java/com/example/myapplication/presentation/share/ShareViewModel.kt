package com.example.myapplication.presentation.share

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.remote.dto.DeckPreviewResponse
import com.example.myapplication.data.remote.dto.JoinDeckResponse
import com.example.myapplication.data.remote.dto.ShareInfoResponse
import com.example.myapplication.data.remote.dto.SubscriberInfo
import com.example.myapplication.data.repository.ShareRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShareUiState(
    val isLoading: Boolean = false,
    val shareInfo: ShareInfoResponse? = null,
    val preview: DeckPreviewResponse? = null,
    val joinResult: JoinDeckResponse? = null,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class ShareViewModel @Inject constructor(
    private val shareRepository: ShareRepositoryImpl,
    private val deckRepository: com.example.myapplication.domain.repository.DeckRepository,
    private val userRepository: com.example.myapplication.domain.repository.UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShareUiState())
    val uiState: StateFlow<ShareUiState> = _uiState.asStateFlow()

    // ── Owner: Create share link ──

    fun createShare(deckId: String, permission: String = "read") {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val result = shareRepository.createShare(deckId, permission)
                _uiState.update { it.copy(isLoading = false, shareInfo = result) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }

    // ── Owner: Load existing share info ──

    fun loadShareInfo(deckId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val result = shareRepository.getShareInfo(deckId)
                _uiState.update { it.copy(isLoading = false, shareInfo = result) }
            } catch (e: Exception) {
                // Not shared yet — not an error, just no share info
                _uiState.update { it.copy(isLoading = false, shareInfo = null) }
            }
        }
    }

    // ── Owner: Stop sharing ──

    fun stopSharing(deckId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                shareRepository.stopSharing(deckId)
                _uiState.update { it.copy(isLoading = false, shareInfo = null, successMessage = "Đã hủy chia sẻ") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }

    // ── Owner: Update subscriber permission ──

    fun updatePermission(deckId: String, userId: String, permission: String) {
        viewModelScope.launch {
            try {
                shareRepository.updateSubscriberPermission(deckId, userId, permission)
                loadShareInfo(deckId) // Refresh
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    // ── Owner: Kick subscriber ──

    fun kickSubscriber(deckId: String, userId: String) {
        viewModelScope.launch {
            try {
                shareRepository.kickSubscriber(deckId, userId)
                loadShareInfo(deckId) // Refresh
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    // ── User: Preview deck by code ──

    fun previewDeck(code: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, preview = null) }
            try {
                val result = shareRepository.previewDeck(code)
                _uiState.update { it.copy(isLoading = false, preview = result) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Mã không hợp lệ hoặc đã hết hạn") }
            }
        }
    }

    // ── User: Join deck by code ──

    fun joinDeck(code: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val result = shareRepository.joinDeck(code)
                val userId = userRepository.getCurrentUserId()
                if (userId != null) {
                    // Immediately insert the joined deck so it appears in UI right away
                    val joinedDeck = com.example.myapplication.domain.model.Deck(
                        id = result.deckId,
                        userId = userId,
                        name = result.deckName,
                        description = result.description,
                        isOwner = false,
                        permission = result.permission,
                        ownerName = result.ownerName,
                        cardCount = result.cardCount,
                        isShared = true
                    )
                    deckRepository.saveDeckLocally(joinedDeck)
                    android.util.Log.d("ShareVM", "joinDeck: saved deck ${result.deckId} to local DB")

                    // Full sync in background to get cards etc.
                    try {
                        deckRepository.syncDecks(userId)
                        android.util.Log.d("ShareVM", "joinDeck: full sync completed")
                    } catch (syncEx: Exception) {
                        android.util.Log.e("ShareVM", "joinDeck: syncDecks failed", syncEx)
                    }
                }
                _uiState.update { it.copy(isLoading = false, joinResult = result, successMessage = "Đã tham gia bộ thẻ \"${result.deckName}\"!") }
            } catch (e: retrofit2.HttpException) {
                val msg = if (e.code() == 409) "Bạn đã tham gia bộ thẻ này rồi"
                          else "HTTP ${e.code()}"
                _uiState.update { it.copy(isLoading = false, error = msg) }
            } catch (e: Exception) {
                val msg = when {
                    e.message?.contains("409") == true -> "Bạn đã tham gia bộ thẻ này rồi"
                    else -> e.localizedMessage ?: "Không thể tham gia"
                }
                _uiState.update { it.copy(isLoading = false, error = msg) }
            }
        }
    }

    // ── User: Leave shared deck ──

    fun leaveDeck(deckId: String) {
        viewModelScope.launch {
            try {
                shareRepository.leaveDeck(deckId)
                _uiState.update { it.copy(successMessage = "Đã rời khỏi bộ thẻ") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }

    fun resetState() {
        _uiState.value = ShareUiState()
    }
}
