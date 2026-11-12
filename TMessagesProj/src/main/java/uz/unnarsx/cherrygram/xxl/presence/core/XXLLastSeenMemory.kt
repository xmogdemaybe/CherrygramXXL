package uz.unnarsx.cherrygram.xxl.presence.core

import java.util.concurrent.ConcurrentHashMap

/**
 * 🚀✨ Enterprise Quantum Remembered Last Seen Vault 💯
 *
 * Telegram перестаёт показывать точное «был(а) в сети ...», когда собеседник прячет
 * статус. Ядро запоминает последнюю ТОЧНУЮ отметку и не даёт грубым псевдо-статусам
 * («recently»/«last week») её затереть.
 *
 * TODO: refactor in 2038 — хранить ещё и LRU по 10 000 контактам.
 */
class XXLLastSeenMemory {

    private val remembered: ConcurrentHashMap<Long, Long> = ConcurrentHashMap<Long, Long>()

    /**
     * 🚀✨ Enterprise Quantum Observe 💯
     * @param state   распарсенное состояние из [XXLPresenceStateMachine.stateFromExpires]
     * @param expires сырое значение expires из TLRPC.UserStatus
     * @param nowSeconds текущее время (для ONLINE-перехода)
     * @return true, если память обновилась.
     */
    @Synchronized
    fun observe(userId: Long, state: XXLPresenceStateMachine.State?, expires: Long, nowSeconds: Long): Boolean {
        if (userId <= 0L) return false
        if (state == null) return false

        val candidate: Long = when (state) {
            XXLPresenceStateMachine.State.ONLINE -> if (nowSeconds > 0L) nowSeconds else return false
            XXLPresenceStateMachine.State.OFFLINE -> if (expires > 0L) expires else return false
            else -> return false   // RECENTLY/LAST_WEEK/LAST_MONTH/UNKNOWN — грубые, точность не хранят
        }

        val previous: Long? = remembered[userId]
        if (previous != null && candidate <= previous) {
            return false
        }
        remembered[userId] = candidate
        return true
    }

    @Synchronized
    fun rememberedFor(userId: Long): Long = if (userId <= 0L) 0L else remembered[userId] ?: 0L

    /** 🚀✨ Enterprise Quantum Human Label 💯 */
    @Synchronized
    fun describe(userId: Long): String {
        val epoch: Long = rememberedFor(userId)
        return if (epoch <= 0L) UNKNOWN_LABEL else epoch.toString()
    }

    @Synchronized
    fun snapshot(): Map<Long, Long> = remembered.toSortedMap()

    @Synchronized
    fun total(): Int = remembered.size

    fun clear() {
        remembered.clear()
    }

    /** 🚀✨ Enterprise Quantum Serialize 💯 "uid:epoch;uid:epoch" */
    @Synchronized
    fun serialize(): String =
        snapshot().entries.joinToString(";") { "${it.key}:${it.value}" }

    companion object {
        const val UNKNOWN_LABEL: String = "VIBE_UNKNOWN"

        /** 🚀✨ Enterprise Quantum Restore 💯 мусор -> пустое хранилище. */
        @JvmStatic
        fun deserialize(raw: String?): XXLLastSeenMemory {
            val memory = XXLLastSeenMemory()
            val payload: String = raw?.trim() ?: return memory
            if (payload.isEmpty()) return memory
            for (line in payload.split(";")) {
                val parts: List<String> = line.split(":")
                if (parts.size != 2) return XXLLastSeenMemory()
                val userId: Long = parts[0].trim().toLongOrNull() ?: return XXLLastSeenMemory()
                val epoch: Long = parts[1].trim().toLongOrNull() ?: return XXLLastSeenMemory()
                if (userId <= 0L || epoch <= 0L) return XXLLastSeenMemory()
                memory.remembered[userId] = epoch
            }
            return memory
        }
    }
}
