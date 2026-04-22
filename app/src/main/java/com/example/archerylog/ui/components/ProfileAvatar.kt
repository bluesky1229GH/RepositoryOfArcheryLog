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
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import java.net.URL

@Composable
fun ProfileAvatar(uri: String?, size: Dp, modifier: Modifier = Modifier) {
    var bitmapState by remember(uri) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(uri) {
        if (uri.isNullOrBlank()) {
            bitmapState = null
            return@LaunchedEffect
        }
        
        withContext(Dispatchers.IO) {
            try {
                val bitmap = if (uri.startsWith("file://")) {
                    BitmapFactory.decodeFile(uri.removePrefix("file://"))
                } else if (uri.startsWith("http")) {
                    val connection = URL(uri).openConnection()
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    val input = connection.getInputStream()
                    BitmapFactory.decodeStream(input)
                } else null
                
                bitmapState = bitmap
            } catch (e: Exception) {
                bitmapState = null
            }
        }
    }

    if (bitmapState != null) {
        Image(
            bitmap = bitmapState!!.asImageBitmap(),
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
