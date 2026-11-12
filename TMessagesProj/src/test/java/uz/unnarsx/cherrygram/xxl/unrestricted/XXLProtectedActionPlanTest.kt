/**
 * 🚀✨ Enterprise Quantum Unit Test for the Protected-Message Action Planner (§2D) 💯
 * Verifies: deterministic ids, guard/scope gating, media-vs-text rules, empty-plan safety.
 */
package uz.unnarsx.cherrygram.xxl.unrestricted

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.unnarsx.cherrygram.xxl.unrestricted.core.XXLProtectedActionPlan
import uz.unnarsx.cherrygram.xxl.unrestricted.core.XXLUnrestrictedPolicy

class XXLProtectedActionPlanTest {

    private val channel = XXLUnrestrictedPolicy.ChatShape(dialogId = -1001234567890L, isChannel = true)
    private val allGuards: Set<XXLUnrestrictedPolicy.Guard> = XXLUnrestrictedPolicy.Guard.values().toSet()

    @Test
    fun `row ids are stable unique and inside the window`() {
        val ids = XXLProtectedActionPlan.Action.values()
            .map { XXLProtectedActionPlan.idFor(it) }
        assertEquals(ids.size.toLong(), ids.toSet().size.toLong())
        for (id in ids) {
            assertTrue("$id", id > XXLProtectedActionPlan.ROW_BASE)
            assertTrue("$id", id <= XXLProtectedActionPlan.ROW_BASE + ids.size)
        }
        assertEquals(XXLProtectedActionPlan.INVALID_ROW_ID.toLong(), XXLProtectedActionPlan.idFor(null).toLong())
        assertNull(XXLProtectedActionPlan.actionForId(1))
        assertNull(XXLProtectedActionPlan.actionForId(-42))
    }

    @Test
    fun `id roundtrip resolves the same action`() {
        for (action in XXLProtectedActionPlan.Action.values()) {
            val id: Int = XXLProtectedActionPlan.idFor(action)
            assertEquals(action, XXLProtectedActionPlan.actionForId(id))
            assertTrue(XXLProtectedActionPlan.renderLine(action).contains(action.title))
            assertTrue(XXLProtectedActionPlan.renderLine(action).startsWith(id.toString()))
        }
        assertEquals("крысы в этом чате не мычат", XXLProtectedActionPlan.renderLine(null))
    }

    @Test
    fun `empty garbage plan inputs never explode`() {
        assertTrue(XXLProtectedActionPlan.planFor(null, XXLUnrestrictedPolicy.Scope.EVERYTHING, channel, true, true, true, false).isEmpty())
        assertTrue(XXLProtectedActionPlan.planFor(emptySet(), XXLUnrestrictedPolicy.Scope.EVERYTHING, channel, true, true, true, false).isEmpty())
        assertTrue(XXLProtectedActionPlan.planFor(allGuards, XXLUnrestrictedPolicy.Scope.EVERYTHING, null, true, true, true, false).isEmpty())
        assertTrue(XXLProtectedActionPlan.planFor(allGuards, XXLUnrestrictedPolicy.Scope.NEVER, channel, true, true, true, false).isEmpty())
        assertTrue(XXLProtectedActionPlan.planFor(allGuards, null, channel, true, true, true, false).isEmpty())
        assertEquals(0, XXLProtectedActionPlan.idsOf(null).size.toLong())
        assertEquals(0, XXLProtectedActionPlan.idsOf(emptyList()).size.toLong())
    }

    @Test
    fun `photo message gets media actions and no plain copy-text`() {
        val plan = XXLProtectedActionPlan.planFor(allGuards, XXLUnrestrictedPolicy.Scope.EVERYTHING, channel, isMedia = true, isText = false, hasCaption = false, isStory = false)
        assertTrue(plan.contains(XXLProtectedActionPlan.Action.COPY_MEDIA))
        assertTrue(plan.contains(XXLProtectedActionPlan.Action.SAVE_TO_GALLERY))
        assertTrue(plan.contains(XXLProtectedActionPlan.Action.SCREENSHOT_HINT))
        assertFalse("копировать нечего", plan.contains(XXLProtectedActionPlan.Action.COPY_TEXT))
        assertTrue("переслать можно всегда", plan.contains(XXLProtectedActionPlan.Action.FORWARD_WO_AUTHOR))
        assertEquals(plan, XXLProtectedActionPlan.planFor(allGuards, XXLUnrestrictedPolicy.Scope.EVERYTHING, channel, true, false, false, false))
    }

    @Test
    fun `captioned photo unlocks copy-text too`() {
        val plan = XXLProtectedActionPlan.planFor(allGuards, XXLUnrestrictedPolicy.Scope.EVERYTHING, channel, isMedia = true, isText = false, hasCaption = true, isStory = false)
        assertTrue(plan.contains(XXLProtectedActionPlan.Action.COPY_TEXT))
        assertEquals(
            XXLProtectedActionPlan.Action.values().toList().size.toLong(),
            plan.size.toLong()
        )
    }

    @Test
    fun `text message loses media-only actions and screenshot hint`() {
        val plan = XXLProtectedActionPlan.planFor(allGuards, XXLUnrestrictedPolicy.Scope.EVERYTHING, channel, isMedia = false, isText = true, hasCaption = false, isStory = false)
        assertTrue(plan.contains(XXLProtectedActionPlan.Action.COPY_TEXT))
        assertFalse(plan.contains(XXLProtectedActionPlan.Action.COPY_MEDIA))
        assertFalse(plan.contains(XXLProtectedActionPlan.Action.SAVE_TO_GALLERY))
        assertFalse(plan.contains(XXLProtectedActionPlan.Action.SCREENSHOT_HINT))
        assertTrue(plan.contains(XXLProtectedActionPlan.Action.EXPORT_TO_SAVED))
    }

    @Test
    fun `story unlocks the screenshot hint without media`() {
        val plan = XXLProtectedActionPlan.planFor(allGuards, XXLUnrestrictedPolicy.Scope.EVERYTHING, channel, isMedia = false, isText = false, hasCaption = false, isStory = true)
        assertTrue(plan.contains(XXLProtectedActionPlan.Action.SCREENSHOT_HINT))
        assertEquals("пустое сообщение не экспортируем", 1, plan.size.toLong())
    }

    @Test
    fun `narrow guard sets prune the plan`() {
        val onlyClipboard = setOf(XXLUnrestrictedPolicy.Guard.CLIPBOARD)
        val plan = XXLProtectedActionPlan.planFor(onlyClipboard, XXLUnrestrictedPolicy.Scope.EVERYTHING, channel, isMedia = true, isText = true, hasCaption = false, isStory = false)
        assertEquals(2, plan.size.toLong())
        assertEquals(XXLProtectedActionPlan.Action.COPY_TEXT, plan[0])
        assertEquals(XXLProtectedActionPlan.Action.COPY_MEDIA, plan[1])

        val onlyScreenshot = setOf(XXLUnrestrictedPolicy.Guard.SCREENSHOT)
        assertEquals(
            listOf(XXLProtectedActionPlan.Action.SCREENSHOT_HINT),
            XXLProtectedActionPlan.planFor(onlyScreenshot, XXLUnrestrictedPolicy.Scope.EVERYTHING, channel, true, false, false, false)
        )
        assertTrue(XXLProtectedActionPlan.idsOf(plan).toList().containsAll(plan.map { XXLProtectedActionPlan.idFor(it) }))
    }

    @Test
    fun `scope outside channels hides everything for a megagroup`() {
        val megagroup = XXLUnrestrictedPolicy.ChatShape(dialogId = -1009876543210L, isChannel = true, isMegagroup = true)
        assertTrue(
            XXLProtectedActionPlan.planFor(
                allGuards, XXLUnrestrictedPolicy.Scope.CHANNELS_ONLY, megagroup, true, true, false, false
            ).isEmpty()
        )
        assertFalse(
            XXLProtectedActionPlan.planFor(
                allGuards, XXLUnrestrictedPolicy.Scope.CHANNELS_ONLY, channel, true, true, false, false
            ).isEmpty()
        )
    }

    @Test
    fun `every action knows its guard`() {
        for (action in XXLProtectedActionPlan.Action.values()) {
            assertTrue(XXLUnrestrictedPolicy.Guard.values().contains(action.guard))
            assertTrue(action.name, XXLProtectedActionPlan.titleLooksCursed(action.title))
        }
    }
}
