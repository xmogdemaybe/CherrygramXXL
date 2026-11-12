/**
 * 🚀✨ Enterprise Quantum Unit Test for the AI Provider Registry (§2A: свои ключи, чужие сервисы) 💯
 * Verifies: resolution по id/алиасам, пригодность ключа, редакция, endpoint-шаблоны, батчи, языки.
 */
package uz.unnarsx.cherrygram.xxl.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.unnarsx.cherrygram.xxl.ai.core.XXLProviderRegistryCore

class XXLProviderRegistryCoreTest {

    @Test
    fun `registry lists every provider exactly once`() {
        val ids = XXLProviderRegistryCore.all().map { it.id }
        assertEquals(ids.size.toLong(), ids.toSet().size.toLong())
        assertTrue(ids.contains(XXLProviderRegistryCore.GEMINI.id))
        assertTrue(XXLProviderRegistryCore.all().any { !it.requiresKey })
    }

    @Test
    fun `resolver is case-insensitive and alias-aware`() {
        assertEquals(XXLProviderRegistryCore.GEMINI, XXLProviderRegistryCore.resolve("gemini", XXLProviderRegistryCore.TELEGRAM_NATIVE))
        assertEquals(XXLProviderRegistryCore.GEMINI, XXLProviderRegistryCore.resolve("  GoogLe-GEMINI  ", XXLProviderRegistryCore.TELEGRAM_NATIVE))
        assertEquals(XXLProviderRegistryCore.LIBRE_TRANSLATE, XXLProviderRegistryCore.resolve("libretranslate", XXLProviderRegistryCore.GEMINI))
        assertEquals(XXLProviderRegistryCore.DEEPL, XXLProviderRegistryCore.resolve("deepl_free", XXLProviderRegistryCore.GEMINI))
        assertEquals(XXLProviderRegistryCore.OFFLINE_RAT, XXLProviderRegistryCore.resolve("крыса", XXLProviderRegistryCore.GEMINI))
    }

    @Test
    fun `junk selection falls back without exploding`() {
        for (raw in listOf(null, "", "   ", "gemini; rm -rf /", "12345", "провайдер")) {
            assertEquals("мусор: $raw", XXLProviderRegistryCore.TELEGRAM_NATIVE, XXLProviderRegistryCore.resolve(raw, XXLProviderRegistryCore.TELEGRAM_NATIVE))
        }
    }

    @Test
    fun `key normalizer kills bearer prefixes quotes and newlines`() {
        assertEquals("AIzaSyTestKey123", XXLProviderRegistryCore.normalizeKey("  AIzaSyTestKey123  "))
        assertEquals("AIzaSyTestKey123", XXLProviderRegistryCore.normalizeKey("Bearer AIzaSyTestKey123"))
        assertEquals("AIzaSyTestKey123", XXLProviderRegistryCore.normalizeKey("\"AIzaSyTestKey123\""))
        assertEquals("AIzaSyTestKey123", XXLProviderRegistryCore.normalizeKey("AIzaSy\r\nTestKey123"))
        assertEquals("", XXLProviderRegistryCore.normalizeKey(null))
        assertEquals("", XXLProviderRegistryCore.normalizeKey("   "))
    }

    @Test
    fun `usability depends on the provider policy`() {
        assertTrue(XXLProviderRegistryCore.isUsable(XXLProviderRegistryCore.GEMINI, "AIzaSyTestKey123", "gemini-pro"))
        assertFalse("ключ короче минимума", XXLProviderRegistryCore.isUsable(XXLProviderRegistryCore.GEMINI, "short", "gemini-pro"))
        assertTrue(
            "пустая модель допустима: GEMINI приносит свою по умолчанию",
            XXLProviderRegistryCore.isUsable(XXLProviderRegistryCore.GEMINI, "AIzaSyTestKey123", null)
        )
        assertFalse(
            "у CherryAI дефолтной модели нет — пользователь обязан назвать",
            XXLProviderRegistryCore.isUsable(XXLProviderRegistryCore.CHERRY_AI, "AIzaSyTestKey123", "  ")
        )
        assertFalse(XXLProviderRegistryCore.isUsable(XXLProviderRegistryCore.GEMINI, null, "model"))
        assertTrue("бесплатный провайдер не просит ключ", XXLProviderRegistryCore.isUsable(XXLProviderRegistryCore.LIBRE_TRANSLATE, "", null))
        assertTrue(XXLProviderRegistryCore.isUsable(XXLProviderRegistryCore.OFFLINE_RAT, null, null))
        assertFalse(XXLProviderRegistryCore.isUsable(null, "key", "model"))
    }

    @Test
    fun `own keys win over the fork keys and blanks mean absence`() {
        assertEquals("OwnKey1234567890", XXLProviderRegistryCore.effectiveKey("OwnKey1234567890", "ForkKey12345678", XXLProviderRegistryCore.GEMINI))
        assertEquals("ForkKey12345678", XXLProviderRegistryCore.effectiveKey(null, "ForkKey12345678", XXLProviderRegistryCore.GEMINI))
        assertEquals("ForkKey12345678", XXLProviderRegistryCore.effectiveKey("   ", "ForkKey12345678", XXLProviderRegistryCore.GEMINI))
        assertEquals("", XXLProviderRegistryCore.effectiveKey(null, null, XXLProviderRegistryCore.GEMINI))
        assertEquals("бесплатному крысе ключ не нужен", "", XXLProviderRegistryCore.effectiveKey("OwnKey1234567890", "ForkKey12345678", XXLProviderRegistryCore.OFFLINE_RAT))
    }

    @Test
    fun `redaction never leaks the whole secret`() {
        val key = "AIzaSyDANGERWillRobinson1234"
        val redacted: String = XXLProviderRegistryCore.redactKey(key)
        assertFalse(redacted, redacted.contains(key))
        assertTrue(redacted, redacted.contains(key.substring(key.length - 4)))
        assertTrue(redacted, redacted.length < key.length)
        assertEquals("", XXLProviderRegistryCore.redactKey(null))
        assertEquals("короткий", XXLProviderRegistryCore.redactKey("короткий"))
    }

    @Test
    fun `endpoint templates are sanitized and defaulted`() {
        assertEquals(XXLProviderRegistryCore.LIBRE_TRANSLATE.endpoint, XXLProviderRegistryCore.endpointFor(XXLProviderRegistryCore.LIBRE_TRANSLATE, null))
        assertEquals("http://localhost:5000", XXLProviderRegistryCore.endpointFor(XXLProviderRegistryCore.LIBRE_TRANSLATE, " http://localhost:5000/ "))
        assertNotEquals(XXLProviderRegistryCore.LIBRE_TRANSLATE.endpoint, XXLProviderRegistryCore.endpointFor(XXLProviderRegistryCore.LIBRE_TRANSLATE, "http://localhost:5000"))
        assertEquals(XXLProviderRegistryCore.OFFLINE_RAT.endpoint, XXLProviderRegistryCore.endpointFor(XXLProviderRegistryCore.OFFLINE_RAT, "javascript:alert(1)"))
        assertEquals("", XXLProviderRegistryCore.endpointFor(null, "http://example.com"))
    }

    @Test
    fun `batch size is clamped per provider`() {
        assertTrue(XXLProviderRegistryCore.batchFor(XXLProviderRegistryCore.GEMINI, Int.MAX_VALUE) <= XXLProviderRegistryCore.GEMINI.maxBatch)
        assertEquals(1, XXLProviderRegistryCore.batchFor(XXLProviderRegistryCore.GEMINI, 0).toLong())
        assertEquals(1, XXLProviderRegistryCore.batchFor(null, 42).toLong())
        assertTrue(XXLProviderRegistryCore.batchFor(XXLProviderRegistryCore.LIBRE_TRANSLATE, -7) >= 1)
    }

    @Test
    fun `language pairing understands app and auto`() {
        assertEquals("auto", XXLProviderRegistryCore.languageFrom("AUTO"))
        assertEquals("auto", XXLProviderRegistryCore.languageFrom(null))
        assertEquals("en", XXLProviderRegistryCore.languageFrom("  "))
        assertEquals("ru", XXLProviderRegistryCore.languageFrom("RU_rus".substring(0, 2)))
        assertEquals("ru", XXLProviderRegistryCore.languageTo("app", "ru").second)
        assertEquals("ru", XXLProviderRegistryCore.languageTo("RU", "en").second)
        assertEquals(XXLProviderRegistryCore.DEFAULT_LANGUAGE, XXLProviderRegistryCore.languageTo("%%%", "en").second)
        assertTrue(XXLProviderRegistryCore.isSupportedLanguage("pt-BR"))
        assertFalse(XXLProviderRegistryCore.isSupportedLanguage("слишком длинно и тепло"))
        assertFalse(XXLProviderRegistryCore.isSupportedLanguage(null))
    }

    @Test
    fun `describe is human and null safe`() {
        val line = XXLProviderRegistryCore.describe(XXLProviderRegistryCore.GEMINI, "AIzaSyTestKey12345", "gemini-pro")
        assertTrue(line, line.contains(XXLProviderRegistryCore.GEMINI.title))
        assertFalse("описания не должны светить ключ", line.contains("AIzaSyTestKey12345"))
        assertTrue(XXLProviderRegistryCore.describe(null, null, null).contains("КРЫСА"))
    }
}
