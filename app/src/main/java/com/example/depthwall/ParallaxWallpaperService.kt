package com.example.depthwall

import android.content.Context
import android.graphics.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import java.io.File

class ParallaxWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = ParallaxEngine()

    inner class ParallaxEngine : Engine(), SensorEventListener {

        // Neu hieu ung bi nguoc chieu, doi DIR thanh -1f
        private val DIR = 1f

        private val handler = Handler(Looper.getMainLooper())
        private val sm = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        private val sensor: Sensor? = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        private val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)

        private var bg: Bitmap? = null
        private var fg: Bitmap? = null
        private var tx = 0f
        private var ty = 0f
        private var cx = 0f
        private var cy = 0f
        private var visible = false
        private var w = 0
        private var h = 0

        private val frame = object : Runnable {
            override fun run() {
                drawFrame()
                if (visible) handler.postDelayed(this, 16)
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            w = width; h = height
            loadImages()
        }

        override fun onVisibilityChanged(v: Boolean) {
            visible = v
            if (v) {
                loadImages()
                sensor?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
                handler.post(frame)
            } else {
                sm.unregisterListener(this)
                handler.removeCallbacks(frame)
            }
        }

        override fun onDestroy() {
            sm.unregisterListener(this)
            handler.removeCallbacks(frame)
            super.onDestroy()
        }

        override fun onSensorChanged(e: SensorEvent) {
            tx = (-e.values[0] / 9.8f * DIR).coerceIn(-1f, 1f)
            ty = ((e.values[1] - 7f) / 5f * DIR).coerceIn(-1f, 1f)
        }

        override fun onAccuracyChanged(s: Sensor?, a: Int) {}

        private fun loadImages() {
            if (w == 0 || h == 0) return
            bg = decode(File(filesDir, "bg.img"))
            fg = decode(File(filesDir, "fg.img"))
        }

        private fun decode(f: File): Bitmap? {
            if (!f.exists()) return null
            return try {
                val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(f.path, o)
                var s = 1
                while (o.outWidth / (s * 2) >= w * 1.2f && o.outHeight / (s * 2) >= h * 1.2f) s *= 2
                BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = s })
            } catch (e: Exception) { null }
        }

        private fun layer(c: Canvas, b: Bitmap, dx: Float, dy: Float, zoom: Float) {
            val s = maxOf(w.toFloat() / b.width, h.toFloat() / b.height) * zoom
            val m = Matrix()
            m.postScale(s, s)
            m.postTranslate((w - b.width * s) / 2f + dx, (h - b.height * s) / 2f + dy)
            c.drawBitmap(b, m, paint)
        }

        private fun drawFrame() {
            cx += (tx - cx) * 0.12f
            cy += (ty - cy) * 0.12f
            val holder = surfaceHolder
            val c = holder.lockCanvas() ?: return
            try {
                c.drawColor(Color.BLACK)
                bg?.let { layer(c, it, -cx * w * 0.02f, -cy * h * 0.01f, 1.10f) }
                fg?.let { layer(c, it, cx * w * 0.05f, cy * h * 0.025f, 1.12f) }
            } finally {
                holder.unlockCanvasAndPost(c)
            }
        }
    }
}
