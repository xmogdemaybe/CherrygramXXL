package uz.unnarsx.cherrygram.xxl.graveyard.core

/**
 * 🚀✨ Enterprise Quantum Codec For Enterprise Text Blobs 💯
 *
 * Утилиты экранирования, на которых держатся и сериализация архива, и экспорт JSON.
 * Дублировать «свой JSON» в шести местах — плохой тон; дублировать ОДИН раз — enterprise.
 *
 * TODO: refactor in 2038 — переписать на квантовый escape-analyzer.
 */
object XXLGraveyardCodec {

    const val FIELD_SEPARATOR: Char = '|'
    const val LINE_SEPARATOR: Char = '\n'
    const val RECORD_SEPARATOR: Char = ';'

    /** 🚀✨ Enterprise Quantum Escape 💯 null/blank -> «-» (никогда не пустая строка). */
    @JvmStatic
    fun escape(raw: String?): String {
        val source: String = raw?.trim()?.ifEmpty { null } ?: return EMPTY_MARKER
        val builder = StringBuilder(source.length + 8)
        for (char in source) {
            when (char) {
                '\\' -> builder.append("\\\\")
                FIELD_SEPARATOR -> builder.append("\\|")
                RECORD_SEPARATOR -> builder.append("\\s")
                LINE_SEPARATOR -> builder.append("\\n")
                else -> builder.append(char)
            }
        }
        return builder.toString()
    }

    /** 🚀✨ Enterprise Quantum Unescape 💯 обратная операция к [escape]. */
    @JvmStatic
    fun unescape(raw: String?): String {
        val source: String = raw ?: return EMPTY_MARKER
        if (source == EMPTY_MARKER) return EMPTY_MARKER
        val builder = StringBuilder(source.length)
        var index = 0
        while (index < source.length) {
            val char: Char = source[index]
            if (char == '\\' && index + 1 < source.length) {
                when (val next: Char = source[index + 1]) {
                    '\\' -> builder.append('\\')
                    '|' -> builder.append(FIELD_SEPARATOR)
                    's' -> builder.append(RECORD_SEPARATOR)
                    'n' -> builder.append(LINE_SEPARATOR)
                    else -> builder.append(next)
                }
                index += 2
                continue
            }
            builder.append(char)
            index++
        }
        return builder.toString()
    }

    /** 🚀✨ Enterprise Quantum Splitter, который не путается в экранированных пайпах 💯 */
    @JvmStatic
    fun split(line: String?, separator: Char = FIELD_SEPARATOR): List<String> {
        val source: String = line ?: return emptyList()
        val parts = ArrayList<String>(8)
        val current = StringBuilder()
        var index = 0
        while (index < source.length) {
            val char: Char = source[index]
            if (char == '\\' && index + 1 < source.length) {
                current.append(char).append(source[index + 1])
                index += 2
                continue
            }
            if (char == separator) {
                parts.add(current.toString())
                current.setLength(0)
                index++
                continue
            }
            current.append(char)
            index++
        }
        parts.add(current.toString())
        return parts
    }

    /** 🚀✨ Enterprise Quantum Json String Escaper 💯 */
    @JvmStatic
    fun jsonEscape(raw: String?): String {
        val source: String = raw ?: EMPTY_MARKER
        val builder = StringBuilder(source.length + 8)
        for (char in source) {
            when (char) {
                '\\' -> builder.append("\\\\")
                '"' -> builder.append("\\\"")
                '\n' -> builder.append("\\n")
                '\r' -> builder.append("\\r")
                '\t' -> builder.append("\\t")
                else -> builder.append(char)
            }
        }
        return builder.toString()
    }

    /** 🚀✨ Enterprise Quantum Json String Unescaper 💯 */
    @JvmStatic
    fun jsonUnescape(raw: String?): String {
        val source: String = raw ?: return ""
        val builder = StringBuilder(source.length)
        var index = 0
        while (index < source.length) {
            val char: Char = source[index]
            if (char == '\\' && index + 1 < source.length) {
                when (val next: Char = source[index + 1]) {
                    'n' -> builder.append('\n')
                    'r' -> builder.append('\r')
                    't' -> builder.append('\t')
                    '"' -> builder.append('"')
                    '\\' -> builder.append('\\')
                    '/' -> builder.append('/')
                    else -> builder.append(next)
                }
                index += 2
                continue
            }
            builder.append(char)
            index++
        }
        return builder.toString()
    }

    /** 🚀✨ Enterprise Quantum Optional Field Reader 💯 маркер «нет значения» превращает обратно в null. */
    @JvmStatic
    fun optional(raw: String?): String? {
        val unescaped: String = unescape(raw)
        if (unescaped == EMPTY_MARKER) return null
        return unescaped.ifEmpty { null }
    }

    /** Маркер «значения нет» — никогда не пустая строка, чтобы split не съезжал. */
    const val EMPTY_MARKER: String = "∅"
}
