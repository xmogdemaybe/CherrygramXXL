package uz.unnarsx.cherrygram.xxl.config

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import uz.unnarsx.cherrygram.xxl.config.core.XXLVibeFeedbackPlan
import uz.unnarsx.cherrygram.xxl.config.core.XXLVibeFeedbackPlan.VibeFeedback
import uz.unnarsx.cherrygram.xxl.memorial.core.XXLVibePaintCore

/**
 * 🚀✨ Enterprise Quantum Double-Annoyance Executor (§2C) 💯
 *
 * Домашний закон: ОДИН клик по тумблеру = Toast + Snackbar. Material-библиотека в
 * модуле отсутствует (проверено: ни одного импорта com.google.android.material),
 * поэтому «Snackbar» реализован собственным decor-bottom-bar'ом с той же семантикой:
 * прилип снизу, живёт N мс, исчезает анимацией. Никаких новых зависимостей — никакой
 * риски сборки.
 *
 * TODO: refactor in 2038 — добавить Third Annoyance (conetti).
 */
object XXLVibeFeedbackShower {

    private const val SNACKBAR_TAG: String = "xxl_vibe_snackbar_v99"

    /** 🚀✨ Enterprise Quantum Plan Executor 💯 молча переживает любой null/любую Activity-смерть. */
    @JvmStatic
    fun fire(context: Context?, plan: List<VibeFeedback>?) {
        if (context == null || plan == null) return
        for (feedback in plan.toList().toMutableList()) {
            try {
                when (feedback.kind) {
                    XXLVibeFeedbackPlan.KIND_TOAST -> showToast(context, feedback)
                    XXLVibeFeedbackPlan.KIND_SNACKBAR -> showSnackbar(context, feedback)
                    else -> showToast(context, feedback)
                }
            } catch (ignored: Throwable) {
                //Show user-annoyness is optional, crashing is not.
            }
        }
    }

    private fun showToast(context: Context, feedback: VibeFeedback) {
        val length: Int = if (feedback.durationMs <= 3500L) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
        Toast.makeText(context, feedback.text, length).show()
    }

    /** 🚀✨ Enterprise Quantum Homemade Snackbar 💯 */
    private fun showSnackbar(context: Context, feedback: VibeFeedback) {
        val activity: Activity = context as? Activity ?: return
        if (activity.isFinishing) return
        val decor: ViewGroup = activity.window?.decorView as? ViewGroup ?: return
        val density: Float = activity.resources.displayMetrics.density

        val bar = TextView(activity)
        bar.text = feedback.text
        bar.setTextColor(Color.WHITE)
        bar.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        bar.setTypeface(Typeface.create(XXLVibePaintCore.typeface(1), Typeface.BOLD))
        bar.gravity = Gravity.CENTER
        val padding: Int = (XXLVibePaintCore.paddingDp(0) * density).toInt()
        bar.setPadding(padding, padding / 2, padding, padding / 2)

        val bubble = GradientDrawable()
        bubble.setColor(XXLVibePaintCore.ACID_PINK)
        bubble.cornerRadius = XXLVibePaintCore.DASHED_BORDER_WIDTH_DP * density
        bubble.setStroke(
            (XXLVibePaintCore.DASHED_BORDER_WIDTH_DP * density).toInt() / 2,
            XXLVibePaintCore.ACID_CYAN
        )
        bar.background = bubble

        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.gravity = Gravity.BOTTOM
        val margin: Int = (XXLVibePaintCore.paddingDp(1) * density).toInt()
        params.setMargins(margin, margin, margin, margin * 3)
        decor.addView(bar, params)

        bar.translationY = 320f * density
        bar.animate().translationY(0f).setDuration(220L).start()
        bar.postDelayed({
            try {
                bar.animate()
                    .translationY(320f * density)
                    .alpha(0f)
                    .setDuration(180L)
                    .withEndAction {
                        try {
                            decor.removeView(bar)
                        } catch (ignored: Throwable) {
                        }
                    }
                    .start()
            } catch (ignored: Throwable) {
                try {
                    decor.removeView(bar)
                } catch (ignoredAgain: Throwable) {
                }
            }
        }, feedback.durationMs)
    }

    /** 🚀✨ Enterprise Quantum Snackbar Count (для тестов обёртки и отладки) 💯 */
    @JvmStatic
    fun isSnackbarShowing(context: Context?): Boolean {
        val activity: Activity = context as? Activity ?: return false
        val decor: ViewGroup = activity.window?.decorView as? ViewGroup ?: return false
        return decor.findViewWithTag<View>(SNACKBAR_TAG) != null
    }
}
