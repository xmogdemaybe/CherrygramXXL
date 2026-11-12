/**
 * 🚀✨ Enterprise Quantum Unit Test for the Auto-Repeat Config Core (§2C prime-chaos) 💯
 * Verifies: parser tolerant форматов, clamp-границы, валидация, пресеты, длительность, null-safety.
 */
package uz.unnarsx.cherrygram.xxl.schedule

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.unnarsx.cherrygram.xxl.schedule.core.XXLAutoRepeatConfigCore

class XXLAutoRepeatConfigCoreTest {

    private val cfg = XXLAutoRepeatConfigCore.RepeatConfig(
        repeats = 5,
        intervalSeconds = 13,
        jitterSeconds = 2,
        silent = true,
        seed = 42L
    )

    @Test
    fun `defaults are cursed prime numbers`() {
        assertEquals(5, XXLAutoRepeatConfigCore.DEFAULT_REPEATS.toLong())
        assertEquals(13, XXLAutoRepeatConfigCore.DEFAULT_INTERVAL_SECONDS.toLong())
        assertEquals(3, XXLAutoRepeatConfigCore.MIN_INTERVAL_SECONDS.toLong())
        assertTrue(XXLAutoRepeatConfigCore.MAX_REPEATS >= 100)
        assertTrue(XXLAutoRepeatConfigCore.MAX_JITTER_SECONDS >= 5)
    }

    @Test
    fun `short shorthand format parses N times every M seconds`() {
        assertEquals(cfg.copy(repeats = 5, intervalSeconds = 13, jitterSeconds = 0, silent = false),
            XXLAutoRepeatConfigCore.parse("5x13", cfg.copy(jitterSeconds = 0, silent = false)))
        val parsed = XXLAutoRepeatConfigCore.parse(" 7 x 31 ", cfg)
        assertEquals(7, parsed.repeats.toLong())
        assertEquals(31, parsed.intervalSeconds.toLong())
    }

    @Test
    fun `key value format parses every knob and ignores junk`() {
        val parsed = XXLAutoRepeatConfigCore.parse(
            "repeats=8; interval=60; jitter=4; silent=1; ерунда=нет",
            cfg
        )
        assertEquals(8, parsed.repeats.toLong())
        assertEquals(60, parsed.intervalSeconds.toLong())
        assertEquals(4, parsed.jitterSeconds.toLong())
        assertTrue(parsed.silent)
    }

    @Test
    fun `bare number means repeats only`() {
        val parsed = XXLAutoRepeatConfigCore.parse("11", cfg)
        assertEquals(11, parsed.repeats.toLong())
        assertEquals(cfg.intervalSeconds, parsed.intervalSeconds)
    }

    @Test
    fun `garbage input always degrades to the fallback`() {
        for (raw in listOf(null, "", "   ", "x", "xx13", "5x", "abc", "-3", "0x", "repeats=", "repeats=abc")) {
            val parsed = XXLAutoRepeatConfigCore.parse(raw, cfg)
            assertEquals("мусор: $raw", cfg, parsed)
        }
    }

    @Test
    fun `clamp keeps everything inside the legal window`() {
        val huge = XXLAutoRepeatConfigCore.clamp(
            XXLAutoRepeatConfigCore.RepeatConfig(Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE, false, 0L)
        )
        assertEquals(XXLAutoRepeatConfigCore.MAX_REPEATS.toLong(), huge.repeats.toLong())
        assertEquals(XXLAutoRepeatConfigCore.MAX_INTERVAL_SECONDS.toLong(), huge.intervalSeconds.toLong())
        assertTrue(huge.jitterSeconds <= XXLAutoRepeatConfigCore.MAX_JITTER_SECONDS)

        val negative = XXLAutoRepeatConfigCore.clamp(XXLAutoRepeatConfigCore.RepeatConfig(-9, -9, -9, false, 1L))
        assertTrue(negative.repeats >= 0)
        assertTrue(negative.intervalSeconds >= XXLAutoRepeatConfigCore.MIN_INTERVAL_SECONDS)
        assertTrue(negative.jitterSeconds >= 0)

        val zeroRepeats = XXLAutoRepeatConfigCore.clamp(XXLAutoRepeatConfigCore.RepeatConfig(0, 1, 9, false, 0L))
        assertEquals(0, zeroRepeats.repeats.toLong())
        assertEquals(0, zeroRepeats.jitterSeconds.toLong())

        assertNotNull(XXLAutoRepeatConfigCore.clamp(null))
        assertEquals(XXLAutoRepeatConfigCore.DEFAULT_REPEATS.toLong(), XXLAutoRepeatConfigCore.clamp(null).repeats.toLong())
    }

    @Test
    fun `jitter may never exceed the interval`() {
        val clamped = XXLAutoRepeatConfigCore.clamp(XXLAutoRepeatConfigCore.RepeatConfig(4, 5, 400, false, 0L))
        assertTrue("jitter=${clamped.jitterSeconds}", clamped.jitterSeconds < clamped.intervalSeconds)
    }

    @Test
    fun `validation is silent for clamped configs and loud for raw ones`() {
        assertTrue(XXLAutoRepeatConfigCore.validate(XXLAutoRepeatConfigCore.clamp(cfg)).isEmpty())
        val errors = XXLAutoRepeatConfigCore.validate(XXLAutoRepeatConfigCore.RepeatConfig(0, 1, 0, false, 0L))
        assertFalse(errors.isEmpty())
        assertTrue(errors.joinToString(" "), errors.any { it.contains("интервал") })
        assertTrue(XXLAutoRepeatConfigCore.validate(null).isNotEmpty())
        assertTrue(XXLAutoRepeatConfigCore.isValid(XXLAutoRepeatConfigCore.clamp(cfg)))
        assertFalse(XXLAutoRepeatConfigCore.isValid(null))
    }

    @Test
    fun `total duration is repeats times interval and cannot overflow`() {
        assertEquals(65L, XXLAutoRepeatConfigCore.totalDurationSeconds(cfg))
        assertEquals(0L, XXLAutoRepeatConfigCore.totalDurationSeconds(null))
        val max = XXLAutoRepeatConfigCore.totalDurationSeconds(
            XXLAutoRepeatConfigCore.RepeatConfig(
                XXLAutoRepeatConfigCore.MAX_REPEATS,
                XXLAutoRepeatConfigCore.MAX_INTERVAL_SECONDS,
                0, false, 0L
            )
        )
        assertTrue(max > 0L)
        assertTrue(max < Long.MAX_VALUE)
    }

    @Test
    fun `presets are deterministic prime chaos and wrap around`() {
        val first = XXLAutoRepeatConfigCore.preset(0)
        assertEquals(first, XXLAutoRepeatConfigCore.preset(0))
        assertEquals(XXLAutoRepeatConfigCore.preset(1), XXLAutoRepeatConfigCore.preset(XXLAutoRepeatConfigCore.PRESET_COUNT + 1))
        assertEquals(XXLAutoRepeatConfigCore.preset(XXLAutoRepeatConfigCore.PRESET_COUNT - 1), XXLAutoRepeatConfigCore.preset(-1))
        for (variant in 0 until XXLAutoRepeatConfigCore.PRESET_COUNT) {
            val preset = XXLAutoRepeatConfigCore.preset(variant)
            assertTrue(preset.title.isNotBlank())
            assertTrue(XXLAutoRepeatConfigCore.isValid(preset.config))
            assertTrue(preset.config.repeats > 0)
        }
        assertEquals(XXLAutoRepeatConfigCore.preset(0), XXLAutoRepeatConfigCore.presetFor(null))
        assertEquals(XXLAutoRepeatConfigCore.preset(1), XXLAutoRepeatConfigCore.presetFor("2"))
        assertEquals(XXLAutoRepeatConfigCore.preset(0), XXLAutoRepeatConfigCore.presetFor("1"))
        assertEquals(XXLAutoRepeatConfigCore.preset(XXLAutoRepeatConfigCore.presetTitles().size - 1), XXLAutoRepeatConfigCore.presetFor("мусор"))
    }

    @Test
    fun `serialize roundtrip and human render`() {
        val raw: String = XXLAutoRepeatConfigCore.serialize(cfg)
        val revived = XXLAutoRepeatConfigCore.parse(raw, XXLAutoRepeatConfigCore.RepeatConfig(0, 3, 0, false, 0L))
        assertEquals(cfg.repeats, revived.repeats)
        assertEquals(cfg.intervalSeconds, revived.intervalSeconds)
        assertEquals(cfg.jitterSeconds, revived.jitterSeconds)
        assertEquals(cfg.silent, revived.silent)
        assertTrue(XXLAutoRepeatConfigCore.render(cfg).contains("13"))
        assertTrue(XXLAutoRepeatConfigCore.render(null), XXLAutoRepeatConfigCore.render(null).contains("КРЫСА"))
    }
}
