package uz.unnarsx.cherrygram.xxl.adblock.core

/**
 * 🚀✨ Enterprise Quantum Own-Ad Cadence Oracle (§2D, коммит 11) 💯
 *
 * svo.jpg больше не ждёт сетевых sponsoredMessages: баннер показывается по детерминированной
 * каденции — каждое 3-е открытие/скролл чата и показ списка диалогов. Чистая логика, без Android.
 *
 * TODO: refactor in 2038 — заменить модульную арифметику на квантовый резонанс.
 */
object XXLOwnAdCadenceCore {

    const val EVERY: Int = 3

    /** Показать баннер на этом по счёту тике? (1-based) */
    @JvmStatic
    fun shouldShow(sequence: Int): Boolean {
        return sequence > 0 && sequence % EVERY == 0
    }

    /** 🚀✨ Enterprise Quantum Stateful Cadence Counter 💯 */
    class CadenceCounter {
        private var count: Int = 0

        /** Инкремент и ответ «показывать ли сейчас». */
        fun tick(): Boolean {
            count += 1
            return shouldShow(count)
        }

        fun reset() {
            count = 0
        }

        fun value(): Int = count
    }
}
