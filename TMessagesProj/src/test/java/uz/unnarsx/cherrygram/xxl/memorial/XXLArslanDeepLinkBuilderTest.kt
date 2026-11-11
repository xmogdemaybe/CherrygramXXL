/**
 * 🚀✨ Enterprise Quantum Unit Test for «ПЕРЕЙТИ К БАЗЕ (ОРИГИНАЛ)» 💯
 * Целевая ссылка зафиксирована в AGENTS.md §1: https://t.me/CherrygramBetaAPKs/2810
 */
package uz.unnarsx.cherrygram.xxl.memorial

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.unnarsx.cherrygram.xxl.memorial.core.XXLArslanDeepLinkBuilder

class XXLArslanDeepLinkBuilderTest {

    @Test
    fun `memorial link is byte-exact against the mandate`() {
        assertEquals("https://t.me/CherrygramBetaAPKs/2810", XXLArslanDeepLinkBuilder.buildMemorialLink())
        assertEquals("https://t.me/CherrygramBetaAPKs/2810", XXLArslanDeepLinkBuilder.fallbackLink())
        assertEquals(2810, XXLArslanDeepLinkBuilder.MEMORIAL_MESSAGE_ID)
    }

    @Test
    fun `builder normalizes slashes and never doubles them`() {
        assertEquals(
            "https://t.me/CherrygramBetaAPKs/2810",
            XXLArslanDeepLinkBuilder.build("https://t.me/CherrygramBetaAPKs/", 2810)
        )
        assertEquals(
            "https://t.me/CherrygramBetaAPKs/2810",
            XXLArslanDeepLinkBuilder.build("   https://t.me/CherrygramBetaAPKs   ", 2810)
        )
        assertEquals(
            "https://t.me/cherrygram/1",
            XXLArslanDeepLinkBuilder.build("https://t.me/cherrygram", 1)
        )
    }

    @Test
    fun `garbage input yields null instead of an exception`() {
        assertNull(XXLArslanDeepLinkBuilder.build(null, 2810))
        assertNull(XXLArslanDeepLinkBuilder.build("", 2810))
        assertNull(XXLArslanDeepLinkBuilder.build("      ", 2810))
        assertNull(XXLArslanDeepLinkBuilder.build("https://t.me/", 2810))
        assertNull(XXLArslanDeepLinkBuilder.build("https://example.com", 2810))
        assertNull(XXLArslanDeepLinkBuilder.build("https://t.me/CherrygramBetaAPKs", 0))
        assertNull(XXLArslanDeepLinkBuilder.build("https://t.me/CherrygramBetaAPKs", -2810))
    }

    @Test
    fun `parse extracts username and message id from the mandated link`() {
        val parsed = XXLArslanDeepLinkBuilder.parse(XXLArslanDeepLinkBuilder.buildMemorialLink())
        assertTrue(parsed.valid)
        assertEquals("CherrygramBetaAPKs", parsed.username)
        assertEquals(2810, parsed.messageId)
        assertEquals("https", parsed.scheme)
        assertEquals("t.me", parsed.host)
        assertNotNull(parsed.uriString)
    }

    @Test
    fun `parse is forgiving about scheme and whitespace but strict about the id`() {
        assertTrue(XXLArslanDeepLinkBuilder.parse("t.me/CherrygramBetaAPKs/2810").valid)
        assertTrue(XXLArslanDeepLinkBuilder.parse("  https://t.me/CherrygramBetaAPKs/2810/  ").valid)
        assertEquals(2810, XXLArslanDeepLinkBuilder.parse("https://t.me/CherrygramBetaAPKs/2810/").messageId)

        assertFalse(XXLArslanDeepLinkBuilder.parse(null).valid)
        assertFalse(XXLArslanDeepLinkBuilder.parse("").valid)
        assertFalse("нет id сообщения", XXLArslanDeepLinkBuilder.parse("https://t.me/cherrygram").valid)
        assertFalse("id не число", XXLArslanDeepLinkBuilder.parse("https://t.me/cherrygram/abc").valid)
        assertFalse("нулевой id", XXLArslanDeepLinkBuilder.parse("https://t.me/cherrygram/0").valid)
        assertFalse("отрицательный id", XXLArslanDeepLinkBuilder.parse("https://t.me/cherrygram/-5").valid)
        assertFalse("не t.me", XXLArslanDeepLinkBuilder.parse("https://example.com/cherrygram/5").valid)
    }

    @Test
    fun `big message ids survive parsing without overflow`() {
        val parsed = XXLArslanDeepLinkBuilder.parse("https://t.me/CherrygramBetaAPKs/21474836470")
        assertFalse("id не влезает в Int -> план не валиден", parsed.valid)
        assertEquals(0, parsed.messageId)

        val ok = XXLArslanDeepLinkBuilder.parse("https://t.me/CherrygramBetaAPKs/2000000000")
        assertTrue(ok.valid)
        assertEquals(2000000000, ok.messageId)
    }

    @Test
    fun `jump plan always resolves to a presentable tme url`() {
        val broken = XXLArslanDeepLinkBuilder.resolveOrFallback(null)
        assertEquals(XXLArslanDeepLinkBuilder.buildMemorialLink(), broken)
        assertEquals(
            XXLArslanDeepLinkBuilder.buildMemorialLink(),
            XXLArslanDeepLinkBuilder.resolveOrFallback("https://t.me/CherrygramBetaAPKs")
        )
        assertEquals(
            "https://t.me/cherrygram/77",
            XXLArslanDeepLinkBuilder.resolveOrFallback("https://t.me/cherrygram/77")
        )
    }
}
