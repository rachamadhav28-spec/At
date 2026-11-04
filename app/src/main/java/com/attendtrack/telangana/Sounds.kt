package com.attendtrack.telangana

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Plays a random sound when a period is marked.
 *
 * 1. Valid audio files in res/raw are used first (picked at random).
 * 2. Files that are damaged (see [isValidAudio]) are skipped.
 * 3. If no usable file exists, a random built-in sound effect is generated instead,
 *    so there is always a sound. Present gets a happy one, Absent a funny "fail" one.
 */
object Sounds {
    private var player: MediaPlayer? = null

    /** Master switch for the Present / Absent sounds (More screen). Saved on the phone. */
    var enabled by mutableStateOf(true)
        private set

    fun load(ctx: Context) {
        enabled = ctx.getSharedPreferences("attendtrack", 0).getBoolean("sounds_on", true)
    }

    fun setEnabled(ctx: Context, on: Boolean) {
        enabled = on
        ctx.getSharedPreferences("attendtrack", 0).edit().putBoolean("sounds_on", on).apply()
        if (!on) {
            player?.release()
            player = null
        }
    }
    private const val RATE = 22050
    private const val TWO_PI = 6.2831855f
    private val rnd = Random(System.nanoTime())
    private val cache = HashMap<String, ShortArray>()
    private val validCache = HashMap<Int, Boolean>()
    private var lastKey: String? = null

    private fun allSounds(): List<Int> =
        R.raw::class.java.fields
            .filter { it.name != "readme" }
            .mapNotNull { f -> runCatching { f.getInt(null) }.getOrNull() }

    /** A damaged file has its bytes replaced by the UTF-8 "replacement character" (EF BF BD). */
    private fun isValidAudio(ctx: Context, id: Int): Boolean = validCache.getOrPut(id) {
        runCatching {
            ctx.resources.openRawResource(id).use { input ->
                val b = ByteArray(8192)
                val n = input.read(b)
                if (n < 64) return@use false
                for (i in 0 until n - 2) {
                    if (b[i] == 0xEF.toByte() && b[i + 1] == 0xBF.toByte() && b[i + 2] == 0xBD.toByte()) {
                        return@use false
                    }
                }
                true
            }
        }.getOrDefault(false)
    }

    fun play(ctx: Context, present: Boolean, pct: Double) {
        if (!enabled) return
        var ids = allSounds().filter { isValidAudio(ctx, it) }.shuffled()
        // never play the same file twice in a row (when there is a choice)
        if (ids.size > 1 && "raw${ids[0]}" == lastKey) ids = ids.drop(1) + ids[0]
        for (id in ids) {
            val mp = try {
                MediaPlayer.create(ctx, id)
            } catch (e: Exception) {
                null
            }
            if (mp != null) {
                lastKey = "raw$id"
                player?.release()
                player = mp
                mp.setOnCompletionListener {
                    it.release()
                    if (player === it) player = null
                }
                mp.setOnErrorListener { p, _, _ ->
                    p.release()
                    if (player === p) player = null
                    synth(present)
                    true
                }
                mp.start()
                return
            }
        }
        synth(present)
    }

    // ---------- built-in sound effects ----------

    private fun gen(name: String, seconds: Float, f: (Float) -> Float): ShortArray =
        cache.getOrPut(name) {
            val n = (seconds * RATE).toInt()
            val out = ShortArray(n)
            for (i in 0 until n) {
                out[i] = (f(i / RATE.toFloat()).coerceIn(-1f, 1f) * 30000f).toInt().toShort()
            }
            out
        }

    private fun tone(t: Float, hz: Float) = sin(TWO_PI * hz * t)

    private val happy: List<() -> ShortArray> = listOf(
        { // coin
            gen("coin", 0.35f) { t ->
                val hz = if (t < 0.08f) 988f else 1319f
                tone(t, hz) * exp(-6f * (if (t < 0.08f) t else t - 0.08f)) * 0.7f
            }
        },
        { // ding
            gen("ding", 0.7f) { t ->
                (tone(t, 880f) * 0.6f + tone(t, 1760f) * 0.3f + tone(t, 2640f) * 0.12f) * exp(-5f * t)
            }
        },
        { // pop
            gen("pop", 0.18f) { t ->
                var ph = 0f
                val hz = 260f + 900f * exp(-30f * t)
                ph = TWO_PI * hz * t
                sin(ph) * exp(-22f * t) * 0.9f
            }
        },
        { // happy arpeggio
            gen("arp", 0.5f) { t ->
                val notes = floatArrayOf(523f, 659f, 784f, 1047f)
                val k = (t / 0.11f).toInt().coerceIn(0, 3)
                val lt = t - k * 0.11f
                tone(t, notes[k]) * exp(-7f * lt) * 0.7f
            }
        }
    )

    private val funny: List<() -> ShortArray> = listOf(
        { // boing
            gen("boing", 0.6f) { t ->
                val hz = 140f + 420f * exp(-4f * t) + 40f * sin(TWO_PI * 18f * t)
                sin(TWO_PI * hz * t) * exp(-4.5f * t) * 0.85f
            }
        },
        { // bass drop thud
            val r = Random(9)
            gen("thud", 0.7f) { t ->
                val hz = 38f + 80f * exp(-9f * t)
                val body = sin(TWO_PI * hz * t) * exp(-4f * t)
                val click = (r.nextFloat() * 2f - 1f) * exp(-60f * t) * 0.4f
                body * 0.95f + click
            }
        },
        { // sad trombone
            gen("womp", 0.9f) { t ->
                val notes = floatArrayOf(311f, 294f, 277f, 247f)
                val k = (t / 0.22f).toInt().coerceIn(0, 3)
                val hz = if (k == 3) 247f - 25f * ((t - 0.66f) / 0.24f).coerceIn(0f, 1f) else notes[k]
                val wob = 1f + 0.02f * sin(TWO_PI * 6f * t)
                val env = (1f - (t / 0.9f)).coerceAtLeast(0f)
                (sin(TWO_PI * hz * wob * t) * 0.6f + sin(TWO_PI * hz * 2f * t) * 0.25f) * env
            }
        },
        { // buzzer
            gen("buzz", 0.35f) { t ->
                val sq = if (sin(TWO_PI * 120f * t) >= 0f) 1f else -1f
                sq * 0.45f * (1f - t / 0.35f)
            }
        }
    )

    private fun synth(present: Boolean) {
        val pool = if (present) happy else funny
        var i = rnd.nextInt(pool.size)
        // never repeat the previous built-in sound back to back
        if (lastKey == "syn${present}$i") i = (i + 1 + rnd.nextInt(pool.size - 1)) % pool.size
        lastKey = "syn${present}$i"
        playBuffer(pool[i]())
    }

    private fun playBuffer(buf: ShortArray) {
        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
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
}
