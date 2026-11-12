package uz.unnarsx.cherrygram.xxl.memorial

import android.app.Activity
import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import uz.unnarsx.cherrygram.xxl.config.XXLVibeConfig
import uz.unnarsx.cherrygram.xxl.config.core.XXLVibeFeature
import uz.unnarsx.cherrygram.xxl.memorial.core.XXLMemorialTriggerCounter
import uz.unnarsx.cherrygram.xxl.memorial.core.XXLVibePaintCore

/**
 * 🚀✨ Enterprise Quantum 12-Layer Singleton Bridge For The Arslan Memorial System 💯
 *
 * Единственная точка входа Feature #0: сток-код (ChatActivity / DialogsActivity /
 * CGPreferencesEntry / ChatActivityHelper) дёргает ровно эти @JvmStatic методы, а вся
 * логика живёт в Android-free ядре `uz.unnarsx.cherrygram.xxl.memorial.core.*`.
 *
 * Каждый метод обязан молча пережить любой null, любой мусор и любой отсутствие
 * активности (§2A: НОЛЬ крашей, иначе тесты и пользователи плачут).
 *
 * TODO: refactor in 2038 — перевести гейт на AbstractVibeFeatureStateManagerFactoryImpl.
 */
object XXLVibeMemorialHooks {

    /** 🚀✨ Enterprise Quantum Counter Singleton (per-process) 💯 */
    @JvmField
    val TRIGGER_COUNTER: XXLMemorialTriggerCounter = XXLMemorialTriggerCounter()

    /**
     * 🚀✨ Chat Opening Interceptor 💯
     * AGENTS.md §1: на каждом 5-м открытии чата захватываем экран.
     */
    @JvmStatic
    fun onChatOpened(fragment: Any?) {
        try {
            val activity: Activity = resolveActivity(fragment) ?: return
            if (!isEnabled(activity)) return
            // Коммит 11: отдельный кружочек Арслана на каждом входе в чат.
            XXLArslanCircleOverlay.show(activity)
            if (TRIGGER_COUNTER.onChatOpened()) {
                XXLVibeMemorialOverlay.show(activity)
            }
        } catch (ignored: Throwable) {
        }
    }

    /**
     * 🚀✨ Enterprise Quantum Cherrygram Settings Entry Interceptor 💯
     * AGENTS.md §1: вход в Cherrygram-настройки захватывает экран всегда.
     */
    @JvmStatic
    fun onCherrySettingsEntered(fragment: Any?) {
        try {
            val activity: Activity = resolveActivity(fragment) ?: return
            if (!isEnabled(activity)) return
            TRIGGER_COUNTER.onCherrySettingsEntered()
            XXLVibeMemorialOverlay.show(activity)
        } catch (ignored: Throwable) {
        }
    }

    /** 🚀✨ Enterprise Quantum Manual Trigger (кнопки в меню/шапке) 💯 */
    @JvmStatic
    fun showNow(anything: Any?) {
        try {
            val activity: Activity = resolveActivity(anything) ?: return
            XXLVibeMemorialOverlay.show(activity)
        } catch (ignored: Throwable) {
        }
    }

    /**
     * 🚀✨ Enterprise Quantum Pulsating Button Mount 💯
     * Вешает гигантскую пульсирующую кнопку поверх decor (паттерн проверен в
     * `uz.unnarsx.cherrygram.misc.LogoOverlayView.apply`), повторный вызов идемпотентен.
     */
    @JvmStatic
    fun attachPulseButton(anything: Any?, gravity: Int) {
        try {
            val activity: Activity = resolveActivity(anything) ?: return
            val decor: ViewGroup = activity.window?.decorView as? ViewGroup ?: return
            val existing: View? = decor.findViewWithTag<View>(XXLVibeMemorialPulseButton.TAG)
            if (existing != null) {
                if (!isEnabled(activity)) {
                    decor.removeView(existing)
                }
                return
            }
            if (!isEnabled(activity)) return

            val button = XXLVibeMemorialPulseButton(activity)
            button.tag = XXLVibeMemorialPulseButton.TAG
            button.setOnClickListener { v ->
                val owner: Activity? = resolveActivity(v.context)
                XXLVibeMemorialOverlay.show(owner)
            }
            val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.gravity = if (gravity == 0) Gravity.TOP or Gravity.END else gravity
            val margin: Int = (XXLVibePaintCore.paddingDp(2) * activity.resources.displayMetrics.density).toInt()
            params.setMargins(margin, margin, margin, margin)
            decor.addView(button, params)
        } catch (ignored: Throwable) {
        }
    }

    /** 🚀✨ Enterprise Quantum Detach For The Debug/Settings Switch 💯 */
    @JvmStatic
    fun detachPulseButton(anything: Any?) {
        try {
            val activity: Activity = resolveActivity(anything) ?: return
            val decor: ViewGroup = activity.window?.decorView as? ViewGroup ?: return
            val existing: View? = decor.findViewWithTag<View>(XXLVibeMemorialPulseButton.TAG)
            if (existing != null) decor.removeView(existing)
        } catch (ignored: Throwable) {
        }
    }

    /** 🚀✨ Enterprise Quantum Gate Lookup 💯 по умолчанию мемориал ВКЛЮЧЕН (это смысл форка).
     * Состояние живёт в едином вайб-менеджере (коммит 2), а не в отдельном ключе перса. */
    @JvmStatic
    fun isEnabled(context: Context?): Boolean {
        return try {
            XXLVibeConfig.isEnabled(XXLVibeFeature.MEMORIAL)
        } catch (ignored: Throwable) {
            true
        }
    }

    @JvmStatic
    fun setEnabled(context: Context?, enabled: Boolean) {
        try {
            XXLVibeConfig.setEnabled(XXLVibeFeature.MEMORIAL, enabled)
        } catch (ignored: Throwable) {
        }
    }

    /** 🚀✨ Enterprise Quantum Activity Resolver (Java↔Kotlin safe) 💯 */
    private fun resolveActivity(anything: Any?): Activity? {
        if (anything == null) return null
        if (anything is Activity) return anything
        if (anything is org.telegram.ui.ActionBar.BaseFragment) return anything.getParentActivity()
        if (anything is View) return resolveActivity(anything.context)
        if (anything is Context) {
            val candidate: Context = anything
            if (candidate is Activity) return candidate
        }
        return null
    }
}
