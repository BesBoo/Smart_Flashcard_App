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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.AccentBlue
import com.example.myapplication.ui.theme.DarkBlue
import com.example.myapplication.ui.theme.DarkNavy
import com.example.myapplication.ui.theme.DeepBlue
import com.example.myapplication.ui.theme.ErrorRed
import com.example.myapplication.ui.theme.GradientEnd
import com.example.myapplication.ui.theme.GradientStart
import com.example.myapplication.ui.theme.InputFieldBg
import com.example.myapplication.ui.theme.InputFieldBorder
import com.example.myapplication.ui.theme.TextGrey
import com.example.myapplication.ui.theme.TextLightBlue
import com.example.myapplication.ui.theme.TextWhite

@Composable
fun RegisterScreen(
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onRegisterClick: (email: String, displayName: String, password: String, confirm: String) -> Unit = { _, _, _, _ -> },
    onLoginClick: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DarkNavy, DarkBlue, DeepBlue)
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
            Spacer(modifier = Modifier.height(60.dp))

            // ── App Header ──
            Text(
                text = "Tạo tài khoản",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )

            Text(
                text = "Bắt đầu hành trình học tập cùng Smart Flashcard",
                fontSize = 14.sp,
                color = TextGrey,
                modifier = Modifier.padding(top = 6.dp, bottom = 40.dp)
            )

            // ── Display Name Field ──
            InputFieldBlock(
                label = "Tên hiển thị",
                value = displayName,
                onValueChange = { displayName = it },
                placeholder = "Ví dụ: Nguyễn Văn A",
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            )

            // ── Email Field ──
            InputFieldBlock(
                label = "Email",
                value = email,
                onValueChange = { email = it },
                placeholder = "example@email.com",
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )

            // ── Password Field ──
            PasswordFieldBlock(
                label = "Mật khẩu (ít nhất 6 ký tự)",
                value = password,
                onValueChange = { password = it },
                passwordVisible = passwordVisible,
                onVisibilityToggle = { passwordVisible = !passwordVisible },
                imeAction = ImeAction.Next
            )

            // ── Confirm Password Field ──
            PasswordFieldBlock(
                label = "Xác nhận mật khẩu",
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                passwordVisible = passwordVisible,
                onVisibilityToggle = { passwordVisible = !passwordVisible },
                imeAction = ImeAction.Done
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Error Message ──
            if (!errorMessage.isNullOrBlank()) {
                Text(
                    text = errorMessage,
                    color = ErrorRed,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // ── Register Button ──
            Button(
                onClick = { onRegisterClick(email, displayName, password, confirmPassword) },
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
                            text = "ĐĂNG KÝ",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // ── Back to Login Link ──
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Đã có tài khoản? ",
                    fontSize = 14.sp,
                    color = TextGrey
                )
                Text(
                    text = "Đăng nhập",
                    fontSize = 14.sp,
                    color = AccentBlue,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onLoginClick() }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun InputFieldBlock(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    imeAction: ImeAction
) {
    Text(
        text = label,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = TextLightBlue,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    )

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(text = placeholder, color = TextGrey.copy(alpha = 0.5f))
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction
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
}

@Composable
private fun PasswordFieldBlock(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    passwordVisible: Boolean,
    onVisibilityToggle: () -> Unit,
    imeAction: ImeAction
) {
    Text(
        text = label,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = TextLightBlue,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    )

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(text = "Nhập mật khẩu", color = TextGrey.copy(alpha = 0.5f))
        },
        singleLine = true,
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = imeAction
        ),
        trailingIcon = {
            IconButton(onClick = onVisibilityToggle) {
                Text(
                    text = if (passwordVisible) "ẨN" else "HIỆN",
                    fontSize = 11.sp,
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

    Spacer(modifier = Modifier.height(20.dp))
}
