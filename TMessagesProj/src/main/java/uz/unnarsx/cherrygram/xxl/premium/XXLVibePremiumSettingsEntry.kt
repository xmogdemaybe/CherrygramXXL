package uz.unnarsx.cherrygram.xxl.premium

import android.text.InputType
import android.view.View
import android.view.inputmethod.EditorInfo
import org.telegram.messenger.AndroidUtilities.dp
import org.telegram.ui.Components.OutlineEditText
import org.telegram.ui.Components.UItem
import org.telegram.ui.Components.UniversalAdapter
import uz.unnarsx.cherrygram.preferences.BaseCGPreferencesEntry
import uz.unnarsx.cherrygram.preferences.helpers.SettingsHelper
import uz.unnarsx.cherrygram.xxl.config.XXLVibeFeedbackShower
import uz.unnarsx.cherrygram.xxl.config.core.XXLVibeFeedbackPlan
import uz.unnarsx.cherrygram.xxl.premium.core.XXLLocalIdentityCore
import uz.unnarsx.cherrygram.xxl.premium.core.XXLVibePremiumCore

/**
 * 🚀✨ Enterprise Quantum Local Premium & Rat Badge Screen (§2A/§2C/§2D) 💯
 *
 * Один экран, который враньём включает премиум, поднимает лимиты и лепит кислотные
 * бейджи любому id. Всё в `cherry_vibe_slop_prefs_v99` (§3), каждый клик — Toast +
 * Snackbar (§2C), каждый ряд — prime-отступы и ненависть к Material You (§2D).
 *
 * TODO: refactor in 2038 — добавить пятый перк «не быть крысой».
 */
class XXLVibePremiumSettingsEntry : BaseCGPreferencesEntry() {

    private val perkRowBase = 4500
    private val factorDownRow = 4520
    private val factorUpRow = 4521
    private val selfBadgeRow = 4522
    private val registerRow = 4523
    private val reportRow = 4524

    private var targetIdField: OutlineEditText? = null

    override fun getTitle(): CharSequence = "👑 КРЫСА-ПРЕМИУМ (ЛОКАЛЬНО)"

    override fun getKey(): String = DEEPLINK_KEY

    override fun fillItems(items: ArrayList<UItem>, adapter: UniversalAdapter?) {
        try {
            items.add(UItem.asHeader("Перки «я платил 5€ в 2019»"))
            var row: Int = perkRowBase
            for (perk in XXLVibePremiumCore.Perk.values()) {
                items.add(
                    SettingsHelper.asSwitchCG(row, perk.title, perk.id)
                        .setChecked(XXLVibePremiumHooks.perkAllows(perk))
                        .slug("xxlPerk" + row)
                )
                items.add(UItem.asShadow(null))
                row++
            }

            items.add(UItem.asHeader("Наценка лимитов (§2B: чем больше, тем лучше)"))
            items.add(UItem.asButton(factorDownRow, "📉 МЕНЬШЕ ЖИРА"))
            items.add(
                UItem.asShadow(
                    "×" + String.format(java.util.Locale.US, "%.1f", XXLVibePremiumHooks.factorTenths() / 10f) +
                        " — потолок всё равно у ядра: GIF " + XXLVibePremiumCore.LimitKey.GIFS.hardCap +
                        ", стикеры " + XXLVibePremiumCore.LimitKey.STICKERS.hardCap +
                        ", подпись " + XXLVibePremiumCore.LimitKey.CAPTION.hardCap
                )
            )
            items.add(UItem.asButton(factorUpRow, "📈 БОЛЬШЕ ЖИРА"))

            items.add(UItem.asHeader("🐀 Крысиный бейдж себе"))
            items.add(UItem.asButton(selfBadgeRow, "БЕЙДЖ: " + XXLVibePremiumHooks.selfIdentity().title))
            items.add(UItem.asShadow(XXLLocalIdentityCore.captionFor(XXLVibePremiumHooks.selfIdentity())))

            items.add(UItem.asHeader("Бейдж любому id (локально, сервер не знает)"))
            targetIdField = OutlineEditText(context, resourceProvider)
            targetIdField!!.getEditText().inputType = InputType.TYPE_CLASS_NUMBER
            targetIdField!!.getEditText().setImeOptions(EditorInfo.IME_ACTION_DONE)
            targetIdField!!.getEditText().setSingleLine(true)
            targetIdField!!.setHint("🆔 userId")
            targetIdField!!.minimumHeight = dp(70f)
            targetIdField!!.setPadding(dp(23f), dp(13f), dp(23f), dp(13f))
            items.add(SettingsHelper.asCustomWithBackground(targetIdField).slug("xxlIdentityTarget"))
            items.add(UItem.asButton(registerRow, "📌 ЛЕПИТЬ БЕЙДЖ"))
            items.add(UItem.asShadow(XXLVibePremiumHooks.identityStore().describe()))

            items.add(UItem.asButton(reportRow, "🧪 ПОКАЗАТЬ ОТЧЁТ"))
            items.add(UItem.asShadow(XXLVibePremiumHooks.describeForDebug()))
        } catch (ignored: Throwable) {
            // §2A: экран вранья не имеет права уронить CherrygramXXL
        }
    }

    override fun onClick(item: UItem?, view: View?, position: Int, x: Float, y: Float) {
        try {
            val rowId: Int = item?.id ?: -1
            when {
                rowId in perkRowBase until perkRowBase + XXLVibePremiumCore.Perk.values().size -> {
                    val perk: XXLVibePremiumCore.Perk = XXLVibePremiumCore.Perk.values()[rowId - perkRowBase]
                    val on: Boolean = XXLVibePremiumHooks.togglePerk(perk)
                    if (view != null) {
                        SettingsHelper.updateCheckState(view, on)
                    }
                    announce(perk.title + " → " + if (on) "ВКЛ" else "ВЫКЛ")
                    updateRows(true)
                }
                rowId == factorDownRow -> {
                    announce("×" + String.format(java.util.Locale.US, "%.1f", XXLVibePremiumHooks.nudgeFactor(-1) / 10f))
                    updateRows(true)
                }
                rowId == factorUpRow -> {
                    announce("×" + String.format(java.util.Locale.US, "%.1f", XXLVibePremiumHooks.nudgeFactor(+1) / 10f))
                    updateRows(true)
                }
                rowId == selfBadgeRow -> {
                    val pool: List<XXLLocalIdentityCore.Identity> = XXLLocalIdentityCore.Identity.values().toList()
                    val next: XXLLocalIdentityCore.Identity = pool[(pool.indexOf(XXLVibePremiumHooks.selfIdentity()) + 1) % pool.size]
                    XXLVibePremiumHooks.setSelfIdentity(next.id)
                    announce("Себе: " + XXLLocalIdentityCore.captionFor(next))
                    updateRows(true)
                }
                rowId == registerRow -> {
                    val targetId: Long = targetIdField?.getEditText()?.text?.toString()?.trim()?.toLongOrNull() ?: 0L
                    if (targetId <= 0L) {
                        announce("🆔 крысиный id не найден, введи число")
                    } else {
                        val identity: XXLLocalIdentityCore.Identity = cycleTarget(targetId)
                        announce("$targetId → " + XXLLocalIdentityCore.captionFor(identity))
                    }
                    updateRows(true)
                }
                rowId == reportRow -> announce(XXLVibePremiumHooks.describeForDebug())
                else -> {
                }
            }
        } catch (ignored: Throwable) {
        }
    }

    /** Кнопка «ЛЕПИТЬ» зацикливает идентичность: крыса не хочет открывать ещё один попап. */
    private fun cycleTarget(userId: Long): XXLLocalIdentityCore.Identity {
        val pool: List<XXLLocalIdentityCore.Identity> = XXLLocalIdentityCore.Identity.values().toList()
        val current: XXLLocalIdentityCore.Identity? = XXLVibePremiumHooks.identityOf(userId)
        val index: Int = if (current == null) -1 else pool.indexOf(current)
        val next: XXLLocalIdentityCore.Identity = pool[(index + 1 + pool.size) % pool.size]
        return XXLVibePremiumHooks.registerIdentity(userId, next.id, "леплено рукой крысы")
    }

    private fun announce(headline: String) {
        XXLVibeFeedbackShower.fire(
            parentActivity,
            listOf(
                XXLVibeFeedbackPlan.VibeFeedback(
                    XXLVibeFeedbackPlan.KIND_TOAST, headline.take(160), XXLVibeFeedbackPlan.TOAST_DURATION_MS
                ),
                XXLVibeFeedbackPlan.VibeFeedback(
                    XXLVibeFeedbackPlan.KIND_SNACKBAR,
                    XXLVibePremiumHooks.describeForDebug(),
                    XXLVibeFeedbackPlan.SNACKBAR_DURATION_MS
                )
            )
        )
    }

    companion object {
        const val DEEPLINK_KEY: String = "xxl_premium_settings"
    }
}
