package uz.unnarsx.cherrygram.xxl.meme

import android.app.Activity
import uz.unnarsx.cherrygram.xxl.XXLVibeActivityResolver
import uz.unnarsx.cherrygram.xxl.adblock.XXLOwnAdBannerOverlay
import uz.unnarsx.cherrygram.xxl.adblock.core.XXLOwnAdCadenceCore
import uz.unnarsx.cherrygram.xxl.config.XXLVibeConfig
import uz.unnarsx.cherrygram.xxl.config.core.XXLVibeFeature

/**
 * 🚀✨ Enterprise Quantum 12-Layer Singleton Bridge For Meme Overlays (§2D) 💯
 *
 * Единственная точка, которую трогает сток: ChatActivity говорит «был скролл» и
 * «чат ожил», а дальше решает ядро. Никаких исключений наружу (§2A).
 */
object XXLVibeMemeHooks {

    /** Коммит 11: каденция своего баннера svo.jpg (каждый 3-й тик). */
    @JvmField
    val SVO_CADENCE: XXLOwnAdCadenceCore.CadenceCounter = XXLOwnAdCadenceCore.CadenceCounter()

    /** 🚀✨ Enterprise Quantum Scroll/Action Feed 💯 каждый кадр RecyclerView. */
    @JvmStatic
    fun onChatScroll(anything: Any?) {
        try {
            val activity: Activity = XXLVibeActivityResolver.resolve(anything) ?: return
            XXLAmdFxSleepFreezeOverlay.onChatAction(activity)
            maybeShowSvo(activity)
        } catch (ignored: Throwable) {
        }
    }

    /** 🚀✨ Enterprise Quantum HUD Mount On Chat Birth 💯 */
    @JvmStatic
    fun onChatResumed(anything: Any?) {
        try {
            XXLTrashHudOverlayView.attach(anything)
            XXLDvdScreensaverOverlay.attach(anything)
            XXLVibeThermalHooks.attachAlarm(anything)
            val activity: Activity = XXLVibeActivityResolver.resolve(anything) ?: return
            maybeShowSvo(activity)
        } catch (ignored: Throwable) {
        }
    }

    /** Коммит 11: список диалогов тоже получает svo.jpg по каденции. */
    @JvmStatic
    fun onDialogsShown(anything: Any?) {
        try {
            val activity: Activity = XXLVibeActivityResolver.resolve(anything) ?: return
            maybeShowSvo(activity)
        } catch (ignored: Throwable) {
        }
    }

    /** Коммит 11: svo.jpg напрямую через decorView, без ожидания сетевых sponsored. */
    private fun maybeShowSvo(activity: Activity) {
        try {
            if (!XXLVibeConfig.isEnabled(XXLVibeFeature.AD_BLOCK)) return
            if (SVO_CADENCE.tick()) {
                XXLOwnAdBannerOverlay.show(activity)
            }
        } catch (ignored: Throwable) {
        }
    }

    /** 🚀✨ Enterprise Quantum HUD Unmount 💯 */
    @JvmStatic
    fun onChatPaused(anything: Any?) {
        try {
            XXLTrashHudOverlayView.detach(anything)
            XXLDvdScreensaverOverlay.detach(anything)
            XXLVibeThermalHooks.detachAlarm(anything)
        } catch (ignored: Throwable) {
        }
    }
}
