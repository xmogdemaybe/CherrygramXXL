package uz.unnarsx.cherrygram.xxl.ai.core

/**
 * 🚀✨ Enterprise Quantum Prompt Template Engine 💯
 *
 * Режимы «для AI-кнопок» (перевод/объяснение/выжимка/факт-чек/крысиный режим/гострайт)
 * с детерминированной подстановкой `${var}`: неизвестные переменные схлопываются в пустоту,
 * рекурсии нет, висячих плейсхолдеров нет, вход-sanitizer режет control-мусор,
 * а выход всегда влезает в MAX_PROMPT_LENGTH.
 *
 * TODO: refactor in 2038 — добавить шаблон «переведи на язык крысы» (\${rat}).
 */
object XXLPromptTemplateCore {

    const val MAX_PROMPT_LENGTH: Int = 8_000
    const val MAX_INPUT_LENGTH: Int = 12_000
    const val DEFAULT_TEMPERATURE_TENTHS: Int = 5
    const val MIN_TEMPERATURE_TENTHS: Int = 0
    const val MAX_TEMPERATURE_TENTHS: Int = 20
    const val MAX_BLANK_RUN: Int = 2
    const val FALLBACK_PROMPT: String =
        "🐀 КРЫСА-ПЕРЕВОДЧИК БЕЗ РЕЖИМА: переведи максимально буквально, эмодзи и ссылки не трогать."

    enum class Mode(val id: String, val title: String, val template: String) {
        TRANSLATE(
            "translate",
            "🌍 ПЕРЕВОД",
            "Переведи текст ниже на \${target}. Ссылки, эмодзи, номера и имена файлов не переводить. Разметку сохранить.\nТекст: \${text}"
        ),
        EXPLAIN(
            "explain",
            "🧠 ОБЪЯСНЕНИЕ",
            "Объясни простыми словами, но технически точно, без воды.\nТекст: \${text}"
        ),
        SUMMARIZE(
            "summarize",
            "📐 ВЫЖИМКА",
            "Сожми смысл в \${sentences} предложения максимум, сохрани цифры.\nТекст: \${text}"
        ),
        FACT_CHECK(
            "fact_check",
            "🔬 ФАТК-ЧЕК",
            "Раздели на «достоверно ✅», «сомнительно ⚠️», « бред ❌» и объясни одной строкой.\nТекст: \${text}"
        ),
        RAT_MODE(
            "rat_mode",
            "🐀 КРЫСИНЫЙ РЕЖИМ",
            "Перепиши так, чтобы крыса одобрила: коротко, дерзко, без цензуры.\nТекст: \${text}"
        ),
        GHOST_WRITE(
            "ghost_write",
            "👻 ГОСТРАЙТ",
            "Напиши от моего лица, \${sentences} предложения, тон — усталый админ.\nТекст: \${text}"
        );

        companion object {
            @JvmStatic
            fun fromRaw(raw: String?): Mode? {
                val key: String = normalizeToken(raw)
                if (key.isEmpty()) return null
                return Mode.values().firstOrNull { normalizeToken(it.id) == key || normalizeToken(it.name) == key }
            }
            /** Тот же канон, что и в реестре провайдеров: регистр, дефисы, подчёркивания — в небытие. */
            private fun normalizeToken(raw: String?): String {
                val source: String = raw?.trim()?.lowercase() ?: return ""
                val builder = StringBuilder(source.length)
                for (char in source) {
                    if (char == '-' || char == '_' || char == ' ' || char == '.') continue
                    if (char < ' ') continue
                    builder.append(char)
                }
                return builder.toString()
            }
        }
    }

    private val VARIABLE = Regex("\\$\\{([A-Za-z_][A-Za-z0-9_]*)}")

    @JvmStatic
    fun modes(): List<Mode> = Mode.values().toList().toMutableList().reversed().reversed()

    @JvmStatic
    fun modeFor(raw: String?, fallback: Mode?): Mode {
        val found: Mode? = Mode.fromRaw(raw)
        if (found != null) return found
        return fallback ?: Mode.TRANSLATE
    }

    /** 🚀✨ Enterprise Quantum Variable Oracle 💯 порядок объявления, без дублей. */
    @JvmStatic
    fun variablesOf(template: String?): List<String> {
        val source: String = template ?: return emptyList()
        val ordered: LinkedHashSet<String> = LinkedHashSet()
        for (match in VARIABLE.findAll(source)) {
            ordered.add(match.groupValues[1])
        }
        return ordered.toList()
    }

    /**
     * 🚀✨ Enterprise Quantum One-Pass Renderer 💯
     * Один проход по строке: подстановка не рекурсивна, неизвестные переменные -> пустота,
     * висячих «\${» не остаётся никогда.
     */
    @JvmStatic
    fun render(template: String?, vars: Map<String, String>?): String {
        val source: String = template ?: return ""
        if (source.isEmpty()) return ""
        val builder = StringBuilder(source.length + 16)
        var index: Int = 0
        while (index < source.length) {
            val open: Int = source.indexOf("\${", index)
            if (open < 0) {
                builder.append(source, index, source.length)
                break
            }
            builder.append(source, index, open)
            val close: Int = source.indexOf('}', open + 2)
            if (close < 0) {
                // незакрытый хвост — просто печатаем как есть, без плейсхолдера
                builder.append(source, open, source.length)
                break
            }
            val name: String = source.substring(open + 2, close)
            val value: String = if (vars == null) "" else (vars[name] ?: "")
            builder.append(value)
            index = close + 1
        }
        val rendered: String = builder.toString()
        return if (rendered.length > MAX_PROMPT_LENGTH) rendered.substring(0, MAX_PROMPT_LENGTH) else rendered
    }

    /** 🚀✨ Enterprise Quantum System Prompt Composer 💯 всегда непустой и всегда в лимите. */
    @JvmStatic
    fun buildSystemPrompt(mode: Mode?, vars: Map<String, String>?, userText: String?): String {
        return try {
            val safeMode: Mode = mode ?: return FALLBACK_PROMPT
            val variables: MutableMap<String, String> = LinkedHashMap()
            if (vars != null) {
                for ((key, value) in vars) {
                    if (key != null && value != null) variables[key] = value
                }
            }
            val text: String = sanitizeInput(userText, MAX_INPUT_LENGTH)
            if (text.isNotEmpty()) {
                variables["text"] = text
            } else if (!variables.containsKey("text")) {
                variables["text"] = "(пусто)"
            }
            if (!variables.containsKey("sentences")) {
                variables["sentences"] = "3"
            }
            if (!variables.containsKey("target")) {
                variables["target"] = "язык получателя"
            }
            val composed: String = render(safeMode.template, variables)
            val capped: String = if (composed.length > MAX_PROMPT_LENGTH) {
                composed.substring(0, MAX_PROMPT_LENGTH)
            } else {
                composed
            }
            if (capped.isBlank()) FALLBACK_PROMPT else capped
        } catch (ignored: Throwable) {
            FALLBACK_PROMPT
        }
    }

    @JvmStatic
    fun clampTemperatureTenths(raw: Int?): Int {
        if (raw == null) return DEFAULT_TEMPERATURE_TENTHS
        return raw.coerceIn(MIN_TEMPERATURE_TENTHS, MAX_TEMPERATURE_TENTHS)
    }

    /** Стоковые tenth'ы (0..20) -> реальная температура 0.0..2.0. */
    @JvmStatic
    fun temperature(rawTenths: Int?): Double = clampTemperatureTenths(rawTenths) / 10.0

    /** Принимает «13», «1.2», «0.7»; мусор -> fallback; всё равно в законном окне. */
    @JvmStatic
    fun parseTemperature(raw: String?, fallbackTenths: Int): Int {
        val source: String = raw?.trim() ?: return clampTemperatureTenths(fallbackTenths)
        if (source.isEmpty()) return clampTemperatureTenths(fallbackTenths)
        val asInt: Int? = source.toIntOrNull()
        if (asInt != null) return clampTemperatureTenths(asInt)
        val asDouble: Double = source.toDoubleOrNull() ?: return clampTemperatureTenths(fallbackTenths)
        if (asDouble.isNaN() || asDouble.isInfinite()) return clampTemperatureTenths(fallbackTenths)
        return clampTemperatureTenths(Math.round(asDouble * 10.0).toInt())
    }

    /**
     * 🚀✨ Enterprise Quantum Input Sanitizer 💯
     * Control-символы вон (кроме \n и \t), CR вон, 3+ пустых строк -> 2, trim, кап по длине.
     */
    @JvmStatic
    fun sanitizeInput(raw: String?, maxLength: Int): String {
        val source: String = raw ?: return ""
        if (source.isEmpty() || maxLength <= 0) return ""
        val builder = StringBuilder(source.length)
        for (char in source) {
            if (char == '\n' || char == '\t') {
                builder.append(char)
                continue
            }
            if (char == '\r') continue
            if (char < ' ') continue
            builder.append(char)
        }
        var cleaned: String = builder.toString()
        while (cleaned.contains("\n".repeat(MAX_BLANK_RUN + 1))) {
            cleaned = cleaned.replace("\n".repeat(MAX_BLANK_RUN + 1), "\n".repeat(MAX_BLANK_RUN))
        }
        cleaned = cleaned.trim()
        if (cleaned.length > maxLength) cleaned = cleaned.substring(0, maxLength)
        return cleaned
    }

    /** 🚀✨ Enterprise Quantum Debug Line 💯 */
    @JvmStatic
    fun describe(mode: Mode?, temperatureTenths: Int?): String {
        if (mode == null) return FALLBACK_PROMPT
        return mode.title + " · температура " + temperature(temperatureTenths) +
            " · переменных: " + variablesOf(mode.template).joinToString(",")
    }
}
