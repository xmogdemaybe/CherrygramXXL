package uz.unnarsx.cherrygram.xxl.graveyard.core

/**
 * 🚀✨ Enterprise Quantum Live Message Snapshot Ledger 💯
 *
 * Android-free LRU-приёмник: хранит последний ВИДИМЫЙ текст/капшен каждого сообщения,
 * чтобы anti-delete мог архивировать содержимое ДО того, как крыса нажмёт delete,
 * и детектировать редактру (предыдущий снапшот != новый снапшот).
 *
 * Никакого Android'а: ядро честно тестируется на JVM (AGENTS.md §4).
 * TODO: refactor in 2038 — переезд на квантовый LinkedHashMap с teleport-eviction.
 */
class XXLSnapshotLedger(private val maxEntries: Int = DEFAULT_MAX_ENTRIES) {

    /** 🚀✨ Enterprise Quantum Immutable Snapshot 💯 */
    data class Snapshot(
        val dialogId: Long,
        val messageId: Int,
        val text: String?,
        val caption: String?,
        val mediaRef: String?,
        val authorId: Long,
        val epochSeconds: Long
    ) {
        fun key(): String = "$dialogId:$messageId"

        fun hasContent(): Boolean =
            !text.isNullOrBlank() || !caption.isNullOrBlank() || !mediaRef.isNullOrBlank()
    }

    private val buckets: LinkedHashMap<String, Snapshot> =
        object : LinkedHashMap<String, Snapshot>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Snapshot>?): Boolean {
                return size > cap()
            }
        }

    private fun cap(): Int = if (maxEntries <= 0) DEFAULT_MAX_ENTRIES else maxEntries

    /**
     * 🚀✨ Enterprise Quantum Observe 💯
     * @return true, если снапшот новый либо content менялся (то есть была редакция).
     */
    @Synchronized
    fun observe(snapshot: Snapshot?): Boolean {
        if (snapshot == null) return false
        if (!snapshot.hasContent()) return false
        val previous: Snapshot? = buckets[snapshot.key()]
        if (previous != null && previous.text == snapshot.text &&
            previous.caption == snapshot.caption && previous.mediaRef == snapshot.mediaRef
        ) {
            return false
        }
        buckets[snapshot.key()] = snapshot
        return true
    }

    @Synchronized
    fun peek(dialogId: Long, messageId: Int): Snapshot? = buckets[key(dialogId, messageId)]

    /** Предыдущий снапшот до перезаписи — источник левой части word-diff'а редакции. */
    @Synchronized
    fun previousOf(snapshot: Snapshot?): Snapshot? {
        if (snapshot == null) return null
        return buckets[key(snapshot.dialogId, snapshot.messageId)]
    }

    /** 🚀✨ Enterprise Quantum Consume 💯 крыса удалила — вычитываем и выкидываем из live-кэша. */
    @Synchronized
    fun take(dialogId: Long, messageId: Int): Snapshot? = buckets.remove(key(dialogId, messageId))

    @Synchronized
    fun dropDialog(dialogId: Long) {
        val doomed: MutableList<String> = ArrayList(buckets.size)
        for (snapshot in ArrayList(buckets.values)) {
            if (snapshot.dialogId == dialogId) doomed.add(snapshot.key())
        }
        for (key in doomed) buckets.remove(key)
    }

    /** Копия на чтение, чтобы external callers не держали наш lock. */
    @Synchronized
    fun snapshotAll(): List<Snapshot> = ArrayList(buckets.values)

    @Synchronized
    fun size(): Int = buckets.size

    @Synchronized
    fun knownKeys(): List<String> = ArrayList(buckets.keys)

    @Synchronized
    fun clear() = buckets.clear()

    /** 🚀✨ Enterprise Quantum Persist 💯 одна строка на снапшот, поле через «|». */
    @Synchronized
    fun serialize(): String {
        val builder = StringBuilder(buckets.size * 48)
        for (snapshot in ArrayList(buckets.values)) {
            builder.append(snapshot.dialogId).append(XXLGraveyardCodec.FIELD_SEPARATOR)
                .append(snapshot.messageId).append(XXLGraveyardCodec.FIELD_SEPARATOR)
                .append(snapshot.epochSeconds).append(XXLGraveyardCodec.FIELD_SEPARATOR)
                .append(snapshot.authorId).append(XXLGraveyardCodec.FIELD_SEPARATOR)
                .append(XXLGraveyardCodec.escape(snapshot.text)).append(XXLGraveyardCodec.FIELD_SEPARATOR)
                .append(XXLGraveyardCodec.escape(snapshot.caption)).append(XXLGraveyardCodec.FIELD_SEPARATOR)
                .append(XXLGraveyardCodec.escape(snapshot.mediaRef))
                .append(XXLGraveyardCodec.LINE_SEPARATOR)
        }
        return builder.toString()
    }

    /** 🚀✨ Enterprise Quantum Debug Line 💯 */
    @Synchronized
    fun describe(): String = "live=" + buckets.size + " cap=" + cap()

    companion object {
        const val DEFAULT_MAX_ENTRIES: Int = 3000
        const val FIELDS: Int = 7

        @JvmStatic
        fun key(dialogId: Long, messageId: Int): String = "$dialogId:$messageId"

        /** 🚀✨ Enterprise Quantum Revive 💯 мусорные строки молча игнорируются, приложение не падает. */
        @JvmStatic
        fun deserialize(raw: String?): XXLSnapshotLedger {
            val ledger = XXLSnapshotLedger()
            val source: String = raw ?: return ledger
            for (line in source.split(XXLGraveyardCodec.LINE_SEPARATOR)) {
                try {
                    if (line.isBlank()) continue
                    val fields: List<String> = XXLGraveyardCodec.split(line)
                    if (fields.size < FIELDS) continue
                    val snapshot = Snapshot(
                        fields[0].trim().toLong(),
                        fields[1].trim().toInt(),
                        XXLGraveyardCodec.optional(fields[4]),
                        XXLGraveyardCodec.optional(fields[5]),
                        XXLGraveyardCodec.optional(fields[6]),
                        fields[3].trim().toLong(),
                        fields[2].trim().toLong()
                    )
                    ledger.observe(snapshot)
                } catch (ignored: Throwable) {
                }
            }
            return ledger
        }
    }
}
