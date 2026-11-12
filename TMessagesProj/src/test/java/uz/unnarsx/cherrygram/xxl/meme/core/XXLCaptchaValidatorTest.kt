package uz.unnarsx.cherrygram.xxl.meme.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Коммит 16: капча из двух этапов, точная фраза, сброс при ошибке, буст ×2 на 10 с.
 */
class XXLCaptchaValidatorTest {

    @Test
    fun `канонический пример считается верно`() {
        val c = XXLCaptchaValidator.newChallenge(0)
        assertEquals("(1488 * 3) - (228 + 1337) + 69 = ?", c.expression)
        assertEquals((1488 * 3) - (228 + 1337) + 69, c.answer)
        assertTrue(XXLCaptchaValidator.checkMath(c, c.answer))
        assertFalse(XXLCaptchaValidator.checkMath(c, c.answer + 1))
    }

    @Test
    fun `фраза только точная, без опечаток и trim`() {
        assertTrue(XXLCaptchaValidator.checkPhrase(XXLCaptchaValidator.PHRASE))
        assertFalse(XXLCaptchaValidator.checkPhrase(XXLCaptchaValidator.PHRASE + " "))
        assertFalse(XXLCaptchaValidator.checkPhrase("я признаю величие cherrygramxxl"))
        assertFalse(XXLCaptchaValidator.checkPhrase(null))
        assertFalse(XXLCaptchaValidator.checkPhrase(""))
    }

    @Test
    fun `только оба этапа открывают выключение`() {
        val s = XXLCaptchaValidator.Session(0)
        assertFalse(s.fullyPassed)
        assertFalse("фраза до математики не принимается", s.submitPhrase(XXLCaptchaValidator.PHRASE))
        assertTrue(s.submitMath(s.challenge.answer))
        assertFalse(s.fullyPassed)
        assertTrue(s.submitPhrase(XXLCaptchaValidator.PHRASE))
        assertTrue(s.fullyPassed)
    }

    @Test
    fun `ошибка математики сбрасывает и фразу`() {
        val s = XXLCaptchaValidator.Session(0)
        assertTrue(s.submitMath(s.challenge.answer))
        assertFalse(s.submitMath(s.challenge.answer + 5))
        assertFalse(s.mathPassed)
        assertFalse(s.submitPhrase(XXLCaptchaValidator.PHRASE))
    }

    @Test
    fun `reset выдаёт новую капчу и сбрасывает этапы`() {
        val s = XXLCaptchaValidator.Session(0)
        s.submitMath(s.challenge.answer)
        s.reset(1)
        assertFalse(s.mathPassed)
        assertFalse(s.phrasePassed)
        assertEquals(XXLCaptchaValidator.newChallenge(1).expression, s.challenge.expression)
    }

    @Test
    fun `буст ускоряет вращение вдвое на 10 секунд`() {
        XXLRotatingTextCore.resetBoost()
        val now = 1_000_000L
        val normal = XXLRotatingTextCore.angleFor(4000L, now)
        XXLRotatingTextCore.boost(now)
        assertTrue(XXLRotatingTextCore.isBoosted(now))
        val boosted = XXLRotatingTextCore.angleFor(4000L, now)
        assertEquals(normal * 2 % 360, boosted, 0.001f)
        assertFalse("после 10 с буст кончился", XXLRotatingTextCore.isBoosted(now + 10_000L))
        XXLRotatingTextCore.resetBoost()
    }

    @Test
    fun `угол всегда в 0-360`() {
        for (t in 0L..10000L step 173L) {
            val a = XXLRotatingTextCore.angleFor(t, t)
            assertTrue(a >= 0f && a < 360f)
        }
    }
}
