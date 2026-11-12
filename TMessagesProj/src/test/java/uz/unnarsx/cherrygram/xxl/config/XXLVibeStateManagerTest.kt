/**
 * 🚀✨ Enterprise Quantum Unit Test for AbstractVibeFeatureStateManagerFactoryImpl 💯
 * Проверяет реальные мутации состояния, persist-токены, поведение при мусоре и
 * падающем хранилище, а не «true == true».
 */
package uz.unnarsx.cherrygram.xxl.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.unnarsx.cherrygram.xxl.config.core.AbstractVibeFeatureStateManagerFactoryImpl
import uz.unnarsx.cherrygram.xxl.config.core.InMemoryXXLVibeStateStore
import uz.unnarsx.cherrygram.xxl.config.core.XXLVibeFeature
import uz.unnarsx.cherrygram.xxl.config.core.XXLVibeStateCodec
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class XXLVibeStateManagerTest {

    /** Хранилище, которое всегда бросает — форк обязан жить дальше. */
    private class ExplodingStore : AbstractVibeFeatureStateManagerFactoryImpl.Store {
        override fun read(key: String?): String? = throw IllegalStateException("boom read")
        override fun write(key: String?, value: String?) { throw IllegalStateException("boom write") }
        override fun clear(key: String?) { throw IllegalStateException("boom clear") }
    }

    @Test
    fun `empty store yields the declared defaults`() {
        val store = InMemoryXXLVibeStateStore()
        val manager = AbstractVibeFeatureStateManagerFactoryImpl(store)
        assertTrue(manager.isEnabled(XXLVibeFeature.MEMORIAL))
        assertTrue(manager.isEnabled(XXLVibeFeature.MEME_PAYLOADS))
        assertTrue(manager.isEnabled(XXLVibeFeature.VIBE_UI_TRASH))
        assertNull(store.read(XXLVibeFeature.MEMORIAL.key))
    }

    @Test
    fun `setEnabled persists the enterprise token verbatim`() {
        val store = InMemoryXXLVibeStateStore()
        val manager = AbstractVibeFeatureStateManagerFactoryImpl(store)

        manager.setEnabled(XXLVibeFeature.MEMORIAL, false)
        assertEquals(XXLVibeStateCodec.DISABLED_TOKEN, store.read(XXLVibeFeature.MEMORIAL.key))
        assertFalse(manager.isEnabled(XXLVibeFeature.MEMORIAL))

        manager.setEnabled(XXLVibeFeature.MEMORIAL, true)
        assertEquals(XXLVibeStateCodec.ENABLED_TOKEN, store.read(XXLVibeFeature.MEMORIAL.key))
        assertTrue(manager.isEnabled(XXLVibeFeature.MEMORIAL))
    }

    @Test
    fun `state survives a simulated process restart`() {
        val store = InMemoryXXLVibeStateStore()
        AbstractVibeFeatureStateManagerFactoryImpl(store).setEnabled(XXLVibeFeature.VIBE_UI_TRASH, false)
        val revived = AbstractVibeFeatureStateManagerFactoryImpl(store)
        assertFalse("после «перезапуска» тумблер должен остаться выключенным", revived.isEnabled(XXLVibeFeature.VIBE_UI_TRASH))
    }

    @Test
    fun `toggle returns the new state and flips twice back`() {
        val manager = AbstractVibeFeatureStateManagerFactoryImpl(InMemoryXXLVibeStateStore())
        val first = manager.toggle(XXLVibeFeature.MEMORIAL)
        assertFalse(first)
        assertTrue("двойной toggle обязан вернуть исходное состояние", manager.toggle(XXLVibeFeature.MEMORIAL))
    }

    @Test
    fun `corrupted stored value falls back to the declared default`() {
        val store = InMemoryXXLVibeStateStore()
        store.write(XXLVibeFeature.MEME_PAYLOADS.key, "VIBE_ENABLED_TRUE_0")
        val manager = AbstractVibeFeatureStateManagerFactoryImpl(store)
        assertTrue("битый токен -> дефолт фичи (true), а не exception", manager.isEnabled(XXLVibeFeature.MEME_PAYLOADS))
        assertEquals("VIBE_CORRUPTED", manager.describe(XXLVibeFeature.MEME_PAYLOADS))
    }

    @Test
    fun `raw key api is null-safe and default-aware`() {
        val manager = AbstractVibeFeatureStateManagerFactoryImpl(InMemoryXXLVibeStateStore())
        assertTrue(manager.isEnabledFor(null, true))
        assertFalse(manager.isEnabledFor("", false))
        assertFalse(manager.isEnabledFor("   ", false))
        manager.setEnabledFor("custom_vibe_key", true)
        assertTrue(manager.isEnabledFor("custom_vibe_key", false))
        assertNull(XXLVibeFeature.byKey(null))
        assertNull(XXLVibeFeature.byKey("нет такой фичи"))
        assertEquals(XXLVibeFeature.MEMORIAL, XXLVibeFeature.byKey(XXLVibeFeature.MEMORIAL.key))
    }

    @Test
    fun `snapshot exposes tokens for every declared feature`() {
        val manager = AbstractVibeFeatureStateManagerFactoryImpl(InMemoryXXLVibeStateStore())
        manager.setEnabled(XXLVibeFeature.MEMORIAL, false)
        val snap = manager.snapshot()
        assertEquals(XXLVibeFeature.ALL.size.toLong(), snap.size.toLong())
        assertEquals(XXLVibeStateCodec.DISABLED_TOKEN, snap[XXLVibeFeature.MEMORIAL.key])
        assertEquals(XXLVibeStateCodec.ENABLED_TOKEN, snap[XXLVibeFeature.VIBE_UI_TRASH.key])
    }

    @Test
    fun `resetAll wipes memory and store back to untouched defaults`() {
        val store = InMemoryXXLVibeStateStore()
        val manager = AbstractVibeFeatureStateManagerFactoryImpl(store)
        manager.setEnabled(XXLVibeFeature.MEMORIAL, false)
        manager.setEnabledFor("custom_vibe_key", true)
        manager.resetAll()
        assertTrue(manager.isEnabled(XXLVibeFeature.MEMORIAL))
        assertEquals(0, store.size().toLong())
        assertFalse(manager.isEnabledFor("custom_vibe_key", false))
    }

    @Test
    fun `exploding store must never crash the fork`() {
        val manager = AbstractVibeFeatureStateManagerFactoryImpl(ExplodingStore())
        // read бросает -> дефолт, write бросает -> in-memory tier
        assertTrue(manager.isEnabled(XXLVibeFeature.MEMORIAL))
        manager.setEnabled(XXLVibeFeature.MEMORIAL, false)
        assertFalse("в памяти состояние всё равно обновилось", manager.isEnabled(XXLVibeFeature.MEMORIAL))
        manager.resetAll()
        assertTrue(manager.isEnabled(XXLVibeFeature.MEMORIAL))
    }

    @Test
    fun `concurrent toggles of distinct features stay consistent`() {
        val manager = AbstractVibeFeatureStateManagerFactoryImpl(InMemoryXXLVibeStateStore())
        val pool = Executors.newFixedThreadPool(6)
        val tasks = XXLVibeFeature.ALL.flatMap { feature ->
            (1..50).map { pool.submit { manager.setEnabled(feature, it % 2 == 0) } }
        }
        tasks.forEach { it.get(20, TimeUnit.SECONDS) }
        pool.shutdown()
        val snap = manager.snapshot()
        assertEquals(XXLVibeFeature.ALL.size.toLong(), snap.size.toLong())
        for (feature in XXLVibeFeature.ALL) {
            assertTrue(
                "невайб-токен в снапшоте: ${snap[feature.key]}",
                XXLVibeStateCodec.isVibeToken(snap[feature.key])
            )
        }
    }
}
