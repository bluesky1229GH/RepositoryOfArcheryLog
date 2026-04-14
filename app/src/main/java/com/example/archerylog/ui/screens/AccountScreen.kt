package com.example.archerylog.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import com.example.archerylog.ui.ArcheryViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.lazy.items
import com.example.archerylog.utils.ImageUtils
import com.example.archerylog.ui.components.ProfileAvatar
import com.example.archerylog.ui.utils.L10n
import com.example.archerylog.ui.utils.AppLanguage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    viewModel: ArcheryViewModel,
    onLogout: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val l10n = L10n(currentLanguage)
    
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showEmailDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val localPath = ImageUtils.saveImageToInternalStorage(context, uri)
            if (localPath.isNotEmpty()) {
                viewModel.updateAvatarUri(localPath)
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            val localPath = ImageUtils.saveBitmapToInternalStorage(context, bitmap)
            if (localPath.isNotEmpty()) {
                viewModel.updateAvatarUri(localPath)
            }
        }
    }
    
    Scaffold(
        topBar = { TopAppBar(title = { Text(l10n.accountTitle) }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (currentUser != null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(32.dp))
                    ProfileAvatar(uri = currentUser!!.avatarUri, size = 100.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        TextButton(onClick = { cameraLauncher.launch(null) }) {
                            Text(l10n.takePhoto)
                        }
                        TextButton(onClick = { 
                            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) 
                        }) {
                            Text(l10n.chooseGallery)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(l10n.username, style = MaterialTheme.typography.labelMedium)
                        Text(currentUser!!.username, style = MaterialTheme.typography.bodyLarge)
                        
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(l10n.email, style = MaterialTheme.typography.labelMedium)
                                Text(if (currentUser!!.email.isEmpty()) l10n.notSet else currentUser!!.email, style = MaterialTheme.typography.bodyLarge)
                            }
                            IconButton(onClick = { showEmailDialog = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Email")
                            }
                        }
                        
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(l10n.password, style = MaterialTheme.typography.labelMedium)
                                Text("********", style = MaterialTheme.typography.bodyLarge)
                            }
                            IconButton(onClick = { showPasswordDialog = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Password")
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Language Settings
                Text(l10n.settingsLanguage, style = MaterialTheme.typography.titleMedium)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        AppLanguage.values().forEach { lang ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = currentLanguage == lang,
                                    onClick = { viewModel.setLanguage(lang) }
                                )
                                Text(
                                    text = lang.label,
                                    modifier = Modifier.padding(start = 16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // AI Favorites Section
                var showFavorites by remember { mutableStateOf(false) }
                Text(l10n.myFavorites, style = MaterialTheme.typography.titleMedium)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showFavorites = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(l10n.viewSavedAdvice)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                }

                if (showFavorites) {
                    val favorites by viewModel.aiFavorites.collectAsState(initial = emptyList())
                    AlertDialog(
                        onDismissRequest = { showFavorites = false },
                        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
                        modifier = Modifier.fillMaxSize(),
                        text = {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(l10n.myFavorites, style = MaterialTheme.typography.headlineSmall)
                                    IconButton(onClick = { showFavorites = false }) {
                                        Icon(Icons.Default.Close, contentDescription = "Close")
                                    }
                                }
                                if (favorites.isEmpty()) {
                                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        Text(l10n.noDataMatch)
                                    }
                                } else {
                                    androidx.compose.foundation.lazy.LazyColumn(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(favorites) { fav ->
                                            var expanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                                            Card(
                                                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                            ) {
                                                Column(modifier = Modifier.padding(16.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.Top
                                                    ) {
                                                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.Top) {
                                                            Icon(
                                                                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(32.dp).padding(top = 4.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(12.dp))
                                                            Text(
                                                                "Q: ${fav.question}",
                                                                fontSize = 24.sp,
                                                                lineHeight = 32.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                maxLines = if (expanded) Int.MAX_VALUE else 2,
                                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                            )
                                                        }
                                                        IconButton(onClick = { viewModel.deleteAiFavorite(fav.id) }, modifier = Modifier.size(36.dp).padding(top = 4.dp)) {
                                                            Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(24.dp))
                                                        }
                                                    }
                                                    if (expanded) {
                                                        Spacer(modifier = Modifier.height(16.dp))
                                                        Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                                                        Spacer(modifier = Modifier.height(16.dp))
                                                        Text(
                                                            fav.answer, 
                                                            fontSize = 20.sp, 
                                                            lineHeight = 28.sp,
                                                            style = MaterialTheme.typography.bodyLarge
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = { }
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = { 
                        viewModel.logout()
                        onLogout()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(contentColor = androidx.compose.ui.graphics.Color.White)
                ) {
                    Text(l10n.logout, color = androidx.compose.ui.graphics.Color.White)
                }
                
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = androidx.compose.ui.graphics.Color.White)
                ) {
                    Text(l10n.deleteAccount, color = androidx.compose.ui.graphics.Color.White)
                }
            } else {
                Text("Loading...")
            }
        }
    }
    
    if (showEmailDialog) {
        var newEmail by remember { mutableStateOf(currentUser?.email ?: "") }
        AlertDialog(
            onDismissRequest = { showEmailDialog = false },
            title = { Text(l10n.changeEmail) },
            text = {
                OutlinedTextField(
                    value = newEmail,
                    onValueChange = { newEmail = it },
                    label = { Text(l10n.email) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateEmail(newEmail)
                    showEmailDialog = false
                }) { Text(l10n.save) }
            },
            dismissButton = {
                TextButton(onClick = { showEmailDialog = false }) { Text(l10n.cancel) }
            }
        )
    }
    
    if (showPasswordDialog) {
        var oldPassword by remember { mutableStateOf("") }
        var newPassword by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = { Text(l10n.changePassword) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (errorMessage.isNotEmpty()) {
                        Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedTextField(
                        value = oldPassword,
                        onValueChange = { oldPassword = it; errorMessage = "" },
                        label = { Text(l10n.oldPassword) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it; errorMessage = "" },
                        label = { Text(l10n.newPassword) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (oldPassword != currentUser?.passwordHash) {
                        errorMessage = "Error"
                    } else if (newPassword.isNotBlank()) {
                        viewModel.updatePassword(newPassword)
                        showPasswordDialog = false
                    } else {
                        errorMessage = "Error"
                    }
                }) { Text(l10n.save) }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }) { Text(l10n.cancel) }
            }
        )
    }
    
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(l10n.deleteAccount) },
            text = { Text(l10n.deleteAccountConfirm) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAccount(onSuccess = onLogout)
                    showDeleteDialog = false
                }) { Text(l10n.delete, color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(l10n.cancel) }
            }
        )
    }
}
