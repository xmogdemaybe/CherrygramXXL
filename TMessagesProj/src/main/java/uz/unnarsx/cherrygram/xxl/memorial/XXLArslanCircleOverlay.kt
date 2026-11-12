package uz.unnarsx.cherrygram.xxl.memorial

import android.animation.ValueAnimator
import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import uz.unnarsx.cherrygram.xxl.XXLVibeActivityResolver
import uz.unnarsx.cherrygram.xxl.meme.XXLMemeAssets
import uz.unnarsx.cherrygram.xxl.memorial.core.XXLMemorialPayload
import uz.unnarsx.cherrygram.xxl.memorial.core.XXLVibePaintCore

/**
 * 🚀✨ Enterprise Quantum Arslan Video-Circle Pop (§2D, коммит 11) 💯
 *
 * Отдельный «кружочек от Арслана» при входе в чат: круглое arslan.jpg с кислотной
 * пульсирующей рамкой, как видео-кружочек в Телеграме, поверх decorView. Не блокирует
 * чат (clickable=false), сам исчезает через несколько секунд. Никогда не крашит.
 */
object XXLArslanCircleOverlay {

    private const val TAG: String = "xxl_arslan_circle_v99"
    private const val LIFE_MS: Long = 4000L

    @JvmStatic
    fun show(anything: Any?) {
        try {
            val activity: Activity = XXLVibeActivityResolver.resolve(anything) ?: return
            if (activity.isFinishing) return
            val decor: ViewGroup = activity.window?.decorView as? ViewGroup ?: return
            val existing: View? = decor.findViewWithTag<View>(TAG)
            if (existing != null) decor.removeView(existing)

            val bitmap = XXLMemeAssets.loadBitmap(activity, XXLMemorialPayload.ARSLAN_CIRCLE_ASSET) ?: return
            val density: Float = activity.resources.displayMetrics.density
            val size: Int = (84 * density).toInt()

            val rounded = RoundedBitmapDrawableFactory.create(activity.resources, bitmap)
            rounded.isCircular = true
            val image = ImageView(activity)
            image.setImageDrawable(rounded)
            image.tag = TAG
            image.isClickable = false

            val ring = GradientDrawable()
            ring.shape = GradientDrawable.OVAL
            ring.setColor(Color.TRANSPARENT)
            ring.setStroke((3 * density).toInt(), XXLVibePaintCore.ACID_PINK)
            image.background = ring
            val pad: Int = (3 * density).toInt()
            image.setPadding(pad, pad, pad, pad)

            val params = FrameLayout.LayoutParams(size, size)
            params.gravity = Gravity.TOP or Gravity.END
            params.topMargin = (72 * density).toInt()
            params.marginEnd = (13 * density).toInt()
            decor.addView(image, params)

            pulse(image)
            image.postDelayed({ try { decor.removeView(image) } catch (ignored: Throwable) {} }, LIFE_MS)
        } catch (ignored: Throwable) {
        }
    }

    private fun pulse(view: View) {
        try {
            val animator = ValueAnimator.ofFloat(1f, 1.1f, 1f)
            animator.duration = 800L
            animator.repeatCount = ValueAnimator.INFINITE
            animator.addUpdateListener {
                val s = it.animatedValue as Float
                view.scaleX = s
                view.scaleY = s
            }
            animator.start()
        } catch (ignored: Throwable) {
        }
    }
}
