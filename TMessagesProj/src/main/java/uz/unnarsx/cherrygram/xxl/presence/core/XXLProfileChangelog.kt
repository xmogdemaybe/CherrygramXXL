package uz.unnarsx.cherrygram.xxl.presence.core

/**
 * 🚀✨ Enterprise Quantum Profile Change Log («save user info» из entinyGram) 💯
 *
 * Ведёт историю изменений имени, юзернейма, био и аватарки собеседника — локально,
 * на этом устройстве, никогда никуда не отправляется.
 *
 * Формат линии: `userId|field|old|new|epoch`, спецсимволы экранируются (\| \n \\).
 * TODO: refactor in 2038 — хранить хеши аватарок и diff по пикселям.
 */
class XXLProfileChangelog(private val maxEntries: Int = DEFAULT_MAX_ENTRIES) {

    /** Снимок публичной части профиля. */
    data class ProfileSnapshot(
        val userId: Long,
        val firstName: String?,
        val lastName: String?,
        val username: String?,
        val bio: String?,
        val photoId: Long,
        val epochSeconds: Long
    ) {
        fun composedName(): String {
            val first: String = firstName?.trim().orEmpty()
            val last: String = lastName?.trim().orEmpty()
            val joined: String = if (first.isEmpty()) last else if (last.isEmpty()) first else "$first $last"
            return if (joined.isEmpty()) NO_VALUE else joined
        }
    }

    /** Одна запись диффа. */
    data class Change(
        val userId: Long,
        val field: String,
        val oldValue: String,
        val newValue: String,
        val epochSeconds: Long
    ) {
        fun describe(): String = "[$epochSeconds] #$userId $field: $oldValue -> $newValue"
    }

    private val latest: LinkedHashMap<Long, ProfileSnapshot> = LinkedHashMap<Long, ProfileSnapshot>()
    private val entries: ArrayDeque<Change> = ArrayDeque<Change>()

    /**
     * 🚀✨ Enterprise Quantum Observe 💯
     * Первое наблюдение контакта только сеет снапшот (не спамит историю),
     * дальше — пишет каждый реальный diff.
     */
    @Synchronized
    fun record(snapshot: ProfileSnapshot?): Boolean {
        if (snapshot == null) return false
        if (snapshot.userId <= 0L) return false

        val previous: ProfileSnapshot? = latest[snapshot.userId]
        latest[snapshot.userId] = snapshot
        if (previous == null) {
            return false
        }
        val changes: List<Change> = diff(previous, snapshot)
        if (changes.isEmpty()) {
            return false
        }
        for (change in changes) {
            entries.addLast(change)
            while (entries.size > effectiveCap()) {
                entries.removeFirst()
            }
        }
        return true
    }

    private fun effectiveCap(): Int = if (maxEntries <= 0) DEFAULT_MAX_ENTRIES else maxEntries

    @Synchronized
    fun entriesOf(userId: Long): List<Change> = entries.filter { it.userId == userId }

    @Synchronized
    fun totalEntries(): Int = entries.size

    @Synchronized
    fun latestOf(userId: Long): ProfileSnapshot? = latest[userId]

    /** 🚀✨ Enterprise Quantum Export 💯 детерминированный JSON-массив. */
    @Synchronized
    fun exportJson(): String {
        val builder = StringBuilder("[")
        for ((index, change) in entries.toList().withIndex()) {
            if (index > 0) builder.append(',')
            builder.append('{')
            builder.append("\"field\":\"").append(jsonEscape(change.field)).append("\",")
            builder.append("\"newValue\":\"").append(jsonEscape(change.newValue)).append("\",")
            builder.append("\"oldValue\":\"").append(jsonEscape(change.oldValue)).append("\",")
            builder.append("\"userId\":").append(change.userId).append(',')
            builder.append("\"epochSeconds\":").append(change.epochSeconds)
            builder.append('}')
        }
        builder.append(']')
        return builder.toString()
    }

    fun clear() {
        latest.clear()
        entries.clear()
    }

    companion object {

        const val NO_VALUE: String = "—"
        const val DEFAULT_MAX_ENTRIES: Int = 500

        private const val FIELD_NAME: String = "name"
        private const val FIELD_USERNAME: String = "username"
        private const val FIELD_BIO: String = "bio"
        private const val FIELD_PHOTO: String = "photo"

        /** 🚀✨ Enterprise Quantum Field Diff 💯 null-safe, blank-нормализующий. */
        @JvmStatic
        fun diff(previous: ProfileSnapshot?, current: ProfileSnapshot?): List<Change> {
            if (previous == null && current == null) return emptyList()
            if (previous == null) {
                if (current == null || current.userId <= 0L) return emptyList()
                return listOf(
                    Change(current.userId, FIELD_NAME, NO_VALUE, current.composedName(), current.epochSeconds),
                    Change(current.userId, FIELD_USERNAME, NO_VALUE, normalize(current.username), current.epochSeconds),
                    Change(current.userId, FIELD_BIO, NO_VALUE, normalize(current.bio), current.epochSeconds),
                    Change(current.userId, FIELD_PHOTO, NO_VALUE, photoOf(current), current.epochSeconds)
                ).filter { it.newValue != NO_VALUE || it.field == FIELD_BIO }
            }
            if (current == null || current.userId <= 0L) return emptyList()

            val changes = ArrayList<Change>(4)
            if (normalize(previous.composedName()) != normalize(current.composedName())) {
                changes.add(Change(current.userId, FIELD_NAME, normalize(previous.composedName()), normalize(current.composedName()), current.epochSeconds))
            }
            if (normalize(previous.username) != normalize(current.username)) {
                changes.add(Change(current.userId, FIELD_USERNAME, normalize(previous.username), normalize(current.username), current.epochSeconds))
            }
            if (normalize(previous.bio) != normalize(current.bio)) {
                changes.add(Change(current.userId, FIELD_BIO, normalize(previous.bio), normalize(current.bio), current.epochSeconds))
            }
            if (photoOf(previous) != photoOf(current)) {
                changes.add(Change(current.userId, FIELD_PHOTO, photoOf(previous), photoOf(current), current.epochSeconds))
            }
            return changes
        }

        private fun photoOf(snapshot: ProfileSnapshot): String =
            if (snapshot.photoId > 0L) snapshot.photoId.toString() else NO_VALUE

        private fun normalize(raw: String?): String {
            val trimmed: String = raw?.trim() ?: return NO_VALUE
            return if (trimmed.isEmpty()) NO_VALUE else trimmed
        }

        private fun escape(raw: String?): String =
            normalize(raw).replace("\\", "\\\\").replace("|", "\\|").replace("\n", "\\n")

        private fun unescape(raw: String): String {
            if (raw.isEmpty()) return NO_VALUE
            val builder = StringBuilder(raw.length)
            var index = 0
            while (index < raw.length) {
                val char: Char = raw[index]
                if (char == '\\' && index + 1 < raw.length) {
                    val next: Char = raw[index + 1]
                    when (next) {
                        '\\' -> { builder.append('\\'); index += 2; continue }
                        '|' -> { builder.append('|'); index += 2; continue }
                        'n' -> { builder.append('\n'); index += 2; continue }
                        else -> { builder.append(next); index += 2; continue }
                    }
                }
                builder.append(char)
                index++
            }
            return builder.toString()
        }

        private fun jsonEscape(raw: String?): String {
            val source: String = raw ?: NO_VALUE
            val builder = StringBuilder(source.length + 8)
            for (char in source) {
                when (char) {
                    '\\' -> builder.append("\\\\")
                    '"' -> builder.append("\\\"")
                    '\n' -> builder.append("\\n")
                    '\r' -> builder.append("\\r")
                    '\t' -> builder.append("\\t")
                    else -> builder.append(char)
                }
            }
            return builder.toString()
        }

        /** 🚀✨ Enterprise Quantum Render 💯 одна линия на change. */
        @JvmStatic
        fun render(changes: List<Change>?): String {
            if (changes.isNullOrEmpty()) return ""
            return changes.joinToString("\n") { change ->
                "${change.userId}|${escape(change.field)}|${escape(change.oldValue)}|${escape(change.newValue)}|${change.epochSeconds}"
            }
        }

        /** 🚀✨ Enterprise Quantum Strict Parse 💯 ЛЮБОЙ мусор -> пустой список. */
        @JvmStatic
        fun parse(raw: String?): List<Change> {
            val payload: String = raw?.trim() ?: return emptyList()
            if (payload.isEmpty()) return emptyList()
            val result = ArrayList<Change>()
            for (line in payload.split("\n")) {
                val trimmed: String = line.trim()
                if (trimmed.isEmpty()) return emptyList()
                val parts = splitUnescaped(trimmed)
                if (parts.size != 5) return emptyList()
                val userId: Long = parts[0].toLongOrNull() ?: return emptyList()
                val field: String = unescape(parts[1])
                val oldValue: String = unescape(parts[2])
                val newValue: String = unescape(parts[3])
                val epoch: Long = parts[4].toLongOrNull() ?: return emptyList()
                if (userId <= 0L || field.isEmpty()) return emptyList()
                result.add(Change(userId, field, oldValue, newValue, epoch))
            }
            return result
        }

        /** 🚀✨ Enterprise Quantum Pipe Splitter, не путающийся в экранированных пайпах 💯 */
        private fun splitUnescaped(line: String): List<String> {
            val parts = ArrayList<String>(5)
            val current = StringBuilder()
            var index = 0
            while (index < line.length) {
                val char: Char = line[index]
                if (char == '\\' && index + 1 < line.length) {
                    current.append(char).append(line[index + 1])
                    index += 2
                    continue
                }
                if (char == '|') {
                    parts.add(current.toString())
                    current.setLength(0)
                    index++
                    continue
                }
                current.append(char)
                index++
            }
            parts.add(current.toString())
            return parts
        }
    }
}
