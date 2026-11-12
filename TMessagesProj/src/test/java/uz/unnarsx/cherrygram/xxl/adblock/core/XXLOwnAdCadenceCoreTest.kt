package uz.unnarsx.cherrygram.xxl.adblock.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Коммит 11: svo.jpg показывается не «когда сервер пришлёт sponsored», а по детерминированной
 * каденции — каждое 3-е открытие/скролл. Логика чистая, без Android.
 */
class XXLOwnAdCadenceCoreTest {

    @Test
    fun `каждый третий тик показывает баннер`() {
        assertFalse(XXLOwnAdCadenceCore.shouldShow(1))
        assertFalse(XXLOwnAdCadenceCore.shouldShow(2))
        assertTrue(XXLOwnAdCadenceCore.shouldShow(3))
        assertFalse(XXLOwnAdCadenceCore.shouldShow(4))
        assertTrue(XXLOwnAdCadenceCore.shouldShow(6))
        assertTrue(XXLOwnAdCadenceCore.shouldShow(9))
    }

    @Test
    fun `нулевой и отрицательный тик не показывают`() {
        assertFalse(XXLOwnAdCadenceCore.shouldShow(0))
        assertFalse(XXLOwnAdCadenceCore.shouldShow(-3))
    }

    @Test
    fun `счётчик тикает и срабатывает на каждом третьем`() {
        val counter = XXLOwnAdCadenceCore.CadenceCounter()
        val hits = ArrayList<Int>()
        for (i in 1..9) {
            if (counter.tick()) hits.add(i)
        }
        assertEquals(listOf(3, 6, 9), hits)
    }

    @Test
    fun `сброс счётчика начинает каденцию заново`() {
        val counter = XXLOwnAdCadenceCore.CadenceCounter()
        counter.tick(); counter.tick()
        counter.reset()
        assertFalse(counter.tick())
        assertFalse(counter.tick())
        assertTrue(counter.tick())
    }
}
