package com.example.myapplication.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.domain.model.UserRole
import com.example.myapplication.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RootSessionViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    /**
     * If already logged in, where to go: `admin_flow`, `main_flow`, or null if stay on login.
     */
    fun resolveInitialRoute(onResolved: (String?) -> Unit) {
        viewModelScope.launch {
            if (!userRepository.isLoggedIn()) {
                onResolved(null)
                return@launch
            }
            val dest = when (userRepository.getCurrentUserRole()) {
                UserRole.ADMIN -> "admin_flow"
                UserRole.USER -> "main_flow"
            }
            onResolved(dest)
        }
    }
}
