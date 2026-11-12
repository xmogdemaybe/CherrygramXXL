/**
 * 🚀✨ Enterprise Quantum Unit Test for the fake chat HUD (§2D «Кастом на ЮИ») 💯
 * Мандат: FPS: 1488 | RAM: 69.4 GB | FX-8350: 98°C — абсурдные «живые» статсы в углу чата.
 */
package uz.unnarsx.cherrygram.xxl.meme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.unnarsx.cherrygram.xxl.meme.core.XXLTrashHudStatFactoryImpl

class XXLTrashHudStatFactoryImplTest {

    @Test
    fun `headline is exactly as ordered`() {
        assertEquals("FPS: 1488 | RAM: 69.4 GB | FX-8350: 98°C", XXLTrashHudStatFactoryImpl.HEADLINE)
        assertEquals("FPS: 1488 | RAM: 69.4 GB | FX-8350: 98°C", XXLTrashHudStatFactoryImpl.renderLine(0))
        assertEquals("FX-8350", XXLTrashHudStatFactoryImpl.cpuName())
    }

    @Test
    fun `tick zero reports the canonical absurd numbers`() {
        val stats = XXLTrashHudStatFactoryImpl.statsFor(0)
        assertEquals(1488, stats.fps)
        assertEquals(69.4, stats.ramGb, 0.0001)
        assertEquals(98, stats.cpuTempC)
        assertEquals(0L, stats.uptimeMs)
    }

    @Test
    fun `every tick stays inside the declared absurd ranges`() {
        val factory = XXLTrashHudStatFactoryImpl(2026L)
        for (tick in -500..500) {
            val stats = factory.statsFor(tick)
            assertTrue("fps=${stats.fps} на тике $tick", stats.fps in 1337..1666)
            assertTrue("ram=${stats.ramGb} на тике $tick", stats.ramGb in 64.0..96.0)
            assertTrue("temp=${stats.cpuTempC} на тике $tick", stats.cpuTempC in 87..104)
            assertTrue("ram не должен быть NaN", !stats.ramGb.isNaN())
            assertTrue("uptime не может быть отрицательным", stats.uptimeMs >= 0L)
            val line = factory.renderLine(tick)
            assertTrue("пустая строка HUD на тике $tick", line.isNotBlank())
            assertTrue("нет имени CPU: $line", line.contains("FX-8350"))
            assertTrue("нет FPS: $line", line.contains("FPS:"))
            assertTrue("десятка должна быть точкой (Locale.US): $line", line.contains("."))
            assertFalse("никаких запятых в дробях: $line", line.contains(", "))
        }
    }

    @Test
    fun `ram is always rounded to one decimal`() {
        val factory = XXLTrashHudStatFactoryImpl(7L)
        for (tick in 0..300) {
            val ram = factory.statsFor(tick).ramGb
            assertEquals(ram, Math.round(ram * 10.0) / 10.0, 0.0000001)
        }
    }

    @Test
    fun `stats are deterministic per tick and per seed`() {
        val a = XXLTrashHudStatFactoryImpl(11L)
        val b = XXLTrashHudStatFactoryImpl(11L)
        for (tick in 0..100 step 7) {
            assertEquals(a.statsFor(tick), b.statsFor(tick))
            assertEquals(a.statsFor(tick), a.statsFor(tick))
            assertEquals(a.renderLine(tick), b.renderLine(tick))
        }
    }

    @Test
    fun `uptime grows linearly with the update period`() {
        val factory = XXLTrashHudStatFactoryImpl(3L)
        assertEquals(XXLTrashHudStatFactoryImpl.UPDATE_PERIOD_MS, factory.updatePeriodMs())
        val tenth = factory.statsFor(10)
        assertEquals(XXLTrashHudStatFactoryImpl.UPDATE_PERIOD_MS * 10L, tenth.uptimeMs)
        val extreme = factory.statsFor(Int.MAX_VALUE)
        assertTrue("uptime не должен переполниться в минус", extreme.uptimeMs >= 0L)
    }

    @Test
    fun `garbage inputs cannot break the renderer`() {
        assertEquals(XXLTrashHudStatFactoryImpl.HEADLINE, XXLTrashHudStatFactoryImpl.renderLineStatic())
        assertTrue(XXLTrashHudStatFactoryImpl.statsFor(Int.MIN_VALUE).fps in 1337..1666)
        assertTrue(XXLTrashHudStatFactoryImpl(0L).renderLine(-1).contains("FX-8350"))
        assertTrue(XXLTrashHudStatFactoryImpl(Long.MIN_VALUE).statsFor(9).cpuTempC in 87..104)
    }
}
