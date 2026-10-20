package com.onimeku.app

import com.onimeku.app.data.DoorMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DoorMathTest {

    @Test fun dirAndFileName() {
        assertEquals(
            "https://cdn.jsdelivr.net/gh/u/gd-01@abc/127230/1/q720/",
            DoorMath.dirOf("https://cdn.jsdelivr.net/gh/u/gd-01@abc/127230/1/q720/q720.m3u8"),
        )
        assertEquals("seg-0000.ts", DoorMath.fileNameOf("https://x.test/a/b/seg-0000.ts?tok=1"))
        assertEquals("q720.m3u8", DoorMath.fileNameOf("https://x.test/a/q720.m3u8"))
    }

    @Test fun resolveRelativeSegmentAndVariant() {
        val master = "https://cdn.jsdelivr.net/gh/u/gd-01@abc/127230/1/master.m3u8"
        assertEquals(
            "https://cdn.jsdelivr.net/gh/u/gd-01@abc/127230/1/q720/q720.m3u8",
            DoorMath.resolve(master, "q720/q720.m3u8"),
        )
        val variant = "https://cdn.jsdelivr.net/gh/u/gd-01@abc/127230/1/q720/q720.m3u8"
        assertEquals(
            "https://cdn.jsdelivr.net/gh/u/gd-01@abc/127230/1/q720/seg-0000.ts",
            DoorMath.resolve(variant, "seg-0000.ts"),
        )
        // URI absolut dibiarkan apa adanya; ../ naik satu direktori.
        assertEquals("https://lain.test/x.ts", DoorMath.resolve(variant, "https://lain.test/x.ts"))
        assertEquals(
            "https://cdn.jsdelivr.net/gh/u/gd-01@abc/127230/1/subs/id.vtt",
            DoorMath.resolve(variant, "../subs/id.vtt"),
        )
    }

    @Test fun canonicalDirAcrossDoorShapes() {
        val expect = "127230/1/q720/"
        assertEquals(
            expect,
            DoorMath.canonicalDir("https://cdn.jsdelivr.net/gh/adiytharpansa/gd-01@ecedc62/127230/1/q720/seg-0000.ts"),
        )
        assertEquals(
            expect,
            DoorMath.canonicalDir("https://onimeku-cdn.adiyth-arpansa.workers.dev/gd-01@ecedc62/127230/1/q720/seg-0000.ts"),
        )
        assertEquals(
            expect,
            DoorMath.canonicalDir("https://adiytharpansa.github.io/gd-01/127230/1/q720/seg-0000.ts"),
        )
        assertEquals(
            expect,
            DoorMath.canonicalDir("https://rawcdn.githack.com/adiytharpansa/gd-01/ecedc62/127230/1/q720/seg-0000.ts"),
        )
        // Playlist tangga itu sendiri juga memetakan ke direktori yang sama.
        assertEquals(
            expect,
            DoorMath.canonicalDir("https://cdn.jsdelivr.net/gh/adiytharpansa/gd-01@ecedc62/127230/1/q720/q720.m3u8"),
        )
    }

    @Test fun canonicalDirRejectsNonEpisodePaths() {
        // Poster tingkat judul: sesudah id tidak ada segmen episode angka.
        assertNull(DoorMath.canonicalDir("https://cdn.jsdelivr.net/gh/u/gd-01@abc/127230/poster.jpg"))
        assertNull(DoorMath.canonicalDir("https://example.com/video/seg-0000.ts"))
        // master.m3u8 berada di direktori episode itu sendiri -> kanonisnya direktori episode.
        assertEquals(
            "127230/1/",
            DoorMath.canonicalDir("https://cdn.jsdelivr.net/gh/u/gd-01@abc/127230/1/master.m3u8"),
        )
    }

    @Test fun rotationOrderWrapsOnce() {
        assertEquals(listOf(0, 1, 2), DoorMath.rotationOrder(3, 0))
        assertEquals(listOf(2, 0, 1), DoorMath.rotationOrder(3, 2))
        assertEquals(listOf(1, 2, 0), DoorMath.rotationOrder(3, 4))
        assertEquals(emptyList<Int>(), DoorMath.rotationOrder(0, 0))
    }

    @Test fun splitRoundRobinDistributesEvenly() {
        val parts = DoorMath.splitRoundRobin((1..10).toList(), 4)
        assertEquals(4, parts.size)
        assertEquals(listOf(1, 5, 9), parts[0])
        assertEquals(listOf(2, 6, 10), parts[1])
        assertEquals(listOf(3, 7), parts[2])
        assertEquals(listOf(4, 8), parts[3])
        assertEquals(listOf(1, 2, 3), DoorMath.splitRoundRobin(listOf(1, 2, 3), 1).single())
    }

    private val master = """
        #EXTM3U
        #EXT-X-STREAM-INF:BANDWIDTH=5000000,RESOLUTION=1920x1080
        q1080/q1080.m3u8
        #EXT-X-STREAM-INF:BANDWIDTH=2500000,RESOLUTION=1280x720
        q720/q720.m3u8
        #EXT-X-STREAM-INF:BANDWIDTH=900000,RESOLUTION=854x480
        q480/q480.m3u8
    """.trimIndent()

    @Test fun parseVariantsAndChoose() {
        val url = "https://cdn.jsdelivr.net/gh/u/gd-01@abc/127230/1/master.m3u8"
        val variants = DoorMath.parseVariants(master, url)
        assertEquals(listOf(1080, 720, 480), variants.map { it.height })
        assertTrue(variants[1].uri.endsWith("/127230/1/q720/q720.m3u8"))
        assertEquals(1080, DoorMath.chooseVariant(variants, "1080p")?.height)
        assertEquals(720, DoorMath.chooseVariant(variants, "Auto")?.height)
        assertEquals(480, DoorMath.chooseVariant(variants, "480p")?.height)
        // Kualitas diminta tidak ada -> tertinggi di bawahnya.
        val noHd = variants.filter { it.height < 1080 }
        assertEquals(720, DoorMath.chooseVariant(noHd, "1080p")?.height)
    }

    @Test fun parseVariantsIgnoresIframeEntries() {
        val withIframe = """
            #EXTM3U
            #EXT-X-STREAM-INF:BANDWIDTH=2500000,RESOLUTION=1280x720
            q720/q720.m3u8
            #EXT-X-I-FRAME-STREAM-INF:BANDWIDTH=86000,RESOLUTION=1280x720,URI="q720/iframes.m3u8"
            #EXT-X-STREAM-INF:BANDWIDTH=900000,RESOLUTION=854x480
            q480/q480.m3u8
        """.trimIndent()
        val url = "https://cdn.jsdelivr.net/gh/u/gd-01@abc/127230/1/master.m3u8"
        val variants = DoorMath.parseVariants(withIframe, url)
        assertEquals(listOf(720, 480), variants.map { it.height })
        assertTrue(variants[1].uri.endsWith("/q480/q480.m3u8"))
    }

    @Test fun parseSegmentsSkipsComments() {
        val variant = """
            #EXTM3U
            #EXT-X-TARGETDURATION:4
            #EXTINF:4.0,
            seg-0000.ts
            #EXTINF:4.0,
            seg-0001.ts
            #EXT-X-ENDLIST
        """.trimIndent()
        val segs = DoorMath.parseSegments(variant, "https://h.test/127230/1/q720/q720.m3u8")
        assertEquals(
            listOf(
                "https://h.test/127230/1/q720/seg-0000.ts",
                "https://h.test/127230/1/q720/seg-0001.ts",
            ),
            segs,
        )
    }
}
