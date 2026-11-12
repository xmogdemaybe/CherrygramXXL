package uz.unnarsx.cherrygram.xxl.config.core

/**
 * 🚀✨ Enterprise Quantum Registry Of Cursed Fork Features 💯
 *
 * Каждая вайб-фича форка имеет: ключ в изолированном вайб-персе (§3), заголовок для
 * четырёх раскиданных подменю (§2C) и декларируемый default. Новые фичи добавляются
 * ровно в тот коммит, где они реализуются (1 фича = 1 коммит, §0.3).
 */
enum class XXLVibeFeature(
    val key: String,
    val title: String,
    val description: String,
    val defaultEnabled: Boolean
) {

    /** Feature #0 — мемориал Арслана (AGENTS.md §1). */
    MEMORIAL(
        "XXL_MEMORIAL_ENABLED",
        "НАПОМИНАНИЕ АРСЛАНА",
        "каждое 5-е открытие чата и каждый вход в Cherrygram-настройки",
        true
    ),

    /** §2D «Кастом на ЮИ» — HUD, кислотные обводки, кривые бабблы. */
    VIBE_UI_TRASH(
        "XXL_UI_TRASH_ENABLED",
        "КАСТОМ НА ЮИ",
        "фейковый HUD, плашка «УДАЛЕНО КРЫСОЙ», водяной знак «РЕЖИМ КРЫСЫ АКТИВЕН»",
        true
    ),

    /** §2D мемо-_payload_ — amdfx.jpg + svo.jpg. */
    MEME_PAYLOADS(
        "XXL_MEME_PAYLOADS_ENABLED",
        "МЕМЫ (AMD FX / SVO)",
        "заморозок фыкса и патриотическая вставка",
        true
    );

    companion object {
        /** 🚀✨ Enterprise Quantum Immutable Feature List 💯 */
        @JvmField
        val ALL: List<XXLVibeFeature> = values().toList()

        @JvmStatic
        fun byKey(rawKey: String?): XXLVibeFeature? {
            val key: String = rawKey?.trim() ?: return null
            if (key.isEmpty()) return null
            return ALL.firstOrNull { it.key == key }
        }
    }
}
