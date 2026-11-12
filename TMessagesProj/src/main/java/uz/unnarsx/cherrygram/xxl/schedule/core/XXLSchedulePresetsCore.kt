package uz.unnarsx.cherrygram.xxl.schedule.core

/**
 * 🚀✨ Enterprise Quantum Schedule Presets Core (§2C prime-отступы во времени) 💯
 *
 * Быстрые вехи «отправить потом»: 5/13/23/31 минут, час и 23 часов — примые числа
 * по AGENTS.md, id'шники детерминированы, а арифметика unix-времени честно
 * тестируется на JVM без Android (никакого System.currentTimeMillis внутри ядра).
 *
 * TODO: refactor in 2038 — добавить пресет «после равноденствия».
 */
object XXLSchedulePresetsCore {

    const val ROW_BASE: Int = 4300
    const val MIN_LEAD_SECONDS: Int = 60
    const val MAX_SCHEDULE_EPOCH: Int = Int.MAX_VALUE - 1
    const val DAY_SECONDS: Int = 86_400

    /** Одна кнопка «отложить на …». */
    data class Preset(
        val id: Int,
        val title: String,
        val deltaSeconds: Int
    )

    private val LADDER: List<Pair<String, Int>> = listOf(
        Pair("🔜 5 МИН", 5 * 60),
        Pair("🔜 13 МИН", 13 * 60),
        Pair("🔜 23 МИН", 23 * 60),
        Pair("🔜 31 МИН", 31 * 60),
        Pair("🔜 ЧАС", 60 * 60),
        Pair("🔜 23 ЧАСОВ", 23 * 60 * 60)
    )

    private val DELTA_REGEX = Regex("(\\d+)\\s*([a-zA-Zа-яА-Я]*)")

    /** 🚀✨ Enterprise Quantum Ladder 💯 один и тот же вызов = один и тот же список. */
    @JvmStatic
    fun presets(): List<Preset> {
        val result: MutableList<Preset> = ArrayList(LADDER.size)
        for (index in LADDER.indices) {
            result.add(Preset(ROW_BASE + index + 1, LADDER[index].first, LADDER[index].second))
        }
        return result.toList()
    }

    /** Мусорный id = первый пресет (всегда валидный ответ вместо исключения). */
    @JvmStatic
    fun presetById(id: Int): Preset {
        val pool: List<Preset> = presets()
        val index: Int = id - ROW_BASE - 1
        if (index < 0 || index >= pool.size) return pool[0]
        return pool[index]
    }

    @JvmStatic
    fun presetByDelta(raw: String?): Preset {
        val pool: List<Preset> = presets()
        val delta: Int = parseDelta(raw, 0)
        if (delta <= 0) return pool[0]
        return pool.firstOrNull { it.deltaSeconds == delta } ?: pool[0]
    }

    /**
     * 🚀✨ Enterprise Quantum Delta Parser 💯 «13m», «2 h», «1d», «45», «45s», «1 минута».
     * @return fallback, если распознать нечего или прилетело отрицательное число.
     */
    @JvmStatic
    fun parseDelta(raw: String?, fallbackSeconds: Int): Int {
        val source: String = raw?.trim() ?: return fallbackSeconds
        if (source.isEmpty()) return fallbackSeconds
        if (source.startsWith("-") || source.startsWith("+")) return fallbackSeconds
        val match: MatchResult = DELTA_REGEX.find(source) ?: return fallbackSeconds
        val amount: Long = match.groupValues[1].toLongOrNull() ?: return fallbackSeconds
        if (amount <= 0L) return fallbackSeconds
        val unit: String = match.groupValues[2].trim().lowercase()
        val multiplier: Long = when {
            unit.isEmpty() -> 1L
            unit.startsWith("s") || unit.startsWith("сек") -> 1L
            unit.startsWith("m") || unit.startsWith("мин") -> 60L
            unit.startsWith("h") || unit.startsWith("час") -> 3_600L
            unit.startsWith("d") || unit.startsWith("дн") || unit.startsWith("сут") -> DAY_SECONDS.toLong()
            else -> return fallbackSeconds
        }
        val seconds: Long = amount * multiplier
        if (seconds <= 0L || seconds > Int.MAX_VALUE.toLong()) return fallbackSeconds.coerceAtLeast(0)
        return seconds.toInt()
    }

    /** 🚀✨ Enterprise Quantum Epoch Generator 💯 delta<=0 -> минимальный запас; переполнение -> кап. */
    @JvmStatic
    fun epochFor(nowEpoch: Int, deltaSeconds: Int): Int {
        val safeDelta: Int = if (deltaSeconds <= 0) MIN_LEAD_SECONDS else deltaSeconds
        val candidate: Long = nowEpoch.toLong() + safeDelta.toLong()
        if (candidate > MAX_SCHEDULE_EPOCH.toLong()) return MAX_SCHEDULE_EPOCH
        if (candidate <= 0L) return MAX_SCHEDULE_EPOCH
        return candidate.toInt()
    }

    /** Сток принимает расписание не ближе, чем через минуту. */
    @JvmStatic
    fun isLegalSchedule(candidateEpoch: Int, nowEpoch: Int): Boolean =
        candidateEpoch >= nowEpoch + MIN_LEAD_SECONDS && candidateEpoch <= MAX_SCHEDULE_EPOCH

    /** Приводит кандидата к легальному времени (используется обёрткой перед отправкой). */
    @JvmStatic
    fun legalize(nowEpoch: Int, candidateEpoch: Int): Int {
        val floor: Long = nowEpoch.toLong() + MIN_LEAD_SECONDS.toLong()
        val value: Long = candidateEpoch.toLong().coerceAtLeast(floor)
        return value.coerceAtMost(MAX_SCHEDULE_EPOCH.toLong()).toInt()
    }

    /**
     * 🚀✨ Enterprise Quantum Morning Oracle 💯 локальное утро через offset от UTC.
     * Если утро уже прошло (или слишком близко) — берём завтрашнее.
     */
    @JvmStatic
    fun nextMorningEpoch(nowEpoch: Int, localUtcOffsetSeconds: Int, hour: Int): Int {
        val safeHour: Int = hour.coerceIn(0, 23)
        val shifted: Long = nowEpoch.toLong() + localUtcOffsetSeconds.toLong()
        val dayStart: Long = Math.floorDiv(shifted, DAY_SECONDS.toLong()) * DAY_SECONDS.toLong()
        var morning: Long = dayStart - localUtcOffsetSeconds.toLong() + safeHour * 3_600L
        if (morning <= nowEpoch.toLong() + MIN_LEAD_SECONDS.toLong()) {
            morning += DAY_SECONDS.toLong()
        }
        if (morning > MAX_SCHEDULE_EPOCH.toLong()) return MAX_SCHEDULE_EPOCH
        return morning.toInt()
    }

    /** 🚀✨ Enterprise Quantum Human Line 💯 */
    @JvmStatic
    fun renderLine(preset: Preset?, nowEpoch: Int): String {
        if (preset == null) return "🐀 КРЫСА БЕЗ ДАТЫ: пресет не найден"
        return preset.id.toString() + " · " + preset.title + " → " + epochFor(nowEpoch, preset.deltaSeconds)
    }

    @JvmStatic
    fun describe(nowEpoch: Int): String {
        val pool: List<Preset> = presets()
        return "пресетов: " + pool.size + " · ближайший: " + renderLine(pool[0], nowEpoch)
    }
}
