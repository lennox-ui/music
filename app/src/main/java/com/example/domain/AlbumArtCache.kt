package com.example.domain

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import android.util.LruCache
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

object AlbumArtCache {
    private val memoryCache = LruCache<String, Uri>(200)
    private val failedCache = java.util.concurrent.ConcurrentHashMap<String, Boolean>()
    private val retrieverSemaphore = Semaphore(2) // Limit concurrent native-extractions of embedded graphics to prevent CPU starvation

    fun getCachedArt(audioContentUri: Uri?): Uri? {
        if (audioContentUri == null) return null
        val cached = memoryCache.get(audioContentUri.toString())
        return if (cached == Uri.EMPTY) null else cached
    }

    fun isKnownToFail(uriString: String): Boolean {
        return failedCache[uriString] == true
    }

    fun markAsFailed(uriString: String) {
        if (uriString.isNotEmpty()) {
            failedCache[uriString] = true
        }
    }

    fun hasChecked(audioContentUri: Uri?): Boolean {
        if (audioContentUri == null) return true // no need to check
        return memoryCache.get(audioContentUri.toString()) != null
    }

    suspend fun getAlbumArt(context: Context, audioContentUri: Uri): Uri? = withContext(Dispatchers.IO) {
        val uriString = audioContentUri.toString()
        
        // 1. Check in-memory Cache
        val cachedUri = memoryCache.get(uriString)
        if (cachedUri != null) {
            return@withContext if (cachedUri == Uri.EMPTY) null else cachedUri
        }

        val nameHash = uriString.hashCode()
        val fileName = "album_art_$nameHash.jpg"
        val emptyFileName = "album_art_$nameHash.empty"
        val cacheFile = File(context.cacheDir, fileName)
        val emptyFile = File(context.cacheDir, emptyFileName)
        
        // 2. Check disk-cache for previous failure
        if (emptyFile.exists()) {
            memoryCache.put(uriString, Uri.EMPTY)
            return@withContext null
        }

        // 3. Check disk-cache for previous success
        if (cacheFile.exists()) {
            val fileUri = Uri.fromFile(cacheFile)
            memoryCache.put(uriString, fileUri)
            return@withContext fileUri
        }
        
        // 4. Extract with thread limiting (using Semaphore-gate to prevent scroll lagging)
        try {
            retrieverSemaphore.withPermit {
                // Secondary check inside semaphore zone to avoid duplication
                if (emptyFile.exists()) {
                    memoryCache.put(uriString, Uri.EMPTY)
                    return@withContext null
                }
                if (cacheFile.exists()) {
                    val fileUri = Uri.fromFile(cacheFile)
                    memoryCache.put(uriString, fileUri)
                    return@withContext fileUri
                }

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    try {
                        val bitmap = context.contentResolver.loadThumbnail(audioContentUri, android.util.Size(256, 256), null)
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, FileOutputStream(cacheFile))
                        val fileUri = Uri.fromFile(cacheFile)
                        memoryCache.put(uriString, fileUri)
                        return@withContext fileUri
                    } catch (e: Exception) {
                        // ignore and fallback to MediaMetadataRetriever
                    }
                }
                
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, audioContentUri)
                val bytes = retriever.embeddedPicture
                retriever.release()
                
                if (bytes != null) {
                    FileOutputStream(cacheFile).use { it.write(bytes) }
                    val fileUri = Uri.fromFile(cacheFile)
                    memoryCache.put(uriString, fileUri)
                    return@withContext fileUri
                } else {
                    // Mark as empty persistently so we never query MediaMetadataRetriever for this track again
                    try {
                        emptyFile.createNewFile()
                    } catch (ioe: Exception) {
                        // ignore
                    }
                    memoryCache.put(uriString, Uri.EMPTY)
                }
            }
        } catch (e: Exception) {
            memoryCache.put(uriString, Uri.EMPTY)
        }
        
        return@withContext null
    }
}
