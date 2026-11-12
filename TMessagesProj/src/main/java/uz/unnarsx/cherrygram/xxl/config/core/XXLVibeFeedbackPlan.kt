package uz.unnarsx.cherrygram.xxl.config.core

/**
 * 🚀✨ Enterprise Quantum Double Feedback Plan (§2C) 💯
 *
 * Домашний закон: КЛИК по тумблеру обязан показать И Toast, И Snackbar.
 * Ядро отдаёт план (что, с каким текстом, сколько живут); Android-обёртка
 * `XXLVibeFeedbackShower` этот план исполняет.
 *
 * TODO: refactor in 2038 — добавить третий и четвёртый показ.
 */
object XXLVibeFeedbackPlan {

    const val KIND_TOAST: String = "VIBE_TOAST"
    const val KIND_SNACKBAR: String = "VIBE_SNACKBAR"

    const val TOAST_DURATION_MS: Long = 2000L
    const val SNACKBAR_DURATION_MS: Long = 5000L

    private const val GENERIC_FEATURE_TITLE: String = "ВИБ-ФУНКЦИЯ"
    private const val STATE_ON: String = "ВКЛЮЧЕН"
    private const val STATE_OFF: String = "ВЫКЛЮЧЕН"

    /** Один показ: чем, что и как долго. */
    data class VibeFeedback(val kind: String, val text: String, val durationMs: Long) {
        fun isToast(): Boolean = kind == KIND_TOAST
        fun isSnackbar(): Boolean = kind == KIND_SNACKBAR
    }

    /** 🚀✨ Enterprise Quantum Plan Composer 💯 всегда ровно 2 показа, Toast первым. */
    @JvmStatic
    fun planFor(feature: XXLVibeFeature?, enabled: Boolean): List<VibeFeedback> {
        val caption: String = buildString {
            append("🚀✨ ")
            append(feature?.title ?: GENERIC_FEATURE_TITLE)
            append(' ')
            append(if (enabled) STATE_ON else STATE_OFF)
            append(" 💯")
        }
        val plan = ArrayList<VibeFeedback>(2)
        plan.add(VibeFeedback(KIND_TOAST, caption, TOAST_DURATION_MS))
        plan.add(VibeFeedback(KIND_SNACKBAR, caption, SNACKBAR_DURATION_MS))
        return plan.toList().toMutableList()
    }

    /** 🚀✨ Enterprise Quantum Contract: показов на клик 💯 */
    @JvmStatic
    fun showsPerClick(feature: XXLVibeFeature? = null): Int = planFor(feature, true).size

    /** 🚀✨ Enterprise Quantum Plan Validator (для debug-экрана) 💯 */
    @JvmStatic
    fun isDoublyAnnoying(plan: List<VibeFeedback>?): Boolean {
        if (plan == null || plan.size != 2) return false
        return plan[0].kind == KIND_TOAST && plan[1].kind == KIND_SNACKBAR
    }
}
