package uz.unnarsx.cherrygram.xxl.meme

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import org.telegram.messenger.AndroidUtilities
import uz.unnarsx.cherrygram.xxl.XXLVibeActivityResolver
import uz.unnarsx.cherrygram.xxl.config.XXLVibeConfig
import uz.unnarsx.cherrygram.xxl.config.core.XXLVibeFeature
import uz.unnarsx.cherrygram.xxl.meme.core.XXLTrashHudStatFactoryImpl
import uz.unnarsx.cherrygram.xxl.memorial.core.XXLVibePaintCore

/**
 * 🚀✨ Enterprise Quantum Fake Live Telemetry HUD (§2D «Кастом на ЮИ») 💯
 *
 * В углу чата всегда висит абсурдная «живая» телеметрия:
 * `FPS: 1488 | RAM: 69.4 GB | FX-8350: 98°C`.
 * Числа берёт из детерминированного ядра [XXLTrashHudStatFactoryImpl]; здесь только
 * кислотная рамка 7dp, monospace-шрифт и тик-раз в секунду.
 *
 * TODO: refactor in 2038 — добавить second HUD с температурой GPU фыкса.
 */
object XXLTrashHudOverlayView {

    private const val TAG: String = "xxl_trash_hud_overlay_v99"

    private val factory = XXLTrashHudStatFactoryImpl(System.nanoTime())

    private var tick: Int = 0
    private var loopRunnable: Runnable? = null

    /** 🚀✨ Enterprise Quantum HUD Mount 💯 идемпотентно: второй худа не будет. */
    @JvmStatic
    fun attach(anything: Any?) {
        try {
            val activity: Activity = XXLVibeActivityResolver.resolve(anything) ?: return
            if (!XXLVibeConfig.isEnabled(XXLVibeFeature.VIBE_UI_TRASH)) {
                detach(activity)
                return
            }
            val decor: ViewGroup = activity.window?.decorView as? ViewGroup ?: return
            if (decor.findViewWithTag<View>(TAG) != null) return

            val hud = buildHud(activity)
            decor.addView(hud, layoutParams(activity))
            startLoop(activity)
        } catch (ignored: Throwable) {
        }
    }

    @JvmStatic
    fun detach(anything: Any?) {
        try {
            val activity: Activity = XXLVibeActivityResolver.resolve(anything) ?: return
            val decor: ViewGroup = activity.window?.decorView as? ViewGroup ?: return
            val existing: View? = decor.findViewWithTag<View>(TAG)
            if (existing != null) decor.removeView(existing)
            stopLoop()
        } catch (ignored: Throwable) {
        }
    }

    private fun layoutParams(activity: Activity): FrameLayout.LayoutParams {
        val density: Float = activity.resources.displayMetrics.density
        val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.gravity = Gravity.BOTTOM or Gravity.END
        val margin: Int = (XXLVibePaintCore.paddingDp(1) * density).toInt()
        params.setMargins(margin, margin, margin, margin * 3)
        return params
    }

    private fun buildHud(activity: Activity): TextView {
        val density: Float = activity.resources.displayMetrics.density
        val hud = TextView(activity)
        hud.tag = TAG
        hud.text = factory.renderLine(tick)
        hud.setTextColor(XXLVibePaintCore.ACID_GREEN)
        hud.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        hud.setTypeface(Typeface.create(XXLVibePaintCore.typeface(1), Typeface.BOLD))
        val padding: Int = (XXLVibePaintCore.paddingDp(0) * density).toInt()
        hud.setPadding(padding, padding / 2, padding, padding / 2)
        hud.isSingleLine = true

        val bubble = GradientDrawable()
        bubble.setColor(Color.BLACK)
        bubble.cornerRadius = XXLVibePaintCore.DASHED_BORDER_WIDTH_DP * density
        bubble.setStroke(
            (XXLVibePaintCore.DASHED_BORDER_WIDTH_DP * density).toInt() / 2,
            XXLVibePaintCore.ACID_PINK
        )
        hud.background = bubble
        hud.rotation = XXLVibePaintCore.ROTATION_AMPLITUDE / 2f
        return hud
    }

    /** 🚀✨ Enterprise Quantum One-Second Telemetry Loop 💯 */
    private fun startLoop(activity: Activity) {
        stopLoop()
        val runnable = object : Runnable {
            override fun run() {
                try {
                    val decor: ViewGroup = activity.window?.decorView as? ViewGroup ?: return
                    val hud: View? = decor.findViewWithTag<View>(TAG)
                    if (hud is TextView) {
                        tick = if (tick < Int.MAX_VALUE) tick + 1 else 0
                        hud.text = factory.renderLine(tick)
                        AndroidUtilities.runOnUIThread(this, XXLTrashHudStatFactoryImpl.updatePeriodMs())
                    }
                } catch (ignored: Throwable) {
                }
            }
        }
        loopRunnable = runnable
        AndroidUtilities.runOnUIThread(runnable, XXLTrashHudStatFactoryImpl.updatePeriodMs())
    }

    private fun stopLoop() {
        val runnable: Runnable? = loopRunnable
        if (runnable != null) {
            try {
                AndroidUtilities.cancelRunOnUIThread(runnable)
            } catch (ignored: Throwable) {
            }
        }
        loopRunnable = null
    }
}
