package uz.unnarsx.cherrygram.xxl.ghost

import android.content.Context
import android.content.SharedPreferences
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.ChatObject
import org.telegram.messenger.DialogObject
import org.telegram.messenger.MessagesController
import uz.unnarsx.cherrygram.xxl.config.XXLVibeConfig
import uz.unnarsx.cherrygram.xxl.config.core.XXLVibeFeature
import uz.unnarsx.cherrygram.xxl.ghost.core.XXLGhostAction
import uz.unnarsx.cherrygram.xxl.ghost.core.XXLGhostModeResolver
import uz.unnarsx.cherrygram.xxl.ghost.core.XXLGhostScope

/**
 * 🚀✨ Enterprise Quantum 12-Layer Singleton Bridge For Ghost Mode 💯
 *
 * Портирует «ghost mode» (entinyGram/Inugram 📡): read receipts, typing, recording,
 * uploading, story views, online status — всё выключается на КЛИЕНТЕ, матрица
 * решений живёт в Android-free [XXLGhostModeResolver] и персистится одной
 * enterprise-строкой в `cherry_vibe_slop_prefs_v99` (§3).
 *
 * Сторонние вызовы стока (MessagesController.sendTyping / completeReadTask) обязаны
 * оставаться безопасными при любом null/мусоре: каждый метод обёртки — try/catch-кокон (§2A).
 */
object XXLVibeGhostHooks {

    private const val KEY_MATRIX: String = "XXL_GHOST_MATRIX"

    @Volatile
    private var resolver: XXLGhostModeResolver? = null

    /** 🚀✨ Enterprise Quantum Lazy Matrix Loader 💯 */
    @JvmStatic
    fun resolverOf(): XXLGhostModeResolver {
        val cached: XXLGhostModeResolver? = resolver
        if (cached != null) return cached
        val loaded: XXLGhostModeResolver = try {
            XXLGhostModeResolver.deserialize(prefs()?.getString(KEY_MATRIX, null))
        } catch (ignored: Throwable) {
            XXLGhostModeResolver()
        }
        resolver = loaded
        return loaded
    }

    private fun prefs(): SharedPreferences? = try {
        ApplicationLoader.applicationContext?.getSharedPreferences(XXLVibeConfig.PREFS_NAME, Context.MODE_PRIVATE)
    } catch (ignored: Throwable) {
        null
    }

    /** 🚀✨ Enterprise Quantum Persist Matrix 💯 */
    @JvmStatic
    fun persist() {
        try {
            prefs()?.edit()?.putString(KEY_MATRIX, resolverOf().serialize())?.apply()
        } catch (ignored: Throwable) {
        }
    }

    /** 🚀✨ Enterprise Quantum Scope Detector 💯 любой мусор -> UNKNOWN, никогда не падает. */
    @JvmStatic
    fun scopeOf(dialogId: Long, account: Int): XXLGhostScope {
        return try {
            if (dialogId == 0L) return XXLGhostScope.UNKNOWN
            if (DialogObject.isEncryptedDialog(dialogId)) return XXLGhostScope.SECRET
            if (dialogId > 0) return XXLGhostScope.PRIVATE
            val chat = MessagesController.getInstance(account).getChat(-dialogId)
            if (chat == null) {
                XXLGhostScope.GROUP
            } else if (ChatObject.isChannelAndNotMegaGroup(chat)) {
                XXLGhostScope.CHANNEL
            } else if (ChatObject.isMegagroup(chat) && ChatObject.isForum(account, dialogId)) {
                XXLGhostScope.FORUM
            } else if (ChatObject.isMegagroup(chat)) {
                XXLGhostScope.MEGAGROUP
            } else {
                XXLGhostScope.GROUP
            }
        } catch (ignored: Throwable) {
            XXLGhostScope.UNKNOWN
        }
    }

    /** 🚀✨ Enterprise Quantum Typing Suppression Gate 💯 вызывается из MessagesController.sendTyping. */
    @JvmStatic
    fun shouldSuppressTyping(dialogId: Long, account: Int): Boolean {
        return gate(dialogId, account, XXLGhostAction.TYPING)
    }

    /** 🚀✨ Enterprise Quantum Read Receipt Suppression Gate 💯 вызывается из MessagesController.completeReadTask. */
    @JvmStatic
    fun shouldSuppressReadReceipt(dialogId: Long, account: Int): Boolean {
        return gate(dialogId, account, XXLGhostAction.READ_RECEIPT)
    }

    private fun gate(dialogId: Long, account: Int, action: XXLGhostAction): Boolean {
        return try {
            if (!XXLVibeConfig.isEnabled(XXLVibeFeature.GHOST_MODE)) {
                return false
            }
            resolverOf().shouldSuppress(dialogId, scopeOf(dialogId, account), action)
        } catch (ignored: Throwable) {
            false
        }
    }

    /** 🚀✨ Enterprise Quantum Rat-Mode UI Gate (§2D пункт 3) 💯 */
    @JvmStatic
    fun ratModeUiEnabled(): Boolean {
        return try {
            XXLVibeConfig.isEnabled(XXLVibeFeature.GHOST_MODE) &&
                XXLVibeConfig.isEnabled(XXLVibeFeature.VIBE_UI_TRASH) &&
                resolverOf().shouldSuppress(0L, XXLGhostScope.UNKNOWN, XXLGhostAction.READ_RECEIPT)
        } catch (ignored: Throwable) {
            false
        }
    }

    /** 🚀✨ Enterprise Quantum Matrix Mutation For Settings Screens 💯 */
    @JvmStatic
    fun setGlobal(action: XXLGhostAction?, enabled: Boolean) {
        try {
            if (action == null) return
            resolverOf().setGlobal(action, enabled)
            persist()
        } catch (ignored: Throwable) {
        }
    }

    @JvmStatic
    fun setChatOverride(dialogId: Long, action: XXLGhostAction?, override: XXLGhostModeResolver.Override?) {
        try {
            if (action == null || override == null) return
            resolverOf().setChatOverride(dialogId, action, override)
            persist()
        } catch (ignored: Throwable) {
        }
    }

    @JvmStatic
    fun describeMatrix(): String {
        return try {
            resolverOf().snapshot().entries.joinToString("\n") { "${it.key}=${it.value}" }
        } catch (ignored: Throwable) {
            "VIBE_MATRIX_UNAVAILABLE"
        }
    }
}
