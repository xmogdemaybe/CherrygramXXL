/**
 * 🚀✨ Enterprise Quantum Unit Test for the Ghost Mode decision matrix (§портировано из entinyGram) 💯
 * Приоритет: override конкретного чата > правило типа чата > глобальный тумблер действия.
 */
package uz.unnarsx.cherrygram.xxl.ghost

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.unnarsx.cherrygram.xxl.ghost.core.XXLGhostAction
import uz.unnarsx.cherrygram.xxl.ghost.core.XXLGhostModeResolver
import uz.unnarsx.cherrygram.xxl.ghost.core.XXLGhostScope

class XXLGhostModeResolverTest {

    @Test
    fun `defaults follow the fork mandate not the server`() {
        val resolver = XXLGhostModeResolver()
        assertTrue("read receipt по умолчанию супрессится", resolver.shouldSuppress(101L, XXLGhostScope.PRIVATE, XXLGhostAction.READ_RECEIPT))
        assertTrue(resolver.shouldSuppress(-202L, XXLGhostScope.CHANNEL, XXLGhostAction.READ_RECEIPT))
        assertFalse("typing по умолчанию НЕ супрессится", resolver.shouldSuppress(101L, XXLGhostScope.PRIVATE, XXLGhostAction.TYPING))
    }

    @Test
    fun `global switch beats nothing but is beaten by scope rule`() {
        val resolver = XXLGhostModeResolver()
        resolver.setGlobal(XXLGhostAction.STORY_VIEW, false)
        assertFalse(resolver.shouldSuppress(1L, XXLGhostScope.PRIVATE, XXLGhostAction.STORY_VIEW))

        resolver.setScopeRule(XXLGhostScope.MEGAGROUP, XXLGhostAction.STORY_VIEW, XXLGhostModeResolver.Override.FORCE_ON)
        assertTrue("правило типа чата важнее глобального тумблера", resolver.shouldSuppress(1L, XXLGhostScope.MEGAGROUP, XXLGhostAction.STORY_VIEW))
        assertFalse("а для PRIVATE правило MEGAGROUP не действует", resolver.shouldSuppress(1L, XXLGhostScope.PRIVATE, XXLGhostAction.STORY_VIEW))
    }

    @Test
    fun `chat override wins over everything`() {
        val resolver = XXLGhostModeResolver()
        resolver.setGlobal(XXLGhostAction.TYPING, true)
        resolver.setScopeRule(XXLGhostScope.GROUP, XXLGhostAction.TYPING, XXLGhostModeResolver.Override.FORCE_ON)
        resolver.setChatOverride(777L, XXLGhostAction.TYPING, XXLGhostModeResolver.Override.FORCE_OFF)

        assertFalse(resolver.shouldSuppress(777L, XXLGhostScope.GROUP, XXLGhostAction.TYPING))
        assertTrue(resolver.shouldSuppress(778L, XXLGhostScope.GROUP, XXLGhostAction.TYPING))
    }

    @Test
    fun `chat override can be revoked back to inheritance`() {
        val resolver = XXLGhostModeResolver()
        resolver.setGlobal(XXLGhostAction.READ_ON_SEND, true)
        resolver.setChatOverride(42L, XXLGhostAction.READ_ON_SEND, XXLGhostModeResolver.Override.FORCE_OFF)
        assertFalse(resolver.shouldSuppress(42L, XXLGhostScope.PRIVATE, XXLGhostAction.READ_ON_SEND))
        resolver.setChatOverride(42L, XXLGhostAction.READ_ON_SEND, XXLGhostModeResolver.Override.FOLLOW_GLOBAL)
        assertTrue("после сброса override снова наследует глобальный", resolver.shouldSuppress(42L, XXLGhostScope.PRIVATE, XXLGhostAction.READ_ON_SEND))
    }

    @Test
    fun `unknown dialog id and null-ish inputs never explode`() {
        val resolver = XXLGhostModeResolver()
        assertTrue(resolver.shouldSuppress(0L, XXLGhostScope.UNKNOWN, XXLGhostAction.READ_RECEIPT))
        assertFalse(resolver.shouldSuppress(0L, XXLGhostScope.UNKNOWN, XXLGhostAction.TYPING))
        // мусорные scope/action тоже разваливаются безопасно
        assertTrue(resolver.shouldSuppress(5L, null, XXLGhostAction.READ_RECEIPT))
        assertFalse(resolver.shouldSuppress(5L, XXLGhostScope.SECRET, null))
    }

    @Test
    fun `serialize and deserialize roundtrip preserves the whole matrix`() {
        val resolver = XXLGhostModeResolver()
        resolver.setGlobal(XXLGhostAction.UPLOADING, false)
        resolver.setScopeRule(XXLGhostScope.FORUM, XXLGhostAction.RECORDING, XXLGhostModeResolver.Override.FORCE_ON)
        resolver.setChatOverride(-900L, XXLGhostAction.ONLINE_STATUS, XXLGhostModeResolver.Override.FORCE_OFF)

        val raw = resolver.serialize()
        val revived = XXLGhostModeResolver.deserialize(raw)

        assertEquals(resolver.snapshot(), revived.snapshot())
        assertFalse(revived.shouldSuppress(1L, XXLGhostScope.CHANNEL, XXLGhostAction.UPLOADING))
        assertTrue(revived.shouldSuppress(1L, XXLGhostScope.FORUM, XXLGhostAction.RECORDING))
        assertFalse(revived.shouldSuppress(-900L, XXLGhostScope.CHANNEL, XXLGhostAction.ONLINE_STATUS))
    }

    @Test
    fun `deserialize tolerates complete garbage`() {
        for (raw in listOf("", "   ", "|", ";;;!!!", "read_receipt;FOREVER", "🚀✨")) {
            val revived = XXLGhostModeResolver.deserialize(raw)
            assertTrue("мусор не должен сломать дефолты: $raw", revived.shouldSuppress(1L, XXLGhostScope.PRIVATE, XXLGhostAction.READ_RECEIPT))
        }
        val fromNull = XXLGhostModeResolver.deserialize(null)
        assertEquals(XXLGhostModeResolver().snapshot(), fromNull.snapshot())
    }

    @Test
    fun `scope resolution from raw telegram flags`() {
        assertEquals(XXLGhostScope.SECRET, XXLGhostScope.of(dialogId = 100L, encrypted = true, channel = false, megagroup = false, forum = false))
        assertEquals(XXLGhostScope.PRIVATE, XXLGhostScope.of(dialogId = 100L, encrypted = false, channel = false, megagroup = false, forum = false))
        assertEquals(XXLGhostScope.FORUM, XXLGhostScope.of(dialogId = -100L, encrypted = false, channel = false, megagroup = true, forum = true))
        assertEquals(XXLGhostScope.MEGAGROUP, XXLGhostScope.of(dialogId = -100L, encrypted = false, channel = false, megagroup = true, forum = false))
        assertEquals(XXLGhostScope.CHANNEL, XXLGhostScope.of(dialogId = -100L, encrypted = false, channel = true, megagroup = false, forum = false))
        assertEquals(XXLGhostScope.GROUP, XXLGhostScope.of(dialogId = -100L, encrypted = false, channel = false, megagroup = false, forum = false))
        assertEquals(XXLGhostScope.UNKNOWN, XXLGhostScope.of(dialogId = 0L, encrypted = false, channel = false, megagroup = false, forum = false))
        assertEquals(XXLGhostScope.PRIVATE, XXLGhostScope.fromRaw("private"))
        assertEquals(XXLGhostScope.MEGAGROUP, XXLGhostScope.fromRaw(" MEGAGROUP "))
        assertEquals(XXLGhostScope.UNKNOWN, XXLGhostScope.fromRaw(null))
        assertEquals(XXLGhostScope.UNKNOWN, XXLGhostScope.fromRaw("нет такого"))
    }

    @Test
    fun `snapshot keys are stable and parseable by the debug screen`() {
        val resolver = XXLGhostModeResolver()
        resolver.setGlobal(XXLGhostAction.TYPING, true)
        val snap = resolver.snapshot()
        assertTrue(snap.isNotEmpty())
        for ((key, value) in snap) {
            assertTrue("кривой ключ: $key", key.isNotBlank())
            assertTrue("кривое значение: $value", value == "FORCE_ON" || value == "FORCE_OFF" || value == "FOLLOW_GLOBAL")
        }
    }
}
