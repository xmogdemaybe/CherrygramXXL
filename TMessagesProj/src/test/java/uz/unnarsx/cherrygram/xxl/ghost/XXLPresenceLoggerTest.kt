/**
 * 🚀✨ Enterprise Quantum Unit Test for Presence Logger + Last Seen Memory 💯
 * Портируется слой «presence logger / read time / remembered last seen» из
 * entinyGram FEATURES.md (privacy & protection), ядро — Android-free.
 */
package uz.unnarsx.cherrygram.xxl.ghost

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.unnarsx.cherrygram.xxl.presence.core.XXLLastSeenMemory
import uz.unnarsx.cherrygram.xxl.presence.core.XXLPresenceStateMachine

class XXLPresenceLoggerTest {

    @Test
    fun `stock pseudo-expires map onto our states`() {
        assertEquals(XXLPresenceStateMachine.State.RECENTLY, XXLPresenceStateMachine.stateFromExpires(-100, false))
        assertEquals(XXLPresenceStateMachine.State.LAST_WEEK, XXLPresenceStateMachine.stateFromExpires(-101, false))
        assertEquals(XXLPresenceStateMachine.State.LAST_MONTH, XXLPresenceStateMachine.stateFromExpires(-102, false))
        assertEquals(XXLPresenceStateMachine.State.ONLINE, XXLPresenceStateMachine.stateFromExpires(0, true))
        assertEquals(XXLPresenceStateMachine.State.OFFLINE, XXLPresenceStateMachine.stateFromExpires(1_700_000_000L, false))
        assertEquals(XXLPresenceStateMachine.State.UNKNOWN, XXLPresenceStateMachine.stateFromExpires(0, false))
    }

    @Test
    fun `identical consecutive events are deduped`() {
        val log = XXLPresenceStateMachine()
        assertTrue(log.record(7L, XXLPresenceStateMachine.State.ONLINE, 1000L))
        assertFalse("повтор того же состояния не пишется", log.record(7L, XXLPresenceStateMachine.State.ONLINE, 1001L))
        assertTrue(log.record(7L, XXLPresenceStateMachine.State.OFFLINE, 1002L))
        assertEquals(2, log.historyOf(7L).size.toLong())
    }

    @Test
    fun `junk user ids never enter the log`() {
        val log = XXLPresenceStateMachine()
        assertFalse(log.record(0L, XXLPresenceStateMachine.State.ONLINE, 1L))
        assertFalse(log.record(-1L, XXLPresenceStateMachine.State.ONLINE, 1L))
        assertFalse(log.record(5L, null, 1L))
        assertFalse(log.record(5L, XXLPresenceStateMachine.State.UNKNOWN, -5L))
        assertEquals(0, log.totalEvents())
    }

    @Test
    fun `log is capped and drops the oldest`() {
        val log = XXLPresenceStateMachine(maxEventsPerUser = 10)
        val states = listOf(
            XXLPresenceStateMachine.State.ONLINE,
            XXLPresenceStateMachine.State.OFFLINE
        )
        for (i in 1..50) {
            log.record(42L, states[i % 2], 1_000_000L + i)
        }
        val history = log.historyOf(42L)
        assertEquals(10, history.size.toLong())
        assertEquals(1_000_041L, history.first().epochSeconds)
        assertEquals(1_000_050L, history.last().epochSeconds)
    }

    @Test
    fun `notification only on real transitions and throttled per user`() {
        val log = XXLPresenceStateMachine()
        assertTrue(log.shouldNotify(9L, XXLPresenceStateMachine.State.ONLINE, 500L))
        assertFalse("тот же статус не должен шуметь", log.shouldNotify(9L, XXLPresenceStateMachine.State.ONLINE, 501L))
        assertFalse("троттлинг по юзеру", log.shouldNotify(9L, XXLPresenceStateMachine.State.OFFLINE, 502L))
        assertTrue("через MIN_NOTIFY_INTERVAL можно снова", log.shouldNotify(9L, XXLPresenceStateMachine.State.OFFLINE, 502L + XXLPresenceStateMachine.MIN_NOTIFY_INTERVAL_SECONDS))
        assertTrue(log.shouldNotify(10L, XXLPresenceStateMachine.State.ONLINE, 500L))
        assertFalse(log.shouldNotify(0L, XXLPresenceStateMachine.State.ONLINE, 500L))
        assertFalse(log.shouldNotify(11L, XXLPresenceStateMachine.State.UNKNOWN, 500L))
    }

    @Test
    fun `compact drops entries older than ttl`() {
        val log = XXLPresenceStateMachine()
        log.record(3L, XXLPresenceStateMachine.State.ONLINE, 100L)
        log.record(3L, XXLPresenceStateMachine.State.OFFLINE, 200L)
        log.record(3L, XXLPresenceStateMachine.State.ONLINE, 10_000L)
        log.compact(nowSeconds = 11_000L, ttlSeconds = 5_000L)
        val rest = log.historyOf(3L)
        assertEquals(1, rest.size.toLong())
        assertEquals(10_000L, rest[0].epochSeconds)
    }

    @Test
    fun `presence serialize roundtrip survives garbage`() {
        val log = XXLPresenceStateMachine()
        log.record(1L, XXLPresenceStateMachine.State.ONLINE, 10L)
        log.record(1L, XXLPresenceStateMachine.State.RECENTLY, 20L)
        log.record(2L, XXLPresenceStateMachine.State.OFFLINE, 30L)
        val revived = XXLPresenceStateMachine.deserialize(log.serialize())
        assertEquals(log.serialize(), revived.serialize())
        assertEquals(3, revived.totalEvents())

        for (raw in listOf("", "  ", "junk", "1|ONLINE", "1|ONLINE|abc;2|;|;|", "🚀")) {
            assertEquals("мусор должен дать пустой лог: $raw", 0, XXLPresenceStateMachine.deserialize(raw).totalEvents())
        }
        assertEquals(0, XXLPresenceStateMachine.deserialize(null).totalEvents())
    }

    @Test
    fun `last seen memory prefers exact epoch over coarse status`() {
        val memory = XXLLastSeenMemory()
        memory.observe(5L, XXLPresenceStateMachine.State.ONLINE, 0L, nowSeconds = 1_000L)
        assertEquals(1_000L, memory.rememberedFor(5L))

        // «recently» не перетирает точное значение
        memory.observe(5L, XXLPresenceStateMachine.State.RECENTLY, -100L, nowSeconds = 2_000L)
        assertEquals("грубый статус не должен затирать точный last seen", 1_000L, memory.rememberedFor(5L))

        // более свежий точный epoch обновляет
        memory.observe(5L, XXLPresenceStateMachine.State.OFFLINE, 1_500L, nowSeconds = 1_600L)
        assertEquals(1_500L, memory.rememberedFor(5L))

        // старый точный epoch игнорируется
        memory.observe(5L, XXLPresenceStateMachine.State.OFFLINE, 900L, nowSeconds = 1_700L)
        assertEquals(1_500L, memory.rememberedFor(5L))
    }

    @Test
    fun `last seen unknown user is described safely`() {
        val memory = XXLLastSeenMemory()
        assertEquals(0L, memory.rememberedFor(77L))
        assertEquals(XXLLastSeenMemory.UNKNOWN_LABEL, memory.describe(77L))
        memory.observe(0L, XXLPresenceStateMachine.State.ONLINE, 0L, 10L)
        assertEquals(0L, memory.rememberedFor(0L))
        memory.observe(-3L, null, 0L, 10L)
        assertTrue(memory.describe(-3L).isNotBlank())
    }

    @Test
    fun `last seen serialize roundtrip`() {
        val memory = XXLLastSeenMemory()
        memory.observe(1L, XXLPresenceStateMachine.State.ONLINE, 0L, 111L)
        memory.observe(2L, XXLPresenceStateMachine.State.OFFLINE, 222L, 300L)
        val raw = memory.serialize()
        val revived = XXLLastSeenMemory.deserialize(raw)
        assertEquals(111L, revived.rememberedFor(1L))
        assertEquals(222L, revived.rememberedFor(2L))
        assertEquals(raw, revived.serialize())
        assertEquals(0L, XXLLastSeenMemory.deserialize("мусор|не|разобрано").rememberedFor(1L))
    }
}
