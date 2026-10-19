package com.onimeku.app

import com.onimeku.app.data.ApiSubtitle
import com.onimeku.app.data.Playback
import com.onimeku.app.ui.SubLang
import com.onimeku.app.ui.SubMenuEntry
import com.onimeku.app.ui.fmt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerLogicTest {
    @Test fun fmtMinutesAndHours() {
        assertEquals("0:00", fmt(0))
        assertEquals("1:05", fmt(65))
        assertEquals("23:55", fmt(1435))
        assertEquals("1:02:05", fmt(3725))   // film > 1 jam tidak lagi tampil "62:05"
        assertEquals("0:00", fmt(-5))
    }

    private fun sub(lang: String, label: String = "") = ApiSubtitle(lang = lang, label = label, url = "u")

    @Test fun subtitlePickHonoursPreferenceAndFallsBack() {
        val both = listOf(sub("en", "English"), sub("id", "Indonesian"))
        assertEquals(-1, SubLang.pick(both, "off", "en"))
        assertEquals(-1, SubLang.pick(emptyList(), "auto", "en"))
        assertEquals(0, SubLang.pick(both, "auto", "en"))   // app English -> English dulu
        assertEquals(1, SubLang.pick(both, "auto", "id"))   // app Indonesia -> Indonesia dulu
        assertEquals(1, SubLang.pick(both, "id", "en"))
        // bahasa pilihan tak ada -> bahasa kedua, lalu trek pertama
        assertEquals(0, SubLang.pick(listOf(sub("en")), "id", "id"))
        assertEquals(0, SubLang.pick(listOf(sub("ja", "Japanese")), "auto", "en"))
    }

    @Test fun subtitleMenuOrderIsAutoEnglishIndonesianOthersOff() {
        val tracks = listOf(sub("ar", "Arabic"), sub("fr", "French"), sub("en", "English"))
        // Indonesian belum ada -> tetap tampil tepat di bawah English; Off paling bawah
        assertEquals(
            listOf(SubMenuEntry.Auto, SubMenuEntry.Track(2), SubMenuEntry.IndonesianPending, SubMenuEntry.Track(0), SubMenuEntry.Track(1), SubMenuEntry.Off),
            SubLang.menu(tracks),
        )
    }

    @Test fun subtitleMenuUsesRealIndonesianTrackWhenPresent() {
        val tracks = listOf(sub("id", "Indonesia (terjemahan)"), sub("en", "English"))
        assertEquals(
            listOf(SubMenuEntry.Auto, SubMenuEntry.Track(1), SubMenuEntry.Track(0), SubMenuEntry.Off),
            SubLang.menu(tracks),
        )
    }

    @Test fun subtitleMenuWithNoTracksStillOffersIndonesianAndOff() {
        assertEquals(listOf(SubMenuEntry.Auto, SubMenuEntry.IndonesianPending, SubMenuEntry.Off), SubLang.menu(emptyList()))
    }

    @Test fun playbackOutroDefaultsToNull() {
        val p = Playback()
        assertNull(p.outroStart); assertNull(p.outroEnd); assertNull(p.introEnd)
    }
}
