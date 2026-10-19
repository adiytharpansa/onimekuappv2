package com.onimeku.app

import com.onimeku.app.ui.Cue
import com.onimeku.app.ui.CueText
import org.junit.Assert.assertEquals
import org.junit.Test

class CueTextTest {
    @Test fun parsesSrtAndStripsTags() {
        val raw = "1\n00:00:01,000 --> 00:00:03,500\nHello <i>there</i>\n\n2\n00:01:02,250 --> 00:01:04,000\nSecond line\n"
        assertEquals(listOf(Cue(1000, 3500, "Hello there"), Cue(62250, 64000, "Second line")), CueText.parse(raw))
    }

    @Test fun parsesVttWithoutHoursAndDotMillis() {
        val raw = "WEBVTT\n\n00:01.500 --> 00:03.000\nHi\n"
        assertEquals(listOf(Cue(1500, 3000, "Hi")), CueText.parse(raw))
    }

    @Test fun parsesAssDialogueAndLineBreaks() {
        val raw = "[Script Info]\nTitle: x\n\n[Events]\n" +
            "Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text\n" +
            "Dialogue: 0,0:00:01.00,0:00:02.50,Default,,0,0,0,,Hello\\NWorld\n"
        assertEquals(listOf(Cue(1000, 2500, "Hello World")), CueText.parse(raw))
    }

    @Test fun toSrtWritesStandardFormatAndRoundTrips() {
        val cues = listOf(Cue(1000, 3500, "A"), Cue(3661001, 3662000, "B"))
        val srt = CueText.toSrt(cues)
        assertEquals("1\n00:00:01,000 --> 00:00:03,500\nA\n\n2\n01:01:01,001 --> 01:01:02,000\nB\n", srt)
        assertEquals(cues, CueText.parse(srt))
    }

    private fun c(start: Long, end: Long, text: String) = Cue(start, end, text)

    @Test fun groupMergesCutSentencesButNotAcrossLongGaps() {
        val joined = listOf(c(0, 1000, "Hello there,"), c(1200, 2000, "how are you?"), c(5000, 6000, "New sentence."))
        assertEquals(listOf(0..1, 2..2), CueText.group(joined))
        val gap = listOf(c(0, 1000, "Hello,"), c(5000, 6000, "world."))
        assertEquals(listOf(0..0, 1..1), CueText.group(gap))
    }

    @Test fun groupLimitsToThreeLines() {
        val cues = listOf(c(0, 100, "a,"), c(200, 300, "b,"), c(400, 500, "c,"), c(600, 700, "d."))
        assertEquals(listOf(0..2, 3..3), CueText.group(cues))
    }

    @Test fun splitBackDistributesWordsByOriginalLength() {
        val cues = listOf(c(0, 1, "ab"), c(1, 2, "cdef"))
        assertEquals(listOf("one", "two three four"), CueText.splitBack("one two three four", cues, 0..1))
        assertEquals(listOf("single", ""), CueText.splitBack("single", cues, 0..1))
        assertEquals(listOf("whole"), CueText.splitBack("whole", cues, 0..0))
    }

    @Test fun namesKeepsRepeatedCharacterNamesOnly() {
        val cues = listOf(
            c(0, 1, "I saw Mashle today"), c(1, 2, "Then Mashle ran"),
            c(2, 3, "We met on Monday"), c(3, 4, "We left on Monday"), c(4, 5, "Hi Zed there"),
        )
        assertEquals(listOf("Mashle"), CueText.names(cues))   // "Monday" = kata umum, "Zed" hanya sekali
    }
}
