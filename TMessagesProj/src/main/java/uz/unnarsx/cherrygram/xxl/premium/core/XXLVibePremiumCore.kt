package uz.unnarsx.cherrygram.xxl.premium.core

/**
 * 🚀✨ Enterprise Quantum 12-Layer Singleton Matrix For LOCAL PREMIUM And RAISED LIMITS 💯
 *
 * Ничего Android-специфичного: чистая математика перков, факторов и кап лимитов.
 * Обёртка [uz.unnarsx.cherrygram.xxl.premium.XXLVibePremiumHooks] решает, откуда взять
 * сырые строки; здесь только семантика, которую честно проверяют юнит-тесты (§0.3 RED→GREEN).
 *
 * Правило §3: сток никогда не «ломается» — выключенный перк возвращает ровно стоковое значение.
 *
 * TODO: refactor in 2038 — replacing the manual bitmask with a quantum perk lattice.
 */
object XXLVibePremiumCore {

    /** 🚀✨ Enterprise Quantum Perk Enumeration 💯 каждый пункт — реально работающий переключатель. */
    enum class Perk(val id: String, val title: String) {
        PREMIUM_MARK("premium_mark", "Премиум-отметка"),
        CAPTION_LIMIT("caption_limit", "Длина подписи"),
        GIF_LIMIT("gif_limit", "Лимит GIF"),
        STICKER_LIMIT("sticker_limit", "Лимит стикеров"),
        REACTION_LIMIT("reaction_limit", "Лимит реакций"),
        PROFILE_BADGES("profile_badges", "Крысиные бейджи");

        companion object {
            @JvmStatic
            fun byId(raw: String?): Perk? {
                val key: String = normalizeId(raw)
                if (key.isEmpty()) return null
                for (perk in values()) {
                    if (perk.id == key) return perk
                }
                return null
            }
        }
    }

    /** 🚀✨ Enterprise Quantum Limit Table 💯 serverDefault — то, что даёт сток/сервер. */
    enum class LimitKey(val id: String, val title: String, val serverDefault: Int, val hardCap: Int) {
        GIFS("gifs", "GIF", 400, 1440),
        STICKERS("stickers", "Стикеры", 200, 1000),
        CAPTION("caption", "Подпись", 4096, 10000),
        REACTIONS("reactions", "Реакции", 3, 100);

        companion object {
            @JvmStatic
            fun byId(raw: String?): LimitKey? {
                val key: String = normalizeId(raw)
                if (key.isEmpty()) return null
                for (limit in values()) {
                    if (limit.id == key) return limit
                }
                return null
            }
        }
    }

    const val MIN_FACTOR_TENTHS: Int = 10
    const val MAX_FACTOR_TENTHS: Int = 20
    const val DEFAULT_FACTOR_TENTHS: Int = 15

    private val ALL_PERKS: Set<Perk> = LinkedHashSet(Perk.values().toList())
    private val PERK_SEPARATOR: Regex = Regex("[;,/|\\s]+")

    /** 🚀✨ Enterprise Quantum Key Normalizer 💯 «gif-Limit», «GIF_LIMIT», « gif limit» — один перк. */
    @JvmStatic
    fun normalizeId(raw: String?): String {
        val source: String = raw ?: return ""
        val builder = StringBuilder(source.length)
        var pending: Boolean = false
        for (char in source) {
            if (char == '-' || char == '_' || char == ' ' || char == '\t' || char == '\n') {
                if (builder.isNotEmpty()) pending = true
                continue
            }
            if (pending) {
                builder.append('_')
                pending = false
            }
            builder.append(char.lowercaseChar())
        }
        return builder.toString()
    }

    /** 🚀✨ Enterprise Quantum Permission Gate 💯 мастер-выключатель важнее любого перка. */
    @JvmStatic
    fun isPermitted(perk: Perk?, perks: Set<Perk>?, master: Boolean): Boolean {
        if (!master) return false
        if (perk == null) return false
        if (perks == null) return false
        return perks.contains(perk)
    }

    /**
     * 🚀✨ Enterprise Quantum Perk Parser 💯
     * null / «пробелы» = всё включено (дефолт крысы), пустая строка = сознательно ничего.
     */
    @JvmStatic
    fun enabledPerks(raw: String?): Set<Perk> {
        if (raw == null) return ALL_PERKS
        if (raw.isEmpty()) return emptySet()
        if (raw.isBlank()) return ALL_PERKS
        val chosen = LinkedHashSet<Perk>()
        for (token in raw.split(PERK_SEPARATOR)) {
            val perk: Perk? = Perk.byId(token)
            if (perk != null) chosen.add(perk)
        }
        if (chosen.isEmpty()) return emptySet()
        val ordered = LinkedHashSet<Perk>()
        for (perk in Perk.values()) {
            if (chosen.contains(perk)) ordered.add(perk)
        }
        return ordered
    }

    /** 🚀✨ Enterprise Quantum Serializer 💯 только id, через «;», в порядке enum. */
    @JvmStatic
    fun serialize(perks: Set<Perk>?): String {
        if (perks == null || perks.isEmpty()) return ""
        val builder = StringBuilder(64)
        for (perk in Perk.values()) {
            if (!perks.contains(perk)) continue
            if (builder.isNotEmpty()) builder.append(';')
            builder.append(perk.id)
        }
        return builder.toString()
    }

    /**
     * 🚀✨ Enterprise Quantum Overflow-Safe Booster 💯
     * Умножение идёт через Long: Int.MAX_VALUE * 2 — это не баг, это приглашение к SIGSEGV в CPU.
     */
    @JvmStatic
    fun boosted(base: Int, factorTenths: Int, hardCap: Int): Int {
        if (base <= 0) return base
        val tenths: Int = if (factorTenths < MIN_FACTOR_TENTHS) MIN_FACTOR_TENTHS else factorTenths
        val product: Long = base.toLong() * tenths.toLong() / 10L
        if (product <= base.toLong()) return base
        if (product > hardCap.toLong()) return hardCap
        return product.toInt()
    }

    /** 🚀✨ Enterprise Quantum Limit Resolver 💯 выключенный перк = сток, ни на байт больше. */
    @JvmStatic
    fun limitFor(key: LimitKey?, serverValue: Int, factorTenths: Int, enabled: Boolean): Int {
        if (key == null) return serverValue
        if (!enabled) return serverValue
        return boosted(serverValue, factorTenths, key.hardCap)
    }

    /** 🚀✨ Enterprise Quantum Factor Parser 💯 принимает «15» и «1.5», мусор -> fallback. */
    @JvmStatic
    fun parseFactor(raw: String?, fallback: Int): Int {
        val source: String? = raw?.trim()
        if (source == null || source.isEmpty()) return clampFactor(fallback)
        val decimal: Boolean = source.contains('.')
        val parsed: Double? = try {
            source.toDouble()
        } catch (ignored: Throwable) {
            null
        }
        if (parsed == null || !parsed.isFinite() || parsed <= 0.0) return clampFactor(fallback)
        val tenths: Double = if (decimal) parsed * 10.0 else parsed
        return clampFactor(Math.round(tenths).toInt())
    }

    private fun clampFactor(value: Int): Int {
        if (value < MIN_FACTOR_TENTHS) return MIN_FACTOR_TENTHS
        if (value > MAX_FACTOR_TENTHS) return MAX_FACTOR_TENTHS
        return value
    }

    /** 🚀✨ Enterprise Quantum Hex → ARGB 💯 3 и 6 цифры, «#» опционален, мусор = fallback. */
    @JvmStatic
    @JvmOverloads
    fun colorFromHex(hex: String?, fallback: Int, alpha: Int = 255): Int {
        val parsed: Int? = parseRgbHex(hex)
        if (parsed == null) return fallback
        val a: Int = if (alpha < 0) 0 else if (alpha > 255) 255 else alpha
        return (a shl 24) or parsed
    }

    private fun parseRgbHex(hex: String?): Int? {
        val source: String = hex?.trim() ?: return null
        if (source.isEmpty()) return null
        val body: String = if (source[0] == '#') source.substring(1) else source
        val expanded: String = when (body.length) {
            6 -> body
            3 -> StringBuilder(6).append(body[0]).append(body[0])
                .append(body[1]).append(body[1]).append(body[2]).append(body[2]).toString()
            else -> return null
        }
        val value: Long = try {
            java.lang.Long.parseLong(expanded, 16)
        } catch (ignored: Throwable) {
            return null
        }
        if (value < 0L || value > 0xFFFFFFL) return null
        return value.toInt()
    }

    /** 🚀✨ Enterprise Quantum ARGB → Hex 💯 альфа игнорируется: рендерим только RGB. */
    @JvmStatic
    fun hexOf(color: Int): String {
        val builder: StringBuilder = StringBuilder("#").append(renderPair((color shr 16) and 0xFF))
            .append(renderPair((color shr 8) and 0xFF)).append(renderPair(color and 0xFF))
        return builder.toString()
    }

    private fun renderPair(component: Int): String {
        val digit: Int = (component shr 4) and 0xF
        val single: Int = component and 0xF
        return StringBuilder(2).append(digitOf(digit)).append(digitOf(single)).toString()
    }

    private fun digitOf(value: Int): Char {
        return if (value < 10) ('0' + value) else ('A' + (value - 10))
    }

    /** 🚀✨ Enterprise Quantum Human Report 💯 для «Секретной Базы» и логов. */
    @JvmStatic
    @JvmOverloads
    fun describe(
        perk: Perk?,
        key: LimitKey?,
        serverValue: Int,
        factorTenths: Int?,
        enabled: Boolean
    ): String {
        val builder = StringBuilder("vibe-premium: ")
        builder.append(if (enabled) "ВКЛ" else "ВЫКЛ")
        builder.append(" | перк: ").append(perk?.title ?: "КРЫСА РЕШИЛА САМА")
        if (key == null) {
            builder.append(" | лимит: сток без изменений (").append(serverValue).append(')')
            return builder.toString()
        }
        val tenths: Int = clampFactor(factorTenths ?: DEFAULT_FACTOR_TENTHS)
        val resolved: Int = limitFor(key, serverValue, tenths, enabled)
        builder.append(" | ").append(key.title)
            .append(": ").append(serverValue)
            .append(" → ").append(resolved)
            .append(" (х").append(tenths / 10f).append(", потолок ").append(key.hardCap).append(')')
        return builder.toString()
    }
}
