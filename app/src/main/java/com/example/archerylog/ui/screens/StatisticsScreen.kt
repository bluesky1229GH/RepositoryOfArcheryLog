package com.example.archerylog.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.shadow
import androidx.compose.animation.*
import androidx.compose.material.icons.filled.Close
import kotlinx.coroutines.launch
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import com.example.archerylog.data.ArcherySession
import com.example.archerylog.data.LocationType
import com.example.archerylog.ui.ArcheryViewModel
import com.example.archerylog.ui.utils.L10n

enum class TimeRangeFilter {
    ALL, DAY, WEEK, MONTH, YEAR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: ArcheryViewModel
) {
    val allSessions by viewModel.allSessions.collectAsState()
    val allEnds by viewModel.allEndsWithMetadata.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val l10n = L10n(currentLanguage)

    // AI Chat State
    var showAiChat by remember { mutableStateOf(false) }
    var aiQuestion by remember { mutableStateOf("") }
    val aiResponse by viewModel.aiResponse.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var isCurrentResponseSaved by remember { mutableStateOf(false) }

    // Reset saved state when new response comes
    LaunchedEffect(aiResponse) {
        isCurrentResponseSaved = false
    }

    var selectedLocation by remember { mutableStateOf<LocationType?>(null) } // null means ALL
    var selectedDistance by remember { mutableStateOf<Int?>(null) } // null means ALL
    var selectedTimeRange by remember { mutableStateOf(TimeRangeFilter.ALL) }
    
    // Extract unique distances from sessions
    val availableDistances = remember(allSessions) {
        allSessions.map { it.distance }.distinct().sorted()
    }

    val filteredEnds = remember(allEnds, selectedLocation, selectedDistance, selectedTimeRange) {
        val now = System.currentTimeMillis()
        val dayMs = 24L * 60 * 60 * 1000
        val cutoff = when (selectedTimeRange) {
            TimeRangeFilter.DAY -> now - dayMs
            TimeRangeFilter.WEEK -> now - (7 * dayMs)
            TimeRangeFilter.MONTH -> now - (30 * dayMs)
            TimeRangeFilter.YEAR -> now - (365 * dayMs)
            TimeRangeFilter.ALL -> 0L
        }
        
        allEnds.filter { end ->
            (end.timestamp >= cutoff) &&
            (end.endTotalScore > 0) &&
            (selectedLocation == null || end.locationType == selectedLocation) &&
            (selectedDistance == null || end.distance == selectedDistance)
        }.sortedWith(compareBy({ it.timestamp }, { it.endNumber }))
    }

    val filteredSessionsCount = remember(allSessions, selectedLocation, selectedDistance, selectedTimeRange) {
        val now = System.currentTimeMillis()
        val dayMs = 24L * 60 * 60 * 1000
        val cutoff = when (selectedTimeRange) {
            TimeRangeFilter.DAY -> now - dayMs
            TimeRangeFilter.WEEK -> now - (7 * dayMs)
            TimeRangeFilter.MONTH -> now - (30 * dayMs)
            TimeRangeFilter.YEAR -> now - (365 * dayMs)
            TimeRangeFilter.ALL -> 0L
        }
        allSessions.count { session ->
            session.timestamp >= cutoff && session.totalShots > 0 &&
            (selectedLocation == null || session.locationType == selectedLocation) &&
            (selectedDistance == null || session.distance == selectedDistance)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(l10n.dashboardTitle) },
                actions = {
                    IconButton(onClick = { showAiChat = true }) {
                        Icon(
                            Icons.Default.AutoAwesome, 
                            contentDescription = "AI Coach",
                            tint = MaterialTheme.colorScheme.onSurface 
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Top Dropdown for AI Chat
            AnimatedVisibility(
                visible = showAiChat,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
                modifier = Modifier.zIndex(1f) // Ensure it's on top
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth().shadow(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                l10n.aiConsultant,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(onClick = { 
                                showAiChat = false 
                                viewModel.clearAiResponse()
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = aiQuestion,
                            onValueChange = { aiQuestion = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(l10n.askAiPlaceholder) },
                            trailingIcon = {
                                if (isAiLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                } else {
                                    IconButton(onClick = { viewModel.askGemini(aiQuestion) }) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                    }
                                }
                            }
                        )
                        
                        if (aiResponse != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                                    .padding(16.dp)
                            ) {
                                Box(modifier = Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState())) {
                                    Text(aiResponse!!, style = MaterialTheme.typography.bodyMedium)
                                }
                                Divider(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = { 
                                        if (!isCurrentResponseSaved) {
                                            viewModel.saveAiFavorite(aiQuestion, aiResponse!!) 
                                            isCurrentResponseSaved = true
                                            scope.launch {
                                                snackbarHostState.showSnackbar(l10n.savedSuccess)
                                            }
                                        }
                                    }) {
                                        Icon(
                                            imageVector = if (isCurrentResponseSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = if (isCurrentResponseSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            if (isCurrentResponseSaved) l10n.savedSuccess else l10n.saveToFavorites,
                                            color = if (isCurrentResponseSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxSize()
                    .padding(16.dp)
            ) {
                // Time Range Filter
                Text(l10n.timeRange, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(selected = selectedTimeRange == TimeRangeFilter.ALL, onClick = { selectedTimeRange = TimeRangeFilter.ALL }, label = { Text(l10n.allTime) }) }
                    item { FilterChip(selected = selectedTimeRange == TimeRangeFilter.DAY, onClick = { selectedTimeRange = TimeRangeFilter.DAY }, label = { Text(l10n.last24h) }) }
                    item { FilterChip(selected = selectedTimeRange == TimeRangeFilter.WEEK, onClick = { selectedTimeRange = TimeRangeFilter.WEEK }, label = { Text(l10n.lastWeek) }) }
                    item { FilterChip(selected = selectedTimeRange == TimeRangeFilter.MONTH, onClick = { selectedTimeRange = TimeRangeFilter.MONTH }, label = { Text(l10n.lastMonth) }) }
                    item { FilterChip(selected = selectedTimeRange == TimeRangeFilter.YEAR, onClick = { selectedTimeRange = TimeRangeFilter.YEAR }, label = { Text(l10n.lastYear) }) }
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Location Filter
                Text(l10n.locationFilter, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = selectedLocation == null, onClick = { selectedLocation = null }, label = { Text(l10n.all) })
                    FilterChip(selected = selectedLocation == LocationType.INDOOR, onClick = { selectedLocation = LocationType.INDOOR }, label = { Text(l10n.indoor) })
                    FilterChip(selected = selectedLocation == LocationType.OUTDOOR, onClick = { selectedLocation = LocationType.OUTDOOR }, label = { Text(l10n.outdoor) })
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Distance Filter
                Text(l10n.distanceFilter, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(selected = selectedDistance == null, onClick = { selectedDistance = null }, label = { Text(l10n.all) })
                    }
                    items(availableDistances) { dist ->
                        FilterChip(selected = selectedDistance == dist, onClick = { selectedDistance = dist }, label = { Text("${dist}m") })
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Data Content (Chart or No Data)
                if (filteredEnds.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(l10n.noDataMatch, color = Color.Gray)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(l10n.trendTitle, fontWeight = FontWeight.Bold)
                        Text("${filteredSessionsCount} ${l10n.navRecords}", color = MaterialTheme.colorScheme.primary)
                    }
                    if (filteredEnds.size >= 2) {
                        val sdf = java.text.SimpleDateFormat("MM/dd", java.util.Locale.getDefault())
                        val dateRange = "${sdf.format(filteredEnds.first().timestamp)} - ${sdf.format(filteredEnds.last().timestamp)}"
                        Text(dateRange, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth().height(360.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            EndTrendChart(
                                ends = filteredEnds,
                                timeRange = selectedTimeRange,
                                modifier = Modifier.weight(1f).padding(top = 32.dp, start = 40.dp, end = 24.dp, bottom = 12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EndTrendChart(
    ends: List<com.example.archerylog.data.EndWithMetadata>, 
    timeRange: TimeRangeFilter,
    modifier: Modifier = Modifier
) {
    if (ends.isEmpty()) return
    val chartColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)

    // Zoom and Pan States
    var zoomScale by remember { mutableStateOf(1f) }
    var scrollOffset by remember { mutableStateOf(0f) }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        zoomScale = (zoomScale * zoom).coerceIn(1f, 10f)
                        scrollOffset += pan.x
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                val maxScroll = (zoomScale - 1f) * width
                val safeOffset = scrollOffset.coerceIn(-maxScroll, 0f)
                if (scrollOffset != safeOffset) {
                    scrollOffset = safeOffset
                }

                // Draw Background Grid
                val gridLines = 5
                for (i in 0..gridLines) {
                    val score = i * 2 // 0, 2, 4, 6, 8, 10
                    val y = height - (score.toFloat() / 10f * height)
                    drawLine(color = gridColor, start = Offset(0f, y), end = Offset(width, y), strokeWidth = 1.dp.toPx())
                    
                    // Y-AXIS NUMBERS
                    drawContext.canvas.nativeCanvas.drawText(
                        score.toString(), -15.dp.toPx(), y + 4.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = textColor.toArgb()
                            textSize = 10.sp.toPx()
                            textAlign = android.graphics.Paint.Align.RIGHT
                        }
                    )
                }

                chartColor // Dummy use
                clipRect(0f, 0f, width, height) {
                    val points = ends.mapIndexed { index, end ->
                        val px = if (ends.size > 1) {
                            (index.toFloat() / (ends.size - 1).toFloat()) * width * zoomScale + scrollOffset
                        } else {
                            width / 2f
                        }
                        val py = height - ((end.endTotalScore.toFloat() / 6f) / 10f * height)
                        Offset(px, py)
                    }

                    if (points.size >= 2) {
                        val path = Path()
                        path.moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            val p0 = points[i - 1]
                            val p1 = points[i]
                            val cx = (p0.x + p1.x) / 2
                            path.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                        }
                        
                        val fillPath = Path()
                        fillPath.addPath(path)
                        fillPath.lineTo(points.last().x, height)
                        fillPath.lineTo(points.first().x, height)
                        fillPath.close()

                        drawPath(fillPath, Brush.verticalGradient(listOf(chartColor.copy(0.4f), Color.Transparent)))
                        drawPath(path, chartColor, style = Stroke(2.5.dp.toPx()))
                    }
                    
                    for (p in points) {
                        if (p.x in 0f..width) {
                            drawCircle(chartColor, 3.5.dp.toPx(), p)
                            drawCircle(Color.White, 1.2.dp.toPx(), p)
                        }
                    }
                }

                // X-AXIS LABELS - Based on sample indices
                val sdf = java.text.SimpleDateFormat(if(timeRange == TimeRangeFilter.DAY) "HH:mm" else "MM/dd")
                val total = ends.size
                if (total > 0) {
                    val step = (total / (3 * zoomScale).toInt()).coerceAtLeast(1)
                    for (i in 0 until total step step) {
                        val labelX = (i.toFloat() / (total - 1).coerceAtLeast(1).toFloat()) * width * zoomScale + scrollOffset
                        if (labelX in -50f..width + 50f) {
                            drawContext.canvas.nativeCanvas.drawText(
                                sdf.format(java.util.Date(ends[i].timestamp)), labelX, height + 35.dp.toPx(),
                                android.graphics.Paint().apply { color = textColor.toArgb(); textSize = 11.sp.toPx(); textAlign = android.graphics.Paint.Align.CENTER }
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))

        // Final Slider Row
        Row(
            verticalAlignment = Alignment.CenterVertically, 
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp)
        ) {
            val viewModel: ArcheryViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            val l10n = L10n(viewModel.currentLanguage.collectAsState().value)
            Text("${l10n.zoom} ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(
                value = zoomScale,
                onValueChange = { zoomScale = it },
                valueRange = 1f..10f,
                modifier = Modifier.weight(1f).height(32.dp)
            )
            Text("${zoomScale.toInt()}x", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = chartColor, modifier = Modifier.width(30.dp))
        }
    }
}

// Helper functions to keep code clean
fun startTime(timeRange: TimeRangeFilter, ends: List<com.example.archerylog.data.EndWithMetadata>): Long {
    val now = System.currentTimeMillis()
    val dayMs = 24L * 60 * 60 * 1000
    return when (timeRange) {
        TimeRangeFilter.DAY -> now - dayMs
        TimeRangeFilter.WEEK -> now - (7 * dayMs)
        TimeRangeFilter.MONTH -> now - (30 * dayMs)
        TimeRangeFilter.YEAR -> now - (365 * dayMs)
        TimeRangeFilter.ALL -> if(ends.isEmpty()) now else ends.minOf { it.timestamp }
    }
}

fun endTime(timeRange: TimeRangeFilter, ends: List<com.example.archerylog.data.EndWithMetadata>): Long {
    val now = System.currentTimeMillis()
    return when (timeRange) {
        TimeRangeFilter.ALL -> if(ends.isEmpty()) now else ends.maxOf { it.timestamp }
        else -> now
    }
}

fun duration(timeRange: TimeRangeFilter, ends: List<com.example.archerylog.data.EndWithMetadata>): Long {
    val start = startTime(timeRange, ends)
    val end = endTime(timeRange, ends)
    return (end - start).coerceAtLeast(1)
}
