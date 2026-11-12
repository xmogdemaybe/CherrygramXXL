package uz.unnarsx.cherrygram.xxl.nativecheck.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.AssumptionViolatedException
import org.junit.Test
import java.io.File

/**
 * Тест на инвариант «из чистого checkout собирается»: ни один неопубликованный путь не может
 * подключаться в CMakeLists.txt / jni.c без страховки.
 *
 * Первый блок — чистая логика парсера. Второй аудитует РЕАЛЬНЫЕ файлы репозитория; если
 * дерева исходников рядом нет (hermetic-харнесс вне Gradle), тест честно помечается skipped,
 * а не «зелёным».
 */
class XXLVibeNativeCheckoutGuardCoreTest {

    private val cmakeWithUnpublished = """
        #voip
        include(${'$'}{CMAKE_HOME_DIRECTORY}/voip/CMakeLists.txt)
        include(${'$'}{CMAKE_HOME_DIRECTORY}/colorado/CMakeLists.txt)
        set(XXL_COLORADO_TARGET "")
        if(EXISTS ${'$'}{CMAKE_HOME_DIRECTORY}/integrity/CMakeLists.txt)
            include(${'$'}{CMAKE_HOME_DIRECTORY}/integrity/CMakeLists.txt)
            set(XXL_COLORADO_TARGET colorado)
        endif()
    """.trimIndent()

    private val cmakeAllGuarded = """
        set(XXL_COLORADO_TARGET "")
        if(EXISTS ${'$'}{CMAKE_HOME_DIRECTORY}/colorado/CMakeLists.txt)
            include(${'$'}{CMAKE_HOME_DIRECTORY}/colorado/CMakeLists.txt)
            set(XXL_COLORADO_TARGET colorado)
        endif()
    """.trimIndent()

    @Test
    fun `collects every cmake home include`() {
        assertEquals(
            listOf("voip", "colorado", "integrity"),
            XXLVibeNativeCheckoutGuardCore.cmakeIncludedDirs(cmakeWithUnpublished)
        )
    }

    @Test
    fun `flags missing directory that is included without guard`() {
        assertEquals(
            listOf("colorado"),
            XXLVibeNativeCheckoutGuardCore.unprotectedIncludes(cmakeWithUnpublished, setOf("voip"))
        )
    }

    @Test
    fun `guarded missing directory is not an offender`() {
        assertEquals(
            emptyList<String>(),
            XXLVibeNativeCheckoutGuardCore.unprotectedIncludes(cmakeAllGuarded, emptySet())
        )
        assertTrue(
            XXLVibeNativeCheckoutGuardCore.isGuardedByExists(
                listOf(
                    "if(EXISTS \${CMAKE_HOME_DIRECTORY}/integrity/CMakeLists.txt)",
                    "include(\${CMAKE_HOME_DIRECTORY}/integrity/CMakeLists.txt)"
                ),
                "integrity"
            )
        )
    }

    @Test
    fun `existing directory never offender`() {
        assertEquals(
            emptyList<String>(),
            XXLVibeNativeCheckoutGuardCore.unprotectedIncludes(cmakeWithUnpublished, setOf("voip", "colorado"))
        )
    }

    @Test
    fun `null and empty input do not crash the auditor`() {
        assertEquals(emptyList<String>(), XXLVibeNativeCheckoutGuardCore.cmakeIncludedDirs(null))
        assertEquals(emptyList<String>(), XXLVibeNativeCheckoutGuardCore.cmakeIncludedDirs(""))
        assertEquals(emptyList<String>(), XXLVibeNativeCheckoutGuardCore.unprotectedIncludes(null, null))
        assertFalse(XXLVibeNativeCheckoutGuardCore.isGuardedByExists(null, "voip"))
        assertFalse(XXLVibeNativeCheckoutGuardCore.isGuardedByExists(emptyList(), null))
        assertFalse(XXLVibeNativeCheckoutGuardCore.headerIncludeIsGuarded(null, "colorado/colorado.h"))
        assertFalse(XXLVibeNativeCheckoutGuardCore.headerIncludeIsGuarded("int main(){}", null))
    }

    @Test
    fun `header include needs has_include guard`() {
        val guarded = """
            #if defined(__has_include)
            #if __has_include("colorado/colorado.h")
            #include "colorado/colorado.h"
            #endif
            #endif
        """.trimIndent()
        assertTrue(XXLVibeNativeCheckoutGuardCore.headerIncludeIsGuarded(guarded, "colorado/colorado.h"))
        assertFalse(
            XXLVibeNativeCheckoutGuardCore.headerIncludeIsGuarded(
                "#include \"colorado/colorado.h\"",
                "colorado/colorado.h"
            )
        )
        // Нет include вообще — ничего и защищать не надо.
        assertTrue(
            XXLVibeNativeCheckoutGuardCore.headerIncludeIsGuarded(
                "int main(){return 0;}",
                "colorado/colorado.h"
            )
        )
    }

    private val multiLineCmake = """
        add_library(tdutils STATIC IMPORTED)
        add_library(tgnet STATIC
                security/secure_validator.cpp
                openssl/aes_ige.c
                tgnet/ApiScheme.cpp
                ${'$'}<GENEX_EVAL:whatever>
                third_party/libyuv/source/*.cc
                )
        add_library(breakpad STATIC
                third_party/breakpad/src/client/linux/handler/exception_handler.cc)
    """.trimIndent()

    @Test
    fun `collects sources from multiline add_library blocks`() {
        val sources = XXLVibeNativeCheckoutGuardCore.addLibrarySources(multiLineCmake)
        assertTrue(sources.contains("security/secure_validator.cpp"))
        assertTrue(sources.contains("tgnet/ApiScheme.cpp"))
        assertTrue(sources.contains("third_party/breakpad/src/client/linux/handler/exception_handler.cc"))
    }

    @Test
    fun `ignores imported targets, globs and generator expressions`() {
        val sources = XXLVibeNativeCheckoutGuardCore.addLibrarySources(multiLineCmake)
        assertFalse("IMPORTED-цель не должна давать файлов", sources.any { it.contains("tdutils") })
        assertFalse(sources.any { it.contains('*') })
        assertFalse(sources.any { it.contains("\$") })
    }

    @Test
    fun `missingNativeSources catches the exact CMakeLists 105 failure`() {
        val present = setOf(
            "openssl/aes_ige.c",
            "tgnet/ApiScheme.cpp",
            "third_party/breakpad/src/client/linux/handler/exception_handler.cc"
        )
        assertEquals(
            listOf("security/secure_validator.cpp"),
            XXLVibeNativeCheckoutGuardCore.missingNativeSources(multiLineCmake, present)
        )
        val everything = present + setOf("security/secure_validator.cpp")
        assertEquals(
            emptyList<String>(),
            XXLVibeNativeCheckoutGuardCore.missingNativeSources(multiLineCmake, everything)
        )
    }

    @Test
    fun `null safe for add_library parsing`() {
        assertEquals(emptyList<String>(), XXLVibeNativeCheckoutGuardCore.addLibrarySources(null))
        assertEquals(emptyList<String>(), XXLVibeNativeCheckoutGuardCore.addLibrarySources(""))
        assertEquals(emptyList<String>(), XXLVibeNativeCheckoutGuardCore.missingNativeSources(null, null))
    }

    @Test
    fun `real repository files contain no unguarded unpublished native paths`() {
        var candidate: File? = File(".").absoluteFile.parentFile
        var jniDir: File? = null
        while (candidate != null && jniDir == null) {
            val probe = File(candidate, "TMessagesProj/jni")
            if (File(probe, "CMakeLists.txt").isFile) {
                jniDir = probe
            }
            candidate = candidate.parentFile
        }
        if (jniDir == null) {
            val local = File("jni")
            jniDir = if (File(local, "CMakeLists.txt").isFile) local else null
        }
        val tree = jniDir ?: throw AssumptionViolatedException("нет дерева исходников рядом с тестом")

        val cmakeText = File(tree, "CMakeLists.txt").readText()
        val existing = tree.listFiles()?.filter { it.isDirectory }?.map { it.name }?.toSet()
            ?: emptySet()
        assertEquals(
            "CMakeLists.txt требует неопубликованные директории без if(EXISTS)",
            emptyList<String>(),
            XXLVibeNativeCheckoutGuardCore.unprotectedIncludes(cmakeText, existing)
        )

        // Исходники из подмодулей в этом окружении не гарантируются (compile-check инициализирует
        // только media + jlatexmath), поэтому проверяем только то, что обязано лежать в самом git.
        val repoRoot = generateSequence(tree) { it.parentFile }
            ?.firstOrNull { File(it, ".gitmodules").isFile }
        val submodulePrefixes = if (repoRoot == null) emptyList() else
            File(repoRoot, ".gitmodules").readLines()
                .filter { it.trim().startsWith("path") }
                .map { it.substringAfter('=').trim() }
                .filter { it.startsWith("TMessagesProj/jni/") }
                .map { it.removePrefix("TMessagesProj/jni/") }
        val existingFiles: Set<String> = tree.walkTopDown().filter { it.isFile }
            .map { it.relativeTo(tree).path.replace('\\', '/') }.toSet()
        val requiredSources = XXLVibeNativeCheckoutGuardCore.addLibrarySources(cmakeText)
            .filter { source -> submodulePrefixes.none { source == it || source.startsWith("$it/") } }
        assertEquals(
            "add_library требует файлов, которых нет в git (gitignore-ловушка как с security/)",
            emptyList<String>(),
            requiredSources.filter { it !in existingFiles }
        )

        val jniC = File(tree, "jni.c")
        if (jniC.isFile) {
            assertTrue(
                "jni.c инклудит colorado/colorado.h без __has_include",
                XXLVibeNativeCheckoutGuardCore.headerIncludeIsGuarded(jniC.readText(), "colorado/colorado.h")
            )
        }
    }
}
