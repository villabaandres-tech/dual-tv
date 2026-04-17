package com.dualsubtv

object SrtParser {

    fun parse(content: String): List<SubtitleEntry> {
        val entries = mutableListOf<SubtitleEntry>()
        // Split by double newline (subtitle blocks)
        val blocks = content.trim().split(Regex("\\n\\s*\\n"))

        for (block in blocks) {
            val lines = block.trim().lines()
            if (lines.size < 3) continue

            // Line 0: index number (skip)
            // Line 1: timestamps  "00:00:01,000 --> 00:00:04,000"
            val timeLine = lines[1].trim()
            val timeMatch = Regex(
                """(\d{2}):(\d{2}):(\d{2})[,.](\d{3})\s*-->\s*(\d{2}):(\d{2}):(\d{2})[,.](\d{3})"""
            ).find(timeLine) ?: continue

            val (h1, m1, s1, ms1, h2, m2, s2, ms2) = timeMatch.destructured
            val startMs = toMs(h1.toLong(), m1.toLong(), s1.toLong(), ms1.toLong())
            val endMs   = toMs(h2.toLong(), m2.toLong(), s2.toLong(), ms2.toLong())

            // Lines 2+: subtitle text (may span multiple lines)
            val text = lines.drop(2).joinToString("\n")
                .replace(Regex("<[^>]+>"), "") // strip HTML tags like <i>, <b>
                .trim()

            if (text.isNotEmpty()) {
                entries.add(SubtitleEntry(startMs, endMs, text))
            }
        }

        return entries.sortedBy { it.startMs }
    }

    private fun toMs(h: Long, m: Long, s: Long, ms: Long): Long {
        return h * 3_600_000L + m * 60_000L + s * 1_000L + ms
    }
}
