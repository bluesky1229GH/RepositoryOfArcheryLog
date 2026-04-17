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
import com.example.archerylog.ui.utils.L10n
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CustomWheelDatePickerDialog(
    initialDateMillis: Long,
    l10n: L10n,
    onDismiss: () -> Unit,
    onConfirmDate: (Long) -> Unit,
    minDateMillis: Long? = null,
    maxDateMillis: Long? = null
) {
    val initialCalendar = Calendar.getInstance().apply { timeInMillis = initialDateMillis }
    
    val minCal = minDateMillis?.let { Calendar.getInstance().apply { timeInMillis = it } }
    val maxCal = maxDateMillis?.let { Calendar.getInstance().apply { timeInMillis = it } }

    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val defaultMaxYear = currentYear + 10
    
    // 1. Calculate Available Years
    val years = remember(minCal, maxCal) {
        val startYear = minCal?.get(Calendar.YEAR) ?: 2020
        val endYear = maxCal?.get(Calendar.YEAR) ?: defaultMaxYear
        (startYear..endYear).toList()
    }

    var selectedYear by remember { 
        val year = initialCalendar.get(Calendar.YEAR)
        mutableStateOf(year.coerceIn(years.first(), years.last())) 
    }

    // 2. Calculate Available Months for selectedYear
    val months = remember(selectedYear, minCal, maxCal) {
        val minMonth = if (minCal != null && selectedYear == minCal.get(Calendar.YEAR)) {
            minCal.get(Calendar.MONTH) + 1
        } else 1
        
        val maxMonth = if (maxCal != null && selectedYear == maxCal.get(Calendar.YEAR)) {
            maxCal.get(Calendar.MONTH) + 1
        } else 12
        
        (minMonth..maxMonth).toList()
    }

    var selectedMonth by remember(selectedYear) {
        val month = initialCalendar.get(Calendar.MONTH) + 1
        mutableStateOf(month.coerceIn(months.first(), months.last()))
    }

    // Adjust selectedMonth if it goes out of bounds when year changes
    LaunchedEffect(months) {
        if (selectedMonth !in months) {
            selectedMonth = selectedMonth.coerceIn(months.first(), months.last())
        }
    }

    // 3. Calculate Available Days for selectedYear and selectedMonth
    val days = remember(selectedYear, selectedMonth, minCal, maxCal) {
        val cal = Calendar.getInstance()
        cal.set(selectedYear, selectedMonth - 1, 1)
        val maxDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        val minDay = if (minCal != null && selectedYear == minCal.get(Calendar.YEAR) && (selectedMonth - 1) == minCal.get(Calendar.MONTH)) {
            minCal.get(Calendar.DAY_OF_MONTH)
        } else 1
        
        val maxDay = if (maxCal != null && selectedYear == maxCal.get(Calendar.YEAR) && (selectedMonth - 1) == maxCal.get(Calendar.MONTH)) {
            maxCal.get(Calendar.DAY_OF_MONTH)
        } else maxDaysInMonth
        
        (minDay..maxDay).toList()
    }

    var selectedDay by remember(selectedYear, selectedMonth) {
        val day = initialCalendar.get(Calendar.DAY_OF_MONTH)
        mutableStateOf(day.coerceIn(days.first(), days.last()))
    }

    // Adjust selectedDay if it goes out of bounds when month changes
    LaunchedEffect(days) {
        if (selectedDay !in days) {
            selectedDay = selectedDay.coerceIn(days.first(), days.last())
        }
    }

    val yearState = rememberPagerState(initialPage = years.indexOf(selectedYear).coerceAtLeast(0), pageCount = { years.size })
    val monthState = rememberPagerState(initialPage = months.indexOf(selectedMonth).coerceAtLeast(0), pageCount = { months.size })
    val dayState = rememberPagerState(initialPage = days.indexOf(selectedDay).coerceAtLeast(0), pageCount = { days.size })

    // Update state variables and PagerState sync
    LaunchedEffect(yearState.currentPage) {
        val newYear = years.getOrElse(yearState.currentPage) { years.first() }
        if (selectedYear != newYear) {
            selectedYear = newYear
        }
    }
    
    LaunchedEffect(monthState.currentPage) {
        val newMonth = months.getOrElse(monthState.currentPage) { months.first() }
        if (selectedMonth != newMonth) {
            selectedMonth = newMonth
        }
    }
    
    LaunchedEffect(dayState.currentPage) {
        val newDay = days.getOrElse(dayState.currentPage) { days.first() }
        if (selectedDay != newDay) {
            selectedDay = newDay
        }
    }

    // Handle index shifts when list contents change (e.g. going from year with 12 months to year with 3 months)
    LaunchedEffect(selectedMonth, months) {
        val targetPage = months.indexOf(selectedMonth)
        if (targetPage != -1 && targetPage != monthState.currentPage) {
            monthState.scrollToPage(targetPage)
        }
    }
    
    LaunchedEffect(selectedDay, days) {
        val targetPage = days.indexOf(selectedDay)
        if (targetPage != -1 && targetPage != dayState.currentPage) {
            dayState.scrollToPage(targetPage)
        }
    }

    val dayOfWeekFormatter = SimpleDateFormat("EEEE", l10n.pickerLocale)
    val selectedCal = Calendar.getInstance().apply {
        set(selectedYear, selectedMonth - 1, selectedDay)
    }
    val dayOfWeekStr = dayOfWeekFormatter.format(selectedCal.time)

    // Check if quick buttons are valid
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    
    val isTodayValid = (minDateMillis == null || today.timeInMillis >= minDateMillis) && 
                       (maxDateMillis == null || today.timeInMillis <= maxDateMillis)
    val isYesterdayValid = (minDateMillis == null || yesterday.timeInMillis >= minDateMillis) && 
                           (maxDateMillis == null || yesterday.timeInMillis <= maxDateMillis)

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
                        Text(l10n.pickerCancel, color = Color(0xFF4CAF50), fontSize = 16.sp)
                    }
                    Text(dayOfWeekStr, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { onConfirmDate(selectedCal.timeInMillis) }) {
                        Text(l10n.pickerDone, color = Color(0xFF4CAF50), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Yesterday / Today Buttons
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
                ) {
                    if (isYesterdayValid) {
                        OutlinedButton(
                            onClick = { onConfirmDate(yesterday.timeInMillis) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4CAF50))
                        ) {
                            Text(l10n.pickerYesterday)
                        }
                    }
                    if (isTodayValid) {
                        OutlinedButton(
                            onClick = { onConfirmDate(today.timeInMillis) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4CAF50))
                        ) {
                            Text(l10n.pickerToday)
                        }
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
                            modifier = Modifier.height(150.dp).weight(1.2f)
                        ) { page ->
                            val text = "${years[page]}${l10n.pickerYearSuffix}"
                            WheelText(text, isSelected = page == yearState.currentPage)
                        }

                        // Month
                        VerticalPager(
                            state = monthState,
                            contentPadding = PaddingValues(vertical = 55.dp),
                            modifier = Modifier.height(150.dp).weight(1.5f)
                        ) { page ->
                            val text = if (l10n.pickerMonthNames.isNotEmpty()) {
                                l10n.pickerMonthNames.getOrElse(months[page] - 1) { "" }
                            } else {
                                "${months[page]}${l10n.pickerMonthSuffix}"
                            }
                            WheelText(text, isSelected = page == monthState.currentPage)
                        }

                        // Day
                        VerticalPager(
                            state = dayState,
                            contentPadding = PaddingValues(vertical = 55.dp),
                            modifier = Modifier.height(150.dp).weight(1f)
                        ) { page ->
                            val text = "${days[page]}${l10n.pickerDaySuffix}"
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
