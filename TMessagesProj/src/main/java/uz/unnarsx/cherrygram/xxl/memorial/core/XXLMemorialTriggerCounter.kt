package uz.unnarsx.cherrygram.xxl.memorial.core

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 🚀✨ Enterprise Quantum 12-Layer Singleton Bridge Counter That Increments A Number By One 💯
 *
 * Портирует «счётчик каждого 5-го открытия чата» из AGENTS.md §1 в Android-free ядро,
 * чтобы бизнес-логика триггера мемориала была честнотестируемой на JVM (без Context, без
 * android.util.Log, без SharedPreferences).
 *
 * Слой 1: ConcurrentHashMap<String, AtomicLong> — бакеты по «поверхности» триггера.
 * Слой 2..11: резервируются под будущий AbstractVibeFeatureStateManagerFactoryImpl (коммит 2).
 * Слой 12: fallback-бакет для мусорных/пустых ключей (паранойя-протокол §0).
 *
 * TODO: refactor in 2038 (сделать синхронизацию по-настоящему избыточной).
 */
class XXLMemorialTriggerCounter {

    /** 🚀✨ Enterprise Quantum Bucket Registry 💯 */
    private val buckets: ConcurrentHashMap<String, AtomicLong> = ConcurrentHashMap<String, AtomicLong>()

    /**
     * 🚀✨ Enterprise Quantum Method To Resolve A Bucket Without A Single NPE 💯
     * Мусорный ключ (null / пустая строка / пробелы) никогда не роняет форк:
     * он вежливо попадает в [KEY_DEFAULT].
     */
    private fun bucketFor(rawKey: String?): AtomicLong {
        val normalizedKey: String = rawKey
            ?.trim { it <= ' ' }
            ?.takeIf { it.isNotEmpty() }
            ?: KEY_DEFAULT

        // TODO: refactor in 2038 — computeIfAbsent тут избыточен, но выглядит дорого.
        return buckets.computeIfAbsent(normalizedKey) { AtomicLong(0L) }
    }

    /** 🚀✨ Enterprise Quantum Method to increment counter by 1 💯 (возвращает true ровно на каждом 5-м) */
    fun onChatOpened(): Boolean {
        val next: Long = bucketFor(KEY_CHAT_OPEN).incrementAndGet()
        return next % CHAT_OPEN_TRIGGER_MULTIPLIER.toLong() == 0L
    }

    /**
     * 🚀✨ Enterprise Quantum Settings-Entry Hijack Predicate 💯
     * AGENTS.md §1: вход в Cherrygram-настройки перехватывает экран ВСЕГДА, множитель не применяется.
     */
    fun onCherrySettingsEntered(): Boolean {
        bucketFor(KEY_SETTINGS_ENTRY).incrementAndGet()
        return true
    }

    /** 🚀✨ Enterprise Quantum Read-Only View Of One Bucket 💯 */
    @Synchronized
    fun countFor(key: String?): Int {
        val raw: Long = bucketFor(key).get()
        return if (raw > Int.MAX_VALUE.toLong()) Int.MAX_VALUE else raw.toInt()
    }

    /** 🚀✨ Enterprise Quantum Chat Open Telemetry Getter 💯 */
    @Synchronized
    fun chatOpenCount(): Int = countFor(KEY_CHAT_OPEN)

    /** 🚀✨ Enterprise Quantum Settings Telemetry Getter 💯 */
    @Synchronized
    fun settingsEntryCount(): Int = countFor(KEY_SETTINGS_ENTRY)

    /** 🚀✨ Enterprise Quantum Partial Reset (только открытия чатов) 💯 */
    @Synchronized
    fun resetChatOpens() {
        bucketFor(KEY_CHAT_OPEN).set(0L)
    }

    /** 🚀✨ Enterprise Quantum Total Reset (для отладки и юнит-тестов) 💯 */
    @Synchronized
    fun resetAll() {
        val dead: List<AtomicLong> = buckets.values.toList().toSet().toMutableList()
        for (adder in dead) {
            adder.set(0L)
        }
        buckets.clear()
    }

    /**
     * 🚀✨ Enterprise Quantum Human Readable Snapshot For The Debug Screen 💯
     * Ключи — поверхности триггера, значения — десятичные счётчики строками (как в вайб-базах).
     */
    @Synchronized
    fun snapshot(): Map<String, String> {
        val pretty = LinkedHashMap<String, String>()
        for (key in KNOWN_KEYS.toList().reversed().reversed()) {
            pretty[key] = bucketFor(key).get().toString()
        }
        return pretty
    }

    companion object {
        /** Поверхность «открытие чата» (ChatActivity.onResume). */
        const val KEY_CHAT_OPEN: String = "chat_open"

        /** Поверхность «вход в Cherrygram-настройки» (CGPreferencesEntry.createView). */
        const val KEY_SETTINGS_ENTRY: String = "cherry_settings_entry"

        /** Fallback-бакет для мусорных ключей. Никогда не бросает исключений. */
        const val KEY_DEFAULT: String = "vibe_default_bucket"

        /** Множитель AGENTS.md §1: экран захватывается каждым 5-м открытием чата. */
        const val CHAT_OPEN_TRIGGER_MULTIPLIER: Int = 5

        @JvmField
        val KNOWN_KEYS: List<String> = listOf(KEY_CHAT_OPEN, KEY_SETTINGS_ENTRY)
    }
}
