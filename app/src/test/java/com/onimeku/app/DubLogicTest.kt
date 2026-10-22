package com.onimeku.app

import com.onimeku.app.ui.DubLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DubLogicTest {
    @Test fun normalizeCodesAndNames() {
        assertEquals("ja", DubLogic.normalize("jpn"))
        assertEquals("en", DubLogic.normalize("en-US"))
        assertEquals("id", DubLogic.normalize("ind"))
        assertEquals("ja", DubLogic.normalize("Japanese - Stereo"))
        assertEquals("id", DubLogic.normalize("Bahasa Indonesia"))
        assertEquals("id", DubLogic.normalize("Indonesian"))
        assertEquals("", DubLogic.normalize("und"))
        assertEquals("", DubLogic.normalize(null))
        assertEquals("", DubLogic.normalize("  "))
    }

    @Test fun genericLabelsAreDetected() {
        for (g in listOf("Track 1", "track 2", "Audio 1", "Audio Track 3", "Stream", "default", "  ", ""))
            org.junit.Assert.assertTrue(g, DubLogic.isGenericLabel(g))
        for (n in listOf("English", "Japanese", "Bahasa Indonesia", "Japanese - Stereo", "Dub ID"))
            org.junit.Assert.assertFalse(n, DubLogic.isGenericLabel(n))
        org.junit.Assert.assertTrue(DubLogic.isGenericLabel(null))
    }

    @Test fun pickerShownOnlyWhenUseful() {
        org.junit.Assert.assertFalse(DubLogic.showPicker(emptyList()))
        org.junit.Assert.assertFalse(DubLogic.showPicker(listOf("")))      // satu trek tak dikenal ("Track 1")
        org.junit.Assert.assertTrue(DubLogic.showPicker(listOf("ja")))     // satu trek dikenal: tampil tanpa pilihan
        org.junit.Assert.assertTrue(DubLogic.showPicker(listOf("", "")))   // beberapa trek
    }

    @Test fun pickPrefersTitleThenGlobal() {
        val keys = listOf("ja", "en", "id")
        assertEquals(2, DubLogic.pick(keys, "id", "ja"))      // judul menang atas global
        assertEquals(0, DubLogic.pick(keys, null, "ja"))
        assertEquals(1, DubLogic.pick(keys, "ko", "en"))      // judul tak tersedia -> global
        assertNull(DubLogic.pick(keys, null, "auto"))
        assertNull(DubLogic.pick(keys, "", "ko"))             // tak ada yang cocok -> bawaan stream
        assertNull(DubLogic.pick(emptyList(), "ja", "ja"))
    }

    @Test fun fallbackNoticeOnlyWhenChoiceExistsButWantedMissing() {
        assertEquals("id", DubLogic.fallbackNotice(listOf("ja", "en"), null, "id", 0))
        assertNull(DubLogic.fallbackNotice(listOf("ja", "id"), null, "id", 1))      // sudah dipakai
        assertNull(DubLogic.fallbackNotice(listOf("ja"), null, "id", 0))             // satu trek: sub-saja, diam
        assertNull(DubLogic.fallbackNotice(listOf("ja", "en"), null, "auto", 0))
        assertEquals("ko", DubLogic.fallbackNotice(listOf("ja", "en"), "ko", "en", 1)) // judul ingin ko, jatuh ke global en
    }

    @Test fun rememberKeepsLatestAndTrims() {
        val m1 = DubLogic.remember(emptyMap(), "a", "ja")
        val m2 = DubLogic.remember(m1, "b", "id")
        assertEquals(listOf("a", "b"), m2.keys.toList())
        val m3 = DubLogic.remember(m2, "a", "en")                  // diperbarui -> pindah ke akhir
        assertEquals(listOf("b", "a"), m3.keys.toList())
        assertEquals("en", m3["a"])
        val trimmed = DubLogic.remember(DubLogic.remember(m3, "c", "ja", max = 2), "d", "ja", max = 2)
        assertEquals(listOf("c", "d"), trimmed.keys.toList())
        assertEquals(m1, DubLogic.remember(m1, "x", ""))           // kunci kosong tidak disimpan
        assertEquals(m1, DubLogic.remember(m1, "", "ja"))
    }

    @Test fun uniqueLabelsAndSheetOrder() {
        assertEquals(listOf("English", "Japanese", "English 2"), DubLogic.uniqueLabels(listOf("English", "Japanese", "English")))
        assertEquals(listOf(1, 2, 0, 3), DubLogic.sheetOrder(listOf("ja", "en", "id", "ko")))
    }
}
