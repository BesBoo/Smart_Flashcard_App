package com.example.myapplication.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.remote.dto.AdminUserDto
import com.example.myapplication.domain.repository.AdminRepository
import com.example.myapplication.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminUsersUiState(
    val isLoading: Boolean = true,
    val users: List<AdminUserDto> = emptyList(),
    val searchQuery: String = "",
    val error: String? = null,
    val actionMessage: String? = null,
    val selfDemoted: Boolean = false
)

@HiltViewModel
class AdminUsersViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUsersUiState())
    val uiState: StateFlow<AdminUsersUiState> = _uiState.asStateFlow()

    init { loadUsers() }

    fun refresh() { loadUsers(_uiState.value.searchQuery.ifBlank { null }) }

    fun search(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        loadUsers(query)
    }

    fun loadUsers(search: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val users = adminRepository.fetchUsers(search = search)
                _uiState.update { it.copy(isLoading = false, users = users) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage ?: "Lỗi tải danh sách") }
            }
        }
    }

    fun banUser(userId: String, ban: Boolean) {
        viewModelScope.launch {
            try {
                adminRepository.banUser(userId, ban)
                _uiState.update { it.copy(actionMessage = if (ban) "Đã khóa tài khoản" else "Đã mở khóa") }
                loadUsers(_uiState.value.searchQuery.ifBlank { null })
            } catch (e: Exception) {
                _uiState.update { it.copy(actionMessage = "Lỗi: ${e.localizedMessage}") }
            }
        }
    }

    fun changeRole(userId: String, newRole: String) {
        viewModelScope.launch {
            try {
                adminRepository.changeRole(userId, newRole)

                // Detect self-demotion: admin removing own admin role
                val currentUserId = userRepository.getCurrentUserId()
                if (userId == currentUserId && newRole.lowercase() != "admin") {
                    _uiState.update {
                        it.copy(
                            actionMessage = "Bạn đã hạ quyền của chính mình. Đang đăng xuất...",
                            selfDemoted = true
                        )
                    }
                    // Logout so the stale admin token is discarded
                    userRepository.logout()
                    return@launch
                }

                _uiState.update { it.copy(actionMessage = "Đã đổi quyền thành $newRole") }
                loadUsers(_uiState.value.searchQuery.ifBlank { null })
            } catch (e: Exception) {
                _uiState.update { it.copy(actionMessage = "Lỗi: ${e.localizedMessage}") }
            }
        }
    }

    fun clearActionMessage() {
        _uiState.update { it.copy(actionMessage = null) }
    }
}
