/**
 * 🚀✨ Enterprise Quantum Unit Test for Settings Fragmentation (§2C) 💯
 * Одна фича обязана быть раскидана ровно по 4 разным подменю.
 */
package uz.unnarsx.cherrygram.xxl.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.unnarsx.cherrygram.xxl.config.core.XXLVibeFeature
import uz.unnarsx.cherrygram.xxl.config.core.XXLVibeMenuScatter

class XXLVibeMenuScatterTest {

    @Test
    fun `there are exactly four cursed sub menus`() {
        val menus = XXLVibeMenuScatter.menus()
        assertEquals(4, menus.size)
        assertEquals(
            listOf("Вайб-Настройки", "Ультра-Экспериментальные", "Секретная База", "Наследие Черри"),
            menus.map { it.title }
        )
        assertEquals(
            listOf("vibe_settings", "ultra_experimental", "secret_base", "cherry_legacy"),
            menus.map { it.slug }
        )
    }

    @Test
    fun `commit 13 - each feature has one home menu and the four screens partition all features`() {
        val union = ArrayList<XXLVibeFeature>()
        for (menu in XXLVibeMenuScatter.menus()) {
            val features = XXLVibeMenuScatter.featuresOf(menu)
            assertTrue("меню ${menu.slug} пустое", features.isNotEmpty())
            union.addAll(features)
        }
        assertEquals("партиция должна покрывать все фичи ровно один раз", XXLVibeFeature.ALL.size, union.size)
        assertEquals(XXLVibeFeature.ALL.toSet(), union.toSet())
        for (feature in XXLVibeFeature.ALL) {
            assertEquals(1, XXLVibeMenuScatter.scatter(feature).size)
            assertTrue(XXLVibeMenuScatter.isScatteredEnough(feature))
        }
    }

    @Test
    fun `row ids are unique per menu and never collide`() {
        val seen = HashSet<Int>()
        for (feature in XXLVibeFeature.ALL) {
            for (menu in XXLVibeMenuScatter.menus()) {
                val rowId = XXLVibeMenuScatter.rowId(feature, menu)
                assertTrue("rowId вне диапазона: $rowId", rowId in 1..9999)
                assertTrue("коллизия rowId $rowId", seen.add(rowId))
            }
        }
        assertEquals((XXLVibeFeature.ALL.size * 4).toLong(), seen.size.toLong())
    }

    @Test
    fun `row ids are deterministic across calls`() {
        val a = XXLVibeMenuScatter.rowId(XXLVibeFeature.MEMORIAL, XXLVibeMenuScatter.Menu.SECRET_BASE)
        val b = XXLVibeMenuScatter.rowId(XXLVibeFeature.MEMORIAL, XXLVibeMenuScatter.Menu.SECRET_BASE)
        assertEquals(a, b)
    }

    @Test
    fun `titles differ per menu so the user really is lost`() {
        val titles = XXLVibeMenuScatter.menus().map { XXLVibeMenuScatter.titleFor(XXLVibeFeature.MEMORIAL, it) }
        assertEquals(4, titles.toSet().size)
        for (title in titles) {
            assertTrue("пустой заголовок", title.isNotBlank())
            assertTrue("в заголовке нет имени фичи: $title", title.contains("НАПОМИНАНИЕ"))
        }
    }

    @Test
    fun `menu lookup by slug is forgiving`() {
        assertNotNull(XXLVibeMenuScatter.menuFor("secret_base"))
        assertNotNull(XXLVibeMenuScatter.menuFor("  SECRET_BASE  "))
        assertNotNull(XXLVibeMenuScatter.menuFor("cherry-legacy"))
        assertNull(XXLVibeMenuScatter.menuFor(null))
        assertNull(XXLVibeMenuScatter.menuFor(""))
        assertNull(XXLVibeMenuScatter.menuFor("нет такой папки"))
    }

    @Test
    fun `features of a menu and menu of a row agree`() {
        for (menu in XXLVibeMenuScatter.menus()) {
            val features = XXLVibeMenuScatter.featuresOf(menu)
            for (feature in features) {
                assertEquals("фича ${feature.key} не в своём меню", menu, XXLVibeMenuScatter.homeMenu(feature))
                assertEquals(menu, XXLVibeMenuScatter.menuOfRow(XXLVibeMenuScatter.rowId(feature, menu)))
            }
        }
    }

    @Test
    fun `unknown row id resolves to nothing instead of crashing`() {
        assertNull(XXLVibeMenuScatter.menuOfRow(-1))
        assertNull(XXLVibeMenuScatter.menuOfRow(0))
        assertNull(XXLVibeMenuScatter.menuOfRow(999_999))
        assertFalse(XXLVibeMenuScatter.isScatteredEnough(null))
        assertEquals(0, XXLVibeMenuScatter.scatter(null).size.toLong())
    }
}
