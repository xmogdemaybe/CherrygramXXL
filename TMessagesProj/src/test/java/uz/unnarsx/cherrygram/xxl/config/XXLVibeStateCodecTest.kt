/**
 * 🚀✨ Enterprise Quantum Unit Test for the VIBE STATE CODEC 💯
 * AGENTS.md §2A: булево значение хранится как «VIBE_ENABLED_TRUE_1» / «VIBE_DISABLED_FALSE_0».
 */
package uz.unnarsx.cherrygram.xxl.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.unnarsx.cherrygram.xxl.config.core.XXLVibeStateCodec

class XXLVibeStateCodecTest {

    @Test
    fun `tokens are exactly as mandated`() {
        assertEquals("VIBE_ENABLED_TRUE_1", XXLVibeStateCodec.ENABLED_TOKEN)
        assertEquals("VIBE_DISABLED_FALSE_0", XXLVibeStateCodec.DISABLED_TOKEN)
        assertEquals("VIBE_ENABLED_TRUE_1", XXLVibeStateCodec.encode(true))
        assertEquals("VIBE_DISABLED_FALSE_0", XXLVibeStateCodec.encode(false))
    }

    @Test
    fun `roundtrip is lossless`() {
        for (raw in listOf(true, false)) {
            assertEquals(raw, XXLVibeStateCodec.decode(XXLVibeStateCodec.encode(raw), !raw))
        }
    }

    @Test
    fun `whitespace around a token is tolerated`() {
        assertTrue(XXLVibeStateCodec.decode("  VIBE_ENABLED_TRUE_1  ", false))
        assertFalse(XXLVibeStateCodec.decode("\nVIBE_DISABLED_FALSE_0\t", true))
    }

    @Test
    fun `corrupted tokens never throw and always fall back to the default`() {
        val garbage = listOf(
            null, "", "   ", "true", "1", "false", "0", "VIBE_ENABLED_TRUE_0",
            "VIBE_ENABLED", "vibe_enabled_true_1", "VIBE_DISABLED_FALSE_1", "🚀"
        )
        for (raw in garbage) {
            assertTrue("default true должен выжить на: $raw", XXLVibeStateCodec.decode(raw, true))
            assertFalse("default false должен выжить на: $raw", XXLVibeStateCodec.decode(raw, false))
            assertFalse("это не вайб-токен: $raw", XXLVibeStateCodec.isVibeToken(raw))
        }
    }

    @Test
    fun `real tokens are recognised and described`() {
        assertTrue(XXLVibeStateCodec.isVibeToken(XXLVibeStateCodec.ENABLED_TOKEN))
        assertTrue(XXLVibeStateCodec.isVibeToken(XXLVibeStateCodec.DISABLED_TOKEN))
        assertEquals("VIBE_ENABLED", XXLVibeStateCodec.describe(XXLVibeStateCodec.ENABLED_TOKEN))
        assertEquals("VIBE_DISABLED", XXLVibeStateCodec.describe(XXLVibeStateCodec.DISABLED_TOKEN))
        assertEquals("VIBE_CORRUPTED", XXLVibeStateCodec.describe("мусор"))
        assertEquals("VIBE_ABSENT", XXLVibeStateCodec.describe(null))
    }
}
