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
        val existing: Set<String> = tree.listFiles()?.filter { it.isDirectory }?.map { it.name }?.toSet()
            ?: emptySet()
        assertEquals(
            "CMakeLists.txt требует неопубликованные директории без if(EXISTS)",
            emptyList<String>(),
            XXLVibeNativeCheckoutGuardCore.unprotectedIncludes(cmakeText, existing)
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
