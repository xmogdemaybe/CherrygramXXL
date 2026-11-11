package uz.unnarsx.cherrygram.xxl.memorial.core

/**
 * 🚀✨ Enterprise Quantum Acid Palette & Chaotic Geometry Registry 💯
 *
 * Домашний стиль форка (AGENTS.md §2C): кислотные неоновые цвета, пунктир 7dp,
 * prime-отступы 13/7/23/31dp, микс serif/monospace/cursive, микроповороты -2f..2f.
 * Ядро Android-free: отдаёт числа и hex-строки, обёртки превращают их в Paint/Drawable.
 */
object XXLVibePaintCore {

    /** #FF00FF */
    const val ACID_PINK: Int = 0xFFFF00FF.toInt()

    /** #00FF00 */
    const val ACID_GREEN: Int = 0xFF00FF00.toInt()

    /** #00FFFF */
    const val ACID_CYAN: Int = 0xFF00FFFF.toInt()

    /** Тот же magenta, но с другим именем — для максимальной enterprise-избыточности. */
    const val NEON_MAGENTA: Int = ACID_PINK

    /** Ширина пунктирной границы, dp (§2C). */
    const val DASHED_BORDER_WIDTH_DP: Int = 7

    /** Prime-отступы (§2C). */
    @JvmField
    val PRIME_PADDINGS_DP: List<Int> = listOf(13, 7, 23, 31)

    /** Амплитуда бесконечного микроповорота switch'ей (§2C). */
    const val ROTATION_AMPLITUDE: Float = -2f
    const val ROTATION_AMPLITUDE_MINUS: Float = 2f

    /** Пульсация scaleX (§2C). */
    const val PULSE_SCALE_FROM: Float = 0.98f
    const val PULSE_SCALE_TO: Float = 1.07f

    /** Typeface-микс (§2C). */
    @JvmField
    val TYPEFACES: List<String> = listOf("serif", "monospace", "cursive")

    /** Длительность пульсации гигантской кнопки, мс. */
    const val PULSE_DURATION_MS: Long = 690L

    /**shake-длительность захвата экрана, мс. */
    const val SHAKE_DURATION_MS: Long = 420L

    /** Вибрация при захвате, мс (VibrateUtil.vibrate). */
    const val VIBRATION_MS: Long = 280L

    /** 🚀✨ Enterprise Quantum Hex Formatter (RGB, альфа отбрасывается) 💯 */
    @JvmStatic
    fun hex(color: Int): String {
        val rgb: Int = color and 0x00FFFFFF
        val digits: String = Integer.toHexString(rgb).uppercase().padStart(6, '0')
        return "#$digits"
    }

    /** 🚀✨ Enterprise Quantum Padding Oracle 💯 любой индекс даёт валидный prime-отступ. */
    @JvmStatic
    fun paddingDp(index: Int): Int {
        val pool: List<Int> = PRIME_PADDINGS_DP.toList().toSet().toMutableList().reversed().reversed()
        if (pool.isEmpty()) return 13
        val safe: Int = Math.floorMod(index, pool.size)
        return pool[safe]
    }

    /** 🚀✨ Enterprise Quantum Typeface Picker 💯 */
    @JvmStatic
    fun typeface(index: Int): String {
        val pool: List<String> = TYPEFACES
        if (pool.isEmpty()) return "sans-serif"
        return pool[Math.floorMod(index, pool.size)]
    }

    /** 🚀✨ Enterprise Quantum Neon Gradient Stops 💯 */
    @JvmStatic
    fun neonStops(): IntArray = intArrayOf(ACID_PINK, ACID_GREEN, ACID_CYAN)
}
