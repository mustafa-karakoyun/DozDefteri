package app.dozdefteri.export

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

/** Where a backup was written: its URI and the folder path shown to the user. */
data class SavedFile(val uri: Uri, val displayPath: String)

/**
 * File I/O without storage permissions: reports go to the app cache and are shared through [FileProvider];
 * backups go to Downloads/DozDefteri through MediaStore (Android 10+) or a user-picked document.
 */
@Singleton
class DocumentIo @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun write(uri: Uri, block: (OutputStream) -> Unit) = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openOutputStream(uri, "wt") ?: throw IOException("Cannot open $uri")
        stream.use(block)
    }

    suspend fun readText(uri: Uri): String = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openInputStream(uri) ?: throw IOException("Cannot open $uri")
        stream.use { it.readBytes().toString(Charsets.UTF_8).removePrefix("﻿") }
    }

    /** Writes a report into the cache and returns a content URI other apps can read when shared. */
    suspend fun writeShareable(fileName: String, block: (OutputStream) -> Unit): Uri = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, REPORTS_DIR).apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, fileName)
        file.outputStream().use(block)
        FileProvider.getUriForFile(context, "${context.packageName}$AUTHORITY_SUFFIX", file)
    }

    /** Saves a backup into Downloads/DozDefteri; returns null below Android 10 (the caller asks for a location). */
    suspend fun saveToDownloads(fileName: String, bytes: ByteArray): SavedFile? = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@withContext null
        saveWithMediaStore(fileName, bytes)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveWithMediaStore(fileName: String, bytes: ByteArray): SavedFile {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, BACKUP_MIME)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$BACKUP_FOLDER")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("Cannot create $fileName")
        try {
            resolver.openOutputStream(uri, "w")?.use { it.write(bytes) } ?: throw IOException("Cannot open $uri")
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        } catch (e: IOException) {
            resolver.delete(uri, null, null)
            throw e
        }
        // MediaStore may rename duplicates ("yedek-… (1).ddb"); report the name it actually used.
        val savedName = resolver.query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        } ?: fileName
        return SavedFile(uri, "${Environment.DIRECTORY_DOWNLOADS}/$BACKUP_FOLDER/$savedName")
    }

    companion object {
        const val BACKUP_MIME = "application/octet-stream"
        const val BACKUP_FOLDER = "DozDefteri"
        const val AUTHORITY_SUFFIX = ".files"
        private const val REPORTS_DIR = "reports"
    }
}
