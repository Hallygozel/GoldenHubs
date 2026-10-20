package com.example.goldcube

import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import java.io.File
import kotlin.math.*

class CubeWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = CubeEngine()

    inner class CubeEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private var visible = false
        private var w = 0
        private var h = 0
        private var ax = 0.45f
        private var ay = 0f
        private val bitmaps = arrayOfNulls<Bitmap>(6)

        private val verts = arrayOf(
            floatArrayOf(-1f, -1f, -1f), floatArrayOf(1f, -1f, -1f),
            floatArrayOf(1f, 1f, -1f), floatArrayOf(-1f, 1f, -1f),
            floatArrayOf(-1f, -1f, 1f), floatArrayOf(1f, -1f, 1f),
            floatArrayOf(1f, 1f, 1f), floatArrayOf(-1f, 1f, 1f)
        )
        // 0 front, 1 back, 2 right, 3 left, 4 top, 5 bottom
        private val faces = arrayOf(
            intArrayOf(0, 1, 2, 3), intArrayOf(5, 4, 7, 6),
            intArrayOf(1, 5, 6, 2), intArrayOf(4, 0, 3, 7),
            intArrayOf(4, 5, 1, 0), intArrayOf(3, 2, 6, 7)
        )

        private val gold = 0xFFD4AF37.toInt()
        private val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = gold
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }
        private val bmpPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        private val shadePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val placeholder = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF2A2210.toInt() }
        private val matrix = Matrix()
        private val rot = Array(8) { FloatArray(3) }
        private val proj = Array(8) { FloatArray(2) }

        private val drawRunnable = object : Runnable {
            override fun run() {
                drawFrame()
                if (visible) handler.postDelayed(this, 16)
            }
        }

        private fun loadImages() {
            for (i in 0 until 6) {
                bitmaps[i]?.recycle()
                val f = File(filesDir, "face$i.jpg")
                bitmaps[i] = if (f.exists()) BitmapFactory.decodeFile(f.path) else null
            }
        }

        override fun onVisibilityChanged(v: Boolean) {
            visible = v
            handler.removeCallbacks(drawRunnable)
            if (v) {
                loadImages()
                handler.post(drawRunnable)
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            w = width; h = height
            super.onSurfaceChanged(holder, format, width, height)
        }

        override fun onDestroy() {
            visible = false
            handler.removeCallbacks(drawRunnable)
            super.onDestroy()
        }

        private fun drawFrame() {
            if (w == 0 || h == 0) return
            ay += 0.012f
            ax += 0.004f
            val holder = surfaceHolder
            val c = holder.lockCanvas() ?: return
            try {
                c.drawColor(Color.BLACK)
                renderCube(c)
            } finally {
                holder.unlockCanvasAndPost(c)
            }
        }

        private fun renderCube(c: Canvas) {
            val cx = w / 2f
            val cy = h * 0.45f
            val size = min(w, h) * 0.25f
            val d = 4.5f
            val cosY = cos(ay); val sinY = sin(ay)
            val cosX = cos(ax); val sinX = sin(ax)

            for (i in 0 until 8) {
                val (x, y, z) = verts[i].let { Triple(it[0], it[1], it[2]) }
                val x1 = x * cosY + z * sinY
                val z1 = -x * sinY + z * cosY
                val y2 = y * cosX - z1 * sinX
                val z2 = y * sinX + z1 * cosX
                rot[i][0] = x1; rot[i][1] = y2; rot[i][2] = z2
                val s = d / (z2 + d)
                proj[i][0] = cx + x1 * s * size
                proj[i][1] = cy + y2 * s * size
            }

            edgePaint.strokeWidth = size * 0.07f

            for (fi in 0 until 6) {
                val f = faces[fi]
                val p0 = proj[f[0]]; val p1 = proj[f[1]]; val p2 = proj[f[2]]
                val cross2d = (p1[0] - p0[0]) * (p2[1] - p1[1]) - (p1[1] - p0[1]) * (p2[0] - p1[0])
                if (cross2d <= 0f) continue // arka tarap, görünmeýär

                val path = Path().apply {
                    moveTo(p0[0], p0[1])
                    for (k in 1..3) lineTo(proj[f[k]][0], proj[f[k]][1])
                    close()
                }

                val bmp = bitmaps[fi]
                if (bmp != null) {
                    val dst = floatArrayOf(
                        p0[0], p0[1], p1[0], p1[1],
                        proj[f[2]][0], proj[f[2]][1], proj[f[3]][0], proj[f[3]][1]
                    )
                    val bw = bmp.width.toFloat(); val bh = bmp.height.toFloat()
                    val src = floatArrayOf(0f, 0f, bw, 0f, bw, bh, 0f, bh)
                    matrix.reset()
                    matrix.setPolyToPoly(src, 0, dst, 0, 4)
                    c.save()
                    c.clipPath(path)
                    c.drawBitmap(bmp, matrix, bmpPaint)
                    c.restore()
                } else {
                    c.drawPath(path, placeholder)
                }

                // ýeňil kölegeleme: tarap näçe gyşarsa, şonça garalýar
                val a = rot[f[0]]; val b = rot[f[1]]; val cc = rot[f[2]]
                val ux = b[0] - a[0]; val uy = b[1] - a[1]; val uz = b[2] - a[2]
                val vx = cc[0] - b[0]; val vy = cc[1] - b[1]; val vz = cc[2] - b[2]
                val nx = uy * vz - uz * vy
                val ny = uz * vx - ux * vz
                val nz = ux * vy - uy * vx
                val len = sqrt(nx * nx + ny * ny + nz * nz)
                val facing = if (len > 0f) abs(nz) / len else 1f
                shadePaint.color = Color.argb(((1f - facing) * 150).toInt().coerceIn(0, 255), 0, 0, 0)
                c.drawPath(path, shadePaint)

                c.drawPath(path, edgePaint) // altyn gapyrgalar
            }
        }
    }
}
