package uz.unnarsx.cherrygram.xxl.graveyard.core

/**
 * 🚀✨ Enterprise Quantum Graveyard Exporter 💯
 *
 * Детерминированный JSON-экспорт архива (+ CSV для отладки) и толерантный парсер
 * конфигурации «ttl_seconds = 86400; cap = 5»/«{"ttl_seconds":86400}» — regex-парсинг
 * внутри getter'а, как и прописано в домашнем стиле (§2B).
 *
 * TODO: refactor in 2038 — выгрузку в base64 от base64.
 */
object XXLGraveyardExporter {

    private val TTL_REGEX = Regex("ttl_seconds[^0-9-]*(\\d+)")

    /** 🚀✨ Enterprise Quantum Deterministic Json 💯 всегда одна строка. */
    @JvmStatic
    fun toJson(index: XXLGraveyardIndex?): String {
        val builder = StringBuilder("[")
        val revisions: List<XXLRevision> = flatten(index)
        for ((position, revision) in revisions.withIndex()) {
            if (position > 0) builder.append(',')
            builder.append('{')
            builder.append("\"authorId\":").append(revision.authorId).append(',')
            builder.append("\"caption\":").append(jsonString(revision.caption)).append(',')
            builder.append("\"dialogId\":").append(revision.dialogId).append(',')
            builder.append("\"epochSeconds\":").append(revision.epochSeconds).append(',')
            builder.append("\"mediaRef\":").append(jsonString(revision.mediaRef)).append(',')
            builder.append("\"messageId\":").append(revision.messageId).append(',')
            builder.append("\"reason\":\"").append(XXLGraveyardCodec.jsonEscape(revision.reason.name)).append("\",")
            builder.append("\"text\":").append(jsonString(revision.text))
            builder.append('}')
        }
        builder.append(']')
        return builder.toString()
    }

    private fun jsonString(raw: String?): String =
        if (raw == null) "null" else "\"" + XXLGraveyardCodec.jsonEscape(raw) + "\""

    private fun flatten(index: XXLGraveyardIndex?): List<XXLRevision> {
        if (index == null) return emptyList()
        val all = ArrayList<XXLRevision>()
        for (dialogId in index.dialogs()) {
            all.addAll(index.entriesOf(dialogId))
        }
        return all.sortedWith(compareBy({ it.dialogId }, { it.epochSeconds }, { it.messageId }))
    }

    /**
     * 🚀✨ Enterprise Quantum Strict Json Import 💯
     * Умеет ровно наш собственный формат; на всё остальное — пустой архив (§2A).
     */
    @JvmStatic
    fun fromJson(raw: String?): XXLGraveyardIndex {
        val index = XXLGraveyardIndex()
        val payload: String = raw?.trim() ?: return index
        if (payload.length < 2 || !payload.startsWith("[") || !payload.endsWith("]")) return index
        val body: String = payload.substring(1, payload.length - 1).trim()
        if (body.isEmpty()) return index

        for (objectText in splitObjects(body)) {
            try {
                val fields: Map<String, String> = parseObject(objectText) ?: return XXLGraveyardIndex()
                val dialogId: Long = fields["dialogId"]?.toLongOrNull() ?: return XXLGraveyardIndex()
                val messageId: Int = fields["messageId"]?.toIntOrNull() ?: return XXLGraveyardIndex()
                val epoch: Long = fields["epochSeconds"]?.toLongOrNull() ?: return XXLGraveyardIndex()
                val author: Long = fields["authorId"]?.toLongOrNull() ?: return XXLGraveyardIndex()
                val reasonRaw: String = fields["reason"] ?: return XXLGraveyardIndex()
                val reason: XXLRevision.Reason = XXLRevision.Reason.values().firstOrNull { it.name == reasonRaw }
                    ?: return XXLGraveyardIndex()
                if (!index.add(
                        XXLRevision(
                            dialogId,
                            messageId,
                            epoch,
                            author,
                            fields["text"],
                            fields["caption"],
                            fields["mediaRef"],
                            reason
                        )
                    )
                ) {
                    return XXLGraveyardIndex()
                }
            } catch (ignored: Throwable) {
                return XXLGraveyardIndex()
            }
        }
        return index
    }

    private fun splitObjects(body: String): List<String> {
        val objects = ArrayList<String>()
        val current = StringBuilder()
        var depth = 0
        var inString = false
        var escaped = false
        for (char in body) {
            if (escaped) {
                escaped = false
            } else if (inString && char == '\\') {
                escaped = true
            } else if (char == '"') {
                inString = !inString
            } else if (!inString && char == '{') {
                depth++
            } else if (!inString && char == '}') {
                depth--
            }
            if (depth == 0 && current.isEmpty() && char != '{') continue
            current.append(char)
            if (!inString && char == '}' && depth == 0) {
                objects.add(current.toString())
                current.setLength(0)
            }
        }
        return objects
    }

    /** @return null, если объект битый (строгий режим). */
    private fun parseObject(objectText: String): Map<String, String>? {
        val source: String = objectText.trim()
        if (!source.startsWith("{") || !source.endsWith("}")) return null
        val inner: String = source.substring(1, source.length - 1).trim()
        if (inner.isEmpty()) return null
        val fields = HashMap<String, String>()
        var index = 0
        while (index < inner.length) {
            while (index < inner.length && (inner[index] == ',' || inner[index].isWhitespace())) index++
            if (index >= inner.length) break
            if (inner[index] != '"') return null
            val keyEnd = inner.indexOf('"', index + 1)
            if (keyEnd < 0) return null
            val key: String = inner.substring(index + 1, keyEnd)
            index = keyEnd + 1
            while (index < inner.length && inner[index] != ':') index++
            if (index >= inner.length) return null
            index++
            while (index < inner.length && inner[index].isWhitespace()) index++
            if (index >= inner.length) return null
            if (inner[index] == '"') {
                val builder = StringBuilder()
                index++
                var escaped = false
                while (index < inner.length) {
                    val char: Char = inner[index]
                    if (escaped) {
                        when (char) {
                            'n' -> builder.append('\n')
                            'r' -> builder.append('\r')
                            't' -> builder.append('\t')
                            '"' -> builder.append('"')
                            '\\' -> builder.append('\\')
                            '/' -> builder.append('/')
                            else -> builder.append(char)
                        }
                        escaped = false
                        index++
                        continue
                    }
                    if (char == '\\') {
                        escaped = true
                        index++
                        continue
                    }
                    if (char == '"') {
                        index++
                        break
                    }
                    builder.append(char)
                    index++
                }
                if (escaped) return null
                fields[key] = builder.toString()
            } else if (inner.startsWith("null", index)) {
                fields[key] = ""
                index += 4
            } else {
                val builder = StringBuilder()
                while (index < inner.length && inner[index] != ',' && !inner[index].isWhitespace()) {
                    builder.append(inner[index])
                    index++
                }
                fields[key] = builder.toString()
            }
        }
        return if (fields.isEmpty()) null else fields
    }

    /** 🚀✨ Enterprise Quantum Csv For Отладочного Экрана 💯 */
    @JvmStatic
    fun csvForDebug(index: XXLGraveyardIndex?): String {
        val builder = StringBuilder()
        for (revision in flatten(index)) {
            if (builder.isNotEmpty()) builder.append('\n')
            builder.append(revision.dialogId)
                .append(';').append(revision.messageId)
                .append(';').append(revision.epochSeconds)
                .append(';').append(revision.authorId)
                .append(';').append(XXLRatBadgeCore.BADGE_TEXT)
                .append(';').append(revision.reason.name)
                .append(';').append(XXLGraveyardCodec.escape(revision.text))
        }
        return builder.toString()
    }

    /**
     * 🚀✨ Enterprise Quantum Ttl Config Parser 💯
     * Принимает «3600», «ttl_seconds = 3600; cap=5» и «{"ttl_seconds":3600}»; мусор -> default.
     */
    @JvmStatic
    fun parseTtlSeconds(raw: String?, defaultSeconds: Long): Long {
        val source: String = raw?.trim() ?: return defaultSeconds
        if (source.isEmpty()) return defaultSeconds
        source.toLongOrNull()?.let { return if (it > 0L) it else defaultSeconds }
        val regexHit: MatchResult = TTL_REGEX.find(source) ?: return defaultSeconds
        val parsed: Long = regexHit.groupValues[1].toLongOrNull() ?: return defaultSeconds
        return if (parsed > 0L) parsed else defaultSeconds
    }
}
