/**
 * 🚀✨ Enterprise Quantum Unit Test for meme asset loading (§2D) 💯
 * Ядро не знает про BitmapFactory: оно работает с инъектируемым потокодасте и
 * честно отдаёт цветную fallback-заставку, когда ассета нет (как в юнит-тестах).
 */
package uz.unnarsx.cherrygram.xxl.meme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.unnarsx.cherrygram.xxl.meme.core.XXLMemeAssetSource
import java.io.ByteArrayInputStream
import java.io.InputStream

class XXLMemeAssetSourceTest {

    private class FakeProvider(
        private val files: Map<String, ByteArray>,
        private val throwOnOpen: Boolean = false
    ) : XXLMemeAssetSource.StreamProvider {
        var opens: Int = 0
        override fun open(name: String?): InputStream? {
            opens++
            if (throwOnOpen) throw IllegalStateException("asset explosion")
            if (name == null) return null
            val bytes = files[name] ?: return null
            return ByteArrayInputStream(bytes)
        }
    }

    private val pngish = byteArrayOf(-1, -48, -1, -32, 13, 10, 26, 7)

    @Test
    fun `the two mandated meme assets are known`() {
        assertEquals("amdfx.jpg", XXLMemeAssetSource.AMD_FX_ASSET)
        assertEquals("svo.jpg", XXLMemeAssetSource.SVO_ASSET)
        assertEquals(listOf("amdfx.jpg", "svo.jpg"), XXLMemeAssetSource.knownAssets())
        assertTrue(XXLMemeAssetSource.isKnown("amdfx.jpg"))
        assertTrue(XXLMemeAssetSource.isKnown("svo.jpg"))
        assertFalse(XXLMemeAssetSource.isKnown("nope.jpg"))
        assertFalse(XXLMemeAssetSource.isKnown(null))
        assertFalse(XXLMemeAssetSource.isKnown(""))
    }

    @Test
    fun `normalization strips path noise and case`() {
        assertEquals("amdfx.jpg", XXLMemeAssetSource.normalize("  assets/AMDfx.JPG "))
        assertEquals("svo.jpg", XXLMemeAssetSource.normalize("./assets/SVO.jpg"))
        assertNull(XXLMemeAssetSource.normalize(null))
        assertNull(XXLMemeAssetSource.normalize("    "))
    }

    @Test
    fun `fallback colors are acid and assigned per asset`() {
        assertEquals(XXLMemeAssetSource.fallbackColorFor("amdfx.jpg"), XXLMemeAssetSource.AMD_FX_FALLBACK_COLOR)
        assertEquals(XXLMemeAssetSource.fallbackColorFor("svo.jpg"), XXLMemeAssetSource.SVO_FALLBACK_COLOR)
        assertEquals(XXLMemeAssetSource.fallbackColorFor("мусор"), XXLMemeAssetSource.UNKNOWN_FALLBACK_COLOR)
        assertEquals(XXLMemeAssetSource.fallbackColorFor(null), XXLMemeAssetSource.UNKNOWN_FALLBACK_COLOR)
        for (color in listOf(
            XXLMemeAssetSource.AMD_FX_FALLBACK_COLOR,
            XXLMemeAssetSource.SVO_FALLBACK_COLOR,
            XXLMemeAssetSource.UNKNOWN_FALLBACK_COLOR
        )) {
            assertTrue("цвет без непрозрачной альфы: $color", ((color shr 24) and 0xFF) == 0xFF)
        }
    }

    @Test
    fun `decode returns the decoder result when the asset exists`() {
        val provider = FakeProvider(mapOf("amdfx.jpg" to pngish))
        val decoded = XXLMemeAssetSource.decode("amdfx.jpg", provider, { stream -> stream.readBytes().size }, -1)
        assertEquals(pngish.size, decoded)
        assertEquals(1, provider.opens)
    }

    @Test
    fun `missing asset falls back without crashing`() {
        val provider = FakeProvider(emptyMap())
        val decoded = XXLMemeAssetSource.decode("svo.jpg", provider, { stream -> stream.readBytes().size }, -7)
        assertEquals("нет ассета -> fallback без чтения", -7, decoded)
        assertEquals(1, provider.opens)

        assertEquals(-7, XXLMemeAssetSource.decode("garbage.jpg", provider, { it.readBytes().size }, -7))
        assertEquals(-7, XXLMemeAssetSource.decode(null, provider, { it.readBytes().size }, -7))
        assertEquals(-7, XXLMemeAssetSource.decode("   ", provider, { it.readBytes().size }, -7))
    }

    @Test
    fun `exploding provider or decoder falls back instead of crashing`() {
        val exploding = FakeProvider(mapOf("amdfx.jpg" to pngish), throwOnOpen = true)
        assertEquals(-9, XXLMemeAssetSource.decode("amdfx.jpg", exploding, { it.readBytes().size }, -9))

        val nullDecoder = FakeProvider(mapOf("amdfx.jpg" to pngish))
        assertEquals(
            "decoder вернул null (например BitmapFactory failed) -> fallback",
            -9, XXLMemeAssetSource.decode("amdfx.jpg", nullDecoder, { null }, -9)
        )

        val throwingDecoder = FakeProvider(mapOf("amdfx.jpg" to pngish))
        assertEquals(
            -9,
            XXLMemeAssetSource.decode<Int>(
                "amdfx.jpg", throwingDecoder,
                { throw OutOfMemoryError("огромная картинка") }, -9
            )
        )
    }

    @Test
    fun `plan describes what the overlay will render`() {
        val plan = XXLMemeAssetSource.planFor("amdfx.jpg")
        assertNotNull(plan)
        assertTrue(plan.isKnown)
        assertEquals("amdfx.jpg", plan.assetName)
        assertEquals(XXLMemeAssetSource.AMD_FX_FALLBACK_COLOR, plan.fallbackColor)
        assertTrue(plan.label.isNotBlank())

        val broken = XXLMemeAssetSource.planFor("что-то не то")
        assertFalse(broken.isKnown)
        assertNull(broken.assetName)
        assertEquals(XXLMemeAssetSource.UNKNOWN_FALLBACK_COLOR, broken.fallbackColor)
    }

    @Test
    fun `resolve never returns blank captions for any input`() {
        for (raw in listOf(null, "", "  ", "amdfx.jpg", "svo.jpg", "SVO.JPG", "assets/amdfx.jpg", "🗑️")) {
            assertTrue("пустой caption для: $raw", XXLMemeAssetSource.captionFor(raw).isNotBlank())
        }
    }
}
