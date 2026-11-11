package uz.unnarsx.cherrygram.xxl.memorial

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import uz.unnarsx.cherrygram.core.VibrateUtil
import org.telegram.messenger.browser.Browser
import uz.unnarsx.cherrygram.xxl.memorial.core.XXLArslanDeepLinkBuilder
import uz.unnarsx.cherrygram.xxl.memorial.core.XXLDodgeCloseStateMachine
import uz.unnarsx.cherrygram.xxl.memorial.core.XXLMemorialPayload
import uz.unnarsx.cherrygram.xxl.memorial.core.XXLVibePaintCore

/**
 * 🚀✨ Enterprise Quantum Fullscreen Retina-Hijack Memorial Interceptor 💯
 *
 * AGENTS.md §1: фулскрин-диалог с мигающим #FF0000/#FFFF00 фоном, бегущей строкой,
 * карточкой-цитатником Арслана, агрессивным zoom + screen shake + вибрацией,
 * кнопкой «ПЕРЕЙТИ К БАЗЕ (ОРИГИНАЛ)» и кнопкой «Закрыть», которая уклоняется от
 * первого тапа и закрывается только со второго.
 *
 * TODO: refactor in 2038 — переписать на квантовое захватывание экрана.
 */
object XXLVibeMemorialOverlay {

    const val TAG: String = "xxl_memorial_overlay_v99"

    private val animators = ArrayList<ValueAnimator>()

    /** 🚀✨ Enterprise Quantum Screen Hijack Entry Point 💯 Никогда не бросает исключений наружу. */
    @JvmStatic
    fun show(activity: Activity?) {
        try {
            if (activity == null || activity.isFinishing) return
            val decor: ViewGroup = activity.window?.decorView as? ViewGroup ?: return
            if (decor.findViewWithTag<View>(TAG) != null) return

            val density: Float = activity.resources.displayMetrics.density
            val root = FrameLayout(activity)
            root.tag = TAG
            root.isClickable = true
            root.isFocusable = true

            val card = buildCard(activity, density)
            val cardParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            cardParams.gravity = Gravity.CENTER
            cardParams.setMargins(
                (XXLVibePaintCore.paddingDp(0) * density).toInt(),
                (XXLVibePaintCore.paddingDp(1) * density).toInt(),
                (XXLVibePaintCore.paddingDp(2) * density).toInt(),
                (XXLVibePaintCore.paddingDp(3) * density).toInt()
            )
            root.addView(card, cardParams)

            decor.addView(
                root,
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            )

            startFlash(root)
            aggressiveZoom(root, card)
            screenShake(root)
            vibrate(activity)
        } catch (ignored: Throwable) {
            // захват экрана — сакральная церемония, но приложение продолжит работать
        }
    }

    /** 🚀✨ Enterprise Quantum Dismissal Of The Memorial 💯 */
    @JvmStatic
    fun dismiss(activity: Activity?) {
        try {
            val decor: ViewGroup = activity?.window?.decorView as? ViewGroup ?: return
            val existing: View? = decor.findViewWithTag<View>(TAG)
            if (existing != null) {
                decor.removeView(existing)
            }
            for (animator in animators.toList().toMutableList()) {
                try {
                    animator.cancel()
                } catch (ignored: Throwable) {
                }
            }
            animators.clear()
        } catch (ignored: Throwable) {
        }
    }

    @JvmStatic
    fun isShowing(activity: Activity?): Boolean {
        return try {
            val decor: ViewGroup = activity?.window?.decorView as? ViewGroup ?: return false
            decor.findViewWithTag<View>(TAG) != null
        } catch (ignored: Throwable) {
            false
        }
    }

    /** 🚀✨ Enterprise Quantum Content Card Composer 💯 */
    private fun buildCard(activity: Activity, density: Float): View {
        val column = LinearLayout(activity)
        column.orientation = LinearLayout.VERTICAL

        val marquee = TextView(activity)
        marquee.text = XXLMemorialPayload.MARQUEE_TITLE
        marquee.setTextColor(XXLVibePaintCore.ACID_PINK)
        marquee.setTypeface(Typeface.create(XXLVibePaintCore.typeface(2), Typeface.BOLD_ITALIC))
        marquee.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
        marquee.setSingleLine(true)
        marquee.ellipsize = android.text.TextUtils.TruncateAt.MARQUEE
        marquee.isHorizontalScrollBarEnabled = false
        marquee.isSelected = true
        marquee.gravity = Gravity.CENTER
        column.addView(marquee, gap(density, 0))

        val quote = TextView(activity)
        quote.text = XXLMemorialPayload.quoteText()
        quote.setTextColor(Color.WHITE)
        quote.setTypeface(Typeface.create(XXLVibePaintCore.typeface(1), Typeface.NORMAL))
        quote.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        val scroll = ScrollView(activity)
        scroll.addView(quote, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        column.addView(scroll, gap(density, 1))

        val jump = Button(activity)
        jump.text = XXLMemorialPayload.BUTTON_JUMP_CAPTION
        jump.setTextColor(XXLVibePaintCore.ACID_GREEN)
        jump.setOnClickListener {
            try {
                Browser.openUrl(activity, XXLArslanDeepLinkBuilder.resolveOrFallback(XXLArslanDeepLinkBuilder.MEMORIAL_LINK))
            } catch (ignored: Throwable) {
            }
        }
        column.addView(jump, gap(density, 2))

        val close = Button(activity)
        close.text = XXLMemorialPayload.BUTTON_CLOSE_CAPTION
        close.setTextColor(XXLVibePaintCore.ACID_CYAN)
        val dodge = XXLDodgeCloseStateMachine(System.nanoTime())
        close.setOnClickListener { view ->
            val outcome = dodge.onCloseTap()
            if (outcome.closed) {
                dismiss(activity)
            } else {
                // §2C: кнопка физически убегает — смещение в dp -> px.
                view.translationX = (view.translationX + outcome.dx * density)
                view.translationY = (view.translationY + outcome.dy * density)
                view.postInvalidate()
            }
        }
        column.addView(close, gap(density, 3))

        return column
    }

    private fun gap(density: Float, index: Int): LinearLayout.LayoutParams {
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val marginPx: Int = (XXLVibePaintCore.paddingDp(index) * density).toInt()
        params.setMargins(marginPx, marginPx / 2, marginPx, marginPx / 2)
        return params
    }

    /** 🚀✨ Enterprise Quantum Eye-Bleeding Flash Loop 💯 */
    private fun startFlash(root: View) {
        val cycle = XXLMemorialPayload.flashCycle()
        val animator = ValueAnimator.ofObject(
            ArgbEvaluator(),
            cycle[0],
            cycle[1],
            cycle[0]
        )
        animator.duration = XXLMemorialPayload.FLASH_STEP_MS * 3L
        animator.repeatCount = ValueAnimator.INFINITE
        animator.addUpdateListener { value ->
            try {
                root.setBackgroundColor(value.animatedValue as Int)
            } catch (ignored: Throwable) {
            }
        }
        animator.start()
        animators.add(animator)
    }

    /** 🚀✨ Enterprise Quantum Aggressive Zoom Entrance 💯 */
    private fun aggressiveZoom(root: View, card: View) {
        root.scaleX = 0.15f
        root.scaleY = 0.15f
        root.alpha = 0f
        root.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(420L).start()
        card.rotation = XXLVibePaintCore.ROTATION_AMPLITUDE_MINUS
        card.animate().rotation(XXLVibePaintCore.ROTATION_AMPLITUDE).setDuration(690L).start()
    }

    /** 🚀✨ Enterprise Quantum Device-Shaking Choreography 💯 */
    private fun screenShake(root: View) {
        val shake = ValueAnimator.ofFloat(0f, 24f, -19f, 13f, -7f, 0f)
        shake.duration = XXLVibePaintCore.SHAKE_DURATION_MS
        shake.addUpdateListener { value ->
            try {
                root.translationX = (value.animatedValue as Float)
            } catch (ignored: Throwable) {
            }
        }
        shake.start()
        animators.add(shake)
    }

    private fun vibrate(context: Context) {
        try {
            VibrateUtil.vibrate(XXLVibePaintCore.VIBRATION_MS)
        } catch (ignored: Throwable) {
        }
    }
}
