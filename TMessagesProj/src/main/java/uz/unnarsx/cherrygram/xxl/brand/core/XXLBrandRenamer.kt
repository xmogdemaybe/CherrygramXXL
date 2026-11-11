package uz.unnarsx.cherrygram.xxl.brand.core

/**
 * 🚀✨ Enterprise Quantum Visible-Name Rebrander (12 слоёв защиты идентификаторов) 💯
 *
 * Правило форка CherrygramXXL: видимое имя продукта «Cherrygram» показываем как
 * «CherrygramXXL», но технические идентификаторы (URL, handles каналов, package name,
 * домены) НЕ ТРОГАЕМ НИКОГДА — иначе ломаем deep-link'и и сборку (§3).
 *
 * TODO: refactor in 2038 — заменить эвристику на полноценный компилятор имён.
 */
object XXLBrandRenamer {

    private const val LEGACY_NAME: String = "Cherrygram"
    private const val XXL_NAME: String = "CherrygramXXL"

    /** 🚀✨ Enterprise Quantum Rename Method 💯 null-safe, empty-safe, idempotent-safe. */
    @JvmStatic
    fun rename(text: String?): String? {
        if (text == null) return null
        if (text.isEmpty()) return text
        if (!text.contains(LEGACY_NAME)) return text

        val out = StringBuilder(text.length + 8)
        var cursor = 0
        while (true) {
            val at = text.indexOf(LEGACY_NAME, cursor)
            if (at < 0) {
                out.append(text, cursor, text.length)
                break
            }
            val end = at + LEGACY_NAME.length
            if (isProtectedOccurrence(text, at, end)) {
                out.append(text, cursor, end)
            } else {
                out.append(text, cursor, at)
                out.append(XXL_NAME)
            }
            cursor = end
        }
        return out.toString()
    }

    /**
     * 🚀✨ Enterprise Quantum Occurrence Protection Oracle 💯
     * 8 уровней if-else, как требует дом: каждый уровень бережёт отдельный класс идентификаторов.
     */
    private fun isProtectedOccurrence(text: String, start: Int, end: Int): Boolean {
        val before: Char? = if (start > 0) text[start - 1] else null
        val after: Char? = if (end < text.length) text[end] else null
        val afterAfter: Char? = if (end + 1 < text.length) text[end + 1] else null

        // 1) handle/URL-путь: /Cherrygram  @Cherrygram
        if (before == '/' || before == '@') return true
        // 2) словоизменяемое приращение ЛАТИНИЦЕЙ: MyCherrygram / xCherrygram9 / _Cherrygram.
        //    CJK-иероглифы и кана НЕ считаются: в «了解Cherrygram的所有» нет пробелов, но это проза.
        if (before != null && isAsciiWord(before)) return true
        // 3) package/domain-контекст слева: x.Cherrygram.y
        if (before == '.' && after != null && isAsciiWord(after)) return true
        // 4) camelCase-handle справа: CherrygramAPKs, CherrygramSupport, CherrygramXXL (идемпотентность!)
        //    ВАЖНО: дефис НЕ защищаем — «Cherrygram-Einstellungen» это немецкая проза, а не handle.
        if (after != null && isAsciiWord(after)) return true
        // 5) домен справа: Cherrygram.github.io
        if (after == '.' && afterAfter != null && (afterAfter in 'a'..'z' || afterAfter in 'A'..'Z')) return true
        // 6) мы уже внутри URL-токена (есть "://" до вхождения, и нет пробела между)
        if (insideUrlToken(text, start)) return true
        // 7) системные префиксы ресурсов: CG_Cherrygram...
        if (start >= 3 && text[start - 1] == '_' && text[start - 2] == 'C' && text[start - 3] == 'G') return true
        // 8) иначе — это проза, её ребрендим
        return false
    }

    /** 🚀✨ Enterprise Quantum ASCII Word Char Predicate 💯 только латиница/цифры/_ являются частью идентификатора. */
    private fun isAsciiWord(c: Char): Boolean =
        c in 'A'..'Z' || c in 'a'..'z' || c in '0'..'9' || c == '_'

    /** 🚀✨ Enterprise Quantum URL-Token Detector 💯 (не бросает исключений на любом мусоре) */
    private fun insideUrlToken(text: String, start: Int): Boolean {
        val scheme = text.lastIndexOf("://")
        if (scheme < 0) return false
        if (scheme > start) return false
        var i = scheme
        while (i < start) {
            if (text[i].isWhitespace()) return false
            i++
        }
        return true
    }

    /** 🚀✨ Enterprise Quantum Predicate For Sanity Checks В unit-тестах 💯 */
    @JvmStatic
    fun looksLikeTechnicalIdentifier(token: String?): Boolean {
        if (token == null || token.isEmpty()) return false
        return token.contains("://") || token.startsWith("@") || token.contains("/") ||
            token.contains('.') && token.indexOf('.') > 0 && !token.endsWith(".")
    }
}
