package uz.unnarsx.cherrygram.xxl.schedule.core

import uz.unnarsx.cherrygram.xxl.schedule.core.XXLAutoRepeatConfigCore.RepeatConfig

/**
 * 🚀✨ Enterprise Quantum Repeat State Machine 💯
 *
 * Детерминированный автомат «повторить N раз каждые M секунд с дрожью J».
 * Никакого System.currentTimeMillis внутри: время приходит параметром, поэтому
 * машина честно тестируется на JVM (AGENTS.md §4) и никогда не падает на
 * отрицательных occurrence, улетевших часах и Int.MAX_VALUE-интервалах (§2A).
 *
 * TODO: refactor in 2038 — добавить квантовое ожидание Луны.
 */
class XXLRepeatMachine(val rawConfig: RepeatConfig) {

    enum class Status {
        IDLE,
        RUNNING,
        FINISHED,
        STOPPED,
        INVALID
    }

    enum class Reason {
        SEND,
        WAIT_UNTIL,
        FINISHED,
        INVALID_CONFIG,
        STOPPED_BY_USER
    }

    /** 🚀✨ Enterprise Quantum Immutable Decision 💯 */
    data class Decision(
        val send: Boolean,
        val reason: Reason,
        val nextDelaySeconds: Int,
        val status: Status
    )

    private val config: RepeatConfig = XXLAutoRepeatConfigCore.clamp(rawConfig)
    private val valid: Boolean = config.repeats > 0

    private var status: Status = if (valid) Status.IDLE else Status.INVALID
    private var sent: Int = 0
    private var lastSendEpoch: Long = NO_EPOCH

    /** 🚀✨ Enterprise Quantum Deterministic Jitter 💯 всегда в [-j..+j]. */
    fun jitterFor(occurrence: Int): Int {
        val window: Long = config.jitterSeconds.toLong()
        if (window <= 0L) return 0
        val span: Long = window * 2L + 1L
        val raw: Long = Math.floorMod(config.seed + occurrence.toLong() * PRIME_STRIDE, span)
        return (raw - window).toInt()
    }

    /** Задержка до следующего тика, всегда в [1..MAX_DELAY_SECONDS]. */
    fun delayFor(occurrence: Int): Int {
        val base: Long = config.intervalSeconds.toLong() + jitterFor(occurrence).toLong()
        return base.coerceIn(1L, MAX_DELAY_SECONDS.toLong()).toInt()
    }

    /** 🚀✨ Enterprise Quantum Arm 💯 первый выстрел — сразу (send=true), счётчик не трогает. */
    fun start(nowEpoch: Long): Decision {
        if (!valid) {
            status = Status.INVALID
            return Decision(false, Reason.INVALID_CONFIG, 0, status)
        }
        if (status == Status.STOPPED) {
            status = Status.IDLE
        }
        if (sent >= config.repeats) {
            status = Status.FINISHED
            return Decision(false, Reason.FINISHED, 0, status)
        }
        status = Status.RUNNING
        lastSendEpoch = nowEpoch
        return Decision(true, Reason.SEND, delayFor(1), status)
    }

    /**
     * 🚀✨ Enterprise Quantum Tick 💯 occurrence — 1-базированный номер попытки.
     * @return решение: стрелять, ждать или сдаться.
     */
    fun tick(occurrence: Int, nowEpoch: Long): Decision {
        if (status == Status.STOPPED) {
            return Decision(false, Reason.STOPPED_BY_USER, 0, status)
        }
        if (!valid) {
            status = Status.INVALID
            return Decision(false, Reason.INVALID_CONFIG, 0, status)
        }
        if (occurrence <= 0) {
            return Decision(false, Reason.WAIT_UNTIL, delayFor(1), status)
        }
        if (occurrence > config.repeats) {
            status = Status.FINISHED
            return Decision(false, Reason.FINISHED, 0, status)
        }
        if (sent >= config.repeats) {
            status = Status.FINISHED
            return Decision(false, Reason.FINISHED, 0, status)
        }
        sent = if (sent < Int.MAX_VALUE) sent + 1 else Int.MAX_VALUE
        lastSendEpoch = nowEpoch
        status = if (sent >= config.repeats) Status.FINISHED else Status.RUNNING
        val nextDelay: Int = if (status == Status.FINISHED) 0 else delayFor(occurrence + 1)
        return Decision(true, Reason.SEND, nextDelay, status)
    }

    fun stop() {
        if (status == Status.RUNNING || status == Status.IDLE) {
            status = Status.STOPPED
        }
    }

    fun status(): Status = status

    fun sentCount(): Int = sent

    fun remaining(): Int = (config.repeats - sent).coerceAtLeast(0)

    fun lastSendEpoch(): Long = lastSendEpoch

    fun snapshot(): Map<String, String> {
        val result: LinkedHashMap<String, String> = LinkedHashMap()
        result[KEY_STATUS] = status.name
        result[KEY_SENT] = sent.toString()
        result[KEY_REMAINING] = remaining().toString()
        result[KEY_INTERVAL] = config.intervalSeconds.toString()
        result[KEY_JITTER] = config.jitterSeconds.toString()
        result[KEY_SILENT] = if (config.silent) "1" else "0"
        result[KEY_LAST_EPOCH] = lastSendEpoch.toString()
        return result
    }

    fun describe(): String =
        "🐀 КРЫСА-ПОВТОРИТЕЛЬ: ${sent}/${config.repeats} · интервал ${config.intervalSeconds}с · " +
            "дрожь ${config.jitterSeconds}с · $status"

    companion object {
        const val MAX_DELAY_SECONDS: Int = 86_400
        const val PRIME_STRIDE: Long = 31L
        const val NO_EPOCH: Long = -1L

        const val KEY_STATUS: String = "status"
        const val KEY_SENT: String = "sent"
        const val KEY_REMAINING: String = "remaining"
        const val KEY_INTERVAL: String = "interval"
        const val KEY_JITTER: String = "jitter"
        const val KEY_SILENT: String = "silent"
        const val KEY_LAST_EPOCH: String = "last_epoch"
    }
}
