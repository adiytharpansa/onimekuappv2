package com.onimeku.app

import com.onimeku.app.ui.SubSync
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubSyncTest {
    @Test fun srtShiftsBothTimesAndKeepsText() {
        val src = "1\n00:00:01,000 --> 00:00:03,500\nHalo\n\n2\n00:01:59,900 --> 00:02:01,000\nSampai jumpa\n"
        val out = SubSync.shift(src, ".srt", 500)
        assertEquals("1\n00:00:01,500 --> 00:00:04,000\nHalo\n\n2\n00:02:00,400 --> 00:02:01,500\nSampai jumpa\n", out)
    }

    @Test fun vttShortAndLongTimestampsAndCueSettings() {
        val src = "WEBVTT\n\n01.000 --> 02.000\nx\n\n00:00:10.000 --> 00:00:12.250 align:start line:90%\ny\n"
        val out = SubSync.shift(src.replace("01.000 --> 02.000", "00:01.000 --> 00:02.000"), ".vtt", 1000)
        assertTrue(out.contains("00:00:02.000 --> 00:00:03.000"))
        assertTrue(out.contains("00:00:11.000 --> 00:00:13.250 align:start line:90%"))
    }

    @Test fun negativeShiftClampsAtZero() {
        val out = SubSync.shift("1\n00:00:00,300 --> 00:00:02,000\nA\n", ".srt", -1000)
        assertTrue(out.contains("00:00:00,000 --> 00:00:01,000"))
    }

    @Test fun zeroOffsetReturnsSameText() {
        val src = "WEBVTT\n\n00:00:01.000 --> 00:00:02.000\nA\n"
        assertEquals(src, SubSync.shift(src, ".vtt", 0))
    }

    @Test fun crlfIsPreserved() {
        val out = SubSync.shift("1\r\n00:00:01,000 --> 00:00:02,000\r\nA\r\n", ".srt", 100)
        assertEquals("1\r\n00:00:01,100 --> 00:00:02,100\r\nA\r\n", out)
    }

    @Test fun textContainingColonsIsNotTouched() {
        val src = "1\n00:00:01,000 --> 00:00:02,000\nPukul 10:30:15,500 tepat\n"
        val out = SubSync.shift(src, ".srt", 1000)
        assertTrue(out.contains("Pukul 10:30:15,500 tepat"))
    }

    @Test fun assShiftsStartAndEndOnly() {
        val src = "[Events]\nFormat: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text\n" +
            "Dialogue: 0,0:00:01.50,0:00:03.00,Default,,0,0,0,,Halo, dunia\n"
        val out = SubSync.shift(src, ".ass", 250)
        assertTrue(out.contains("Dialogue: 0,0:00:01.75,0:00:03.25,Default,,0,0,0,,Halo, dunia"))
    }

    @Test fun clampAndLabel() {
        assertEquals(30_000, SubSync.clamp(99_999))
        assertEquals(-30_000, SubSync.clamp(-99_999))
        assertEquals("0.0 s", SubSync.label(0))
        assertEquals("+0.5 s", SubSync.label(500))
        assertEquals("-1.2 s", SubSync.label(-1200))
    }

    @Test fun pathHelpersRoundTrip() {
        val orig = "/cache/subs/a_1_en_0.srt"
        assertEquals(orig, SubSync.targetPath(orig, 0))
        assertEquals("/cache/subs/a_1_en_0.s500.srt", SubSync.targetPath(orig, 500))
        assertEquals("/cache/subs/a_1_en_0.s-300.srt", SubSync.targetPath(orig, -300))
        assertEquals(orig, SubSync.originalPath("/cache/subs/a_1_en_0.s500.srt"))
        assertEquals(orig, SubSync.originalPath("/cache/subs/a_1_en_0.s-300.srt"))
        assertEquals(orig, SubSync.originalPath(orig))
    }

    @Test fun rememberDropsZeroAndCapsSize() {
        var m = SubSync.remember(emptyMap(), "a", 400)
        assertEquals(400, m["a"])
        m = SubSync.remember(m, "a", 0)
        assertTrue(m.isEmpty())
        var big = emptyMap<String, Int>()
        for (i in 1..5) big = SubSync.remember(big, "t$i", 100, max = 3)
        assertEquals(listOf("t3", "t4", "t5"), big.keys.toList())
    }
}
