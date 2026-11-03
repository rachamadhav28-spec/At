package com.attendtrack.telangana

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/** Generated mechanical-keyboard clicks, one per typed letter. No audio files needed. */
object IntroSound {
    private const val RATE = 22050
    private const val TWO_PI = 6.2831855f
    private val cache = HashMap<String, ShortArray>()
    private val rnd = Random(42)

    /** variant 0..3 = slightly different key pitches, 4 = space bar (deeper). */
    private fun keyBuf(variant: Int): ShortArray = cache.getOrPut("key$variant") {
        val seconds = if (variant == 4) 0.09f else 0.06f
        val n = (seconds * RATE).toInt()
        val out = ShortArray(n)
        val r = Random(100 + variant)
        val pitch = if (variant == 4) 190f else 1500f + variant * 260f
        for (i in 0 until n) {
            val t = i / RATE.toFloat()
            // sharp click: filtered noise burst + short resonant tone, then a soft thock
            val click = (r.nextFloat() * 2f - 1f) * exp(-170f * t) * 0.75f
            val tone = sin(TWO_PI * pitch * t) * exp(-95f * t) * 0.55f
            val thock = sin(TWO_PI * (pitch / 5f) * t) * exp(-55f * t) * 0.45f
            out[i] = ((click + tone + thock).coerceIn(-1f, 1f) * 30000f).toInt().toShort()
        }
        out
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
            }, buf.size * 1000L / RATE + 300L)
        } catch (e: Exception) {
        }
    }

    /** Play the click for one typed character. */
    fun key(c: Char) {
        play(keyBuf(if (c == ' ') 4 else rnd.nextInt(4)))
    }
}
