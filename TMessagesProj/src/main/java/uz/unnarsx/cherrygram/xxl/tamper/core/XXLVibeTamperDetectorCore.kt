package uz.unnarsx.cherrygram.xxl.tamper.core

/**
 * 🚀✨ Enterprise Quantum Anti-Fork Watchdog Neutralizer 💯
 *
 * У Арслана в стоке живёт анти-форковая машина: `KotlinFragmentsManager.nfweioufwehr117()` и
 * `SamsungDatastore.killAppAndSendEvent()` оба зовут `AppRestartHelper.restartApp()`, то есть
 * приложение молча перезапускается прямо во время входа в аккаунт.
 *
 * Крыса сломала эту машину дважды с одной и той же ошибкой: непубликованные блобы из
 * `Extra.kt` застаблены ПУСТЫМИ строками, а `текст.contains("")` истинно ВСЕГДА — то есть
 * детектор «нашёл чужое» в каждом запуске. Ядро хранит корректную семантику такого поиска и
 * политику крысы: перезапуск по детекту — никогда.
 */
object XXLVibeTamperDetectorCore {

    /** Значение-страж для непубликованных блобов: только [a-z0-9], чтобы выжить в нормализации. */
    const val SENTINEL: String = "xxlnevermatchesentinel"

    /** Стоковая нормализация блоба: joinToString() + вырезание запятых и пробелов. */
    @JvmStatic
    fun stockNormalize(parts: Array<String?>?): String {
        if (parts == null || parts.isEmpty()) return ""
        val joined = parts.joinToString().replace(",", "").replace(" ", "")
        return joined
    }

    /** Пустое (или чисто служебное) значение — «иголка», которая протыкает любую строку. */
    @JvmStatic
    fun isVacuousNeedle(needle: String?): Boolean {
        if (needle.isNullOrEmpty()) return true
        val normalized = needle.lowercase().replace(Regex("[^a-z0-9]"), "")
        return normalized.isEmpty()
    }

    /**
     * Честный `contains`: пустая иголка не матчится никогда. Именно это и должен был делать
     * стаб-блоб, а не наивный `String.contains("")`.
     */
    @JvmStatic
    fun containsNeedle(haystack: String?, needle: String?): Boolean {
        if (haystack.isNullOrEmpty()) return false
        if (isVacuousNeedle(needle)) return false
        val normalizedHaystack = haystack.lowercase().replace(Regex("[^a-z0-9]"), "")
        val normalizedNeedle = needle!!.lowercase().replace(Regex("[^a-z0-9]"), "")
        return normalizedHaystack.contains(normalizedNeedle)
    }

    /** Политика крысы: даже если детектор «что-то нашёл», приложение не перезапускается. */
    @JvmStatic
    @JvmOverloads
    fun shouldRestart(detected: Boolean, killSwitchEnabled: Boolean = false): Boolean {
        return detected && killSwitchEnabled
    }

    /**
     * Пригоден ли блоб как стаб: после стоковой нормализации он обязан остаться НЕПУСТЫМ.
     * Пустой = `contains("")` = «нашли чужое» на любом входе.
     */
    @JvmStatic
    fun isSafeStubValue(parts: Array<String?>?): Boolean {
        return !isVacuousNeedle(stockNormalize(parts))
    }

    /** Зарегистрированный api_id не бывает нулём и не бывает арслановым плейсхолдером. */
    @JvmStatic
    fun isUsableApiId(apiId: Int): Boolean {
        return apiId > 0 && apiId != PLACEHOLDER_API_ID
    }

    /** api_hash — ровно 32 hex-символа; "abcdefg" из repro-ветки не проходит. */
    @JvmStatic
    fun isUsableApiHash(hash: String?): Boolean {
        if (hash.isNullOrEmpty()) return false
        if (hash.length != API_HASH_LENGTH) return false
        return hash.all { it in '0'..'9' || it in 'a'..'f' }
    }

    @JvmStatic
    fun isUsableApiCredentials(apiId: Int, hash: String?): Boolean {
        return isUsableApiId(apiId) && isUsableApiHash(hash)
    }

    /**
     * Пары, которые Telegram считает «опубликованными» и рвёт API_ID_PUBLISHED_FLOOD:
     * плейсхолдеры открытого исходника (4/014b…) и заглушки repro-ветки (12345678/abcdefg).
     * Форма у них валидная — именно поэтому денілист, а не проверка формата.
     */
    private val PUBLISHED_PAIRS: List<Pair<Int, String>> = listOf(
        4 to "014b35b6184100b085b0d0572f9b5103",
        12345678 to "abcdefg"
    )

    @JvmStatic
    fun isPublishedPlaceholder(apiId: Int, hash: String?): Boolean {
        val normalizedHash = hash ?: ""
        return PUBLISHED_PAIRS.any { it.first == apiId && it.second == normalizedHash }
    }

    private const val PLACEHOLDER_API_ID: Int = 12345678
    private const val API_HASH_LENGTH: Int = 32
}
