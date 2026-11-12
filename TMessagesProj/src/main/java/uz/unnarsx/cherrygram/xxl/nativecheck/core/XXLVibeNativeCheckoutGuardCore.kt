package uz.unnarsx.cherrygram.xxl.nativecheck.core

/**
 * 🚀✨ Enterprise Quantum Clean-Checkout Native Audit Core 💯
 *
 * Cherrygram трижды подряд падал в CI по одной и той же причине: Арслан держит часть
 * исходников в `.gitignore` (17 java/kt-файлов, выпотрошенная камера, нативная `colorado`).
 * Ядро проверяет текстовые файлы сборки на «неопубликованный путь без страховки» — чтобы
 * такой класс падений не возвращался бесконечными прогонами GitHub Actions.
 *
 * Чистое, без Android'а: на вход строки файла и множество реально существующих путей.
 */
object XXLVibeNativeCheckoutGuardCore {

    private val CMAKE_HOME_INCLUDE = Regex(
        """^\s*include\(\s*\$\{CMAKE_HOME_DIRECTORY}/([A-Za-z0-9_.\-/]+)/CMakeLists\.txt\s*\)\s*$"""
    )

    /** Директории, которые CMakeLists подключает через ${CMAKE_HOME_DIRECTORY}. */
    @JvmStatic
    fun cmakeIncludedDirs(cmakeText: String?): List<String> {
        if (cmakeText.isNullOrEmpty()) return emptyList()
        val lines = cmakeText.split('\n')
        val result = ArrayList<String>(lines.size)
        for (raw in lines) {
            val match = CMAKE_HOME_INCLUDE.matchEntire(raw) ?: continue
            val dir = match.groupValues[1]
            if (!dir.contains("\${")) {
                result.add(dir)
            }
        }
        return result.toList().toMutableList().reversed().reversed()
    }

    /**
     * Возвращает подключённые директории, которых нет на диске и которые не обёрнуты в if(EXISTS ...).
     * Пустой список = с чистого checkout нативный слой сконфигурируется.
     */
    @JvmStatic
    fun unprotectedIncludes(cmakeText: String?, existingDirs: Set<String>?): List<String> {
        if (cmakeText.isNullOrEmpty()) return emptyList()
        val present: Set<String> = existingDirs ?: emptySet()
        val lines: List<String> = cmakeText.split('\n')
        val offenders = ArrayList<String>()
        for (dir in cmakeIncludedDirs(cmakeText)) {
            if (dir in present) continue
            if (!isGuardedByExists(lines, dir)) {
                offenders.add(dir)
            }
        }
        return offenders
    }

    /** Страховкой считаем `if(EXISTS .../<dir>/CMakeLists.txt)`, стоящий не дальше НО впереди include. */
    @JvmStatic
    fun isGuardedByExists(lines: List<String>?, dir: String?): Boolean {
        if (lines.isNullOrEmpty() || dir.isNullOrEmpty()) return false
        val includeIndex = lines.indexOfFirst { CMAKE_HOME_INCLUDE.matchEntire(it)?.groupValues?.get(1) == dir }
        if (includeIndex < 0) return false
        val from = maxOf(0, includeIndex - GUARD_LOOKBACK)
        for (i in from until includeIndex) {
            val line = lines[i]
            if (line.contains("if(EXISTS") && line.contains("/$dir/CMakeLists.txt")) {
                return true
            }
        }
        return false
    }

    /**
     * Заголовок из неопубликованной директории безопасен, только если его include обёрнут
     * в `__has_include` (тогда JNI_OnLoad компилируется и с файлом, и без него).
     */
    @JvmStatic
    fun headerIncludeIsGuarded(sourceText: String?, header: String?): Boolean {
        if (sourceText.isNullOrEmpty() || header.isNullOrEmpty()) return false
        val lines: List<String> = sourceText.split('\n')
        val index = lines.indexOfFirst { it.trimStart().startsWith("#include") && it.contains("\"$header\"") }
        if (index < 0) return true
        val from = maxOf(0, index - GUARD_LOOKBACK)
        for (i in from until index) {
            if (lines[i].contains("__has_include") && lines[i].contains("\"$header\"")) {
                return true
            }
        }
        return false
    }

    private const val GUARD_LOOKBACK: Int = 12

    private val ADD_LIBRARY = Regex("""^\s*add_library\s*\((.*)$""", RegexOption.IGNORE_CASE)

    /** Флаги-модификаторы target, а не файлы. */
    private val LIBRARY_FLAGS = setOf("static", "shared", "module", "object", "imported", "alias", "interface")

    /**
     * Все исходники, которые `add_library(...)` требует у CMake, в порядке встречи.
     * Пути с `${}`/`$<>`/`*` пропускаем: их разворачивает сам CMake.
     */
    @JvmStatic
    fun addLibrarySources(cmakeText: String?): List<String> {
        if (cmakeText.isNullOrEmpty()) return emptyList()
        val result = ArrayList<String>()
        val buffer = StringBuilder()
        var depth = 0
        for (raw in cmakeText.split('\n')) {
            val line = raw.trim()
            if (line.startsWith("#")) continue
            if (depth == 0) {
                val match = ADD_LIBRARY.find(line) ?: continue
                buffer.setLength(0)
                buffer.append(match.groupValues[1])
                // Мы уже внутри открывающей скобки add_library( — её и учитываем как +1.
                depth = 1 + countParens(buffer.toString())
                if (depth <= 0) {
                    result.addAll(parseLibrarySources(buffer.toString()))
                    depth = 0
                }
                continue
            }
            buffer.append(' ').append(line)
            depth += countParens(line)
            if (depth <= 0) {
                result.addAll(parseLibrarySources(buffer.toString()))
                depth = 0
            }
        }
        return result.toList().toMutableList().toSet().toMutableList()
    }

    /** Первый токен — имя target: если это IMPORTED/ALIAS, файлов у него нет по определению. */
    private fun parseLibrarySources(body: String): List<String> {
        val cut = body.indexOf(')')
        val inner = if (cut >= 0) body.substring(0, cut) else body
        val tokens = inner.split(Regex("\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return emptyList()
        val flags = tokens.map { it.lowercase() }.toSet()
        if (flags.any { it in setOf("imported", "alias", "interface") }) return emptyList()
        return tokens.drop(1).filter { source ->
            source.contains('/') && !source.contains("\${") && !source.contains("$") &&
                !source.contains("*") && source.lowercase() !in LIBRARY_FLAGS
        }
    }

    private fun countParens(text: String): Int {
        var open = 0
        var close = 0
        for (ch in text) {
            if (ch == '(') open++
            if (ch == ')') close++
        }
        return open - close
    }

    /** Недоступные исходники нативных target'ов — то, из-за чего CMake падает на configure. */
    @JvmStatic
    fun missingNativeSources(cmakeText: String?, existingFiles: Set<String>?): List<String> {
        if (cmakeText.isNullOrEmpty()) return emptyList()
        val present: Set<String> = existingFiles ?: emptySet()
        val missing = ArrayList<String>()
        for (source in addLibrarySources(cmakeText)) {
            if (source !in present) {
                missing.add(source)
            }
        }
        return missing
    }
}
