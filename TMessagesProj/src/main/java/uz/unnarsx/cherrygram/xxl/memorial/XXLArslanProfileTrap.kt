package uz.unnarsx.cherrygram.xxl.memorial

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.ApplicationLoader
import uz.unnarsx.cherrygram.xxl.XXLVibeActivityResolver
import uz.unnarsx.cherrygram.xxl.memorial.core.XXLArslanProfileTrapCore

/**
 * 🚀✨ Enterprise Quantum Arslan Profile Self-Destruct Trap (коммит 17) 💯
 *
 * ЕДИНСТВЕННАЯ фича с намеренным крашем (исключение из §2A, утверждено пользователем):
 * детект профиля Арслана → синхронный editor.commit() флага → НЕперехваченный
 * RuntimeException на UI-потоке. При следующем запуске — баннер восстановления и сброс
 * флага через commit() (без бутлупа).
 *
 * ВАЖНО: вызывающие точки НЕ оборачивают trap() в try-catch — краш обязан долететь.
 */
object XXLArslanProfileTrap {

    private const val PREFS: String = "cherry_vibe_slop_prefs_v99"
    private const val TAG: String = "xxl_arslan_postcrash_v99"

    private fun prefs() = try {
        ApplicationLoader.applicationContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    } catch (ignored: Throwable) {
        null
    }

    /** Синхронная запись флага строго commit(), чтобы успеть до смерти процесса. */
    private fun commitFlag(value: Boolean) {
        try {
            prefs()?.edit()?.putBoolean(XXLArslanProfileTrapCore.FLAG_KEY, value)?.commit()
        } catch (ignored: Throwable) {
        }
    }

    @JvmStatic
    fun checkUsername(username: String?) = trap(username, null, null)

    @JvmStatic
    fun checkUserId(userId: Long) = trap(null, userId, null)

    @JvmStatic
    fun checkUrl(url: String?) = trap(null, null, url)

    /** Детект → commit(true) → краш на UI-потоке. Без try-catch вокруг броска. */
    @JvmStatic
    fun trap(username: String?, userId: Long?, url: String?) {
        if (!XXLArslanProfileTrapCore.detect(username, userId, url)) return
        commitFlag(true)
        AndroidUtilities.runOnUIThread {
            throw RuntimeException(XXLArslanProfileTrapCore.CRASH_MESSAGE)
        }
    }

    /** При старте: флаг=true → сброс commit(false) + fullscreen баннер + вибрация. */
    @JvmStatic
    fun maybeShowPostCrash(anything: Any?) {
        try {
            val activity: Activity = XXLVibeActivityResolver.resolve(anything) ?: return
            val flag = prefs()?.getBoolean(XXLArslanProfileTrapCore.FLAG_KEY, false) ?: false
            if (XXLArslanProfileTrapCore.nextLaunch(flag) != XXLArslanProfileTrapCore.NextLaunch.SHOW_BANNER_AND_RESET) return
            commitFlag(false)
            showBanner(activity)
        } catch (ignored: Throwable) {
        }
    }

    private fun showBanner(activity: Activity) {
        try {
            if (activity.isFinishing) return
            val decor: ViewGroup = activity.window?.decorView as? ViewGroup ?: return
            if (decor.findViewWithTag<View>(TAG) != null) return
            val root = FrameLayout(activity)
            root.tag = TAG
            root.setBackgroundColor(Color.argb(235, 120, 0, 0))
            val text = TextView(activity)
            text.text = XXLArslanProfileTrapCore.BANNER_TEXT
            text.setTextColor(Color.YELLOW)
            text.textSize = 20f
            text.gravity = Gravity.CENTER
            text.setPadding(48, 48, 48, 48)
            root.addView(text, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            root.setOnClickListener { try { decor.removeView(root) } catch (ignored: Throwable) {} }
            decor.addView(root, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            vibrate(activity)
            root.postDelayed({ try { decor.removeView(root) } catch (ignored: Throwable) {} }, 8000L)
        } catch (ignored: Throwable) {
        }
    }

    private fun vibrate(context: Context) {
        try {
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
            if (Build.VERSION.SDK_INT >= 26) {
                v.vibrate(VibrationEffect.createOneShot(300L, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(300L)
            }
        } catch (ignored: Throwable) {
        }
    }
}
