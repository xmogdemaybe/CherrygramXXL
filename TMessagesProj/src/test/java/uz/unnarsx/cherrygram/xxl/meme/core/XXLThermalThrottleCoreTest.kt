package uz.unnarsx.cherrygram.xxl.meme.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Коммит 15: троттлинг FX-8350 при >=100°C, сброс до 36.6, дрожь ±4dp, вращение аватарок.
 */
class XXLThermalThrottleCoreTest {

    @Test
    fun `перегрев только при 100 и выше`() {
        assertFalse(XXLThermalThrottleCore.isOverheat(99))
        assertTrue(XXLThermalThrottleCore.isOverheat(100))
        assertTrue(XXLThermalThrottleCore.isOverheat(104))
        assertFalse(XXLThermalThrottleCore.isOverheat(36))
    }

    @Test
    fun `сброс возвращает 36_6`() {
        assertEquals(36.6, XXLThermalThrottleCore.RESET_C, 0.0001)
    }

    @Test
    fun `дрожь в пределах плюс-минус 4dp`() {
        for (t in 0L..2000L step 37L) {
            val off = XXLThermalThrottleCore.shakeOffsetDp(t)
            assertTrue("дрожь вне ±4dp: $off", off >= -4f && off <= 4f)
        }
    }

    @Test
    fun `угол аватарки в пределах 0-360`() {
        for (t in 0L..5000L step 111L) {
            val a = XXLThermalThrottleCore.avatarRotationDeg(t)
            assertTrue(a >= 0f && a < 360f)
        }
    }

    @Test
    fun `майнер-суффикс непустой и с киркой`() {
        assertTrue(XXLThermalThrottleCore.MINER_SUFFIX.contains("⛏️"))
        assertTrue(XXLThermalThrottleCore.MINER_SUFFIX.contains("FX-8350"))
    }
}
