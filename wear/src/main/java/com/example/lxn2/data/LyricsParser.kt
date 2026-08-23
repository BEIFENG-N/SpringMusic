package com.example.lxn2.data

/**
 * LRC 歌词解析器。
 *
 * 支持格式：`[mm:ss.xx]` / `[mm:ss.xxx]` 时间标签，同一行可含多个标签，
 * 解析后按时间升序返回；空文本行与非法行为会被跳过。
 */
object LyricsParser {

    private val timeTagRegex = "\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})]".toRegex()

    fun parse(lrcContent: String): List<LyricLine> {
        val lyrics = mutableListOf<LyricLine>()
        lrcContent.lines().forEach { line ->
            val matches = timeTagRegex.findAll(line).toList()
            if (matches.isEmpty()) return@forEach
            val text = line.replace(timeTagRegex, "").trim()
            if (text.isEmpty()) return@forEach

            matches.forEach { match ->
                val min = match.groupValues[1].toLong()
                val sec = match.groupValues[2].toLong()
                val msStr = match.groupValues[3]
                val ms = if (msStr.length == 2) msStr.toLong() * 10 else msStr.toLong()
                val totalTimeMs = (min * 60 + sec) * 1000 + ms
                lyrics.add(LyricLine(totalTimeMs, text))
            }
        }
        return lyrics.sortedBy { it.timeMs }
    }
}
