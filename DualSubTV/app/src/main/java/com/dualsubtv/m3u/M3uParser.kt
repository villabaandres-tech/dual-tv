package com.dualsubtv.m3u

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object M3uParser {

    suspend fun loadFromUrl(urlString: String): List<M3uEntry> = withContext(Dispatchers.IO) {
        try {
            val conn = URL(urlString).openConnection() as HttpURLConnection
            conn.connectTimeout = 15_000
            conn.readTimeout    = 30_000
            conn.setRequestProperty("User-Agent", "DualSubTV/1.0")
            val content = conn.inputStream.bufferedReader(Charsets.UTF_8).readText()
            conn.disconnect()
            parse(content)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun loadFromFile(context: Context, uriOrPath: String): List<M3uEntry> =
        withContext(Dispatchers.IO) {
            try {
                val content = when {
                    uriOrPath.startsWith("content://") -> {
                        context.contentResolver
                            .openInputStream(Uri.parse(uriOrPath))
                            ?.bufferedReader(Charsets.UTF_8)?.readText() ?: ""
                    }
                    else -> java.io.File(uriOrPath).readText(Charsets.UTF_8)
                }
                parse(content)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }

    fun parse(content: String): List<M3uEntry> {
        val entries = mutableListOf<M3uEntry>()
        val lines = content.lines()
        var i = 0

        while (i < lines.size) {
            val line = lines[i].trim()

            if (line.startsWith("#EXTINF")) {
                // Parse attributes from #EXTINF line
                val title     = extractTitle(line)
                val group     = extractAttr(line, "group-title") ?: ""
                val logo      = extractAttr(line, "tvg-logo") ?: ""
                val tvgId     = extractAttr(line, "tvg-id") ?: ""

                // Next non-empty, non-comment line is the URL
                var j = i + 1
                while (j < lines.size && (lines[j].isBlank() || lines[j].trimStart().startsWith("#"))) j++

                if (j < lines.size) {
                    val url = lines[j].trim()
                    if (url.isNotEmpty() && !url.startsWith("#")) {
                        val type = detectType(url, group)
                        entries.add(M3uEntry(title, url, group, logo, tvgId, type))
                        i = j + 1
                        continue
                    }
                }
            }
            i++
        }

        return entries
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun extractTitle(extinf: String): String {
        // Title is everything after the last comma on the #EXTINF line
        val commaIdx = extinf.lastIndexOf(',')
        return if (commaIdx >= 0) extinf.substring(commaIdx + 1).trim() else "Sin título"
    }

    private fun extractAttr(line: String, attr: String): String? {
        // Handles both quoted and unquoted values:  attr="value"  or  attr=value
        val pattern = Regex("""$attr=["']?([^"',\s]+)["']?""", RegexOption.IGNORE_CASE)
        return pattern.find(line)?.groupValues?.get(1)
    }

    private fun detectType(url: String, group: String): ContentType {
        val lUrl   = url.lowercase()
        val lGroup = group.lowercase()

        // Live indicators
        if (lUrl.contains(".m3u8") || lUrl.contains("live") ||
            lUrl.contains("/iptv/") || lGroup.contains("live") ||
            lGroup.contains("canal") || lGroup.contains("channel") ||
            lGroup.contains("tv") || lUrl.endsWith(":8080/")) {
            return ContentType.LIVE
        }

        // VOD indicators
        if (lUrl.contains(".mp4") || lUrl.contains(".mkv") ||
            lUrl.contains(".avi") || lUrl.contains("movie") ||
            lUrl.contains("vod") || lUrl.contains("film") ||
            lGroup.contains("movie") || lGroup.contains("vod") ||
            lGroup.contains("película") || lGroup.contains("serie")) {
            return ContentType.VOD
        }

        return ContentType.UNKNOWN
    }
}
