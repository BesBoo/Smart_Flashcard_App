package com.example.myapplication.presentation.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.presentation.login.ForgotPasswordScreen
import com.example.myapplication.presentation.login.ForgotPasswordViewModel
import com.example.myapplication.presentation.login.LoginScreen
import com.example.myapplication.presentation.login.LoginViewModel
import com.example.myapplication.presentation.login.RegisterScreen
import com.example.myapplication.presentation.login.RegisterViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

private const val WEB_CLIENT_ID = "541677107799-t49kq7g15csl9h5jgid7ust1jf5rvr9i.apps.googleusercontent.com"

@Composable
fun RootNavGraph() {
    val navController = rememberNavController()
    val sessionViewModel: RootSessionViewModel = hiltViewModel()

    LaunchedEffect(Unit) {
        sessionViewModel.resolveInitialRoute { dest ->
            if (dest != null) {
                navController.navigate(dest) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(route = Screen.Login.route) {
            val loginViewModel: LoginViewModel = hiltViewModel()
            val uiState by loginViewModel.uiState.collectAsState()
            val context = LocalContext.current
            val scope = rememberCoroutineScope()

            LaunchedEffect(uiState.isSuccess, uiState.openAdminFlow) {
                if (uiState.isSuccess) {
                    val route = if (uiState.openAdminFlow) "admin_flow" else "main_flow"
                    navController.navigate(route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                    loginViewModel.resetState()
                }
            }

            LoginScreen(
                isLoading = uiState.isLoading,
                errorMessage = uiState.error,
                onLoginClick = { email, password ->
                    loginViewModel.login(email, password)
                },
                onRegisterClick = {
                    navController.navigate(Screen.Register.route)
                },
                onForgotPasswordClick = {
                    navController.navigate(Screen.ForgotPassword.route)
                },
                onGoogleSignInClick = {
                    scope.launch {
                        try {
                            val credentialManager = CredentialManager.create(context)
                            val googleIdOption = GetGoogleIdOption.Builder()
                                .setFilterByAuthorizedAccounts(false)
                                .setServerClientId(WEB_CLIENT_ID)
                                .build()

                            val request = GetCredentialRequest.Builder()
                                .addCredentialOption(googleIdOption)
                                .build()

                            val result = credentialManager.getCredential(context, request)
                            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
                            val idToken = googleIdTokenCredential.idToken

                            loginViewModel.loginWithGoogle(idToken)
                        } catch (e: Exception) {
                            Log.e("GoogleSignIn", "Google sign-in failed", e)
                            // Don't show error if user cancelled
                            if (e.message?.contains("cancel", ignoreCase = true) != true) {
                                loginViewModel.loginWithGoogle("") // will trigger error in VM
                            }
                        }
                    }
                },
                onSkipLogin = {
                    // AUTH-07: Navigate to main flow without authentication
                    navController.navigate("main_flow") {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(route = Screen.Register.route) {
            val registerViewModel: RegisterViewModel = hiltViewModel()
            val uiState by registerViewModel.uiState.collectAsState()

            // Navigate when registration is successful (which auto-logs in the user)
            LaunchedEffect(uiState.isSuccess) {
                if (uiState.isSuccess) {
                    navController.navigate("main_flow") {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                    registerViewModel.resetState()
                }
            }

            RegisterScreen(
                isLoading = uiState.isLoading,
                errorMessage = uiState.error,
                onRegisterClick = { email, name, password, confirm ->
                    registerViewModel.register(email, name, password, confirm)
                },
                onLoginClick = {
                    navController.navigateUp() // go back to login screen
                }
            )
        }

        composable(route = Screen.ForgotPassword.route) {
            val forgotVm: ForgotPasswordViewModel = hiltViewModel()
            val uiState by forgotVm.uiState.collectAsState()

            ForgotPasswordScreen(
                uiState = uiState,
                onSendOtp = { email -> forgotVm.sendOtp(email) },
                onResetPassword = { otp, newPw, confirmPw ->
                    forgotVm.resetPassword(otp, newPw, confirmPw)
                },
                onBackToLogin = {
                    forgotVm.resetState()
                    navController.navigateUp()
                }
            )
        }

        composable(route = "main_flow") {
            MainScreen(
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(route = "admin_flow") {
            AdminMainScreen(
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
