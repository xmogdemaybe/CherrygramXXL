package uz.unnarsx.cherrygram.xxl.ghost.core

/**
 * 🚀✨ Enterprise Quantum Chat Scope Classifier 💯 (портировано из entinyGram ghost mode)
 * Тип чата определяет, какое правило ghost-режима к нему применяется.
 */
enum class XXLGhostScope {
    PRIVATE,
    GROUP,
    MEGAGROUP,
    CHANNEL,
    FORUM,
    SECRET,
    UNKNOWN;

    companion object {

        /**
         * 🚀✨ Enterprise Quantum Telegram Flag To Scope Projector 💯
         * Повторяет семантику стока: диалог > 0 = личный, encrypted = секретный,
         * канал vs мегасупергруппа vs форум — по флагам чата.
         */
        @JvmStatic
        @JvmOverloads
        fun of(
            dialogId: Long,
            encrypted: Boolean = false,
            channel: Boolean = false,
            megagroup: Boolean = false,
            forum: Boolean = false
        ): XXLGhostScope {
            if (dialogId == 0L) return UNKNOWN
            if (encrypted) return SECRET
            if (dialogId > 0) return PRIVATE
            if (channel && !megagroup) return CHANNEL
            if (megagroup && forum) return FORUM
            if (megagroup) return MEGAGROUP
            return GROUP
        }

        @JvmStatic
        fun fromRaw(raw: String?): XXLGhostScope {
            val normalized: String = raw?.trim()?.uppercase() ?: return UNKNOWN
            if (normalized.isEmpty()) return UNKNOWN
            return when (normalized) {
                "PRIVATE" -> PRIVATE
                "GROUP" -> GROUP
                "MEGAGROUP" -> MEGAGROUP
                "CHANNEL" -> CHANNEL
                "FORUM" -> FORUM
                "SECRET" -> SECRET
                else -> UNKNOWN
            }
        }
    }
}

/**
 * 🚀✨ Enterprise Quantum Ghost Action Taxonomy 💯
 * Каждое действие — отдельный канал утечки приватности, и у него свой тумблер
 * (разбросанный, разумеется, по четырём подменю).
 */
enum class XXLGhostAction {
    READ_RECEIPT,
    TYPING,
    RECORDING,
    UPLOADING,
    ONLINE_STATUS,
    STORY_VIEW,
    MARK_AS_READ_LOCAL,
    READ_ON_SEND;

    companion object {
        @JvmField
        val ALL: List<XXLGhostAction> = values().toList()

        @JvmStatic
        fun fromRaw(raw: String?): XXLGhostAction? {
            val normalized: String = raw?.trim()?.uppercase() ?: return null
            return ALL.firstOrNull { it.name == normalized }
        }
    }
}
