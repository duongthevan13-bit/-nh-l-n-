package com.example.depthwall

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class MainActivity : AppCompatActivity() {

    private val pickBg = registerForActivityResult(ActivityResultContracts.GetContent()) { save(it, "bg.img") }
    private val pickFg = registerForActivityResult(ActivityResultContracts.GetContent()) { save(it, "fg.img") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnBg).setOnClickListener { pickBg.launch("image/*") }
        findViewById<Button>(R.id.btnFg).setOnClickListener { pickFg.launch("image/*") }
        findViewById<Button>(R.id.btnSet).setOnClickListener {
            val i = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            i.putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(this, ParallaxWallpaperService::class.java)
            )
            startActivity(i)
        }
    }

    private fun save(uri: Uri?, name: String) {
        if (uri == null) return
        try {
            contentResolver.openInputStream(uri)?.use { input ->
                File(filesDir, name).outputStream().use { input.copyTo(it) }
            }
            Toast.makeText(this, "Da luu", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Loi: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
