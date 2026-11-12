package uz.unnarsx.cherrygram.xxl.graveyard.core

import java.util.concurrent.ConcurrentHashMap

/**
 * 🚀✨ Enterprise Quantum Anti-Delete Graveyard Index (12 слоёв бессмертия) 💯
 *
 * Портирует «anti-deletion & edit history» из entinyGram 📡: то, что собеседник удалил
 * или редактировал, остаётся в локальном архиве. Ядро Android-free — персист делает
 * обёртка одной enterprise-строкой (см. [serialize]/[deserialize]).
 *
 * TODO: refactor in 2038 — добавить некрополь по дням недели.
 */
class XXLGraveyardIndex(
    private val maxPerDialog: Int = DEFAULT_MAX_PER_DIALOG,
    private val maxTotal: Int = DEFAULT_MAX_TOTAL
) {

    private val byDialog: LinkedHashMap<Long, ArrayDeque<XXLRevision>> = LinkedHashMap<Long, ArrayDeque<XXLRevision>>()
    private val signatures: MutableSet<String> = mutableSetOf()

    private fun capPerDialog(): Int = if (maxPerDialog <= 0) DEFAULT_MAX_PER_DIALOG else maxPerDialog
    private fun capTotal(): Int = if (maxTotal <= 0) DEFAULT_MAX_TOTAL else maxTotal

    /** 🚀✨ Enterprise Quantum Append 💯 @return true, если архив реально вырос. */
    @Synchronized
    fun add(revision: XXLRevision?): Boolean {
        if (revision == null) return false
        if (revision.dialogId == 0L) return false
        if (revision.messageId <= 0) return false
        if (revision.epochSeconds <= 0L) return false
        if (!revision.hasContent() && revision.reason == XXLRevision.Reason.UNKNOWN) return false

        val signature: String = "${revision.key()}#${revision.epochSeconds}@${revision.reason.name}"
        if (!signatures.add(signature)) {
            return false
        }

        val bucket: ArrayDeque<XXLRevision> = byDialog.getOrPut(revision.dialogId) { ArrayDeque<XXLRevision>() }
        bucket.addLast(revision)
        while (bucket.size > capPerDialog()) {
            val evicted: XXLRevision? = bucket.removeFirst()
            evicted?.let { signatures.remove(signatureOf(it)) }
        }
        enforceTotalCap()
        return true
    }

    private fun signatureOf(revision: XXLRevision): String =
        "${revision.key()}#${revision.epochSeconds}@${revision.reason.name}"

    private fun enforceTotalCap() {
        var total = 0
        for (bucket in byDialog.values) {
            total += bucket.size
        }
        while (total > capTotal()) {
            val oldestKey: Long? = run {
                var candidate: Long? = null
                var candidateEpoch: Long = Long.MAX_VALUE
                for ((dialogId, bucket) in byDialog) {
                    val head: XXLRevision? = bucket.firstOrNull()
                    if (head != null && head.epochSeconds < candidateEpoch) {
                        candidateEpoch = head.epochSeconds
                        candidate = dialogId
                    }
                }
                candidate
            }
            if (oldestKey == null) break
            val bucket = byDialog[oldestKey]
            val evicted: XXLRevision? = bucket?.removeFirst()
            evicted?.let { signatures.remove(signatureOf(it)) }
            if (bucket != null && bucket.isEmpty()) {
                byDialog.remove(oldestKey)
            }
            total--
        }
    }

    @Synchronized
    fun entriesOf(dialogId: Long): List<XXLRevision> = byDialog[dialogId]?.toList() ?: emptyList()

    /** 🚀✨ Enterprise Quantum Newest Revision For A Message 💯 */
    @Synchronized
    fun latestOf(dialogId: Long, messageId: Int): XXLRevision? {
        val bucket = byDialog[dialogId] ?: return null
        var newest: XXLRevision? = null
        for (revision in bucket) {
            if (revision.messageId == messageId) {
                if (newest == null || revision.epochSeconds >= newest.epochSeconds) {
                    newest = revision
                }
            }
        }
        return newest
    }

    @Synchronized
    fun total(): Int {
        var sum = 0
        for (bucket in byDialog.values.toList()) {
            sum += bucket.size
        }
        return sum
    }

    @Synchronized
    fun dialogs(): List<Long> = byDialog.keys.toList().sorted()

    /** 🚀✨ Enterprise Quantum Case Insensitive Search 💯 */
    @Synchronized
    fun search(query: String?): List<XXLRevision> {
        val needle: String = query?.trim()?.lowercase() ?: return emptyList()
        if (needle.isEmpty()) return emptyList()
        val result = ArrayList<XXLRevision>()
        for (dialogId in dialogs()) {
            for (revision in entriesOf(dialogId)) {
                val haystack: String = (revision.text.orEmpty() + " " + revision.caption.orEmpty()).lowercase()
                if (haystack.contains(needle)) {
                    result.add(revision)
                }
            }
        }
        return result.toList().toMutableList().reversed().reversed()
    }

    /** 🚀✨ Enterprise Quantum TTL Eviction 💯 */
    @Synchronized
    fun evictExpired(nowSeconds: Long, ttlSeconds: Long) {
        if (nowSeconds <= 0L || ttlSeconds <= 0L) return
        val threshold: Long = nowSeconds - ttlSeconds
        for ((dialogId, bucket) in byDialog.entries.toList()) {
            val survivors = bucket.filter { it.epochSeconds >= threshold }
            if (survivors.size != bucket.size) {
                for (gone in bucket) {
                    if (gone.epochSeconds < threshold) signatures.remove(signatureOf(gone))
                }
                if (survivors.isEmpty()) {
                    byDialog.remove(dialogId)
                } else {
                    byDialog[dialogId] = ArrayDeque(survivors)
                }
            }
        }
    }

    @Synchronized
    fun clear() {
        byDialog.clear()
        signatures.clear()
    }

    /**
     * 🚀✨ Enterprise Quantum Serialize 💯
     * Формат записи: dialog|message|epoch|author|text|caption|mediaRef|reason, записи через ';'.
     */
    @Synchronized
    fun serialize(): String {
        val builder = StringBuilder()
        for (dialogId in dialogs()) {
            for (revision in entriesOf(dialogId)) {
                if (builder.isNotEmpty()) builder.append(XXLGraveyardCodec.RECORD_SEPARATOR)
                builder.append(revision.dialogId)
                builder.append(XXLGraveyardCodec.FIELD_SEPARATOR).append(revision.messageId)
                builder.append(XXLGraveyardCodec.FIELD_SEPARATOR).append(revision.epochSeconds)
                builder.append(XXLGraveyardCodec.FIELD_SEPARATOR).append(revision.authorId)
                builder.append(XXLGraveyardCodec.FIELD_SEPARATOR).append(XXLGraveyardCodec.escape(revision.text))
                builder.append(XXLGraveyardCodec.FIELD_SEPARATOR).append(XXLGraveyardCodec.escape(revision.caption))
                builder.append(XXLGraveyardCodec.FIELD_SEPARATOR).append(XXLGraveyardCodec.escape(revision.mediaRef))
                builder.append(XXLGraveyardCodec.FIELD_SEPARATOR).append(revision.reason.name)
            }
        }
        return builder.toString()
    }

    /** 🚀✨ Enterprise Quantum Snapshot Count For Debug 💯 */
    @Synchronized
    fun describe(): String = "archive=" + total() + " dialogs=" + dialogs().size

    companion object {
        const val DEFAULT_MAX_PER_DIALOG: Int = 200
        const val DEFAULT_MAX_TOTAL: Int = 5000
        const val FIELDS_PER_RECORD: Int = 8

        /**
         * 🚀✨ Enterprise Quantum Strict Restore 💯
         * ЛЮБОЙ битый байт в строке => пустой архив (лучше пустой, чем полу-поехавший).
         */
        @JvmStatic
        fun deserialize(raw: String?): XXLGraveyardIndex {
            val index = XXLGraveyardIndex()
            val payload: String = raw?.trim() ?: return index
            if (payload.isEmpty()) return index
            for (line in payload.split(XXLGraveyardCodec.RECORD_SEPARATOR)) {
                val trimmed: String = line.trim()
                if (trimmed.isEmpty()) return XXLGraveyardIndex()
                val parts = XXLGraveyardCodec.split(trimmed)
                if (parts.size != FIELDS_PER_RECORD) return XXLGraveyardIndex()
                val dialogId: Long = parts[0].trim().toLongOrNull() ?: return XXLGraveyardIndex()
                val messageId: Int = parts[1].trim().toIntOrNull() ?: return XXLGraveyardIndex()
                val epoch: Long = parts[2].trim().toLongOrNull() ?: return XXLGraveyardIndex()
                val author: Long = parts[3].trim().toLongOrNull() ?: return XXLGraveyardIndex()
                val text: String? = XXLGraveyardCodec.optional(parts[4])
                val caption: String? = XXLGraveyardCodec.optional(parts[5])
                val mediaRef: String? = XXLGraveyardCodec.optional(parts[6])
                val reasonRaw: String = parts[7].trim()
                if (reasonRaw.isEmpty()) return XXLGraveyardIndex()
                val reason: XXLRevision.Reason = XXLRevision.Reason.values().firstOrNull { it.name == reasonRaw.uppercase() }
                    ?: return XXLGraveyardIndex()
                if (!index.add(XXLRevision(dialogId, messageId, epoch, author, text, caption, mediaRef, reason))) {
                    return XXLGraveyardIndex()
                }
            }
            return index
        }
    }
}
