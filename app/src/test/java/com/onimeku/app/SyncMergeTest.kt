package com.onimeku.app

import com.onimeku.app.data.Profile
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

    @Test fun unmarkedEpisodeStaysUnseenAfterMerge() {
        val local = Profile("Me", seen = emptyList(), seenTs = mapOf("x:1" to 50L))
        val remote = Profile("Me", seen = listOf("x:1"), seenTs = mapOf("x:1" to 40L))
        assertEquals(emptyList<String>(), Sync.merge(listOf(local), listOf(remote)).single().seen)
    }
}
