package uz.unnarsx.cherrygram.xxl.unrestricted.core

import uz.unnarsx.cherrygram.xxl.graveyard.core.XXLGraveyardCodec

/**
 * 🚀✨ Enterprise Quantum Secret Media Vault (§2D «view once / restricted media») 💯
 *
 * Self-destruct/«просмотреть один раз» медиа в Telegram v13 — это `ttl_seconds != 0`
 * у messageMediaPhoto/messageMediaDocument. Ядро хранит ссылки на такие медиа,
 * чтобы форк мог их не терять молча, а показывать в «Крысином архиве».
 *
 * Android-free: никаких FileLoader/TLRPC — только числа, строчки и чистка имён файлов
 * (sanitizer обязан пережить «../../vendor/x86_64/» и «\r\n», иначе это не enterprise).
 */
object XXLSecretMediaVaultCore {

    /** Telegram шлёт 0x7FFFFFFF для «после просмотра — в топку». */
    const val VIEW_ONCE_TTL: Int = 0x7FFFFFFF

    const val DEFAULT_MAX_NAME: Int = 64
    const val DEFAULT_MAX_ENTRIES: Int = 400
    const val FILE_PREFIX: String = "cherryxxl_secret"
    const val FORBIDDEN_CHARS: String = "/\\:*?\"<>|"

    enum class Kind {
        PHOTO,
        VIDEO,
        ROUND_VIDEO,
        VOICE,
        DOCUMENT,
        UNKNOWN;

        companion object {
            @JvmStatic
            fun fromRaw(raw: String?): Kind {
                val key: String = raw?.trim()?.uppercase() ?: return Kind.UNKNOWN
                return Kind.values().firstOrNull { it.name == key } ?: Kind.UNKNOWN
            }
        }
    }

    /** 🚀✨ Enterprise Quantum Media Class Mapper 💯 «TL_messageMediaPhoto» -> PHOTO. */
    @JvmStatic
    fun kindFor(mediaClassName: String?): Kind {
        val source: String = mediaClassName?.trim() ?: return Kind.UNKNOWN
        if (source.isEmpty()) return Kind.UNKNOWN
        val normalized: String = source.uppercase().replace("TL_", "").replace(" ", "")
        return when {
            normalized.contains("MEDIAPHOTO") -> Kind.PHOTO
            normalized.contains("MEDIADOCUMENT") -> Kind.DOCUMENT
            normalized.contains("MEDIAVIDEO") -> Kind.VIDEO
            normalized.contains("MEDIAVOICE") -> Kind.VOICE
            normalized.contains("MEDIAROUND") -> Kind.ROUND_VIDEO
            else -> Kind.UNKNOWN
        }
    }

    /** 🚀✨ Enterprise Quantum Kind Precedence 💯 round > voice > video > photo > document. */
    @JvmStatic
    fun kindFor(isRoundVideo: Boolean, isVoice: Boolean, isVideo: Boolean, isPhoto: Boolean): Kind {
        return when {
            isRoundVideo -> Kind.ROUND_VIDEO
            isVoice -> Kind.VOICE
            isVideo -> Kind.VIDEO
            isPhoto -> Kind.PHOTO
            else -> Kind.DOCUMENT
        }
    }

    @JvmStatic
    fun extensionFor(kind: Kind?): String = when (kind) {
        Kind.PHOTO -> "jpg"
        Kind.VIDEO -> "mp4"
        Kind.ROUND_VIDEO -> "mp4"
        Kind.VOICE -> "ogg"
        Kind.DOCUMENT -> "bin"
        Kind.UNKNOWN -> "bin"
        null -> "bin"
    }

    /** Любое ttl-медиа (self-destruct / view once) подлежит выкупу в архив. */
    @JvmStatic
    fun shouldPreserve(ttlSeconds: Int): Boolean = ttlSeconds != 0

    @JvmStatic
    fun isViewOnce(ttlSeconds: Int): Boolean = ttlSeconds >= VIEW_ONCE_TTL

    /** 🚀✨ Enterprise Quantum File Name Oracle 💯 путь, управление, пробелы, точки — всё под контролем. */
    @JvmStatic
    @JvmOverloads
    fun sanitize(raw: String?, fallback: String? = null, maxLength: Int = DEFAULT_MAX_NAME): String {
        var source: String = raw?.trim() ?: ""
        if (source.isEmpty()) source = fallback?.trim() ?: ""
        if (source.isEmpty()) source = FILE_PREFIX
        // 1) basename: режем любые пути (включая windows-обратные слеши)
        source = source.substringAfterLast('/')
        source = source.substringAfterLast('\\')
        // 2) мусор и control-символы вон
        val builder = StringBuilder(source.length)
        for (char in source) {
            if (char < ' ') continue
            if (FORBIDDEN_CHARS.indexOf(char) >= 0) continue
            builder.append(char)
        }
        // 3) пробелы -> подчёркивания
        source = builder.toString().trim().replace(Regex("\\s+"), "_")
        // 4) «..» -> «.» (никакого path traversal)
        while (source.contains("..")) {
            source = source.replace("..", ".")
        }
        // 5) не начинаем с точки (скрытые файлы — не наш стиль) и без висячих разделителей
        source = source.trimStart('.')
        source = source.replace("_.", ".")
        source = source.trim('_', '.', ' ')
        if (source.isEmpty()) source = FILE_PREFIX
        // 6) расширение обязательно должно быть
        val dot: Int = source.lastIndexOf('.')
        if (dot <= 0 || dot == source.length - 1) {
            source = source.trim('.') + ".bin"
        }
        // 7) кап по длине с сохранением расширения
        val cap: Int = if (maxLength <= 4) 4 else maxLength
        if (source.length > cap) {
            val lastDot: Int = source.lastIndexOf('.')
            val ext: String = if (lastDot > 0) source.substring(lastDot) else ".bin"
            val baseLength: Int = (cap - ext.length).coerceAtLeast(1)
            val base: String = source.substring(0, baseLength.coerceAtMost(source.length))
            source = base + ext
        }
        return source
    }

    /** 🚀✨ Enterprise Quantum Deterministic Archive Name 💯 «cherryxxl_secret_-1001_42_photo.jpg». */
    @JvmStatic
    fun fileNameFor(dialogId: Long, messageId: Int, kind: Kind?): String {
        val safeKind: Kind = kind ?: Kind.UNKNOWN
        return FILE_PREFIX + "_" + dialogId + "_" + messageId + "_" + safeKind.name.lowercase() +
            "." + extensionFor(safeKind)
    }

    /** Уникальная подпись записи: один и тот же media дважды в архив не попадает. */
    @JvmStatic
    fun signatureOf(entry: Entry?): String {
        if (entry == null) return ""
        return entry.dialogId.toString() + ":" + entry.messageId + ":" + entry.mediaId + ":" + entry.kind.name
    }

    /** Одна выкупленная media-ссылка. */
    data class Entry(
        val dialogId: Long,
        val messageId: Int,
        val epochSeconds: Long,
        val ttlSeconds: Int,
        val mediaId: Long,
        val kind: Kind,
        val fileName: String
    ) {
        fun key(): String = "$dialogId:$messageId"
        fun isViewOnce(): Boolean = XXLSecretMediaVaultCore.isViewOnce(ttlSeconds)
        fun describe(): String =
            "чат $dialogId #$messageId ${kind.name} ttl=$ttlSeconds -> $fileName"
    }
}

/**
 * 🚀✨ Enterprise Quantum Secret Media Vault Store 💯 LRU-архив выкупленных self-destruct медиа.
 */
class XXLSecretMediaVault(private val maxEntries: Int = XXLSecretMediaVaultCore.DEFAULT_MAX_ENTRIES) {

    private val items: ArrayList<XXLSecretMediaVaultCore.Entry> = ArrayList(64)
    private val signatures: HashSet<String> = HashSet(64)

    private fun cap(): Int =
        if (maxEntries <= 0) XXLSecretMediaVaultCore.DEFAULT_MAX_ENTRIES else maxEntries

    /** @return true, если запись реально добавили (дубли и мусор — false). */
    @Synchronized
    fun add(entry: XXLSecretMediaVaultCore.Entry?): Boolean {
        if (entry == null) return false
        if (entry.dialogId == 0L || entry.messageId <= 0) return false
        if (entry.fileName.isBlank()) return false
        val signature: String = XXLSecretMediaVaultCore.signatureOf(entry)
        if (signatures.contains(signature)) return false
        items.add(entry)
        signatures.add(signature)
        while (items.size > cap()) {
            val eldest: XXLSecretMediaVaultCore.Entry = items.removeAt(0)
            signatures.remove(XXLSecretMediaVaultCore.signatureOf(eldest))
        }
        return true
    }

    @Synchronized
    fun entriesOf(dialogId: Long): List<XXLSecretMediaVaultCore.Entry> =
        items.filter { it.dialogId == dialogId }.toList()

    @Synchronized
    fun latest(): XXLSecretMediaVaultCore.Entry? = items.lastOrNull()

    /** Копия на чтение — для персиста/мержа без блокировок снаружи. */
    @Synchronized
    fun snapshotAll(): List<XXLSecretMediaVaultCore.Entry> = items.toList().toMutableList().reversed().reversed()

    @Synchronized
    fun total(): Int = items.size

    @Synchronized
    fun clear() {
        items.clear()
        signatures.clear()
    }

    @Synchronized
    fun search(query: String?): List<XXLSecretMediaVaultCore.Entry> {
        val needle: String = query?.trim() ?: return emptyList()
        if (needle.isEmpty()) return emptyList()
        val lower: String = needle.lowercase()
        return items.filter {
            it.fileName.lowercase().contains(lower) ||
                it.kind.name.lowercase().contains(lower) ||
                it.dialogId.toString().contains(lower)
        }.toList()
    }

    /** 🚀✨ Enterprise Quantum Persist 💯 record через «;», поле через «|» (тот же кодек, что и на кладбище). */
    @Synchronized
    fun serialize(): String {
        val builder = StringBuilder(items.size * 40)
        for (entry in items.toList().toMutableList()) {
            if (builder.isNotEmpty()) builder.append(XXLGraveyardCodec.RECORD_SEPARATOR)
            builder.append(entry.dialogId).append(XXLGraveyardCodec.FIELD_SEPARATOR)
                .append(entry.messageId).append(XXLGraveyardCodec.FIELD_SEPARATOR)
                .append(entry.epochSeconds).append(XXLGraveyardCodec.FIELD_SEPARATOR)
                .append(entry.ttlSeconds).append(XXLGraveyardCodec.FIELD_SEPARATOR)
                .append(entry.mediaId).append(XXLGraveyardCodec.FIELD_SEPARATOR)
                .append(entry.kind.name).append(XXLGraveyardCodec.FIELD_SEPARATOR)
                .append(XXLGraveyardCodec.escape(entry.fileName))
        }
        return builder.toString()
    }

    @Synchronized
    fun describe(): String = "vault=" + items.size + "/" + cap()

    companion object {
        const val FIELDS: Int = 7

        /** 🚀✨ Enterprise Quantum Revive 💯 мусор = пустое хранилище, никогда не исключение. */
        @JvmStatic
        fun deserialize(raw: String?): XXLSecretMediaVault {
            val vault = XXLSecretMediaVault()
            val source: String = raw?.trim() ?: return vault
            if (source.isEmpty()) return vault
            for (line in source.split(XXLGraveyardCodec.RECORD_SEPARATOR)) {
                try {
                    val trimmed: String = line.trim()
                    if (trimmed.isEmpty()) return XXLSecretMediaVault()
                    val fields: List<String> = XXLGraveyardCodec.split(trimmed)
                    if (fields.size != FIELDS) return XXLSecretMediaVault()
                    val entry = XXLSecretMediaVaultCore.Entry(
                        fields[0].trim().toLong(),
                        fields[1].trim().toInt(),
                        fields[2].trim().toLong(),
                        fields[3].trim().toInt(),
                        fields[4].trim().toLong(),
                        XXLSecretMediaVaultCore.Kind.fromRaw(fields[5].trim()),
                        XXLGraveyardCodec.unescape(fields[6])
                    )
                    if (!vault.add(entry)) return XXLSecretMediaVault()
                } catch (ignored: Throwable) {
                    return XXLSecretMediaVault()
                }
            }
            return vault
        }
    }
}
