package com.example.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

object AudioFeedback {
    private val scope = CoroutineScope(Dispatchers.Default)

    fun playTone(freqHz: Double, durationMs: Int = 100, volume: Float = 0.25f) {
        scope.launch {
            try {
                val sampleRate = 44100
                val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
                val buffer = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val time = i.toDouble() / sampleRate
                    // Apply smooth envelope attack and decay to prevent clicking
                    val envelope = when {
                        i < numSamples * 0.1 -> i / (numSamples * 0.1)
                        i > numSamples * 0.8 -> (numSamples - i) / (numSamples * 0.2)
                        else -> 1.0
                    }
                    val sampleVal = sin(2.0 * Math.PI * freqHz * time) * envelope * volume * Short.MAX_VALUE
                    buffer[i] = sampleVal.toInt().toShort()
                }

                val track = AudioTrack.Builder()
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
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(buffer, 0, buffer.size)
                track.play()
                // Clean up after playing
                Thread.sleep(durationMs.toLong() + 50)
                track.release()
            } catch (_: Exception) {
                // Ignore audio errors gracefully
            }
        }
    }

    fun playSuccess() {
        playTone(660.0, 90, 0.25f)
    }

    fun playError() {
        playTone(220.0, 160, 0.3f)
    }

    fun playFanfare() {
        scope.launch {
            playTone(880.0, 120, 0.25f)
            Thread.sleep(130)
            playTone(1046.0, 200, 0.3f)
        }
    }
}
