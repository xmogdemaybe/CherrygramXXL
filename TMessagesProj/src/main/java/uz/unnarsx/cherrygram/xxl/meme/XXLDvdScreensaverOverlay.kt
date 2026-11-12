package uz.unnarsx.cherrygram.xxl.meme

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import uz.unnarsx.cherrygram.xxl.XXLVibeActivityResolver
import uz.unnarsx.cherrygram.xxl.config.XXLVibeConfig
import uz.unnarsx.cherrygram.xxl.config.core.XXLVibeFeature
import uz.unnarsx.cherrygram.xxl.meme.core.XXLDvdBounceCore

/**
 * 🚀✨ Enterprise Quantum DVD Screensaver Over Chat (коммит 14) 💯
 *
 * Непрерывно летающий по диагонали блок 90dp поверх переписки через decorView,
 * clickable=false (не блокирует чат). Отскок от стены — смена картинки по кругу
 * arslan.jpg -> svo.jpg -> amdfx.jpg; попадание в угол — вибрация + тост
 * «🎉 ГОЙДА-РИКОШЕТ В УГОЛ ЭКРАНА!». Никогда не крашит (§2A).
 */
object XXLDvdScreensaverOverlay {

    private const val TAG: String = "xxl_dvd_screensaver_v99"
    private const val SIZE_DP: Float = 90f
    private const val FRAME_MS: Long = 16L

    @JvmStatic
    fun attach(anything: Any?) {
        try {
            val activity: Activity = XXLVibeActivityResolver.resolve(anything) ?: return
            if (activity.isFinishing) return
            if (!XXLVibeConfig.isEnabled(XXLVibeFeature.DVD_SCREENSAVER)) return
            val decor: ViewGroup = activity.window?.decorView as? ViewGroup ?: return
            if (decor.findViewWithTag<View>(TAG) != null) return
            val view = DvdView(activity)
            view.tag = TAG
            view.isClickable = false
            view.isFocusable = false
            decor.addView(view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            view.start()
        } catch (ignored: Throwable) {
        }
    }

    @JvmStatic
    fun detach(anything: Any?) {
        try {
            val decor: ViewGroup = XXLVibeActivityResolver.decorOf(anything) ?: return
            val existing: View? = decor.findViewWithTag<View>(TAG)
            if (existing is DvdView) existing.stop()
            if (existing != null) decor.removeView(existing)
        } catch (ignored: Throwable) {
        }
    }

    private class DvdView(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var state = XXLDvdBounceCore.State(40f, 120f, 4f, 3f)
        private var bounces = 0
        private var bitmap: Bitmap? = null
        private var running = false
        private val sizePx: Int = (SIZE_DP * context.resources.displayMetrics.density).toInt()

        private val ticker = object : Runnable {
            override fun run() {
                if (!running) return
                val r = XXLDvdBounceCore.step(state, width.toFloat(), height.toFloat(), sizePx.toFloat())
                state = r.state
                if (r.hitWall) {
                    bounces++
                    loadBitmap(XXLDvdBounceCore.IMAGE_CYCLE[XXLDvdBounceCore.imageIndexFor(bounces)])
                }
                if (r.hitCorner) corner()
                invalidate()
                postDelayed(this, FRAME_MS)
            }
        }

        fun start() {
            running = true
            loadBitmap(XXLDvdBounceCore.IMAGE_CYCLE[0])
            post(ticker)
        }

        fun stop() {
            running = false
            removeCallbacks(ticker)
        }

        private fun loadBitmap(name: String) {
            bitmap = try {
                XXLMemeAssets.loadBitmap(context, name)
            } catch (ignored: Throwable) {
                null
            }
        }

        private fun corner() {
            try {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (vibrator != null) {
                    if (Build.VERSION.SDK_INT >= 26) {
                        vibrator.vibrate(VibrationEffect.createOneShot(60L, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(60L)
                    }
                }
                Toast.makeText(context, "🎉 ГОЙДА-РИКОШЕТ В УГОЛ ЭКРАНА!", Toast.LENGTH_SHORT).show()
            } catch (ignored: Throwable) {
            }
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            try {
                val b = bitmap
                if (b != null) {
                    val src = android.graphics.Rect(0, 0, b.width, b.height)
                    val dst = android.graphics.RectF(state.x, state.y, state.x + sizePx, state.y + sizePx)
                    canvas.drawBitmap(b, src, dst, paint)
                } else {
                    paint.color = Color.MAGENTA
                    canvas.drawRect(state.x, state.y, state.x + sizePx, state.y + sizePx, paint)
                }
            } catch (ignored: Throwable) {
            }
        }
    }
}
