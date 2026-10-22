package com.onimeku.app

import com.onimeku.app.data.Profile
import com.onimeku.app.data.ProfileRules
import com.onimeku.app.data.ProfileRules.Problem
import com.onimeku.app.data.Sync
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileRulesTest {
    private val two = listOf(Profile("Me"), Profile("Kid"))

    @Test fun cleanTrimsCollapsesAndCaps() {
        assertEquals("a b", ProfileRules.clean("  a    b "))
        assertEquals(12, ProfileRules.clean("x".repeat(30)).length)
    }

    @Test fun rejectsEmptyAndDuplicateIgnoringCase() {
        assertEquals(Problem.EMPTY, ProfileRules.problem("   ", two))
        assertEquals(Problem.DUPLICATE, ProfileRules.problem("kid", two))
        assertNull(ProfileRules.problem("Mom", two))
    }

    @Test fun renameMayKeepOwnNameButNotTakeAnothers() {
        assertNull(ProfileRules.problem("ME", two, exceptIndex = 0))
        assertEquals(Problem.DUPLICATE, ProfileRules.problem("Kid", two, exceptIndex = 0))
    }

    @Test fun fullAtFourProfiles() {
        val four = listOf(Profile("A"), Profile("B"), Profile("C"), Profile("D"))
        assertEquals(Problem.FULL, ProfileRules.problem("E", four))
    }

    @Test fun syncDoesNotResurrectDeletedProfile() {
        val local = listOf(Profile("Me"))
        val remote = listOf(Profile("Me"), Profile("Kid"))
        assertEquals(2, Sync.merge(local, remote).size)
        assertEquals(listOf("Me"), Sync.merge(local, remote, gone = listOf("kid")).map { it.name })
    }
}
