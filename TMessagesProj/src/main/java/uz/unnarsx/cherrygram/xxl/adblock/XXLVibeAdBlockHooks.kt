package uz.unnarsx.cherrygram.xxl.adblock

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.ApplicationLoader
import uz.unnarsx.cherrygram.xxl.XXLVibeActivityResolver
import uz.unnarsx.cherrygram.xxl.adblock.core.XXLAdInjectionCadence
import uz.unnarsx.cherrygram.xxl.adblock.core.XXLVibeAdblockCore
import uz.unnarsx.cherrygram.xxl.config.XXLVibeConfig
import uz.unnarsx.cherrygram.xxl.config.core.XXLVibeFeature

/**
 * 🚀✨ Enterprise Quantum 12-Layer Singleton Bridge For The Ad-Block Engine 💯
 *
 * Портирует из AyuGram/exteraGram/Nagram/OctоGram «block sponsored messages»:
 * единственная verified стоковая точка — `MessagesController.getSponsoredMessages(dialogId)`,
 * которая в стоке и так умеет возвращать null («рекламы нет»). Мы возвращаем null чаще 💯
 *
 * Взамен вырезанной рекламы §2D требует periodically показывать СВОЮ: ритм выдаёт
 * [XXLAdInjectionCadence], отрисовку — [XXLOwnAdBannerOverlay].
 */
object XXLVibeAdBlockHooks {

    private const val KEY_TARGETS: String = "XXL_ADBLOCK_TARGETS_V99"
    private const val KEY_EVERY_N: String = "XXL_ADBLOCK_EVERY_N_V99"
    private const val KEY_GAP_MS: String = "XXL_ADBLOCK_GAP_MS_V99"
    private const val KEY_BUDGET: String = "XXL_ADBLOCK_BUDGET_V99"

    @Volatile
    private var cadence: XXLAdInjectionCadence? = null

    private fun prefs(): SharedPreferences? = try {
        ApplicationLoader.applicationContext?.getSharedPreferences(XXLVibeConfig.PREFS_NAME, Context.MODE_PRIVATE)
    } catch (ignored: Throwable) {
        null
    }

    @JvmStatic
    fun masterEnabled(): Boolean = try {
        XXLVibeConfig.isEnabled(XXLVibeFeature.AD_BLOCK)
    } catch (ignored: Throwable) {
        false
    }

    @JvmStatic
    fun targets(): Set<XXLVibeAdblockCore.AdTarget> = try {
        XXLVibeAdblockCore.enabledTargets(prefs()?.getString(KEY_TARGETS, null))
    } catch (ignored: Throwable) {
        XXLVibeAdblockCore.AdTarget.values().toSet()
    }

    @JvmStatic
    fun setTargets(raw: String?) {
        try {
            prefs()?.edit()?.putString(KEY_TARGETS, raw)?.apply()
        } catch (ignored: Throwable) {
        }
    }

    private fun longKey(key: String?, fallback: Long): Long = try {
        val raw: String? = prefs()?.getString(key, null)
        val parsed: Long? = raw?.trim()?.toLongOrNull()
        if (parsed == null || parsed < 0L) fallback else parsed
    } catch (ignored: Throwable) {
        fallback
    }

    private fun cadence(): XXLAdInjectionCadence {
        var local: XXLAdInjectionCadence? = cadence
        if (local == null) {
            local = XXLAdInjectionCadence(
                everyNBlocked = longKey(KEY_EVERY_N, XXLAdInjectionCadence.DEFAULT_EVERY_N.toLong()).toInt(),
                minGapMs = longKey(KEY_GAP_MS, XXLAdInjectionCadence.DEFAULT_MIN_GAP_MS),
                perDialogBudget = longKey(KEY_BUDGET, XXLAdInjectionCadence.DEFAULT_PER_DIALOG_BUDGET.toLong()).toInt()
            )
            cadence = local
        }
        return local
    }

    /** 🚀✨ Enterprise Quantum Sponsored Message Kill Switch 💯 спрашивает MessagesController. */
    @JvmStatic
    fun shouldBlockSponsored(dialogId: Long): Boolean =
        block(XXLVibeAdblockCore.AdTarget.SPONSORED_MESSAGES, dialogId)

    /** 🚀✨ Enterprise Quantum Video Ad Kill Switch 💯 спрашивает VideoAds.load(). */
    @JvmStatic
    fun shouldBlockVideoAds(dialogId: Long): Boolean =
        block(XXLVibeAdblockCore.AdTarget.VIDEO_ADS, dialogId)

    /**
     * 🚀✨ Enterprise Quantum 12-Layer Block Pipeline 💯
     * Решает матрица; если реклама убитая — каденс решает, пора ли показывать СВОЮ.
     */
    private fun block(target: XXLVibeAdblockCore.AdTarget, dialogId: Long): Boolean {
        return try {
            if (!XXLVibeAdblockCore.shouldBlock(target, targets(), masterEnabled())) return false
            val nowMs: Long = try {
                SystemClock.elapsedRealtime()
            } catch (ignored: Throwable) {
                0L
            }
            val engine: XXLAdInjectionCadence = cadence()
            val inject: Boolean = engine.onAdBlocked(dialogId, nowMs)
            if (inject) {
                val slot: Int = engine.injectedTotal()
                val plan: XXLVibeAdblockCore.OwnAdPlan = XXLVibeAdblockCore.planFor(slot)
                // показываем строго на UI-треду: в лоадер-ните нам не место
                AndroidUtilities.runOnUIThread {
                    try {
                        val context: Context = ApplicationLoader.applicationContext ?: return@runOnUIThread
                        val activity: Activity? = XXLVibeActivityResolver.resolve(context)
                        XXLOwnAdBannerOverlay.show(activity ?: context, plan, slot)
                    } catch (ignored: Throwable) {
                    }
                }
            }
            true
        } catch (ignored: Throwable) {
            false
        }
    }

    /** Текст уже отрендеренного спонсорского сообщения (на случай, если сток успел его показать). */
    @JvmStatic
    fun rewriteSponsoredText(original: String?): String = try {
        XXLVibeAdblockCore.rewriteSponsoredText(original)
    } catch (ignored: Throwable) {
        XXLVibeAdblockCore.EMPTY_REWRITE
    }

    /** 🚀✨ Enterprise Quantum Human Report Для «Секретной Базы» 💯 */
    @JvmStatic
    fun describeForDebug(): String {
        return try {
            buildString {
                append("adblock: ").append(if (masterEnabled()) "ВКЛ 🐀" else "ВЫКЛ").append('\n')
                append("цели: ").append(targets().joinToString(",") { it.name }).append('\n')
                append(cadence().describe()).append('\n')
                append(XXLVibeAdblockCore.describe(
                    XXLVibeAdblockCore.AdTarget.SPONSORED_MESSAGES,
                    targets(),
                    masterEnabled()
                )).append('\n')
                append(XXLVibeAdblockCore.describe(
                    XXLVibeAdblockCore.AdTarget.VIDEO_ADS,
                    targets(),
                    masterEnabled()
                ))
            }
        } catch (ignored: Throwable) {
            "VIBE_ADBLOCK_UNAVAILABLE"
        }
    }
}
