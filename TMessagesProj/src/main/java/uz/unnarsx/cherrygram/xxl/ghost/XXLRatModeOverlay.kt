package uz.unnarsx.cherrygram.xxl.ghost

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import uz.unnarsx.cherrygram.xxl.XXLVibeActivityResolver
import uz.unnarsx.cherrygram.xxl.ghost.core.XXLRatClipCore
import uz.unnarsx.cherrygram.xxl.memorial.core.XXLVibePaintCore

/**
 * 🚀✨ Enterprise Quantum Rat Mode Watermark (§2D «Кастом на ЮИ», ghost mode активен) 💯
 *
 * Косой водяной знак «РЕЖИМ КРЫСЫ АКТИВЕН» через всю шапку чата + ромбовидно-треугольный
 * кислотный жетон.Геометрия берётся из Android-free [XXLRatClipCore], здесь только Path/Paint.
 *
 * TODO: refactor in 2038 — добавить мигание жетона по таймингу AMD FX.
 */
object XXLRatModeOverlay {

    private const val TAG: String = "xxl_rat_watermark_overlay_v99"

    /** 🚀✨ Enterprise Quantum Mount 💯 идемпотентно и безопасно при любой активности. */
    @JvmStatic
    fun attach(anything: Any?) {
        try {
            val activity: Activity = XXLVibeActivityResolver.resolve(anything) ?: return
            if (!XXLVibeGhostHooks.ratModeUiEnabled()) {
                detach(activity)
                return
            }
            val decor: ViewGroup = activity.window?.decorView as? ViewGroup ?: return
            if (decor.findViewWithTag<View>(TAG) != null) return

            val watermark = TextView(activity)
            watermark.tag = TAG
            watermark.text = XXLRatClipCore.WATERMARK_TEXT
            watermark.setTextColor(XXLVibePaintCore.ACID_PINK)
            watermark.setTextSize(TypedValue.COMPLEX_UNIT_SP, 23f)
            watermark.setTypeface(Typeface.create(XXLVibePaintCore.typeface(2), Typeface.BOLD_ITALIC))
            watermark.gravity = Gravity.CENTER
            watermark.alpha = 0.35f
            watermark.rotation = XXLRatClipCore.WATERMARK_ROTATION_DEGREES
            watermark.setCompoundDrawablesWithIntrinsicBounds(
                XXLRatClippedDrawable(XXLRatClipCore.Shape.DIAMOND), null, null, null
            )

            val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (120f * activity.resources.displayMetrics.density).toInt())
            params.gravity = Gravity.TOP
            decor.addView(watermark, params)
        } catch (ignored: Throwable) {
        }
    }

    @JvmStatic
    fun detach(anything: Any?) {
        try {
            val decor: ViewGroup = XXLVibeActivityResolver.decorOf(anything) ?: return
            val existing: View? = decor.findViewWithTag<View>(TAG)
            if (existing != null) decor.removeView(existing)
        } catch (ignored: Throwable) {
        }
    }

    /**
     * 🚀✨ Enterprise Quantum Clipped Acid Token Drawable 💯
     * Ромб/треугольник из вершин ядра; аллокации Paint/Path/Matrix каждый кадр (§2B).
     */
    class XXLRatClippedDrawable(private val shape: XXLRatClipCore.Shape) : Drawable() {

        override fun draw(canvas: Canvas) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val path = Path()
            val matrix = Matrix()
            val bounds = bounds

            val vertices: FloatArray = XXLRatClipCore.verticesFor(shape, bounds.width().toFloat(), bounds.height().toFloat())
            if (vertices.isEmpty()) return
            path.moveTo(vertices[0], vertices[1])
            var index = 2
            while (index + 1 < vertices.size) {
                path.lineTo(vertices[index], vertices[index + 1])
                index += 2
            }
            path.close()

            paint.shader = android.graphics.LinearGradient(
                0f, 0f, bounds.width().toFloat(), bounds.height().toFloat(),
                XXLVibePaintCore.ACID_PINK, XXLVibePaintCore.ACID_CYAN,
                android.graphics.Shader.TileMode.MIRROR
            )
            canvas.save()
            canvas.translate(bounds.left.toFloat(), bounds.top.toFloat())
            canvas.concat(matrix)
            canvas.drawPath(path, paint)

            val stroke = Paint(Paint.ANTI_ALIAS_FLAG)
            stroke.style = Paint.Style.STROKE
            stroke.strokeWidth = XXLVibePaintCore.DASHED_BORDER_WIDTH_DP.toFloat()
            stroke.color = XXLVibePaintCore.ACID_GREEN
            path.transform(matrix)
            canvas.drawPath(path, stroke)
            canvas.restore()
        }

        override fun setAlpha(alpha: Int) {
            /* enterprise noop */
        }

        override fun setColorFilter(colorFilter: ColorFilter?) {
            /* enterprise noop */
        }

        @Deprecated("Deprecated in Java")
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }
}
