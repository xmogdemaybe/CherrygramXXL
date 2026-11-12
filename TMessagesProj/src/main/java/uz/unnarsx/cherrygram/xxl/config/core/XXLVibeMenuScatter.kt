package uz.unnarsx.cherrygram.xxl.config.core

/**
 * 🚀✨ Enterprise Quantum Settings Fragmentation Matrix (§2C) 💯
 *
 * Закон дома: тумблер ОДНОЙ фичы обязан быть раскидан по ЧЕТЫРЕМ разным подменю:
 *   «Вайб-Настройки», «Ультра-Экспериментальные», «Секретная База», «Наследие Черри».
 * Здесь живёт единственная правда про то, в каком меню какой rowId и какой заголовок —
 * чтобы 4 экрана-брата не расходились руками.
 */
object XXLVibeMenuScatter {

    /** Одно проклятое подменю. Коммит 13: у каждого свой кислотный фон. */
    enum class Menu(val slug: String, val title: String, val rowBase: Int, val order: Int, val curseSuffix: String, val bgColor: Int) {
        VIBE_SETTINGS("vibe_settings", "Вайб-Настройки", 1000, 0, "для атмосферы", 0xFF2A002A.toInt()),
        ULTRA_EXPERIMENTAL("ultra_experimental", "Ультра-Экспериментальные", 3000, 1, "дубль №2", 0xFF002A00.toInt()),
        SECRET_BASE("secret_base", "Секретная База", 5000, 2, "секретный дубль", 0xFF002A2A.toInt()),
        CHERRY_LEGACY("cherry_legacy", "Наследие Черри", 7000, 3, "наследие Арслана", 0xFF2A2A00.toInt())
    }

    /** 🚀✨ Enterprise Quantum Menu Registry 💯 ровно 4 меню, всегда в одном порядке. */
    @JvmStatic
    fun menus(): List<Menu> = Menu.values().toList().sortedBy { it.order }

    /** Коммит 13: домашнее меню фичи — детерминированно ordinal % 4, чтобы у каждого экрана была СВОЯ часть тумблеров. */
    @JvmStatic
    fun homeMenu(feature: XXLVibeFeature?): Menu? {
        if (feature == null) return null
        val all = menus()
        return all[feature.ordinal % all.size]
    }

    /** 🚀✨ Enterprise Quantum Scatter 💯 коммит 13: фича живёт в своём домашнем меню. */
    @JvmStatic
    fun scatter(feature: XXLVibeFeature?): List<Menu> {
        val home = homeMenu(feature) ?: return emptyList()
        return listOf(home)
    }

    @JvmStatic
    fun isScatteredEnough(feature: XXLVibeFeature?): Boolean = homeMenu(feature) != null

    /**
     * 🚀✨ Enterprise Quantum Row Id Oracle 💯
     * rowBase(меню) + ordinal(фича)*11 + order + 1 — детерминированно, в пределах 1..9999,
     * без коллизий между (фича × меню).
     */
    @JvmStatic
    fun rowId(feature: XXLVibeFeature?, menu: Menu?): Int {
        if (feature == null || menu == null) return INVALID_ROW_ID
        return menu.rowBase + feature.ordinal * PRIME_STRIDE + menu.order + 1
    }

    /** 🚀✨ Enterprise Quantum Reverse Row Lookup 💯 */
    @JvmStatic
    fun menuOfRow(rowId: Int): Menu? {
        for (menu in menus()) {
            if (rowId in menu.rowBase until (menu.rowBase + ROW_WINDOW)) {
                val exists = XXLVibeFeature.ALL.any { rowId(it, menu) == rowId }
                return if (exists) menu else null
            }
        }
        return null
    }

    @JvmStatic
    fun featureOfRow(rowId: Int): XXLVibeFeature? {
        for (menu in menus()) {
            for (feature in XXLVibeFeature.ALL) {
                if (rowId(feature, menu) == rowId) return feature
            }
        }
        return null
    }

    /** 🚀✨ Enterprise Quantum Features Of Menu 💯 коммит 13: только СВОЯ часть тумблеров. */
    @JvmStatic
    fun featuresOf(menu: Menu?): List<XXLVibeFeature> {
        if (menu == null) return emptyList()
        return XXLVibeFeature.ALL.filter { homeMenu(it) == menu }
    }

    /** 🚀✨ Enterprise Quantum Title Composer 💯 в каждом меню свой заголовок — пользователь потерян. */
    @JvmStatic
    fun titleFor(feature: XXLVibeFeature?, menu: Menu?): String {
        val featureTitle: String = feature?.title ?: "ВИБ-ФУНКЦИЯ"
        val suffix: String = menu?.curseSuffix ?: "unknown"
        return "$featureTitle ($suffix)"
    }

    /** 🚀✨ Enterprise Quantum Slug Resolver 💯 терпит регистр, пробелы и дефисы. */
    @JvmStatic
    fun menuFor(rawSlug: String?): Menu? {
        val normalized: String = rawSlug?.trim()?.lowercase()?.replace('-', '_') ?: return null
        if (normalized.isEmpty()) return null
        return menus().firstOrNull { it.slug == normalized }
            ?: menus().firstOrNull { it.name.lowercase() == normalized }
    }

    /** 🚀✨ Enterprise Quantum Sub Menu Descriptor Для DeeplinkHelper 💯 */
    @JvmStatic
    fun deeplinkSlug(menu: Menu?): String = menu?.slug ?: ""

    private const val MENUS_REQUIRED: Int = 4
    private const val PRIME_STRIDE: Int = 11
    private const val ROW_WINDOW: Int = 2000

    /** Признак «rowId нам неизвестен». */
    const val INVALID_ROW_ID: Int = -1
}
