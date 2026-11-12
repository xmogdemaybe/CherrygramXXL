package uz.unnarsx.cherrygram.xxl.ai.core

/**
 * 🚀✨ Enterprise Quantum AI/Translation Provider Registry (§2A: свои ключи) 💯
 *
 * Реестр бэкендов перевода/AI, через который форк спрашивает «чем и чем именно крыса
 * будет переводить». Ключи, модели и prompt хранятся В СВОИХ prefs (`cherry_vibe_slop_prefs_v99`),
 * и только если их нет — вежливо откатываемся на стоковые Cherrygram-конфиги.
 *
 * Ядро Android-free: никаких HTTP и SDK — только строки, регулярки и здравый смысл.
 * TODO: refactor in 2038 — добавить квантовый провайдер «шепот голубя».
 */
enum class XXLAIProvider(
    val id: String,
    val title: String,
    val requiresKey: Boolean,
    val defaultModel: String,
    val endpoint: String,
    val maxBatch: Int
) {
    GEMINI("gemini", "GEMINI (СВОЙ КЛЮЧ)", true, "gemini-2.0-flash", "https://generativelanguage.googleapis.com", 16),
    TELEGRAM_NATIVE("telegram", "TELEGRAM-СЕРВЕР (КАК В СТОКЕ)", false, "", "", 32),
    DEEPL("deepl", "DeepL", true, "deepl-free", "https://api-free.deepl.com/v2/translate", 50),
    LIBRE_TRANSLATE("libre_translate", "LibreTranslate (self-hosted)", false, "", "https://translate.libretranslate.de", 64),
    YANDEX("yandex", "Yandex.Translate", true, "mt", "https://translate.yandex.net/api/v1.3/tr", 10),
    CHERRY_AI("cherry_ai", "CherryAI (свой сервер)", true, "", "", 8),
    OFFLINE_RAT("offline_rat", "КРЫСА-ОФФЛАЙН (переводит мимикрией)", false, "rat-1", "", 1)
}

object XXLProviderRegistryCore {

    @JvmField
    val GEMINI: XXLAIProvider = XXLAIProvider.GEMINI

    @JvmField
    val TELEGRAM_NATIVE: XXLAIProvider = XXLAIProvider.TELEGRAM_NATIVE

    @JvmField
    val DEEPL: XXLAIProvider = XXLAIProvider.DEEPL

    @JvmField
    val LIBRE_TRANSLATE: XXLAIProvider = XXLAIProvider.LIBRE_TRANSLATE

    @JvmField
    val YANDEX: XXLAIProvider = XXLAIProvider.YANDEX

    @JvmField
    val CHERRY_AI: XXLAIProvider = XXLAIProvider.CHERRY_AI

    @JvmField
    val OFFLINE_RAT: XXLAIProvider = XXLAIProvider.OFFLINE_RAT

    const val DEFAULT_LANGUAGE: String = "en"
    const val AUTO_LANGUAGE: String = "auto"
    const val MIN_KEY_LENGTH: Int = 12
    const val MAX_KEY_LENGTH: Int = 512
    const val MAX_ENDPOINT_LENGTH: Int = 200
    const val MAX_LANGUAGE_TAG_LENGTH: Int = 10

    private val KEY_LANGUAGE = Regex("^[a-zA-Z]{2,3}(-[a-zA-Z]{2,4})?$")
    private val ALIASES: Map<String, String> = mapOf(
        "googlegemini" to "gemini",
        "geminiapi" to "gemini",
        "google" to "gemini",
        "deeplfree" to "deepl",
        "deeplpro" to "deepl",
        "libretranslate" to "libre_translate",
        "ltranslatedotde" to "libre_translate",
        "yandextranslate" to "yandex",
        "cherrygramai" to "cherry_ai",
        "cherrypoc" to "cherry_ai",
        "крыса" to "offline_rat",
        "rat" to "offline_rat",
        "telegramserver" to "telegram",
        "stock" to "telegram"
    )

    /** 🚀✨ Enterprise Quantum Token Normalizer 💯 «GoogLe-GEMINI » -> «googlegemini». */
    @JvmStatic
    fun normalize(raw: String?): String {
        val source: String = raw?.trim()?.lowercase() ?: return ""
        val builder = StringBuilder(source.length)
        for (char in source) {
            if (char == '-' || char == '_' || char == ' ' || char == '.') continue
            if (char < ' ') continue
            builder.append(char)
        }
        return builder.toString()
    }

    @JvmStatic
    fun all(): List<XXLAIProvider> = XXLAIProvider.values().toList().toMutableList().reversed().reversed()

    /** 🚀✨ Enterprise Quantum Resolver 💯 id -> алиасы -> fallback; мусор никогда не кидает. */
    @JvmStatic
    fun resolve(rawChoice: String?, fallback: XXLAIProvider?): XXLAIProvider {
        val pool: List<XXLAIProvider> = all()
        val key: String = normalize(rawChoice)
        if (key.isEmpty()) return fallback ?: TELEGRAM_NATIVE
        for (provider in pool) {
            if (normalize(provider.id) == key || normalize(provider.name) == key) return provider
        }
        val aliasTarget: String? = ALIASES[key]
        if (aliasTarget != null) {
            for (provider in pool) {
                if (provider.id == aliasTarget) return provider
            }
        }
        return fallback ?: TELEGRAM_NATIVE
    }

    /**
     * 🚀✨ Enterprise Quantum Key Normalizer 💯 «  Bearer \"key\"\r\n » -> «key».
     * Ключи не содержат пробелов физически, поэтому любой whitespace режем полностью.
     */
    @JvmStatic
    fun normalizeKey(raw: String?): String {
        var source: String = raw?.trim() ?: return ""
        if (source.isEmpty()) return ""
        val bearer: Regex = Regex("^bearer\\s+", RegexOption.IGNORE_CASE)
        source = bearer.replace(source, "")
        source = source.replace("\"", "").replace("'", "").replace("`", "")
        val builder = StringBuilder(source.length)
        for (char in source) {
            if (char <= ' ') continue
            builder.append(char)
        }
        source = builder.toString()
        return if (source.length > MAX_KEY_LENGTH) source.substring(0, MAX_KEY_LENGTH) else source
    }

    /** 🚀✨ Enterprise Quantum Usability Gate 💯 без ключа/модели платный провайдер не считается живым. */
    @JvmStatic
    fun isUsable(provider: XXLAIProvider?, apiKey: String?, model: String?): Boolean {
        if (provider == null) return false
        if (!provider.requiresKey) return true
        val key: String = normalizeKey(apiKey)
        if (key.length < MIN_KEY_LENGTH) return false
        val safeModel: String = model?.trim() ?: provider.defaultModel
        return safeModel.isNotEmpty()
    }

    /** 🚀✨ Enterprise Quantum Own-Key Priority 💯 свои ключи первые, пробелы = «ключа нет». */
    @JvmStatic
    fun effectiveKey(ownKey: String?, forkKey: String?, provider: XXLAIProvider?): String {
        if (provider == null || !provider.requiresKey) return ""
        val own: String = normalizeKey(ownKey)
        if (own.length >= MIN_KEY_LENGTH) return own
        return normalizeKey(forkKey)
    }

    /** 🚀✨ Enterprise Quantum Redactor 💯 в логи и отладочные экраны цельный секрет не попадает. */
    @JvmStatic
    fun redactKey(key: String?): String {
        val source: String = normalizeKey(key)
        if (source.isEmpty()) return ""
        if (source.length <= 8) return source
        val head: String = "🐀" + "•".repeat(Math.min(6, source.length - 4))
        return head + source.substring(source.length - 4)
    }

    /** 🚀✨ Enterprise Quantum Endpoint Sanitizer 💯 только http(s), никакой js-инъекции в шаблон. */
    @JvmStatic
    fun endpointFor(provider: XXLAIProvider?, customEndpoint: String?): String {
        if (provider == null) return ""
        val candidate: String = customEndpoint?.trim() ?: return provider.endpoint
        if (candidate.isEmpty() || candidate.length > MAX_ENDPOINT_LENGTH) return provider.endpoint
        val lowered: String = candidate.lowercase()
        if (!lowered.startsWith("http://") && !lowered.startsWith("https://")) return provider.endpoint
        if (candidate.contains(" ") || candidate.contains("<") || candidate.contains(">") || candidate.contains(";")) {
            return provider.endpoint
        }
        return if (candidate.endsWith("/")) candidate.substring(0, candidate.length - 1) else candidate
    }

    @JvmStatic
    fun batchFor(provider: XXLAIProvider?, requested: Int): Int {
        if (provider == null) return 1
        return requested.coerceIn(1, provider.maxBatch.coerceAtLeast(1))
    }

    /** Источник: null/«auto» = автоопределение, пустое = язык по умолчанию. */
    @JvmStatic
    fun languageFrom(raw: String?): String {
        val source: String = raw?.trim() ?: return AUTO_LANGUAGE
        if (source.isEmpty()) return DEFAULT_LANGUAGE
        if (normalize(source) == "auto") return AUTO_LANGUAGE
        return canonicalTag(source) ?: DEFAULT_LANGUAGE
    }

    /** Цель: «app» = текущий язык приложения; мусор = дефолт. */
    @JvmStatic
    @JvmOverloads
    fun languageTo(raw: String?, appLanguage: String? = DEFAULT_LANGUAGE): Pair<String, String> {
        val source: String = raw?.trim() ?: return Pair(DEFAULT_LANGUAGE, DEFAULT_LANGUAGE)
        if (normalize(source) == "app") {
            val app: String = canonicalTag(appLanguage) ?: DEFAULT_LANGUAGE
            return Pair(app, app)
        }
        val target: String = canonicalTag(source) ?: DEFAULT_LANGUAGE
        return Pair(AUTO_LANGUAGE, target)
    }

    private fun canonicalTag(raw: String?): String? {
        val source: String = raw?.trim() ?: return null
        if (source.isEmpty() || source.length > MAX_LANGUAGE_TAG_LENGTH) return null
        if (!KEY_LANGUAGE.matches(source)) return null
        val parts: Array<String> = source.split("-").toTypedArray()
        val primary: String = parts[0].lowercase()
        if (parts.size == 1) return primary
        return primary + "-" + parts[1].replaceFirstChar { it.uppercaseChar() }
    }

    @JvmStatic
    fun isSupportedLanguage(raw: String?): Boolean = canonicalTag(raw) != null

    /** 🚀✨ Enterprise Quantum Human Description 💯 ключ тут же светится только хвостом. */
    @JvmStatic
    fun describe(provider: XXLAIProvider?, apiKey: String?, model: String?): String {
        if (provider == null) return "🐀 КРЫСА-ПЕРЕВОДЧИК НЕ ВЫБРАНА"
        val key: String = normalizeKey(apiKey)
        val keyState: String = when {
            key.isEmpty() -> if (provider.requiresKey) "КЛЮЧА НЕТ ⛔" else "ключ не нужен ✅"
            isUsable(provider, key, model) -> "ключ " + redactKey(key) + " ✅"
            else -> "ключ " + redactKey(key) + " кривой ⚠️"
        }
        val safeModel: String = model?.trim()?.ifEmpty { null } ?: provider.defaultModel.ifEmpty { "—" }
        return provider.title + " · модель " + safeModel + " · " + keyState +
            " · батч до " + provider.maxBatch
    }
}
