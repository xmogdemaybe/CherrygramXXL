/**
 * 🚀✨ Enterprise Quantum Unit Test for the word diff engine (anti-delete / edit history) 💯
 * Портируется «edit history with word and caption diffs» из entinyGram FEATURES.md.
 */
package uz.unnarsx.cherrygram.xxl.graveyard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.unnarsx.cherrygram.xxl.graveyard.core.XXLWordDiffEngine

class XXLWordDiffEngineTest {

    @Test
    fun `identical texts produce a single equal hunk`() {
        val hunks = XXLWordDiffEngine.diff("cherry is a piece of shit", "cherry is a piece of shit")
        assertEquals(1, hunks.size.toLong())
        assertEquals(XXLWordDiffEngine.Op.EQUAL, hunks[0].op)
        assertEquals("cherry is a piece of shit", hunks[0].text)
    }

    @Test
    fun `null and blank inputs never explode`() {
        assertTrue(XXLWordDiffEngine.diff(null, null).isEmpty())
        assertEquals(1, XXLWordDiffEngine.diff(null, "привет").size.toLong())
        assertEquals(XXLWordDiffEngine.Op.INSERT, XXLWordDiffEngine.diff(null, "привет")[0].op)
        assertEquals(XXLWordDiffEngine.Op.DELETE, XXLWordDiffEngine.diff("привет", "   ")[0].op)
        assertEquals(emptyList<XXLWordDiffEngine.Hunk>(), XXLWordDiffEngine.diff("", ""))
    }

    @Test
    fun `middle word replacement is detected precisely`() {
        val hunks = XXLWordDiffEngine.diff("a b c d", "a x c d")
        val rendered = XXLWordDiffEngine.render(hunks)
        assertTrue("должно быть удалено b: $rendered", rendered.contains("[-b]"))
        assertTrue("должно быть добавлено x: $rendered", rendered.contains("[+x]"))
        assertTrue("a должно остаться: $rendered", rendered.contains("a "))
        assertTrue("ханков не может быть меньше трёх: ${hunks.size}", hunks.size >= 3)
        assertEquals(
            "EQUAL + DELETE обязаны восстановить исходник", "a b c d",
            XXLWordDiffEngine.reconstruct(hunks, includeInsertions = false, includeDeletions = true)
        )
        assertEquals(
            "EQUAL + INSERT обязаны восстановить редакттуру", "a x c d",
            XXLWordDiffEngine.reconstruct(hunks, includeInsertions = true, includeDeletions = false)
        )
    }

    @Test
    fun `pure insert and pure delete at the end`() {
        val added = XXLWordDiffEngine.diff("один два", "один два три")
        assertEquals(XXLWordDiffEngine.Op.EQUAL, added[0].op)
        assertEquals(XXLWordDiffEngine.Op.INSERT, added[1].op)
        assertEquals("три", added[1].text)

        val removed = XXLWordDiffEngine.diff("один два три", "один два")
        assertEquals(XXLWordDiffEngine.Op.DELETE, removed[1].op)
        assertEquals("три", removed[1].text)
    }

    @Test
    fun `word operation counter matches human expectation`() {
        assertEquals(0, XXLWordDiffEngine.changedWords("a b c", "a b c"))
        assertEquals(2, XXLWordDiffEngine.changedWords("a b c", "a x c"))
        assertEquals(1, XXLWordDiffEngine.changedWords("a b c", "a b c d"))
        assertEquals(1, XXLWordDiffEngine.changedWords("a b c d", "a b c"))
    }

    @Test
    fun `tokenize keeps words and splits punctuation`() {
        assertEquals(listOf("Привет", "мир"), XXLWordDiffEngine.tokenize("  Привет   мир  "))
        assertEquals(listOf("a", "b"), XXLWordDiffEngine.tokenize("a,b"))
        assertTrue(XXLWordDiffEngine.tokenize(null).isEmpty())
        assertTrue(XXLWordDiffEngine.tokenize("   ").isEmpty())
        assertEquals(3, XXLWordDiffEngine.tokenize("один|два|три").size.toLong())
    }

    @Test
    fun `oversized inputs degrade to a safe full replace`() {
        val big = (1..(XXLWordDiffEngine.MAX_DIFF_TOKENS + 500)).joinToString(" ") { "w$it" }
        val big2 = (1..(XXLWordDiffEngine.MAX_DIFF_TOKENS + 500)).joinToString(" ") { "z$it" }
        val hunks = XXLWordDiffEngine.diff(big, big2)
        assertEquals(2, hunks.size.toLong())
        assertEquals(XXLWordDiffEngine.Op.DELETE, hunks[0].op)
        assertEquals(XXLWordDiffEngine.Op.INSERT, hunks[1].op)
        assertEquals(XXLWordDiffEngine.MAX_DIFF_TOKENS.toLong() + 500L, XXLWordDiffEngine.tokenize(big).size.toLong())
    }

    @Test
    fun `compact merges neighbouring hunks of the same kind`() {
        val compacted = XXLWordDiffEngine.compact(listOf(
            XXLWordDiffEngine.Hunk(XXLWordDiffEngine.Op.EQUAL, "a"),
            XXLWordDiffEngine.Hunk(XXLWordDiffEngine.Op.EQUAL, "b"),
            XXLWordDiffEngine.Hunk(XXLWordDiffEngine.Op.INSERT, "c")
        ))
        assertEquals(2, compacted.size.toLong())
        assertEquals("a b", compacted[0].text)
        assertFalse(compacted[0].text.contains("[-"))
        assertEquals("склейка обязана сохранять границы слов", "a b c", XXLWordDiffEngine.reconstruct(compacted, true, true))
        assertTrue("render не должен быть пустым", XXLWordDiffEngine.render(compacted).isNotBlank())
    }

    @Test
    fun `caption diff helper handles media captions with nulls`() {
        assertEquals("", XXLWordDiffEngine.render(XXLWordDiffEngine.captionDiff(null, null)))
        assertTrue(XXLWordDiffEngine.render(XXLWordDiffEngine.captionDiff(null, "подпись")).contains("[+подпись]"))
        assertTrue(XXLWordDiffEngine.render(XXLWordDiffEngine.captionDiff("старая", null)).contains("[-старая]"))
    }
}
