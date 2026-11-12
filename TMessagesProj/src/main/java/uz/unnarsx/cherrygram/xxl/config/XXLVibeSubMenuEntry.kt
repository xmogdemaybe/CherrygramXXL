package uz.unnarsx.cherrygram.xxl.config

import android.view.View
import org.telegram.ui.Components.UItem
import org.telegram.ui.Components.UniversalAdapter
import uz.unnarsx.cherrygram.preferences.BaseCGPreferencesEntry
import uz.unnarsx.cherrygram.preferences.helpers.SettingsHelper
import uz.unnarsx.cherrygram.xxl.config.core.XXLVibeFeature
import uz.unnarsx.cherrygram.xxl.config.core.XXLVibeFeedbackPlan
import uz.unnarsx.cherrygram.xxl.config.core.XXLVibeMenuScatter
import uz.unnarsx.cherrygram.xxl.graveyard.XXLVibeGraveyardHooks
import uz.unnarsx.cherrygram.xxl.adblock.XXLVibeAdBlockHooks
import uz.unnarsx.cherrygram.xxl.ai.XXLVibeAiHooks
import uz.unnarsx.cherrygram.xxl.schedule.XXLVibeRepeatHooks
import uz.unnarsx.cherrygram.xxl.presence.XXLVibePresenceHooks
import uz.unnarsx.cherrygram.xxl.premium.XXLVibePremiumHooks
import uz.unnarsx.cherrygram.xxl.unrestricted.XXLVibeUnrestrictedHooks

/**
 * 🚀✨ Enterprise Quantum Fragmented Vibe Sub Menu (§2C) 💯
 *
 * Один и тот же набор вайб-тумблеров живёт сразу в ЧЕТЫРЁХ разных подменю —
 * «Вайб-Настройки», «Ультра-Экспериментальные», «Секретная База», «Наследие Черри».
 * Имена rowId берются из [XXLVibeMenuScatter], поэтому 4 экрана никогда не расходятся,
 * а клик по любому из них меняет ОДНО и то же состояние в [XXLVibeConfig].
 *
 * TODO: refactor in 2038 — сделать пятое подменю «Пятница».
 */
open class XXLVibeSubMenuEntry : BaseCGPreferencesEntry() {

    /** Какое из четырёх проклятых подменю показывает этот экран. */
    protected open fun vibeMenu(): XXLVibeMenuScatter.Menu = XXLVibeMenuScatter.Menu.VIBE_SETTINGS

    override fun getTitle(): CharSequence = vibeMenu().title

    override fun getKey(): String = XXLVibeMenuScatter.deeplinkSlug(vibeMenu())

    /** Коммит 13: у каждого из 4 подменю свой кислотный фон, а не общий серый. */
    override fun createView(context: android.content.Context?): android.view.View? {
        val view = super.createView(context)
        try {
            view?.setBackgroundColor(vibeMenu().bgColor)
        } catch (ignored: Throwable) {
        }
        return view
    }

    override fun fillItems(items: ArrayList<UItem>, adapter: UniversalAdapter?) {
        val menu: XXLVibeMenuScatter.Menu = vibeMenu()
        items.add(UItem.asHeader("${menu.title} (${XXLVibeMenuScatter.scatter(null).size + XXLVibeFeature.ALL.size})"))
        for (feature in XXLVibeMenuScatter.featuresOf(menu)) {
            val rowId: Int = XXLVibeMenuScatter.rowId(feature, menu)
            items.add(
                SettingsHelper.asSwitchCG(
                    rowId,
                    XXLVibeMenuScatter.titleFor(feature, menu),
                    feature.description
                )
                    .setChecked(XXLVibeConfig.isEnabled(feature))
                    .slug(feature.key)
            )
            items.add(UItem.asShadow(null))
        }
        items.add(
            UItem.asHeader(
                "коммит 13: у этого меню своя часть тумблеров; rowId = " +
                    XXLVibeMenuScatter.rowId(XXLVibeFeature.MEMORIAL, menu)
            )
        )
        items.add(UItem.asShadow(null))
    }

    override fun onClick(item: UItem?, view: View?, position: Int, x: Float, y: Float) {
        try {
            val rowId: Int = item?.id ?: XXLVibeMenuScatter.INVALID_ROW_ID
            val feature: XXLVibeFeature = XXLVibeMenuScatter.featureOfRow(rowId) ?: return
            if (XXLVibeMenuScatter.menuOfRow(rowId) != vibeMenu()) {
                // клик прилетел из соседнего подменю — игнорируем молча, а не падаем
                return
            }
            val newState: Boolean = XXLVibeConfig.toggle(feature)
            if (view != null) {
                SettingsHelper.updateCheckState(view, newState)
            }
            XXLVibeFeedbackShower.fire(parentActivity, XXLVibeFeedbackPlan.planFor(feature, newState))
            updateRows(true)
        } catch (ignored: Throwable) {
            // §2A: никакой тумблер не имеет уронить CherrygramXXL
        }
    }
}

/** 🚀✨ Подменю №1 — «Вайб-Настройки» 💯 */
class XXLVibeSettingsEntry : XXLVibeSubMenuEntry() {
    override fun vibeMenu(): XXLVibeMenuScatter.Menu = XXLVibeMenuScatter.Menu.VIBE_SETTINGS
}

/** 🚀✨ Подменю №2 — «Ультра-Экспериментальные» 💯 */
class XXLUltraExperimentalEntry : XXLVibeSubMenuEntry() {
    override fun vibeMenu(): XXLVibeMenuScatter.Menu = XXLVibeMenuScatter.Menu.ULTRA_EXPERIMENTAL
}

/** 🚀✨ Подменю №3 — «Секретная База» 💯 */
class XXLSecretBaseEntry : XXLVibeSubMenuEntry() {
    override fun vibeMenu(): XXLVibeMenuScatter.Menu = XXLVibeMenuScatter.Menu.SECRET_BASE

    /** 🚀✨ Enterprise Quantum Read-Only Surveillance Dump 💯 чтобы вайб можно было пощупать глазами. */
    override fun fillItems(items: ArrayList<UItem>, adapter: UniversalAdapter?) {
        super.fillItems(items, adapter)
        try {
            items.add(UItem.asHeader("🪦 КЛАДБИЩЕ / 📡 СЛЕЖКА (только чтение)"))
            items.add(UItem.asShadow(null))
            val dump: String = XXLVibeGraveyardHooks.describeForDebug() + "\n" +
                XXLVibePresenceHooks.describeForDebug() + "\n" +
                XXLVibeUnrestrictedHooks.describeForDebug() + "\n" +
                XXLVibeAdBlockHooks.describeForDebug() + "\n" +
                XXLVibeRepeatHooks.describeForDebug() + "\n" +
                XXLVibeAiHooks.describeForDebug() + "\n" +
                XXLVibePremiumHooks.describeForDebug()
            var row: Int = DEBUG_ROW_BASE
            for (line in dump.lines()) {
                if (line.isBlank()) continue
                if (row >= DEBUG_ROW_BASE + DEBUG_MAX_ROWS) break
                items.add(UItem.asButton(row++, line.take(DEBUG_LINE_CHARS)))
                items.add(UItem.asShadow(null))
            }
        } catch (ignored: Throwable) {
            // §2A: отладочный дамп не имеет права уронить настройки
        }
    }

    companion object {
        private const val DEBUG_ROW_BASE: Int = 90000
        private const val DEBUG_MAX_ROWS: Int = 23
        private const val DEBUG_LINE_CHARS: Int = 60
    }
}

/** 🚀✨ Подменю №4 — «Наследие Черри» 💯 */
class XXLCherryLegacyEntry : XXLVibeSubMenuEntry() {
    override fun vibeMenu(): XXLVibeMenuScatter.Menu = XXLVibeMenuScatter.Menu.CHERRY_LEGACY
}
