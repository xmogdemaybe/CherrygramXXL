/**
 * 🚀✨ Enterprise Quantum Unit Test for the Unrestricted-Content Decision Matrix (§2A) 💯
 * Verifies: scope/chat-shape matching, guard parsing, hard blocks, deterministic description,
 * channel dialog id math and total null-safety (никакого NPE в проде).
 */
package uz.unnarsx.cherrygram.xxl.unrestricted

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.unnarsx.cherrygram.xxl.unrestricted.core.XXLUnrestrictedPolicy

class XXLUnrestrictedPolicyTest {

    private val channel = XXLUnrestrictedPolicy.ChatShape(
        dialogId = -1001234567890L, isChannel = true, isMegagroup = false
    )
    private val megagroup = XXLUnrestrictedPolicy.ChatShape(
        dialogId = -1009876543210L, isChannel = true, isMegagroup = true
    )
    private val dm = XXLUnrestrictedPolicy.ChatShape(dialogId = 777L)
    private val saved = XXLUnrestrictedPolicy.ChatShape(dialogId = 12345L, isSaved = true)
    private val secret = XXLUnrestrictedPolicy.ChatShape(dialogId = 999L, isEncrypted = true)

    @Test
    fun `NEVER scope disables every guard everywhere`() {
        for (guard in XXLUnrestrictedPolicy.Guard.values()) {
            for (shape in listOf(channel, megagroup, dm, saved, secret)) {
                assertFalse(
                    "${guard.name} / ${shape.dialogId}",
                    XXLUnrestrictedPolicy.isAllowed(guard, XXLUnrestrictedPolicy.Scope.NEVER, shape)
                )
            }
        }
    }

    @Test
    fun `EVERYTHING scope unlocks every guard for every chat shape`() {
        for (guard in XXLUnrestrictedPolicy.Guard.values()) {
            for (shape in listOf(channel, megagroup, dm, saved, secret)) {
                assertTrue(
                    "${guard.name} / ${shape.dialogId}",
                    XXLUnrestrictedPolicy.isAllowed(guard, XXLUnrestrictedPolicy.Scope.EVERYTHING, shape)
                )
            }
        }
    }

    @Test
    fun `channels-only scope hits broadcast channels and misses megagroups`() {
        val scope = XXLUnrestrictedPolicy.Scope.CHANNELS_ONLY
        assertTrue(XXLUnrestrictedPolicy.isAllowed(XXLUnrestrictedPolicy.Guard.CLIPBOARD, scope, channel))
        assertFalse(XXLUnrestrictedPolicy.isAllowed(XXLUnrestrictedPolicy.Guard.CLIPBOARD, scope, megagroup))
        assertFalse(XXLUnrestrictedPolicy.isAllowed(XXLUnrestrictedPolicy.Guard.CLIPBOARD, scope, dm))
        assertTrue("избранное — святое", XXLUnrestrictedPolicy.isAllowed(XXLUnrestrictedPolicy.Guard.CLIPBOARD, scope, saved))
    }

    @Test
    fun `megagroups-only and dms-only scopes are mutually exclusive`() {
        assertTrue(XXLUnrestrictedPolicy.isAllowed(
            XXLUnrestrictedPolicy.Guard.SAVE_TO_GALLERY, XXLUnrestrictedPolicy.Scope.MEGAGROUPS_ONLY, megagroup
        ))
        assertFalse(XXLUnrestrictedPolicy.isAllowed(
            XXLUnrestrictedPolicy.Guard.SAVE_TO_GALLERY, XXLUnrestrictedPolicy.Scope.MEGAGROUPS_ONLY, channel
        ))
        assertTrue(XXLUnrestrictedPolicy.isAllowed(
            XXLUnrestrictedPolicy.Guard.SAVE_TO_GALLERY, XXLUnrestrictedPolicy.Scope.DMS_ONLY, dm
        ))
        assertFalse(XXLUnrestrictedPolicy.isAllowed(
            XXLUnrestrictedPolicy.Guard.SAVE_TO_GALLERY, XXLUnrestrictedPolicy.Scope.DMS_ONLY, megagroup
        ))
    }

    @Test
    fun `scope parser survives junk hyphens and case`() {
        assertEquals(XXLUnrestrictedPolicy.Scope.CHANNELS_ONLY, XXLUnrestrictedPolicy.scopeFor("channels_only"))
        assertEquals(XXLUnrestrictedPolicy.Scope.CHANNELS_ONLY, XXLUnrestrictedPolicy.scopeFor("  CHANNELS-ONLY  "))
        assertEquals(XXLUnrestrictedPolicy.Scope.MEGAGROUPS_ONLY, XXLUnrestrictedPolicy.scopeFor("megagroups only"))
        assertEquals(XXLUnrestrictedPolicy.Scope.NEVER, XXLUnrestrictedPolicy.scopeFor("never"))
        assertEquals(XXLUnrestrictedPolicy.Scope.EVERYTHING, XXLUnrestrictedPolicy.scopeFor(null))
        assertEquals(XXLUnrestrictedPolicy.Scope.EVERYTHING, XXLUnrestrictedPolicy.scopeFor("   "))
        assertEquals(XXLUnrestrictedPolicy.Scope.EVERYTHING, XXLUnrestrictedPolicy.scopeFor("межгалактический"))
        assertEquals(
            XXLUnrestrictedPolicy.Scope.SAVED_ONLY,
            XXLUnrestrictedPolicy.scopeFor("какой-то мусор", XXLUnrestrictedPolicy.Scope.SAVED_ONLY)
        )
    }

    @Test
    fun `guard parser is strict deterministic and deduplicated`() {
        val all = XXLUnrestrictedPolicy.enabledGuards(null)
        assertEquals(XXLUnrestrictedPolicy.Guard.values().size.toLong(), all.size.toLong())
        assertEquals(XXLUnrestrictedPolicy.Guard.values().size.toLong(), XXLUnrestrictedPolicy.enabledGuards("  ").size.toLong())

        val parsed = XXLUnrestrictedPolicy.enabledGuards("clipboard; screenshot,CLIPBOARD , мусор")
        assertEquals(2, parsed.size.toLong())
        assertTrue(parsed.contains(XXLUnrestrictedPolicy.Guard.CLIPBOARD))
        assertTrue(parsed.contains(XXLUnrestrictedPolicy.Guard.SCREENSHOT))
        assertFalse(parsed.contains(XXLUnrestrictedPolicy.Guard.FORWARD))

        assertTrue(XXLUnrestrictedPolicy.enabledGuards("нет таких кнопок").isEmpty())
    }

    @Test
    fun `guardsFor returns enum-ordered allowed subset`() {
        val shape = channel
        val allowed: List<XXLUnrestrictedPolicy.Guard> = XXLUnrestrictedPolicy.guardsFor(
            XXLUnrestrictedPolicy.Scope.CHANNELS_ONLY, shape
        )
        assertEquals(XXLUnrestrictedPolicy.Guard.values().toList(), allowed)
        assertTrue(
            XXLUnrestrictedPolicy.guardsFor(XXLUnrestrictedPolicy.Scope.NEVER, shape).isEmpty()
        )
        assertTrue(XXLUnrestrictedPolicy.guardsFor(null, shape).isEmpty())
        assertTrue(XXLUnrestrictedPolicy.guardsFor(XXLUnrestrictedPolicy.Scope.EVERYTHING, null).isEmpty())
    }

    @Test
    fun `dialog id classification matches telegram peer math`() {
        assertTrue(XXLUnrestrictedPolicy.isChannelLikeDialog(-1001234567890L))
        assertTrue(XXLUnrestrictedPolicy.isChannelLikeDialog(-1000000000000L))
        assertFalse(XXLUnrestrictedPolicy.isChannelLikeDialog(-42L))
        assertFalse(XXLUnrestrictedPolicy.isChannelLikeDialog(42L))
        assertFalse(XXLUnrestrictedPolicy.isChannelLikeDialog(0L))
        assertTrue(XXLUnrestrictedPolicy.isUserLikeDialog(42L))
        assertFalse(XXLUnrestrictedPolicy.isUserLikeDialog(-1001234567890L))
    }

    @Test
    fun `shape factory derives flags instead of trusting the caller`() {
        val derived = XXLUnrestrictedPolicy.shapeOf(-1005550001112L, savedDialogId = 7L)
        assertTrue(derived.isChannel)
        assertFalse(derived.isSaved)
        assertTrue(XXLUnrestrictedPolicy.shapeOf(7L, savedDialogId = 7L).isSaved)
        assertFalse(XXLUnrestrictedPolicy.shapeOf(0L, savedDialogId = 7L).isSaved)
        val unknown = XXLUnrestrictedPolicy.shapeOf(489000L, savedDialogId = 7L)
        assertEquals(489000L, unknown.dialogId)
    }

    @Test
    fun `describe is null safe and human readable`() {
        val line = XXLUnrestrictedPolicy.describe(XXLUnrestrictedPolicy.Guard.SCREENSHOT, XXLUnrestrictedPolicy.Scope.CHANNELS_ONLY, channel)
        assertTrue(line, line.contains("SCREENSHOT"))
        assertTrue(line, line.contains("CHANNELS_ONLY"))
        assertTrue(line, line.contains("-1001234567890"))
        assertNotNull(XXLUnrestrictedPolicy.describe(null, null, null))
        assertTrue(XXLUnrestrictedPolicy.describe(null, null, null).contains("НЕТ"))
    }

    @Test
    fun `encrypted chats are not a special case for the client-side gate`() {
        for (guard in XXLUnrestrictedPolicy.Guard.values()) {
            assertTrue(
                guard.name,
                XXLUnrestrictedPolicy.isAllowed(guard, XXLUnrestrictedPolicy.Scope.EVERYTHING, secret)
            )
        }
    }
}
