/**
 * 🚀✨ Enterprise Quantum Unit Test for the Graveyard archive index (anti-delete) 💯
 */
package uz.unnarsx.cherrygram.xxl.graveyard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.unnarsx.cherrygram.xxl.graveyard.core.XXLGraveyardIndex
import uz.unnarsx.cherrygram.xxl.graveyard.core.XXLRevision

class XXLGraveyardIndexTest {

    private fun revision(
        dialog: Long = -100L,
        message: Int = 5,
        epoch: Long = 1000L,
        text: String = "оригинальный текст",
        caption: String? = null,
        reason: XXLRevision.Reason = XXLRevision.Reason.DELETED_BY_OTHERS,
        author: Long = 77L
    ) = XXLRevision(dialog, message, epoch, author, text, caption, null, reason)

    @Test
    fun `add stores the revision and dedupes exact repeats`() {
        val index = XXLGraveyardIndex()
        assertTrue(index.add(revision()))
        assertFalse("полный дубль не должен удваивать архив", index.add(revision()))
        assertEquals(1, index.total().toLong())
        assertEquals(1, index.entriesOf(-100L).size.toLong())
    }

    @Test
    fun `same message with new text is a new revision, newest wins`() {
        val index = XXLGraveyardIndex()
        index.add(revision(text = "первая редакция", epoch = 100L))
        index.add(revision(text = "вторая редакция", epoch = 200L))
        val latest = index.latestOf(-100L, 5)
        assertEquals("вторая редакция", latest?.text)
        assertEquals(2, index.entriesOf(-100L).size.toLong())
    }

    @Test
    fun `junk revisions are rejected without exceptions`() {
        val index = XXLGraveyardIndex()
        assertFalse(index.add(null))
        assertFalse(index.add(revision(dialog = 0L)))
        assertFalse(index.add(revision(message = -3)))
        assertFalse(index.add(revision(epoch = -5L)))
        assertEquals(0, index.total().toLong())
        assertNull(index.latestOf(0L, 0))
    }

    @Test
    fun `per dialog cap keeps the newest entries`() {
        val index = XXLGraveyardIndex(maxPerDialog = 4)
        for (i in 1..25) {
            index.add(revision(message = i, epoch = 1_000L + i))
        }
        val entries = index.entriesOf(-100L)
        assertEquals(4, entries.size.toLong())
        assertEquals(25, entries.last().messageId)
        assertEquals(22, entries.first().messageId)
        assertEquals(4, index.total().toLong())
    }

    @Test
    fun `global cap drops the oldest dialog entries`() {
        val index = XXLGraveyardIndex(maxPerDialog = 100, maxTotal = 10)
        for (i in 1..40) {
            index.add(revision(dialog = -200L - i, message = i, epoch = 2_000L + i))
        }
        assertTrue(index.total() <= 10)
        assertEquals(0, index.entriesOf(-201L).size.toLong())
    }

    @Test
    fun `ttl eviction removes expired revisions only`() {
        val index = XXLGraveyardIndex()
        index.add(revision(message = 1, epoch = 1_000L))
        index.add(revision(message = 2, epoch = 9_000L))
        index.evictExpired(nowSeconds = 10_000L, ttlSeconds = 5_000L)
        assertEquals(1, index.total().toLong())
        assertEquals(2, index.entriesOf(-100L).first().messageId)
        // мусорные параметры ничего не удаляют
        index.evictExpired(nowSeconds = -1L, ttlSeconds = 5L)
        index.evictExpired(nowSeconds = 10L, ttlSeconds = 0L)
        assertEquals(1, index.total().toLong())
    }

    @Test
    fun `search is case insensitive and null safe`() {
        val index = XXLGraveyardIndex()
        index.add(revision(message = 1, text = "CherryGram — лучший форк"))
        index.add(revision(message = 2, text = "ничего интересного"))
        assertEquals(1, index.search("cherrygram").size.toLong())
        assertEquals(1, index.search("ЛУЧШИЙ").size.toLong())
        assertTrue(index.search(null).isEmpty())
        assertTrue(index.search("   ").isEmpty())
        assertTrue(index.search("несуществующее слово").isEmpty())
    }

    @Test
    fun `serialize roundtrip preserves everything`() {
        val index = XXLGraveyardIndex()
        index.add(revision(text = "текст|с пайпом", caption = "подпись\nвторая строка", epoch = 123L))
        index.add(revision(message = 6, reason = XXLRevision.Reason.EDITED, text = "редакция", epoch = 124L))
        index.add(revision(dialog = -300L, message = 9, text = "другой чат", epoch = 125L))

        val revived = XXLGraveyardIndex.deserialize(index.serialize())
        assertEquals(index.serialize(), revived.serialize())
        assertEquals(3, revived.total().toLong())
        assertEquals("текст|с пайпом", revived.latestOf(-100L, 5)?.text)
        assertEquals("подпись\nвторая строка", revived.latestOf(-100L, 5)?.caption)
        assertEquals(XXLRevision.Reason.EDITED, revived.latestOf(-100L, 6)?.reason)
        assertEquals("другой чат", revived.latestOf(-300L, 9)?.text)
    }

    @Test
    fun `deserialize of garbage yields an empty archive`() {
        for (raw in listOf(null, "", "   ", "мусор", "1|2|3", "-100|5|123|77|текст||||", "-100|5|abc|77|текст|||DELETED_BY_OTHERS")) {
            assertEquals("мусор: $raw", 0, XXLGraveyardIndex.deserialize(raw).total().toLong())
        }
    }

    @Test
    fun `concurrent adds are not lost`() {
        val index = XXLGraveyardIndex(maxPerDialog = 1000, maxTotal = 5000)
        val threads = (1..64).map { id ->
            Thread {
                index.add(revision(dialog = -500L, message = id, epoch = 5_000L + id, text = "nit $id"))
            }.apply { start() }
        }
        threads.forEach { it.join(10_000) }
        assertEquals(64, index.total().toLong())
        assertEquals(64, index.entriesOf(-500L).size.toLong())
    }

    @Test
    fun `reasons describe themselves for the ui`() {
        assertEquals("DELETED_BY_OTHERS", XXLRevision.Reason.DELETED_BY_OTHERS.name)
        assertTrue(XXLRevision.Reason.fromRaw("edited") == XXLRevision.Reason.EDITED)
        assertEquals(XXLRevision.Reason.DELETED_LOCAL, XXLRevision.Reason.fromRaw("DELETED_LOCAL"))
        assertEquals(XXLRevision.Reason.UNKNOWN, XXLRevision.Reason.fromRaw(null))
        assertEquals(XXLRevision.Reason.UNKNOWN, XXLRevision.Reason.fromRaw("чушь"))
        assertTrue(revision().describe().contains("чат"))
    }
}
