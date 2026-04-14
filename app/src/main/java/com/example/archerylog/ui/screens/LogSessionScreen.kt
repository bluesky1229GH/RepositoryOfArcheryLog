package com.example.archerylog.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Check
import com.example.archerylog.ui.ArcheryViewModel
import com.example.archerylog.ui.utils.L10n
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogSessionScreen(
    viewModel: ArcheryViewModel,
    onFinish: () -> Unit
) {
    // Collect all states safely
    val currentSession by viewModel.currentSession.collectAsState()
    val isCreating by viewModel.isCreatingSession.collectAsState()
    val isActive by viewModel.isSessionActive.collectAsState()
    val currentEndShots by viewModel.currentEndShots.collectAsState()
    val currentEndNumber by viewModel.currentEndNumber.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val l10n = L10n(currentLanguage)

    // Using stable local values to avoid model extraction crashes
    val stableDistance = viewModel.currentDistance
    val stableLocType = viewModel.currentLocationType

    // Explicit navigation callback
    LaunchedEffect(isActive) {
        if (!isActive) {
            onFinish()
        }
    }

    // Defensive loading screen
    if (currentSession == null || isCreating) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(l10n.newSession)
            }
        }
        return
    }

    val session = currentSession!!

    var showInstructions by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("${l10n.end} $currentEndNumber")
                        val locLabel = if(stableLocType == com.example.archerylog.data.LocationType.OUTDOOR) l10n.outdoor else l10n.indoor
                        Text(
                            text = "${stableDistance}m - $locLabel",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showInstructions = true }) {
                        Icon(Icons.Default.Info, contentDescription = "Instructions", tint = Color.White)
                    }
                }
            )
        }
    ) { paddingValues ->
        // Use a standard AlertDialog to show instructions
        if (showInstructions) {
            AlertDialog(
                onDismissRequest = { showInstructions = false },
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(l10n.instructionsTitle)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        l10n.usageGuide.split("\n").forEach { step ->
                            Row(verticalAlignment = Alignment.Top) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(8.dp)
                                        ) {}
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = step.substringAfter(". ").trim(),
                                    style = MaterialTheme.typography.bodyLarge,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showInstructions = false }) {
                        Text(l10n.close)
                    }
                }
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${l10n.totalScore}: ${session.totalScore}",
                style = MaterialTheme.typography.headlineMedium
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Score chips for the current end, rendered with safety
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (i in 0 until 6) {
                    val shot = currentEndShots.getOrNull(i)
                    val scoreStr = shot?.score ?: ""
                    Box(
                        modifier = Modifier
                            .size(45.dp)
                            .background(safeGetBgColor(scoreStr), CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = scoreStr,
                            color = safeGetTextColor(scoreStr),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Box(modifier = Modifier.weight(1f)) {
                TargetFace(
                    ends = emptyList(),
                    onTap = { x, y, _ ->
                        if (currentEndShots.size < 6) {
                            val score = calculateScore(x, y)
                            viewModel.addShot(score, x, y)
                        }
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { viewModel.undoLastShot() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                ) {
                    Text(l10n.undo)
                }
                
                Button(
                    onClick = { viewModel.finishSession() },
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                ) {
                    Text(l10n.finish)
                }
            }
        }
    }
}

// Global safety wrappers for colors
private fun safeGetBgColor(score: String): androidx.compose.ui.graphics.Color {
    return try {
        getScoreBackgroundColor(score)
    } catch (e: Exception) {
        androidx.compose.ui.graphics.Color.Transparent
    }
}

private fun safeGetTextColor(score: String): androidx.compose.ui.graphics.Color {
    return try {
        getScoreTextColor(score)
    } catch (e: Exception) {
        androidx.compose.ui.graphics.Color.Black
    }
}

private fun calculateScore(x: Float, y: Float): String {
    val dist = sqrt((x * x + y * y).toDouble())
    return when {
        dist <= 0.05 -> "X"
        dist <= 0.1 -> "10"
        dist <= 0.2 -> "9"
        dist <= 0.3 -> "8"
        dist <= 0.4 -> "7"
        dist <= 0.5 -> "6"
        dist <= 0.6 -> "5"
        dist <= 0.7 -> "4"
        dist <= 0.8 -> "3"
        dist <= 0.9 -> "2"
        dist <= 1.0 -> "1"
        else -> "M"
    }
}
