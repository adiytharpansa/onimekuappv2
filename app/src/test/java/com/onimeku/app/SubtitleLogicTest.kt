package com.onimeku.app

import com.onimeku.app.data.ApiSubtitle
import com.onimeku.app.ui.SubLang
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtitleLogicTest {
    private fun sub(lang: String, label: String = "") = ApiSubtitle(lang = lang, label = label, url = "u")

    @Test fun legacyIndonesianCodeIsRecognised() {
        assertEquals("id", SubLang.code("in", ""))
        assertEquals("id", SubLang.code("ind", ""))
        assertEquals("en", SubLang.code("eng", ""))
        assertNull(SubLang.code("ja", "Japanese"))
    }

    @Test fun partialTracksAreDetected() {
        assertTrue(SubLang.isPartial(sub("en", "English - Signs & Songs")))
        assertTrue(SubLang.isPartial(sub("en", "English [Forced]")))
        assertFalse(SubLang.isPartial(sub("en", "English")))
        assertFalse(SubLang.isPartial(sub("id", "Indonesian")))
    }

    @Test fun autoPickPrefersFullDialogueOverSignsAndSongs() {
        val tracks = listOf(sub("en", "English - Signs & Songs"), sub("en", "English"), sub("id", "Indonesian"))
        assertEquals(1, SubLang.pick(tracks, "auto", "en"))   // trek dialog penuh, bukan Signs & Songs
        assertEquals(2, SubLang.pick(tracks, "auto", "id"))
        assertEquals(2, SubLang.pick(tracks, "id", "en"))
    }

    @Test fun onlyPartialTrackStillUsedWhenNothingElse() {
        assertEquals(0, SubLang.pick(listOf(sub("en", "English - Signs & Songs")), "auto", "en"))
    }

    @Test fun otherLanguagePreferenceIsHonouredThenFallsBack() {
        val tracks = listOf(sub("en", "English"), sub("ar", "Arabic"), sub("ko", "Korean"))
        assertEquals(1, SubLang.pick(tracks, "ar", "en"))
        assertEquals(2, SubLang.pick(tracks, "ko", "id"))
        assertEquals(0, SubLang.pick(tracks, "th", "en"))   // tak ada -> English
    }

    @Test fun keyGivesPersistableLanguage() {
        assertEquals("en", SubLang.key(sub("en", "English")))
        assertEquals("id", SubLang.key(sub("", "Bahasa Indonesia")))
        assertEquals("ar", SubLang.key(sub("ara", "Arabic")))
        assertNull(SubLang.key(sub("", "")))
    }

    @Test fun displayDistinguishesVariants() {
        assertEquals("English", SubLang.display(sub("en", "English"), 0))
        assertEquals("English · SDH", SubLang.display(sub("en", "English (SDH)"), 1))
        assertEquals("Indonesian", SubLang.display(sub("id", "Indonesian"), 2))
        assertEquals("Arabic", SubLang.display(sub("ar", "Arabic"), 3))
    }
    @Test fun restoreKeepsUserChoiceAfterReattach() {
        val tracks = listOf(sub("en", "English"), sub("en", "English (SDH)"), sub("id", "Indonesian"))
        // Pengguna memilih "English (SDH)"; pasang ulang karena geseran tidak boleh mengembalikannya ke trek pertama.
        assertEquals(1, SubLang.restore(true, 1, tracks, "en", "en"))
        assertEquals(-1, SubLang.restore(true, -1, tracks, "auto", "en"))   // sengaja dimatikan tetap mati
        // Tanpa keep (muat pertama / daftar trek berubah): ikut preferensi.
        assertEquals(0, SubLang.restore(false, 1, tracks, "en", "en"))
        // Indeks di luar daftar -> jatuh ke preferensi.
        assertEquals(2, SubLang.restore(true, 9, tracks, "id", "en"))
    }
}
