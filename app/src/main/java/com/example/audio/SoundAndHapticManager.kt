package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Procedural Sound & Haptic Engine for Amaze Arrows.
 * Generates relaxing Zen marimba/piano tones, arrow whoosh/pop effects, collision buzzes,
 * victory fanfares, and device haptic feedback without requiring external audio files.
 */
class SoundAndHapticManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var musicJob: Job? = null

    @Volatile
    var soundEnabled: Boolean = true

    @Volatile
    var vibrationEnabled: Boolean = true

    @Volatile
    var musicEnabled: Boolean = true
        set(value) {
            field = value
            if (!value) stopAmbientMusic()
        }

    // Pentatonic scale frequencies (C4, D4, E4, G4, A4, C5, D5, E5, G5)
    private val pentatonicNotes = doubleArrayOf(
        261.63, 293.66, 329.63, 392.00, 440.00, 523.25, 587.33, 659.25, 783.99
    )

    fun playArrowPreviewSelect() {
        if (vibrationEnabled) {
            vibrateClick(8L)
        }
        if (!soundEnabled) return
        scope.launch {
            playTone(520.0, 45, 0.08)
        }
    }

    fun playArrowEscape(comboCount: Int) {
        if (vibrationEnabled) {
            vibrateClick(14L)
        }
        if (!soundEnabled) return
        scope.launch {
            val noteIndex = (comboCount - 1).coerceIn(0, pentatonicNotes.size - 1)
            val baseFreq = pentatonicNotes[noteIndex]
            playSlidingWhooshAndChime(baseFreq, durationMs = 115)
        }
    }

    fun playArrowBlocked() {
        if (vibrationEnabled) {
            vibrateError()
        }
        if (!soundEnabled) return
        scope.launch {
            playCollisionImpactBuzz()
        }
    }

    fun playHintChime() {
        if (vibrationEnabled) {
            vibrateClick(12L)
        }
        if (!soundEnabled) return
        scope.launch {
            playTone(523.25, 85, 0.12)
            delay(55)
            playTone(659.25, 110, 0.14)
        }
    }

    fun playUndoSound() {
        if (vibrationEnabled) {
            vibrateClick(10L)
        }
        if (!soundEnabled) return
        scope.launch {
            playTone(392.0, 70, 0.10)
            delay(45)
            playTone(329.63, 85, 0.10)
        }
    }

    fun playVictoryFanfare() {
        if (vibrationEnabled) {
            vibrateVictoryCelebration()
        }
        if (!soundEnabled) return
        scope.launch {
            val notes = doubleArrayOf(392.00, 493.88, 587.33, 783.99)
            for (freq in notes) {
                playTone(freq, 115, 0.15)
                delay(90)
            }
        }
    }

    private fun vibrateVictoryCelebration() {
        try {
            val v = getVibrator() ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Soft celebratory 3-pulse light haptic feedback on level completion
                v.vibrate(
                    VibrationEffect.createWaveform(
                        longArrayOf(0, 22, 45, 25, 45, 40),
                        intArrayOf(0, 95, 0, 120, 0, 150),
                        -1
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(55L)
            }
        } catch (_: Throwable) {
        }
    }

    fun startAmbientMusicIfNeeded() {
        if (!musicEnabled) return
        if (musicJob?.isActive == true) return
        musicJob = scope.launch {
            delay(600L)
            val ambientSequence = doubleArrayOf(
                261.63, 329.63, 392.00, 523.25,
                293.66, 392.00, 440.00, 587.33,
                329.63, 392.00, 523.25, 659.25
            )
            var idx = 0
            while (isActive && musicEnabled) {
                val freq = ambientSequence[idx % ambientSequence.size]
                playSoftBellNote(freq, durationMs = 620, volume = 0.05)
                idx++
                delay(1350)
            }
        }
    }

    fun stopAmbientMusic() {
        musicJob?.cancel()
        musicJob = null
    }

    fun release() {
        stopAmbientMusic()
    }

    /**
     * Synthesizes a very soft, light, pleasant silk-like glide note when an arrow slides out.
     * No harsh noise or high frequencies — gentle and relaxing for repeated play.
     */
    private suspend fun playSlidingWhooshAndChime(freq: Double, durationMs: Int) {
        try {
            val sampleRate = 22050
            val numSamples = (sampleRate * durationMs) / 1000
            val samples = ShortArray(numSamples)
            // Keep frequency in a warm, soothing mid-range
            val warmFreq = (freq * 0.68).coerceIn(240.0, 520.0)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val progress = i.toDouble() / numSamples
                // Smooth sine-window attack and soft exponential decay (zero click, zero harshness)
                val window = sin(PI * progress) * exp(-t * 12.0)
                val glideFreq = warmFreq * (0.96 + 0.14 * progress)
                val wave = sin(2.0 * PI * glideFreq * t)
                val value = (wave * window * 0.11 * Short.MAX_VALUE).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                samples[i] = value.toShort()
            }
            writeAudioSamples(samples, sampleRate, durationMs)
        } catch (_: Throwable) {
            // Ignore audio hardware issues on headless environments
        }
    }

    /**
     * Synthesizes a soft, subtle, low-volume muted tap when an arrow bumps into an obstacle.
     */
    private suspend fun playCollisionImpactBuzz() {
        try {
            val sampleRate = 22050
            val durationMs = 110
            val numSamples = (sampleRate * durationMs) / 1000
            val samples = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val progress = i.toDouble() / numSamples
                val env = sin(PI * progress) * exp(-t * 14.0)
                val dropFreq = 145.0 - 35.0 * progress
                val wave = sin(2.0 * PI * dropFreq * t)
                val value = (wave * env * 0.14 * Short.MAX_VALUE).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                samples[i] = value.toShort()
            }
            writeAudioSamples(samples, sampleRate, durationMs)
        } catch (_: Throwable) {
        }
    }

    private suspend fun playTone(freq: Double, durationMs: Int, volume: Double) {
        try {
            val sampleRate = 22050
            val numSamples = (sampleRate * durationMs) / 1000
            val samples = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val progress = i.toDouble() / numSamples
                val env = sin(PI * progress) * exp(-t * 13.0)
                val wave = sin(2.0 * PI * freq * t)
                val value = (wave * env * volume * Short.MAX_VALUE).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                samples[i] = value.toShort()
            }
            writeAudioSamples(samples, sampleRate, durationMs)
        } catch (_: Throwable) {
        }
    }

    private suspend fun playSoftBellNote(freq: Double, durationMs: Int, volume: Double) {
        try {
            val sampleRate = 22050
            val numSamples = (sampleRate * durationMs) / 1000
            val samples = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val attack = (i.toDouble() / (sampleRate * 0.02)).coerceAtMost(1.0)
                val decay = exp(-t * 4.5)
                val wave = sin(2.0 * PI * freq * t) + 0.15 * sin(3.0 * PI * freq * t)
                val value = (wave * attack * decay * volume * Short.MAX_VALUE).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                samples[i] = value.toShort()
            }
            writeAudioSamples(samples, sampleRate, durationMs)
        } catch (_: Throwable) {
        }
    }

    private suspend fun writeAudioSamples(samples: ShortArray, sampleRate: Int, durationMs: Int) {
        val bufferSize = samples.size * 2
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize.coerceAtLeast(1024))
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        try {
            track.write(samples, 0, samples.size)
            track.play()
            delay(durationMs.toLong() + 20L)
            track.stop()
        } finally {
            track.release()
        }
    }

    private fun getVibrator(): Vibrator? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun vibrateClick(durationMs: Long) {
        try {
            val v = getVibrator() ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(durationMs)
            }
        } catch (_: Throwable) {
        }
    }

    private fun vibrateError() {
        try {
            val v = getVibrator() ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(
                    VibrationEffect.createWaveform(
                        longArrayOf(0, 45, 35, 65),
                        intArrayOf(0, 180, 0, 220),
                        -1
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(90L)
            }
        } catch (_: Throwable) {
        }
    }
}
