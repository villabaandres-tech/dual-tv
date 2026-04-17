package com.dualsubtv

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL

object SubtitleLoader {

    suspend fun load(context: Context, source: String): List<SubtitleEntry> =
        withContext(Dispatchers.IO) {
            try {
                val content = when {
                    source.startsWith("http://") || source.startsWith("https://") -> {
                        URL(source).readText(Charsets.UTF_8)
                    }
                    source.startsWith("content://") -> {
                        val uri = Uri.parse(source)
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).readText()
                        } ?: ""
                    }
                    else -> {
                        // Local file path
                        java.io.File(source).readText(Charsets.UTF_8)
                    }
                }

                when {
                    source.endsWith(".ass", ignoreCase = true) ||
                    source.endsWith(".ssa", ignoreCase = true) -> AssParser.parse(content)
                    else -> SrtParser.parse(content) // default .srt
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }

    /**
     * Find the subtitle entry active at [positionMs].
     */
    fun findActiveEntry(entries: List<SubtitleEntry>, positionMs: Long): SubtitleEntry? {
        return entries.firstOrNull { positionMs in it.startMs..it.endMs }
    }
}
