package pl.privatetube

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentUris
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.VideoView
import java.util.Locale

data class Clip(val uri: Uri, val title: String, val folder: String, val duration: Long)

class MainActivity : Activity() {
    private val bg = Color.rgb(16, 16, 16)
    private val panel = Color.rgb(30, 30, 30)
    private val yellow = Color.rgb(245, 197, 24)
    private val white = Color.WHITE
    private val muted = Color.rgb(175, 175, 175)
    private val clips = mutableListOf<Clip>()
    private lateinit var grid: GridLayout
    private lateinit var folderSpinner: Spinner
    private var selectedFolder = "Wszystkie"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        if (hasVideoPermission()) showLibrary() else requestVideoPermission()
    }

    private fun permissionName(): String =
        if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_VIDEO
        else Manifest.permission.READ_EXTERNAL_STORAGE

    private fun hasVideoPermission() =
        Build.VERSION.SDK_INT < 23 || checkSelfPermission(permissionName()) == PackageManager.PERMISSION_GRANTED

    private fun requestVideoPermission() {
        if (Build.VERSION.SDK_INT >= 23) requestPermissions(arrayOf(permissionName()), 42)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        if (requestCode == 42 && results.isNotEmpty() && results[0] == PackageManager.PERMISSION_GRANTED) showLibrary()
        else showPermissionScreen()
    }

    private fun showPermissionScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setBackgroundColor(bg); setPadding(28, 28, 28, 28)
        }
        root.addView(label("PrivateTube", 30, yellow, true))
        root.addView(label("Aby wyświetlić lokalne filmy, zezwól aplikacji na dostęp do filmów na urządzeniu.", 16, white))
        val b = Button(this).apply { text = "Przyznaj dostęp"; setOnClickListener { requestVideoPermission() } }
        root.addView(b)
        setContentView(root)
    }

    private fun showLibrary() {
        loadVideos()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(18, 18, 18, 10); setBackgroundColor(bg)
        }
        val title = label("PrivateTube", 28, yellow, true)
        header.addView(title)
        header.addView(label("Twoja prywatna biblioteka wideo", 13, muted))
        folderSpinner = Spinner(this)
        val folders = listOf("Wszystkie") + clips.map { it.folder }.distinct().sorted()
        folderSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, folders)
        folderSpinner.setSelection(folders.indexOf(selectedFolder).coerceAtLeast(0))
        folderSpinner.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedFolder = folders[position]; renderCards()
            }
        })
        header.addView(folderSpinner)
        root.addView(header)
        val scroll = ScrollView(this)
        grid = GridLayout(this).apply { columnCount = 2; setPadding(8, 4, 8, 18) }
        scroll.addView(grid)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val footer = label("${clips.size} filmów na urządzeniu • offline", 12, muted).apply {
            gravity = Gravity.CENTER; setPadding(8, 10, 8, 14)
        }
        root.addView(footer)
        setContentView(root)
        renderCards()
    }

    private fun loadVideos() {
        clips.clear()
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DURATION
        )
        val sort = "${MediaStore.Video.Media.DATE_ADDED} DESC"
        contentResolver.query(collection, projection, null, null, sort)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val folderCol = c.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            val durationCol = c.getColumnIndex(MediaStore.Video.Media.DURATION)
            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val uri = ContentUris.withAppendedId(collection, id)
                val name = c.getString(nameCol) ?: "Bez nazwy"
                val folder = if (folderCol >= 0) c.getString(folderCol) ?: "Inne" else "Inne"
                val duration = if (durationCol >= 0) c.getLong(durationCol) else 0L
                clips.add(Clip(uri, name, folder, duration))
            }
        }
    }

    private fun renderCards() {
        if (!::grid.isInitialized) return
        grid.removeAllViews()
        val visible = if (selectedFolder == "Wszystkie") clips else clips.filter { it.folder == selectedFolder }
        if (visible.isEmpty()) {
            val empty = label("Nie znaleziono filmów w tym folderze.", 16, muted)
            grid.addView(empty, GridLayout.LayoutParams().apply { width = resources.displayMetrics.widthPixels - 32 })
            return
        }
        val width = (resources.displayMetrics.widthPixels - 32) / 2
        visible.forEach { clip ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(panel)
                setPadding(6, 6, 6, 8)
                val params = GridLayout.LayoutParams().apply {
                    this.width = width
                    setMargins(4, 5, 4, 7)
                }
                layoutParams = params
            }
            val thumb = ImageView(this).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                setBackgroundColor(Color.rgb(48, 48, 48))
                val h = (width * 0.58f).toInt()
                layoutParams = LinearLayout.LayoutParams(-1, h)
                setImageBitmap(makeThumbnail(clip.uri))
                contentDescription = clip.title
            }
            card.addView(thumb)
            card.addView(label(clip.title, 14, white, true).apply { setPadding(4, 8, 4, 2); maxLines = 2 })
            card.addView(label("${formatDuration(clip.duration)}  •  ${clip.folder}", 11, muted).apply { setPadding(4, 2, 4, 2) })
            card.setOnClickListener { showPreview(clip) }
            grid.addView(card)
        }
    }

    private fun makeThumbnail(uri: Uri): Bitmap? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(this, uri)
            val bitmap = retriever.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            retriever.release()
            bitmap
        } catch (_: Exception) { null }
    }

    private fun showPreview(clip: Clip) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(16, 12, 16, 12); setBackgroundColor(bg)
        }
        val video = VideoView(this).apply {
            setVideoURI(clip.uri)
            layoutParams = LinearLayout.LayoutParams(-1, 220)
            setOnPreparedListener { mp ->
                mp.isLooping = false
                start()
                video.postDelayed({
                    if (isPlaying) pause()
                    seekTo(0)
                }, 5000)
            }
        }
        layout.addView(video)
        layout.addView(label(clip.title, 16, white, true).apply { setPadding(0, 12, 0, 8) })
        val play = Button(this).apply {
            text = "Odtwórz cały film"
            setOnClickListener {
                video.stopPlayback()
                playFullScreen(clip)
            }
        }
        layout.addView(play)
        val dialog = AlertDialog.Builder(this).setView(layout)
            .setNegativeButton("Zamknij", null).create()
        dialog.setOnDismissListener { video.stopPlayback() }
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        video.start()
    }

    private fun playFullScreen(clip: Clip) {
        val video = VideoView(this).apply {
            setBackgroundColor(Color.BLACK)
            setVideoURI(clip.uri)
            setMediaController(android.widget.MediaController(this@MainActivity))
            setOnPreparedListener { start() }
        }
        AlertDialog.Builder(this).setTitle(clip.title).setView(video)
            .setNegativeButton("Zamknij") { _, _ -> video.stopPlayback() }
            .show()
    }

    private fun label(text: String, size: Int, color: Int, bold: Boolean = false): TextView =
        TextView(this).apply {
            this.text = text; textSize = size.toFloat(); setTextColor(color)
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

    private fun formatDuration(ms: Long): String {
        val total = ms / 1000
        return String.format(Locale.getDefault(), "%d:%02d", total / 60, total % 60)
    }
}
