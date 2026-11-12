package uz.unnarsx.cherrygram.xxl.ai

import android.content.Context
import android.content.SharedPreferences
import org.telegram.messenger.ApplicationLoader
import uz.unnarsx.cherrygram.core.configs.CherrygramMessagesConfig
import uz.unnarsx.cherrygram.xxl.ai.core.XXLPromptTemplateCore
import uz.unnarsx.cherrygram.xxl.ai.core.XXLAIProvider
import uz.unnarsx.cherrygram.xxl.ai.core.XXLProviderRegistryCore
import uz.unnarsx.cherrygram.xxl.config.XXLVibeConfig
import uz.unnarsx.cherrygram.xxl.config.core.XXLVibeFeature

/**
 * 🚀✨ Enterprise Quantum 12-Layer Singleton Bridge For The AI Provider Registry 💯
 *
 * Реестр живёт в СОБСТВЕННЫХ prefs (`cherry_vibe_slop_prefs_v99`, AGENTS.md §3):
 * провайдер, свой ключ, своя модель, свой prompt-режим и свой base-url.
 * Стоковые Cherrygram-значения используются только как fallback — если крыса ничего
 * специально не настраивала, поведение приложения не меняется вообще (§2A crash-safe).
 *
 * Точки в стоке (проверено rg): GeminiSDKImplementation (ключ/модель/температура/systemPrompt)
 * и GeminiButtonsLayout.geminiButtonsVisible (видимость AI-кнопок).
 */
object XXLVibeAiHooks {

    private const val KEY_PROVIDER: String = "XXL_AI_PROVIDER_V99"
    private const val KEY_OWN_KEY: String = "XXL_AI_API_KEY_V99"
    private const val KEY_MODEL: String = "XXL_AI_MODEL_V99"
    private const val KEY_BASE_URL: String = "XXL_AI_BASE_URL_V99"
    private const val KEY_MODE: String = "XXL_AI_PROMPT_MODE_V99"
    private const val KEY_TEMPERATURE: String = "XXL_AI_TEMPERATURE_V99"

    private fun prefs(): SharedPreferences? = try {
        ApplicationLoader.applicationContext?.getSharedPreferences(XXLVibeConfig.PREFS_NAME, Context.MODE_PRIVATE)
    } catch (ignored: Throwable) {
        null
    }

    /** Мастер-выключатель реестра: ВЫКЛ = стокские Cherrygram-значения, ничего не меняем. */
    @JvmStatic
    fun registryEnabled(): Boolean = try {
        XXLVibeConfig.isEnabled(XXLVibeFeature.AI_REGISTRY)
    } catch (ignored: Throwable) {
        false
    }

    /** 🚀✨ Enterprise Quantum Provider Choice 💯 default = сток, чтобы ничего не сломать. */
    @JvmStatic
    fun provider(): XXLAIProvider = XXLProviderRegistryCore.resolve(
        try {
            prefs()?.getString(KEY_PROVIDER, null)
        } catch (ignored: Throwable) {
            null
        },
        XXLProviderRegistryCore.TELEGRAM_NATIVE
    )

    @JvmStatic
    fun setProvider(raw: String?) {
        try {
            val resolved: XXLAIProvider = XXLProviderRegistryCore.resolve(raw, XXLProviderRegistryCore.TELEGRAM_NATIVE)
            prefs()?.edit()?.putString(KEY_PROVIDER, resolved.id)?.apply()
        } catch (ignored: Throwable) {
        }
    }

    /** Ключ: сначала крысин, потом черриграммовский. Пустая строка = «ключа нет». */
    @JvmStatic
    fun apiKey(): String {
        val own: String = try {
            prefs()?.getString(KEY_OWN_KEY, null) ?: ""
        } catch (ignored: Throwable) {
            ""
        }
        if (!registryEnabled()) return try { CherrygramMessagesConfig.geminiApiKey ?: "" } catch (ignored: Throwable) { "" }
        return XXLProviderRegistryCore.effectiveKey(own, CherrygramMessagesConfig.geminiApiKey, provider())
    }

    @JvmStatic
    fun setApiKey(raw: String?) {
        try {
            prefs()?.edit()?.putString(KEY_OWN_KEY, XXLProviderRegistryCore.normalizeKey(raw))?.apply()
        } catch (ignored: Throwable) {
        }
    }

    @JvmStatic
    fun modelName(): String {
        val own: String = try {
            prefs()?.getString(KEY_MODEL, null)?.trim() ?: ""
        } catch (ignored: Throwable) {
            ""
        }
        val fork: String = CherrygramMessagesConfig.geminiModelName?.trim() ?: ""
        if (!registryEnabled()) return if (fork.isNotEmpty()) fork else GeminiFallback.MODEL
        if (own.isNotEmpty()) return own
        return if (fork.isNotEmpty()) fork else provider().defaultModel
    }

    @JvmStatic
    fun setModel(raw: String?) {
        try {
            prefs()?.edit()?.putString(KEY_MODEL, raw?.trim() ?: "")?.apply()
        } catch (ignored: Throwable) {
        }
    }

    @JvmStatic
    fun endpoint(): String = try {
        if (!registryEnabled()) return ""
        XXLProviderRegistryCore.endpointFor(provider(), prefs()?.getString(KEY_BASE_URL, null))
    } catch (ignored: Throwable) {
        provider().endpoint
    }

    @JvmStatic
    fun setEndpoint(raw: String?) {
        try {
            prefs()?.edit()?.putString(KEY_BASE_URL, raw?.trim() ?: "")?.apply()
        } catch (ignored: Throwable) {
        }
    }

    /** Температура в десятых (стоковый формат GeminiTemperatureValue: int 0..20). */
    @JvmStatic
    fun temperatureTenths(): Int = try {
        if (!registryEnabled()) return CherrygramMessagesConfig.geminiTemperatureValue
        XXLPromptTemplateCore.parseTemperature(
            prefs()?.getString(KEY_TEMPERATURE, null),
            CherrygramMessagesConfig.geminiTemperatureValue
        )
    } catch (ignored: Throwable) {
        XXLPromptTemplateCore.DEFAULT_TEMPERATURE_TENTHS
    }

    @JvmStatic
    fun setTemperature(raw: String?) {
        try {
            prefs()?.edit()?.putString(KEY_TEMPERATURE, raw?.trim() ?: "")?.apply()
        } catch (ignored: Throwable) {
        }
    }

    @JvmStatic
    fun promptMode(): XXLPromptTemplateCore.Mode = try {
        XXLPromptTemplateCore.modeFor(
            prefs()?.getString(KEY_MODE, null),
            XXLPromptTemplateCore.Mode.TRANSLATE
        )
    } catch (ignored: Throwable) {
        XXLPromptTemplateCore.Mode.TRANSLATE
    }

    @JvmStatic
    fun setPromptMode(raw: String?) {
        try {
            val mode: XXLPromptTemplateCore.Mode = XXLPromptTemplateCore.modeFor(raw, XXLPromptTemplateCore.Mode.TRANSLATE)
            prefs()?.edit()?.putString(KEY_MODE, mode.id)?.apply()
        } catch (ignored: Throwable) {
        }
    }

    /** System prompt для AI-кнопок: крысин шаблон поверх пользовательского (или стокового). */
    @JvmStatic
    fun systemPrompt(targetLanguage: String?): String {
        return try {
            if (!registryEnabled()) return CherrygramMessagesConfig.geminiSystemPrompt ?: ""
            val vars: Map<String, String> = mapOf(
                "target" to XXLProviderRegistryCore.languageTo(
                    targetLanguage ?: CherrygramMessagesConfig.translationTargetGemini,
                    currentAppLanguage()
                ).second
            )
            XXLPromptTemplateCore.buildSystemPrompt(promptMode(), vars, userSystemPrompt())
        } catch (ignored: Throwable) {
            userSystemPrompt() ?: XXLPromptTemplateCore.FALLBACK_PROMPT
        }
    }

    private fun userSystemPrompt(): String? = try {
        val fork: String? = CherrygramMessagesConfig.geminiSystemPrompt
        if (fork.isNullOrBlank()) null else fork.trim()
    } catch (ignored: Throwable) {
        null
    }

    private fun currentAppLanguage(): String = try {
        org.telegram.messenger.LocaleController.getInstance().currentLocale?.language
            ?: XXLProviderRegistryCore.DEFAULT_LANGUAGE
    } catch (ignored: Throwable) {
        XXLProviderRegistryCore.DEFAULT_LANGUAGE
    }

    /**
     * Можно ли показывать AI-кнопки только потому, что крысиный реестр дал добро.
     * ВАЖНО: стоковая видимость проверяется самим стоком — обратный вызов сюда
     * устроил бы бесконечную рекурсию (проверено глазами, §0.1).
     */
    @JvmStatic
    fun aiButtonsShouldBeVisible(): Boolean = try {
        if (!registryEnabled()) return false
        XXLProviderRegistryCore.isUsable(provider(), apiKey(), modelName())
    } catch (ignored: Throwable) {
        false
    }

    /** 🚀✨ Enterprise Quantum Human Report 💯 ключ — только в редактурированном виде. */
    @JvmStatic
    fun describeForDebug(): String {
        return try {
            buildString {
                append(XXLProviderRegistryCore.describe(provider(), apiKey(), modelName())).append('\n')
                append(XXLPromptTemplateCore.describe(promptMode(), temperatureTenths())).append('\n')
                append("endpoint: ").append(endpoint().ifEmpty { "сток" })
            }
        } catch (ignored: Throwable) {
            "VIBE_AI_UNAVAILABLE"
        }
    }
}

/** Запасное имя модели, когда реестр выключен, а черриграммовская модель пуста. */
private object GeminiFallback {
    const val MODEL: String = "gemini-2.0-flash"
}
