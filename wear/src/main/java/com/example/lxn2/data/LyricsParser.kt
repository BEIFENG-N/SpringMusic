package com.example.lxn2.data

private data class RawLyricLine(val timeMs: Long, val text: String)

/**
 * LRC 歌词解析器。
 *
 * 支持格式：`[mm:ss.xx]` / `[mm:ss.xxx]` 时间标签。
 * 支持主歌词与翻译歌词合并、去重及根据 showTranslation 开关选择性提取翻译。
 */
object LyricsParser {

    private val timeTagRegex = "\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})]".toRegex()

    fun parse(
        lrcContent: String,
        tlyricContent: String? = null,
        showTranslation: Boolean = true
    ): List<LyricLine> {
        val rawLines = mutableListOf<RawLyricLine>()

        // 1. 解析主 LRC 歌词
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
                rawLines.add(RawLyricLine(totalTimeMs, text))
            }
        }

        // 按时间戳分组
        val timeMap = mutableMapOf<Long, MutableList<String>>()
        rawLines.forEach { line ->
            timeMap.getOrPut(line.timeMs) { mutableListOf() }.add(line.text)
        }

        // 2. 若开启翻译且存在独立的翻译 LRC 文本，解析并合并
        if (showTranslation && !tlyricContent.isNullOrBlank()) {
            tlyricContent.lines().forEach { line ->
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
                    
                    timeMap[totalTimeMs]?.let { list ->
                        if (list.isNotEmpty() && !list.contains(text)) {
                            if (list.size == 1) list.add(text)
                        }
                    }
                }
            }
        }

        // 3. 构建包含可选翻译的 LyricLine 列表（按时间戳升序排序）
        val result = mutableListOf<LyricLine>()
        timeMap.keys.sorted().forEach { time ->
            val texts = timeMap[time] ?: return@forEach
            if (texts.isEmpty()) return@forEach

            val mainText = texts[0]
            val translationText = if (showTranslation && texts.size > 1) texts[1] else null

            result.add(LyricLine(timeMs = time, text = mainText, translation = translationText))
        }

        return result
    }
}
