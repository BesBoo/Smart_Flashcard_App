package com.example.myapplication.presentation.login

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.*

@Composable
fun ForgotPasswordScreen(
    modifier: Modifier = Modifier,
    uiState: ForgotPasswordUiState,
    onSendOtp: (email: String) -> Unit = {},
    onResetPassword: (otp: String, newPassword: String, confirmPassword: String) -> Unit = { _, _, _ -> },
    onBackToLogin: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E293B),
                        Color(0xFF334155)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            // ── Icon ──
            Text(
                text = "",
                fontSize = 48.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // ── Title ──
            Text(
                text = if (!uiState.otpSent) "Quên mật khẩu" else "Xác nhận OTP",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )

            Text(
                text = if (!uiState.otpSent)
                    "Nhập email đã đăng ký để nhận mã OTP"
                else
                    "Nhập mã OTP đã gửi tới ${uiState.email}",
                fontSize = 14.sp,
                color = TextGrey,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
            )

            // ── Error Message ──
            if (!uiState.error.isNullOrBlank()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0x33FF4444)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Text(
                        text = uiState.error,
                        color = ErrorRed,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // ── Success Message ──
            if (!uiState.successMessage.isNullOrBlank() && !uiState.isResetComplete) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0x3344BB44)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Text(
                        text = uiState.successMessage,
                        color = Color(0xFF4CAF50),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (!uiState.otpSent) {
                // ══════════════════════════════════════
                //  STEP 1: Enter Email
                // ══════════════════════════════════════
                Text(
                    text = "Email",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextLightBlue,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text("example@email.com", color = TextGrey.copy(alpha = 0.5f)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done
                    ),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        cursorColor = GradientEnd,
                        focusedBorderColor = GradientEnd,
                        unfocusedBorderColor = InputFieldBorder,
                        focusedContainerColor = InputFieldBg,
                        unfocusedContainerColor = InputFieldBg
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // ── Send OTP Button ──
                Button(
                    onClick = { onSendOtp(email) },
                    enabled = !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent),
                    contentPadding = ButtonDefaults.TextButtonContentPadding
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(colors = listOf(Color(0xFF56C9C9), Color(0xFF3BA8A8))),
                                shape = RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("GỬI MÃ OTP", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 1.sp)
                        }
                    }
                }
            } else if (!uiState.isResetComplete) {
                // ══════════════════════════════════════
                //  STEP 2: Enter OTP + New Password
                // ══════════════════════════════════════
                Text(
                    text = "Mã OTP",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextLightBlue,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = otp,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) otp = it },
                    placeholder = { Text("Nhập 6 chữ số", color = TextGrey.copy(alpha = 0.5f)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite, unfocusedTextColor = TextWhite,
                        cursorColor = GradientEnd, focusedBorderColor = GradientEnd,
                        unfocusedBorderColor = InputFieldBorder,
                        focusedContainerColor = InputFieldBg, unfocusedContainerColor = InputFieldBg
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Mật khẩu mới",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextLightBlue,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    placeholder = { Text("Ít nhất 6 ký tự", color = TextGrey.copy(alpha = 0.5f)) },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Text(
                                text = if (passwordVisible) "\uD83D\uDE48" else "\uD83D\uDC41\uFE0F",
                                fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GradientEnd
                            )
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite, unfocusedTextColor = TextWhite,
                        cursorColor = GradientEnd, focusedBorderColor = GradientEnd,
                        unfocusedBorderColor = InputFieldBorder,
                        focusedContainerColor = InputFieldBg, unfocusedContainerColor = InputFieldBg
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Xác nhận mật khẩu",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextLightBlue,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    placeholder = { Text("Nhập lại mật khẩu mới", color = TextGrey.copy(alpha = 0.5f)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite, unfocusedTextColor = TextWhite,
                        cursorColor = GradientEnd, focusedBorderColor = GradientEnd,
                        unfocusedBorderColor = InputFieldBorder,
                        focusedContainerColor = InputFieldBg, unfocusedContainerColor = InputFieldBg
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // ── Reset Password Button ──
                Button(
                    onClick = { onResetPassword(otp, newPassword, confirmPassword) },
                    enabled = !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent),
                    contentPadding = ButtonDefaults.TextButtonContentPadding
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(colors = listOf(Color(0xFF56C9C9), Color(0xFF3BA8A8))),
                                shape = RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("ĐẶT LẠI MẬT KHẨU", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 1.sp)
                        }
                    }
                }
            } else {
                // ══════════════════════════════════════
                //  STEP 3: Success
                // ══════════════════════════════════════
                Text("✅", fontSize = 48.sp, modifier = Modifier.padding(bottom = 16.dp))

                Text(
                    text = "Đặt lại mật khẩu thành công!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF4CAF50)
                )

                Text(
                    text = "Bạn có thể đăng nhập với mật khẩu mới.",
                    fontSize = 14.sp,
                    color = TextGrey,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
                )

                Button(
                    onClick = { onBackToLogin() },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = ButtonDefaults.TextButtonContentPadding
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(colors = listOf(Color(0xFF56C9C9), Color(0xFF3BA8A8))),
                                shape = RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("QUAY LẠI ĐĂNG NHẬP", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 1.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Back to Login ──
            if (!uiState.isResetComplete) {
                Text(
                    text = "← Quay lại đăng nhập",
                    fontSize = 14.sp,
                    color = AccentBlue,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onBackToLogin() }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
