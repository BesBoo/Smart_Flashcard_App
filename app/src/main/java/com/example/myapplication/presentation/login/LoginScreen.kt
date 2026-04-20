package com.example.myapplication.presentation.login

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.AccentBlue
import androidx.compose.material3.MaterialTheme
//import com.example.myapplication.ui.theme.DarkBlue
//import com.example.myapplication.ui.theme.DarkNavy
//import com.example.myapplication.ui.theme.DeepBlue
import com.example.myapplication.ui.theme.GradientEnd
import com.example.myapplication.ui.theme.GradientStart
import com.example.myapplication.ui.theme.InputFieldBg
import com.example.myapplication.ui.theme.InputFieldBorder
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.ui.theme.TextGrey
import com.example.myapplication.ui.theme.TextLightBlue
import com.example.myapplication.ui.theme.TextWhite
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.myapplication.R

import androidx.compose.material3.CircularProgressIndicator

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onLoginClick: (email: String, password: String) -> Unit = { _, _ -> },
    onRegisterClick: () -> Unit = {},
    onForgotPasswordClick: () -> Unit = {},
    onGoogleSignInClick: () -> Unit = {},
    onSkipLogin: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
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

            // ── App Logo ──

            Image(
                painter = painterResource(id = R.drawable.flashcard_launcher_playstore), // tên file ảnh của bạn
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(56.dp)  // nhỏ hơn Box một chút để có padding
                    .clip(CircleShape),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )


            Spacer(modifier = Modifier.height(20.dp))

            // ── App Title ──
            Text(
                text = "Smart Flashcard",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )

            Text(
                text = "Ứng dụng thẻ ghi nhớ",
                fontSize = 14.sp,
                color = TextGrey,
                modifier = Modifier.padding(top = 6.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // ── Welcome Text ──
            Text(
                text = "Đăng nhập",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextWhite,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Nhập thông tin tài khoản của bạn",
                fontSize = 14.sp,
                color = TextGrey,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ── Email Field ──
            Text(
                text = "Email",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextLightBlue,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = {
                    Text(
                        text = "example@email.com",
                        color = TextGrey.copy(alpha = 0.5f)
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
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

            Spacer(modifier = Modifier.height(20.dp))

            // ── Password Field ──
            Text(
                text = "Mật khẩu",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextLightBlue,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = {
                    Text(
                        text = "Nhập mật khẩu",
                        color = TextGrey.copy(alpha = 0.5f)
                    )
                },
                singleLine = true,
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Text(
                            text = if (passwordVisible) "\uD83D\uDE48" else "\uD83D\uDC41\uFE0F",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = GradientEnd
                        )
                    }
                },
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

            // ── Forgot Password ──
            Text(
                text = "Quên mật khẩu ?",
                fontSize = 13.sp,
                color = AccentBlue,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 12.dp)
                    .clickable { onForgotPasswordClick() }
            )

            Spacer(modifier = Modifier.height(24.dp))
            
            // ── Error Message ──
            if (!errorMessage.isNullOrBlank()) {
                Text(
                    text = errorMessage,
                    color = com.example.myapplication.ui.theme.ErrorRed,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // ── Login Button (Gradient) ──
            Button(
                onClick = { onLoginClick(email, password) },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent
                ),
                contentPadding = ButtonDefaults.TextButtonContentPadding
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF56C9C9), Color(0xFF3BA8A8))
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text = "ĐĂNG NHẬP",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Divider ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = InputFieldBorder
                )
                Text(
                    text = "  HOẶC  ",
                    fontSize = 12.sp,
                    color = TextGrey,
                    fontWeight = FontWeight.Medium
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = InputFieldBorder
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Google Sign-In Button ──
            OutlinedButton(
                onClick = { onGoogleSignInClick() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, InputFieldBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = InputFieldBg
                )
            ) {
                Text(
                    text = "G",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFDB4437)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Đăng nhập với Google",
                    fontSize = 14.sp,
                    color = TextWhite,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // ── Register Link ──
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Chưa có tài khoản ? ",
                    fontSize = 14.sp,
                    color = TextGrey
                )
                Text(
                    text = "Đăng ký",
                    fontSize = 14.sp,
                    color = AccentBlue,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onRegisterClick() }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Skip Login (Offline mode – AUTH-07) ──
            Text(
                text = "Dùng offline (không đăng nhập)",
                fontSize = 13.sp,
                color = TextGrey,
                modifier = Modifier.clickable { onSkipLogin() }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Preview(
    showBackground = true,
    device = "spec:width=411dp,height=891dp",
    backgroundColor = 0xFF000000
)
@Composable
fun LoginScreenPreview() {
    MyApplicationTheme {
        LoginScreen()
    }
}
