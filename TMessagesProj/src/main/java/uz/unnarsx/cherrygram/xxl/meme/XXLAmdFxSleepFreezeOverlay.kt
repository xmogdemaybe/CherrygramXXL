package uz.unnarsx.cherrygram.xxl.meme

import android.animation.ValueAnimator
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
import android.widget.ImageView
import android.widget.TextView
import uz.unnarsx.cherrygram.core.VibrateUtil
import uz.unnarsx.cherrygram.xxl.config.XXLVibeConfig
import uz.unnarsx.cherrygram.xxl.config.core.XXLVibeFeature
import uz.unnarsx.cherrygram.xxl.meme.core.XXLAmdFxTriggerModel
import uz.unnarsx.cherrygram.xxl.meme.core.XXLMemeAssetSource
import uz.unnarsx.cherrygram.xxl.memorial.core.XXLVibePaintCore
import org.telegram.messenger.AndroidUtilities

/**
 * 🚀✨ Enterprise Quantum AMD FX-8350 Sleep Freeze Overlay (§2D) 💯
 *
 * Показывает `amdfx.jpg` с капсом «АМД ФЫКС ПЕРЕШЕЛ В СОН», а РОВНО через
 * [XXLAmdFxTriggerModel.SHOW_DELAY_MS] вешает UI-тред на
 * [XXLAmdFxTriggerModel.FREEZE_MS] — телефон жёстко фризит и не успевает дать ANR.
 *
 * TODO: refactor in 2038 — добавить звук вентилятора на 120 дБ.
 */
object XXLAmdFxSleepFreezeOverlay {

    private const val TAG: String = "xxl_amdfx_freeze_overlay_v99"

    /** Единственный планировщик на процесс — детерминизм только в ядре, тут живой скролл. */
    @JvmField
    val TRIGGER_MODEL: XXLAmdFxTriggerModel = XXLAmdFxTriggerModel(seed = System.nanoTime())

    /**
     * 🚀✨ Enterprise Quantum Scroll Feed 💯
     * Вызывается из ChatActivity.onScrolled на каждый кадр скролла.
     */
    @JvmStatic
    fun onChatAction(activity: Activity?) {
        try {
            if (!XXLVibeConfig.isEnabled(XXLVibeFeature.MEME_PAYLOADS)) {
                return
            }
            val decision = TRIGGER_MODEL.onAction()
            if (decision.fire) {
                show(activity, decision.showDelayMs, decision.freezeMs)
            }
        } catch (ignored: Throwable) {
        }
    }

    /** 🚀✨ Enterprise Quantum Show + Scheduled UI Hang 💯 */
    @JvmStatic
    fun show(activity: Activity?, showDelayMs: Long = XXLAmdFxTriggerModel.SHOW_DELAY_MS, freezeMs: Long = XXLAmdFxTriggerModel.FREEZE_MS) {
        try {
            if (activity == null || activity.isFinishing) return
            val decor: ViewGroup = activity.window?.decorView as? ViewGroup ?: return
            if (decor.findViewWithTag<View>(TAG) != null) return

            val overlay = buildOverlay(activity)
            decor.addView(overlay, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

            try {
                VibrateUtil.vibrate(XXLVibePaintCore.VIBRATION_MS)
            } catch (ignored: Throwable) {
            }

            overlay.scaleX = 0.35f
            overlay.scaleY = 0.35f
            overlay.animate().scaleX(1f).scaleY(1f).setDuration(320L).start()

            // §2D: ровно через SHOW_DELAY_MS вешаем главный поток. Не раньше и не позже.
            AndroidUtilities.runOnUIThread({
                try {
                    if (freezeMs > 0 && freezeMs < XXLAmdFxTriggerModel.ANR_LIMIT_MS) {
                        Thread.sleep(freezeMs)
                    }
                } catch (ignored: Throwable) {
                    // прервали сон фыкса — ну и ладно, форк живой
                }
            }, if (showDelayMs > 0) showDelayMs else XXLAmdFxTriggerModel.SHOW_DELAY_MS)

            AndroidUtilities.runOnUIThread({ dismiss(activity) }, AUTO_DISMISS_MS)
        } catch (ignored: Throwable) {
        }
    }

    @JvmStatic
    fun dismiss(activity: Activity?) {
        try {
            val decor: ViewGroup = activity?.window?.decorView as? ViewGroup ?: return
            val existing: View? = decor.findViewWithTag<View>(TAG)
            if (existing != null) decor.removeView(existing)
        } catch (ignored: Throwable) {
        }
    }

    private fun buildOverlay(activity: Activity): View {
        val density: Float = activity.resources.displayMetrics.density
        val root = FrameLayout(activity)
        root.tag = TAG
        root.isClickable = true
        root.setOnClickListener { dismiss(activity) }

        val image = ImageView(activity)
        image.setImageDrawable(XXLMemeAssets.loadDrawable(activity, XXLMemeAssetSource.AMD_FX_ASSET))
        image.scaleType = ImageView.ScaleType.FIT_CENTER
        val imageParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (260f * density).toInt())
        imageParams.gravity = Gravity.CENTER
        root.addView(image, imageParams)

        val caption = TextView(activity)
        caption.text = CAPTION
        caption.setTextColor(XXLVibePaintCore.ACID_YELLOW_FOR_TEXT)
        caption.setTextSize(TypedValue.COMPLEX_UNIT_SP, 23f)
        caption.setTypeface(Typeface.create(XXLVibePaintCore.typeface(2), Typeface.BOLD))
        caption.gravity = Gravity.CENTER
        caption.setSingleLine(true)
        caption.ellipsize = android.text.TextUtils.TruncateAt.MARQUEE
        caption.isSelected = true
        val captionParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        captionParams.gravity = Gravity.BOTTOM
        val margin: Int = (XXLVibePaintCore.paddingDp(3) * density).toInt()
        captionParams.setMargins(margin, margin, margin, margin * 2)
        root.addView(caption, captionParams)

        val bubble = GradientDrawable()
        bubble.setColor(XXLVibePaintCore.FLASH_BACKDROP_BLACK)
        bubble.setStroke((XXLVibePaintCore.DASHED_BORDER_WIDTH_DP * density).toInt(), XXLVibePaintCore.ACID_PINK)
        root.background = bubble
        return root
    }

    /** Капс из AGENTS.md §2D — буквально. */
    const val CAPTION: String = "АМД ФЫКС ПЕРЕШЕЛ В СОН"

    private const val AUTO_DISMISS_MS: Long = 6900L
}
