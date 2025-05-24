package com.tm.zircon

import android.content.Context
import android.media.MediaPlayer
import java.io.File

object MediaPlayerManager {
    private var mediaPlayer: MediaPlayer? = null
    private var currentTrack: File? = null
    private var onCompletion: (() -> Unit)? = null

    fun playTrack(context: Context, file: File, onComplete: (() -> Unit)? = null) {
        if (currentTrack?.absolutePath == file.absolutePath && mediaPlayer?.isPlaying == true) {
            return // уже играет
        }

        stop() // остановить текущий

        mediaPlayer = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            prepare()
            start()
            setOnCompletionListener {
                onCompletion?.invoke()
                onComplete?.invoke()
            }
        }

        currentTrack = file
        onCompletion = onComplete
    }

    fun pause() {
        mediaPlayer?.pause()
    }

    fun resume() {
        mediaPlayer?.start()
    }

    fun stop() {
        mediaPlayer?.release()
        mediaPlayer = null
        currentTrack = null
        onCompletion = null
    }

    fun seekTo(ms: Int) {
        mediaPlayer?.seekTo(ms)
    }

    fun isPlaying(): Boolean = mediaPlayer?.isPlaying == true

    fun getCurrentPosition(): Int = mediaPlayer?.currentPosition ?: 0

    fun getDuration(): Int = mediaPlayer?.duration ?: 0

    fun getCurrentFile(): File? = currentTrack
}
