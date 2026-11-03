package com.attendtrack.telangana

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

object IntroSound {
    private const val RATE = 22050
    private val cache = HashMap<String, ShortArray>()
    private const val TWO_PI = 6.2831855f

    private fun make(name: String, seconds: Float, f: (Float) -> Float): ShortArray =
        cache.getOrPut(name) {
            val n = (seconds * RATE).toInt()
            val out = ShortArray(n)
            for (i in 0 until n) {
                val t = i / RATE.toFloat()
                val v = f(t).coerceIn(-1f, 1f)
                out[i] = (v * 32000f).toInt().toShort()
            }
            out
        }

    private fun boomBuf(): ShortArray {
        var phase = 0f
        val rnd = Random(7)
        return make("boom", 1.0f) { t ->
            val freq = 36f + 74f * exp(-7f * t)
            phase += TWO_PI * freq / RATE
            val body = sin(phase) * exp(-3.0f * t)
            val click = (rnd.nextFloat() * 2f - 1f) * exp(-45f * t) * 0.5f
            body * 0.95f + click
        }
    }

    private fun hitBuf(): ShortArray {
        var phase = 0f
        val rnd = Random(5)
        return make("hit", 0.5f) { t ->
            val freq = 48f + 100f * exp(-16f * t)
            phase += TWO_PI * freq / RATE
            val body = sin(phase) * exp(-8f * t)
            val click = (rnd.nextFloat() * 2f - 1f) * exp(-60f * t) * 0.45f
            body * 0.95f + click
        }
    }

    private fun whooshBuf(): ShortArray {
        var y = 0f
        val rnd = Random(11)
        return make("whoosh", 0.8f) { t ->
            val a = 0.03f + 0.5f * (t / 0.8f)
            y += a * ((rnd.nextFloat() * 2f - 1f) - y)
            val s = sin(PI.toFloat() * t / 0.8f)
            y * s * s * 1.6f
        }
    }

    private fun shineBuf(): ShortArray =
        make("shine", 1.4f) { t ->
            val attack = (t / 0.01f).coerceAtMost(1f)
            val env = attack * exp(-2.8f * t)
            val tau = TWO_PI * t
            (sin(tau * 880f) * 0.5f + sin(tau * 1320f) * 0.3f +
                sin(tau * 1760f) * 0.25f + sin(tau * 2640f) * 0.15f) * env * 0.6f
        }

    private fun droneBuf(): ShortArray {
        var phase = 0f
        val rnd = Random(3)
        return make("drone", 1.6f) { t ->
            val p = t / 1.6f
            val freq = 55f + 55f * p
            phase += TWO_PI * freq / RATE
            val env = if (p < 0.75f) p / 0.75f else (1f - p) / 0.25f
            (sin(phase) * 0.7f + sin(phase * 2f) * 0.2f +
                (rnd.nextFloat() * 2f - 1f) * 0.05f) * env * 0.8f
        }
    }

    private fun play(buf: ShortArray) {
        if (Motion.level == 0) return
        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buf.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(buf, 0, buf.size)
            track.play()
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    track.release()
                } catch (e: Exception) {
                }
            }, buf.size * 1000L / RATE + 400L)
        } catch (e: Exception) {
        }
    }

    fun boom() = play(boomBuf())
    fun hit() = play(hitBuf())
    fun whoosh() = play(whooshBuf())
    fun shine() = play(shineBuf())
    fun drone() = play(droneBuf())
}
