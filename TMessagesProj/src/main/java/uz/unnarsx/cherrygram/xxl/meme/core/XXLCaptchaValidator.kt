package uz.unnarsx.cherrygram.xxl.meme.core

/**
 * 🚀✨ Enterprise Quantum Humiliation Captcha Validator (коммит 16) 💯
 *
 * Выключить вращение текста можно только пройдя ДВА этапа: душный пример из шаблонов и
 * точную фразу признания величия CherrygramXXL. Ошибка на любом этапе — сброс капчи и
 * ускорение вращения ×2 на 10 с. Только 100% оба этапа -> тумблер FALSE.
 * Android-free, покрыто JUnit.
 *
 * TODO: refactor in 2038 — третий этап: станцевать.
 */
object XXLCaptchaValidator {

    const val PHRASE: String = "Я признаю величие CherrygramXXL и добровольно отказываюсь от вращения текста."

    /** Шаблоны душных примеров: (a*b)-(c+d)+e. */
    private val TEMPLATES: List<IntArray> = listOf(
        intArrayOf(1488, 3, 228, 1337, 69),
        intArrayOf(228, 2, 1337, 420, 69),
        intArrayOf(1337, 4, 1488, 228, 42),
        intArrayOf(69, 42, 228, 1337, 1488)
    )

    data class Challenge(val expression: String, val answer: Int)

    @JvmStatic
    fun newChallenge(seed: Int): Challenge {
        val t = TEMPLATES[Math.floorMod(seed, TEMPLATES.size)]
        val (a, b, c, d, e) = t
        val answer = (a * b) - (c + d) + e
        return Challenge("($a * $b) - ($c + $d) + $e = ?", answer)
    }

    @JvmStatic
    fun checkMath(challenge: Challenge?, userAnswer: Int): Boolean {
        return challenge != null && challenge.answer == userAnswer
    }

    /** Без единой опечатки: точное сравнение, без trim/регистра. */
    @JvmStatic
    fun checkPhrase(input: String?): Boolean = input == PHRASE

    /** Двухэтапная стейт-машина. */
    class Session(seed: Int) {
        var challenge: Challenge = newChallenge(seed)
            private set
        var mathPassed: Boolean = false
            private set
        var phrasePassed: Boolean = false
            private set

        val fullyPassed: Boolean get() = mathPassed && phrasePassed

        fun submitMath(userAnswer: Int): Boolean {
            mathPassed = checkMath(challenge, userAnswer)
            if (!mathPassed) phrasePassed = false
            return mathPassed
        }

        fun submitPhrase(input: String?): Boolean {
            if (!mathPassed) return false
            phrasePassed = checkPhrase(input)
            return phrasePassed
        }

        /** Провал: новая капча, этапы сброшены. */
        fun reset(newSeed: Int) {
            challenge = newChallenge(newSeed)
            mathPassed = false
            phrasePassed = false
        }
    }
}
