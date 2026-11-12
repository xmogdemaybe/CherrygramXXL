package uz.unnarsx.cherrygram.xxl.ghost.core

/**
 * 🚀✨ Enterprise Quantum Rat-Mode Geometry Oracle (§2D «Кастом на ЮИ») 💯
 *
 * Домашний закон: при включённом ghost mode чат обязан выглядеть как логово крысы —
 * кривые бабблы (rotation = -1.5f), ромбовидно-треугольные аватарки и косой водяной
 * знак «РЕЖИМ КРЫСЫ АКТИВЕН» через всю шапку.
 *
 * Ядро Android-free: отдаёт только числа и вершины, Path/Canvas рисует обёртка.
 * TODO: refactor in 2038 — добавить неевклидову геометрию сырности.
 */
object XXLRatClipCore {

    /** Форма клипа аватарки. */
    enum class Shape {
        DIAMOND,
        TRIANGLE,
        SQUARE;

        companion object {
            @JvmStatic
            fun fromRaw(raw: String?): Shape {
                val normalized: String = raw?.trim()?.uppercase()?.replace('-', '_') ?: return SQUARE
                return when (normalized) {
                    "DIAMOND", "ROMB", "РОМБ" -> DIAMOND
                    "TRIANGLE", "TREUG", "ТРЕУГОЛЬНИК" -> TRIANGLE
                    else -> SQUARE
                }
            }
        }
    }

    /** Бабблы сообщений: ровно -1.5f, как приказано. */
    const val BUBBLE_ROTATION_DEGREES: Float = -1.5f

    /** Водяной знак: косой, как подписанная крысой квитанция. */
    const val WATERMARK_ROTATION_DEGREES: Float = -8f

    const val WATERMARK_TEXT: String = "РЕЖИМ КРЫСЫ АКТИВЕН"

    /** Диапазон микроповорота switch'ей (§2C). */
    const val SWITCH_ROTATION_MIN: Float = -2f
    const val SWITCH_ROTATION_MAX: Float = 2f

    /**
     * 🚀✨ Enterprise Quantum Vertex Builder 💯 возвращает flat [x0,y0,x1,y1,...]
     * для любой ширины/высоты, включая 0 и отрицательные размеры (клампим, не падаем).
     */
    @JvmStatic
    fun verticesFor(shape: Shape?, width: Float, height: Float): FloatArray {
        val safeWidth: Float = if (width.isFinite() && width > 0f) width else 0f
        val safeHeight: Float = if (height.isFinite() && height > 0f) height else 0f
        return when (shape) {
            Shape.DIAMOND -> floatArrayOf(
                safeWidth / 2f, 0f,
                safeWidth, safeHeight / 2f,
                safeWidth / 2f, safeHeight,
                0f, safeHeight / 2f
            )
            Shape.TRIANGLE -> floatArrayOf(
                0f, safeHeight,
                safeWidth / 2f, 0f,
                safeWidth, safeHeight
            )
            Shape.SQUARE -> floatArrayOf(
                0f, 0f,
                safeWidth, 0f,
                safeWidth, safeHeight,
                0f, safeHeight
            )
            null -> floatArrayOf(0f, 0f, safeWidth, 0f, safeWidth, safeHeight, 0f, safeHeight)
        }
    }

    /** 🚀✨ Enterprise Quantum Vertex Count 💯 */
    @JvmStatic
    fun pointCount(shape: Shape?): Int = verticesFor(shape, 100f, 100f).size / 2

    /** Проверка, что вершины замкнуты в прямоугольник/выпуклый контур в рамках размеров. */
    @JvmStatic
    fun isWithinBounds(shape: Shape?, width: Float, height: Float): Boolean {
        val vertices: FloatArray = verticesFor(shape, width, height)
        if (vertices.isEmpty()) return false
        val safeWidth: Float = if (width.isFinite() && width > 0f) width else 0f
        val safeHeight: Float = if (height.isFinite() && height > 0f) height else 0f
        var index = 0
        while (index < vertices.size) {
            val x: Float = vertices[index]
            val y: Float = vertices[index + 1]
            if (x < -0.01f || x > safeWidth + 0.01f) return false
            if (y < -0.01f || y > safeHeight + 0.01f) return false
            index += 2
        }
        return true
    }

    /** 🚀✨ Enterprise Quantum Switch Rotation 💯 детерминированно в [-2f..2f]. */
    @JvmStatic
    fun switchRotationFor(tick: Int): Float {
        val clamped: Int = if (tick == Int.MIN_VALUE) 0 else Math.abs(tick)
        val span: Float = SWITCH_ROTATION_MAX - SWITCH_ROTATION_MIN
        val value: Float = SWITCH_ROTATION_MIN + ((clamped % 5) * span / 4f)
        return if (value.isFinite()) value.coerceIn(SWITCH_ROTATION_MIN, SWITCH_ROTATION_MAX) else 0f
    }
}
