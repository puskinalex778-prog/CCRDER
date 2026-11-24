package com.nfcpay.simulator.data

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.sin

/**
 * Offline synthesized terminal sound and haptic feedback manager.
 * Zero external audio assets or network dependencies.
 */
class FeedbackManager(context: Context) {

    private val appContext = context.applicationContext
    private val audioExecutor = Executors.newSingleThreadExecutor()

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun playKeyTap(vibrationEnabled: Boolean) {
        if (!vibrationEnabled) return
        vibrateOneShot(durationMs = 14L, amplitude = 55)
    }

    fun playCardDetected(soundEnabled: Boolean, vibrationEnabled: Boolean) {
        if (vibrationEnabled) {
            vibrateOneShot(durationMs = 35L, amplitude = 140)
        }
        if (soundEnabled) {
            playTonesAsync(listOf(ToneSegment(freqHz = 740.0, durationMs = 70, gain = 0.25)))
        }
    }

    fun playSuccess(soundEnabled: Boolean, vibrationEnabled: Boolean) {
        if (vibrationEnabled) {
            vibrateWaveform(
                timings = longArrayOf(0L, 45L, 55L, 85L),
                amplitudes = intArrayOf(0, 150, 0, 210)
            )
        }
        if (soundEnabled) {
            // Crisp dual-tone terminal confirmation chime (A5 -> E6)
            playTonesAsync(
                listOf(
                    ToneSegment(freqHz = 880.0, durationMs = 95, gain = 0.30),
                    ToneSegment(freqHz = 0.0, durationMs = 20, gain = 0.0),
                    ToneSegment(freqHz = 1318.5, durationMs = 155, gain = 0.32)
                )
            )
        }
    }

    fun playError(soundEnabled: Boolean, vibrationEnabled: Boolean) {
        if (vibrationEnabled) {
            vibrateWaveform(
                timings = longArrayOf(0L, 60L, 60L, 60L),
                amplitudes = intArrayOf(0, 180, 0, 180)
            )
        }
        if (soundEnabled) {
            playTonesAsync(
                listOf(
                    ToneSegment(freqHz = 330.0, durationMs = 110, gain = 0.25),
                    ToneSegment(freqHz = 0.0, durationMs = 35, gain = 0.0),
                    ToneSegment(freqHz = 260.0, durationMs = 140, gain = 0.25)
                )
            )
        }
    }

    private fun vibrateOneShot(durationMs: Long, amplitude: Int) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        try {
            val effect = if (v.hasAmplitudeControl()) {
                VibrationEffect.createOneShot(durationMs, amplitude.coerceIn(1, 255))
            } else {
                VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
            }
            v.vibrate(effect)
        } catch (_: SecurityException) {
            // Ignore if vibration permission is restricted by OEM policy
        }
    }

    private fun vibrateWaveform(timings: LongArray, amplitudes: IntArray) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        try {
            val effect = if (v.hasAmplitudeControl()) {
                VibrationEffect.createWaveform(timings, amplitudes, -1)
            } else {
                VibrationEffect.createWaveform(timings, -1)
            }
            v.vibrate(effect)
        } catch (_: SecurityException) {
            // Ignore if vibration permission is restricted by OEM policy
        }
    }

    private data class ToneSegment(
        val freqHz: Double,
        val durationMs: Int,
        val gain: Double
    )

    private fun playTonesAsync(segments: List<ToneSegment>) {
        audioExecutor.execute(Runnable {
            val sampleRate = 44100
            val totalSamples = segments.sumOf { (sampleRate * it.durationMs) / 1000 }
            if (totalSamples <= 0) return@Runnable

            val pcm = ShortArray(totalSamples)
            var cursor = 0
            for (seg in segments) {
                val count = (sampleRate * seg.durationMs) / 1000
                val fadeSamples = (sampleRate * 8) / 1000 // 8ms smooth envelope
                for (i in 0 until count) {
                    if (seg.freqHz <= 0.0) {
                        pcm[cursor++] = 0
                    } else {
                        val env = when {
                            i < fadeSamples -> i.toDouble() / fadeSamples
                            i > count - fadeSamples -> (count - i).toDouble() / fadeSamples
                            else -> 1.0
                        }.coerceIn(0.0, 1.0)
                        val sample = sin(2.0 * PI * seg.freqHz * i / sampleRate) * seg.gain * env
                        pcm[cursor++] = (sample * Short.MAX_VALUE).toInt().toShort()
                    }
                }
            }

            var track: AudioTrack? = null
            try {
                val minBuf = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val byteCount = pcm.size * 2
                track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .setBufferSizeInBytes(maxOf(minBuf, byteCount))
                    .build()

                track.write(pcm, 0, pcm.size)
                track.play()
                val totalDurationMs = segments.sumOf { it.durationMs }.toLong()
                Thread.sleep(totalDurationMs + 40L)
            } catch (_: Throwable) {
                // Non-fatal audio playback fallback
            } finally {
                try {
                    track?.stop()
                    track?.release()
                } catch (_: Throwable) {
                    // Ignore release error
                }
            }
        })
    }
}
