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

    /** 📡 Ghost mode (entinyGram/Inugram):read receipts, typing, presence-утечки. */
    GHOST_MODE(
        "XXL_GHOST_MODE_ENABLED",
        "ПРИЗРАК КРЫСЫ",
        "read receipts, набор текста, загрузка, просмотр сторис, онлайн-статус",
        false
    ),

    /** 📡 Presence logger / remembered last seen / profile change log. */
    PRESENCE_LOGGER(
        "XXL_PRESENCE_LOGGER_ENABLED",
        "СЛЕЖКА ЗА КРЫСАМИ",
        "журнал online/offline, запомненный last seen, история смены имени и био",
        true
    ),

    /** §2D мемо-_payload_ — amdfx.jpg + svo.jpg. */
    MEME_PAYLOADS(
        "XXL_MEME_PAYLOADS_ENABLED",
        "МЕМЫ (AMD FX / SVO)",
        "заморозок фыкса и патриотическая вставка",
        true
    ),

    /** Anti-Delete: кладбище удалённых сообщений + история правок + плашка «УДАЛЕНО КРЫСОЙ». */
    ANTI_DELETE(
        "XXL_ANTI_DELETE_ENABLED",
        "КЛАДБИЩЕ УДАЛЁННЫХ",
        "архив текста до крыс-удаления, истории редактур, кислотная плашка",
        true
    ),

    /** Restricted-content unlock: скрины, копипаст, пересылка, выкуп view-once. */
    UNRESTRICTED(
        "XXL_UNRESTRICTED_ENABLED",
        "АНЛОК ЗАПРЕЩЁННОГО",
        "скриншоты restricted-чатов, копипаст без автора, view-once в Крысиный Архив",
        true
    ),

    /** Adblock: вырезаем спонсорские сообщения Дурова и встаём на их место своей рекламой. */
    AD_BLOCK(
        "XXL_AD_BLOCK_ENABLED",
        "АНТИ-РЕКЛАМА ДУРОВА",
        "спонсорские сообщения долой; каждые N — свой кислотный баннер",
        true
    ),

    /** Repeat-operator: крыса повторяет сообщение N раз каждые M секунд + пресеты расписания. */
    AUTO_REPEAT(
        "XXL_AUTO_REPEAT_ENABLED",
        "КРЫСА-ПОВТОРИТЕЛЬ",
        "авто-повтор сообщения, прайм-интервалы, дрожь, нативное расписание",
        true
    ),

    /** AI provider registry: крыса приносит свои ключи, модели и prompt-шаблоны. */
    AI_REGISTRY(
        "XXL_AI_REGISTRY_ENABLED",
        "КРЫСА-ИИ (СВОИ КЛЮЧИ)",
        "свой провайдер, ключ, модель, base-url и prompt-режим вместо стоковых",
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
