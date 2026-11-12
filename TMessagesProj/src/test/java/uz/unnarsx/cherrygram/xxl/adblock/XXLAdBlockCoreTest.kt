/**
 * 🚀✨ Enterprise Quantum Unit Test for the Ad-Block Engine + cadence of our OWN banner (§2D) 💯
 * Verifies: target gating, every-N-th cadence, per-dialog budget, monotonic time guard, counters.
 */
package uz.unnarsx.cherrygram.xxl.adblock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.unnarsx.cherrygram.xxl.adblock.core.XXLAdInjectionCadence
import uz.unnarsx.cherrygram.xxl.adblock.core.XXLVibeAdblockCore

class XXLAdBlockCoreTest {

    @Test
    fun `adblock decision follows the target switch matrix`() {
        val on = setOf(XXLVibeAdblockCore.AdTarget.SPONSORED_MESSAGES)
        assertTrue(XXLVibeAdblockCore.shouldBlock(XXLVibeAdblockCore.AdTarget.SPONSORED_MESSAGES, on, true))
        assertFalse(XXLVibeAdblockCore.shouldBlock(XXLVibeAdblockCore.AdTarget.SPONSORED_STORIES, on, true))
        assertFalse("master switch выключен — не смеем трогать стоковые сети",
            XXLVibeAdblockCore.shouldBlock(XXLVibeAdblockCore.AdTarget.SPONSORED_MESSAGES, on, false))
        assertFalse(XXLVibeAdblockCore.shouldBlock(null, on, true))
        assertFalse(XXLVibeAdblockCore.shouldBlock(XXLVibeAdblockCore.AdTarget.SPONSORED_MESSAGES, emptySet(), true))
    }

    @Test
    fun `target parser is tolerant and deduplicated`() {
        val parsed = XXLVibeAdblockCore.enabledTargets("sponsored_messages, sponsored-stories МУСОР sponsored_messages")
        assertEquals(2, parsed.size.toLong())
        assertTrue(parsed.contains(XXLVibeAdblockCore.AdTarget.SPONSORED_MESSAGES))
        assertTrue(parsed.contains(XXLVibeAdblockCore.AdTarget.SPONSORED_STORIES))
        assertEquals(XXLVibeAdblockCore.AdTarget.values().toSet(), XXLVibeAdblockCore.enabledTargets(null))
        assertEquals(XXLVibeAdblockCore.AdTarget.values().toSet(), XXLVibeAdblockCore.enabledTargets("   "))
        assertTrue(XXLVibeAdblockCore.enabledTargets("никакой крысы").isEmpty())
    }

    @Test
    fun `only implemented choke points may be blocked`() {
        assertTrue(XXLVibeAdblockCore.hasChokePoint(XXLVibeAdblockCore.AdTarget.SPONSORED_MESSAGES))
        assertFalse(XXLVibeAdblockCore.hasChokePoint(null))
        for (target in XXLVibeAdblockCore.AdTarget.values()) {
            assertEquals(
                "${target.name} обязан быть честным про свою точку",
                XXLVibeAdblockCore.implementedTargets().contains(target),
                XXLVibeAdblockCore.hasChokePoint(target)
            )
        }
        // «заблокировано» не бывает без точки вешания
        assertFalse(
            XXLVibeAdblockCore.shouldBlock(
                XXLVibeAdblockCore.AdTarget.PREMIUM_UPSELL,
                XXLVibeAdblockCore.AdTarget.values().toSet(),
                true
            )
        )
    }

    @Test
    fun `cadence fires exactly every nth blocked ad`() {
        val cadence = XXLAdInjectionCadence(everyNBlocked = 3, minGapMs = 0L, perDialogBudget = 100)
        assertFalse(cadence.onAdBlocked(-100L, 1_000L))
        assertFalse(cadence.onAdBlocked(-100L, 2_000L))
        assertTrue(cadence.onAdBlocked(-100L, 3_000L))
        assertFalse(cadence.onAdBlocked(-100L, 4_000L))
        assertFalse(cadence.onAdBlocked(-100L, 5_000L))
        assertTrue(cadence.onAdBlocked(-100L, 6_000L))
        assertEquals(6, cadence.blockedTotal().toLong())
        assertEquals(2, cadence.injectedTotal().toLong())
    }

    @Test
    fun `min gap throttles the banner even on the right multiple`() {
        val cadence = XXLAdInjectionCadence(everyNBlocked = 1, minGapMs = 5_000L, perDialogBudget = 100)
        assertTrue(cadence.onAdBlocked(-100L, 1_000L))
        assertFalse("щадим глаза: слишком часто", cadence.onAdBlocked(-100L, 2_000L))
        assertFalse(cadence.onAdBlocked(-100L, 5_999L))
        assertTrue(cadence.onAdBlocked(-100L, 6_001L))
        assertEquals(4, cadence.blockedTotal().toLong())
        assertEquals(2, cadence.injectedTotal().toLong())
    }

    @Test
    fun `per dialog budget is counted per dialog not globally`() {
        val cadence = XXLAdInjectionCadence(everyNBlocked = 1, minGapMs = 0L, perDialogBudget = 2)
        assertTrue(cadence.onAdBlocked(-100L, 1L))
        assertTrue(cadence.onAdBlocked(-100L, 2L))
        assertFalse("бюджет этого чата исчерпан", cadence.onAdBlocked(-100L, 3L))
        assertTrue("чужой бюджет не трогаем", cadence.onAdBlocked(-200L, 4L))
        assertEquals(2, cadence.dialogInjects(-100L).toLong())
        assertEquals(1, cadence.dialogInjects(-200L).toLong())
        assertEquals(0, cadence.dialogInjects(0L).toLong())
    }

    @Test
    fun `disabled cadence never injects and never explodes`() {
        val cadence = XXLAdInjectionCadence(everyNBlocked = 0, minGapMs = -100L, perDialogBudget = -5)
        for (i in 1..10) {
            assertFalse(cadence.onAdBlocked(-100L, i.toLong()))
        }
        assertEquals(10, cadence.blockedTotal().toLong())
        assertEquals(0, cadence.injectedTotal().toLong())
        assertFalse(cadence.onAdBlocked(0L, 1L))
        cadence.reset()
        assertEquals(0, cadence.blockedTotal().toLong())
        assertEquals(0, cadence.injectedTotal().toLong())
    }

    @Test
    fun `time travelling backwards does not unlock infinite banners`() {
        val cadence = XXLAdInjectionCadence(everyNBlocked = 1, minGapMs = 1_000L, perDialogBudget = 100)
        assertTrue(cadence.onAdBlocked(-100L, 100_000L))
        assertFalse("часы отмотали — молчим", cadence.onAdBlocked(-100L, 50_000L))
        assertTrue(cadence.onAdBlocked(-100L, 101_001L))
    }

    @Test
    fun `snapshot exposes counters for the debug screen`() {
        val cadence = XXLAdInjectionCadence(everyNBlocked = 2, minGapMs = 0L, perDialogBudget = 10)
        cadence.onAdBlocked(-100L, 1L)
        cadence.onAdBlocked(-100L, 2L)
        cadence.onAdBlocked(-200L, 3L)
        val snap: Map<String, String> = cadence.snapshot()
        assertEquals("3", snap[XXLAdInjectionCadence.KEY_BLOCKED])
        assertEquals("1", snap[XXLAdInjectionCadence.KEY_INJECTED])
        assertEquals("-200", snap[XXLAdInjectionCadence.KEY_LAST_DIALOG])
        assertTrue(snap.containsKey(XXLAdInjectionCadence.KEY_EVERY))
    }

    @Test
    fun `own banner copy is deterministic and never blank`() {
        for (slot in 0..11) {
            val plan = XXLVibeAdblockCore.planFor(slot)
            assertTrue(slot.toString(), plan.headline.isNotBlank())
            assertTrue(slot.toString(), plan.cta.isNotBlank())
            assertEquals("svo.jpg", plan.assetName)
            assertTrue(plan.durationMs >= 2_000L)
            assertEquals(plan, XXLVibeAdblockCore.planFor(slot))
            assertEquals(XXLVibeAdblockCore.planFor(slot % 4), XXLVibeAdblockCore.planFor(slot))
        }
        assertEquals("floorMod для отрицательных слотов", XXLVibeAdblockCore.planFor(3), XXLVibeAdblockCore.planFor(-17))
    }

    @Test
    fun `sponsored message rewrite is null safe and cursed`() {
        val rewritten = XXLVibeAdblockCore.rewriteSponsoredText("Купите премиум за 3 копейки")
        assertTrue(rewritten, rewritten.contains("КРЫСА"))
        assertTrue(rewritten, rewritten.contains("Купите премиум"))
        assertEquals("🐀 КРЫСА ЗАКРЫЛА РЕКЛАМУ ДУРОВА (пусто)", XXLVibeAdblockCore.rewriteSponsoredText(null))
        assertEquals(XXLVibeAdblockCore.rewriteSponsoredText(null), XXLVibeAdblockCore.rewriteSponsoredText("   "))
        assertTrue(XXLVibeAdblockCore.rewriteSponsoredText("x".repeat(500)).length <= 240)
        assertEquals(
            "https://t.me/CherrygramBetaAPKs/2810",
            XXLVibeAdblockCore.rewriteUrl("https://ad.durov.example/click")
        )
        assertEquals("https://t.me/CherrygramBetaAPKs/2810", XXLVibeAdblockCore.rewriteUrl(null))
    }
}
