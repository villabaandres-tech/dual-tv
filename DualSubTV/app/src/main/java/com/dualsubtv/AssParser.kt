package com.dualsubtv

object AssParser {

    fun parse(content: String): List<SubtitleEntry> {
        val entries = mutableListOf<SubtitleEntry>()
        var inEvents = false
        var formatLine: List<String> = emptyList()
        var textIndex = -1
        var startIndex = -1
        var endIndex = -1

        for (line in content.lines()) {
            val trimmed = line.trim()

            when {
                trimmed.equals("[Events]", ignoreCase = true) -> {
                    inEvents = true
                }
                inEvents && trimmed.startsWith("Format:", ignoreCase = true) -> {
                    formatLine = trimmed.removePrefix("Format:").split(",").map { it.trim() }
                    startIndex = formatLine.indexOf("Start")
                    endIndex   = formatLine.indexOf("End")
                    textIndex  = formatLine.indexOf("Text")
                }
                inEvents && trimmed.startsWith("Dialogue:", ignoreCase = true) -> {
                    if (textIndex < 0 || startIndex < 0 || endIndex < 0) continue

                    val values = trimmed.removePrefix("Dialogue:")
                        .split(",", limit = formatLine.size)
                        .map { it.trim() }

                    if (values.size <= textIndex) continue

                    val startMs = parseAssTime(values[startIndex])
                    val endMs   = parseAssTime(values[endIndex])
                    val rawText = values[textIndex]

                    // Remove ASS override tags like {\an8}, {\i1}, etc.
                    val text = rawText.replace(Regex("\\{[^}]*\\}"), "")
                        .replace("\\N", "\n")
                        .replace("\\n", "\n")
                        .trim()

                    if (text.isNotEmpty() && endMs > startMs) {
                        entries.add(SubtitleEntry(startMs, endMs, text))
                    }
                }
            }
        }

        return entries.sortedBy { it.startMs }
    }

    // Format: H:MM:SS.cc  (centiseconds)
    private fun parseAssTime(time: String): Long {
        return try {
            val parts = time.trim().split(":", ".")
            if (parts.size < 4) return 0L
            val h  = parts[0].toLong()
            val m  = parts[1].toLong()
            val s  = parts[2].toLong()
            val cs = parts[3].toLong() // centiseconds
            h * 3_600_000L + m * 60_000L + s * 1_000L + cs * 10L
        } catch (e: Exception) {
            0L
        }
    }
}
