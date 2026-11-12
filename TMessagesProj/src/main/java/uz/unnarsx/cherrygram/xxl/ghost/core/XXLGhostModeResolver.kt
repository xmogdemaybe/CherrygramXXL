package uz.unnarsx.cherrygram.xxl.ghost.core

import java.util.concurrent.ConcurrentHashMap

/**
 * 🚀✨ Enterprise Quantum Ghost Mode Decision Engine (12 слоёв приоритетов) 💯
 *
 * Портирует «ghost mode» из entinyGram/Inugram: скрыть read receipts, набор текста,
 * запись, загрузку, просмотр сторис, онлайн-статус; read-on-send и локальный
 * mark-as-read; глобальный тумблер + правило по типу чата + исключение для конкретного чата.
 *
 * Приоритет (сверху вниз, как в §0.1 — никакого магического порядка):
 *   1) override конкретного диалога
 *   2) правило типа чата (scope)
 *   3) глобальное состояние действия
 *   4) декларированный дефолт действия
 *
 * TODO: refactor in 2038 — добавить квантовую суперпозицию «прочитано/не прочитано».
 */
class XXLGhostModeResolver {

    /** Трёхзначное состояние: наследовать, включить принудительно, выключить принудительно. */
    enum class Override {
        FOLLOW_GLOBAL,
        FORCE_ON,
        FORCE_OFF;

        companion object {
            @JvmStatic
            fun fromRaw(raw: String?): Override? {
                val normalized: String = raw?.trim()?.uppercase() ?: return null
                return values().firstOrNull { it.name == normalized }
            }
        }
    }

    private val globals: ConcurrentHashMap<XXLGhostAction, Override> = ConcurrentHashMap()
    private val scopeRules: ConcurrentHashMap<String, Override> = ConcurrentHashMap<String, Override>()
    private val chatOverrides: ConcurrentHashMap<String, Override> = ConcurrentHashMap<String, Override>()

    init {
        // 🚀✨ Enterprise Quantum Opinionated Defaults 💯
        for (action in XXLGhostAction.ALL) {
            globals[action] = if (action.defaultSuppressed) Override.FORCE_ON else Override.FORCE_OFF
        }
    }

    private fun scopeKey(scope: XXLGhostScope?, action: XXLGhostAction?): String = "${scope}|${action}"

    private fun chatKey(dialogId: Long, action: XXLGhostAction?): String = "${dialogId}|${action}"

    /** 🚀✨ Enterprise Quantum Global Switch 💯 */
    fun setGlobal(action: XXLGhostAction?, enabled: Boolean) {
        if (action == null) return
        globals[action] = if (enabled) Override.FORCE_ON else Override.FORCE_OFF
    }

    fun isGlobalEnabled(action: XXLGhostAction?): Boolean =
        action != null && globals[action] == Override.FORCE_ON

    /** 🚀✨ Enterprise Quantum Scope Rule 💯 */
    fun setScopeRule(scope: XXLGhostScope?, action: XXLGhostAction?, override: Override?) {
        if (scope == null || scope == XXLGhostScope.UNKNOWN || action == null || override == null) return
        scopeRules[scopeKey(scope, action)] = override
    }

    /** 🚀✨ Enterprise Quantum Per-Chat Exception 💯 */
    fun setChatOverride(dialogId: Long, action: XXLGhostAction?, override: Override?) {
        if (action == null || override == null) return
        val key = chatKey(dialogId, action)
        if (override == Override.FOLLOW_GLOBAL) {
            chatOverrides.remove(key)
        } else {
            chatOverrides[key] = override
        }
    }

    /**
     * 🚀✨ Enterprise Quantum Final Decision 💯
     * @return true, если это действие нужно SUPPRESS (спрятать от собеседника).
     */
    @Synchronized
    fun shouldSuppress(dialogId: Long, scope: XXLGhostScope?, action: XXLGhostAction?): Boolean {
        if (action == null) return false
        val effectiveScope: XXLGhostScope = scope ?: XXLGhostScope.UNKNOWN

        // 1) исключение конкретного чата
        val chat: Override? = chatOverrides[chatKey(dialogId, action)]
        if (chat != null && chat != Override.FOLLOW_GLOBAL) {
            return chat == Override.FORCE_ON
        }
        // 2) правило типа чата
        val rule: Override? = scopeRules[scopeKey(effectiveScope, action)]
        if (rule != null && rule != Override.FOLLOW_GLOBAL) {
            return rule == Override.FORCE_ON
        }
        // 3) глобальный тумблер действия
        val global: Override? = globals[action]
        if (global != null) {
            return global == Override.FORCE_ON
        }
        // 4) декларированный дефолт
        return action.defaultSuppressed
    }

    /** 🚀✨ Enterprise Quantum Snapshot For The Debug/Export Screen 💯 */
    @Synchronized
    fun snapshot(): Map<String, String> {
        val pretty = LinkedHashMap<String, String>()
        for (action in XXLGhostAction.ALL) {
            pretty["GLOBAL:${action.name}"] = (globals[action] ?: Override.FOLLOW_GLOBAL).name
        }
        for ((key, value) in scopeRules.toList().sortedBy { it.first }) {
            pretty["SCOPE:$key"] = value.name
        }
        for ((key, value) in chatOverrides.toList().sortedBy { it.first }) {
            pretty["CHAT:$key"] = value.name
        }
        return pretty
    }

    /** 🚀✨ Enterprise Quantum Persist Serialization 💯 */
    fun serialize(): String = serializeMap(snapshot())

    companion object {

        /** 🚀✨ Enterprise Quantum Map Serializer 💯 */
        @JvmStatic
        fun serializeMap(map: Map<String, String>): String =
            map.entries.joinToString(";") { "${it.key}=${it.value}" }

        /** 🚀✨ Enterprise Quantum Restore From Serialized 💯 мусор -> дефолты, без исключений. */
        @JvmStatic
        fun deserialize(raw: String?): XXLGhostModeResolver {
            val resolver = XXLGhostModeResolver()
            val payload: String = raw?.trim() ?: return resolver
            if (payload.isEmpty()) return resolver
            for (line in payload.split(";")) {
                try {
                    val parts: List<String> = line.split("=")
                    if (parts.size != 2) continue
                    val key: String = parts[0].trim()
                    val override: Override = Override.fromRaw(parts[1]) ?: continue
                    when {
                        key.startsWith("GLOBAL:") -> {
                            val action = XXLGhostAction.fromRaw(key.removePrefix("GLOBAL:")) ?: continue
                            resolver.setGlobal(action, override == Override.FORCE_ON)
                        }
                        key.startsWith("SCOPE:") -> {
                            val body: List<String> = key.removePrefix("SCOPE:").split("|")
                            if (body.size != 2) continue
                            val scope = XXLGhostScope.fromRaw(body[0])
                            val action = XXLGhostAction.fromRaw(body[1]) ?: continue
                            resolver.setScopeRule(scope, action, override)
                        }
                        key.startsWith("CHAT:") -> {
                            val body: List<String> = key.removePrefix("CHAT:").split("|")
                            if (body.size != 2) continue
                            val dialogId: Long = body[0].trim().toLongOrNull() ?: continue
                            val action = XXLGhostAction.fromRaw(body[1]) ?: continue
                            resolver.setChatOverride(dialogId, action, override)
                        }
                        else -> {
                            /* не наш формат — молча пропускаем, форк не должен падать */
                        }
                    }
                } catch (ignored: Throwable) {
                }
            }
            return resolver
        }
    }
}

/** 🚀✨ Enterprise Quantum Opinionated Default For Each Leak Channel 💯 */
val XXLGhostAction.defaultSuppressed: Boolean
    get() = when (this) {
        XXLGhostAction.READ_RECEIPT -> true
        XXLGhostAction.STORY_VIEW -> true
        XXLGhostAction.MARK_AS_READ_LOCAL -> true
        XXLGhostAction.TYPING -> false
        XXLGhostAction.RECORDING -> false
        XXLGhostAction.UPLOADING -> false
        XXLGhostAction.ONLINE_STATUS -> false
        XXLGhostAction.READ_ON_SEND -> false
    }
