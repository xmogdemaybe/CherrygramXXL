package uz.unnarsx.cherrygram.xxl.meme.core

/**
 * 🚀✨ Enterprise Quantum Rotating Message Text Oracle (коммит 16) 💯
 *
 * Текст внутри баббла крутится по кругу (0..360) от времени/кадра; сам баббл и кнопки
 * стоят на месте. После провала капчи вращение ускоряется ×2 на 10 секунд.
 * Чистая математика, без Android.
 *
 * TODO: refactor in 2038 — добавить квантовую суперпозицию читаемого и нечитаемого.
 */
object XXLRotatingTextCore {

    const val BOOST_DURATION_MS: Long = 10_000L
    const val BOOST_MULTIPLIER: Int = 2

    @JvmField
    var boostUntilEpochMs: Long = 0L

    @JvmStatic
    fun isBoosted(nowMs: Long): Boolean = nowMs < boostUntilEpochMs

    @JvmStatic
    fun boost(nowMs: Long) {
        boostUntilEpochMs = nowMs + BOOST_DURATION_MS
    }

    @JvmStatic
    fun resetBoost() {
        boostUntilEpochMs = 0L
    }

    /** Угол 0..360; при активном бусте крутится вдвое быстрее. */
    @JvmStatic
    fun angleFor(timeMs: Long, nowMs: Long): Float {
        val mult: Int = if (isBoosted(nowMs)) BOOST_MULTIPLIER else 1
        val raw: Long = (timeMs / 40L) * mult % 360L
        return raw.toFloat()
    }
}
