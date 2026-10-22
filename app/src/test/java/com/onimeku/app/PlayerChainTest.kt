package com.onimeku.app

import androidx.media3.common.PlaybackException
import com.onimeku.app.data.LogRedact
import com.onimeku.app.ui.ChainStep
import com.onimeku.app.ui.EpisodeMath
import com.onimeku.app.ui.PlaybackChain
import com.onimeku.app.ui.SkipLogic
import com.onimeku.app.ui.StallPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Logika murni pemutar yang dulu tertanam di composable (rantai cadangan, navigasi episode, skip, pengawas macet, log). */
class PlayerChainTest {
    // ---- rantai cadangan ----
    @Test fun gudangMovesThroughDoorsThenDies() {
        assertEquals(ChainStep.NextDoor(1), PlaybackChain.onFatal(3, 0, false, 0, true))
        assertEquals(ChainStep.NextDoor(2), PlaybackChain.onFatal(3, 1, false, 0, true))
        assertEquals(ChainStep.Dead, PlaybackChain.onFatal(3, 2, false, 0, true))
        // satu-satunya pintu: langsung habis
        assertEquals(ChainStep.Dead, PlaybackChain.onFatal(1, 0, false, 0, true))
    }

    @Test fun nonGudangRetriesMainThenDirectThenDies() {
        assertEquals(ChainStep.RetryMain, PlaybackChain.onFatal(0, 0, false, 0, true))
        assertEquals(ChainStep.UseDirect, PlaybackChain.onFatal(0, 0, false, 1, true))
        assertEquals(ChainStep.Dead, PlaybackChain.onFatal(0, 0, false, 1, false))
        assertEquals(ChainStep.Dead, PlaybackChain.onFatal(0, 0, true, 1, true))
    }

    @Test fun transientErrorsAreRetriedInPlace() {
        assertTrue(PlaybackChain.isTransient(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED, null))
        assertTrue(PlaybackChain.isTransient(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT, null))
        assertTrue(PlaybackChain.isTransient(PlaybackException.ERROR_CODE_IO_UNSPECIFIED, null))
        assertTrue(PlaybackChain.isTransient(PlaybackException.ERROR_CODE_TIMEOUT, null))
        assertTrue(PlaybackChain.isTransient(PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS, 503))
        assertTrue(PlaybackChain.isTransient(PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS, 429))
    }

    @Test fun deadTokensAndDecodeErrorsGoStraightToFallback() {
        assertFalse(PlaybackChain.isTransient(PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS, 403))
        assertFalse(PlaybackChain.isTransient(PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS, 404))
        assertFalse(PlaybackChain.isTransient(PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS, null))
        assertFalse(PlaybackChain.isTransient(PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED, null))
        assertFalse(PlaybackChain.isTransient(PlaybackException.ERROR_CODE_DECODING_FAILED, null))
    }

    // ---- pengawas macet ----
    @Test fun stallPolicyTimingAndQualityDrop() {
        assertEquals(12_000L, StallPolicy.delayMs(0))
        assertEquals(15_000L, StallPolicy.delayMs(1))
        assertEquals(15_000L, StallPolicy.delayMs(2))
        assertFalse(StallPolicy.shouldLower(1, 480))
        assertTrue(StallPolicy.shouldLower(2, 480))
        assertFalse(StallPolicy.shouldLower(2, null))
    }

    // ---- navigasi episode ----
    @Test fun nextEpisodeUsesAvailableListStrictly() {
        assertEquals(2, EpisodeMath.next(listOf(1, 2, 3), 12, 1))
        assertEquals(4, EpisodeMath.next(listOf(2, 4, 7), 12, 2))
        // episode terakhir yang tersedia: tidak boleh lompat ke nomor yang belum tayang
        assertNull(EpisodeMath.next(listOf(1, 2, 3), 12, 3))
        // tanpa daftar: nomor+1 sampai total
        assertEquals(6, EpisodeMath.next(emptyList(), 12, 5))
        assertNull(EpisodeMath.next(emptyList(), 12, 12))
    }

    @Test fun stepMovesWithinAvailableOrTotal() {
        assertEquals(1, EpisodeMath.step(listOf(1, 2, 3), 3, 2, -1))
        assertNull(EpisodeMath.step(listOf(1, 2, 3), 3, 1, -1))
        assertEquals(7, EpisodeMath.step(listOf(2, 4, 7), 12, 4, 1))
        assertEquals(4, EpisodeMath.step(emptyList(), 5, 3, 1))
        assertNull(EpisodeMath.step(emptyList(), 5, 5, 1))
        assertNull(EpisodeMath.step(emptyList(), 5, 1, -1))
    }

    @Test fun resumeOfferedFromThirtySeconds() {
        assertFalse(EpisodeMath.offerResume(29))
        assertTrue(EpisodeMath.offerResume(30))
    }

    // ---- lewati intro ----
    @Test fun autoSkipJumpsToIntroEndOnlyInsideIntro() {
        assertEquals(90, SkipLogic.autoSkipTarget(10, 0, 90, true, true))
        assertEquals(90, SkipLogic.autoSkipTarget(88, 0, 90, true, true))
        assertNull(SkipLogic.autoSkipTarget(89, 0, 90, true, true))   // sisa < 1 dtk: biarkan
        assertNull(SkipLogic.autoSkipTarget(95, 0, 90, true, true))   // sudah lewat intro
        assertNull(SkipLogic.autoSkipTarget(10, 30, 90, true, true))  // intro belum mulai
        assertEquals(90, SkipLogic.autoSkipTarget(30, 30, 90, true, true))
        assertNull(SkipLogic.autoSkipTarget(10, 0, 90, true, false))  // opsi otomatis mati
        assertNull(SkipLogic.autoSkipTarget(10, 0, 90, false, true))  // lewati intro dimatikan
        assertNull(SkipLogic.autoSkipTarget(10, 0, null, true, true)) // tanpa penanda
    }

    @Test fun fallbackSkipOnlyForFullEpisodesWithoutMarker() {
        assertTrue(SkipLogic.fallbackVisible(30, false, true, 1400))
        assertFalse(SkipLogic.fallbackVisible(30, true, true, 1400))   // API punya penanda
        assertFalse(SkipLogic.fallbackVisible(10, false, true, 1400))  // terlalu awal
        assertFalse(SkipLogic.fallbackVisible(241, false, true, 1400)) // terlalu lambat
        assertFalse(SkipLogic.fallbackVisible(30, false, true, 300))   // klip pendek
        assertFalse(SkipLogic.fallbackVisible(30, false, true, 0))     // durasi belum diketahui
        assertFalse(SkipLogic.fallbackVisible(30, false, false, 1400)) // opsi mati
    }

    // ---- penyamar log ----
    @Test fun redactDropsQueryButKeepsHostAndPath() {
        assertEquals(
            "GET https://a.example/api/proxy?… -> IOException",
            LogRedact.urls("GET https://a.example/api/proxy?url=https%3A%2F%2Fcdn&ref=x -> IOException"),
        )
        assertEquals("ok https://a.example/x.m3u8 done", LogRedact.urls("ok https://a.example/x.m3u8 done"))
        assertEquals("(https://a.example/p?…)", LogRedact.urls("(https://a.example/p?t=1)"))
    }

    @Test fun redactIsIdempotent() {
        val once = LogRedact.urls("u=https://a.example/p?token=abc")
        assertEquals(once, LogRedact.urls(once))
    }
}
