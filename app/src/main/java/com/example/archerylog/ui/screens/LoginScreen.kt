package com.example.archerylog.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.unit.dp
import com.example.archerylog.ui.ArcheryViewModel
import com.example.archerylog.ui.utils.L10n
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource

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
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isSignUp by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }

    val oauthError by viewModel.oauthError.collectAsState()
    LaunchedEffect(oauthError) {
        if (oauthError != null) {
            error = oauthError
            viewModel.clearOauthError()
        }
    }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
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
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                val image = if (isPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                val description = if (isPasswordVisible) "Hide password" else "Show password"
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(imageVector = image, contentDescription = description)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        
        if (!isSignUp) {
            TextButton(
                onClick = { showForgotPasswordDialog = true },
                modifier = Modifier.align(Alignment.End),
                contentPadding = PaddingValues(top = 4.dp, bottom = 4.dp)
            ) {
                Text(
                    text = l10n.forgotPassword,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

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
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isSignUp) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.primary,
                contentColor = Color.Black
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isSignUp) l10n.signupButton else l10n.loginButton)
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = {
                isSignUp = !isSignUp
                error = null
            },
            modifier = Modifier.align(Alignment.CenterHorizontally),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                text = if (isSignUp) l10n.hasAccount else l10n.noAccount,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                text = l10n.or,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = {
                viewModel.loginWithGoogle()
            },
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(id = com.example.archerylog.R.drawable.ic_google_logo),
                    contentDescription = "Google Logo",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = l10n.loginWithGoogle,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                text = l10n.or,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = {
                scope.launch {
                    viewModel.loginAsGuest()
                    onLoginSuccess()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(l10n.continueAsGuest)
        }
    }
        
    Text(
        text = "v${com.example.archerylog.BuildConfig.VERSION_NAME}",
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 16.dp)
    )
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
