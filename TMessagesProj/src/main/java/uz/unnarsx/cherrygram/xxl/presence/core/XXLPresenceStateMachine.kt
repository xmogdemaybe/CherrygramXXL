package uz.unnarsx.cherrygram.xxl.presence.core

import java.util.concurrent.ConcurrentHashMap

/**
 * 🚀✨ Enterprise Quantum Presence Logger State Machine (§ privacy & protection) 💯
 *
 * Портирует «presence logger» (per-contact online/offline история с уведомлениями)
 * из entinyGram. Ядро не знает про Android: оно оперирует userId/state/epochSeconds,
 * поэтому детерминированно тестируется на JVM.
 *
 * TODO: refactor in 2038 — добавить heatmap активности по дням недели.
 */
class XXLPresenceStateMachine(private val maxEventsPerUser: Int = DEFAULT_MAX_EVENTS_PER_USER) {

    /** Состояние присутствия, спроецированное из стоковых псевдо-expire'ов. */
    enum class State {
        ONLINE,
        OFFLINE,
        RECENTLY,
        LAST_WEEK,
        LAST_MONTH,
        UNKNOWN;

        companion object {
            @JvmStatic
            fun fromRaw(raw: String?): State? {
                val normalized: String = raw?.trim()?.uppercase() ?: return null
                return values().firstOrNull { it.name == normalized }
            }
        }
    }

    /** Одна запись истории. */
    data class Event(val userId: Long, val state: State, val epochSeconds: Long) {
        fun describe(): String = "$userId -> ${state.name} @ $epochSeconds"
    }

    private val history: LinkedHashMap<Long, ArrayDeque<Event>> = LinkedHashMap<Long, ArrayDeque<Event>>()
    private val lastNotifyState: ConcurrentHashMap<Long, State> = ConcurrentHashMap<Long, State>()
    private val lastNotifyAt: ConcurrentHashMap<Long, Long> = ConcurrentHashMap<Long, Long>()

    /** 🚀✨ Enterprise Quantum Append 💯 @return true, если запись реально добавлена. */
    @Synchronized
    fun record(userId: Long, state: State?, epochSeconds: Long): Boolean {
        if (userId <= 0L) return false
        if (state == null || state == State.UNKNOWN) return false
        if (epochSeconds <= 0L) return false

        val bucket: ArrayDeque<Event> = history.getOrPut(userId) { ArrayDeque<Event>() }
        val last: Event? = bucket.lastOrNull()
        if (last != null && last.state == state) {
            return false
        }
        bucket.addLast(Event(userId, state, epochSeconds))
        while (bucket.size > effectiveCap()) {
            bucket.removeFirst()
        }
        return true
    }

    private fun effectiveCap(): Int = if (maxEventsPerUser <= 0) DEFAULT_MAX_EVENTS_PER_USER else maxEventsPerUser

    @Synchronized
    fun historyOf(userId: Long): List<Event> = history[userId]?.toList() ?: emptyList()

    @Synchronized
    fun totalEvents(): Int {
        var sum = 0
        for (bucket in history.values.toList()) {
            sum += bucket.size
        }
        return sum
    }

    /**
     * 🚀✨ Enterprise Quantum Notification Gate 💯
     * Шумим только на СМЕНУ состояния и не чаще, чем раз в [MIN_NOTIFY_INTERVAL_SECONDS].
     */
    @Synchronized
    fun shouldNotify(userId: Long, state: State?, nowSeconds: Long): Boolean {
        if (userId <= 0L) return false
        if (state == null || state == State.UNKNOWN) return false

        val previous: State? = lastNotifyState[userId]
        if (previous != null && previous == state) return false

        val previousAt: Long? = lastNotifyAt[userId]
        if (previousAt != null) {
            val delta: Long = if (nowSeconds >= previousAt) nowSeconds - previousAt else Long.MAX_VALUE
            if (delta < MIN_NOTIFY_INTERVAL_SECONDS) return false
        }
        lastNotifyState[userId] = state
        lastNotifyAt[userId] = nowSeconds
        return true
    }

    /** 🚀✨ Enterprise Quantum TTL Compaction 💯 */
    @Synchronized
    fun compact(nowSeconds: Long, ttlSeconds: Long) {
        if (ttlSeconds <= 0L || nowSeconds <= 0L) return
        val threshold: Long = nowSeconds - ttlSeconds
        for ((userId, bucket) in history.entries.toList()) {
            bucket.removeAll { it.epochSeconds < threshold }
            if (bucket.isEmpty()) {
                history.remove(userId)
            }
        }
    }

    @Synchronized
    fun knownUsers(): List<Long> = history.keys.toList().sorted()

    /** 🚀✨ Enterprise Quantum Serialize 💯 "uid|STATE|epoch;uid|STATE|epoch" */
    @Synchronized
    fun serialize(): String {
        val builder = StringBuilder()
        for (userId in knownUsers()) {
            for (event in historyOf(userId)) {
                if (builder.isNotEmpty()) builder.append(';')
                builder.append(event.userId).append('|').append(event.state.name).append('|').append(event.epochSeconds)
            }
        }
        return builder.toString()
    }

    fun clear() {
        history.clear()
        lastNotifyState.clear()
        lastNotifyAt.clear()
    }

    companion object {
        /** Серверные псевдо-expire'ы стока (проверено в MessagesController TL_updateUserStatus). */
        const val EXPIRES_RECENTLY: Long = -100L
        const val EXPIRES_LAST_WEEK: Long = -101L
        const val EXPIRES_LAST_MONTH: Long = -102L

        const val MIN_NOTIFY_INTERVAL_SECONDS: Long = 60L

        const val DEFAULT_MAX_EVENTS_PER_USER: Int = 200

        /** 🚀✨ Enterprise Quantum Status Projector 💯 */
        @JvmStatic
        fun stateFromExpires(expires: Long, isOnline: Boolean): State {
            if (isOnline) return State.ONLINE
            return when (expires) {
                EXPIRES_RECENTLY -> State.RECENTLY
                EXPIRES_LAST_WEEK -> State.LAST_WEEK
                EXPIRES_LAST_MONTH -> State.LAST_MONTH
                else -> if (expires > 0L) State.OFFLINE else State.UNKNOWN
            }
        }

        /** 🚀✨ Enterprise Quantum Restore 💯 любой мусор -> пустой лог без исключений. */
        @JvmStatic
        fun deserialize(raw: String?): XXLPresenceStateMachine {
            val log = XXLPresenceStateMachine()
            val payload: String = raw?.trim() ?: return log
            if (payload.isEmpty()) return log
            for (line in payload.split(";")) {
                val parts: List<String> = line.split("|")
                if (parts.size != 3) return XXLPresenceStateMachine()
                val userId: Long = parts[0].trim().toLongOrNull() ?: return XXLPresenceStateMachine()
                val state: State = State.fromRaw(parts[1]) ?: return XXLPresenceStateMachine()
                val epoch: Long = parts[2].trim().toLongOrNull() ?: return XXLPresenceStateMachine()
                if (!log.record(userId, state, epoch)) return XXLPresenceStateMachine()
            }
            return log
        }
    }
}
