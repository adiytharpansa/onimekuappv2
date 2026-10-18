package com.onimeku.app

import com.onimeku.app.data.Downloads
import com.onimeku.app.data.Last
import com.onimeku.app.data.Profile
import com.onimeku.app.data.Sync
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreTest {
    @Test fun downloadKeyRoundTrip() {
        val k = Downloads.key("one-piece", 12)
        assertEquals("one-piece" to 12, Downloads.parseKey(k))
        assertNull(Downloads.parseKey("bukan-kunci"))
        assertNull(Downloads.parseKey("dl:x"))
    }

    @Test fun mergeUnionsListAndKeepsFurthestProgress() {
        val local = listOf(Profile("Me", myList = listOf("a", "b"), prog = mapOf("a:1" to 100), seen = listOf("b:1"), last = Last("a", 1)))
        val remote = listOf(
            Profile("Me", myList = listOf("b", "c"), prog = mapOf("a:1" to 300, "b:1" to 50), seen = listOf("c:1")),
            Profile("Kid", myList = listOf("z")),
        )
        val m = Sync.merge(local, remote)
        assertEquals(listOf("a", "b", "c"), m[0].myList)
        assertEquals(300, m[0].prog["a:1"])
        assertTrue("b:1" !in m[0].prog)          // sudah ditonton -> bukan progres
        assertEquals(setOf("b:1", "c:1"), m[0].seen.toSet())
        assertEquals("Kid", m[1].name)
    }

    @Test fun mergeCapsAtFourProfiles() {
        val local = listOf(Profile("A"), Profile("B"), Profile("C"))
        val remote = listOf(Profile("D"), Profile("E"))
        assertEquals(4, Sync.merge(local, remote).size)
    }
}
