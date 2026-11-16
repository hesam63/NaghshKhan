package ir.naghshkhan.studio

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var status: TextView
    private var recorder: MediaRecorder? = null
    private var recordingFile: File? = null
    private var isRecording = false

    private val pickVideo = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try { contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            playVideo(uri)
            status.text = "کلیپ انتخاب شد؛ آماده ضبط دوبله‌ای"
        }
    }

    private val requestMic = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startRecording() else toast("برای ضبط دوبله باید اجازه میکروفون را بدهی")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.layoutDirection = android.view.View.LAYOUT_DIRECTION_RTL
        buildScreen()
        player = ExoPlayer.Builder(this).build()
        playerView.player = player
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 28, 24, 24)
            setBackgroundColor(android.graphics.Color.rgb(23, 21, 43))
        }
        val title = TextView(this).apply {
            text = "نقش‌خوان  |  استودیوی دوبله"
            textSize = 23f; setTextColor(-1); gravity = android.view.Gravity.CENTER
            setPadding(0, 8, 0, 18)
        }
        status = TextView(this).apply {
            text = "برای شروع، یک کلیپ از گوشی انتخاب کن"
            textSize = 14f; setTextColor(android.graphics.Color.LTGRAY); gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 14)
        }
        playerView = PlayerView(this).apply {
            useController = true
            setBackgroundColor(android.graphics.Color.BLACK)
        }
        root.addView(title)
        root.addView(status)
        root.addView(playerView, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(makeButton("انتخاب کلیپ از گوشی") { pickVideo.launch(arrayOf("video/*")) })
        root.addView(makeButton("شروع ضبط دوبله") { ensureMicAndRecord() })
        root.addView(makeButton("توقف ضبط و ذخیره صدا") { stopRecording() })
        root.addView(makeButton("پخش / مکث فیلم") {
            val p = player
            if (p != null) { if (p.isPlaying) p.pause() else p.play() }
        })
        root.addView(TextView(this).apply {
            text = "نسخه آزمایشی ۰.۱ • فایل صدای دوبله جداگانه در پوشه Music/NaghshKhan ذخیره می‌شود."
            textSize = 11f; setTextColor(android.graphics.Color.GRAY); gravity = android.view.Gravity.CENTER
            setPadding(0, 14, 0, 0)
        })
        setContentView(root)
    }

    private fun makeButton(label: String, action: () -> Unit): Button = Button(this).apply {
        text = label; isAllCaps = false
        setOnClickListener { action() }
    }

    private fun playVideo(uri: Uri) {
        val p = player ?: return
        p.setMediaItem(MediaItem.fromUri(uri))
        p.prepare()
        p.playWhenReady = true
    }

    private fun ensureMicAndRecord() {
        if (isRecording) { toast("ضبط در حال انجام است"); return }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startRecording()
        else requestMic.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun startRecording() {
        try {
            val dir = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "NaghshKhan")
            if (!dir.exists()) dir.mkdirs()
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            recordingFile = File(dir, "Dub_$stamp.m4a")
            val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(this) else @Suppress("DEPRECATION") MediaRecorder()
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(128000)
            r.setAudioSamplingRate(44100)
            r.setOutputFile(recordingFile!!.absolutePath)
            r.prepare(); r.start()
            recorder = r; isRecording = true
            status.text = "در حال ضبط دوبله… فیلم را پخش کن و صحبت کن"
            toast("ضبط شروع شد")
        } catch (e: Exception) {
            recorder?.release(); recorder = null; isRecording = false
            toast("شروع ضبط ناموفق بود: ${e.localizedMessage}")
        }
    }

    private fun stopRecording() {
        if (!isRecording) { toast("ضبط فعالی وجود ندارد"); return }
        try {
            recorder?.stop()
            status.text = "صدای دوبله ذخیره شد: ${recordingFile?.name}"
            toast("صدای دوبله ذخیره شد")
        } catch (_: Exception) { toast("فایل ضبط‌شده قابل ذخیره نبود؛ دوباره ضبط کن") }
        finally { recorder?.release(); recorder = null; isRecording = false }
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    override fun onStop() {
        if (isRecording) stopRecording()
        super.onStop()
    }

    override fun onDestroy() {
        player?.release(); player = null
        super.onDestroy()
    }
}
