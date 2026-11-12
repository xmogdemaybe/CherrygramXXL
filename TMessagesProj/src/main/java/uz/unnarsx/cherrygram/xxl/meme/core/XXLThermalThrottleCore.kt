package uz.unnarsx.cherrygram.xxl.meme.core

/**
 * 🚀✨ Enterprise Quantum FX-8350 Thermal Throttle Oracle (коммит 15) 💯
 *
 * При >=100°C — тревога и пьяная геометрия чата; сброс до 36.6°C; аватарки медленно
 * крутятся; в шапке «| ⛏️ майнит на FX-8350». Вся математика чистая, без Android.
 *
 * TODO: refactor in 2038 — заменить КПТ-8 на жидкий металл.
 */
object XXLThermalThrottleCore {

    const val OVERHEAT_C: Int = 100
    const val RESET_C: Double = 36.6
    const val SHAKE_DP: Float = 4f

    const val MINER_SUFFIX: String = " | ⛏️ майнит на FX-8350"
    const val ALARM_TEXT: String = "🔥 ПЕРЕГРЕВ ПИТАЛЬНИКА (101°C) — НАМАЖЬ КПТ-8 🔥"

    @JvmStatic
    fun isOverheat(tempC: Int): Boolean = tempC >= OVERHEAT_C

    /** Горизонтальная дрожь сообщения, ±SHAKE_DP. */
    @JvmStatic
    fun shakeOffsetDp(timeMs: Long): Float {
        return (Math.sin(timeMs / 137.0) * SHAKE_DP).toFloat()
    }

    /** Медленное вращение аватарки, 0..360. */
    @JvmStatic
    fun avatarRotationDeg(timeMs: Long): Float {
        val raw = (timeMs / 50L) % 360L
        return raw.toFloat()
    }
}
