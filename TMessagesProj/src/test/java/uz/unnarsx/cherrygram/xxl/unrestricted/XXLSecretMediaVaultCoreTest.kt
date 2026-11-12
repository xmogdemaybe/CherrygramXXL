/**
 * 🚀✨ Enterprise Quantum Unit Test for the Secret Media Vault (§2D view-once выкуп) 💯
 * Verifies: ttl-семантика, маппинг классов медиа, sanitizer имён, dedupe/cap, roundtrip, null-safety.
 */
package uz.unnarsx.cherrygram.xxl.unrestricted

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.unnarsx.cherrygram.xxl.unrestricted.core.XXLSecretMediaVault
import uz.unnarsx.cherrygram.xxl.unrestricted.core.XXLSecretMediaVaultCore

class XXLSecretMediaVaultCoreTest {

    private fun entry(
        dialog: Long = -1001L,
        msg: Int = 42,
        media: Long = 777L,
        kind: XXLSecretMediaVaultCore.Kind = XXLSecretMediaVaultCore.Kind.PHOTO,
        ttl: Int = XXLSecretMediaVaultCore.VIEW_ONCE_TTL,
        name: String = "cherryxxl_secret_-1001_42_photo.jpg",
        epoch: Long = 1000L
    ) = XXLSecretMediaVaultCore.Entry(dialog, msg, epoch, ttl, media, kind, name)

    @Test
    fun `ttl semantics decide what must be rescued`() {
        assertTrue(XXLSecretMediaVaultCore.shouldPreserve(60))
        assertTrue(XXLSecretMediaVaultCore.shouldPreserve(XXLSecretMediaVaultCore.VIEW_ONCE_TTL))
        assertFalse("обычное медиа не наша добыча", XXLSecretMediaVaultCore.shouldPreserve(0))
        assertTrue(XXLSecretMediaVaultCore.isViewOnce(XXLSecretMediaVaultCore.VIEW_ONCE_TTL))
        assertFalse("на единицу ниже маркера — уже обычный ephemeral", XXLSecretMediaVaultCore.isViewOnce(XXLSecretMediaVaultCore.VIEW_ONCE_TTL - 1))
        assertFalse(XXLSecretMediaVaultCore.isViewOnce(5))
        assertTrue(entry(ttl = XXLSecretMediaVaultCore.VIEW_ONCE_TTL).isViewOnce())
    }

    @Test
    fun `media class mapper reads stock constructor names`() {
        assertEquals(XXLSecretMediaVaultCore.Kind.PHOTO, XXLSecretMediaVaultCore.kindFor("TL_messageMediaPhoto"))
        assertEquals(XXLSecretMediaVaultCore.Kind.DOCUMENT, XXLSecretMediaVaultCore.kindFor("messageMediaDocument"))
        assertEquals(XXLSecretMediaVaultCore.Kind.UNKNOWN, XXLSecretMediaVaultCore.kindFor("TL_messageMediaWebPage"))
        assertEquals(XXLSecretMediaVaultCore.Kind.UNKNOWN, XXLSecretMediaVaultCore.kindFor(null))
        assertEquals(XXLSecretMediaVaultCore.Kind.UNKNOWN, XXLSecretMediaVaultCore.kindFor("  "))

        assertEquals(
            XXLSecretMediaVaultCore.Kind.ROUND_VIDEO,
            XXLSecretMediaVaultCore.kindFor(isRoundVideo = true, isVoice = true, isVideo = true, isPhoto = true)
        )
        assertEquals(XXLSecretMediaVaultCore.Kind.VOICE, XXLSecretMediaVaultCore.kindFor(false, true, true, true))
        assertEquals(XXLSecretMediaVaultCore.Kind.VIDEO, XXLSecretMediaVaultCore.kindFor(false, false, true, true))
        assertEquals(XXLSecretMediaVaultCore.Kind.PHOTO, XXLSecretMediaVaultCore.kindFor(false, false, false, true))
        assertEquals(XXLSecretMediaVaultCore.Kind.DOCUMENT, XXLSecretMediaVaultCore.kindFor(false, false, false, false))
    }

    @Test
    fun `extensions follow the kind`() {
        assertEquals("jpg", XXLSecretMediaVaultCore.extensionFor(XXLSecretMediaVaultCore.Kind.PHOTO))
        assertEquals("mp4", XXLSecretMediaVaultCore.extensionFor(XXLSecretMediaVaultCore.Kind.ROUND_VIDEO))
        assertEquals("ogg", XXLSecretMediaVaultCore.extensionFor(XXLSecretMediaVaultCore.Kind.VOICE))
        assertEquals("bin", XXLSecretMediaVaultCore.extensionFor(null))
        assertEquals("bin", XXLSecretMediaVaultCore.extensionFor(XXLSecretMediaVaultCore.Kind.UNKNOWN))
    }

    @Test
    fun `deterministic file names are collision free per message`() {
        val first = XXLSecretMediaVaultCore.fileNameFor(-1001L, 42, XXLSecretMediaVaultCore.Kind.PHOTO)
        assertEquals("cherryxxl_secret_-1001_42_photo.jpg", first)
        assertEquals(first, XXLSecretMediaVaultCore.fileNameFor(-1001L, 42, XXLSecretMediaVaultCore.Kind.PHOTO))
        assertFalse(first, XXLSecretMediaVaultCore.fileNameFor(-1001L, 43, XXLSecretMediaVaultCore.Kind.PHOTO) == first)
        assertEquals(
            "cherryxxl_secret_-2_9_voice.ogg",
            XXLSecretMediaVaultCore.fileNameFor(-2L, 9, XXLSecretMediaVaultCore.Kind.VOICE)
        )
        assertTrue(XXLSecretMediaVaultCore.fileNameFor(1L, 1, null).endsWith(".bin"))
    }

    @Test
    fun `sanitizer kills path traversal and control junk`() {
        assertEquals("passwd", XXLSecretMediaVaultCore.sanitize("../../../etc/passwd", null, 64).substringBefore('.'))
        val evil = XXLSecretMediaVaultCore.sanitize("../../vendor/x86_64/lib\rt.so", null, 64)
        assertFalse(evil, evil.contains("/"))
        assertFalse(evil, evil.contains(".."))
        assertFalse(evil, evil.contains("\r"))
        assertTrue(evil, evil.endsWith(".so"))

        assertEquals("cherryxxl_secret", XXLSecretMediaVaultCore.sanitize(null, null, 64).substringBefore('.'))
        assertEquals("cherryxxl_secret.bin", XXLSecretMediaVaultCore.sanitize("   ", "", 64))
        assertEquals("my_file.jpg", XXLSecretMediaVaultCore.sanitize("my  file.jpg", null, 64))
        assertEquals("report.final.v2.pdf", XXLSecretMediaVaultCore.sanitize("report.final.v2.pdf", null, 64))
        assertEquals("x.bin", XXLSecretMediaVaultCore.sanitize("x.", null, 64))
        assertEquals("fallback.png", XXLSecretMediaVaultCore.sanitize(null, "fallback.png", 64))
        assertEquals("underscored.mp4", XXLSecretMediaVaultCore.sanitize("_underscored_.mp4", null, 64))
    }

    @Test
    fun `sanitizer cap preserves the extension`() {
        val long = "a".repeat(200) + ".mp4"
        val short = XXLSecretMediaVaultCore.sanitize(long, null, 32)
        assertEquals(32, short.length.toLong())
        assertTrue(short, short.endsWith(".mp4"))
        val extreme = XXLSecretMediaVaultCore.sanitize(long, null, 2)
        assertTrue(extreme, extreme.length >= 4)
    }

    @Test
    fun `vault dedupes by signature and never stores junk`() {
        val vault = XXLSecretMediaVault()
        assertTrue(vault.add(entry()))
        assertFalse("та же ссылка уже в архиве", vault.add(entry()))
        assertTrue("другое медиа — новая запись", vault.add(entry(media = 778L)))
        assertFalse(vault.add(null))
        assertFalse(vault.add(entry(dialog = 0L)))
        assertFalse(vault.add(entry(msg = 0)))
        assertFalse(vault.add(entry(name = "   ")))
        assertEquals(2, vault.total().toLong())
        assertEquals(2, vault.entriesOf(-1001L).size.toLong())
        assertNotNull(vault.latest())
        assertTrue(vault.describe().startsWith("vault=2"))
    }

    @Test
    fun `vault cap evicts oldest first`() {
        val vault = XXLSecretMediaVault(maxEntries = 4)
        for (i in 1..10) {
            vault.add(entry(msg = i, media = i.toLong(), epoch = 1000L + i))
        }
        assertEquals(4, vault.total().toLong())
        assertTrue(vault.entriesOf(-1001L).first().messageId >= 7)
    }

    @Test
    fun `vault roundtrip and garbage tolerance`() {
        val vault = XXLSecretMediaVault()
        vault.add(entry(kind = XXLSecretMediaVaultCore.Kind.PHOTO, name = "первый | файл.jpg"))
        vault.add(entry(msg = 43, media = 778L, kind = XXLSecretMediaVaultCore.Kind.VOICE, name = "голосовое.ogg"))
        val revived = XXLSecretMediaVault.deserialize(vault.serialize())
        assertEquals(2, revived.total().toLong())
        assertEquals("первый | файл.jpg", revived.entriesOf(-1001L)[0].fileName)
        assertEquals(XXLSecretMediaVaultCore.Kind.VOICE, revived.entriesOf(-1001L)[1].kind)

        assertEquals(0, XXLSecretMediaVault.deserialize(null).total().toLong())
        assertEquals(0, XXLSecretMediaVault.deserialize("").total().toLong())
        assertEquals(0, XXLSecretMediaVault.deserialize("мусор").total().toLong())
        assertEquals(0, XXLSecretMediaVault.deserialize("мусор;еще мусор").total().toLong())
        assertEquals(
            0,
            XXLSecretMediaVault.deserialize("-1001|42|1000|5|7|PHOTO").total().toLong()
        )
    }

    @Test
    fun `search matches name kind and dialog`() {
        val vault = XXLSecretMediaVault()
        vault.add(entry(name = "секретное_фото.jpg"))
        vault.add(entry(msg = 44, media = 779L, kind = XXLSecretMediaVaultCore.Kind.VOICE, name = "голос.ogg"))
        assertEquals(1, vault.search("фото").size.toLong())
        assertEquals(1, vault.search("VOICE").size.toLong())
        assertEquals(2, vault.search("-1001").size.toLong())
        assertTrue(vault.search(null).isEmpty())
        assertTrue(vault.search("  ").isEmpty())
        assertTrue(vault.search("несуществующее").isEmpty())
    }

    @Test
    fun `signature and describe are null safe`() {
        assertEquals("", XXLSecretMediaVaultCore.signatureOf(null))
        assertEquals("-1001:42:777:PHOTO", XXLSecretMediaVaultCore.signatureOf(entry()))
        assertTrue(entry().describe().contains("чат -1001"))
        assertTrue(entry().describe().contains("ttl="))
    }
}
