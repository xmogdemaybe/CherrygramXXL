package uz.unnarsx.cherrygram.xxl.graveyard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Коммит 12: ранний перехват удаления. Логика решения и дедупликации ID — чистая, без Android.
 */
class XXLAntiDeleteCoreTest {

    @Test
    fun `перехват только когда фича включена`() {
        assertTrue(XXLAntiDeleteCore.shouldIntercept(true))
        assertFalse(XXLAntiDeleteCore.shouldIntercept(false))
    }

    @Test
    fun `filterNew отсеивает уже сохранённые и дубли`() {
        val existing = setOf(1, 2)
        val incoming = listOf(2, 3, 3, 4, 1)
        assertEquals(listOf(3, 4), XXLAntiDeleteCore.filterNew(existing, incoming))
    }

    @Test
    fun `filterNew терпит null и пустоту`() {
        assertEquals(emptyList<Int>(), XXLAntiDeleteCore.filterNew(emptySet(), null))
        assertEquals(emptyList<Int>(), XXLAntiDeleteCore.filterNew(setOf(1), emptyList()))
        assertEquals(listOf(5), XXLAntiDeleteCore.filterNew(null, listOf(5)))
    }

    @Test
    fun `пустой список не перехватывается`() {
        assertFalse(XXLAntiDeleteCore.hasWork(null))
        assertFalse(XXLAntiDeleteCore.hasWork(emptyList()))
        assertTrue(XXLAntiDeleteCore.hasWork(listOf(1)))
    }
}
