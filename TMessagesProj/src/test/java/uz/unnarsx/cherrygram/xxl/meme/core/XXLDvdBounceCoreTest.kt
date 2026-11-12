package uz.unnarsx.cherrygram.xxl.meme.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Коммит 14: математика DVD-отскока, детектор угла и цикл картинок — чистые, без Android.
 */
class XXLDvdBounceCoreTest {

    @Test
    fun `свободный полёт не меняет скорость`() {
        val s = XXLDvdBounceCore.State(100f, 100f, 3f, 2f)
        val r = XXLDvdBounceCore.step(s, 1000f, 1000f, 90f)
        assertEquals(103f, r.state.x, 0.001f)
        assertEquals(102f, r.state.y, 0.001f)
        assertEquals(3f, r.state.vx, 0.001f)
        assertFalse(r.hitWall)
        assertFalse(r.hitCorner)
    }

    @Test
    fun `отскок от правой стены разворачивает vx`() {
        val s = XXLDvdBounceCore.State(950f, 100f, 3f, 2f)
        val r = XXLDvdBounceCore.step(s, 1000f, 1000f, 90f)
        assertTrue(r.hitWall)
        assertTrue(r.state.vx < 0)
        assertTrue(r.state.x <= 1000f - 90f)
    }

    @Test
    fun `отскок от нижней стены разворачивает vy`() {
        val s = XXLDvdBounceCore.State(100f, 950f, 3f, 2f)
        val r = XXLDvdBounceCore.step(s, 1000f, 1000f, 90f)
        assertTrue(r.hitWall)
        assertTrue(r.state.vy < 0)
    }

    @Test
    fun `попадание в угол детектится когда обе оси на пределе`() {
        val s = XXLDvdBounceCore.State(950f, 950f, 3f, 3f)
        val r = XXLDvdBounceCore.step(s, 1000f, 1000f, 90f)
        assertTrue(r.hitCorner)
        assertTrue(r.state.vx < 0)
        assertTrue(r.state.vy < 0)
    }

    @Test
    fun `цикл картинок arslan svo amdfx по кругу`() {
        assertEquals(0, XXLDvdBounceCore.imageIndexFor(0))
        assertEquals(1, XXLDvdBounceCore.imageIndexFor(1))
        assertEquals(2, XXLDvdBounceCore.imageIndexFor(2))
        assertEquals(0, XXLDvdBounceCore.imageIndexFor(3))
        assertEquals(1, XXLDvdBounceCore.imageIndexFor(4))
    }

    @Test
    fun `нулевой размер экрана не крашит и не делит на ноль`() {
        val s = XXLDvdBounceCore.State(0f, 0f, 3f, 3f)
        val r = XXLDvdBounceCore.step(s, 0f, 0f, 90f)
        assertFalse(r.hitCorner)
    }
}
