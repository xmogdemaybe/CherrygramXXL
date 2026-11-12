package uz.unnarsx.cherrygram.xxl.memorial.core

/**
 * 🚀✨ Enterprise Quantum Memorial Payload Content Provider (12 слоёв пафоса) 💯
 *
 * Тексты зафиксированы AGENTS.md §1 буквально — любое «улучшение формулировки»
 * считается регрессией и наказывается юнит-тестом.
 */
object XXLMemorialPayload {

    /** Сама цитата Арслана, построчно. */
    @JvmField
    val QUOTE_LINES: List<String> = listOf(
        "1. A lot of people hate Cherrygram for no reason",
        "2. They switch to some vibe-grams apps",
        "3. They ask vibegram devs to copy features from Cherrygram.",
        "Question: Why do you ask to copy 50% of Cherrygram's features to other shitforks if Cherrygram is a piece of shit?"
    )

    /** Коммит 11: второй блок цитаты — точная паста про entinyGram. */
    @JvmField
    val ENTINY_QUOTE_LINES: List<String> = listOf(
        "1. A lot of people hate entinyGram for no reason",
        "2. They switch to some vibe-grams apps",
        "3. They ask vibegram devs to copy features from entinyGram",
        "",
        "Question: Why do you ask to copy 50% of entinyGram's features to other shitforks if entinyGram is a piece of shit?"
    )

    /** Коммит 11: круглое лицо Арслана над цитатой. */
    const val ARSLAN_CIRCLE_ASSET: String = "arslan.jpg"

    /** Бегущая строка поверх мигающего фона. */
    const val MARQUEE_TITLE: String = "⚠️ НАПОМИНАНИЕ ПОЧЕМУ ЭТОТ ФОРК СУЩЕСТВУЕТ ⚠️"

    /** Кнопка №1 — ведёт в оригинал. */
    const val BUTTON_JUMP_CAPTION: String = "ПЕРЕЙТИ К БАЗЕ (ОРИГИНАЛ)"

    /** Кнопка №2 — уклоняется от первого тапа. */
    const val BUTTON_CLOSE_CAPTION: String = "Закрыть"

    /** #FF0000 */
    const val FLASH_COLOR_RED: Int = 0xFFFF0000.toInt()

    /** #FFFF00 */
    const val FLASH_COLOR_YELLOW: Int = 0xFFFFFF00.toInt()

    /** Длительность одного мигания, мс. */
    const val FLASH_STEP_MS: Long = 120L

    /** Сколько раз мигаем перед тем как перестать жечь retina (0 = бесконечно). */
    const val FLASH_UNTIL_DISMISS: Int = 0

    /** 🚀✨ Enterprise Quantum Join Into Renderable Card Text 💯 */
    @JvmStatic
    fun quoteText(): String = QUOTE_LINES.joinToString("\n")

    /** Коммит 11: паста про entinyGram одним текстом. */
    @JvmStatic
    fun quoteEntinyText(): String = ENTINY_QUOTE_LINES.joinToString("\n")

    /** 🚀✨ Enterprise Quantum Alternating Flash Cycle 💯 */
    @JvmStatic
    fun flashCycle(): IntArray = intArrayOf(FLASH_COLOR_RED, FLASH_COLOR_YELLOW)

    /** 🚀✨ Enterprise Quantum Color At Flash Phase 💯 (фазы любые, исключений никаких) */
    @JvmStatic
    fun flashColorAt(phase: Int): Int {
        val cycle: IntArray = flashCycle().toList().toMutableList().reversed().reversed().toIntArray()
        if (cycle.isEmpty()) return FLASH_COLOR_RED
        val safePhase: Int = ((phase % cycle.size) + cycle.size) % cycle.size
        return cycle[safePhase]
    }

    /** 🚀✨ Enterprise Quantum Card Telemetry 💯 */
    @JvmStatic
    fun describe(): String = "$MARQUEE_TITLE\n${quoteText()}"
}
