package com.tm.zircon

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.os.*
import android.provider.Settings
import android.view.MenuItem
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File

class MainActivity : Activity() {

    private lateinit var listView: ListView
    private lateinit var songTitle: TextView
    private lateinit var songArtist: TextView
    private lateinit var playPauseButton: ImageButton
    private lateinit var openPlayerButton: ImageButton
    private lateinit var seekBar: SeekBar

    private var currentDir: File = Environment.getExternalStorageDirectory()
    private var currentTrack: File? = null
    private val seekBarHandler = Handler(Looper.getMainLooper())
    private val seekBarRunnable = object : Runnable {
        override fun run() {
            val position = MediaPlayerManager.getCurrentPosition()
            if (position != -1) {
                seekBar.progress = position
                seekBarHandler.postDelayed(this, 500)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        actionBar?.setDisplayShowHomeEnabled(true)
        actionBar?.setIcon(R.drawable.ic_zircon)
        title = "Zircon"

        listView = findViewById(R.id.listView)
        songTitle = findViewById(R.id.songTitle)
        songArtist = findViewById(R.id.songArtist)
        playPauseButton = findViewById(R.id.playPauseButton)
        openPlayerButton = findViewById(R.id.openPlayerButton)
        seekBar = findViewById(R.id.seekBar)

        playPauseButton.setOnClickListener {
            if (MediaPlayerManager.isPlaying()) {
                MediaPlayerManager.pause()
                playPauseButton.setImageResource(android.R.drawable.ic_media_play)
            } else {
                MediaPlayerManager.resume()
                playPauseButton.setImageResource(android.R.drawable.ic_media_pause)
            }
        }

        openPlayerButton.setOnClickListener {
            currentTrack?.let { file ->
                val intent = Intent(this, PlayerActivity::class.java)
                intent.putExtra("trackPath", file.absolutePath)
                intent.putExtra("trackDir", currentDir.absolutePath)
                startActivity(intent)
            } ?: Toast.makeText(this, "Трек не выбран", Toast.LENGTH_SHORT).show()
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) MediaPlayerManager.seekTo(progress)
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        checkPermissionsAndLoad()
        setupFileList()
    }

    private fun checkPermissionsAndLoad() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
                Toast.makeText(this, "Найдите Zircon и разрешите доступ. Затем перезапустите приложение.", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Не удалось открыть настройки", Toast.LENGTH_LONG).show()
            }
            return
        }

        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), 100)
        } else {
            showFiles(currentDir)
        }
    }

    private fun setupFileList() {
        listView.setOnItemClickListener { _, _, position, _ ->
            val files = getFilteredFiles(currentDir)
            if (position == 0) {
                val parent = currentDir.parentFile
                if (parent != null) {
                    currentDir = parent
                    showFiles(currentDir)
                }
            } else {
                val file = files[position - 1]
                if (file.isDirectory) {
                    currentDir = file
                    showFiles(currentDir)
                } else {
                    playFile(file)
                }
            }
        }
    }

    private fun playFile(file: File) {
        currentTrack = file
        MediaPlayerManager.playTrack(this, file) {
            // автопереход к следующему треку
            val files = getFilteredFiles(currentDir).filter { it.name.endsWith(".mp3") }
            val index = files.indexOfFirst { it.absolutePath == file.absolutePath }
            if (index >= 0 && index + 1 < files.size) {
                playFile(files[index + 1])
            }
        }

        val duration = MediaPlayerManager.getDuration()
        seekBar.max = duration
        seekBarHandler.removeCallbacks(seekBarRunnable)
        seekBarHandler.post(seekBarRunnable)
        updateNowPlayingUI(file)
        playPauseButton.setImageResource(android.R.drawable.ic_media_pause)
    }

    private fun updateNowPlayingUI(file: File) {
        val mmr = MediaMetadataRetriever()
        try {
            mmr.setDataSource(file.absolutePath)
            val title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: "Неизвестно"
            val artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Неизвестно"
            songTitle.text = title
            songArtist.text = artist
        } catch (e: Exception) {
            songTitle.text = "Неизвестно"
            songArtist.text = "Неизвестно"
        } finally {
            mmr.release()
        }
    }

    private fun getFilteredFiles(dir: File): List<File> {
        return dir.listFiles()?.filter {
            it.isDirectory || it.name.lowercase().endsWith(".mp3")
        }?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) ?: emptyList()
    }

    private fun showFiles(dir: File) {
        val items = mutableListOf("Вверх")
        val files = getFilteredFiles(dir)
        for (file in files) {
            items.add(if (file.isDirectory) "[${file.name}]" else file.name)
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
        listView.adapter = adapter
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            showFiles(currentDir)
        } else {
            Toast.makeText(this, "Разрешения не предоставлены", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        seekBarHandler.removeCallbacks(seekBarRunnable)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return super.onOptionsItemSelected(item)
    }
}
