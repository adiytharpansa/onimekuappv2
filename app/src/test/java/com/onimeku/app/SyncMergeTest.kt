package com.onimeku.app

import com.onimeku.app.data.Profile
import com.onimeku.app.data.Settings
import com.onimeku.app.data.Sync
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncMergeTest {
    @Test fun newerRemoteDeletionWins() {
        val local = Profile("Me", myList = listOf("a", "b"), myListTs = mapOf("a" to 10L))
        val remote = Profile("Me", myList = listOf("b"), myListTs = mapOf("a" to 20L))
        assertEquals(listOf("b"), Sync.merge(listOf(local), listOf(remote)).single().myList)
    }

    @Test fun newerLocalReAddWins() {
        val local = Profile("Me", myList = listOf("a"), myListTs = mapOf("a" to 30L))
        val remote = Profile("Me", myList = emptyList(), myListTs = mapOf("a" to 20L))
        assertEquals(listOf("a"), Sync.merge(listOf(local), listOf(remote)).single().myList)
    }

    @Test fun legacyWithoutStampsStaysUnion() {
        val local = Profile("Me", myList = listOf("a"))
        val remote = Profile("Me", myList = listOf("b"))
        assertEquals(setOf("a", "b"), Sync.merge(listOf(local), listOf(remote)).single().myList.toSet())
    }

    @Test fun newerRemoteSettingsWin() {
        val local = Settings(subLang = "en")
        val remote = Settings(subLang = "id")
        val (merged, ts) = Sync.mergeSettings(local, 100L, remote, 200L)
        assertEquals("id", merged.subLang)
        assertEquals(200L, ts)
    }

    @Test fun newerLocalSettingsWin() {
        val local = Settings(subLang = "id")
        val remote = Settings(subLang = "en")
        val (merged, ts) = Sync.mergeSettings(local, 300L, remote, 200L)
        assertEquals("id", merged.subLang)
        assertEquals(300L, ts)
    }

    @Test fun equalSettingsStampKeepsLocal() {
        val local = Settings(subLang = "id")
        val remote = Settings(subLang = "en")
        val (merged, ts) = Sync.mergeSettings(local, 200L, remote, 200L)
        assertEquals("id", merged.subLang)
        assertEquals(200L, ts)
    }

    @Test fun legacyRemoteWithoutSettingsKeepsLocal() {
        val local = Settings(subLang = "id")
        val (merged, ts) = Sync.mergeSettings(local, 50L, null, 0L)
        assertEquals("id", merged.subLang)
        assertEquals(50L, ts)
    }

    @Test fun unmarkedEpisodeStaysUnseenAfterMerge() {
        val local = Profile("Me", seen = emptyList(), seenTs = mapOf("x:1" to 50L))
        val remote = Profile("Me", seen = listOf("x:1"), seenTs = mapOf("x:1" to 40L))
        assertEquals(emptyList<String>(), Sync.merge(listOf(local), listOf(remote)).single().seen)
    }
}
