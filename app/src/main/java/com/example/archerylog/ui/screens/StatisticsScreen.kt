package com.example.archerylog.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.archerylog.data.ArcherySession
import com.example.archerylog.data.LocationType
import com.example.archerylog.ui.ArcheryViewModel
import com.example.archerylog.ui.utils.L10n

enum class TimeRangeFilter { ALL, DAY, WEEK, MONTH, YEAR }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: ArcheryViewModel
) {
    val allSessions by viewModel.allSessions.collectAsState()
    val allEnds by viewModel.allEndsWithMetadata.collectAsState()
    val allShots by viewModel.allShotsWithMetadata.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val l10n = L10n(currentLanguage)

    var showAiChat by remember { mutableStateOf(false) }
    var aiQuestion by remember { mutableStateOf("") }
    val aiResponse by viewModel.aiResponse.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearAiResponse()
        }
    }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showFavorites by remember { mutableStateOf(false) }
    val aiFavorites by viewModel.aiFavorites.collectAsState()

    var selectedLocation by remember { mutableStateOf<LocationType?>(null) }
    var selectedDistance by remember { mutableStateOf<Int?>(null) }
    var selectedTimeRange by remember { mutableStateOf(TimeRangeFilter.ALL) }
    
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
            end.timestamp >= cutoff && end.endTotalScore > 0 &&
            (selectedLocation == null || end.locationType == selectedLocation) &&
            (selectedDistance == null || end.distance == selectedDistance)
        }.sortedBy { it.timestamp }
    }

    val filteredShots = remember(allShots, selectedLocation, selectedDistance, selectedTimeRange) {
        val now = System.currentTimeMillis()
        val cutoff = when (selectedTimeRange) {
            TimeRangeFilter.DAY -> now - (24L * 60 * 60 * 1000)
            TimeRangeFilter.WEEK -> now - (7L * 24 * 60 * 60 * 1000)
            TimeRangeFilter.MONTH -> now - (30L * 24 * 60 * 60 * 1000)
            TimeRangeFilter.YEAR -> now - (365L * 24 * 60 * 60 * 1000)
            TimeRangeFilter.ALL -> 0L
        }
        allShots.filter { shot ->
            shot.timestamp >= cutoff &&
            (selectedLocation == null || shot.locationType == selectedLocation) &&
            (selectedDistance == null || shot.distance == selectedDistance)
        }
    }

    val filteredSessionsCount = remember(allSessions, selectedLocation, selectedDistance, selectedTimeRange) {
        val now = System.currentTimeMillis()
        val cutoff = when (selectedTimeRange) {
            TimeRangeFilter.DAY -> now - (24L * 60 * 60 * 1000)
            TimeRangeFilter.WEEK -> now - (7 * 24L * 60 * 60 * 1000)
            TimeRangeFilter.MONTH -> now - (30 * 24L * 60 * 60 * 1000)
            TimeRangeFilter.YEAR -> now - (365 * 24L * 60 * 60 * 1000)
            TimeRangeFilter.ALL -> 0L
        }
        allSessions.count { it.timestamp >= cutoff && it.totalShots > 0 &&
            (selectedLocation == null || it.locationType == selectedLocation) &&
            (selectedDistance == null || it.distance == selectedDistance)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(l10n.dashboardTitle) },
                actions = {
                    IconButton(onClick = { showAiChat = true }) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI Coach", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (showAiChat) {
                Surface(
                    modifier = Modifier.fillMaxWidth().shadow(8.dp).zIndex(2f),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(l10n.aiConsultant, style = MaterialTheme.typography.titleLarge, color = Color(0xFF4CAF50))
                            IconButton(onClick = { showAiChat = false; aiQuestion = ""; viewModel.clearAiResponse() }) { Icon(Icons.Default.Close, null) }
                        }
                        OutlinedTextField(
                            value = aiQuestion,
                            onValueChange = { aiQuestion = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(l10n.askAiPlaceholder) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4CAF50),
                                unfocusedBorderColor = Color(0xFF4CAF50).copy(alpha = 0.5f),
                                cursorColor = Color(0xFF4CAF50)
                            ),
                            trailingIcon = {
                                IconButton(onClick = { viewModel.askGemini(aiQuestion) }) {
                                    if (isAiLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFF4CAF50))
                                    else Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF4CAF50))
                                }
                            }
                        )
                        if (aiResponse != null) {
                            val alreadySaved = aiFavorites.any { it.question == aiQuestion }
                            var isSaved by remember { mutableStateOf(alreadySaved) }
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
                            ) {
                                Column {
                                    Box(modifier = Modifier.heightIn(max = 280.dp).padding(16.dp).verticalScroll(rememberScrollState())) {
                                        Text(aiResponse!!, style = MaterialTheme.typography.bodyMedium)
                                    }
                                    Divider(color = Color.Gray.copy(alpha = 0.3f), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(
                                            onClick = {
                                                if (!isSaved) {
                                                    viewModel.saveAiFavorite(aiQuestion, aiResponse!!)
                                                    isSaved = true
                                                }
                                            }
                                        ) {
                                            Icon(
                                                if (isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                null,
                                                modifier = Modifier.size(16.dp),
                                                tint = Color(0xFF4CAF50)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(l10n.saveToFavorites, fontSize = 12.sp, color = Color(0xFF4CAF50))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())
            ) {
                Text(l10n.timeRange, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(selected = selectedTimeRange == TimeRangeFilter.ALL, onClick = { selectedTimeRange = TimeRangeFilter.ALL }, label = { Text(l10n.allTime) }) }
                    item { FilterChip(selected = selectedTimeRange == TimeRangeFilter.WEEK, onClick = { selectedTimeRange = TimeRangeFilter.WEEK }, label = { Text(l10n.lastWeek) }) }
                    item { FilterChip(selected = selectedTimeRange == TimeRangeFilter.MONTH, onClick = { selectedTimeRange = TimeRangeFilter.MONTH }, label = { Text(l10n.lastMonth) }) }
                    item { FilterChip(selected = selectedTimeRange == TimeRangeFilter.YEAR, onClick = { selectedTimeRange = TimeRangeFilter.YEAR }, label = { Text(l10n.lastYear) }) }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(l10n.locationFilter, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(selected = selectedLocation == null, onClick = { selectedLocation = null }, label = { Text(l10n.all) }) }
                    item { FilterChip(selected = selectedLocation == LocationType.INDOOR, onClick = { selectedLocation = LocationType.INDOOR }, label = { Text(l10n.indoor) }) }
                    item { FilterChip(selected = selectedLocation == LocationType.OUTDOOR, onClick = { selectedLocation = LocationType.OUTDOOR }, label = { Text(l10n.outdoor) }) }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(l10n.distanceFilter, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(selected = selectedDistance == null, onClick = { selectedDistance = null }, label = { Text(l10n.all) }) }
                    items(availableDistances) { dist ->
                        FilterChip(selected = selectedDistance == dist, onClick = { selectedDistance = dist }, label = { Text("${dist}m") })
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (filteredEnds.isEmpty()) {
                    Box(modifier = Modifier.height(300.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(l10n.noDataMatch, color = Color.Gray)
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(l10n.trendTitle, fontWeight = FontWeight.Bold)
                                Text("${filteredSessionsCount}场", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            EndTrendChart(
                                ends = filteredEnds,
                                timeRange = selectedTimeRange,
                                l10n = l10n,
                                modifier = Modifier.height(240.dp).fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(l10n.ringDistribution, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(16.dp))
                            RingDistributionChart(shots = filteredShots, l10n = l10n)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
fun RingDistributionChart(shots: List<com.example.archerylog.data.ShotWithMetadata>, l10n: L10n) {
    if (shots.isEmpty()) return
    val total = shots.size.toFloat()
    
    val gold = shots.count { it.numericValue >= 9 } / total
    val red = shots.count { it.numericValue in 7..8 } / total
    val blue = shots.count { it.numericValue in 5..6 } / total
    val black = shots.count { it.numericValue in 3..4 } / total
    val white = shots.count { it.numericValue in 0..2 } / total

    val colors = listOf(
        Color(0xFFFFD700),
        Color(0xFFF44336),
        Color(0xFF2196F3),
        Color(0xFF212121),
        Color(0xFFEEEEEE)
    )
    val labels = listOf(l10n.goldZone, l10n.redZone, l10n.blueZone, l10n.blackZone, l10n.whiteZone)
    val values = listOf(gold, red, blue, black, white)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .shadow(2.dp, RoundedCornerShape(8.dp))
                .background(Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                .border(1.5.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
        ) {
            values.forEachIndexed { index, ratio ->
                if (ratio > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(ratio.coerceAtLeast(0.01f))
                            .fillMaxHeight()
                            .background(colors[index])
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            values.forEachIndexed { index, ratio ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(12.dp).background(colors[index], RoundedCornerShape(2.dp)))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(labels[index], fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text("${(ratio * 100).toInt()}%", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun EndTrendChart(
    ends: List<com.example.archerylog.data.EndWithMetadata>, 
    timeRange: TimeRangeFilter,
    l10n: L10n,
    modifier: Modifier = Modifier
) {
    if (ends.isEmpty()) return
    val chartColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)

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
                val leftPadding = 24.dp.toPx()
                val rightPadding = 8.dp.toPx()
                val topPadding = 20.dp.toPx()
                val bottomPadding = 30.dp.toPx()
                
                val chartWidth = size.width - leftPadding - rightPadding
                val chartHeight = size.height - topPadding - bottomPadding
                val height = size.height
                
                val maxScroll = (zoomScale - 1f) * chartWidth
                val safeOffset = scrollOffset.coerceIn(-maxScroll, 0f)
                if (scrollOffset != safeOffset) scrollOffset = safeOffset

                // Grid lines and labels (0 to 10)
                val gridLines = 5
                for (i in 0..gridLines) {
                    val score = i * 2
                    val y = topPadding + chartHeight - (score.toFloat() / 10f * chartHeight)
                    drawLine(color = gridColor, start = Offset(leftPadding, y), end = Offset(size.width - rightPadding, y), strokeWidth = 1.dp.toPx())
                    drawContext.canvas.nativeCanvas.drawText(
                        score.toString(), leftPadding - 8.dp.toPx(), y + 4.dp.toPx(),
                        android.graphics.Paint().apply { color = textColor.toArgb(); textSize = 10.sp.toPx(); textAlign = android.graphics.Paint.Align.RIGHT }
                    )
                }

                clipRect(leftPadding, topPadding, size.width - rightPadding, topPadding + chartHeight) {
                    val points = ends.mapIndexed { index, end ->
                        val px = if (ends.size > 1) leftPadding + (index.toFloat() / (ends.size - 1).toFloat()) * chartWidth * zoomScale + scrollOffset else leftPadding + chartWidth/2f
                        // Important: Clamp avg score to max 10.0 to prevent data errors
                        val avgScore = (end.endTotalScore.toFloat() / 6f).coerceIn(0f, 10f)
                        val py = topPadding + chartHeight - (avgScore / 10f * chartHeight)
                        Offset(px, py)
                    }

                    if (points.size >= 2) {
                        val path = Path()
                        path.moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            val p0 = points[i - 1]; val p1 = points[i]
                            val cx = (p0.x + p1.x) / 2
                            // Use slight smoothing but keep control points within the segment range to prevent overshoot
                            path.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                        }
                        drawPath(path, chartColor, style = Stroke(2.5.dp.toPx()))
                        
                        val fillPath = Path().apply { 
                            addPath(path)
                            lineTo(points.last().x, topPadding + chartHeight)
                            lineTo(points.first().x, topPadding + chartHeight)
                            close() 
                        }
                        drawPath(fillPath, Brush.verticalGradient(listOf(chartColor.copy(0.2f), Color.Transparent)))
                    }
                    points.forEach { p -> 
                        if (p.x in leftPadding..(size.width - rightPadding)) {
                            drawCircle(chartColor, 3.dp.toPx(), p)
                        }
                    }
                }

                val sdf = java.text.SimpleDateFormat("MM/dd")
                val total = ends.size
                if (total > 0) {
                    val step = (total / (3 * zoomScale).toInt()).coerceAtLeast(1)
                    for (i in 0 until total step step) {
                        val lx = leftPadding + (i.toFloat() / (total - 1).coerceAtLeast(1).toFloat()) * chartWidth * zoomScale + scrollOffset
                        if (lx in -50f..size.width + 50f) {
                            drawContext.canvas.nativeCanvas.drawText(
                                sdf.format(java.util.Date(ends[i].timestamp)), lx, topPadding + chartHeight + 24.dp.toPx(),
                                android.graphics.Paint().apply { color = textColor.toArgb(); textSize = 10.sp.toPx(); textAlign = android.graphics.Paint.Align.CENTER }
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            Text(l10n.zoom, fontSize = 10.sp, color = textColor)
            Slider(value = zoomScale, onValueChange = { zoomScale = it }, valueRange = 1f..10f, modifier = Modifier.weight(1f).height(24.dp))
            Text("${zoomScale.toInt()}x", fontSize = 10.sp, color = chartColor, modifier = Modifier.width(24.dp))
        }
    }
}
