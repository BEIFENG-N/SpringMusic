package com.example.lxn2.data

import org.junit.Assert.assertEquals
import org.junit.Test

class LyricsParserTest {

    @Test
    fun parsesBasicLines() {
        val lrc = "[00:01.00]第一句\n[00:02.50]第二句\n"
        val result = LyricsParser.parse(lrc)
        assertEquals(2, result.size)
        assertEquals(1000L, result[0].timeMs)
        assertEquals("第一句", result[0].text)
        assertEquals(2500L, result[1].timeMs)
        assertEquals("第二句", result[1].text)
    }

    @Test
    fun skipsEmptyAndInvalidLines() {
        val lrc = "[00:01.00]\n[00:02.50]有歌词\nno tag line\n"
        val result = LyricsParser.parse(lrc)
        assertEquals(1, result.size)
        assertEquals("有歌词", result[0].text)
    }

    @Test
    fun handlesMultipleTagsOnOneLine() {
        val lrc = "[00:01.00][00:03.00]重复副歌\n"
        val result = LyricsParser.parse(lrc)
        assertEquals(2, result.size)
        assertEquals(1000L, result[0].timeMs)
        assertEquals(3000L, result[1].timeMs)
        assertEquals("重复副歌", result[0].text)
    }

    @Test
    fun handlesTwoDigitAndThreeDigitMilliseconds() {
        val lrc = "[00:01.20]a\n[00:02.123]b\n"
        val result = LyricsParser.parse(lrc)
        assertEquals(1200L, result[0].timeMs) // ".20" -> 20 * 10
        assertEquals(2123L, result[1].timeMs)
    }

    @Test
    fun sortsByTimeAscending() {
        val lrc = "[00:05.00]后\n[00:01.00]前\n"
        val result = LyricsParser.parse(lrc)
        assertEquals("前", result[0].text)
        assertEquals("后", result[1].text)
    }
}
