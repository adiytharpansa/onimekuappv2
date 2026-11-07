package com.onimeku.app

import com.onimeku.app.data.SubRenditions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pembacaan rendition subtitle dari playlist master gudang (dasar perbaikan "subtitle Indonesia tidak tampil"). */
class SubRenditionsTest {

    private val master = """
        #EXTM3U
        #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="aud",NAME="Japanese",LANGUAGE="ja",DEFAULT=YES,URI="audio/ja.m3u8"
        #EXT-X-MEDIA:TYPE=SUBTITLES,GROUP-ID="subs",NAME="English",LANGUAGE="en",DEFAULT=YES,URI="subs/en.vtt"
        #EXT-X-MEDIA:TYPE=SUBTITLES,GROUP-ID="subs",NAME="Indonesian",LANGUAGE="id",DEFAULT=NO,URI="subs/id.vtt?token=abc"
        #EXT-X-STREAM-INF:BANDWIDTH=800000,RESOLUTION=1280x720,SUBTITLES="subs"
        720/index.m3u8
    """.trimIndent()
    private val base = "https://cdn.example.com/show/1/0/master.m3u8"

    @Test fun readsOnlySubtitleRenditionsInOrder() {
        val r = SubRenditions.parse(master, base)
        assertEquals(2, r.size)
        assertEquals(listOf("en", "id"), r.map { it.lang })
        assertEquals(listOf("English", "Indonesian"), r.map { it.label })
        // TYPE=AUDIO tidak boleh ikut (dulu tidak ada bedanya).
        assertTrue(r.none { it.url.endsWith("ja.m3u8") })
    }

    @Test fun resolvesAgainstMasterDirectoryAndKeepsQuery() {
        val r = SubRenditions.parse(master, base)
        assertEquals("https://cdn.example.com/show/1/0/subs/en.vtt", r[0].url)
        assertEquals("https://cdn.example.com/show/1/0/subs/id.vtt?token=abc", r[1].url)
        assertEquals("subs/en.vtt", r[0].rel)
        assertEquals("subs/id.vtt?token=abc", r[1].rel)
        assertFalse(r[0].playlist)
        assertTrue(r[1].sideLoadable)
    }

    @Test fun playlistRenditionIsNotSideLoadable() {
        val m = """
            #EXTM3U
            #EXT-X-MEDIA:TYPE=SUBTITLES,GROUP-ID="subs",NAME="Indonesian",LANGUAGE="id",URI="subs/id.m3u8"
            #EXT-X-MEDIA:TYPE=SUBTITLES,GROUP-ID="subs",NAME="English",LANGUAGE="en",URI="subs/en/index.m3u8?x=1"
        """.trimIndent()
        val r = SubRenditions.parse(m, base)
        assertEquals(2, r.size)
        assertTrue(r[0].playlist)
        assertFalse(r[0].sideLoadable)
        assertTrue(r[1].playlist) // ekstensi dibaca dari path, bukan query
        assertFalse(r[1].sideLoadable)
    }

    @Test fun absoluteAndProtocolRelativeUrisAreResolved() {
        val m = """
            #EXTM3U
            #EXT-X-MEDIA:TYPE=SUBTITLES,NAME="Indonesian",LANGUAGE="id",URI="https://other.example/x/id.vtt"
            #EXT-X-MEDIA:TYPE=SUBTITLES,NAME="English",LANGUAGE="en",URI="//mirror.example/en.vtt"
        """.trimIndent()
        val r = SubRenditions.parse(m, base)
        assertEquals("https://other.example/x/id.vtt", r[0].url)
        assertNull(r[0].rel) // di luar direktori master
        assertEquals("https://mirror.example/en.vtt", r[1].url)
    }

    @Test fun unquotedTypeAndDuplicateUris() {
        val m = """
            #EXTM3U
            #EXT-X-MEDIA:TYPE=SUBTITLES,GROUP-ID=subs,LANGUAGE=id,NAME=Indonesian,URI=subs/id.vtt
            #EXT-X-MEDIA:TYPE=SUBTITLES,GROUP-ID=subs,LANGUAGE=id,NAME=Indonesian,URI=subs/id.vtt
        """.trimIndent()
        val r = SubRenditions.parse(m, base)
        assertEquals(1, r.size)
        assertEquals("id", r[0].lang)
        assertEquals("Indonesian", r[0].label)
    }

    @Test fun blankInputYieldsNothing() {
        assertTrue(SubRenditions.parse("", base).isEmpty())
        assertTrue(SubRenditions.parse(master, "").isEmpty())
        assertTrue(SubRenditions.parse("#EXTM3U\n720/index.m3u8", base).isEmpty())
    }

    @Test fun legacyIndonesianCodeIsRecognised() {
        val m = """
            #EXTM3U
            #EXT-X-MEDIA:TYPE=SUBTITLES,NAME="English",LANGUAGE="en",URI="subs/en.vtt"
            #EXT-X-MEDIA:TYPE=SUBTITLES,NAME="Bahasa Indonesia",LANGUAGE="in",URI="subs/in.vtt"
        """.trimIndent()
        val r = SubRenditions.parse(m, base)
        assertEquals("en", SubRenditions.firstEnglish(r)?.lang)
        assertEquals("in", SubRenditions.firstIndonesian(r)?.lang)
        assertNull(SubRenditions.firstIndonesian(r.filter { it.lang == "en" }))
    }

    @Test fun attributesWithCommasInsideQuotesStayIntact() {
        val line = """#EXT-X-MEDIA:TYPE=SUBTITLES,GROUP-ID="subs",NAME="Indonesian, SDH",LANGUAGE="id",URI="subs/id.vtt""""
        val a = SubRenditions.attrs(line)
        assertEquals("Indonesian, SDH", a["NAME"])
        assertEquals("subs/id.vtt", a["URI"])
        assertEquals("SUBTITLES", a["TYPE"])
    }
}
