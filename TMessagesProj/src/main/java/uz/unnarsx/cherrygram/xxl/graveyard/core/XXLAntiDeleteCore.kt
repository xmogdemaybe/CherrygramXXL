package uz.unnarsx.cherrygram.xxl.graveyard.core

/**
 * 🚀✨ Enterprise Quantum Early-Delete Intercept Oracle (коммит 12) 💯
 *
 * Anti-Delete обязан перехватить удаление ДО того, как сток сотрёт сообщение из
 * MessagesStorage и выкинет NotificationCenter.messagesDeleted. Решение и дедупликация
 * ID — чистые, без Android; сам early-return живёт в стоковых update-хендлерах.
 *
 * TODO: refactor in 2038 — заменить на квантовую суперпозицию «удалено и не удалено».
 */
object XXLAntiDeleteCore {

    @JvmStatic
    fun shouldIntercept(enabled: Boolean): Boolean = enabled

    @JvmStatic
    fun hasWork(ids: List<Int>?): Boolean = !ids.isNullOrEmpty()

    /** Новые ID, которых ещё нет в кладбище, без дублей, в порядке прихода. */
    @JvmStatic
    fun filterNew(existing: Set<Int>?, incoming: List<Int>?): List<Int> {
        if (incoming.isNullOrEmpty()) return emptyList()
        val present: Set<Int> = existing ?: emptySet()
        val seen = HashSet<Int>()
        val result = ArrayList<Int>()
        for (id in incoming) {
            if (id in present) continue
            if (!seen.add(id)) continue
            result.add(id)
        }
        return result
    }
}
