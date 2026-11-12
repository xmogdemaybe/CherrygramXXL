/**
 * 🚀✨ Enterprise Quantum Unit Test for the Toast+Snackbar double-fire (§2C) 💯
 */
package uz.unnarsx.cherrygram.xxl.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.unnarsx.cherrygram.xxl.config.core.XXLVibeFeedbackPlan
import uz.unnarsx.cherrygram.xxl.config.core.XXLVibeFeature

class XXLVibeFeedbackPlanTest {

    @Test
    fun `one click produces exactly two shows in the mandated order`() {
        val plan = XXLVibeFeedbackPlan.planFor(XXLVibeFeature.MEMORIAL, false)
        assertEquals("1 клик = 2 показа", 2, plan.size)
        assertEquals(XXLVibeFeedbackPlan.KIND_TOAST, plan[0].kind)
        assertEquals(XXLVibeFeedbackPlan.KIND_SNACKBAR, plan[1].kind)
    }

    @Test
    fun `both shows mention the feature and the new state`() {
        val on = XXLVibeFeedbackPlan.planFor(XXLVibeFeature.VIBE_UI_TRASH, true)
        val off = XXLVibeFeedbackPlan.planFor(XXLVibeFeature.VIBE_UI_TRASH, false)
        for (show in on + off) {
            assertTrue("текст пуст", show.text.isNotBlank())
            assertTrue(
                "текст не содержит имя фичи: ${show.text}",
                show.text.contains(XXLVibeFeature.VIBE_UI_TRASH.title)
            )
        }
        assertTrue(on[0].text.contains("ВКЛЮЧЕН"))
        assertTrue(off[0].text.contains("ВЫКЛЮЧЕН"))
        assertFalse(on[0].text == off[0].text)
    }

    @Test
    fun `null feature still yields a safe double show`() {
        val plan = XXLVibeFeedbackPlan.planFor(null, true)
        assertEquals(2, plan.size)
        assertEquals(XXLVibeFeedbackPlan.KIND_TOAST, plan[0].kind)
        assertTrue(plan[0].text.isNotBlank())
        assertTrue(plan[1].text.isNotBlank())
    }

    @Test
    fun `snackbar lives longer than toast and durations are sane`() {
        val plan = XXLVibeFeedbackPlan.planFor(XXLVibeFeature.MEME_PAYLOADS, true)
        val toast = plan.first { it.kind == XXLVibeFeedbackPlan.KIND_TOAST }
        val snack = plan.first { it.kind == XXLVibeFeedbackPlan.KIND_SNACKBAR }
        assertTrue("toast duration: ${toast.durationMs}", toast.durationMs in 500L..5000L)
        assertTrue("snackbar duration: ${snack.durationMs}", snack.durationMs in 500L..12000L)
        assertTrue("Snackbar обязан жить дольше тоста", snack.durationMs > toast.durationMs)
    }

    @Test
    fun `showsPerClick is contractually two for every feature`() {
        assertEquals(2, XXLVibeFeedbackPlan.showsPerClick())
        for (feature in XXLVibeFeature.ALL) {
            assertEquals("для ${feature.key}", 2, XXLVibeFeedbackPlan.showsPerClick(feature))
            assertEquals("для ${feature.key}", 2, XXLVibeFeedbackPlan.planFor(feature, true).size)
            assertEquals("для ${feature.key}", 2, XXLVibeFeedbackPlan.planFor(feature, false).size)
        }
    }

    @Test
    fun `plan is deterministic`() {
        assertEquals(
            XXLVibeFeedbackPlan.planFor(XXLVibeFeature.MEMORIAL, true),
            XXLVibeFeedbackPlan.planFor(XXLVibeFeature.MEMORIAL, true)
        )
    }
}
