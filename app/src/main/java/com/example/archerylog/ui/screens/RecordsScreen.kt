package com.example.archerylog.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.archerylog.ui.ArcheryViewModel
import com.example.archerylog.data.LocationType
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material.icons.Icons
import com.example.archerylog.ui.components.ProfileAvatar
import com.example.archerylog.ui.components.DateRangeFilterBar
import com.example.archerylog.ui.components.CustomWheelDatePickerDialog
import com.example.archerylog.ui.utils.L10n

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordsScreen(
    viewModel: ArcheryViewModel,
    onAddSessionClick: () -> Unit,
    onSessionClick: (Long) -> Unit
) {
    val sessions by viewModel.allSessions.collectAsState(initial = emptyList())
    val currentUser by viewModel.currentUser.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val l10n = L10n(currentLanguage)
    
    var sessionToDelete by remember { mutableStateOf<Long?>(null) }
    
    var startDateMillis by remember { mutableStateOf<Long?>(null) }
    var endDateMillis by remember { mutableStateOf<Long?>(null) }
    var pickingStart by remember { mutableStateOf(false) }
    var pickingEnd by remember { mutableStateOf(false) }

    val filteredSessions = remember(sessions, startDateMillis, endDateMillis) {
        sessions.filter { session ->
            val sessionTime = session.timestamp
            
            // Start of start day
            val afterStart = if (startDateMillis != null) {
                val cal = Calendar.getInstance().apply { timeInMillis = startDateMillis!! }
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                sessionTime >= cal.timeInMillis
            } else true
            
            // End of end day
            val beforeEnd = if (endDateMillis != null) {
                val cal = Calendar.getInstance().apply { timeInMillis = endDateMillis!! }
                cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
                sessionTime <= cal.timeInMillis
            } else true
            
            afterStart && beforeEnd
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(l10n.recordsTitle) },
                actions = {
                    if (currentUser != null) {
                        ProfileAvatar(
                            uri = currentUser!!.avatarUri,
                            size = 36.dp,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Spacer(modifier = Modifier.height(12.dp)) // Added breathing space
            
            DateRangeFilterBar(
                l10n = l10n,
                modifier = Modifier.padding(horizontal = 16.dp), // Matched with records padding
                startDateMillis = startDateMillis,
                endDateMillis = endDateMillis,
                onStartDateClick = { pickingStart = true },
                onEndDateClick = { pickingEnd = true },
                onClearStartDate = { startDateMillis = null },
                onClearEndDate = { endDateMillis = null }
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredSessions, key = { it.id }) { session ->
                SwipeToDeleteContainer(
                    onDeleteClick = { viewModel.deleteSession(session.id) },
                    deleteLabel = l10n.delete
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onSessionClick(session.id) }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            val dateString = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(session.timestamp))
                            Text(text = "${l10n.date}: $dateString", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                val locLabel = if (session.locationType == LocationType.OUTDOOR) l10n.outdoor else l10n.indoor
                                Text(text = "${session.distance}m - $locLabel")
                                
                                val recordedShots = session.totalShots
                                val maxPossibleScore = recordedShots * 10
                                
                                Text(text = "${l10n.score}: ${session.totalScore} / $maxPossibleScore", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

    if (pickingStart) {
        CustomWheelDatePickerDialog(
            initialDateMillis = startDateMillis ?: System.currentTimeMillis(),
            l10n = l10n,
            onDismiss = { pickingStart = false },
            onConfirmDate = { millis -> 
                startDateMillis = millis
                pickingStart = false
            }
        )
    }

    if (pickingEnd) {
        CustomWheelDatePickerDialog(
            initialDateMillis = endDateMillis ?: System.currentTimeMillis(),
            l10n = l10n,
            onDismiss = { pickingEnd = false },
            onConfirmDate = { millis -> 
                endDateMillis = millis
                pickingEnd = false
            }
        )
    }
}

@Composable
fun SwipeToDeleteContainer(
    onDeleteClick: () -> Unit,
    deleteLabel: String,
    content: @Composable () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    val animatedOffset by animateFloatAsState(targetValue = offsetX, label = "")

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color(0xFFF44336), RoundedCornerShape(12.dp))
                .clickable {
                    offsetX = 0f
                    onDeleteClick()
                }
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(deleteLabel, color = Color.White, fontWeight = FontWeight.Bold)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offsetX < -150f) {
                                offsetX = -250f 
                            } else {
                                offsetX = 0f 
                            }
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        offsetX = (offsetX + dragAmount).coerceIn(-250f, 0f)
                    }
                }
        ) {
            content()
        }
    }
}
