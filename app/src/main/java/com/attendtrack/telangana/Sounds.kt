package com.attendtrack.telangana

import android.content.Context
import android.media.MediaPlayer

object Sounds {
    private var player: MediaPlayer? = null

    private fun allSounds(): List<Int> =
        R.raw::class.java.fields
            .filter { it.name != "readme" }
            .mapNotNull { f -> runCatching { f.getInt(null) }.getOrNull() }

    fun play(ctx: Context, present: Boolean, pct: Double) {
        for (id in allSounds().shuffled()) {
            val mp = try {
                MediaPlayer.create(ctx, id)
            } catch (e: Exception) {
                null
            }
            if (mp != null) {
                player?.release()
                player = mp
                mp.setOnCompletionListener {
                    it.release()
                    if (player === it) player = null
                }
                mp.start()
                return
            }
        }
    }
}
