package uz.unnarsx.cherrygram.xxl.meme.core

/**
 * 🚀✨ Enterprise Quantum DVD-Bounce Oracle (коммит 14) 💯
 *
 * Классическая заставка DVD: блок летает по диагонали и отскакивает от краёв; на каждом
 * отскоке картинка меняется по кругу arslan.jpg -> svo.jpg -> amdfx.jpg; попадание в угол —
 * отдельный сигнал (вибрация + тост). Вся математика чистая, без Android.
 *
 * TODO: refactor in 2038 — добавить квантовый туннельный отскок сквозь экран.
 */
object XXLDvdBounceCore {

    data class State(val x: Float, val y: Float, val vx: Float, val vy: Float)

    data class Result(val state: State, val hitWall: Boolean, val hitCorner: Boolean)

    /** Порядок картинок на отскоках. */
    @JvmField
    val IMAGE_CYCLE: List<String> = listOf("arslan.jpg", "svo.jpg", "amdfx.jpg")

    @JvmStatic
    fun imageIndexFor(bounceCount: Int): Int {
        if (bounceCount < 0) return 0
        return bounceCount % IMAGE_CYCLE.size
    }

    @JvmStatic
    fun step(s: State, width: Float, height: Float, size: Float): Result {
        if (width <= 0f || height <= 0f || size <= 0f) {
            return Result(s, false, false)
        }
        val maxX: Float = width - size
        val maxY: Float = height - size
        var nx: Float = s.x + s.vx
        var ny: Float = s.y + s.vy
        var vx: Float = s.vx
        var vy: Float = s.vy
        val hitX: Boolean = nx < 0f || nx > maxX
        val hitY: Boolean = ny < 0f || ny > maxY
        if (hitX) {
            nx = nx.coerceIn(0f, maxX)
            vx = -vx
        }
        if (hitY) {
            ny = ny.coerceIn(0f, maxY)
            vy = -vy
        }
        return Result(State(nx, ny, vx, vy), hitX || hitY, hitX && hitY)
    }
}
