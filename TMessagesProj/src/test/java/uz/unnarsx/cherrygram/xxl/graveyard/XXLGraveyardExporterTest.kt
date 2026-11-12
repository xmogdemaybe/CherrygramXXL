/**
 * 🚀✨ Enterprise Quantum Unit Test for the Graveyard exporter + rat badge payload (§2D) 💯
 */
package uz.unnarsx.cherrygram.xxl.graveyard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.unnarsx.cherrygram.xxl.graveyard.core.XXLGraveyardExporter
import uz.unnarsx.cherrygram.xxl.graveyard.core.XXLGraveyardIndex
import uz.unnarsx.cherrygram.xxl.graveyard.core.XXLRevision
import uz.unnarsx.cherrygram.xxl.graveyard.core.XXLRatBadgeCore
import uz.unnarsx.cherrygram.xxl.graveyard.core.XXLWordDiffEngine

class XXLGraveyardExporterTest {

    private fun populated(): XXLGraveyardIndex {
        val index = XXLGraveyardIndex()
        index.add(XXLRevision(-100L, 1, 10L, 5L, "первый текст", null, null, XXLRevision.Reason.DELETED_BY_OTHERS))
        index.add(XXLRevision(-100L, 2, 20L, 6L, "второй \"текст\" с кавычками", "кап\nстрока", null, XXLRevision.Reason.EDITED))
        index.add(XXLRevision(-200L, 3, 30L, 7L, "чужой|чат", null, "media:123", XXLRevision.Reason.DELETED_LOCAL))
        return index
    }

    @Test
    fun `json export is deterministic and escaped`() {
        val index = populated()
        val json = XXLGraveyardExporter.toJson(index)
        assertEquals(json, XXLGraveyardExporter.toJson(index))
        assertTrue(json.startsWith("["))
        assertTrue(json.endsWith("]"))
        assertTrue("должен быть экранирован кавычный текст", json.contains("\\\"текст\\\""))
        assertTrue("newline внутри значения обязан экранироваться", json.contains("\\n"))
        assertTrue(json.contains("\"field\":\"text\"") || json.contains("\"text\":\""))
        assertFalse("сырой пайп ломает json-строку", json.contains("\n"))
    }

    @Test
    fun `json import restores the archive`() {
        val index = populated()
        val revived = XXLGraveyardExporter.fromJson(XXLGraveyardExporter.toJson(index))
        assertEquals(index.total().toLong(), revived.total().toLong())
        assertEquals("чужой|чат", revived.latestOf(-200L, 3)?.text)
        assertEquals("второй \"текст\" с кавычками", revived.latestOf(-100L, 2)?.text)
        assertEquals("кап\nстрока", revived.latestOf(-100L, 2)?.caption)
        assertEquals("media:123", revived.latestOf(-200L, 3)?.mediaRef)
    }

    @Test
    fun `json import of garbage yields empty archive`() {
        for (raw in listOf(null, "", "  ", "[]", "{}", "[{", "\"text\"", "[{\"text\":\"x\"}]")) {
            assertEquals("должно быть пусто на: $raw", 0, XXLGraveyardExporter.fromJson(raw).total().toLong())
        }
    }

    @Test
    fun `csv debug dump has one line per revision`() {
        val csv = XXLGraveyardExporter.csvForDebug(populated())
        val lines = csv.split("\n").filter { it.isNotBlank() }
        assertEquals(3, lines.size.toLong())
        assertTrue("каждая строка обязана начинаться с id диалога", lines.all { it.startsWith("-") })
        assertTrue(csv.contains("-100;1;"))
        assertTrue(csv.contains("-100;2;"))
        assertTrue(csv.contains("-200;3;"))
        assertTrue(csv.contains(XXLRatBadgeCore.BADGE_TEXT))
    }

    @Test
    fun `ttl config parser is tolerant and enterprise`() {
        assertEquals(3600L, XXLGraveyardExporter.parseTtlSeconds("3600", 60L))
        assertEquals(60L, XXLGraveyardExporter.parseTtlSeconds(null, 60L))
        assertEquals(60L, XXLGraveyardExporter.parseTtlSeconds("", 60L))
        assertEquals(60L, XXLGraveyardExporter.parseTtlSeconds("не число", 60L))
        assertEquals(60L, XXLGraveyardExporter.parseTtlSeconds("-5", 60L))
        assertEquals(86400L, XXLGraveyardExporter.parseTtlSeconds("ttl_seconds = 86400; cap = 5", 60L))
        assertEquals(86400L, XXLGraveyardExporter.parseTtlSeconds("""{"ttl_seconds":86400}""", 60L))
        assertEquals(1200L, XXLGraveyardExporter.parseTtlSeconds("  ttl_seconds=1200  ", 60L))
    }

    @Test
    fun `rat badge payload follows the mandate`() {
        assertEquals("🗑️ УДАЛЕНО КРЫСОЙ", XXLRatBadgeCore.BADGE_TEXT)
        assertEquals(XXLRatBadgeCore.ACID_BORDER_COLOR, XXLRatBadgeCore.borderColorFor(XXLRevision.Reason.DELETED_BY_OTHERS))
        assertEquals(7, XXLRatBadgeCore.DASH_WIDTH_DP.toLong())
        assertTrue(XXLRatBadgeCore.shouldMark(XXLRevision.Reason.UNKNOWN))

        val revision = XXLRevision(-100L, 42, 900L, 3L, "очень длинный текст ".repeat(40), null, null, XXLRevision.Reason.DELETED_BY_OTHERS)
        val caption = XXLRatBadgeCore.captionFor(revision, "prev", "next")
        assertTrue(caption.contains(XXLRatBadgeCore.BADGE_TEXT))
        assertTrue(caption.contains("#42"))
        assertTrue(caption.contains("чат -100"))
        assertTrue("текст обязан обрезаться: ${caption.length}", caption.length <= 400)
        assertTrue("должен быть diff", caption.contains("[-prev]") || caption.contains("[+next]"))

        val emptyDiff = XXLRatBadgeCore.captionFor(revision, "одинаково", "одинаково")
        assertFalse(emptyDiff.contains("[-одинаково]"))
    }

    @Test
    fun `badge caption survives nulls`() {
        val caption = XXLRatBadgeCore.captionFor(null, null, null)
        assertTrue(caption.isNotBlank())
        assertTrue(caption.contains(XXLRatBadgeCore.BADGE_TEXT))
        assertEquals(0, XXLRatBadgeCore.truncate(null, 10).length.toLong())
        assertEquals("abc", XXLRatBadgeCore.truncate("abcdef", 3))
        assertEquals("abcdef", XXLRatBadgeCore.truncate("abcdef", 10))
    }

    @Test
    fun `word diff feeds the badge caption`() {
        val revision = XXLRevision(-1L, 1, 1L, 1L, "x", null, null, XXLRevision.Reason.EDITED)
        val caption = XXLRatBadgeCore.captionFor(revision, "a b c", "a x c")
        assertTrue(caption.contains("[-b]"))
        assertTrue(caption.contains("[+x]"))
        assertEquals(2, XXLWordDiffEngine.changedWords("a b c", "a x c").toLong())
    }
}
