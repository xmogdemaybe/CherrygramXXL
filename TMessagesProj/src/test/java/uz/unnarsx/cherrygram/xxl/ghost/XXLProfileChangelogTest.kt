/**
 * 🚀✨ Enterprise Quantum Unit Test for Profile Change Log («save user info» из entinyGram) 💯
 */
package uz.unnarsx.cherrygram.xxl.ghost

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.unnarsx.cherrygram.xxl.presence.core.XXLProfileChangelog
import uz.unnarsx.cherrygram.xxl.presence.core.XXLProfileChangelog.Change
import uz.unnarsx.cherrygram.xxl.presence.core.XXLProfileChangelog.ProfileSnapshot

class XXLProfileChangelogTest {

    private fun snapshot(
        id: Long = 5L,
        first: String = "Arslan",
        last: String = "",
        username: String = "arsLan4k1390",
        bio: String = "cherry",
        photoId: Long = 1L,
        epoch: Long = 100L
    ) = ProfileSnapshot(id, first, last, username, bio, photoId, epoch)

    @Test
    fun `equal snapshots produce no changes`() {
        assertEquals(emptyList<Change>(), XXLProfileChangelog.diff(snapshot(), snapshot()))
    }

    @Test
    fun `first observation reports every field as newly seen`() {
        val changes = XXLProfileChangelog.diff(null, snapshot())
        assertTrue(changes.isNotEmpty())
        for (change in changes) {
            assertEquals(XXLProfileChangelog.NO_VALUE, change.oldValue)
            assertEquals(5L, change.userId)
            assertEquals(100L, change.epochSeconds)
        }
        assertEquals(
            listOf("name", "username", "bio", "photo"),
            changes.map { it.field }
        )
    }

    @Test
    fun `each mutable field is detected separately`() {
        val base = snapshot()
        assertEquals(listOf("name"), XXLProfileChangelog.diff(base, snapshot(first = "Aslan")).map { it.field })
        assertEquals(listOf("name"), XXLProfileChangelog.diff(base, snapshot(last = "K")).map { it.field })
        assertEquals(listOf("username"), XXLProfileChangelog.diff(base, snapshot(username = "newbie")).map { it.field })
        assertEquals(listOf("bio"), XXLProfileChangelog.diff(base, snapshot(bio = "нет дел")).map { it.field })
        assertEquals(listOf("photo"), XXLProfileChangelog.diff(base, snapshot(photoId = 9L)).map { it.field })

        val nameChange = XXLProfileChangelog.diff(base, snapshot(first = "Arslan", last = "K"))[0]
        assertEquals("Arslan", nameChange.oldValue)
        assertEquals("Arslan K", nameChange.newValue)
        assertEquals("name", nameChange.field)
    }

    @Test
    fun `null and blank fields normalize instead of crashing`() {
        val weird = ProfileSnapshot(1L, null, null, null, null, 0L, -5L)
        val changes = XXLProfileChangelog.diff(snapshot(id = 1L), weird)
        assertTrue(changes.isNotEmpty())
        for (change in changes) {
            assertFalse("null в значении поля", change.newValue.contains("null"))
            assertTrue(change.oldValue.isNotBlank())
        }
        assertEquals(emptyList<Change>(), XXLProfileChangelog.diff(null, null))
    }

    @Test
    fun `render and parse roundtrip with hostile content`() {
        val hostile = listOf(
            Change(1L, "bio", "раз|два", "строка\nс переносом", 10L),
            Change(2L, "name", "Имя \"кавычка\"", "Другое \\ имя", 20L),
            Change(3L, "photo", "1", "2", 30L)
        )
        val raw = XXLProfileChangelog.render(hostile)
        assertEquals(hostile, XXLProfileChangelog.parse(raw))
        assertTrue(raw.isNotBlank())
    }

    @Test
    fun `parse of garbage returns empty list not exception`() {
        for (raw in listOf(null, "", "   ", "|", "1|bio", "1|bio|a|b|notanumber", "🚀✨💯", "1|bio|a|b|5|extra")) {
            assertEquals("мусор: $raw", emptyList<Change>(), XXLProfileChangelog.parse(raw))
        }
    }

    @Test
    fun `record keeps previous snapshot and accumulates history`() {
        val log = XXLProfileChangelog()
        assertFalse("первое наблюдение только сея снапшот, без спама историей", log.record(snapshot(bio = "первая")))
        assertFalse("идентичный снапшот не пишет историю", log.record(snapshot(bio = "первая")))
        assertTrue(log.record(snapshot(bio = "вторая")))
        val history = log.entriesOf(5L)
        assertEquals(1, history.size.toLong())
        assertEquals("первая", history[0].oldValue)
        assertEquals("вторая", history[0].newValue)
        assertEquals(0, log.entriesOf(404L).size.toLong())
    }

    @Test
    fun `log is capped and export is deterministic json`() {
        val log = XXLProfileChangelog(maxEntries = 12)
        log.record(snapshot(bio = "старт", epoch = 999L))
        for (i in 1..40) {
            log.record(snapshot(bio = "итерация $i", epoch = 1_000L + i))
        }
        assertTrue(log.totalEntries() <= 12)
        val json = log.exportJson()
        assertTrue(json.startsWith("["))
        assertTrue(json.endsWith("]"))
        assertTrue("в json должен быть экранированный ключ", json.contains("\"field\":\"bio\""))
        val quoted = XXLProfileChangelog()
        quoted.record(snapshot(bio = "обычно", epoch = 4L))
        quoted.record(snapshot(bio = "привет \"мир\"", epoch = 5L))
        assertTrue("кавычки внутри значений обязаны экранироваться", quoted.exportJson().contains("\\\""))
        assertEquals(json, log.exportJson())

        val empty = XXLProfileChangelog().exportJson()
        assertEquals("[]", empty)
    }

    @Test
    fun `record rejects junk snapshots`() {
        val log = XXLProfileChangelog()
        assertFalse(log.record(null))
        assertFalse(log.record(snapshot(id = 0L)))
        assertFalse(log.record(snapshot(id = -7L)))
        assertEquals(0, log.totalEntries())
    }
}
