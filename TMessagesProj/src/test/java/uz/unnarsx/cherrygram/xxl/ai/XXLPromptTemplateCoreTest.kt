/**
 * 🚀✨ Enterprise Quantum Unit Test for the Prompt Template Engine 💯
 * Verifies: подстановка ${var}, отсутствие висячих плейсхолдеров, temperatore-кап,
 * санитайз входа, режимы, null-safety.
 */
package uz.unnarsx.cherrygram.xxl.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.unnarsx.cherrygram.xxl.ai.core.XXLPromptTemplateCore

class XXLPromptTemplateCoreTest {

    @Test
    fun `modes are unique and every template declares its variables`() {
        val ids = XXLPromptTemplateCore.modes().map { it.id }
        assertEquals(ids.size.toLong(), ids.toSet().size.toLong())
        for (mode in XXLPromptTemplateCore.modes()) {
            assertTrue(mode.title.isNotBlank())
            assertTrue(mode.template.contains("\${text}"))
            assertTrue(XXLPromptTemplateCore.variablesOf(mode.template).contains("text"))
        }
    }

    @Test
    fun `mode resolver survives junk and aliases`() {
        assertEquals(XXLPromptTemplateCore.Mode.TRANSLATE, XXLPromptTemplateCore.modeFor("translate", XXLPromptTemplateCore.Mode.EXPLAIN))
        assertEquals(XXLPromptTemplateCore.Mode.TRANSLATE, XXLPromptTemplateCore.modeFor("  TRANSLATE  ", XXLPromptTemplateCore.Mode.EXPLAIN))
        assertEquals(XXLPromptTemplateCore.Mode.RAT_MODE, XXLPromptTemplateCore.modeFor("ratmode", XXLPromptTemplateCore.Mode.TRANSLATE))
        assertEquals(XXLPromptTemplateCore.Mode.EXPLAIN, XXLPromptTemplateCore.modeFor(null, XXLPromptTemplateCore.Mode.EXPLAIN))
        assertEquals(XXLPromptTemplateCore.Mode.EXPLAIN, XXLPromptTemplateCore.modeFor("   ", XXLPromptTemplateCore.Mode.EXPLAIN))
        assertEquals(XXLPromptTemplateCore.Mode.EXPLAIN, XXLPromptTemplateCore.modeFor("переведи меня полностью", XXLPromptTemplateCore.Mode.EXPLAIN))
    }

    @Test
    fun `render substitutes known vars and blanks unknown ones`() {
        val rendered = XXLPromptTemplateCore.render(
            "Переведи на \${target}: \${text} (!\${missing})",
            mapOf("target" to "ru", "text" to "привет крыса")
        )
        assertEquals("Переведи на ru: привет крыса (!)", rendered)
        assertFalse(rendered, rendered.contains("\${"))
        assertEquals("", XXLPromptTemplateCore.render(null, null))
        assertEquals("", XXLPromptTemplateCore.render("", emptyMap()))
        assertEquals("без переменных", XXLPromptTemplateCore.render("без переменных", null))
    }

    @Test
    fun `render never dies on recursive or hostile values`() {
        val evil = "\${text}\${text}"
        val rendered = XXLPromptTemplateCore.render("шаблон: \${value}", mapOf("value" to evil))
        assertTrue(rendered, rendered.contains(evil))
        assertFalse("подстановка не рекурсивна", rendered.contains("\${value}"))
        assertEquals(
            "x".repeat(XXLPromptTemplateCore.MAX_PROMPT_LENGTH + 500).take(XXLPromptTemplateCore.MAX_PROMPT_LENGTH).length.toLong(),
            XXLPromptTemplateCore.render("x".repeat(XXLPromptTemplateCore.MAX_PROMPT_LENGTH + 500), null).length.toLong()
        )
    }

    @Test
    fun `variables are extracted in order without duplicates`() {
        assertEquals(listOf("a", "b"), XXLPromptTemplateCore.variablesOf("\${a} и \${b} и снова \${a}"))
        assertTrue(XXLPromptTemplateCore.variablesOf(null).isEmpty())
        assertTrue(XXLPromptTemplateCore.variablesOf("пусто без скобок").isEmpty())
        assertTrue(XXLPromptTemplateCore.variablesOf("\${}").isEmpty())
    }

    @Test
    fun `system prompt always has content and stays under the cap`() {
        val prompt = XXLPromptTemplateCore.buildSystemPrompt(
            XXLPromptTemplateCore.Mode.TRANSLATE,
            mapOf("target" to "en"),
            "оригинальный текст"
        )
        assertTrue(prompt.contains("оригинальный текст"))
        assertTrue(prompt.length <= XXLPromptTemplateCore.MAX_PROMPT_LENGTH)
        assertTrue(prompt.isNotBlank())

        val empty = XXLPromptTemplateCore.buildSystemPrompt(null, null, null)
        assertTrue(empty, empty.contains("КРЫСА"))
        assertTrue(empty.length <= XXLPromptTemplateCore.MAX_PROMPT_LENGTH)

        val huge = XXLPromptTemplateCore.buildSystemPrompt(
            XXLPromptTemplateCore.Mode.SUMMARIZE,
            mapOf("sentences" to "2"),
            "ы".repeat(XXLPromptTemplateCore.MAX_PROMPT_LENGTH * 2)
        )
        assertTrue(huge.length <= XXLPromptTemplateCore.MAX_PROMPT_LENGTH)
    }

    @Test
    fun `temperature is clamped to a sane physical range`() {
        assertEquals(XXLPromptTemplateCore.DEFAULT_TEMPERATURE_TENTHS.toLong(), XXLPromptTemplateCore.clampTemperatureTenths(null).toLong())
        assertEquals(0, XXLPromptTemplateCore.clampTemperatureTenths(-5).toLong())
        assertEquals(XXLPromptTemplateCore.MAX_TEMPERATURE_TENTHS.toLong(), XXLPromptTemplateCore.clampTemperatureTenths(999).toLong())
        assertEquals(0.5, XXLPromptTemplateCore.temperature(null), 0.0001)
        assertEquals(2.0, XXLPromptTemplateCore.temperature(25), 0.0001)
        assertEquals(0.0, XXLPromptTemplateCore.temperature(-1), 0.0001)
        assertEquals(1.3, XXLPromptTemplateCore.temperature(13), 0.0001)
    }

    @Test
    fun `temperature parser accepts ints decimals and junk fallback`() {
        assertEquals(13, XXLPromptTemplateCore.parseTemperature("13", 5).toLong())
        assertEquals(12, XXLPromptTemplateCore.parseTemperature("1.2", 5).toLong())
        assertEquals(5, XXLPromptTemplateCore.parseTemperature(null, 5).toLong())
        assertEquals(5, XXLPromptTemplateCore.parseTemperature("горячо", 5).toLong())
        assertEquals(20, XXLPromptTemplateCore.parseTemperature("9.99", 5).toLong())
        assertEquals(0, XXLPromptTemplateCore.parseTemperature("-2", 5).toLong())
    }

    @Test
    fun `sanitizer kills control junk and collapses empty lines`() {
        assertEquals("ab", XXLPromptTemplateCore.sanitizeInput("a\u0000b", 100))
        assertEquals("a\nb\n\nc", XXLPromptTemplateCore.sanitizeInput("a\u0000\nb\r\n\nc", 100))
        assertEquals("a\n\nb", XXLPromptTemplateCore.sanitizeInput("a\n\n\n\n\nb", 100))
        assertEquals("x".repeat(10), XXLPromptTemplateCore.sanitizeInput("x".repeat(50), 10))
        assertEquals("", XXLPromptTemplateCore.sanitizeInput(null, 10))
        assertEquals("", XXLPromptTemplateCore.sanitizeInput("   \n  ", 10))
        assertEquals("tab\tkept", XXLPromptTemplateCore.sanitizeInput("tab\tkept", 100))
        assertEquals(0, XXLPromptTemplateCore.sanitizeInput("abc", 0).length.toLong())
    }

    @Test
    fun `describe is human and mode-agnostic`() {
        val line = XXLPromptTemplateCore.describe(XXLPromptTemplateCore.Mode.FACT_CHECK, 13)
        assertTrue(line, line.contains(XXLPromptTemplateCore.Mode.FACT_CHECK.title))
        assertTrue(line, line.contains("1.3"))
        assertTrue(XXLPromptTemplateCore.describe(null, null).contains("КРЫСА"))
    }
}
