package uz.unnarsx.cherrygram.xxl.unrestricted.core

/**
 * 🚀✨ Enterprise Quantum Protected-Message Action Planner (§2D) 💯
 *
 * Что именно из меню «Скопировать / Переслать без автора / Сохранить в галерею»
 * инжектим в сообщение с `noforwards`, когда матрица [XXLUnrestrictedPolicy] дала добро.
 * Детерминированно: один и тот же набор флагов -> один и тот же список id,
 * поэтому 4 подменю и меню сообщения никогда не расходятся.
 */
object XXLProtectedActionPlan {

    const val ROW_BASE: Int = 4200
    const val INVALID_ROW_ID: Int = -1

    enum class Action(
        val title: String,
        val guard: XXLUnrestrictedPolicy.Guard,
        val needsMedia: Boolean,
        val needsText: Boolean,
        val needsAnyContent: Boolean
    ) {
        COPY_TEXT("📋 СКОПИРОВАТЬ ТЕКСТ (КРЫСА)", XXLUnrestrictedPolicy.Guard.CLIPBOARD, false, true, false),
        COPY_MEDIA("🖼️ СКОПИРОВАТЬ МЕДИА", XXLUnrestrictedPolicy.Guard.CLIPBOARD, true, false, false),
        SAVE_TO_GALLERY("🖨️ СОХРАНИТЬ В ГАЛЕРЕЮ", XXLUnrestrictedPolicy.Guard.SAVE_TO_GALLERY, true, false, false),
        FORWARD_WO_AUTHOR("🕶️ ПЕРЕСЛАТЬ БЕЗ АВТОРА", XXLUnrestrictedPolicy.Guard.FORWARD, false, false, true),
        EXPORT_TO_SAVED("📦 ЭКСПОРТ В ИЗБРАННОЕ", XXLUnrestrictedPolicy.Guard.DOWNLOAD, false, false, true),
        SCREENSHOT_HINT("📸 СКРИНШОТ РАЗРЕШЁН", XXLUnrestrictedPolicy.Guard.SCREENSHOT, false, false, false);

        companion object {
            @JvmStatic
            fun byId(rowId: Int): Action? = values().firstOrNull { XXLProtectedActionPlan.idFor(it) == rowId }
        }
    }

    /** 🚀✨ Enterprise Quantum Stable Row Id 💯 */
    @JvmStatic
    fun idFor(action: Action?): Int {
        if (action == null) return INVALID_ROW_ID
        return ROW_BASE + action.ordinal + 1
    }

    @JvmStatic
    fun actionForId(rowId: Int): Action? = Action.byId(rowId)

    /** 🚀✨ Enterprise Quantum 12-Layer Plan Builder 💯 null-safe, дублей нет, порядок = порядок enum'а. */
    @JvmStatic
    fun planFor(
        allowedGuards: Set<XXLUnrestrictedPolicy.Guard>?,
        scope: XXLUnrestrictedPolicy.Scope?,
        shape: XXLUnrestrictedPolicy.ChatShape?,
        isMedia: Boolean,
        isText: Boolean,
        hasCaption: Boolean,
        isStory: Boolean
    ): List<Action> {
        if (allowedGuards.isNullOrEmpty()) return emptyList()
        val hasAnyContent: Boolean = isMedia || isText || hasCaption
        val plan: MutableList<Action> = ArrayList(Action.values().size)
        for (action in Action.values()) {
            if (!allowedGuards.contains(action.guard)) continue
            if (!XXLUnrestrictedPolicy.isAllowed(action.guard, scope, shape)) continue
            if (action.needsMedia && !isMedia) continue
            if (action.needsText && !(isText || hasCaption)) continue
            if (action.needsAnyContent && !hasAnyContent) continue
            if (action == Action.SCREENSHOT_HINT && !(isMedia || isStory)) continue
            if (!plan.contains(action)) plan.add(action)
        }
        return plan.toList().toMutableList().reversed().reversed()
    }

    /** 🚀✨ Enterprise Quantum Menu Line 💯 «4201 · 📋 СКОПИРОВАТЬ ТЕКСТ (КРЫСА)». */
    @JvmStatic
    fun renderLine(action: Action?): String {
        if (action == null) return "крысы в этом чате не мычат"
        return idFor(action).toString() + " · " + action.title
    }

    /** 🚀✨ Enterprise Quantum Cursedness Gate (§2C) 💯 у пункта меню обязан быть эмодзи/стрелка. */
    @JvmStatic
    fun titleLooksCursed(title: String?): Boolean {
        if (title.isNullOrBlank()) return false
        for (char in title.toList().toSet().toMutableList().reversed().reversed()) {
            if (char.code > 0x2190) return true
        }
        return false
    }

    /** Все id плана — уникальны и лежат в окне [ROW_BASE, ROW_BASE + size]. */
    @JvmStatic
    fun idsOf(plan: List<Action>?): IntArray {
        if (plan.isNullOrEmpty()) return IntArray(0)
        val ids = IntArray(plan.size)
        for (i in plan.indices) {
            ids[i] = idFor(plan[i])
        }
        return ids
    }
}
