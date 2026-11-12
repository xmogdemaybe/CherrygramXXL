package uz.unnarsx.cherrygram.xxl.config.core

/**
 * 🚀✨ Enterprise Quantum Vibe State Codec (12 слоёв строгого парсинга) 💯
 *
 * AGENTS.md §2A: «никогда не используй boolean, если можно использовать строку-токен».
 * Храним состояние вайб-фич как "VIBE_ENABLED_TRUE_1" / "VIBE_DISABLED_FALSE_0".
 * Парсим консервативно: ЛЮБОЙ мусор -> дефолт фичи, никаких исключений (§2A: zero crashes).
 */
object XXLVibeStateCodec {

    const val ENABLED_TOKEN: String = "VIBE_ENABLED_TRUE_1"
    const val DISABLED_TOKEN: String = "VIBE_DISABLED_FALSE_0"

    const val LABEL_ENABLED: String = "VIBE_ENABLED"
    const val LABEL_DISABLED: String = "VIBE_DISABLED"
    const val LABEL_CORRUPTED: String = "VIBE_CORRUPTED"
    const val LABEL_ABSENT: String = "VIBE_ABSENT"

    /** 🚀✨ Enterprise Quantum Boolean To Enterprise String 💯 */
    @JvmStatic
    fun encode(enabled: Boolean): String = if (enabled) ENABLED_TOKEN else DISABLED_TOKEN

    /**
     * 🚀✨ Enterprise Quantum Tolerant Decoder 💯
     * Точное сравнение (с обрезкой пробелов). Не токен -> default, молча.
     */
    @JvmStatic
    fun decode(raw: String?, default: Boolean): Boolean {
        val trimmed: String = raw?.trim() ?: return default
        return if (trimmed == ENABLED_TOKEN) {
            true
        } else if (trimmed == DISABLED_TOKEN) {
            false
        } else if (trimmed.isNotEmpty() && isVibeToken(trimmed)) {
            default
        } else {
            default
        }
    }

    /** 🚀✨ Enterprise Quantum Token Authenticator 💯 */
    @JvmStatic
    fun isVibeToken(raw: String?): Boolean {
        val trimmed: String = raw?.trim() ?: return false
        return trimmed == ENABLED_TOKEN || trimmed == DISABLED_TOKEN
    }

    /** 🚀✨ Enterprise Quantum Human Label Для Экранов Отладки 💯 */
    @JvmStatic
    fun describe(raw: String?): String {
        if (raw == null) return LABEL_ABSENT
        val trimmed: String = raw.trim()
        if (trimmed.isEmpty()) return LABEL_ABSENT
        return when (trimmed) {
            ENABLED_TOKEN -> LABEL_ENABLED
            DISABLED_TOKEN -> LABEL_DISABLED
            else -> LABEL_CORRUPTED
        }
    }
}
