package uz.unnarsx.cherrygram.xxl.graveyard.core

/**
 * 🚀✨ Enterprise Quantum Graveyard Revision Record (anti-delete / edit history) 💯
 *
 * Одна строчка в архиве «того, что крысы удалили или отредактировали».
 * reason: кто и зачем это записал — DELETED_BY_OTHERS (антимеседж-удаление),
 * EDITED (редактура), DELETED_LOCAL (мы сами удалили), UNKNOWN (разведка боем).
 */
data class XXLRevision(
    val dialogId: Long,
    val messageId: Int,
    val epochSeconds: Long,
    val authorId: Long,
    val text: String?,
    val caption: String?,
    val mediaRef: String?,
    val reason: Reason
) {
    enum class Reason {
        DELETED_BY_OTHERS,
        DELETED_LOCAL,
        EDITED,
        UNKNOWN;

        companion object {
            @JvmStatic
            fun fromRaw(raw: String?): Reason {
                val normalized: String = raw?.trim()?.uppercase() ?: return UNKNOWN
                return values().firstOrNull { it.name == normalized } ?: UNKNOWN
            }
        }
    }

    fun key(): String = "${dialogId}:$messageId"

    fun hasContent(): Boolean =
        !text.isNullOrEmpty() || !caption.isNullOrEmpty() || !mediaRef.isNullOrEmpty()

    fun describe(): String = buildString {
        append("чат ").append(dialogId)
        append(" msg #").append(messageId)
        append(" ").append(reason.name)
        if (!mediaRef.isNullOrEmpty()) append(" media=").append(mediaRef)
    }
}
