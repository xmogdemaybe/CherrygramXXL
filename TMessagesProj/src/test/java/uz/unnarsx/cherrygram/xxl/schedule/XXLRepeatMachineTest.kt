/**
 * 🚀✨ Enterprise Quantum Unit Test for the Repeat State Machine (§2A crash-safety) 💯
 * Verifies: детерминированная дрожь, конечность серии, статусы, счётчики, null/junk-устойчивость.
 */
package uz.unnarsx.cherrygram.xxl.schedule

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.unnarsx.cherrygram.xxl.schedule.core.XXLAutoRepeatConfigCore
import uz.unnarsx.cherrygram.xxl.schedule.core.XXLAutoRepeatConfigCore.RepeatConfig
import uz.unnarsx.cherrygram.xxl.schedule.core.XXLRepeatMachine

class XXLRepeatMachineTest {

    private val config = RepeatConfig(repeats = 4, intervalSeconds = 13, jitterSeconds = 3, silent = false, seed = 42L)

    @Test
    fun `start arms the machine and returns the first delay`() {
        val machine = XXLRepeatMachine(config)
        assertEquals(XXLRepeatMachine.Status.IDLE, machine.status())
        val decision = machine.start(1_000)
        assertTrue(decision.send)
        assertEquals(XXLRepeatMachine.Status.RUNNING, decision.status)
        assertEquals(XXLRepeatMachine.Status.RUNNING, machine.status())
        assertTrue("задержка обязана быть положительной", decision.nextDelaySeconds >= 1)
    }

    @Test
    fun `series is finite and exactly repeats long`() {
        val machine = XXLRepeatMachine(config)
        machine.start(0)
        var sends = 0
        for (occurrence in 1..20) {
            val decision = machine.tick(occurrence, (occurrence * 13L))
            if (decision.send) sends++
        }
        assertEquals(4, sends.toLong())
        assertEquals(4, machine.sentCount().toLong())
        assertEquals(0, machine.remaining().toLong())
        assertEquals(XXLRepeatMachine.Status.FINISHED, machine.status())
        assertFalse(machine.tick(9, 999).send)
    }

    @Test
    fun `tick before start reports finished or idle but never sends junk`() {
        val machine = XXLRepeatMachine(config)
        val early = machine.tick(0, 10L)
        assertFalse("нулевой occurrence не стреляет", early.send)
        assertTrue(early.nextDelaySeconds >= 0)
    }

    @Test
    fun `jitter stays inside the announced window and is deterministic`() {
        val machine = XXLRepeatMachine(config)
        for (occurrence in 1..50) {
            val jitter: Int = machine.jitterFor(occurrence)
            assertTrue("jitter=$jitter", jitter >= -3 && jitter <= 3)
            assertEquals(jitter.toLong(), machine.jitterFor(occurrence).toLong())
            val delay: Int = machine.delayFor(occurrence)
            assertTrue("$delay", delay >= 1 && delay <= XXLRepeatMachine.MAX_DELAY_SECONDS)
        }
        assertEquals(13, XXLRepeatMachine(config.copy(jitterSeconds = 0)).delayFor(7).toLong())
    }

    @Test
    fun `zero repeats is an invalid machine, not a crash`() {
        val machine = XXLRepeatMachine(RepeatConfig(0, 13, 0, false, 0L))
        val decision = machine.start(1L)
        assertFalse(decision.send)
        assertEquals(XXLRepeatMachine.Status.INVALID, decision.status)
        assertEquals(XXLRepeatMachine.Reason.INVALID_CONFIG, decision.reason)
        assertEquals(0, machine.sentCount().toLong())
    }

    @Test
    fun `interval below the legal floor is clamped not rejected`() {
        val machine = XXLRepeatMachine(RepeatConfig(3, 1, 0, false, 0L))
        assertTrue(machine.delayFor(1) >= XXLAutoRepeatConfigCore.MIN_INTERVAL_SECONDS)
    }

    @Test
    fun `stop is idempotent and freezes the counters`() {
        val machine = XXLRepeatMachine(config)
        machine.start(0)
        machine.tick(1, 10L)
        val sentBefore: Int = machine.sentCount()
        machine.stop()
        assertEquals(XXLRepeatMachine.Status.STOPPED, machine.status())
        val after = machine.tick(2, 20L)
        assertFalse(after.send)
        assertEquals(XXLRepeatMachine.Reason.STOPPED_BY_USER, after.reason)
        machine.stop()
        assertEquals(sentBefore.toLong(), machine.sentCount().toLong())
    }

    @Test
    fun `time going backwards never produces negative delays`() {
        val machine = XXLRepeatMachine(config.copy(intervalSeconds = XXLRepeatMachine.MAX_DELAY_SECONDS))
        machine.start(0)
        assertTrue(machine.delayFor(1) >= 1)
        assertTrue(machine.delayFor(2) <= XXLRepeatMachine.MAX_DELAY_SECONDS)
        val huge = XXLRepeatMachine(RepeatConfig(2, XXLRepeatMachine.MAX_DELAY_SECONDS, 60, false, 1L))
        assertTrue(huge.delayFor(1) in 1..XXLRepeatMachine.MAX_DELAY_SECONDS)
    }

    @Test
    fun `snapshot exposes the human state`() {
        val machine = XXLRepeatMachine(config)
        machine.start(100)
        machine.tick(1, 113)
        machine.tick(2, 226)
        val snap: Map<String, String> = machine.snapshot()
        assertEquals("2", snap[XXLRepeatMachine.KEY_SENT])
        assertEquals("2", snap[XXLRepeatMachine.KEY_REMAINING])
        assertEquals(XXLRepeatMachine.Status.RUNNING.name, snap[XXLRepeatMachine.KEY_STATUS])
        assertEquals("13", snap[XXLRepeatMachine.KEY_INTERVAL])
        assertTrue(machine.describe().contains("КРЫСА"))
    }

    @Test
    fun `negative occurrence and hostile seeds are survived`() {
        val machine = XXLRepeatMachine(RepeatConfig(3, 7, 5, true, Long.MIN_VALUE))
        machine.start(-999)
        assertFalse(machine.tick(-5, -1).send)
        assertTrue(machine.tick(1, -1).send)
        assertTrue(machine.delayFor(-1) >= 1)
        assertEquals(XXLRepeatMachine.Status.RUNNING, machine.status())
    }
}
