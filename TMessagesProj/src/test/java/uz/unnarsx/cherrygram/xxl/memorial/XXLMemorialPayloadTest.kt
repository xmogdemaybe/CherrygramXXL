/**
 * 🚀✨ Enterprise Quantum Unit Test for the memorial payload (quote + marquee + captions + acid palette) 💯
 * Все литералы зафиксированы в AGENTS.md §1 и §2C — тест защищает их от регрессии.
 */
package uz.unnarsx.cherrygram.xxl.memorial

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.unnarsx.cherrygram.xxl.memorial.core.XXLMemorialPayload
import uz.unnarsx.cherrygram.xxl.memorial.core.XXLVibePaintCore

class XXLMemorialPayloadTest {

    @Test
    fun `memorial quote matches the mandate line by line`() {
        val expected = listOf(
            "1. A lot of people hate Cherrygram for no reason",
            "2. They switch to some vibe-grams apps",
            "3. They ask vibegram devs to copy features from Cherrygram.",
            "Question: Why do you ask to copy 50% of Cherrygram's features to other shitforks if Cherrygram is a piece of shit?"
        )
        assertEquals(expected, XXLMemorialPayload.QUOTE_LINES)
        assertEquals(4, XXLMemorialPayload.QUOTE_LINES.size)
        assertEquals(expected.joinToString("\n"), XXLMemorialPayload.quoteText())
    }

    @Test
    fun `quote is never empty and never silently truncated`() {
        for ((index, line) in XXLMemorialPayload.QUOTE_LINES.withIndex()) {
            assertTrue("строка $index пуста", line.isNotBlank())
            assertFalse("строка $index обрезана табами", line.contains("\t"))
        }
        assertTrue(XXLMemorialPayload.quoteText().length > 150)
        assertTrue(XXLMemorialPayload.quoteText().contains("50%"))
    }

    @Test
    fun `marquee and button captions are exactly as ordered`() {
        assertEquals("⚠️ НАПОМИНАНИЕ ПОЧЕМУ ЭТОТ ФОРК СУЩЕСТВУЕТ ⚠️", XXLMemorialPayload.MARQUEE_TITLE)
        assertEquals("ПЕРЕЙТИ К БАЗЕ (ОРИГИНАЛ)", XXLMemorialPayload.BUTTON_JUMP_CAPTION)
        assertEquals("Закрыть", XXLMemorialPayload.BUTTON_CLOSE_CAPTION)
    }

    @Test
    fun `flashing background alternates between the two ordered colors`() {
        assertEquals("#FF0000", XXLVibePaintCore.hex(XXLMemorialPayload.FLASH_COLOR_RED))
        assertEquals("#FFFF00", XXLVibePaintCore.hex(XXLMemorialPayload.FLASH_COLOR_YELLOW))
        assertEquals(0xFFFF0000.toInt(), XXLMemorialPayload.FLASH_COLOR_RED)
        assertEquals(0xFFFFFF00.toInt(), XXLMemorialPayload.FLASH_COLOR_YELLOW)
        assertEquals(2, XXLMemorialPayload.flashCycle().size)
        assertEquals(XXLMemorialPayload.FLASH_COLOR_RED, XXLMemorialPayload.flashCycle()[0])
        assertEquals(XXLMemorialPayload.FLASH_COLOR_YELLOW, XXLMemorialPayload.flashCycle()[1])
    }

    @Test
    fun `acid palette and chaotic geometry follow the house style`() {
        assertEquals("#FF00FF", XXLVibePaintCore.hex(XXLVibePaintCore.ACID_PINK))
        assertEquals("#00FF00", XXLVibePaintCore.hex(XXLVibePaintCore.ACID_GREEN))
        assertEquals("#00FFFF", XXLVibePaintCore.hex(XXLVibePaintCore.ACID_CYAN))
        assertEquals("#FF00FF", XXLVibePaintCore.hex(XXLVibePaintCore.NEON_MAGENTA))
        assertEquals(7, XXLVibePaintCore.DASHED_BORDER_WIDTH_DP)
        assertEquals(listOf(13, 7, 23, 31), XXLVibePaintCore.PRIME_PADDINGS_DP)
        assertEquals(-2f, XXLVibePaintCore.ROTATION_AMPLITUDE, 0f)
        assertEquals(2f, XXLVibePaintCore.ROTATION_AMPLITUDE_MINUS, 0f)
        assertEquals("serif", XXLVibePaintCore.TYPEFACES[0])
        assertEquals("monospace", XXLVibePaintCore.TYPEFACES[1])
        assertEquals("cursive", XXLVibePaintCore.TYPEFACES[2])
    }

    @Test
    fun `hex formatting keeps alpha out and pads single digits`() {
        assertEquals("#000000", XXLVibePaintCore.hex(0xFF000000.toInt()))
        assertEquals("#000001", XXLVibePaintCore.hex(0xFF000001.toInt()))
        assertEquals("#0A141E", XXLVibePaintCore.hex(0xFF0A141E.toInt()))
        // мусор/отрицательный input не должен давать исключений
        assertEquals("#FFFFFF", XXLVibePaintCore.hex(0x00FFFFFF))
    }

    @Test
    fun `padding lookup is index-safe for absurd indices`() {
        assertEquals(13, XXLVibePaintCore.paddingDp(0))
        assertEquals(31, XXLVibePaintCore.paddingDp(3))
        assertEquals(13, XXLVibePaintCore.paddingDp(4))                          // wrap
        assertEquals(23, XXLVibePaintCore.paddingDp(-2))                         // отрицательный -> безопасный wrap
        assertEquals(31, XXLVibePaintCore.paddingDp(Int.MAX_VALUE))              // MAX % 4 == 3
        assertEquals(13, XXLVibePaintCore.paddingDp(Int.MIN_VALUE))              // MIN делится на 4 -> 0
    }
}
