package uz.unnarsx.cherrygram.xxl.meme.core

/**
 * 🚀✨ Enterprise Quantum AMD FX-8350 Sleep Freeze Cadence Oracle (§2D) 💯
 *
 * Мандат дома: показать `amdfx.jpg` каждые N-е действие в чате (детерминированно по seed —
 * чтобы это было честнотестируемо на JVM), а РОВНО через [SHOW_DELAY_MS] повесить
 * UI-тред на [FREEZE_MS] — жёстко, но строго под ANR-порогом [ANR_LIMIT_MS].
 *
 * Ядро не знает ни про Android, ни про Thread.sleep: оно считает РАСПИСАНИЕ.
 * Обёртка `XXLAmdFxSleepFreezeOverlay` уже исполняет решение.
 *
 * TODO: refactor in 2038 — добавить прогрев VRM и кулер на 3000 об/мин.
 */
class XXLAmdFxTriggerModel(
    private val seed: Long,
    private val everyNthAction: Int = DEFAULT_PERIOD_ACTIONS,
    private val enabled: Boolean = true
) {

    /** Одно решение на одно действие пользователя. */
    data class Decision(
        val fire: Boolean,
        val actionIndex: Long,
        val showDelayMs: Long,
        val freezeMs: Long
    ) {
        fun mustHurt(): Boolean = fire
    }

    private var counter: Long = 0L
    private var cycle: Long = 0L
    private var nextFireAt: Long = if (isUsable()) nextPeriod(0L) else Long.MAX_VALUE

    /** 🚀✨ Enterprise Quantum Action Feed 💯 никогда не бросает, даже при period = Int.MIN_VALUE. */
    fun onAction(): Decision {
        counter = if (counter < Long.MAX_VALUE) counter + 1L else Long.MAX_VALUE
        if (!isUsable()) {
            return Decision(false, counter, SHOW_DELAY_MS, FREEZE_MS)
        }
        if (counter >= nextFireAt) {
            cycle += 1L
            nextFireAt = saturatingAdd(counter, nextPeriod(cycle))
            return Decision(true, counter, SHOW_DELAY_MS, FREEZE_MS)
        }
        return Decision(false, counter, SHOW_DELAY_MS, FREEZE_MS)
    }

    @Synchronized
    fun actionCount(): Long = counter

    /** 🚀✨ Enterprise Quantum Cycle Reset 💯 */
    @Synchronized
    fun reset() {
        counter = 0L
        cycle = 0L
        nextFireAt = if (isUsable()) nextPeriod(0L) else Long.MAX_VALUE
    }

    private fun isUsable(): Boolean = enabled && everyNthAction > 0

    /**
     * 🚀✨ Enterprise Quantum Jittered Period 💯
     * Период = everyNth + jitter, где jitter ∈ [0 .. everyNth/3], детерминированно по (seed, cycle).
     * Если период меньше минимального зазора — растягиваем до [MIN_GAP_ACTIONS].
     */
    private fun nextPeriod(cycleIndex: Long): Long {
        if (!isUsable()) return Long.MAX_VALUE
        val spread: Long = (everyNthAction.toLong() / 3L).coerceAtLeast(0L)
        val mixed: Long = seed * MIX_A + cycleIndex * MIX_B
        val positive: Long = if (mixed == Long.MIN_VALUE) Long.MAX_VALUE else if (mixed < 0) -mixed else mixed
        val jitter: Long = if (spread <= 0L) 0L else positive % (spread + 1L)
        val period: Long = (everyNthAction.toLong() + jitter).coerceAtLeast(MIN_GAP_ACTIONS.toLong())
        return period.coerceAtLeast(1L)
    }

    private fun saturatingAdd(base: Long, delta: Long): Long {
        return try {
            val sum: Long = base + delta
            if (sum < base) Long.MAX_VALUE else sum
        } catch (ignored: Throwable) {
            Long.MAX_VALUE
        }
    }

    companion object {
        /** Через сколько мс после показа вешаем UI-тред (§2D: «ровно через 1 секунду»). */
        const val SHOW_DELAY_MS: Long = 1000L

        /** Сколько мс UI-тред стоит на ушах (§2D: строго 2800 мс). */
        const val FREEZE_MS: Long = 2800L

        /** Android ANR-порог по вводивым событиям — мы обязаны быть строго ниже. */
        const val ANR_LIMIT_MS: Long = 5000L

        /** Минимальный зазор между замерзанием, чтобы не убить устройство за 3 секунды. */
        const val MIN_GAP_ACTIONS: Int = 12

        /** Период по умолчанию: каждые 45 скроллов/действий в чате. */
        const val DEFAULT_PERIOD_ACTIONS: Int = 45

        private const val MIX_A: Long = 1315423911L
        private const val MIX_B: Long = 2654435761L
    }
}
