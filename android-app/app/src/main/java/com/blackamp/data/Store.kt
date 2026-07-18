package com.blackamp.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * Simple JSON-file persistence. Playlists and repo sources are small, so a
 * database would be overkill; this also means an export is literally the same
 * shape we already store.
 */
class Store(private val context: Context) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val file: File get() = File(context.filesDir, "blackamp-data.json")

    suspend fun load(): BackupBundle = withContext(Dispatchers.IO) {
        runCatching {
            if (!file.exists()) return@runCatching BackupBundle()
            json.decodeFromString<BackupBundle>(file.readText())
        }.getOrElse { BackupBundle() }
    }

    suspend fun save(bundle: BackupBundle) = withContext(Dispatchers.IO) {
        runCatching { file.writeText(json.encodeToString(BackupBundle.serializer(), bundle)) }
        Unit
    }

    /** Write a backup to a user-chosen location (Storage Access Framework). */
    suspend fun exportTo(out: OutputStream, bundle: BackupBundle) = withContext(Dispatchers.IO) {
        out.use { it.write(json.encodeToString(BackupBundle.serializer(), bundle).toByteArray()) }
    }

    /** Read a backup the user picked. Returns null if it isn't a valid bundle. */
    suspend fun importFrom(input: InputStream): BackupBundle? = withContext(Dispatchers.IO) {
        runCatching {
            val text = input.use { it.readBytes().decodeToString() }
            json.decodeFromString<BackupBundle>(text)
        }.getOrNull()
    }

    companion object {
        fun defaultExportName(): String {
            val stamp = java.text.SimpleDateFormat("yyyy-MM-dd-HHmm", java.util.Locale.US)
                .format(java.util.Date())
            return "blackamp-backup-$stamp.json"
        }
    }
}
