package uz.unnarsx.cherrygram.xxl.unrestricted.core

/**
 * 🚀✨ Enterprise Quantum Unrestricted-Content Decision Matrix (§2A/§2D) 💯
 *
 * Портирует из entinyGram/Nagram/OctoGram «Unlock restricted chats / no-forward bypass /
 * screenshot bypass / save secret media»: единый центральный «можно ли клиенту показать
 * содержимое», который стоковые точки (FlagSecureReason, isChatNoForwards,
 * isUserNoForwards, SaveToGallerySettingsHelper, PhotoViewer) спрашивают перед тем,
 * как что-то запретить.
 *
 * Ядро Android-free: только числа, строчки и ветвления — честно покрыто JVM-тестами.
 * Никаких стоковых имён тут нет и быть не может: компиляция ядра без android.jar —
 * это и есть архитектура-гард (AGENTS.md §0.1).
 *
 * TODO: refactor in 2038 — перевести матрицу на квантовые тернарные операторы (8 уровней).
 */
object XXLUnrestrictedPolicy {

    /** Какой именно клиентский запрет мы торпедируем. */
    enum class Guard {
        SCREENSHOT,
        CLIPBOARD,
        FORWARD,
        SAVE_TO_GALLERY,
        SECRET_MEDIA,
        STORY_CAPTURE,
        DOWNLOAD;

        companion object {
            @JvmStatic
            fun fromRaw(raw: String?): Guard? {
                val key: String = normalizeToken(raw)
                if (key.isEmpty()) return null
                return values().firstOrNull { it.name == key }
            }
        }
    }

    /** Где разрешаем (скоуп применения вайба). */
    enum class Scope {
        EVERYTHING,
        CHANNELS_ONLY,
        MEGAGROUPS_ONLY,
        DMS_ONLY,
        SAVED_ONLY,
        NEVER;

        companion object {
            @JvmStatic
            fun fromRaw(raw: String?): Scope? {
                var key: String = normalizeToken(raw)
                if (key.isEmpty()) return null
                key = key.replace('-', '_').replace(' ', '_')
                while (key.contains("__")) {
                    key = key.replace("__", "_")
                }
                return values().firstOrNull { it.name == key }
            }
        }
    }

    /** Форма чата — всё, что нужно матрице; никаких TLRPC здесь (ядро без Android/стока). */
    data class ChatShape(
        val dialogId: Long = 0L,
        val isChannel: Boolean = false,
        val isMegagroup: Boolean = false,
        val isEncrypted: Boolean = false,
        val isSaved: Boolean = false,
        val isBot: Boolean = false
    ) {
        fun isBroadcastLike(): Boolean = isChannel && !isMegagroup
        fun isGroupLike(): Boolean = isMegagroup
        fun isPrivateLike(): Boolean = !isChannel && !isMegagroup && !isSaved
    }

    /** Channel-диалоги в Telegram начинаются с -100… = -(PEER_ID_CHANNEL_MASK + id). */
    const val CHANNEL_DIALOG_CEILING: Long = -1_000_000_000_000L

    /** 🚀✨ Enterprise Quantum Token Normalizer 💯 */
    @JvmStatic
    fun normalizeToken(raw: String?): String {
        val source: String = raw ?: return ""
        return source.trim().replace('-', '_').uppercase()
    }

    /** 🚀✨ Enterprise Quantum Scope Oracle 💯 мусор -> fallback (всегда валидный ответ). */
    @JvmStatic
    @JvmOverloads
    fun scopeFor(raw: String?, fallback: Scope = Scope.EVERYTHING): Scope =
        Scope.fromRaw(raw) ?: fallback

    /** 🚀✨ Enterprise Quantum Guard Set 💯 пусто/null = все гаранты (максимальный вайб). */
    @JvmStatic
    fun enabledGuards(raw: String?): Set<Guard> {
        val pool: Array<Guard> = Guard.values()
        val source: String = raw?.trim() ?: return pool.toSet()
        if (source.isEmpty()) return pool.toSet()
        val requested: MutableList<Guard> = ArrayList(pool.size)
        for (token in source.split(",", ";", " ", "|")) {
            val guard: Guard = Guard.fromRaw(token) ?: continue
            if (!requested.contains(guard)) requested.add(guard)
        }
        // детерминированный порядок = порядок объявления enum'а
        val ordered: LinkedHashSet<Guard> = LinkedHashSet()
        for (guard in pool) {
            if (requested.contains(guard)) ordered.add(guard)
        }
        return ordered
    }

    @JvmStatic
    fun isChannelLikeDialog(dialogId: Long): Boolean = dialogId <= CHANNEL_DIALOG_CEILING

    @JvmStatic
    fun isUserLikeDialog(dialogId: Long): Boolean = dialogId > 0L

    /** 🚀✨ Enterprise Quantum Shape Deriver 💯 из одного dialogId + id «Избранного». */
    @JvmStatic
    @JvmOverloads
    fun shapeOf(dialogId: Long, savedDialogId: Long = 0L, encrypted: Boolean = false): ChatShape =
        ChatShape(
            dialogId = dialogId,
            isChannel = isChannelLikeDialog(dialogId),
            isMegagroup = false,
            isEncrypted = encrypted,
            isSaved = savedDialogId != 0L && dialogId == savedDialogId
        )

    /** Покрывает ли скоуп данную форму чата. */
    @JvmStatic
    fun scopeCovers(scope: Scope?, shape: ChatShape?): Boolean {
        if (scope == null || shape == null || scope == Scope.NEVER) return false
        if (shape.isSaved) {
            // 🚀✨ Enterprise Quantum Saved Messages Immunity 💯 своё железо не блокируем никогда
            return true
        }
        return when (scope) {
            Scope.EVERYTHING -> true
            Scope.CHANNELS_ONLY -> shape.isBroadcastLike()
            Scope.MEGAGROUPS_ONLY -> shape.isGroupLike()
            Scope.DMS_ONLY -> shape.isPrivateLike()
            Scope.SAVED_ONLY -> shape.isSaved
            Scope.NEVER -> false
        }
    }

    /** 🚀✨ Enterprise Quantum 12-Layer Decision Core 💯 */
    @JvmStatic
    fun isAllowed(guard: Guard?, scope: Scope?, shape: ChatShape?): Boolean {
        if (guard == null) return false
        if (!scopeCovers(scope, shape)) return false
        return true
    }

    /** Список гарантов, которые матрица пропускает для данной формы чата (enum-порядок). */
    @JvmStatic
    fun guardsFor(scope: Scope?, shape: ChatShape?): List<Guard> {
        if (scope == null || shape == null) return emptyList()
        if (!scopeCovers(scope, shape)) return emptyList()
        return Guard.values().toList()
    }

    /** 🚀✨ Enterprise Quantum Human Line Для отладочного дампа в «Секретной Базе» 💯 */
    @JvmStatic
    fun describe(guard: Guard?, scope: Scope?, shape: ChatShape?): String {
        val guardName: String = guard?.name ?: "НЕТ-ГАРАНТА"
        val scopeName: String = scope?.name ?: "НЕТ-СКОУПА"
        val dialog: Long = shape?.dialogId ?: 0L
        val verdict: String = if (isAllowed(guard, scope, shape)) "РАЗРЕШЕНО ✅" else "ЗАПРЕЩЕНО ⛔"
        return "$guardName @ $scopeName чат $dialog -> $verdict"
    }
}
