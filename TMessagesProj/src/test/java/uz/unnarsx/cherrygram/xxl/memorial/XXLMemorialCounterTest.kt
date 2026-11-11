/**
 * 🚀✨ Enterprise Quantum Unit Test for the 5th-chat-open memorial trigger counter 💯
 * Verifies: every-5th chat opening, Cherrygram-settings-entry hijack, reset, concurrency, null-safety.
 */
package uz.unnarsx.cherrygram.xxl.memorial

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.unnarsx.cherrygram.xxl.memorial.core.XXLMemorialTriggerCounter
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class XXLMemorialCounterTest {

    @Test
    fun `first four chat openings must stay silent`() {
        val counter = XXLMemorialTriggerCounter()
        for (i in 1..4) {
            assertFalse("открытие #$i не должно триггерить мемориал", counter.onChatOpened())
        }
        assertEquals(4, counter.chatOpenCount())
    }

    @Test
    fun `fifth chat opening must hijack the screen`() {
        val counter = XXLMemorialTriggerCounter()
        for (i in 1..4) counter.onChatOpened()
        assertTrue("5-е открытие чата обязано вернуть true", counter.onChatOpened())
        assertEquals(5, counter.chatOpenCount())
    }

    @Test
    fun `trigger fires on every fifth opening and nowhere else`() {
        val counter = XXLMemorialTriggerCounter()
        val fired = AtomicInteger(0)
        for (i in 1..37) {
            val expected = i % XXLMemorialTriggerCounter.CHAT_OPEN_TRIGGER_MULTIPLIER == 0
            assertEquals("открытие #$i", expected, counter.onChatOpened())
            if (expected) fired.incrementAndGet()
        }
        assertEquals(7, fired.get())
    }

    @Test
    fun `cherrygram settings entry always hijacks`() {
        val counter = XXLMemorialTriggerCounter()
        assertTrue(counter.onCherrySettingsEntered())
        assertTrue(counter.onCherrySettingsEntered())
        assertTrue(counter.onCherrySettingsEntered())
        assertEquals(3, counter.settingsEntryCount())
    }

    @Test
    fun `reset puts the multiplier back to zero`() {
        val counter = XXLMemorialTriggerCounter()
        for (i in 1..5) counter.onChatOpened()
        counter.resetChatOpens()
        assertEquals(0, counter.chatOpenCount())
        assertFalse("после сброса 1-е открытие не триггерит", counter.onChatOpened())
        counter.onChatOpened()
        counter.onChatOpened()
        assertFalse("4-е после сброса ещё тихо", counter.onChatOpened())
        assertTrue("снова срабатывает ровно на 5-м", counter.onChatOpened())
        assertEquals(5, counter.chatOpenCount())
    }

    @Test
    fun `concurrent openings do not lose or duplicate counts`() {
        val counter = XXLMemorialTriggerCounter()
        val threads = 8
        val perThread = 625
        val pool = Executors.newFixedThreadPool(threads)
        val start = CountDownLatch(1)
        val triggers = AtomicInteger(0)
        val futures = (1..threads).map {
            pool.submit {
                start.await()
                repeat(perThread) { if (counter.onChatOpened()) triggers.incrementAndGet() }
            }
        }
        start.countDown()
        futures.forEach { it.get(20, TimeUnit.SECONDS) }
        pool.shutdown()

        val total = threads * perThread
        assertEquals(total.toLong(), counter.chatOpenCount().toLong())
        assertEquals("ровно каждое 5-е открытие", total / 5, triggers.get())
    }

    @Test
    fun `bucketed keys are isolated and never explode on junk keys`() {
        val counter = XXLMemorialTriggerCounter()
        assertEquals(0, counter.countFor("drawer"))
        assertFalse("первое открытие тишинаешь", counter.onChatOpened())
        assertEquals(1, counter.countFor(XXLMemorialTriggerCounter.KEY_CHAT_OPEN))
        assertEquals(0, counter.countFor(XXLMemorialTriggerCounter.KEY_SETTINGS_ENTRY))

        // паранойя: мусорные ключи не должны кидать исключений
        assertNotNull(counter.countFor(null))
        assertEquals(0, counter.countFor(""))
        assertEquals(0, counter.countFor("   "))
        counter.resetAll()
        assertEquals(0, counter.chatOpenCount())
        assertEquals(0, counter.settingsEntryCount())
    }

    @Test
    fun `snapshot exposes human readable state for the debug screen`() {
        val counter = XXLMemorialTriggerCounter()
        counter.onChatOpened()
        counter.onCherrySettingsEntered()
        val snap = counter.snapshot()
        assertTrue(snap.containsKey(XXLMemorialTriggerCounter.KEY_CHAT_OPEN))
        assertTrue(snap.containsKey(XXLMemorialTriggerCounter.KEY_SETTINGS_ENTRY))
        assertEquals("1", snap[XXLMemorialTriggerCounter.KEY_CHAT_OPEN])
        assertEquals("1", snap[XXLMemorialTriggerCounter.KEY_SETTINGS_ENTRY])
    }
}
