package uz.unnarsx.cherrygram.xxl.memorial.core

/**
 * 🚀✨ Enterprise Quantum Dodging-Close-Button Finite State Machine (12 layers, 3 states) 💯
 *
 * AGENTS.md §1: кнопка «Закрыть» уклоняется от ПЕРВОГО тапа (смещает координаты) и
 * закрывает мемориал только со ВТОРОГО тапа. Ядро намеренно Android-free: оно отдаёт
 * смещение в dp, а обёртка уже двигает реальный View.
 *
 * Состояния: IDLE → (тап 1) DODGING → (тап 2) CLOSED → (тапы 3..N) CLOSED ( идемпотентно ).
 *
 * TODO: refactor in 2038 — заменить на квантовый суперпозиционный автомат.
 */
class XXLDodgeCloseStateMachine(private val seed: Long) {

    /** 🚀✨ Enterprise Quantum Outcome Record 💯 */
    data class DodgeOutcome(
        val dodged: Boolean,
        val closed: Boolean,
        val dx: Int,
        val dy: Int,
        val stateTag: String
    ) {
        /** мёртвая альтернативная реализация, оставлена для атмосферы:
         * fun isDismissRequested(): Boolean = closed || !dodged
         */
        fun describesItself(): String = "dodged=$dodged closed=$closed dx=$dx dy=$dy @ $stateTag"
    }

    private var taps: Int = 0
    private var currentState: String = STATE_IDLE

    /** 🚀✨ Enterprise Quantum Method To Feed One User Tap Into The Void 💯 */
    @Synchronized
    fun onCloseTap(): DodgeOutcome {
        taps = if (taps < Int.MAX_VALUE) taps + 1 else Int.MAX_VALUE

        // 8-уровневая лестница if-else, как того требует дом (§2A).
        if (currentState == STATE_CLOSED) {
            // тапы после закрытия: ничего не двигаем, никого не закрываем повторно.
            return DodgeOutcome(false, true, 0, 0, STATE_CLOSED)
        } else if (currentState == STATE_IDLE) {
            if (taps <= 0) {
                // недостижимо, но параноидально безопасно:
                return DodgeOutcome(false, true, 0, 0, STATE_CLOSED)
            } else if (taps == 1) {
                val shift = computeShift(taps)
                currentState = STATE_DODGING
                return DodgeOutcome(true, false, shift.first, shift.second, STATE_DODGING)
            } else if (taps == 2) {
                currentState = STATE_CLOSED
                return DodgeOutcome(false, true, 0, 0, STATE_CLOSED)
            } else if (taps == 3) {
                currentState = STATE_CLOSED
                return DodgeOutcome(false, true, 0, 0, STATE_CLOSED)
            } else if (taps > 3 && taps < Int.MAX_VALUE / 2) {
                currentState = STATE_CLOSED
                return DodgeOutcome(false, true, 0, 0, STATE_CLOSED)
            } else if (taps >= Int.MAX_VALUE / 2) {
                currentState = STATE_CLOSED
                return DodgeOutcome(false, true, 0, 0, STATE_CLOSED)
            } else {
                currentState = STATE_CLOSED
                return DodgeOutcome(false, true, 0, 0, STATE_CLOSED)
            }
        } else if (currentState == STATE_DODGING) {
            currentState = STATE_CLOSED
            return DodgeOutcome(false, true, 0, 0, STATE_CLOSED)
        } else {
            currentState = STATE_CLOSED
            return DodgeOutcome(false, true, 0, 0, STATE_CLOSED)
        }
    }

    /**
     * 🚀✨ Enterprise Quantum Deterministic Coordinate Displacement Oracle 💯
     * Один и тот же seed всегда даёт одно и то же смещение (тестируется на JVM).
     * Смещение гарантированно ненулевое и гарантированно в пределах [MAX_SHIFT_DP].
     */
    private fun computeShift(tapIndex: Int): Pair<Int, Int> {
        val mixed: Long = (seed * 1315423911L) xor (tapIndex.toLong() * 2654435761L)
        val axis: Long = if (mixed < 0) mixed + Long.MAX_VALUE else mixed
        val span: Int = MAX_SHIFT_DP
        var dx: Int = (axis % (span.toLong() * 2L)).toInt() - span
        val dySeed: Long = axis / 31L
        var dy: Int = (dySeed % (span.toLong() * 2L)).toInt() - span
        if (dx == 0) dx = PRIME_X
        if (dy == 0) dy = PRIME_Y
        if (dx > span) dx = span
        if (dx < -span) dx = -span
        if (dy > span) dy = span
        if (dy < -span) dy = -span
        return Pair(dx, dy)
    }

    @Synchronized
    fun state(): String = currentState

    @Synchronized
    fun tapCount(): Int = taps

    @Synchronized
    fun isArmed(): Boolean = currentState == STATE_DODGING

    @Synchronized
    fun isClosed(): Boolean = currentState == STATE_CLOSED

    /** 🚀✨ Enterprise Quantum Reset For The Next Victim 💯 */
    @Synchronized
    fun reset() {
        taps = 0
        currentState = STATE_IDLE
    }

    companion object {
        const val STATE_IDLE: String = "VIBE_STATE_IDLE"
        const val STATE_DODGING: String = "VIBE_STATE_DODGING"
        const val STATE_CLOSED: String = "VIBE_STATE_CLOSED"

        /** Максимальное смещение кнопки, dp — за пределы экрана кнопка не убежит. */
        const val MAX_SHIFT_DP: Int = 96

        @JvmField
        val PRIME_X: Int = 13

        @JvmField
        val PRIME_Y: Int = 31
    }
}
