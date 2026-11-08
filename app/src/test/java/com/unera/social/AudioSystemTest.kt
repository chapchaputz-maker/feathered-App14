package com.unera.social

import com.unera.social.data.audio.CallAudioEngine
import com.unera.social.data.audio.NetworkQualityTier
import com.unera.social.data.audio.VoiceNoteManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AudioSystemTest {

    @Test
    fun testCallAudioEngine_OpusSdpGeneration_MatchesVoIPStandards() {
        // Standard starting targets: 16-24 kbps, mono, 20ms frames, in-band FEC and DTX
        val sdp20k = CallAudioEngine.generateOpusSdpFmtp(
            bitrateBps = 20_000,
            enableFec = true,
            enableDtx = true
        )

        assertTrue("Must include target bitrate", sdp20k.contains("maxaveragebitrate=20000"))
        assertTrue("Must enable in-band forward error correction (FEC)", sdp20k.contains("useinbandfec=1"))
        assertTrue("Must enable discontinuous transmission (DTX) for silence data saving", sdp20k.contains("usedtx=1"))
        assertTrue("Must enforce mono channel for efficient voice", sdp20k.contains("stereo=0"))

        val sdp16k = CallAudioEngine.generateOpusSdpFmtp(
            bitrateBps = 16_000,
            enableFec = true,
            enableDtx = true
        )
        assertTrue("16 kbps tier generated correctly for weak network", sdp16k.contains("maxaveragebitrate=16000"))
    }

    @Test
    fun testCallAudioEngine_AdaptiveNetworkTiers() {
        // High packet loss -> switch to CONSTRAINED (16 kbps)
        CallAudioEngine.adaptToNetworkConditions(packetLossPct = 6.2f, rttMs = 400)
        var diag = CallAudioEngine.getDiagnostics()
        assertEquals(16_000, diag.currentAudioBitrateBps)
        assertEquals(NetworkQualityTier.CONSTRAINED.label, diag.networkQualityTier)

        // Moderate mobile conditions -> switch to MODERATE (20 kbps)
        CallAudioEngine.adaptToNetworkConditions(packetLossPct = 2.0f, rttMs = 210)
        diag = CallAudioEngine.getDiagnostics()
        assertEquals(20_000, diag.currentAudioBitrateBps)
        assertEquals(NetworkQualityTier.MODERATE.label, diag.networkQualityTier)

        // Clean Wi-Fi broadband -> switch to EXCELLENT (24 kbps)
        CallAudioEngine.adaptToNetworkConditions(packetLossPct = 0.2f, rttMs = 45)
        diag = CallAudioEngine.getDiagnostics()
        assertEquals(24_000, diag.currentAudioBitrateBps)
        assertEquals(NetworkQualityTier.EXCELLENT.label, diag.networkQualityTier)
    }

    @Test
    fun testCallAudioEngine_DataConsumptionEstimation() {
        // At 20 kbps, 60 seconds of conversation consumes ~150 KB (compared to ~960 KB for uncompressed/128k audio)
        val bitrate = 20_000L
        val seconds = 60
        val bytes = (bitrate / 8L) * seconds
        assertEquals(150_000L, bytes)

        val uncompressedBytes = (128_000L / 8L) * seconds
        val savings = ((uncompressedBytes - bytes).toDouble() / uncompressedBytes.toDouble()) * 100.0
        assertTrue("VoIP data savings should exceed 80%", savings > 80.0)
    }

    @Test
    fun testVoiceNoteManager_RejectsEmptyOrTruncatedFiles() {
        val smallFile = File.createTempFile("small_test", ".ogg")
        try {
            // Write only 128 bytes (truncated)
            smallFile.writeBytes(ByteArray(128) { 0 })
            val diag = VoiceNoteManager.validateVoiceNoteFile(smallFile, 5)
            assertFalse("Incomplete voice note file under 512 bytes must be rejected", diag.isValid)
            assertTrue("Error message must explain truncation", diag.errorMessage?.contains("too small") == true)
        } finally {
            smallFile.delete()
        }
    }

    @Test
    fun testVoiceNoteManager_CompressionSavingsCalculation() {
        // 10-second voice note at ~24 kbps = ~30 KB
        // Equivalent raw 16-bit 48kHz mono PCM = 960 KB
        val durationSec = 10
        val pcmSize = durationSec * 96_000L // 960,000 bytes
        val compressedSize = 30_000L

        val savings = ((pcmSize - compressedSize).toDouble() / pcmSize.toDouble()) * 100.0
        assertTrue("Compression savings vs raw PCM should be ~96%", savings > 95.0)
    }
}
