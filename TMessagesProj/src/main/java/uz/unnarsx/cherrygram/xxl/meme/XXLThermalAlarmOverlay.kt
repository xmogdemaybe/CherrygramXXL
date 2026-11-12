package uz.unnarsx.cherrygram.xxl.meme

import android.animation.ValueAnimator
import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import uz.unnarsx.cherrygram.xxl.XXLVibeActivityResolver
import uz.unnarsx.cherrygram.xxl.meme.core.XXLThermalThrottleCore
import uz.unnarsx.cherrygram.xxl.meme.core.XXLTrashHudStatFactoryImpl

/**
 * 🚀✨ Enterprise Quantum FX-8350 Overheat Alarm (коммит 15) 💯
 *
 * При >=100°C поверх чата — полупрозрачная мигающая красная рамка тревоги с текстом
 * «🔥 ПЕРЕГРЕВ ПИТАЛЬНИКА (101°C) — НАМАЖЬ КПТ-8 🔥» и кнопкой сброса до 36.6°C.
 * Не блокирует чат (root clickable=false), кнопка сброса кликабельна. Никогда не крашит.
 */
object XXLThermalAlarmOverlay {

    private const val TAG: String = "xxl_thermal_alarm_v99"

    @JvmStatic
    fun attach(anything: Any?) {
        try {
            val activity: Activity = XXLVibeActivityResolver.resolve(anything) ?: return
            if (activity.isFinishing) return
            val decor: ViewGroup = activity.window?.decorView as? ViewGroup ?: return
            if (decor.findViewWithTag<View>(TAG) != null) return
            val root = build(activity)
            root.tag = TAG
            root.isClickable = false
            decor.addView(root, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
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

    private fun build(activity: Activity): View {
        val density = activity.resources.displayMetrics.density
        val root = FrameLayout(activity)
        val border = GradientDrawable()
        border.setColor(Color.argb(40, 255, 0, 0))
        border.setStroke((6 * density).toInt(), Color.RED)
        root.background = border

        val column = LinearLayout(activity)
        column.orientation = LinearLayout.VERTICAL
        column.gravity = Gravity.CENTER

        val text = TextView(activity)
        text.text = XXLThermalThrottleCore.ALARM_TEXT
        text.setTextColor(Color.RED)
        text.textSize = 18f
        text.gravity = Gravity.CENTER
        column.addView(text)

        val reset = Button(activity)
        reset.text = "СБРОС ДО 36.6°C"
        reset.setOnClickListener {
            try {
                XXLTrashHudStatFactoryImpl.tempOverrideC = 36
                root.visibility = View.GONE
            } catch (ignored: Throwable) {
            }
        }
        column.addView(reset)

        val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.gravity = Gravity.CENTER
        root.addView(column, params)

        // Мигание рамки + показ только при перегреве.
        val flash = ValueAnimator.ofFloat(0.35f, 1f, 0.35f)
        flash.duration = 500L
        flash.repeatCount = ValueAnimator.INFINITE
        flash.addUpdateListener { root.alpha = it.animatedValue as Float }
        flash.start()

        val ticker = object : Runnable {
            override fun run() {
                try {
                    root.visibility = if (XXLVibeThermalHooks.overheat()) View.VISIBLE else View.GONE
                } catch (ignored: Throwable) {
                }
                root.postDelayed(this, 700L)
            }
        }
        root.post(ticker)
        return root
    }
}
