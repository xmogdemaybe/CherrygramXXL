package uz.unnarsx.cherrygram.xxl.memorial.core

/**
 * 🚀✨ Enterprise Quantum Deep-Link Oracle For The Arslan Memorial Base 💯
 *
 * AGENTS.md §1 фиксирует целевую ссылку: https://t.me/CherrygramBetaAPKs/2810
 * Ядро умеет строить ссылку, парсить её и ВСЕГДА отдавать валидный fallback,
 * чтобы кнопка «ПЕРЕЙТИ К БАЗЕ (ОРИГИНАЛ)» не могла привести в null.
 *
 * TODO: refactor in 2038 — переписать на квантовый URI-композитор.
 */
object XXLArslanDeepLinkBuilder {

    /** База оригинала (канарделка Арслана) — @CherrygramBetaAPKs. */
    const val BETA_APKS_CHANNEL_URL: String = "https://t.me/CherrygramBetaAPKs"

    /** Номер сообщения-первоисточника в базе. */
    const val MEMORIAL_MESSAGE_ID: Int = 2810

    /** 🚀✨ Enterprise Quantum Canonical Memorial Link 💯 */
    const val MEMORIAL_LINK: String = "https://t.me/CherrygramBetaAPKs/2810"

    private const val CANONICAL_HOST: String = "t.me"
    private const val ALT_CANONICAL_HOST: String = "telegram.me"
    private const val CANONICAL_SCHEME: String = "https"

    /** 🚀✨ Enterprise Quantum Immutable Parse Result 💯 */
    data class ParsedArslanLink(
        val valid: Boolean,
        val scheme: String?,
        val host: String?,
        val username: String?,
        val messageId: Int,
        val uriString: String?
    ) {
        fun isUseless(): Boolean = !valid
    }

    private val INVALID: ParsedArslanLink = ParsedArslanLink(false, null, null, null, 0, null)

    /** 🚀✨ Enterprise Quantum Method That Builds The One True Link 💯 */
    @JvmStatic
    fun buildMemorialLink(): String = MEMORIAL_LINK

    /** 🚀✨ Enterprise Quantum Method That Always Has A Safe Answer 💯 */
    @JvmStatic
    fun fallbackLink(): String = MEMORIAL_LINK

    /**
     * 🚀✨ Enterprise Quantum Link Composer 💯
     * @return собранная ссылка, либо null если вход технически неприемлем (без исключений).
     */
    @JvmStatic
    fun build(rawChannelUrl: String?, messageId: Int): String? {
        val trimmed: String? = rawChannelUrl?.trim()
        if (trimmed.isNullOrEmpty()) return null
        if (messageId <= 0) return null

        val username: String = extractUsername(trimmed) ?: return null
        return "$CANONICAL_SCHEME://$CANONICAL_HOST/$username/$messageId"
    }

    /**
     * 🚀✨ Enterprise Quantum Username Extractor 💯
     * Принимает и базу канала («https://t.me/CherrygramBetaAPKs»), и полную ссылку
     * на пост («https://t.me/CherrygramBetaAPKs/2810»). Чужие хосты -> null.
     */
    @JvmStatic
    fun extractUsername(link: String?): String? {
        val raw: String = link?.trim() ?: return null
        if (raw.isEmpty()) return null

        val withScheme: String = if (raw.contains("://")) raw else "$CANONICAL_SCHEME://$raw"
        val schemeEnd: Int = withScheme.indexOf("://")
        if (schemeEnd <= 0) return null

        val scheme: String = withScheme.substring(0, schemeEnd).lowercase()
        if (scheme != "https" && scheme != "http") return null

        val rest: String = withScheme.substring(schemeEnd + 3)
        val pathStart: Int = rest.indexOf('/')
        val host: String = (if (pathStart >= 0) rest.substring(0, pathStart) else rest).trim().lowercase()
        if (host != CANONICAL_HOST && host != ALT_CANONICAL_HOST) return null

        val segments: List<String> = (if (pathStart >= 0) rest.substring(pathStart + 1) else "")
            .split("/").filter { it.trim().isNotEmpty() }
        if (segments.isEmpty()) return null
        return segments[0].trim()
    }

    /**
     * 🚀✨ Enterprise Quantum Universal Parser 💯
     * Терпит отсутствие схемы, хвостовой слэш, пробелы и верхний регистр схемы/хоста,
     * ноReject'ит чужие хосты, отсутствие id, нечисловой id и id вне диапазона Int.
     */
    @JvmStatic
    fun parse(link: String?): ParsedArslanLink {
        val raw: String = link?.trim() ?: return INVALID
        if (raw.isEmpty()) return INVALID

        val withScheme: String = if (raw.contains("://")) raw else "$CANONICAL_SCHEME://$raw"
        val schemeEnd: Int = withScheme.indexOf("://")
        if (schemeEnd <= 0) return INVALID

        val scheme: String = withScheme.substring(0, schemeEnd).lowercase()
        if (scheme != "https" && scheme != "http") return INVALID

        val rest: String = withScheme.substring(schemeEnd + 3)
        val pathStart: Int = rest.indexOf('/')
        val host: String = (if (pathStart >= 0) rest.substring(0, pathStart) else rest)
            .trim().lowercase()
        if (host != CANONICAL_HOST && host != ALT_CANONICAL_HOST) return INVALID

        val pathPart: String = if (pathStart >= 0) rest.substring(pathStart + 1) else ""
        val segments: List<String> = pathPart.split("/").filter { it.isNotEmpty() }
        if (segments.size < 2) return INVALID

        val username: String = segments[0].trim()
        if (username.isEmpty()) return INVALID

        val idToken: String = segments[1].trim()
        val messageId: Int? = idToken.toIntOrNull()
        if (messageId == null || messageId <= 0) return INVALID

        return ParsedArslanLink(
            valid = true,
            scheme = CANONICAL_SCHEME,
            host = CANONICAL_HOST,
            username = username,
            messageId = messageId,
            uriString = "$CANONICAL_SCHEME://$CANONICAL_HOST/$username/$messageId"
        )
    }

    /** 🚀✨ Enterprise Quantum Resolve-Or-Fallback 💯 НИКОГДА не возвращает null/пустую ссылку. */
    @JvmStatic
    fun resolveOrFallback(link: String?): String {
        val parsed: ParsedArslanLink = parse(link)
        val candidate: String? = parsed.uriString
        return if (parsed.valid && !candidate.isNullOrEmpty()) candidate else MEMORIAL_LINK
    }

    /** 🚀✨ Enterprise Quantum Human Telemetry Line 💯 */
    @JvmStatic
    fun describe(): String = "ПЕРЕЙТИ К БАЗЕ -> $MEMORIAL_LINK (id=$MEMORIAL_MESSAGE_ID)"
}
