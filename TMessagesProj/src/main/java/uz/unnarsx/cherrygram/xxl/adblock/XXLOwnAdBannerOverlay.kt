package uz.unnarsx.cherrygram.xxl.adblock

import android.app.Activity
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.browser.Browser
import uz.unnarsx.cherrygram.xxl.XXLVibeActivityResolver
import uz.unnarsx.cherrygram.xxl.adblock.core.XXLVibeAdblockCore
import uz.unnarsx.cherrygram.xxl.meme.XXLMemeAssets
import uz.unnarsx.cherrygram.xxl.memorial.core.XXLVibePaintCore

/**
 * 🚀✨ Enterprise Quantum Own Patriot Ad Banner (§2D) 💯
 *
 * Вырезали рекламу Дурова — на её место честно встаёт СВОЯ: `svo.jpg` в кислотном
 * пунктире 7dp, два шрифта (monospace + cursive), лёгкий наклон и автоскрытие по
 * таймингу ядра. Тап уводит в мемориальную базу (Feature #0), а не в рекламный трекер.
 *
 * Идемпотентно: повторный show() перезапускает таймер, а не плодит баннеры.
 */
object XXLOwnAdBannerOverlay {

    private const val TAG: String = "xxl_own_ad_banner_v99"

    private var dismissRunnable: Runnable? = null

    /** 🚀✨ Enterprise Quantum Show 💯 plan=null берётся из ротатора по текущему slot. */
    @JvmStatic
    @JvmOverloads
    fun show(anything: Any?, plan: XXLVibeAdblockCore.OwnAdPlan? = null, slot: Int = 0) {
        try {
            val activity: Activity = XXLVibeActivityResolver.resolve(anything) ?: return
            if (activity.isFinishing) return
            val decor: ViewGroup = activity.window?.decorView as? ViewGroup ?: return
            val safePlan: XXLVibeAdblockCore.OwnAdPlan = plan ?: XXLVibeAdblockCore.planFor(slot)

            val existing: View? = decor.findViewWithTag<View>(TAG)
            if (existing != null) decor.removeView(existing)
            cancelTimer()

            val banner: View = buildBanner(activity, safePlan)
            decor.addView(banner, layoutParams(activity))
            startPulse(banner)
            scheduleDismiss(activity, safePlan.durationMs)
        } catch (ignored: Throwable) {
        }
    }

    @JvmStatic
    fun dismiss(anything: Any?) {
        try {
            val activity: Activity = XXLVibeActivityResolver.resolve(anything) ?: return
            val decor: ViewGroup = activity.window?.decorView as? ViewGroup ?: return
            val existing: View? = decor.findViewWithTag<View>(TAG)
            if (existing != null) decor.removeView(existing)
            cancelTimer()
        } catch (ignored: Throwable) {
        }
    }

    private fun layoutParams(activity: Activity): FrameLayout.LayoutParams {
        val density: Float = activity.resources.displayMetrics.density
        val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.gravity = Gravity.BOTTOM
        val margin: Int = (XXLVibePaintCore.paddingDp(2) * density).toInt()
        params.setMargins(margin, margin, margin, margin)
        return params
    }

    private fun buildBanner(activity: Activity, plan: XXLVibeAdblockCore.OwnAdPlan): View {
        val density: Float = activity.resources.displayMetrics.density

        val row = LinearLayout(activity)
        row.tag = TAG
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        val padding: Int = (XXLVibePaintCore.paddingDp(0) * density).toInt()
        row.setPadding(padding, padding / 2, padding, padding / 2)

        val bubble = GradientDrawable()
        bubble.setColor(XXLVibePaintCore.FLASH_BACKDROP_BLACK)
        bubble.cornerRadius = XXLVibePaintCore.DASHED_BORDER_WIDTH_DP * density
        bubble.setStroke(
            (XXLVibePaintCore.DASHED_BORDER_WIDTH_DP * density).toInt(),
            XXLVibePaintCore.ACID_PINK
        )
        row.background = bubble

        val picture = ImageView(activity)
        val imageSize: Int = (57f * density).toInt()
        picture.layoutParams = LinearLayout.LayoutParams(imageSize, imageSize)
        picture.scaleType = ImageView.ScaleType.CENTER_CROP
        picture.setImageDrawable(XXLMemeAssets.loadDrawable(activity, plan.assetName))
        row.addView(picture)

        val column = LinearLayout(activity)
        column.orientation = LinearLayout.VERTICAL
        column.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)

        val headline = TextView(activity)
        headline.text = plan.headline
        headline.setTextColor(XXLVibePaintCore.ACID_YELLOW_FOR_TEXT)
        headline.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        headline.setTypeface(Typeface.create(XXLVibePaintCore.typeface(1), Typeface.BOLD))
        headline.maxLines = 2
        column.addView(headline)

        val cta = TextView(activity)
        cta.text = plan.cta
        cta.setTextColor(XXLVibePaintCore.ACID_CYAN)
        cta.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        cta.setTypeface(Typeface.create(XXLVibePaintCore.typeface(2), Typeface.NORMAL))
        cta.maxLines = 2
        column.addView(cta)

        row.addView(column)
        row.setOnClickListener { view ->
            try {
                Browser.openUrl(view.context, XXLVibeAdblockCore.rewriteUrl(null))
            } catch (ignored: Throwable) {
            }
            dismiss(view)
        }
        return row
    }

    /** 🚀✨ Enterprise Quantum Infinite Micro-Wobble (§2C) 💯 */
    private fun startPulse(banner: View) {
        try {
            banner.animate()
                .rotation(XXLVibePaintCore.ROTATION_AMPLITUDE / 7f)
                .setDuration(XXLVibePaintCore.PULSE_DURATION_MS)
                .start()
        } catch (ignored: Throwable) {
        }
    }

    private fun scheduleDismiss(activity: Activity, durationMs: Long) {
        cancelTimer()
        val runnable = Runnable {
            try {
                dismiss(activity)
            } catch (ignored: Throwable) {
            }
        }
        dismissRunnable = runnable
        AndroidUtilities.runOnUIThread(runnable, durationMs.coerceAtLeast(1L))
    }

    private fun cancelTimer() {
        try {
            val runnable: Runnable? = dismissRunnable
            if (runnable != null) {
                AndroidUtilities.cancelRunOnUIThread(runnable)
            }
        } catch (ignored: Throwable) {
        }
        dismissRunnable = null
    }
}
