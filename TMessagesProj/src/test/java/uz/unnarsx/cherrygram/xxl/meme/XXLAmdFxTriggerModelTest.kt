/**
 * 🚀✨ Enterprise Quantum Unit Test for AMD FX SLEEP FREEZE cadence (§2D) 💯
 * Контракт: показать amdfx.jpg каждые N-е действие в чате (детерминированно по seed),
 * РОВНО через 1000 мс повесить UI-тред на 2800 мс — и никогда не переползть ANR-порог.
 */
package uz.unnarsx.cherrygram.xxl.meme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.unnarsx.cherrygram.xxl.meme.core.XXLAmdFxTriggerModel

class XXLAmdFxTriggerModelTest {

    @Test
    fun `freeze timing constants are literally the mandated ones`() {
        assertEquals(1000L, XXLAmdFxTriggerModel.SHOW_DELAY_MS)
        assertEquals(2800L, XXLAmdFxTriggerModel.FREEZE_MS)
        assertEquals(5000L, XXLAmdFxTriggerModel.ANR_LIMIT_MS)
        assertTrue(
            "2800 мс обязано быть строго меньше ANR-порога 5000 мс",
            XXLAmdFxTriggerModel.FREEZE_MS < XXLAmdFxTriggerModel.ANR_LIMIT_MS
        )
    }

    @Test
    fun `firing decision carries the exact timing contract`() {
        val model = XXLAmdFxTriggerModel(seed = 42L, everyNthAction = 45)
        var fired: XXLAmdFxTriggerModel.Decision? = null
        for (i in 1..200) {
            val decision = model.onAction()
            if (decision.fire) {
                fired = decision
                break
            }
        }
        val actual = fired ?: fail("за 200 действий ни одного замерза так и не случилось")
        assertEquals(XXLAmdFxTriggerModel.SHOW_DELAY_MS, actual.showDelayMs)
        assertEquals(XXLAmdFxTriggerModel.FREEZE_MS, actual.freezeMs)
    }

    @Test
    fun `first freeze happens in the seeded window and gaps stay in range`() {
        val model = XXLAmdFxTriggerModel(seed = 1337L, everyNthAction = 45)
        val fireIndices = ArrayList<Int>()
        for (i in 1..400) {
            if (model.onAction().fire) fireIndices.add(i)
        }
        assertTrue("слишком мало срабатываний: $fireIndices", fireIndices.size >= 5)
        val first = fireIndices[0]
        assertTrue("первый замерзать слишком рано: $first", first in 45..60)
        for (k in 1 until fireIndices.size) {
            val gap = fireIndices[k] - fireIndices[k - 1]
            assertTrue("неправильный интервал $gap (seed=1337)", gap in 45..60)
        }
    }

    @Test
    fun `same seed yields identical schedule, different seeds diverge`() {
        val scheduleOf = fun(seed: Long): List<Int> {
            val model = XXLAmdFxTriggerModel(seed, 45)
            val hits = ArrayList<Int>()
            for (i in 1..500) if (model.onAction().fire) hits.add(i)
            return hits
        }
        assertEquals(scheduleOf(7L), scheduleOf(7L))
        val distinct = (1L..20L).map { scheduleOf(it).first() }.toSet()
        assertTrue("seed вообще не влияет на расписание", distinct.size >= 2)
    }

    @Test
    fun `minimum gap between freezes is respected even with tiny period`() {
        val model = XXLAmdFxTriggerModel(seed = 5L, everyNthAction = 3)
        val hits = ArrayList<Int>()
        for (i in 1..1000) if (model.onAction().fire) hits.add(i)
        assertTrue("слишком редко: $hits", hits.size >= 10)
        for (k in 1 until hits.size) {
            assertTrue(
                "зазор ${hits[k] - hits[k - 1]} меньше MIN_GAP_ACTIONS",
                hits[k] - hits[k - 1] >= XXLAmdFxTriggerModel.MIN_GAP_ACTIONS
            )
        }
    }

    @Test
    fun `disabled model is completely silent`() {
        val model = XXLAmdFxTriggerModel(seed = 9L, everyNthAction = 5, enabled = false)
        for (i in 1..500) {
            assertFalse("выключенный замерзать не имеет права сработать", model.onAction().fire)
        }
        assertEquals(500, model.actionCount())
    }

    @Test
    fun `garbage period never fires and never throws`() {
        for (period in listOf(0, -1, -45, Int.MIN_VALUE)) {
            val model = XXLAmdFxTriggerModel(seed = 1L, everyNthAction = period)
            for (i in 1..100) assertFalse("период $period должен быть заглушён", model.onAction().fire)
        }
    }

    @Test
    fun `extreme seeds and long runs are safe`() {
        for (seed in listOf(Long.MAX_VALUE, Long.MIN_VALUE, -1L, 0L)) {
            val model = XXLAmdFxTriggerModel(seed, 45)
            var fires = 0
            for (i in 1..5_000) if (model.onAction().fire) fires++
            assertTrue("seed=$seed не сработал ни разу", fires > 0)
            assertTrue("actionCount уехал в минус", model.actionCount() >= 5_000L)
            model.reset()
            assertEquals(0L, model.actionCount())
        }
    }

    private fun fail(message: String): Nothing = throw AssertionError(message)
}
