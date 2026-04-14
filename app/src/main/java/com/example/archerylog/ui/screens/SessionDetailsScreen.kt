package com.example.archerylog.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import com.example.archerylog.ui.ArcheryViewModel
import com.example.archerylog.ui.utils.L10n

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailsScreen(
    sessionId: Long,
    viewModel: ArcheryViewModel,
    onBack: () -> Unit
) {
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val l10n = L10n(currentLanguage)
    val endsWithShots by viewModel.getSessionDetails(sessionId).collectAsState(initial = emptyList())
    val grandTotal = endsWithShots.sumOf { it.end.endTotalScore }
    var selectedEndNumber by remember { mutableStateOf<Int?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(l10n.sessionDetailsTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) { 
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                val displayEnds = if (selectedEndNumber != null) {
                    endsWithShots.filter { it.end.endNumber == selectedEndNumber }
                } else {
                    endsWithShots
                }
                TargetFace(ends = displayEnds)
            }
            items(endsWithShots) { endWithShots ->
                val isSelected = selectedEndNumber == endWithShots.end.endNumber
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (isSelected) {
                                selectedEndNumber = null
                            } else {
                                selectedEndNumber = endWithShots.end.endNumber
                            }
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(getEndMarkerColor(endWithShots.end.endNumber), CircleShape)
                                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("${l10n.end} ${endWithShots.end.endNumber}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                            Text("${l10n.score}: ${endWithShots.end.endTotalScore} / ${endWithShots.shots.size * 10}", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val sortedShots = endWithShots.shots.sortedWith { a, b ->
                                val valA = if (a.score == "X") 11 else if (a.score == "M") -1 else a.score.toIntOrNull() ?: 0
                                val valB = if (b.score == "X") 11 else if (b.score == "M") -1 else b.score.toIntOrNull() ?: 0
                                valB.compareTo(valA)
                            }
                            for (i in 0 until 6) {
                                val score = sortedShots.getOrNull(i)?.score ?: ""
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(getScoreBackgroundColor(score), CircleShape)
                                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), CircleShape)
                                ) {
                                    Text(
                                        text = score,
                                        fontWeight = FontWeight.Bold,
                                        color = getScoreTextColor(score)
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
