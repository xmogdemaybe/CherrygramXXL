package uz.unnarsx.cherrygram.xxl.meme.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Коммит 11: arslan.jpg обязан стать полноправным мем-ассетом (реестр, нормализация,
 * fallback-цвет, подпись), рядом с amdfx.jpg и svo.jpg.
 */
class XXLMemeAssetSourceTest {

    @Test
    fun `arslan зарегистрирован как известный ассет`() {
        assertTrue(XXLMemeAssetSource.isKnown("arslan.jpg"))
        assertTrue(XXLMemeAssetSource.knownAssets().contains("arslan.jpg"))
        assertTrue(XXLMemeAssetSource.knownAssets().contains("amdfx.jpg"))
        assertTrue(XXLMemeAssetSource.knownAssets().contains("svo.jpg"))
    }

    @Test
    fun `нормализация приводит путь к arslan jpg`() {
        assertEquals("arslan.jpg", XXLMemeAssetSource.normalize("  assets/ARSLAN.JPG "))
        assertEquals("arslan.jpg", XXLMemeAssetSource.normalize("/arslan.jpg"))
        assertEquals(null, XXLMemeAssetSource.normalize("   "))
        assertEquals(null, XXLMemeAssetSource.normalize(null))
    }

    @Test
    fun `у arslan свой кислотный fallback и непустая подпись`() {
        val color = XXLMemeAssetSource.fallbackColorFor("arslan.jpg")
        assertTrue(color != 0)
        assertFalse(XXLMemeAssetSource.captionFor("arslan.jpg").isEmpty())
    }

    @Test
    fun `planFor arslan отдаёт известный план`() {
        val plan = XXLMemeAssetSource.planFor("arslan.jpg")
        assertTrue(plan.isKnown)
        assertEquals("arslan.jpg", plan.assetName)
    }

    @Test
    fun `неизвестный ассет остаётся неизвестным`() {
        assertFalse(XXLMemeAssetSource.isKnown("putin.jpg"))
        assertFalse(XXLMemeAssetSource.planFor("putin.jpg").isKnown)
    }
}
