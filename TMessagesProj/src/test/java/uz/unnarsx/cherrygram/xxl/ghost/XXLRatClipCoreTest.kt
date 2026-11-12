/**
 * 🚀✨ Enterprise Quantum Unit Test for Rat-Mode Geometry (§2D) 💯
 */
package uz.unnarsx.cherrygram.xxl.ghost

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.unnarsx.cherrygram.xxl.ghost.core.XXLRatClipCore

class XXLRatClipCoreTest {

    @Test
    fun `mandated angles are literal`() {
        assertEquals(-1.5f, XXLRatClipCore.BUBBLE_ROTATION_DEGREES, 0f)
        assertEquals(-8f, XXLRatClipCore.WATERMARK_ROTATION_DEGREES, 0f)
        assertEquals("РЕЖИМ КРЫСЫ АКТИВЕН", XXLRatClipCore.WATERMARK_TEXT)
    }

    @Test
    fun `shapes parse from hostile raw strings`() {
        assertEquals(XXLRatClipCore.Shape.DIAMOND, XXLRatClipCore.Shape.fromRaw("diamond"))
        assertEquals(XXLRatClipCore.Shape.DIAMOND, XXLRatClipCore.Shape.fromRaw(" РОМБ "))
        assertEquals(XXLRatClipCore.Shape.TRIANGLE, XXLRatClipCore.Shape.fromRaw("triangle"))
        assertEquals(XXLRatClipCore.Shape.SQUARE, XXLRatClipCore.Shape.fromRaw(null))
        assertEquals(XXLRatClipCore.Shape.SQUARE, XXLRatClipCore.Shape.fromRaw(""))
        assertEquals(XXLRatClipCore.Shape.SQUARE, XXLRatClipCore.Shape.fromRaw("шестиугольник"))
    }

    @Test
    fun `vertex counts match the shapes`() {
        assertEquals(4, XXLRatClipCore.pointCount(XXLRatClipCore.Shape.DIAMOND))
        assertEquals(3, XXLRatClipCore.pointCount(XXLRatClipCore.Shape.TRIANGLE))
        assertEquals(4, XXLRatClipCore.pointCount(XXLRatClipCore.Shape.SQUARE))
        assertEquals(4, XXLRatClipCore.pointCount(null))
    }

    @Test
    fun `diamond points are the midpoints of the box`() {
        val vertices = XXLRatClipCore.verticesFor(XXLRatClipCore.Shape.DIAMOND, 100f, 50f)
        assertEquals(listOf(50f, 0f, 100f, 25f, 50f, 50f, 0f, 25f), vertices.toList())
    }

    @Test
    fun `degenerate and negative sizes clamp instead of exploding`() {
        for (shape in XXLRatClipCore.Shape.values()) {
            assertTrue(XXLRatClipCore.isWithinBounds(shape, 0f, 0f))
            assertTrue(XXLRatClipCore.isWithinBounds(shape, -40f, -70f))
            assertTrue(XXLRatClipCore.isWithinBounds(shape, Float.NaN, 10f))
            assertTrue(XXLRatClipCore.isWithinBounds(shape, 1f, Float.POSITIVE_INFINITY))
            assertTrue(XXLRatClipCore.isWithinBounds(null, 64f, 64f))
            for (vertex in XXLRatClipCore.verticesFor(shape, -5f, -5f)) {
                assertFalse("отрицательная вершина для $shape", vertex < 0f)
            }
        }
    }

    @Test
    fun `switch rotation stays inside the cursed amplitude`() {
        for (tick in -300..300) {
            val rotation = XXLRatClipCore.switchRotationFor(tick)
            assertTrue("rotation=$rotation на тике $tick", rotation >= -2f && rotation <= 2f)
        }
        assertEquals(-2f, XXLRatClipCore.switchRotationFor(0), 0f)
        assertTrue(XXLRatClipCore.switchRotationFor(Int.MIN_VALUE) in -2f..2f)
        assertTrue(XXLRatClipCore.switchRotationFor(Int.MAX_VALUE) in -2f..2f)
    }
}
