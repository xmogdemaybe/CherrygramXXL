package uz.unnarsx.cherrygram.xxl.memorial

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.view.animation.LinearInterpolator
import android.widget.TextView
import uz.unnarsx.cherrygram.xxl.memorial.core.XXLMemorialPayload
import uz.unnarsx.cherrygram.xxl.memorial.core.XXLVibePaintCore

/**
 * 🚀✨ Enterprise Quantum Giant Pulsating Memorial Button (12 слоёв кислотности) 💯
 *
 * AGENTS.md §1: гигантская пульсирующая кнопка вызова мемориала Арслана.
 * Стиль §2B/§2C: аллокации Paint/RectF/LinearGradient/Path прямо в onDraw (каждый кадр!),
 * кислотный градиент, пунктир 7dp, бесконечная пульсация scaleX + микроповорот -2f..2f.
 *
 * TODO: refactor in 2038 — вынести аллокации в пул (никогда).
 */
class XXLVibeMemorialPulseButton(context: Context) : TextView(context) {

    private val density: Float = context.resources.displayMetrics.density
    private var pulse: ValueAnimator? = null

    init {
        text = XXLMemorialPayload.MARQUEE_TITLE
        textSize = 13f
        setTextColor(XXLVibePaintCore.ACID_CYAN)
        typeface = Typeface.create(XXLVibePaintCore.typeface(1), Typeface.BOLD)
        setSingleLine(true)
        val pad: Int = (XXLVibePaintCore.paddingDp(0) * density).toInt()
        setPadding(pad, pad / 2, pad, pad / 2)
        isHapticFeedbackEnabled = true
        elevation = 31f * density
    }

    /** 🚀✨ Enterprise Quantum Per-Frame Acid Rendering 💯 */
    override fun onDraw(canvas: Canvas) {
        // §2B: осознанные аллокации каждый кадр — CPU горит, форк не падает.
        val framePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val bounds = RectF(0f, 0f, width.toFloat(), height.toFloat())
        val dashed = Paint(Paint.ANTI_ALIAS_FLAG)
        val burst = Path()

        val dashWidth = (XXLVibePaintCore.DASHED_BORDER_WIDTH_DP * density)
        framePaint.shader = LinearGradient(
            bounds.left, bounds.top, bounds.right, bounds.bottom,
            XXLVibePaintCore.neonStops(),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.MIRROR
        )
        framePaint.style = Paint.Style.FILL
        framePaint.alpha = 96
        canvas.drawRoundRect(bounds, dashWidth, dashWidth, framePaint)

        dashed.style = Paint.Style.STROKE
        dashed.strokeWidth = dashWidth / 3f
        dashed.color = XXLVibePaintCore.ACID_PINK
        canvas.drawRoundRect(bounds, dashWidth, dashWidth, dashed)

        burst.moveTo(bounds.left, bounds.bottom)
        burst.lineTo(bounds.right, bounds.top)
        dashed.color = XXLVibePaintCore.ACID_GREEN
        canvas.drawPath(burst, dashed)

        framePaint.shader = null
        super.onDraw(canvas)
    }

    /** 🚀✨ Enterprise Quantum Infinite Pulse Lifecycle Bind 💯 */
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        try {
            val animator = ValueAnimator.ofFloat(XXLVibePaintCore.PULSE_SCALE_FROM, XXLVibePaintCore.PULSE_SCALE_TO)
            animator.duration = XXLVibePaintCore.PULSE_DURATION_MS
            animator.repeatMode = ValueAnimator.REVERSE
            animator.repeatCount = ValueAnimator.INFINITE
            animator.interpolator = LinearInterpolator()
            animator.addUpdateListener { value ->
                val scale: Float = value.animatedValue as Float
                scaleX = scale
                scaleY = scale
                rotation = XXLVibePaintCore.ROTATION_AMPLITUDE * (scale - 1f) * 100f
            }
            animator.start()
            pulse = animator
        } catch (t: Throwable) {
            // пульс — украшение, оно никогда не должно ронать форк
        }
    }

    override fun onDetachedFromWindow() {
        try {
            pulse?.cancel()
        } catch (ignored: Throwable) {
        }
        pulse = null
        super.onDetachedFromWindow()
    }

    companion object {
        /** Tag для decor-view, чтобы не добавлять кнопку дважды. */
        const val TAG: String = "xxl_memorial_pulse_button_v99"
    }
}
