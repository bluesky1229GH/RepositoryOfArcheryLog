package com.example.archerylog.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CustomWheelDatePickerDialog(
    initialDateMillis: Long,
    onDismiss: () -> Unit,
    onConfirmDate: (Long) -> Unit
) {
    val calendar = Calendar.getInstance().apply { timeInMillis = initialDateMillis }
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    
    val years = (2020..currentYear + 5).toList()
    val months = (1..12).toList()
    
    var selectedYear by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableStateOf(calendar.get(Calendar.MONTH) + 1) } // 1-12
    var selectedDay by remember { mutableStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }

    // Number of days in selected month
    val daysInMonth = remember(selectedYear, selectedMonth) {
        val cal = Calendar.getInstance()
        cal.set(selectedYear, selectedMonth - 1, 1)
        cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }
    
    // Ensure day doesn't exceed max days
    if (selectedDay > daysInMonth) {
        selectedDay = daysInMonth
    }
    val days = (1..daysInMonth).toList()

    val yearState = rememberPagerState(initialPage = years.indexOf(selectedYear), pageCount = { years.size })
    val monthState = rememberPagerState(initialPage = months.indexOf(selectedMonth), pageCount = { months.size })
    val dayState = rememberPagerState(initialPage = days.indexOf(selectedDay), pageCount = { days.size })

    // Update state variables when pagers snap
    LaunchedEffect(yearState.currentPage) {
        selectedYear = years.getOrElse(yearState.currentPage) { years.first() }
    }
    LaunchedEffect(monthState.currentPage) {
        selectedMonth = months.getOrElse(monthState.currentPage) { months.first() }
    }
    LaunchedEffect(dayState.currentPage, days) {
        selectedDay = days.getOrElse(dayState.currentPage) { days.first() }
    }

    val dayOfWeekFormatter = SimpleDateFormat("EEEE", Locale.getDefault())
    val selectedCal = Calendar.getInstance().apply {
        set(selectedYear, selectedMonth - 1, selectedDay)
    }
    val dayOfWeekStr = dayOfWeekFormatter.format(selectedCal.time)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF2C2C2C),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("キャンセル", color = Color(0xFF4CAF50), fontSize = 16.sp)
                    }
                    Text(dayOfWeekStr, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { onConfirmDate(selectedCal.timeInMillis) }) {
                        Text("完了", color = Color(0xFF4CAF50), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Yesterday / Today Buttons
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                            onConfirmDate(yesterday.timeInMillis)
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4CAF50))
                    ) {
                        Text("昨日")
                    }
                    OutlinedButton(
                        onClick = {
                            val today = Calendar.getInstance()
                            onConfirmDate(today.timeInMillis)
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4CAF50))
                    ) {
                        Text("今日")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Wheels
                Box(
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Highlight pill in center
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(40.dp)
                            .background(Color(0xFF3A3A3A), RoundedCornerShape(8.dp))
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(0.8f),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Year
                        VerticalPager(
                            state = yearState,
                            contentPadding = PaddingValues(vertical = 55.dp),
                            modifier = Modifier.height(150.dp).weight(1f)
                        ) { page ->
                            val text = "${years[page]}年"
                            WheelText(text, isSelected = page == yearState.currentPage)
                        }

                        // Month
                        VerticalPager(
                            state = monthState,
                            contentPadding = PaddingValues(vertical = 55.dp),
                            modifier = Modifier.height(150.dp).weight(1f)
                        ) { page ->
                            val text = "${months[page]}月"
                            WheelText(text, isSelected = page == monthState.currentPage)
                        }

                        // Day
                        VerticalPager(
                            state = dayState,
                            contentPadding = PaddingValues(vertical = 55.dp),
                            modifier = Modifier.height(150.dp).weight(1f)
                        ) { page ->
                            val text = "${days[page]}日"
                            WheelText(text, isSelected = page == dayState.currentPage)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WheelText(text: String, isSelected: Boolean) {
    Box(
        modifier = Modifier.fillMaxWidth().height(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else Color.Gray,
            fontSize = if (isSelected) 22.sp else 18.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}
