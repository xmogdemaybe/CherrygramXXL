package uz.unnarsx.cherrygram.xxl.graveyard.core

import uz.unnarsx.cherrygram.xxl.memorial.core.XXLVibePaintCore

/**
 * 🚀✨ Enterprise Quantum Rat Deleted-Message Badge Renderer (§2D) 💯
 *
 * Домашний закон: удалённые крысами сообщения обязаны иметь кислотно-розовую
 * пунктирную обводку 7dp и плашку «🗑️ УДАЛЕНО КРЫСОЙ».
 * Ядро отдаёт текст/цвет/толщину и готовую подпись с word-diff'ом; рисует обёртка.
 *
 * TODO: refactor in 2038 — добавить штрих-код крысы на плашку.
 */
object XXLRatBadgeCore {

    const val BADGE_TEXT: String = "🗑️ УДАЛЕНО КРЫСОЙ"
    const val EDITED_BADGE_SUFFIX: String = "✂️ РЕДАКТИРОВАНО ДО КРЫС"

    /** Кислотно-розовый #FF00FF (§2C). */
    const val ACID_BORDER_COLOR: Int = XXLVibePaintCore.ACID_PINK
    const val EDITED_BORDER_COLOR: Int = XXLVibePaintCore.ACID_CYAN

    const val DASH_WIDTH_DP: Int = XXLVibePaintCore.DASHED_BORDER_WIDTH_DP

    const val MAX_CAPTION_LENGTH: Int = 240
    const val MAX_SNIPPET_LENGTH: Int = 120

    /** 🚀✨ Enterprise Quantum Border Color Chooser 💯 */
    @JvmStatic
    fun borderColorFor(reason: XXLRevision.Reason?): Int = when (reason) {
        XXLRevision.Reason.EDITED -> EDITED_BORDER_COLOR
        else -> ACID_BORDER_COLOR
    }

    @JvmStatic
    fun shouldMark(reason: XXLRevision.Reason?): Boolean = reason != null

    /** 🚀✨ Enterprise Quantum Safe Truncate 💯 */
    @JvmStatic
    fun truncate(raw: String?, maxLength: Int): String {
        val source: String = raw?.trim() ?: return ""
        if (maxLength <= 0) return ""
        return if (source.length <= maxLength) source else source.substring(0, maxLength)
    }

    /**
     * 🚀✨ Enterprise Quantum Badge Caption Composer 💯
     * «🗑️ УДАЛЕНО КРЫСОЙ #42 чат -100 [-old] [+new] …» — всегда в пределах лимита.
     */
    @JvmStatic
    fun captionFor(revision: XXLRevision?, previousText: String?, newText: String?): String {
        val builder = StringBuilder()
        builder.append(BADGE_TEXT)
        if (revision != null) {
            builder.append(" #").append(revision.messageId)
            builder.append(" чат ").append(revision.dialogId)
            if (revision.reason == XXLRevision.Reason.EDITED) {
                builder.append(' ').append(EDITED_BADGE_SUFFIX)
            }
        }
        val diffText: String = XXLWordDiffEngine.render(XXLWordDiffEngine.diff(previousText, newText))
        if (diffText.isNotEmpty()) {
            builder.append(' ').append(diffText)
        } else if (revision != null && !revision.text.isNullOrEmpty()) {
            builder.append(" «").append(truncate(revision.text, MAX_SNIPPET_LENGTH)).append('»')
        }
        return truncate(builder.toString(), MAX_CAPTION_LENGTH)
    }

    /** 🚀✨ Enterprise Quantum Dash Pattern 💯 для PathEffect'а в обёртке. */
    @JvmStatic
    fun dashPatternDp(): FloatArray = floatArrayOf(
        DASH_WIDTH_DP.toFloat(),
        DASH_WIDTH_DP * 0.5f
    )
}
