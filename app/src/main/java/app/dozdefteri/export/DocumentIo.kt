package app.dozdefteri.export

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

/** Storage Access Framework I/O for documents the user picked; needs no storage permission. */
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
}
