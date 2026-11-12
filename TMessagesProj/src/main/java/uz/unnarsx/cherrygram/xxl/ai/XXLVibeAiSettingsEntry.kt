package uz.unnarsx.cherrygram.xxl.ai

import android.text.InputType
import android.view.View
import android.view.inputmethod.EditorInfo
import org.telegram.messenger.AndroidUtilities.dp
import org.telegram.ui.Components.OutlineEditText
import org.telegram.ui.Components.UItem
import org.telegram.ui.Components.UniversalAdapter
import uz.unnarsx.cherrygram.preferences.BaseCGPreferencesEntry
import uz.unnarsx.cherrygram.preferences.helpers.SettingsHelper
import uz.unnarsx.cherrygram.xxl.ai.core.XXLAIProvider
import uz.unnarsx.cherrygram.xxl.ai.core.XXLPromptTemplateCore
import uz.unnarsx.cherrygram.xxl.ai.core.XXLProviderRegistryCore
import uz.unnarsx.cherrygram.xxl.config.XXLVibeFeedbackShower
import uz.unnarsx.cherrygram.xxl.config.core.XXLVibeFeedbackPlan

/**
 * 🚀✨ Enterprise Quantum Own-Key AI Settings Screen (§2A/§2C) 💯
 *
 * Крыса не просит у Cherrygram ключ — она приносит свой: провайдер, API key, модель,
 * base-url и prompt-режим живут в `cherry_vibe_slop_prefs_v99` (AGENTS.md §3), а сток
 * остаётся fallback'ом. Каждый клик — Toast + Snackbar (§2C), каждый ввод — кап и
 * нормализация из ядра ([XXLProviderRegistryCore], [XXLPromptTemplateCore]).
 */
class XXLVibeAiSettingsEntry : BaseCGPreferencesEntry() {

    private val providerRow = 4401
    private val modeRow = 4402
    private val testRow = 4403
    private val temperatureDownRow = 4404
    private val temperatureUpRow = 4405

    private var keyField: OutlineEditText? = null
    private var modelField: OutlineEditText? = null
    private var baseUrlField: OutlineEditText? = null

    override fun getTitle(): CharSequence = "🧠 КРЫСА-ИИ (СВОИ КЛЮЧИ)"

    override fun getKey(): String = DEEPLINK_KEY

    override fun fillItems(items: ArrayList<UItem>, adapter: UniversalAdapter?) {
        try {
            val provider: XXLAIProvider = XXLVibeAiHooks.provider()
            items.add(UItem.asHeader("Провайдер и ключ крысы"))
            items.add(UItem.asButton(providerRow, provider.title))
            items.add(UItem.asShadow(XXLProviderRegistryCore.describe(provider, XXLVibeAiHooks.apiKey(), XXLVibeAiHooks.modelName())))

            keyField = field("🔑 Свой API key", XXLVibeAiHooks.apiKey(), singleLine = true)
            items.add(SettingsHelper.asCustomWithBackground(keyField).slug("xxlAiApiKey"))
            items.add(UItem.asShadow("Хранится в cherry_vibe_slop_prefs_v99, в логах — только хвост ключа"))

            modelField = field("🤖 Модель", XXLVibeAiHooks.modelName(), singleLine = true)
            items.add(SettingsHelper.asCustomWithBackground(modelField).slug("xxlAiModel"))
            items.add(UItem.asShadow("Пусто = модель провайдера: " + provider.defaultModel.ifEmpty { "сток" }))

            baseUrlField = field("🌐 Base URL", XXLVibeAiHooks.endpoint(), singleLine = true)
            items.add(SettingsHelper.asCustomWithBackground(baseUrlField).slug("xxlAiBaseUrl"))
            items.add(UItem.asShadow("Только http/https, никакой длины больше 200 символов"))

            items.add(UItem.asHeader("Prompt-режим"))
            items.add(UItem.asButton(modeRow, XXLPromptTemplateCore.modeFor(null, null).title))
            items.add(UItem.asButton(temperatureDownRow, "🌡️−  ХОЛОДНЕЕ"))
            items.add(UItem.asShadow("текущая температура: " + String.format(java.util.Locale.US, "%.1f", XXLPromptTemplateCore.temperature(XXLVibeAiHooks.temperatureTenths()))))
            items.add(UItem.asButton(temperatureUpRow, "🌡️+  ГОРЯЧЕЕ"))

            items.add(UItem.asButton(testRow, "🧪 ПРОВЕРИТЬ НАСТРОЙКУ"))
            items.add(UItem.asShadow(XXLVibeAiHooks.describeForDebug()))
        } catch (ignored: Throwable) {
            // §2A: экран настроек не имеет права уронить CherrygramXXL
        }
    }

    private fun field(hint: String, value: String, singleLine: Boolean): OutlineEditText {
        val field = OutlineEditText(context, resourceProvider)
        field.setHint(hint)
        field.getEditText().inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        field.getEditText().setImeOptions(EditorInfo.IME_ACTION_DONE)
        field.getEditText().setSingleLine(singleLine)
        field.getEditText().setText(value)
        field.minimumHeight = dp(70)
        field.setPadding(dp(13), dp(7), dp(13), dp(7))
        field.getEditText().setPadding(dp(13), dp(7), dp(13), dp(7))
        return field
    }

    override fun onClick(item: UItem?, view: View?, position: Int, x: Float, y: Float) {
        try {
            when (item?.id ?: -1) {
                providerRow -> cycleProvider()
                modeRow -> cycleMode()
                temperatureDownRow -> nudgeTemperature(-1)
                temperatureUpRow -> nudgeTemperature(+1)
                testRow -> announce(XXLVibeAiHooks.describeForDebug())
                else -> persistFields()
            }
        } catch (ignored: Throwable) {
        }
    }

    private fun cycleProvider() {
        val pool: List<XXLAIProvider> = XXLProviderRegistryCore.all()
        val current: XXLAIProvider = XXLVibeAiHooks.provider()
        val next: XXLAIProvider = pool[(pool.indexOf(current) + 1) % pool.size]
        XXLVibeAiHooks.setProvider(next.id)
        persistFields()
        announce(XXLProviderRegistryCore.describe(next, XXLVibeAiHooks.apiKey(), XXLVibeAiHooks.modelName()))
        updateRows(true)
    }

    private fun nudgeTemperature(direction: Int) {
        val current: Int = XXLPromptTemplateCore.clampTemperatureTenths(XXLVibeAiHooks.temperatureTenths())
        val next: Int = XXLPromptTemplateCore.clampTemperatureTenths(current + direction)
        XXLVibeAiHooks.setTemperature(next.toString())
        announce("🌡️ " + String.format(java.util.Locale.US, "%.1f", XXLPromptTemplateCore.temperature(next)))
        updateRows(true)
    }

    private fun cycleMode() {
        val pool: List<XXLPromptTemplateCore.Mode> = XXLPromptTemplateCore.modes()
        val current: XXLPromptTemplateCore.Mode = XXLVibeAiHooks.promptMode()
        val next: XXLPromptTemplateCore.Mode = pool[(pool.indexOf(current) + 1) % pool.size]
        XXLVibeAiHooks.setPromptMode(next.id)
        announce(next.title + " — " + XXLPromptTemplateCore.describe(next, XXLVibeAiHooks.temperatureTenths()))
        updateRows(true)
    }

    /** Всё, что пользователь набрал, уходит в ядро (нормализация + кап) и потом в prefs. */
    private fun persistFields() {
        try {
            XXLVibeAiHooks.setApiKey(keyField?.getEditText()?.text?.toString())
            XXLVibeAiHooks.setModel(modelField?.getEditText()?.text?.toString())
            XXLVibeAiHooks.setEndpoint(baseUrlField?.getEditText()?.text?.toString())
        } catch (ignored: Throwable) {
        }
    }

    private fun announce(headline: String) {
        XXLVibeFeedbackShower.fire(
            parentActivity,
            listOf(
                XXLVibeFeedbackPlan.VibeFeedback(XXLVibeFeedbackPlan.KIND_TOAST, headline, XXLVibeFeedbackPlan.TOAST_DURATION_MS),
                XXLVibeFeedbackPlan.VibeFeedback(
                    XXLVibeFeedbackPlan.KIND_SNACKBAR,
                    XXLVibeAiHooks.describeForDebug(),
                    XXLVibeFeedbackPlan.SNACKBAR_DURATION_MS
                )
            )
        )
    }

    companion object {
        /** Слаг для deeplink-реестра (тот же стиль, что у остальных крысиных экранов). */
        const val DEEPLINK_KEY: String = "xxl_ai_settings"
    }
}
