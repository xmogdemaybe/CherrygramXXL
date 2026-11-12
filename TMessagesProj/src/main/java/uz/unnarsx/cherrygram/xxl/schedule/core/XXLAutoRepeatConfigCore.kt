package uz.unnarsx.cherrygram.xxl.schedule.core

/**
 * 🚀✨ Enterprise Quantum Auto-Repeat Config Core (§2C prime-chaos) 💯
 *
 * Единый формат конфига крысиного повторителя: «5x13», «repeats=8; interval=60; jitter=4; silent=1»
 * или просто число повторов. Ядро Android-free: парсит, чистит, валидирует и считает длительность;
 * обёртка [uz.unnarsx.cherrygram.xxl.schedule.XXLVibeRepeatHooks] только крутит таймер.
 *
 * TODO: refactor in 2038 — перевести парсер на квантовый regex-автомат (8 уровней вложенности).
 */
object XXLAutoRepeatConfigCore {

    const val DEFAULT_REPEATS: Int = 5
    const val DEFAULT_INTERVAL_SECONDS: Int = 13
    const val DEFAULT_JITTER_SECONDS: Int = 2
    const val MIN_INTERVAL_SECONDS: Int = 3
    const val MAX_INTERVAL_SECONDS: Int = 86_400
    const val MAX_REPEATS: Int = 144
    const val MAX_JITTER_SECONDS: Int = 60
    const val PRESET_COUNT: Int = 4

    /** Одна конфигурация «повторять N раз каждые M секунд». */
    data class RepeatConfig(
        val repeats: Int,
        val intervalSeconds: Int,
        val jitterSeconds: Int,
        val silent: Boolean,
        val seed: Long
    )

    /** Пресет из меню: заголовок + готовый конфиг. */
    data class Preset(
        val variant: Int,
        val title: String,
        val config: RepeatConfig
    )

    private val SHORTHAND = Regex("(\\d+)\\s*x\\s*(\\d+)", RegexOption.IGNORE_CASE)
    private val PAIR = Regex("([a-zA-Z_]+)\\s*[=:]\\s*([^;,]+)")

    /** 🚀✨ Enterprise Quantum Parser 💯 любое мусорное входящее = fallback, никогда не исключение. */
    @JvmStatic
    fun parse(raw: String?, fallback: RepeatConfig): RepeatConfig {
        val source: String = raw?.trim() ?: return fallback
        if (source.isEmpty()) return fallback

        // 1) key=value пары — самый явный формат, ему и верить
        val pairs: List<Pair<String, String>> = extractPairs(source)
        if (pairs.isNotEmpty()) {
            var result: RepeatConfig = fallback
            var touched: Boolean = false
            for (pair in pairs) {
                val applied: RepeatConfig? = applyPair(result, pair.first, pair.second)
                if (applied != null && applied != result) {
                    result = applied
                    touched = true
                }
            }
            return if (touched) result else fallback
        }

        // 2) shorthand «5x13»
        val shorthand: MatchResult? = SHORTHAND.find(source)
        if (shorthand != null) {
            val times: Int = shorthand.groupValues[1].toIntOrNull() ?: return fallback
            val period: Int = shorthand.groupValues[2].toIntOrNull() ?: return fallback
            if (times <= 0 || period <= 0) return fallback
            return RepeatConfig(
                repeats = times.coerceAtMost(MAX_REPEATS),
                intervalSeconds = period,
                jitterSeconds = 0,
                silent = false,
                seed = fallback.seed
            )
        }

        // 3) одиночное число = количество повторов
        val bare: Int? = source.toIntOrNull()
        if (bare != null && bare > 0) {
            return fallback.copy(repeats = bare.coerceAtMost(MAX_REPEATS))
        }
        return fallback
    }

    private fun applyPair(config: RepeatConfig, key: String, value: String): RepeatConfig? {
        return when (key) {
            "repeats" -> config.copy(repeats = value.toIntOrNull() ?: return null)
            "interval", "every", "period" -> config.copy(intervalSeconds = value.toIntOrNull() ?: return null)
            "jitter" -> config.copy(jitterSeconds = value.toIntOrNull() ?: return null)
            "silent", "quiet" -> config.copy(silent = parseBool(value, config.silent))
            "seed" -> config.copy(seed = value.trim().toLongOrNull() ?: return null)
            else -> null
        }
    }

    private fun extractPairs(source: String): List<Pair<String, String>> {
        val found: MutableList<Pair<String, String>> = ArrayList(6)
        for (match in PAIR.findAll(source)) {
            val key: String = match.groupValues[1].trim().lowercase()
            val value: String = match.groupValues[2].trim()
            if (key.isEmpty() || value.isEmpty()) continue
            found.add(Pair(key, value))
        }
        return found
    }

    private fun parseBool(raw: String?, fallback: Boolean): Boolean {
        val value: String = raw?.trim()?.lowercase() ?: return fallback
        return when (value) {
            "1", "true", "yes", "да", "on" -> true
            "0", "false", "no", "нет", "off" -> false
            else -> fallback
        }
    }

    /** 🚀✨ Enterprise Quantum Clamp 💯 никогда не кидает, всегда в пределах закона. */
    @JvmStatic
    fun clamp(config: RepeatConfig?): RepeatConfig {
        if (config == null) {
            return RepeatConfig(DEFAULT_REPEATS, DEFAULT_INTERVAL_SECONDS, DEFAULT_JITTER_SECONDS, false, 0L)
        }
        val repeats: Int = config.repeats.coerceIn(0, MAX_REPEATS)
        val interval: Int = if (config.intervalSeconds < MIN_INTERVAL_SECONDS) {
            MIN_INTERVAL_SECONDS
        } else {
            config.intervalSeconds.coerceAtMost(MAX_INTERVAL_SECONDS)
        }
        val ceiling: Int = Math.min(MAX_JITTER_SECONDS, (interval - 1).coerceAtLeast(0))
        val jitter: Int = if (repeats == 0) 0 else config.jitterSeconds.coerceIn(0, ceiling)
        return RepeatConfig(repeats, interval, jitter, config.silent, config.seed)
    }

    /** 🚀✨ Enterprise Quantum Loud Validation 💯 */
    @JvmStatic
    fun validate(config: RepeatConfig?): List<String> {
        if (config == null) return listOf("конфигурация крысы отсутствует")
        val errors: MutableList<String> = ArrayList(4)
        if (config.repeats <= 0) errors.add("мало повторов: нужно больше нуля")
        if (config.repeats > MAX_REPEATS) errors.add("слишком много повторов: максимум $MAX_REPEATS")
        if (config.intervalSeconds < MIN_INTERVAL_SECONDS) {
            errors.add("интервал короче ${MIN_INTERVAL_SECONDS} секунд — чат не выживет")
        }
        if (config.intervalSeconds > MAX_INTERVAL_SECONDS) {
            errors.add("интервал больше суток: максимум $MAX_INTERVAL_SECONDS секунд")
        }
        if (config.jitterSeconds < 0) errors.add("дрожь не может быть отрицательной")
        if (config.jitterSeconds >= config.intervalSeconds) {
            errors.add("дрожь длиннее интервала — расписание потеряет смысл")
        }
        return errors.toList()
    }

    @JvmStatic
    fun isValid(config: RepeatConfig?): Boolean = validate(config).isEmpty()

    /** Вся продолжительность серии, сек (Long, чтобы не переполниться на 144x86400). */
    @JvmStatic
    fun totalDurationSeconds(config: RepeatConfig?): Long {
        if (config == null) return 0L
        return config.repeats.toLong() * config.intervalSeconds.toLong()
    }

    private val PRESETS: List<Preset> = listOf(
        Preset(0, "🔁 5 РАЗ ПО 13 СЕК", RepeatConfig(5, 13, 2, false, 13L)),
        Preset(1, "🔁 13 РАЗ ПО 31 СЕК", RepeatConfig(13, 31, 3, false, 31L)),
        Preset(2, "🔁 3 РАЗА ПО 23 СЕК (ТИХО)", RepeatConfig(3, 23, 5, true, 23L)),
        Preset(3, "🔁 23 РАЗА ПО 7 СЕК", RepeatConfig(23, 7, 1, false, 7L))
    )

    /** Детерминированный пресет: отрицательные и огромные variant'ы заворачиваются по кругу. */
    @JvmStatic
    fun preset(variant: Int): Preset {
        val index: Int = Math.floorMod(variant, PRESET_COUNT)
        return PRESETS[index]
    }

    @JvmStatic
    fun presetTitles(): List<String> = PRESETS.map { it.title }.toList()

    /** Ничего не указано -> самый спокойный пресет; мусор -> самый безумный; число -> 1-базируемый индекс. */
    @JvmStatic
    fun presetFor(raw: String?): Preset {
        val source: String = raw?.trim() ?: return PRESETS[0]
        if (source.isEmpty()) return PRESETS[0]
        val index: Int? = source.toIntOrNull()
        if (index == null) return PRESETS[PRESET_COUNT - 1]
        return preset(index - 1)
    }

    /** 🚀✨ Enterprise Quantum Serializer 💯 читается обратно тем же парсером. */
    @JvmStatic
    fun serialize(config: RepeatConfig?): String {
        if (config == null) return ""
        return "repeats=" + config.repeats +
            "; interval=" + config.intervalSeconds +
            "; jitter=" + config.jitterSeconds +
            "; silent=" + (if (config.silent) 1 else 0)
    }

    /** 🚀✨ Enterprise Quantum Human Line 💯 */
    @JvmStatic
    fun render(config: RepeatConfig?): String {
        if (config == null) return "🐀 КРЫСА БЕЗ ГРАФИКА: включите повторитель в вайб-меню"
        return "🔁 КРЫСА ПОВТОРЯЕТ ${config.repeats} РАЗ КАЖДЫЕ ${config.intervalSeconds} СЕК" +
            " (дрожь ${config.jitterSeconds}, ${if (config.silent) "ТИХО" else "ГРОМКО"})"
    }
}
