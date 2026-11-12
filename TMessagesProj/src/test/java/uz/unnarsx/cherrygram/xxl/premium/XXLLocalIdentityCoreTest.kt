/**
 * 🚀✨ Enterprise Quantum Unit Test for Local Identities (§2D «локальные бейджи») 💯
 * Verifies: алиасы идентичностей, store-дедуп/кап, NONE = снятие, цвета из палитры, roundtrip.
 */
package uz.unnarsx.cherrygram.xxl.premium

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.unnarsx.cherrygram.xxl.premium.core.XXLLocalIdentityCore

class XXLLocalIdentityCoreTest {

    @Test
    fun `identity resolver reads ids titles and cyrillic aliases`() {
        assertEquals(XXLLocalIdentityCore.Identity.RAT, XXLLocalIdentityCore.identityFor("rat", null))
        assertEquals(XXLLocalIdentityCore.Identity.RAT, XXLLocalIdentityCore.identityFor("  RAT ", null))
        assertEquals(XXLLocalIdentityCore.Identity.RAT, XXLLocalIdentityCore.identityFor("крыса", null))
        assertEquals(XXLLocalIdentityCore.Identity.ADMIN, XXLLocalIdentityCore.identityFor("👑", null))
        assertEquals(XXLLocalIdentityCore.Identity.GHOST, XXLLocalIdentityCore.identityFor("ghost", null))
        assertEquals(XXLLocalIdentityCore.Identity.NONE, XXLLocalIdentityCore.identityFor("none", null))
        assertEquals(XXLLocalIdentityCore.Identity.NONE, XXLLocalIdentityCore.identityFor(null, null))
        assertEquals(XXLLocalIdentityCore.Identity.BEAR, XXLLocalIdentityCore.identityFor("нет такого", XXLLocalIdentityCore.Identity.BEAR))
    }

    @Test
    fun `identities carry a day and night color each`() {
        for (identity in XXLLocalIdentityCore.Identity.values()) {
            assertEquals(identity.name, identity, XXLLocalIdentityCore.identityFor(identity.id, null))
            val day: Int = XXLLocalIdentityCore.colorFor(identity, true, 0)
            val night: Int = XXLLocalIdentityCore.colorFor(identity, false, 0)
            if (identity == XXLLocalIdentityCore.Identity.NONE) {
                assertEquals(identity.name, 0L, day.toLong())
                assertEquals(identity.name, 0L, night.toLong())
                assertEquals(identity.name, "", identity.badge)
            } else {
                assertTrue(identity.name, day != 0)
                assertTrue(identity.name, night != 0)
                assertTrue(identity.name, identity.badge.isNotEmpty())
            }
            assertEquals(identity.name, identity.premium, XXLLocalIdentityCore.isPremiumIdentity(identity))
            assertEquals(identity.name, identity != XXLLocalIdentityCore.Identity.NONE, XXLLocalIdentityCore.isBadgeVisible(identity))
        }
        assertEquals(0xFF000000.toInt(), XXLLocalIdentityCore.colorFor(XXLLocalIdentityCore.Identity.RAT, true, 0xFF000000.toInt()))
        assertEquals(0x112233, XXLLocalIdentityCore.colorFor(null, true, 0x112233))
    }

    @Test
    fun `store registers replaces and refuses junk`() {
        val store = XXLLocalIdentityCore.Store()
        assertFalse(store.register(0L, XXLLocalIdentityCore.Identity.RAT, "мне"))
        assertFalse(store.register(-7L, XXLLocalIdentityCore.Identity.RAT, null))
        assertTrue(store.register(777L, XXLLocalIdentityCore.Identity.RAT, "крыса"))
        assertEquals(1, store.total().toLong())
        assertTrue(store.register(777L, XXLLocalIdentityCore.Identity.ADMIN, "теперь админ"))
        assertEquals("повтор не плодит дублей", 1, store.total().toLong())
        val found = store.lookup(777L)
        assertNotNull(found)
        assertEquals(XXLLocalIdentityCore.Identity.ADMIN, found!!.identity)
        assertEquals("теперь админ", found.note)
        assertNull(store.lookup(1L))
        assertFalse(store.lookup(777L)!!.caption().isEmpty())
    }

    @Test
    fun `NONE removes the registration and reports the truth`() {
        val store = XXLLocalIdentityCore.Store()
        store.register(777L, XXLLocalIdentityCore.Identity.GHOST, "бу")
        assertTrue(store.remove(777L))
        assertFalse(store.remove(777L))
        assertEquals(0, store.total().toLong())
        store.register(777L, XXLLocalIdentityCore.Identity.GHOST, "бу")
        assertTrue("NONE = снятие", store.register(777L, XXLLocalIdentityCore.Identity.NONE, null))
        assertEquals(0, store.total().toLong())
        assertFalse(store.register(777L, null, null))
    }

    @Test
    fun `cap evicts the oldest registration`() {
        val store = XXLLocalIdentityCore.Store(maxEntries = 3)
        for (i in 1..6) store.register(i.toLong(), XXLLocalIdentityCore.Identity.SPONSOR, "n$i")
        assertEquals(3, store.total().toLong())
        assertNull(store.lookup(1))
        assertNotNull(store.lookup(6))
        assertEquals(listOf(4L, 5L, 6L), store.userIds())
    }

    @Test
    fun `serialize roundtrip and garbage tolerance`() {
        val store = XXLLocalIdentityCore.Store()
        store.register(777L, XXLLocalIdentityCore.Identity.RAT, "заметка | с пайпом")
        store.register(888L, XXLLocalIdentityCore.Identity.ADMIN, "две\nстроки")
        val revived = XXLLocalIdentityCore.Store.deserialize(store.serialize())
        assertEquals(2, revived.total().toLong())
        assertEquals("заметка | с пайпом", revived.lookup(777L)?.note)
        assertEquals(XXLLocalIdentityCore.Identity.ADMIN, revived.lookup(888L)?.identity)
        assertEquals(store.serialize(), revived.serialize())

        assertEquals(0, XXLLocalIdentityCore.Store.deserialize(null).total().toLong())
        assertEquals(0, XXLLocalIdentityCore.Store.deserialize("").total().toLong())
        assertEquals(0, XXLLocalIdentityCore.Store.deserialize("мусор").total().toLong())
        assertEquals(0, XXLLocalIdentityCore.Store.deserialize("0|rat|1|note").total().toLong())
        assertEquals(0, XXLLocalIdentityCore.Store.deserialize("777|неизвестный|1|note").total().toLong())
        assertEquals(0, XXLLocalIdentityCore.Store.deserialize("777|none|1|note").total().toLong())
    }

    @Test
    fun `dump is human and sorted for stable screenshots`() {
        val store = XXLLocalIdentityCore.Store()
        store.register(900L, XXLLocalIdentityCore.Identity.BEAR, "медведь")
        store.register(100L, XXLLocalIdentityCore.Identity.RAT, "крыса")
        val dump: List<String> = store.dump()
        assertEquals(2, dump.size.toLong())
        assertTrue(dump[0], dump[0].startsWith("100"))
        assertTrue(dump[1], dump[1].startsWith("900"))
        assertTrue(dump[0].contains("КРЫСА") || dump[0].contains("крыса"))
        assertTrue(store.describe().startsWith("identities=2"))
        store.clear()
        assertEquals(0, store.total().toLong())
        assertTrue(store.dump().isEmpty())
        assertTrue(store.describe().contains("identities=0"))
    }

    @Test
    fun `caption marks the premium identities`() {
        assertTrue(XXLLocalIdentityCore.captionFor(XXLLocalIdentityCore.Identity.RAT).contains("🐀"))
        assertEquals("", XXLLocalIdentityCore.captionFor(XXLLocalIdentityCore.Identity.NONE))
        assertEquals("", XXLLocalIdentityCore.captionFor(null))
        for (identity in XXLLocalIdentityCore.Identity.values()) {
            if (identity == XXLLocalIdentityCore.Identity.NONE) continue
            assertTrue(identity.name, XXLLocalIdentityCore.captionFor(identity).isNotBlank())
            assertTrue(identity.name, XXLLocalIdentityCore.isBadgeVisible(identity))
        }
        assertFalse(XXLLocalIdentityCore.isBadgeVisible(XXLLocalIdentityCore.Identity.NONE))
        assertFalse(XXLLocalIdentityCore.isBadgeVisible(null))
    }
}
