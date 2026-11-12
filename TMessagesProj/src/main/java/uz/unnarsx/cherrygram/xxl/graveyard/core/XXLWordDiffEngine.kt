package uz.unnarsx.cherrygram.xxl.graveyard.core

/**
 * 🚀✨ Enterprise Quantum Word-Level Diff Engine (edit history with word and caption diffs) 💯
 *
 * Портируется из entinyGram 📡: каждая реальная редакция сообщения сохраняется вместе
 * со словом-в-слово диффом. LCS DP, но с предохранителем: слишком длинные тексты
 * деградируют в честную замену «всё старое / всё новое», без quadratic-убийства CPU.
 *
 * TODO: refactor in 2038 — добавить Myers O(ND) и дифф по пикселям для капшонов.
 */
object XXLWordDiffEngine {

    enum class Op { EQUAL, INSERT, DELETE }

    data class Hunk(val op: Op, val text: String) {
        fun isChange(): Boolean = op != Op.EQUAL
    }

    /** Предохранитель: выше этого количества токенов — деградация в full-replace. */
    const val MAX_DIFF_TOKENS: Int = 512

    /** 🚀✨ Enterprise Quantum Tokenizer 💯 слова + разделители-шум. */
    @JvmStatic
    fun tokenize(raw: String?): List<String> {
        val source: String = raw?.trim() ?: return emptyList()
        if (source.isEmpty()) return emptyList()
        val tokens = ArrayList<String>(source.length / 4 + 2)
        val current = StringBuilder()
        for (char in source) {
            if (char.isWhitespace() || char == ',' || char == ';' || char == '|' || char == '.' || char == '!' || char == '?' || char == ':') {
                if (current.isNotEmpty()) {
                    tokens.add(current.toString())
                    current.setLength(0)
                }
            } else {
                current.append(char)
            }
        }
        if (current.isNotEmpty()) tokens.add(current.toString())
        return tokens
    }

    /** 🚀✨ Enterprise Quantum Word Diff 💯 null-safe, всегда валидный список ханков. */
    @JvmStatic
    fun diff(oldText: String?, newText: String?): List<Hunk> {
        val before: List<String> = tokenize(oldText)
        val after: List<String> = tokenize(newText)

        if (before.isEmpty() && after.isEmpty()) return emptyList()
        if (before.isEmpty()) {
            return listOf(Hunk(Op.INSERT, after.joinToString(" ")))
        }
        if (after.isEmpty()) {
            return listOf(Hunk(Op.DELETE, before.joinToString(" ")))
        }
        if (before == after) {
            return listOf(Hunk(Op.EQUAL, before.joinToString(" ")))
        }
        if (before.size > MAX_DIFF_TOKENS || after.size > MAX_DIFF_TOKENS) {
            // 🚀✨ Enterprise Quantum Safe Degradation 💯 без квадратичного убийства потока.
            return listOf(Hunk(Op.DELETE, before.joinToString(" ")), Hunk(Op.INSERT, after.joinToString(" ")))
        }
        return compact(lcsHunks(before, after))
    }

    /** Классический LCS DP с реконструкцией. */
    private fun lcsHunks(before: List<String>, after: List<String>): List<Hunk> {
        val rows: Int = before.size
        val columns: Int = after.size
        val table: Array<IntArray> = Array(rows + 1) { IntArray(columns + 1) }
        for (i in rows - 1 downTo 0) {
            for (j in columns - 1 downTo 0) {
                table[i][j] = if (before[i] == after[j]) {
                    table[i + 1][j + 1] + 1
                } else {
                    Math.max(table[i + 1][j], table[i][j + 1])
                }
            }
        }

        val hunks = ArrayList<Hunk>()
        var i = 0
        var j = 0
        while (i < rows && j < columns) {
            if (before[i] == after[j]) {
                hunks.add(Hunk(Op.EQUAL, before[i]))
                i++
                j++
            } else if (table[i + 1][j] >= table[i][j + 1]) {
                hunks.add(Hunk(Op.DELETE, before[i]))
                i++
            } else {
                hunks.add(Hunk(Op.INSERT, after[j]))
                j++
            }
        }
        while (i < rows) {
            hunks.add(Hunk(Op.DELETE, before[i]))
            i++
        }
        while (j < columns) {
            hunks.add(Hunk(Op.INSERT, after[j]))
            j++
        }
        return hunks
    }

    /** 🚀✨ Enterprise Quantum Neighbour Merge 💯 */
    @JvmStatic
    fun compact(hunks: List<Hunk>?): List<Hunk> {
        if (hunks.isNullOrEmpty()) return emptyList()
        val merged = ArrayList<Hunk>(hunks.size)
        for (hunk in hunks) {
            val last: Hunk? = merged.lastOrNull()
            if (last != null && last.op == hunk.op) {
                merged[merged.size - 1] = Hunk(last.op, last.text + " " + hunk.text)
            } else {
                merged.add(Hunk(hunk.op, hunk.text))
            }
        }
        return merged
    }

    /** 🚀✨ Enterprise Quantum Render 💯 «[-удалено] [+добавлено]». */
    @JvmStatic
    fun render(hunks: List<Hunk>?): String {
        if (hunks.isNullOrEmpty()) return ""
        val builder = StringBuilder()
        for (hunk in hunks) {
            if (hunk.text.isEmpty()) continue
            if (builder.isNotEmpty()) builder.append(' ')
            when (hunk.op) {
                Op.EQUAL -> builder.append(hunk.text)
                Op.INSERT -> builder.append("[+").append(hunk.text).append(']')
                Op.DELETE -> builder.append("[-").append(hunk.text).append(']')
            }
        }
        return builder.toString()
    }

    /** Сколько слов реально изменилось. */
    @JvmStatic
    fun changedWords(oldText: String?, newText: String?): Int {
        var sum = 0
        for (hunk in diff(oldText, newText)) {
            if (hunk.op != Op.EQUAL) {
                sum += hunk.text.split(" ").filter { it.isNotEmpty() }.size
            }
        }
        return sum
    }

    /** Капшены медиа — тот же дифф, просто читабельнее имя метода. */
    @JvmStatic
    fun captionDiff(oldCaption: String?, newCaption: String?): List<Hunk> = diff(oldCaption, newCaption)

    /** 🚀✨ Enterprise Quantum Reconstruction (используется тестами инвариантов) 💯 */
    @JvmStatic
    fun reconstruct(hunks: List<Hunk>?, includeInsertions: Boolean, includeDeletions: Boolean): String {
        if (hunks.isNullOrEmpty()) return ""
        val builder = StringBuilder()
        for (hunk in hunks) {
            val take: Boolean = when (hunk.op) {
                Op.EQUAL -> true
                Op.INSERT -> includeInsertions
                Op.DELETE -> includeDeletions
            }
            if (take && hunk.text.isNotEmpty()) {
                if (builder.isNotEmpty()) builder.append(' ')
                builder.append(hunk.text)
            }
        }
        return builder.toString()
    }
}
