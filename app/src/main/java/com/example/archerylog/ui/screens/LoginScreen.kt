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
    var showForgotPasswordDialog by remember { mutableStateOf(false) }

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
            val isSuccessMessage = error == l10n.verificationSentHint || error == l10n.forgotPasswordSent
            Text(
                text = error!!,
                color = if (isSuccessMessage) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
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

        // Forgot Password button — only show in login mode
        if (!isSignUp) {
            TextButton(
                onClick = { showForgotPasswordDialog = true }
            ) {
                Text(l10n.forgotPassword, color = MaterialTheme.colorScheme.primary)
            }
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

    // Forgot Password Dialog
    if (showForgotPasswordDialog) {
        var resetEmail by remember { mutableStateOf(if (identifier.contains("@")) identifier else "") }
        var isLoading by remember { mutableStateOf(false) }
        var dialogMessage by remember { mutableStateOf<String?>(null) }
        var isSuccess by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { 
                if (!isLoading) {
                    showForgotPasswordDialog = false
                    if (isSuccess) {
                        error = l10n.forgotPasswordSent
                    }
                }
            },
            title = { Text(l10n.forgotPasswordTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = l10n.forgotPasswordDesc,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { 
                            resetEmail = it
                            dialogMessage = null
                        },
                        label = { Text(l10n.email) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading && !isSuccess
                    )
                    if (dialogMessage != null) {
                        Text(
                            text = dialogMessage!!,
                            color = if (isSuccess) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.CenterHorizontally).size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            },
            confirmButton = {
                if (!isSuccess) {
                    TextButton(
                        enabled = !isLoading && resetEmail.contains("@"),
                        onClick = {
                            isLoading = true
                            dialogMessage = null
                            scope.launch {
                                val errorMsg = viewModel.resetPassword(resetEmail)
                                if (errorMsg == null) {
                                    isSuccess = true
                                    dialogMessage = l10n.forgotPasswordSent
                                } else {
                                    dialogMessage = errorMsg
                                }
                                isLoading = false
                            }
                        }
                    ) { Text(l10n.sendResetLink) }
                } else {
                    TextButton(
                        onClick = {
                            showForgotPasswordDialog = false
                            error = l10n.forgotPasswordSent
                        }
                    ) { Text(l10n.pickerDone) }
                }
            },
            dismissButton = {
                if (!isSuccess) {
                    TextButton(
                        enabled = !isLoading,
                        onClick = { showForgotPasswordDialog = false }
                    ) { Text(l10n.cancel) }
                }
            }
        )
    }
}
