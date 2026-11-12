/**
 * 🚀✨ Enterprise Quantum Unit Test for Local Premium & Raised Limits (§2A) 💯
 * Verifies: perk-матрица, факторы, overflow-safe кап лимитов, hex-палитра, сериализация.
 */
package uz.unnarsx.cherrygram.xxl.premium

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.unnarsx.cherrygram.xxl.premium.core.XXLVibePremiumCore

class XXLVibePremiumCoreTest {

    @Test
    fun `every perk is permitted under the master switch`() {
        val all: Set<XXLVibePremiumCore.Perk> = XXLVibePremiumCore.Perk.values().toSet()
        for (perk in all) {
            assertTrue(perk.name, XXLVibePremiumCore.isPermitted(perk, all, true))
            assertFalse(perk.name, XXLVibePremiumCore.isPermitted(perk, all, false))
        }
        assertFalse(XXLVibePremiumCore.isPermitted(null, all, true))
        assertFalse(XXLVibePremiumCore.isPermitted(XXLVibePremiumCore.Perk.PREMIUM_MARK, emptySet(), true))
    }

    @Test
    fun `perk parser is tolerant deduplicated and enum-ordered`() {
        assertEquals(XXLVibePremiumCore.Perk.values().size.toLong(), XXLVibePremiumCore.enabledPerks(null).size.toLong())
        assertEquals(XXLVibePremiumCore.Perk.values().size.toLong(), XXLVibePremiumCore.enabledPerks("   ").size.toLong())
        val subset = XXLVibePremiumCore.enabledPerks("premium_mark; gif-Limit, бред")
        assertEquals(2, subset.size.toLong())
        assertTrue(subset.contains(XXLVibePremiumCore.Perk.PREMIUM_MARK))
        assertTrue(subset.contains(XXLVibePremiumCore.Perk.GIF_LIMIT))
        assertTrue(XXLVibePremiumCore.enabledPerks("никакой крысы").isEmpty())
        assertEquals(subset, XXLVibePremiumCore.enabledPerks(XXLVibePremiumCore.serialize(subset)))
    }

    @Test
    fun `boost multiplies by tenths and never overflows`() {
        assertEquals(600, XXLVibePremiumCore.boosted(400, 15, 1440).toLong())
        assertEquals(800, XXLVibePremiumCore.boosted(400, 20, 1440).toLong())
        assertEquals(1440, XXLVibePremiumCore.boosted(1000, 20, 1440).toLong())
        assertEquals(400, XXLVibePremiumCore.boosted(400, 5, 1440).toLong())
        assertEquals(0, XXLVibePremiumCore.boosted(0, 20, 1440).toLong())
        assertEquals(-5, XXLVibePremiumCore.boosted(-5, 20, 1440).toLong())
        assertEquals(1440, XXLVibePremiumCore.boosted(Int.MAX_VALUE, 20, 1440).toLong())
        assertEquals(Int.MAX_VALUE.toLong(), XXLVibePremiumCore.boosted(Int.MAX_VALUE, 10, Int.MAX_VALUE).toLong())
    }

    @Test
    fun `limit table respects hard caps and the disabled switch`() {
        val gifs = XXLVibePremiumCore.LimitKey.GIFS
        assertEquals((gifs.serverDefault * 15 / 10).toLong(), XXLVibePremiumCore.limitFor(gifs, gifs.serverDefault, 15, true).toLong())
        assertEquals(gifs.hardCap.toLong(), XXLVibePremiumCore.limitFor(gifs, Int.MAX_VALUE, 20, true).toLong())
        assertEquals(
            "выключенный перк не трогает сток",
            gifs.serverDefault.toLong(),
            XXLVibePremiumCore.limitFor(gifs, gifs.serverDefault, 20, false).toLong()
        )
        assertEquals(42L, XXLVibePremiumCore.limitFor(null, 42, 20, true).toLong())
        for (key in XXLVibePremiumCore.LimitKey.values()) {
            assertTrue(key.name, key.hardCap >= key.serverDefault)
            assertTrue(key.name, key.serverDefault > 0)
            assertTrue(XXLVibePremiumCore.limitFor(key, key.serverDefault, 20, true) >= key.serverDefault)
        }
    }

    @Test
    fun `factor parser accepts tenths decimals and junk`() {
        assertEquals(15, XXLVibePremiumCore.parseFactor("15", 10).toLong())
        assertEquals(15, XXLVibePremiumCore.parseFactor("1.5", 10).toLong())
        assertEquals(10, XXLVibePremiumCore.parseFactor(null, 10).toLong())
        assertEquals(10, XXLVibePremiumCore.parseFactor("горячая крыса", 10).toLong())
        assertEquals(XXLVibePremiumCore.MIN_FACTOR_TENTHS.toLong(), XXLVibePremiumCore.parseFactor("1", 20).toLong())
        assertEquals(XXLVibePremiumCore.MAX_FACTOR_TENTHS.toLong(), XXLVibePremiumCore.parseFactor("99", 10).toLong())
        assertEquals(XXLVibePremiumCore.DEFAULT_FACTOR_TENTHS.toLong(), XXLVibePremiumCore.parseFactor("", XXLVibePremiumCore.DEFAULT_FACTOR_TENTHS).toLong())
    }

    @Test
    fun `hex color conversions are strict and safe`() {
        assertEquals(0xFFFF00FF.toInt(), XXLVibePremiumCore.colorFromHex("#FF00FF", 0))
        assertEquals(0xFFFF00FF.toInt(), XXLVibePremiumCore.colorFromHex("ff00ff", 0))
        assertEquals(0xFFFFFFFF.toInt(), XXLVibePremiumCore.colorFromHex("#FFF", 0))
        assertEquals(0x80FF00FF.toInt(), XXLVibePremiumCore.colorFromHex("#ff00ff", 0, 128))
        assertEquals(0xFF123456.toInt(), XXLVibePremiumCore.colorFromHex("#123456", 0, 300))
        assertEquals(69, XXLVibePremiumCore.colorFromHex(null, 69).toLong())
        assertEquals(69, XXLVibePremiumCore.colorFromHex("#GGHHII", 69).toLong())
        assertEquals(69, XXLVibePremiumCore.colorFromHex("#12", 69).toLong())
        assertEquals(69, XXLVibePremiumCore.colorFromHex("", 69).toLong())
    }

    @Test
    fun `hex renderer is uppercase six digits and inverse consistent`() {
        assertEquals("#FF00FF", XXLVibePremiumCore.hexOf(0xFFFF00FF.toInt()))
        assertEquals("#000000", XXLVibePremiumCore.hexOf(0xFF000000.toInt()))
        assertEquals("#000000", XXLVibePremiumCore.hexOf(0))
        for (color in intArrayOf(0xFF00FF, 0x00FF00, 0x123456, 0xABCDEF)) {
            val rendered: String = XXLVibePremiumCore.hexOf(color)
            assertEquals("6 символов: $rendered", 7, rendered.length.toLong())
            assertEquals(
                rendered,
                (color and 0xFFFFFF).toLong(),
                (XXLVibePremiumCore.colorFromHex(rendered, 0) and 0xFFFFFF).toLong()
            )
        }
    }

    @Test
    fun `describe and serialize never leak secrets or crash on nulls`() {
        val line = XXLVibePremiumCore.describe(
            XXLVibePremiumCore.Perk.GIF_LIMIT,
            XXLVibePremiumCore.LimitKey.GIFS,
            XXLVibePremiumCore.LimitKey.GIFS.serverDefault,
            15,
            true
        )
        assertTrue(line, line.contains(XXLVibePremiumCore.Perk.GIF_LIMIT.title))
        assertTrue(line, line.contains(XXLVibePremiumCore.LimitKey.GIFS.hardCap.toString()))
        assertTrue(XXLVibePremiumCore.describe(null, null, 0, null, false).contains("КРЫСА"))
        assertEquals("", XXLVibePremiumCore.serialize(emptySet()))
        assertTrue(XXLVibePremiumCore.enabledPerks(XXLVibePremiumCore.serialize(null)).isEmpty())
    }
}
