package uz.unnarsx.cherrygram.xxl.memorial.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Коммит 17: матчинг Арслана (@arslan/arslan/arslan4k1390, t.me/tg:// ссылки, регистронезависимо,
 * ID владельца) и стейт-машина флага «краш → показ при следующем запуске → сброс».
 */
class XXLArslanProfileTrapCoreTest {

    @Test
    fun `матчит юзернеймы без учёта регистра и собаки`() {
        assertTrue(XXLArslanProfileTrapCore.isArslanUsername("arslan"))
        assertTrue(XXLArslanProfileTrapCore.isArslanUsername("@arslan"))
        assertTrue(XXLArslanProfileTrapCore.isArslanUsername("@ARSLAN"))
        assertTrue(XXLArslanProfileTrapCore.isArslanUsername("Arslan4k1390"))
        assertFalse(XXLArslanProfileTrapCore.isArslanUsername("arslan_fan"))
        assertFalse(XXLArslanProfileTrapCore.isArslanUsername(null))
        assertFalse(XXLArslanProfileTrapCore.isArslanUsername(""))
    }

    @Test
    fun `матчит ссылки t_me и tg_resolve`() {
        assertTrue(XXLArslanProfileTrapCore.isArslanUrl("https://t.me/arslan"))
        assertTrue(XXLArslanProfileTrapCore.isArslanUrl("https://t.me/ARSLAN"))
        assertTrue(XXLArslanProfileTrapCore.isArslanUrl("tg://resolve?domain=arslan"))
        assertTrue(XXLArslanProfileTrapCore.isArslanUrl("tg://resolve?domain=arslan4k1390"))
        assertFalse(XXLArslanProfileTrapCore.isArslanUrl("https://t.me/durov"))
        assertFalse(XXLArslanProfileTrapCore.isArslanUrl("https://example.com/arslan"))
        assertFalse(XXLArslanProfileTrapCore.isArslanUrl(null))
    }

    @Test
    fun `матчит ID владельца`() {
        assertTrue(XXLArslanProfileTrapCore.isArslanUserId(282287840L))
        assertFalse(XXLArslanProfileTrapCore.isArslanUserId(123L))
        assertFalse(XXLArslanProfileTrapCore.isArslanUserId(0L))
    }

    @Test
    fun `детект объединяет username id url`() {
        assertTrue(XXLArslanProfileTrapCore.detect("arslan", null, null))
        assertTrue(XXLArslanProfileTrapCore.detect(null, 282287840L, null))
        assertTrue(XXLArslanProfileTrapCore.detect(null, null, "https://t.me/arslan"))
        assertFalse(XXLArslanProfileTrapCore.detect("durov", 1L, "https://t.me/durov"))
        assertFalse(XXLArslanProfileTrapCore.detect(null, null, null))
    }

    @Test
    fun `стейт-машина флага краш-показ-сброс без бутлупа`() {
        assertEquals(XXLArslanProfileTrapCore.NextLaunch.NONE, XXLArslanProfileTrapCore.nextLaunch(false))
        assertEquals(XXLArslanProfileTrapCore.NextLaunch.SHOW_BANNER_AND_RESET, XXLArslanProfileTrapCore.nextLaunch(true))
        // после показа флаг сброшен -> следующий запуск чистый
        assertEquals(XXLArslanProfileTrapCore.NextLaunch.NONE, XXLArslanProfileTrapCore.nextLaunch(false))
    }
}
