/**
 * 🚀✨ Enterprise Quantum Unit Test for the dodging «Закрыть» button 💯
 * AGENTS.md §1: кнопка закрытия уклоняется от первого тапа (смещение координат)
 * и закрывается только со второго тапа.
 */
package uz.unnarsx.cherrygram.xxl.memorial

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.unnarsx.cherrygram.xxl.memorial.core.XXLDodgeCloseStateMachine

class XXLDodgeCloseStateMachineTest {

    @Test
    fun `first tap must dodge, not close`() {
        val machine = XXLDodgeCloseStateMachine(seed = 42L)
        assertEquals(XXLDodgeCloseStateMachine.STATE_IDLE, machine.state())

        val first = machine.onCloseTap()
        assertTrue("первый тап обязан уклониться", first.dodged)
        assertFalse("первый тап НЕ должен закрывать диалог", first.closed)
        assertEquals(XXLDodgeCloseStateMachine.STATE_DODGING, machine.state())
        assertNotEquals("кнопка должна сместиться по X", 0, first.dx)
        assertNotEquals("кнопка должна сместиться по Y", 0, first.dy)
    }

    @Test
    fun `second tap closes exactly once`() {
        val machine = XXLDodgeCloseStateMachine(seed = 42L)
        machine.onCloseTap()
        val second = machine.onCloseTap()
        assertTrue("второй тап закрывает мемориал", second.closed)
        assertFalse("на закрывающем тапе уклонения уже нет", second.dodged)
        assertEquals(XXLDodgeCloseStateMachine.STATE_CLOSED, machine.state())
        assertEquals(0, second.dx)
        assertEquals(0, second.dy)
    }

    @Test
    fun `taps after close are harmless and idempotent`() {
        val machine = XXLDodgeCloseStateMachine(seed = 7L)
        machine.onCloseTap()
        machine.onCloseTap()
        for (i in 1..5) {
            val late = machine.onCloseTap()
            assertTrue("состояние осталось закрытым", late.closed)
            assertFalse(late.dodged)
            assertEquals(0, late.dx)
            assertEquals(0, late.dy)
        }
        assertEquals(XXLDodgeCloseStateMachine.STATE_CLOSED, machine.state())
    }

    @Test
    fun `dodge vector is deterministic per seed`() {
        val a = XXLDodgeCloseStateMachine(1337L).onCloseTap()
        val b = XXLDodgeCloseStateMachine(1337L).onCloseTap()
        assertEquals("тот же seed -> то же смещение X", a.dx, b.dx)
        assertEquals("тот же seed -> то же смещение Y", a.dy, b.dy)
    }

    @Test
    fun `shift never escapes the safe bounds`() {
        for (seed in 0L..511L) {
            val tap = XXLDodgeCloseStateMachine(seed).onCloseTap()
            assertTrue("dx вне bounds при seed=$seed", Math.abs(tap.dx) <= XXLDodgeCloseStateMachine.MAX_SHIFT_DP)
            assertTrue("dy вне bounds при seed=$seed", Math.abs(tap.dy) <= XXLDodgeCloseStateMachine.MAX_SHIFT_DP)
            assertTrue("смещение обязано быть ненулевым", tap.dx != 0 || tap.dy != 0)
        }
    }

    @Test
    fun `extreme seeds and overflow attempts cannot crash the machine`() {
        val nastiest = longArrayOf(Long.MAX_VALUE, Long.MIN_VALUE, -1L, 0L, Int.MAX_VALUE * 42L)
        for (seed in nastiest) {
            val machine = XXLDodgeCloseStateMachine(seed)
            val first = machine.onCloseTap()
            assertTrue(first.dodged)
            assertTrue(Math.abs(first.dx) <= XXLDodgeCloseStateMachine.MAX_SHIFT_DP)
            assertTrue(Math.abs(first.dy) <= XXLDodgeCloseStateMachine.MAX_SHIFT_DP)
            assertTrue(machine.onCloseTap().closed)
            machine.reset()
            assertEquals(XXLDodgeCloseStateMachine.STATE_IDLE, machine.state())
            assertEquals(0, machine.tapCount())
        }
    }

    @Test
    fun `tap counter tracks real user effort`() {
        val machine = XXLDodgeCloseStateMachine(3L)
        assertEquals(0, machine.tapCount())
        machine.onCloseTap()
        assertEquals(1, machine.tapCount())
        machine.onCloseTap()
        assertEquals(2, machine.tapCount())
        assertTrue(machine.isClosed())
        assertFalse(machine.isArmed())
    }
}
