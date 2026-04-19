package com.example.archerylog.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.archerylog.ui.ArcheryViewModel
import com.example.archerylog.ui.utils.L10n
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color

@Composable
fun LoginScreen(
    viewModel: ArcheryViewModel,
    onLoginSuccess: () -> Unit
) {
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val l10n = L10n(currentLanguage)
    val scope = rememberCoroutineScope()

    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Archery Log",
            style = MaterialTheme.typography.displayMedium,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = identifier,
            onValueChange = { identifier = it },
            label = { Text(if (isSignUp) l10n.email else l10n.identifier) },
            keyboardOptions = KeyboardOptions(keyboardType = if (isSignUp) KeyboardType.Email else KeyboardType.Text),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(l10n.password) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        if (error != null) {
            val isVerificationError = error == l10n.emailNotVerifiedError || error == l10n.verificationSentHint
            Text(
                text = error!!,
                color = if (error == l10n.verificationSentHint) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )

            if (isVerificationError && !isSignUp) {
                var resendLoading by remember { mutableStateOf(false) }
                TextButton(
                    onClick = {
                        scope.launch {
                            resendLoading = true
                            // If we login by username, we might need the email to resend.
                            // But for simplicity, we assume identifier is email if they are resending.
                            // Supabase resend requires email.
                            val msg = viewModel.resendVerificationEmail(identifier)
                            error = msg
                            resendLoading = false
                        }
                    },
                    enabled = !resendLoading && identifier.contains("@")
                ) {
                    Text(
                        text = if (resendLoading) "..." else l10n.resendEmailButton,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                if (!identifier.contains("@")) {
                    Text(
                        text = "(请使用注册邮箱以启用重发功能)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                scope.launch {
                    val errorMsg = if (isSignUp) {
                        viewModel.signup(identifier, password)
                    } else {
                        viewModel.login(identifier, password)
                    }
                    
                    if (errorMsg == null) {
                        onLoginSuccess()
                    } else {
                        error = errorMsg
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isSignUp) l10n.signupButton else l10n.loginButton)
        }

        TextButton(
            onClick = { 
                isSignUp = !isSignUp
                error = null
            }
        ) {
            Text(if (isSignUp) l10n.hasAccount else l10n.noAccount)
        }
    }
}
