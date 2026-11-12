/**
 * 🚀✨ Enterprise Quantum Unit Test For The Live Snapshot Ledger 💯
 * Verifies: content capture, edit detection, LRU eviction, take/consume, null-safety, roundtrip.
 */
package uz.unnarsx.cherrygram.xxl.graveyard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.unnarsx.cherrygram.xxl.graveyard.core.XXLSnapshotLedger

class XXLSnapshotLedgerTest {

    private fun snap(
        dialog: Long = -100L,
        msg: Int = 1,
        text: String? = "текст",
        caption: String? = null,
        media: String? = null,
        epoch: Long = 1000L
    ) = XXLSnapshotLedger.Snapshot(dialog, msg, text, caption, media, 7L, epoch)

    @Test
    fun `content is captured on first sight`() {
        val ledger = XXLSnapshotLedger()
        assertTrue(ledger.observe(snap(text = "привет крыса")))
        val stored = ledger.peek(-100L, 1)
        assertNotNull(stored)
        assertEquals("привет крыса", stored!!.text)
        assertEquals(7L, stored.authorId)
    }

    @Test
    fun `empty and junk snapshots never enter the ledger`() {
        val ledger = XXLSnapshotLedger()
        assertFalse(ledger.observe(null))
        assertFalse(ledger.observe(snap(text = null)))
        assertFalse(ledger.observe(snap(text = "   ", caption = "", media = null)))
        assertEquals(0, ledger.size())
        assertNull(ledger.peek(-100L, 1))
    }

    @Test
    fun `identical re-observation is not reported as an edit`() {
        val ledger = XXLSnapshotLedger()
        assertTrue(ledger.observe(snap(text = "раз два три")))
        assertFalse("повторный рендер того же текста — не редакция", ledger.observe(snap(text = "раз два три")))
        assertEquals(1, ledger.size())
    }

    @Test
    fun `previous content survives long enough to build a diff`() {
        val ledger = XXLSnapshotLedger()
        ledger.observe(snap(text = "cherry is a piece of shit"))
        val previous = ledger.previousOf(snap(text = "cherry is a masterpiece"))
        assertNotNull(previous)
        assertEquals("cherry is a piece of shit", previous!!.text)
        assertTrue(ledger.observe(snap(text = "cherry is a masterpiece")))
        assertEquals("cherry is a masterpiece", ledger.peek(-100L, 1)?.text)
        assertEquals("редакция не создаёт новую запись", 1, ledger.size())
    }

    @Test
    fun `take consumes the entry so delete cannot archive twice`() {
        val ledger = XXLSnapshotLedger()
        ledger.observe(snap(msg = 42, text = "уничтожить"))
        assertEquals("уничтожить", ledger.take(-100L, 42)?.text)
        assertNull("после consume снапшота нет", ledger.peek(-100L, 42))
        assertNull(ledger.take(-100L, 42))
        assertEquals(0, ledger.size())
    }

    @Test
    fun `lru cap evicts the oldest entries and never explodes`() {
        val ledger = XXLSnapshotLedger(maxEntries = 3)
        for (i in 1..10) ledger.observe(snap(msg = i, text = "m$i"))
        assertEquals(3, ledger.size())
        assertNull(ledger.peek(-100L, 1))
        assertNotNull(ledger.peek(-100L, 10))
        for (key in ledger.knownKeys()) {
            assertTrue("ключ обязан быть диалог:msg = $key", key.contains(":"))
        }
    }

    @Test
    fun `dropDialog removes only the target dialog`() {
        val ledger = XXLSnapshotLedger()
        ledger.observe(snap(dialog = -100L, msg = 1))
        ledger.observe(snap(dialog = -200L, msg = 1))
        ledger.observe(snap(dialog = -200L, msg = 2))
        ledger.dropDialog(-200L)
        assertEquals(1, ledger.size())
        assertNotNull(ledger.peek(-100L, 1))
        assertNull(ledger.peek(-200L, 1))
        assertNull(ledger.peek(-200L, 2))
    }

    @Test
    fun `serialize roundtrip revives text caption and media`() {
        val ledger = XXLSnapshotLedger()
        ledger.observe(snap(msg = 5, text = "первый | текст", caption = "капшен", media = "photo:777"))
        ledger.observe(snap(msg = 6, text = null, caption = null, media = "video:888"))
        val revived = XXLSnapshotLedger.deserialize(ledger.serialize())
        assertEquals(2, revived.size())
        val first = revived.peek(-100L, 5)
        assertNotNull(first)
        assertEquals("первый | текст", first!!.text)
        assertEquals("капшен", first.caption)
        assertEquals("photo:777", first.mediaRef)
        assertEquals(1000L, first.epochSeconds)
        assertEquals("video:888", revived.peek(-100L, 6)?.mediaRef)
        assertNull(revived.peek(-100L, 6)?.text)
    }

    @Test
    fun `deserialize survives garbage and nulls`() {
        assertEquals(0, XXLSnapshotLedger.deserialize(null).size())
        assertEquals(0, XXLSnapshotLedger.deserialize("").size())
        assertEquals(0, XXLSnapshotLedger.deserialize("мусор\nеще мусор\n\n").size())
        assertEquals(0, XXLSnapshotLedger.deserialize("-100|1|abc|7|∅|∅|∅").size())
        assertEquals(0, XXLSnapshotLedger.deserialize("-100|1|1000").size())
        val mixed = XXLSnapshotLedger.deserialize("-100|1|1000|7|x|∅|∅\nмусор\n-100|2|1001|7|ok|∅|∅")
        assertEquals(2, mixed.size())
        assertEquals("x", mixed.peek(-100L, 1)?.text)
        assertEquals("ok", mixed.peek(-100L, 2)?.text)
    }

    @Test
    fun `concurrent rendering threads never blow up the ledger`() {
        val ledger = XXLSnapshotLedger(maxEntries = 200)
        val threads = 6
        val perThread = 120
        val pool = java.util.concurrent.Executors.newFixedThreadPool(threads)
        val start = java.util.concurrent.CountDownLatch(1)
        val edits = java.util.concurrent.atomic.AtomicInteger(0)
        val futures = (1..threads).map { worker ->
            pool.submit {
                start.await()
                repeat(perThread) { i ->
                    val msg = i % 40
                    if (ledger.observe(snap(dialog = -100L, msg = msg, text = "w$worker-$i"))) {
                        edits.incrementAndGet()
                    }
                    ledger.peek(-100L, msg)
                }
            }
        }
        start.countDown()
        futures.forEach { it.get(20, java.util.concurrent.TimeUnit.SECONDS) }
        pool.shutdown()
        assertTrue("мутации засчитаны", edits.get() > 0)
        assertTrue("кап соблюдается: ${ledger.size()}", ledger.size() <= 200)
        assertTrue(ledger.describe().startsWith("live="))
    }
}
