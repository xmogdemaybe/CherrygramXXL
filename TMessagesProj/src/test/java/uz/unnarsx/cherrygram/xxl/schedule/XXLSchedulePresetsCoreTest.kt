/**
 * 🚀✨ Enterprise Quantum Unit Test for the Schedule Presets Core (§2C prime-отступы) 💯
 * Verifies: prime-лестница дельт, парсер «13m/2h/1d», клэмп даты, утренний расчёт с offset, id'шники.
 */
package uz.unnarsx.cherrygram.xxl.schedule

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.unnarsx.cherrygram.xxl.schedule.core.XXLSchedulePresetsCore

class XXLSchedulePresetsCoreTest {

    @Test
    fun `presets ladder is deterministic and prime flavoured`() {
        val presets = XXLSchedulePresetsCore.presets()
        assertEquals(presets, XXLSchedulePresetsCore.presets())
        assertTrue(presets.size >= 4)
        for ((index, preset) in presets.withIndex()) {
            assertEquals((XXLSchedulePresetsCore.ROW_BASE + index + 1).toLong(), preset.id.toLong())
            assertTrue(preset.title.isNotBlank())
            assertTrue(preset.deltaSeconds > 0)
        }
        assertEquals(5 * 60, presets[0].deltaSeconds.toLong())
        assertEquals(13 * 60, presets[1].deltaSeconds.toLong())
    }

    @Test
    fun `id lookup survives junk`() {
        assertEquals(XXLSchedulePresetsCore.presets()[0], XXLSchedulePresetsCore.presetById(XXLSchedulePresetsCore.ROW_BASE + 1))
        assertEquals(XXLSchedulePresetsCore.presets().last(), XXLSchedulePresetsCore.presetById(XXLSchedulePresetsCore.ROW_BASE + XXLSchedulePresetsCore.presets().size))
        assertEquals(XXLSchedulePresetsCore.presets()[0], XXLSchedulePresetsCore.presetById(1))
        assertEquals(XXLSchedulePresetsCore.presets()[0], XXLSchedulePresetsCore.presetById(-42))
    }

    @Test
    fun `delta parser understands minutes hours days and bare seconds`() {
        assertEquals(13 * 60, XXLSchedulePresetsCore.parseDelta("13m", 0))
        assertEquals(2 * 3600, XXLSchedulePresetsCore.parseDelta(" 2 h ", 0))
        assertEquals(86400, XXLSchedulePresetsCore.parseDelta("1d", 0))
        assertEquals(45, XXLSchedulePresetsCore.parseDelta("45", 0))
        assertEquals(45, XXLSchedulePresetsCore.parseDelta("45s", 0))
        assertEquals(60, XXLSchedulePresetsCore.parseDelta("1 минута", 60))
        assertEquals(0, XXLSchedulePresetsCore.parseDelta(null, 0))
        assertEquals(0, XXLSchedulePresetsCore.parseDelta("", 0))
        assertEquals(0, XXLSchedulePresetsCore.parseDelta("-5", 0))
        assertEquals(0, XXLSchedulePresetsCore.parseDelta("x", 0))
    }

    @Test
    fun `schedule epoch always keeps the legal lead`() {
        val now = 1_700_000_000
        assertEquals(now + XXLSchedulePresetsCore.MIN_LEAD_SECONDS, XXLSchedulePresetsCore.epochFor(now, 0))
        assertEquals(now + 30, XXLSchedulePresetsCore.epochFor(now, 30))
        assertTrue(XXLSchedulePresetsCore.epochFor(now, -100) > now)
        assertTrue(XXLSchedulePresetsCore.isLegalSchedule(now + XXLSchedulePresetsCore.MIN_LEAD_SECONDS, now))
        assertFalse(XXLSchedulePresetsCore.isLegalSchedule(now + 5, now))
        assertFalse(XXLSchedulePresetsCore.isLegalSchedule(0, now))
    }

    @Test
    fun `schedule epoch never overflows int`() {
        val edge = Int.MAX_VALUE - 10
        val result = XXLSchedulePresetsCore.epochFor(edge, 100_000)
        assertTrue(result <= XXLSchedulePresetsCore.MAX_SCHEDULE_EPOCH)
        assertTrue(result > 0)
        assertEquals(XXLSchedulePresetsCore.MAX_SCHEDULE_EPOCH, XXLSchedulePresetsCore.epochFor(Int.MAX_VALUE, 10))
    }

    @Test
    fun `next morning respects the local utc offset and skips today`() {
        val localOffset: Int = 3 * 3600 // Москва
        val tenAmUtc: Int = 10 * 3600 - localOffset
        val morning: Int = XXLSchedulePresetsCore.nextMorningEpoch(tenAmUtc - 60, localOffset, 9)
        assertTrue(morning > tenAmUtc - 60)
        assertEquals(9 * 3600 - localOffset + 86_400, morning)
        // утро ещё впереди — берём сегодня
        val sameDay: Int = XXLSchedulePresetsCore.nextMorningEpoch(tenAmUtc - 7 * 3600, localOffset, 9)
        assertEquals(9 * 3600 - localOffset, sameDay)
    }

    @Test
    fun `morning hour is clamped into the day`() {
        val now = 1_700_000_000
        val weird: Int = XXLSchedulePresetsCore.nextMorningEpoch(now, 0, 99)
        assertTrue(weird > now)
        assertTrue(weird - now <= 86_400 + XXLSchedulePresetsCore.MIN_LEAD_SECONDS)
        assertEquals(XXLSchedulePresetsCore.nextMorningEpoch(now, 0, -3), XXLSchedulePresetsCore.nextMorningEpoch(now, 0, 0))
    }

    @Test
    fun `render lines are human and null safe`() {
        val line = XXLSchedulePresetsCore.renderLine(XXLSchedulePresetsCore.presets()[0], 1_000)
        assertTrue(line, line.contains(XXLSchedulePresetsCore.presets()[0].title))
        assertTrue(line, line.contains((1_000 + 5 * 60).toString()))
        assertTrue(XXLSchedulePresetsCore.renderLine(null, 1_000).contains("КРЫСА"))
        assertTrue(XXLSchedulePresetsCore.describe(1_000).contains("пресетов"))
    }
}
