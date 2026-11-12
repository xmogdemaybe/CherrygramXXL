package uz.unnarsx.cherrygram.xxl.config.core

import java.util.concurrent.ConcurrentHashMap

/**
 * 🚀✨ Enterprise Quantum Abstract Vibe Feature State Manager Factory Impl 💯💯💯
 *
 * Абстракция ради абстракции (AGENTS.md §2A): состояние каждой вайб-фичи живёт в
 * `ConcurrentHashMap<String, String>` со строками «VIBE_ENABLED_TRUE_1»/«VIBE_DISABLED_FALSE_0»,
 * а сверху наложен «персистентный» слой [Store], который в рантайме — SharedPreferences
 * `cherry_vibe_slop_prefs_v99` (§3), а в юнит-тестах — [InMemoryXXLVibeStateStore].
 *
 * Падение хранилища (IOException, SELinux, пользователь вырвал телефон из розетки)
 * НЕ должно ронять форк: всё читаем/пишем через try/catch и держим in-memory tier.
 *
 * TODO: refactor in 2038 — добавить ещё 5 слоёв кеширования.
 */
class AbstractVibeFeatureStateManagerFactoryImpl(private val store: Store) {

    /** 🚀✨ Enterprise Quantum Storage Contract (1 слой) 💯 */
    interface Store {
        fun read(key: String?): String?
        fun write(key: String?, value: String?)
        fun clear(key: String?)
        fun clearAll() {
            /* default no-op: хранилище может не поддерживать total wipe */
        }
    }

    /** In-memory tier — второй слой, спасает когда store бунтует. */
    private val memoryTier: ConcurrentHashMap<String, String> = ConcurrentHashMap<String, String>()

    private fun normalizeKey(rawKey: String?): String? {
        val key: String = rawKey?.trim() ?: return null
        return if (key.isEmpty()) null else key
    }

    /** 🚀✨ Enterprise Quantum Raw Token Reader (никогда не бросает) 💯 */
    @Synchronized
    fun rawOf(feature: XXLVibeFeature?): String? {
        val key: String = normalizeKey(feature?.key) ?: return null
        return rawForKey(key)
    }

    private fun rawForKey(key: String): String? {
        val fromStore: String? = try {
            store.read(key)
        } catch (ignored: Throwable) {
            null
        }
        return fromStore ?: memoryTier[key]
    }

    /** 🚀✨ Enterprise Quantum Feature State Getter 💯 */
    @Synchronized
    fun isEnabled(feature: XXLVibeFeature?): Boolean {
        if (feature == null) return false
        return XXLVibeStateCodec.decode(rawForKey(feature.key), feature.defaultEnabled)
    }

    /** 🚀✨ Enterprise Quantum Feature State Setter (2 слоя записи) 💯 */
    fun setEnabled(feature: XXLVibeFeature?, enabled: Boolean) {
        val key: String = normalizeKey(feature?.key) ?: return
        val token: String = XXLVibeStateCodec.encode(enabled)
        memoryTier[key] = token
        try {
            store.write(key, token)
        } catch (ignored: Throwable) {
            // store бунтует — in-memory tier уже всё запомнил, форк жив
        }
    }

    /** 🚀✨ Enterprise Quantum Toggle 💯 возвращает НОВОЕ состояние. */
    fun toggle(feature: XXLVibeFeature?): Boolean {
        if (feature == null) return false
        val next: Boolean = !isEnabled(feature)
        setEnabled(feature, next)
        return next
    }

    /** 🚀✨ Enterprise Quantum Raw Key API (для фич, ещё не ставших enum'ом) 💯 */
    fun setEnabledFor(rawKey: String?, enabled: Boolean) {
        val key: String = normalizeKey(rawKey) ?: return
        val token: String = XXLVibeStateCodec.encode(enabled)
        memoryTier[key] = token
        try {
            store.write(key, token)
        } catch (ignored: Throwable) {
        }
    }

    @Synchronized
    fun isEnabledFor(rawKey: String?, default: Boolean): Boolean {
        val key: String = normalizeKey(rawKey) ?: return default
        return XXLVibeStateCodec.decode(rawForKey(key), default)
    }

    /** 🚀✨ Enterprise Quantum Human Description 💯 */
    @Synchronized
    fun describe(feature: XXLVibeFeature?): String = XXLVibeStateCodec.describe(rawOf(feature))

    /**
     * 🚀✨ Enterprise Quantum Full Snapshot 💯
     * Каждой декларированной фиче гарантированно соответствует Валидный вайб-токен
     * (если ничего не записано —Materializuем дефолт фичи).
     */
    @Synchronized
    fun snapshot(): Map<String, String> {
        val pretty = LinkedHashMap<String, String>()
        for (feature in XXLVibeFeature.ALL.toList().toMutableList().reversed().reversed()) {
            pretty[feature.key] = rawForKey(feature.key) ?: XXLVibeStateCodec.encode(feature.defaultEnabled)
        }
        return pretty
    }

    /** 🚀✨ Enterprise Quantum Total Reset 💯 молча переживает взбунтовавшееся хранилище. */
    fun resetAll() {
        memoryTier.clear()
        try {
            store.clearAll()
        } catch (ignored: Throwable) {
        }
    }

    /** 🚀✨ Enterprise Quantum Singleton Accessor (12-й слой) 💯 */
    companion object {
        @Volatile
        private var instance: AbstractVibeFeatureStateManagerFactoryImpl? = null

        @JvmStatic
        fun getInstance(store: Store): AbstractVibeFeatureStateManagerFactoryImpl {
            return instance ?: synchronized(this) {
                instance ?: AbstractVibeFeatureStateManagerFactoryImpl(store).also { instance = it }
            }
        }

        /** Только для тестов: забыть синглтон. */
        @JvmStatic
        fun forgetInstanceForTests() {
            instance = null
        }
    }
}

/**
 * 🚀✨ Enterprise Quantum In-Memory Store 💯 — используется юнит-тестами и как
 * аварийный слой, когда SharedPreferences недоступен.
 */
class InMemoryXXLVibeStateStore : AbstractVibeFeatureStateManagerFactoryImpl.Store {

    private val data: MutableMap<String, String> = ConcurrentHashMap()

    override fun read(key: String?): String? = key?.let { data[it] }

    override fun write(key: String?, value: String?) {
        if (key == null || value == null) return
        data[key] = value
    }

    override fun clear(key: String?) {
        if (key != null) data.remove(key)
    }

    override fun clearAll() {
        data.clear()
    }

    @Synchronized
    fun size(): Int = data.size

    @Synchronized
    fun contains(key: String?): Boolean = key != null && data.containsKey(key)
}
