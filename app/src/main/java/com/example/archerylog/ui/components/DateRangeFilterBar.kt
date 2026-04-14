package com.example.archerylog.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.archerylog.ui.utils.L10n
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DateRangeFilterBar(
    l10n: L10n,
    startDateMillis: Long?,
    endDateMillis: Long?,
    onStartDateClick: () -> Unit,
    onEndDateClick: () -> Unit,
    onClearStartDate: () -> Unit,
    onClearEndDate: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Select correct locale for date format
    val locale = if (l10n.language == com.example.archerylog.ui.utils.AppLanguage.JAPANESE) Locale.JAPANESE else Locale.getDefault()
    
    val yearFormat = SimpleDateFormat("yyyy年", locale)
    val monthDayFormat = SimpleDateFormat("M月d日 (E)", locale)

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFF202020),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Calendar Icon
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = "Date Filter",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Start Date
            DateBlock(
                millis = startDateMillis,
                yearFormat = yearFormat,
                monthDayFormat = monthDayFormat,
                placeholder = l10n.startDate,
                onClick = onStartDateClick,
                onClear = onClearStartDate,
                modifier = Modifier.weight(1f)
            )

            // Arrow Separator
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "To",
                tint = Color.Gray,
                modifier = Modifier.size(20.dp).padding(horizontal = 4.dp)
            )

            // End Date
            DateBlock(
                millis = endDateMillis,
                yearFormat = yearFormat,
                monthDayFormat = monthDayFormat,
                placeholder = l10n.endDate,
                onClick = onEndDateClick,
                onClear = onClearEndDate,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DateBlock(
    millis: Long?,
    yearFormat: SimpleDateFormat,
    monthDayFormat: SimpleDateFormat,
    placeholder: String,
    onClick: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (millis != null) yearFormat.format(Date(millis)) else "---",
                color = Color.LightGray,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp
            )
            Text(
                text = if (millis != null) monthDayFormat.format(Date(millis)) else placeholder,
                color = if (millis != null) Color.White else Color.Gray,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp
            )
        }
        
        if (millis != null) {
            Box(
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(22.dp)
                    .background(Color.Gray.copy(alpha = 0.7f), CircleShape)
                    .clip(CircleShape)
                    .clickable { onClear() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Clear",
                    tint = Color.Black,
                    modifier = Modifier.size(12.dp)
                )
            }
        } else {
            // Invisible placeholder to keep the layout stable
            Spacer(modifier = Modifier.size(22.dp))
        }
    }
}
