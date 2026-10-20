package com.example.goldcube

import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {

    private val names = arrayOf("Öň", "Arka", "Sag", "Çep", "Üst", "Aşak")
    private val thumbs = arrayOfNulls<ImageView>(6)
    private var current = 0

    private val picker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) saveFace(current, uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 32)
            setBackgroundColor(Color.BLACK)
        }
        val title = TextView(this).apply {
            text = "Altyn Kub: 6 tarapa surat saýlaň"
            setTextColor(0xFFD4AF37.toInt())
            textSize = 20f
            gravity = Gravity.CENTER
        }
        root.addView(title)

        val grid = GridLayout(this).apply { columnCount = 2 }
        for (i in 0 until 6) {
            val cell = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(16, 24, 16, 8)
            }
            val iv = ImageView(this).apply {
                setBackgroundColor(0xFF2A2210.toInt())
                scaleType = ImageView.ScaleType.CENTER_CROP
                layoutParams = LinearLayout.LayoutParams(380, 380)
                setOnClickListener { current = i; picker.launch("image/*") }
            }
            val tv = TextView(this).apply {
                text = "${i + 1}. ${names[i]}"
                setTextColor(0xFFD4AF37.toInt())
                gravity = Gravity.CENTER
            }
            thumbs[i] = iv
            cell.addView(iv); cell.addView(tv)
            grid.addView(cell)
        }
        root.addView(grid)

        val btn = Button(this).apply {
            text = "Ekran blokirowka wallpaper edip goý"
            setOnClickListener { setWallpaper() }
        }
        root.addView(btn)

        val hint = TextView(this).apply {
            text = "Açylan penjirede \"Lock screen\" (diňe blokirowka) saýlaň, soň \"Set\" basyň."
            setTextColor(Color.LTGRAY)
            setPadding(0, 24, 0, 0)
        }
        root.addView(hint)

        setContentView(ScrollView(this).apply { setBackgroundColor(Color.BLACK); addView(root) })
        for (i in 0 until 6) loadThumb(i)
    }

    private fun loadThumb(i: Int) {
        val f = File(filesDir, "face$i.jpg")
        if (f.exists()) thumbs[i]?.setImageBitmap(BitmapFactory.decodeFile(f.path))
    }

    private fun saveFace(i: Int, uri: Uri) {
        val src = contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } ?: return
        val side = minOf(src.width, src.height)
        val square = Bitmap.createBitmap(src, (src.width - side) / 2, (src.height - side) / 2, side, side)
        val scaled = Bitmap.createScaledBitmap(square, 512, 512, true)
        FileOutputStream(File(filesDir, "face$i.jpg")).use { scaled.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        thumbs[i]?.setImageBitmap(scaled)
    }

    private fun setWallpaper() {
        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(
            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
            ComponentName(this, CubeWallpaperService::class.java)
        )
        try { startActivity(intent) } catch (e: Exception) {
            startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
        }
    }
}
