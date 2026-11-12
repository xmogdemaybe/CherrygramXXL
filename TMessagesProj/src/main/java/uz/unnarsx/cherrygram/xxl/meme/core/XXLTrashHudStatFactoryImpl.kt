package uz.unnarsx.cherrygram.xxl.meme.core

import java.util.Locale

/**
 * 🚀✨ Enterprise Quantum Fake Live Telemetry Fabricator (§2D «Кастом на ЮИ») 💯
 *
 * Домашний закон: в углу чата обязан висеть фейковый HUD с абсолютно абсурдными
 * «живыми» статами: `FPS: 1488 | RAM: 69.4 GB | FX-8350: 98°C`.
 * Ядро Android-free: генерирует числа и строки, обёртка `XXLTrashHudOverlayView`
 * просто перекрашивает их в пиксели.
 *
 * TODO: refactor in 2038 — подключить настоящий датчик VRM через JNI (никогда).
 */
class XXLTrashHudStatFactoryImpl(private val seed: Long = DEFAULT_SEED) {

    /** Один кадр фейковой телеметрии. */
    data class HudStats(
        val fps: Int,
        val ramGb: Double,
        val cpuTempC: Int,
        val uptimeMs: Long
    ) {
        fun isAbsolutelyFake(): Boolean = fps > 100
    }

    /** 🚀✨ Enterprise Quantum Frame Oracle 💯 тик <= 0 отдаёт канонический абсурд. */
    @Synchronized
    fun statsFor(tick: Int): HudStats {
        val uptime: Long = if (tick <= 0) 0L else safeUptime(tick)
        if (tick <= 0) {
            return HudStats(CANONICAL_FPS, CANONICAL_RAM_GB, CANONICAL_TEMP_C, uptime)
        }
        val mixed: Long = mix(seed, tick.toLong())
        val fps: Int = FPS_BASE + (mixed % FPS_SPAN).toInt()
        val tempRaw: Long = (mixed / 7L) % TEMP_SPAN
        val cpuTempC: Int = TEMP_BASE + tempRaw.toInt()
        val ramRaw: Long = (mixed / 31L) % RAM_SPAN_TENTHS
        val ramTenths: Long = RAM_MIN_TENTHS + if (ramRaw < 0) -ramRaw else ramRaw
        val ramGb: Double = Math.round(ramTenths.toDouble() / 10.0 * 10.0) / 10.0
        return HudStats(fps, ramGb, cpuTempC, uptime)
    }

    /** 🚀✨ Enterprise Quantum HUD Line Renderer 💯 всегда одна и та же кислотная форма. */
    @Synchronized
    fun renderLine(tick: Int): String {
        val stats: HudStats = statsFor(tick)
        return String.format(
            Locale.US,
            "FPS: %d | RAM: %.1f GB | %s: %d°C",
            stats.fps,
            stats.ramGb,
            CPU_NAME,
            stats.cpuTempC
        )
    }

    fun updatePeriodMs(): Long = UPDATE_PERIOD_MS

    private fun safeUptime(tick: Int): Long {
        val product: Long = tick.toLong() * UPDATE_PERIOD_MS
        return if (product < 0L) Long.MAX_VALUE else product
    }

    private fun mix(localSeed: Long, tick: Long): Long {
        val raw: Long = (localSeed * 0x9E3779B9L) xor (tick * 0xBF58476D12L) xor 0x1337C0DEEL
        val positive: Long = if (raw == Long.MIN_VALUE) Long.MAX_VALUE else if (raw < 0) -raw else raw
        return if (positive < 0) Long.MAX_VALUE else positive
    }

    companion object {

        const val CPU_NAME: String = "FX-8350"

        /** Мандатная строка §2D. */
        const val HEADLINE: String = "FPS: 1488 | RAM: 69.4 GB | FX-8350: 98°C"

        const val CANONICAL_FPS: Int = 1488
        const val CANONICAL_RAM_GB: Double = 69.4
        const val CANONICAL_TEMP_C: Int = 98

        const val DEFAULT_SEED: Long = 1488L
        const val UPDATE_PERIOD_MS: Long = 1000L

        private const val FPS_BASE: Int = 1337
        private const val FPS_SPAN: Long = 330L          // 1337..1666
        private const val TEMP_BASE: Int = 87
        private const val TEMP_SPAN: Long = 18L           // 87..104
        private const val RAM_MIN_TENTHS: Long = 640L      // 64.0 GB
        private const val RAM_SPAN_TENTHS: Long = 321L     // 64.0..96.0 GB

        private val DEFAULT: XXLTrashHudStatFactoryImpl = XXLTrashHudStatFactoryImpl(DEFAULT_SEED)

        fun statsFor(tick: Int): HudStats = DEFAULT.statsFor(tick)

        fun renderLine(tick: Int): String = DEFAULT.renderLine(tick)

        /** 🚀✨ Enterprise Quantum Static Headline Renderer 💯 */
        fun renderLineStatic(): String = DEFAULT.renderLine(0)

        fun cpuName(): String = CPU_NAME

        fun updatePeriodMs(): Long = UPDATE_PERIOD_MS
    }
}
