/**
 * 🚀✨ Enterprise Quantum Unit Test for the XXL rebranding rename rule 💯
 * Правило: видимое имя «Cherrygram» -> «CherrygramXXL», но технические идентификаторы
 * (URL, юзернеймы каналов, package name, URL-хосты) не трогаем НИКОГДА.
 */
package uz.unnarsx.cherrygram.xxl.brand

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import uz.unnarsx.cherrygram.xxl.brand.core.XXLBrandNames
import uz.unnarsx.cherrygram.xxl.brand.core.XXLBrandRenamer

class XXLBrandRenamerTest {

    @Test
    fun `visible product name is renamed in prose`() {
        assertEquals(
            "Join the channel to stay up to date with all the news and features of CherrygramXXL!",
            XXLBrandRenamer.rename("Join the channel to stay up to date with all the news and features of Cherrygram!")
        )
        assertEquals(
            "Присоединяйтесь к каналу, чтобы быть в курсе всех новостей и возможностей CherrygramXXL!",
            XXLBrandRenamer.rename("Присоединяйтесь к каналу, чтобы быть в курсе всех новостей и возможностей Cherrygram!")
        )
        assertEquals(
            "CherrygramXXL нужен доступ к вашему местоположению",
            XXLBrandRenamer.rename("Cherrygram нужен доступ к вашему местоположению")
        )
        assertEquals(
            "CherrygramXXL не несет ответственности",
            XXLBrandRenamer.rename("Cherrygram не несет ответственности")
        )
    }

    @Test
    fun `english possessive and sentence-final dot are still prose`() {
        assertEquals("CherrygramXXL's features", XXLBrandRenamer.rename("Cherrygram's features"))
        assertEquals("features of CherrygramXXL.", XXLBrandRenamer.rename("features of Cherrygram."))
        assertEquals("CherrygramXXL, ", XXLBrandRenamer.rename("Cherrygram, "))
    }

    @Test
    fun `cjk prose touching the name has no spaces and must still be renamed`() {
        // Реальные строки из values-ja / values-zh-rCN: в CJK нет пробелов,
        // поэтому «буква слева/справа» не равна техническому идентификатору.
        val cases = listOf(
            "随时了解Cherrygram的所有新闻和功能更新！" to "随时了解CherrygramXXL的所有新闻和功能更新！",
            "この機能はCherrygramのパスワードなしでアクセス" to "この機能はCherrygramXXLのパスワードなしでアクセス",
            "Cherrygramが動作しなくなりました" to "CherrygramXXLが動作しなくなりました",
            "Cherrygramを気に入ってますか?" to "CherrygramXXLを気に入ってますか?"
        )
        for ((input, expected) in cases) {
            val once = XXLBrandRenamer.rename(input)
            assertEquals("CJK проза: $input", expected, once)
            assertEquals("CJK идемпотентность: $input", once, XXLBrandRenamer.rename(once))
        }
    }

    @Test
    fun `german hyphen compounds are prose not identifiers`() {
        // Реальные строки из values-de/cg_strings.xml: дефис в немецком слове — это НЕ handle.
        assertEquals(
            "CherrygramXXL-Einstellungen",
            XXLBrandRenamer.rename("Cherrygram-Einstellungen")
        )
        assertEquals(
            "ohne das CherrygramXXL-Passwort zugegriffen werden",
            XXLBrandRenamer.rename("ohne das Cherrygram-Passwort zugegriffen werden")
        )
        assertEquals(
            "(CherrygramXXL-)",
            XXLBrandRenamer.rename("(Cherrygram-)")
        )
        val once = XXLBrandRenamer.rename("Cherrygram-Einstellungen")
        assertEquals("неидемпотентно на немецком дефисе", once, XXLBrandRenamer.rename(once))
    }

    @Test
    fun `technical identifiers must never be touched`() {
        val untouchables = listOf(
            "https://t.me/CherrygramBetaAPKs/2810",
            "https://t.me/CherrygramSupport",
            "https://t.me/cherrygram",
            "https://github.com/arsLan4k1390/Cherrygram",
            "https://crowdin.com/project/cherrygram",
            "https://arslan4k1390.github.io/cherrygram/privacy",
            "Updates: @CherrygramAPKs",
            "@CherrygramOfftopic",
            "uz.unnarsx.cherrygram",
            "uz.unnarsx.cherrygram.CG_Icon_Cherry",
            "vnd.android.cursor.item/vnd.uz.unnarsx.cherrygram.android.profile",
            "CherrygramAPKs",
            "CherrygramBetaAPKs",
            "CG_CherrygramChannel"
        )
        for (raw in untouchables) {
            assertEquals("сломанный идентификатор: $raw", raw, XXLBrandRenamer.rename(raw))
        }
    }

    @Test
    fun `mixed strings rename only the prose part`() {
        assertEquals(
            "Наш канал: https://t.me/CherrygramSupport — CherrygramXXL не при чём",
            XXLBrandRenamer.rename("Наш канал: https://t.me/CherrygramSupport — Cherrygram не при чём")
        )
        assertEquals(
            "<b>CherrygramXXL</b> обновлён",
            XXLBrandRenamer.rename("<b>Cherrygram</b> обновлён")
        )
    }

    @Test
    fun `rename is idempotent so we never get CherrygramXXLXXL`() {
        val samples = listOf(
            "Cherrygram",
            "features of Cherrygram!",
            "Cherrygram's features",
            "https://t.me/CherrygramBetaAPKs/2810",
            "CherrygramXXL уже было портировано",
            "Привет Cherrygram. Пока Cherrygram!"
        )
        for (s in samples) {
            val once = XXLBrandRenamer.rename(s)
            assertEquals("неидемпотентно на: $s", once, XXLBrandRenamer.rename(once))
        }
    }

    @Test
    fun `null and blank inputs are safe`() {
        assertNull(XXLBrandRenamer.rename(null))
        assertEquals("", XXLBrandRenamer.rename(""))
        assertEquals("   ", XXLBrandRenamer.rename("   "))
        assertEquals("Cherry", XXLBrandRenamer.rename("Cherry"))
        assertEquals("cherrygram в нижнем регистре остаётся как есть", XXLBrandRenamer.rename("cherrygram в нижнем регистре остаётся как есть"))
    }

    @Test
    fun `brand names expose the XXL identity and per-flavor variants`() {
        assertEquals("CherrygramXXL", XXLBrandNames.APP_NAME)
        assertEquals("CherrygramXXL", XXLBrandNames.displayName(null))
        assertEquals("CherrygramXXL Beta", XXLBrandNames.displayName("beta"))
        assertEquals("CherrygramXXL Premium", XXLBrandNames.displayName("premium"))
        assertEquals("CherrygramXXL Dev", XXLBrandNames.displayName("dev"))
        assertEquals("CherrygramXXL", XXLBrandNames.displayName("STABLE"))
        assertEquals("CherrygramXXL", XXLBrandNames.displayName("  "))
        assertEquals("uz.unnarsx.cherrygramxxl", XXLBrandNames.applicationId())
        assertEquals("CherrygramXXL", XXLBrandNames.rebrandLegacy("Cherrygram"))
        assertSame(XXLBrandNames.APP_NAME, XXLBrandNames.displayName("stable"))
    }
}
