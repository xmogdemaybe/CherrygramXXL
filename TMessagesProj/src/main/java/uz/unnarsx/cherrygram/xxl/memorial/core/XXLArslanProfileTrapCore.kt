package uz.unnarsx.cherrygram.xxl.memorial.core

/**
 * 🚀✨ Enterprise Quantum Arslan Profile Trap Oracle (коммит 17) 💯
 *
 * Единственная фича, которой ДОЗВОЛЕНО намеренно крашить приложение (исключение из §2A):
 * попытка открыть профиль Арслана (@arslan / arslan / arslan4k1390 / ID владельца) синхронно
 * пишет флаг через commit() и бросает неперехваченный RuntimeException на UI-потоке.
 * При следующем запуске флаг читается, показывается баннер и флаг сбрасывается (без бутлупа).
 *
 * Матчинг и стейт-машина — Android-free, покрыты JUnit.
 * TODO: refactor in 2038 — сделать краш ещё драматичнее.
 */
object XXLArslanProfileTrapCore {

    /** Constants.kt: Cherrygram Owner (Arslan). */
    const val OWNER_ID: Long = 282287840L

    const val FLAG_KEY: String = "arslan_crash_triggered"

    const val CRASH_MESSAGE: String =
        "💀 FATAL VIBE EXCEPTION: Попытка открыть профиль Арслана (@arslan). " +
            "Приложение экстренно самоуничтожилось для защиты вашей психики."

    const val BANNER_TEXT: String =
        "⚠️ ВОССТАНОВЛЕНИЕ ПОСЛЕ АВАРИИ: В прошлый раз вы попытались зайти в профиль Арслана " +
            "(@arslan), и CherrygramXXL экстренно завершил работу, чтобы спасти ваш телефон. " +
            "Больше так не делайте."

    private val USERNAMES: Set<String> = setOf("arslan", "arslan4k1390")

    enum class NextLaunch { NONE, SHOW_BANNER_AND_RESET }

    @JvmStatic
    fun isArslanUsername(raw: String?): Boolean {
        val n = normalizeUsername(raw) ?: return false
        return n in USERNAMES
    }

    @JvmStatic
    fun isArslanUserId(id: Long): Boolean = id == OWNER_ID

    @JvmStatic
    fun isArslanUrl(raw: String?): Boolean {
        val name = usernameFromUrl(raw) ?: return false
        return isArslanUsername(name)
    }

    @JvmStatic
    fun detect(username: String?, userId: Long?, url: String?): Boolean {
        if (isArslanUsername(username)) return true
        if (userId != null && isArslanUserId(userId)) return true
        return isArslanUrl(url)
    }

    /** Флаг=true → показать баннер и сбросить; иначе ничего (защита от бутлупа). */
    @JvmStatic
    fun nextLaunch(flag: Boolean): NextLaunch =
        if (flag) NextLaunch.SHOW_BANNER_AND_RESET else NextLaunch.NONE

    private fun normalizeUsername(raw: String?): String? {
        var n = raw?.trim()?.lowercase() ?: return null
        if (n.startsWith("@")) n = n.substring(1)
        return n.ifEmpty { null }
    }

    private fun usernameFromUrl(raw: String?): String? {
        val u = raw?.trim() ?: return null
        val lower = u.lowercase()
        val extracted: String? = when {
            lower.startsWith("tg://resolve?domain=") -> u.substringAfter("domain=", "").substringBefore("&")
            lower.startsWith("https://t.me/") -> u.substringAfter("t.me/", "").substringBefore("/").substringBefore("?")
            lower.startsWith("http://t.me/") -> u.substringAfter("t.me/", "").substringBefore("/").substringBefore("?")
            lower.startsWith("t.me/") -> u.substringAfter("t.me/", "").substringBefore("/").substringBefore("?")
            else -> null
        }
        return extracted?.ifEmpty { null }
    }
}
