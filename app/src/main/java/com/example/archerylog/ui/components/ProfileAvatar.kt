package com.example.archerylog.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import android.graphics.BitmapFactory

@Composable
fun ProfileAvatar(uri: String?, size: Dp, modifier: Modifier = Modifier) {
    if (uri.isNullOrBlank()) {
        Box(
            modifier = modifier.size(size).clip(CircleShape).background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, contentDescription = "Avatar", tint = Color.White, modifier = Modifier.size(size * 0.6f))
        }
    } else {
        val bitmap = remember(uri) {
            try {
                if (uri.startsWith("file://")) {
                    BitmapFactory.decodeFile(uri.removePrefix("file://"))?.asImageBitmap()
                } else null
            } catch (e: Exception) { null }
        }
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Avatar",
                contentScale = ContentScale.Crop,
                modifier = modifier.size(size).clip(CircleShape)
            )
        } else {
            Box(
                modifier = modifier.size(size).clip(CircleShape).background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = "Avatar", tint = Color.White, modifier = Modifier.size(size * 0.6f))
            }
        }
    }
}
