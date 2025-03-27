package com.example.audiorecorder

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private lateinit var startRecordButton: Button
    private lateinit var stopRecordButton: Button
    private lateinit var playRecordButton: Button
    private lateinit var recordListView: ListView
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var fileName: String = ""
    private val records = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startRecordButton = findViewById(R.id.startRecordButton)
        stopRecordButton = findViewById(R.id.stopRecordButton)
        playRecordButton = findViewById(R.id.playRecordButton)
        recordListView = findViewById(R.id.recordListView)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, records)
        recordListView.adapter = adapter

        startRecordButton.setOnClickListener { startRecording() }
        stopRecordButton.setOnClickListener { stopRecording() }
        playRecordButton.setOnClickListener { playRecording() }
        recordListView.setOnItemClickListener { _, _, position, _ ->
            playSelectedRecording(records[position])
        }

        checkPermissions()
        loadRecordings()
    }

    private fun checkPermissions() {
        val permissions = arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val granted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        if (!granted) {
            ActivityCompat.requestPermissions(this, permissions, 100)
        }
    }

    private fun startRecording() {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "record_$timeStamp.3gp")
        fileName = file.absolutePath

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(fileName)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            prepare()
            start()
        }

        startRecordButton.visibility = Button.GONE
        stopRecordButton.visibility = Button.VISIBLE
        Toast.makeText(this, "Đang ghi âm...", Toast.LENGTH_SHORT).show()
    }

    private fun stopRecording() {
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null

        saveRecordingToMediaStore(fileName)

        startRecordButton.visibility = Button.VISIBLE
        stopRecordButton.visibility = Button.GONE
        playRecordButton.visibility = Button.VISIBLE

        Toast.makeText(this, "Ghi âm đã lưu!", Toast.LENGTH_SHORT).show()
        loadRecordings()
    }

    private fun saveRecordingToMediaStore(filePath: String) {
        val fileName = File(filePath).name
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/3gpp")
            put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/Recordings")
            put(MediaStore.Audio.Media.IS_PENDING, 1) // Đánh dấu file đang được ghi
        }

        val uri = contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)

        uri?.let {
            contentResolver.openOutputStream(it)?.use { outputStream ->
                File(filePath).inputStream().copyTo(outputStream) // Sao chép file vào MediaStore
            }
            values.clear()
            values.put(MediaStore.Audio.Media.IS_PENDING, 0) // Đánh dấu file đã xong
            contentResolver.update(it, values, null, null)
        }
    }


    private fun playRecording() {
        mediaPlayer = MediaPlayer().apply {
            setDataSource(fileName)
            prepare()
            start()
        }
        Toast.makeText(this, "Đang phát lại...", Toast.LENGTH_SHORT).show()
    }

    private fun playSelectedRecording(filePath: String) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(filePath)
            prepare()
            start()
        }
        Toast.makeText(this, "Đang phát: $filePath", Toast.LENGTH_SHORT).show()
    }

    private fun loadRecordings() {
        records.clear()
        val musicDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        musicDir?.listFiles()?.forEach {
            if (it.extension == "3gp") {
                records.add(it.absolutePath)
            }
        }
        adapter.notifyDataSetChanged()
    }
}