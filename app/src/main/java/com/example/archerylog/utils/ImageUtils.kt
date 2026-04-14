package com.example.archerylog.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object ImageUtils {

    fun saveImageToInternalStorage(context: Context, uri: Uri): String {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return ""
            val file = File(context.filesDir, "avatar_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            "file://${file.absolutePath}"
        } catch(e: Exception) {
            ""
        }
    }

    fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap): String {
        return try {
            val file = File(context.filesDir, "avatar_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.flush()
            outputStream.close()
            "file://${file.absolutePath}"
        } catch (e: Exception) {
            ""
        }
    }
}
