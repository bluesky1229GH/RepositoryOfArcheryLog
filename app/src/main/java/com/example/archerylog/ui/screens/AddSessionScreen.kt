package com.example.archerylog.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.archerylog.data.LocationType
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.archerylog.ui.ArcheryViewModel
import com.example.archerylog.ui.components.TargetFace
import com.example.archerylog.ui.components.getScoreBackgroundColor
import com.example.archerylog.ui.components.getScoreTextColor
import com.example.archerylog.ui.utils.L10n
import kotlin.math.sqrt
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSessionScreen(
    viewModel: ArcheryViewModel,
    onBack: () -> Unit,
    onStartSession: (Int, LocationType) -> Unit
) {
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val l10n = L10n(currentLanguage)
    
    val currentSession by viewModel.currentSession.collectAsState()
    val isSessionActive by viewModel.isSessionActive.collectAsState()
    val endsWithShots by viewModel.currentSessionEndsWithShots.collectAsState()
    val currentEndShots by viewModel.currentEndShots.collectAsState()
    val currentEndId by viewModel.currentEndId.collectAsState()
    val currentEndNumber by viewModel.currentEndNumber.collectAsState()
    val showDialog by viewModel.showEndCompletionDialog.collectAsState()
    val isCreatingSession by viewModel.isCreatingSession.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // Local state for selectors
    var sessionTitle by remember { mutableStateOf("") }
    var venue by remember { mutableStateOf("") }
    var distanceText by remember { mutableStateOf("30") }
    var location by remember { mutableStateOf(LocationType.OUTDOOR) }
    var weatherStr by remember { mutableStateOf("Sunny") }
    var windStr by remember { mutableStateOf("Low") }
    
    var expandedDist by remember { mutableStateOf(false) }
    var expandedWeather by remember { mutableStateOf(false) }
    var expandedWind by remember { mutableStateOf(false) }
    var showInstructions by remember { mutableStateOf(false) }
    var showAbandonConfirm by remember { mutableStateOf(false) }
    val distanceOptions = listOf("8", "10", "18", "30", "50", "60", "70")
    val weatherOptions = listOf("Sunny", "Cloudy", "Rainy")
    val windOptions = listOf("Low", "Mid", "High")

    fun getWeatherIcon(opt: String) = when(opt) {
        "Sunny" -> "☀️"
        "Cloudy" -> "☁️"
        "Rainy" -> "🌧️"
        else -> "☀️"
    }
    
    fun getWindIcon(opt: String) = when(opt) {
        "Low" -> "🍃"
        "Mid" -> "🌬️"
        "High" -> "🌀"
        else -> "🍃"
    }

    // Sync local state to viewModel and handle reset
    LaunchedEffect(sessionTitle, venue, distanceText, location, weatherStr, windStr) {
        viewModel.currentSessionTitle = sessionTitle
        viewModel.currentVenue = venue
        viewModel.currentDistance = distanceText.toIntOrNull() ?: 30
        viewModel.currentLocationType = location
        viewModel.currentWeather = weatherStr
        viewModel.currentWind = windStr
    }

    // Reset local inputs when session ends
    LaunchedEffect(isSessionActive) {
        if (!isSessionActive) {
            sessionTitle = ""
            venue = ""
        }
    }


    if (showAbandonConfirm) {
        AlertDialog(
            onDismissRequest = { showAbandonConfirm = false },
            title = { Text(l10n.abandonSessionTitle) },
            text = { Text(l10n.abandonSessionConfirm) },
            confirmButton = {
                Button(
                    onClick = {
                        showAbandonConfirm = false
                        viewModel.abandonSession()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(l10n.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAbandonConfirm = false }) {
                    Text(l10n.cancel)
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(l10n.newSession) },
                actions = {
                    IconButton(onClick = { showInstructions = true }) {
                        Icon(Icons.Default.Info, contentDescription = "Instructions", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            )
        }
    ) { padding ->
        // Setup Instructions Dialog
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
                    val allSteps = listOf(l10n.usageGuide, l10n.setupGuide, l10n.notes)
                        .flatMap { it.split("\n") }
                        .filter { it.isNotBlank() }
                    
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        allSteps.forEach { step ->
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
                                    text = step.replace("^•\\s*".toRegex(), "").replace("^\\d+\\.\\s*".toRegex(), "").trim(),
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
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Session Configuration & Info Row (Collapses when active)
            AnimatedVisibility(
                visible = !isSessionActive,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = sessionTitle,
                        onValueChange = { sessionTitle = it },
                        label = { Text(l10n.sessionTitle) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = venue,
                        onValueChange = { venue = it },
                        label = { Text(l10n.venue) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Distance Selector
                        ExposedDropdownMenuBox(
                            expanded = expandedDist,
                            onExpandedChange = { expandedDist = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = "${distanceText}m",
                                onValueChange = { },
                                readOnly = true,
                                label = { Text(l10n.distance) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDist) },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expandedDist,
                                onDismissRequest = { expandedDist = false }
                            ) {
                                distanceOptions.forEach { dist ->
                                    DropdownMenuItem(
                                        text = { Text("${dist}m") },
                                        onClick = {
                                            distanceText = dist
                                            viewModel.currentDistance = dist.toInt()
                                            expandedDist = false
                                        }
                                    )
                                }
                            }
                        }

                        // Location Selector
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = location == LocationType.OUTDOOR,
                                onClick = { 
                                    location = LocationType.OUTDOOR 
                                    viewModel.currentLocationType = LocationType.OUTDOOR
                                }
                            )
                            Text(l10n.outdoor, style = MaterialTheme.typography.bodySmall)
                            RadioButton(
                                selected = location == LocationType.INDOOR,
                                onClick = { 
                                    location = LocationType.INDOOR 
                                    viewModel.currentLocationType = LocationType.INDOOR
                                }
                            )
                            Text(l10n.indoor, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Weather Selector
                        ExposedDropdownMenuBox(
                            expanded = expandedWeather,
                            onExpandedChange = { expandedWeather = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            val displayWeather = when(weatherStr) {
                                "Sunny" -> l10n.sunny
                                "Cloudy" -> l10n.cloudy
                                "Rainy" -> l10n.rainy
                                else -> weatherStr
                            }
                            OutlinedTextField(
                                value = "${getWeatherIcon(weatherStr)} $displayWeather",
                                onValueChange = { },
                                readOnly = true,
                                label = { Text(l10n.weather) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedWeather) },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expandedWeather,
                                onDismissRequest = { expandedWeather = false }
                            ) {
                                weatherOptions.forEach { opt ->
                                    val optLabel = when(opt) {
                                        "Sunny" -> l10n.sunny
                                        "Cloudy" -> l10n.cloudy
                                        "Rainy" -> l10n.rainy
                                        else -> opt
                                    }
                                    DropdownMenuItem(
                                        text = { Text("${getWeatherIcon(opt)} $optLabel") },
                                        onClick = {
                                            weatherStr = opt
                                            viewModel.currentWeather = opt
                                            expandedWeather = false
                                        }
                                    )
                                }
                            }
                        }

                        // Wind Selector
                        ExposedDropdownMenuBox(
                            expanded = expandedWind,
                            onExpandedChange = { expandedWind = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            val displayWind = when(windStr) {
                                "Low" -> l10n.lowWind
                                "Mid" -> l10n.midWind
                                "High" -> l10n.highWind
                                else -> windStr
                            }
                            OutlinedTextField(
                                value = "${getWindIcon(windStr)} $displayWind",
                                onValueChange = { },
                                readOnly = true,
                                label = { Text(l10n.wind) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedWind) },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expandedWind,
                                onDismissRequest = { expandedWind = false }
                            ) {
                                windOptions.forEach { opt ->
                                    val optLabel = when(opt) {
                                        "Low" -> l10n.lowWind
                                        "Mid" -> l10n.midWind
                                        "High" -> l10n.highWind
                                        else -> opt
                                    }
                                    DropdownMenuItem(
                                        text = { Text("${getWindIcon(opt)} $optLabel") },
                                        onClick = {
                                            windStr = opt
                                            viewModel.currentWind = opt
                                            expandedWind = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Compact Metadata Summary after recording starts
            AnimatedVisibility(visible = isSessionActive) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp, 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (sessionTitle.isNotBlank()) sessionTitle else l10n.navAdd,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                            maxLines = 1
                        )
                        Text(
                            text = "·",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                        Text(
                            text = "${distanceText}m ${if (location == LocationType.OUTDOOR) l10n.outdoor else l10n.indoor}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "·",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                        Text(
                            text = "${getWeatherIcon(weatherStr)} ${getWindIcon(windStr)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Unified Active Area (Zone 2 + Zone 3) - Dynamic Background
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) {
                        // Intentionally empty to intercept clicks within the recording area
                    },
                tonalElevation = if (isSessionActive) 4.dp else 0.dp,
                color = if (isSessionActive) Color(0xFF424242) else Color.Transparent,
                border = if (isSessionActive) 
                    androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)) 
                    else null,
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Zone 2: Target Face Area
                    if (isSessionActive) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${l10n.end} $currentEndNumber",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            
                            val totalScore = endsWithShots.sumOf { it.end.endTotalScore }
                            val totalShots = endsWithShots.sumOf { it.shots.size }
                            
                            // Right side group: Score + Close Button
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(horizontalAlignment = Alignment.End) {
                                    val totalScore = endsWithShots.sumOf { it.end.endTotalScore }
                                    Text(
                                        text = "${l10n.totalScore}: $totalScore / ${currentEndNumber * 60}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(16.dp))
                                
                                // Modern circular close button
                                Surface(
                                    onClick = { showAbandonConfirm = true },
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.15f),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Abandon session",
                                            modifier = Modifier.size(18.dp),
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        TargetFace(
                            ends = endsWithShots.filter { it.end.id == currentEndId },
                            enabled = currentEndShots.size < 6,
                            onTap = { x, y, _ ->
                                if (isCreatingSession) return@TargetFace
                                if (!isSessionActive) {
                                    viewModel.startSessionManual()
                                } else {
                                    val scoreText = calculateScore(x, y)
                                    viewModel.addShot(scoreText, x, y)
                                }
                            }
                        )
                        if (isCreatingSession) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.3f), MaterialTheme.shapes.extraLarge),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color.White)
                            }
                        }
                    }

                    // Hint text underneath target (only before session starts)
                    if (!isSessionActive) {
                        Text(
                            text = l10n.tapTargetToStart,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }

                    // Zone 3: Controls Area (Ends Indicators & Controls)
                    if (isSessionActive) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Smart Hint Area (Replaces the Dialog)
                            AnimatedVisibility(
                                visible = showDialog,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Check, 
                                            contentDescription = null, 
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            text = if (currentEndNumber >= 6) l10n.lastEndNotice else l10n.bufferMessage,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            lineHeight = 20.sp
                                        )
                                    }
                                }
                            }

                            // Quick Controls
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.undoLastShot() },
                                    enabled = !isCreatingSession && currentEndShots.isNotEmpty(),
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = Color.White
                                    ),
                                    contentPadding = PaddingValues(vertical = 12.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Undo, 
                                        contentDescription = null,
                                        tint = if (!isCreatingSession && currentEndShots.isNotEmpty()) Color.White else Color.Gray
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = l10n.undo,
                                        color = if (!isCreatingSession && currentEndShots.isNotEmpty()) Color.White else Color.Gray
                                    )
                                }

                                Button(
                                    onClick = { 
                                        if (currentEndNumber < 6) {
                                            viewModel.moveToNextEnd()
                                        }
                                    },
                                    enabled = !isCreatingSession && currentEndShots.size >= 6 && currentEndNumber < 6,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF4CAF50),
                                        disabledContainerColor = Color(0xFF4CAF50).copy(alpha = 0.3f),
                                        contentColor = Color.White,
                                        disabledContentColor = Color.White.copy(alpha = 0.5f)
                                    ),
                                    contentPadding = PaddingValues(vertical = 12.dp)
                                ) {
                                    Text(
                                        text = if (currentEndShots.size >= 6) l10n.start else l10n.start, // Simplified for now, but l10n.start is used for both
                                        color = if (isCreatingSession) Color.Gray else Color.White
                                    )
                                }

                                Button(
                                    onClick = { 
                                        if (currentEndShots.isNotEmpty()) {
                                            viewModel.moveToNextEnd()
                                        }
                                        viewModel.finishSession()
                                    },
                                    enabled = !isCreatingSession,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFE57373),
                                        contentColor = Color.White
                                    ),
                                    contentPadding = PaddingValues(vertical = 12.dp)
                                ) {
                                    Text(
                                        text = l10n.finish,
                                        color = if (isCreatingSession) Color.Gray else Color.White
                                    )
                                }
                            }

                            // Current End Shots Indicators
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                repeat(6) { index ->
                                    val shot = currentEndShots.getOrNull(index)
                                    Box(
                                        modifier = Modifier
                                            .size(45.dp)
                                            .background(
                                                color = if (shot != null) getScoreBackgroundColor(shot.score) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                                shape = CircleShape
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = shot?.score ?: "",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = if (shot != null) getScoreTextColor(shot.score) else Color.Transparent
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
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
