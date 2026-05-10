package com.example.archerylog.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.security.MessageDigest

object AvatarCacheManager {
    
    private const val CACHE_DIR = "avatar_cache"

    /**
     * Converts a URL to a safe, unique filename using MD5 hash.
     */
    private fun getCacheKey(url: String): String {
        return try {
            val bytes = MessageDigest.getInstance("MD5").digest(url.toByteArray())
            bytes.joinToString("") { "%02x".format(it) } + ".jpg"
        } catch (e: Exception) {
            "default_cache.jpg"
        }
    }

    /**
     * Gets the local cache file path.
     */
    fun getCachedFile(context: Context, url: String): File {
        val dir = File(context.cacheDir, CACHE_DIR)
        if (!dir.exists()) dir.mkdirs()
        return File(dir, getCacheKey(url))
    }

    /**
     * Checks if a local cache exists for the given URL.
     */
    fun isCached(context: Context, url: String): Boolean {
        return getCachedFile(context, url).exists()
    }

    /**
     * Downloads and saves an image to the local cache (Pre-warming).
     */
    suspend fun downloadToCache(context: Context, url: String): File? = withContext(Dispatchers.IO) {
        if (url.isBlank() || !url.startsWith("http")) return@withContext null
        
        val cacheFile = getCachedFile(context, url)
        
        // If file already exists, return it directly
        if (cacheFile.exists()) return@withContext cacheFile

        try {
            val connection = URL(url).openConnection()
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            val input = connection.getInputStream()
            val bitmap = BitmapFactory.decodeStream(input)
            
            if (bitmap != null) {
                FileOutputStream(cacheFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                return@withContext cacheFile
            }
        } catch (e: Exception) {
            android.util.Log.e("AvatarCache", "Pre-warm download failed: ${e.message}")
        }
        null
    }
}
