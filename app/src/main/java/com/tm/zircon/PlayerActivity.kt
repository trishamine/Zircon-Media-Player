package com.tm.zircon

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import java.io.File
import java.util.Locale

class PlayerActivity : Activity() {

    private lateinit var backButton: ImageButton
    private lateinit var albumArt: ImageView
    private lateinit var albumArtPlaceholder: TextView
    private lateinit var songTitle: TextView
    private lateinit var songArtist: TextView
    private lateinit var seekBar: SeekBar
    private lateinit var timeStart: TextView
    private lateinit var timeEnd: TextView
    private lateinit var playPauseButton: ImageButton
    private lateinit var buttonPrev: ImageButton
    private lateinit var buttonNext: ImageButton
    private lateinit var volumeSeekBar: SeekBar

    private var currentFile: File? = null
    private var currentDir: File? = null

    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            seekBar.progress = MediaPlayerManager.getCurrentPosition()
            timeStart.text = formatTime(MediaPlayerManager.getCurrentPosition())
            handler.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        // UI
        backButton = findViewById(R.id.backButton)
        albumArt = findViewById(R.id.albumArt)
        albumArtPlaceholder = findViewById(R.id.albumArtPlaceholder)
        songTitle = findViewById(R.id.playerSongTitle)
        songArtist = findViewById(R.id.playerSongArtist)
        seekBar = findViewById(R.id.playerSeekBar)
        timeStart = findViewById(R.id.timeStart)
        timeEnd = findViewById(R.id.timeEnd)
        playPauseButton = findViewById(R.id.buttonPlayPause)
        buttonPrev = findViewById(R.id.buttonPrev)
        buttonNext = findViewById(R.id.buttonNext)
        volumeSeekBar = findViewById(R.id.volumeSeekBar)

        backButton.setOnClickListener { finish() }

        val trackPath = intent.getStringExtra("trackPath")
        val trackDir = intent.getStringExtra("trackDir")

        if (trackPath == null || trackDir == null) {
            Toast.makeText(this, "Ошибка загрузки трека", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        currentFile = File(trackPath)
        currentDir = File(trackDir)

        // Настройка громкости
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        volumeSeekBar.max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        volumeSeekBar.progress = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        volumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        loadTrack(currentFile!!)
    }

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("ZirconPrefs", MODE_PRIVATE)
        val langCode = prefs.getString("app_lang", "ru") ?: "ru"
        val locale = Locale(langCode)
        Locale.setDefault(locale)

        val config = Configuration()
        config.setLocale(locale)

        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

    private fun loadTrack(file: File) {
        currentFile = file

        val mmr = MediaMetadataRetriever()
        mmr.setDataSource(file.absolutePath)

        val title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: file.name
        val artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "<unknown>"
        val duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toIntOrNull() ?: 0
        val art = mmr.embeddedPicture

        songTitle.text = title
        songArtist.text = artist
        timeEnd.text = formatTime(duration)
        seekBar.max = duration

        if (art != null) {
            albumArt.setImageBitmap(android.graphics.BitmapFactory.decodeByteArray(art, 0, art.size))
            albumArt.visibility = View.VISIBLE
            albumArtPlaceholder.visibility = View.GONE
        } else {
            albumArt.visibility = View.GONE
            albumArtPlaceholder.visibility = View.VISIBLE
        }

        mmr.release()

        MediaPlayerManager.playTrack(this, file) {
            runOnUiThread {
                buttonNext.performClick()
            }
        }

        handler.removeCallbacks(updateRunnable)
        handler.post(updateRunnable)

        seekBar.progress = 0
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) MediaPlayerManager.seekTo(progress)
                timeStart.text = formatTime(MediaPlayerManager.getCurrentPosition())
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        playPauseButton.setImageResource(
            if (MediaPlayerManager.isPlaying()) android.R.drawable.ic_media_pause
            else android.R.drawable.ic_media_play
        )

        playPauseButton.setOnClickListener {
            if (MediaPlayerManager.isPlaying()) {
                MediaPlayerManager.pause()
                playPauseButton.setImageResource(android.R.drawable.ic_media_play)
            } else {
                MediaPlayerManager.resume()
                playPauseButton.setImageResource(android.R.drawable.ic_media_pause)
            }
        }

        buttonPrev.setOnClickListener {
            val dir = currentDir ?: return@setOnClickListener
            val files = dir.listFiles()?.filter {
                it.isFile && it.name.lowercase().endsWith(".mp3")
            }?.sortedBy { it.name.lowercase() } ?: return@setOnClickListener

            val index = files.indexOfFirst { it.absolutePath == file.absolutePath }
            if (index > 0) {
                loadTrack(files[index - 1])
            } else {
                Toast.makeText(this, "Это первый трек в папке.", Toast.LENGTH_SHORT).show()
            }
        }

        buttonNext.setOnClickListener {
            val dir = currentDir ?: return@setOnClickListener
            val files = dir.listFiles()?.filter {
                it.isFile && it.name.lowercase().endsWith(".mp3")
            }?.sortedBy { it.name.lowercase() } ?: return@setOnClickListener

            val index = files.indexOfFirst { it.absolutePath == file.absolutePath }
            if (index != -1 && index + 1 < files.size) {
                loadTrack(files[index + 1])
            } else {
                Toast.makeText(this, "Это последний трек в папке.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateRunnable)
        // Не вызываем release() — MediaPlayerManager управляет MediaPlayer
    }

    private fun formatTime(ms: Int): String {
        val sec = ms / 1000
        val min = sec / 60
        val secRemain = sec % 60
        return String.format("%02d:%02d", min, secRemain)
    }
}
