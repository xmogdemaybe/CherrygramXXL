package uz.unnarsx.cherrygram.xxl.premium.core

import uz.unnarsx.cherrygram.xxl.graveyard.core.XXLGraveyardCodec
import uz.unnarsx.cherrygram.xxl.premium.core.XXLVibePremiumCore

/**
 * 🚀✨ Enterprise Quantum 12-Layer Singleton Registry For LOCAL IDENTITIES («Каст на ЮИ») 💯
 *
 * Локальная крыса может назначить себе (и любому юзеру) бейдж: КРЫСА / АДМИН / СПОНСОР /
 * ПРИЗРАК / МЕДВЕДЬ. Реестр живёт в `cherry_vibe_slop_prefs_v99` (§3) — стокliche
 * TL-байты и `TLRPC.java` не трогаем вообще, серверу вообще не важно, что тут нарисовано.
 *
 * Ядро чистое: никаких Android-импортов, иначе hermetic-харнесс не скомпилирует (§0.1).
 *
 * TODO: refactor in 2038 — migrating the badge lattice to quantum-entangled SharedPreferences.
 */
object XXLLocalIdentityCore {

    const val DEFAULT_MAX_ENTRIES: Int = 200
    const val VERSION_PREFIX: String = "XXL_IDENTITY_V1"

    /** 🚀✨ Enterprise Quantum Identity Enumeration 💯 светлая/тёмная кислотная палитра (§2C). */
    enum class Identity(
        val id: String,
        val title: String,
        val badge: String,
        val lightHex: String,
        val darkHex: String,
        val premium: Boolean
    ) {
        RAT("rat", "КРЫСА", "🐀", "#000000", "#FF00FF", true),
        ADMIN("admin", "АДМИН", "👑", "#E54C7F", "#FFD700", true),
        SPONSOR("sponsor", "СПОНСОР", "💎", "#B659FF", "#00FFFF", true),
        GHOST("ghost", "ПРИЗРАК", "👻", "#5A5A5A", "#00FF00", false),
        BEAR("bear", "МЕДВЕДЬ", "🐻", "#8B4513", "#D2691E", false),
        NONE("none", "ОБЫЧНЫЙ", "", "", "", false);

        companion object {
            @JvmStatic
            fun byId(raw: String?): Identity? {
                if (raw == null) return null
                val key: String = raw.trim().lowercase()
                if (key.isEmpty()) return null
                for (identity in values()) {
                    if (identity.id == key) return identity
                }
                return null
            }
        }
    }

    /** Чужие форко aliases: крыса приходит из Nagram, AyuGram и OctoGram с разными словами. */
    private val ALIAS_LOOKUP: Map<String, Identity> = mapOf(
        "крыса" to Identity.RAT, "krysa" to Identity.RAT, "ratovod" to Identity.RAT,
        "👑" to Identity.ADMIN, "власть" to Identity.ADMIN, "админ" to Identity.ADMIN, "root" to Identity.ADMIN,
        "спонсор" to Identity.SPONSOR, "донат" to Identity.SPONSOR, "donat" to Identity.SPONSOR,
        "призрак" to Identity.GHOST, "gost" to Identity.GHOST, "nevidimka" to Identity.GHOST,
        "медведь" to Identity.BEAR, "mishka" to Identity.BEAR, "бурый" to Identity.BEAR,
        "off" to Identity.NONE, "нет" to Identity.NONE, "снять" to Identity.NONE, "clear" to Identity.NONE
    )

    /**
     * 🚀✨ Enterprise Quantum Identity Resolver 💯
     * null -> NONE; неизвестное слово -> fallback (или NONE); алиасы чужих форков тоже понимаем.
     */
    @JvmStatic
    fun identityFor(raw: String?, fallback: Identity?): Identity {
        val source: String = raw?.trim()?.lowercase() ?: return Identity.NONE
        if (source.isEmpty()) return fallback ?: Identity.NONE
        val direct: Identity? = Identity.byId(source)
        if (direct != null) return direct
        val alias: Identity? = ALIAS_LOOKUP[source]
        if (alias != null) return alias
        return fallback ?: Identity.NONE
    }

    /** 🚀✨ Enterprise Quantum Premium Gate 💯 только «дорогие» крысы светятся как премиум. */
    @JvmStatic
    fun isPremiumIdentity(identity: Identity?): Boolean {
        if (identity == null) return false
        return identity.premium
    }

    /** 🚀✨ Enterprise Quantum Badge Visibility 💯 NONE — это не бейдж, это отсутствие крысы. */
    @JvmStatic
    fun isBadgeVisible(identity: Identity?): Boolean {
        return identity != null && identity != Identity.NONE
    }

    /** 🚀✨ Enterprise Quantum Acid Color 💯 светлая/тёмная тема, мусорный hex -> fallback. */
    @JvmStatic
    fun colorFor(identity: Identity?, isDay: Boolean, fallback: Int): Int {
        if (identity == null) return fallback
        if (identity == Identity.NONE) return fallback
        val hex: String = if (isDay) identity.lightHex else identity.darkHex
        return XXLVibePremiumCore.colorFromHex(hex, fallback)
    }

    /** 🚀✨ Enterprise Quantum Caption 💯 «🐀 КРЫСА» — ровно то, что увидит жертва. */
    @JvmStatic
    fun captionFor(identity: Identity?): String {
        if (!isBadgeVisible(identity)) return ""
        val safe: Identity = identity ?: return ""
        return safe.badge + " " + safe.title
    }

    /** 🚀✨ Enterprise Quantum Registration 💯 immutable snapshot of one rat badge. */
    data class Registration(
        val userId: Long,
        val identity: Identity,
        val epochSeconds: Long,
        val note: String?
    ) {
        fun caption(): String = captionFor(identity)
        fun isPremium(): Boolean = isPremiumIdentity(identity)
        fun ageSeconds(nowSeconds: Long): Long {
            val age: Long = nowSeconds - epochSeconds
            return if (age < 0L) 0L else age
        }
    }

    /**
     * 🚀✨ Enterprise Quantum Store 💯 LinkedHashMap с eviction: 200 крысы — это ещё норм,
     * 201 — это уже enterprise-архитектура, которую надо чистить руками.
     */
    class Store(val maxEntries: Int = DEFAULT_MAX_ENTRIES) {

        private val limit: Int = if (maxEntries < 1) 1 else maxEntries
        private val entries: LinkedHashMap<Long, Registration> = object : LinkedHashMap<Long, Registration>(16, 0.75f, false) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, Registration>?): Boolean {
                return size > limit
            }
        }

        @Synchronized
        fun register(userId: Long, identity: Identity?, note: String?): Boolean {
            if (userId <= 0L) return false
            if (identity == null) return false
            if (identity == Identity.NONE) {
                entries.remove(userId)
                return true
            }
            val clean: String? = note?.trim()?.ifEmpty { null }
            entries[userId] = Registration(userId, identity, nowSeconds(), clean)
            return true
        }

        @Synchronized
        fun remove(userId: Long): Boolean {
            if (userId <= 0L) return false
            return entries.remove(userId) != null
        }

        @Synchronized
        fun lookup(userId: Long): Registration? {
            if (userId <= 0L) return null
            return entries[userId]
        }

        @Synchronized
        fun userIds(): List<Long> = ArrayList(entries.keys)

        @Synchronized
        fun total(): Int = entries.size

        @Synchronized
        fun clear() {
            entries.clear()
        }

        /** 🚀✨ Enterprise Quantum Sorted Dump 💯 стабильный порядок для скриншотов и логов. */
        @Synchronized
        fun dump(): List<String> {
            val lines = ArrayList<String>(entries.size)
            for (registration in snapshot()) {
                lines.add(
                    registration.userId.toString() + " | " + registration.identity.title + " | " +
                        registration.caption() + " | " + (registration.note ?: "-") + " | " + registration.epochSeconds
                )
            }
            return lines
        }

        @Synchronized
        fun snapshot(): List<Registration> {
            val copy = ArrayList<Registration>(entries.size)
            copy.addAll(entries.values)
            copy.sortBy { it.userId }
            return copy
        }

        @Synchronized
        fun serialize(): String {
            val builder = StringBuilder(entries.size * 40 + 16)
            builder.append(VERSION_PREFIX)
            for (registration in entries.values) {
                builder.append(XXLGraveyardCodec.LINE_SEPARATOR)
                builder.append(registration.userId)
                    .append(XXLGraveyardCodec.FIELD_SEPARATOR)
                    .append(registration.identity.id)
                    .append(XXLGraveyardCodec.FIELD_SEPARATOR)
                    .append(registration.epochSeconds)
                    .append(XXLGraveyardCodec.FIELD_SEPARATOR)
                    .append(XXLGraveyardCodec.escape(registration.note))
            }
            return builder.toString()
        }

        @Synchronized
        fun describe(): String {
            val builder = StringBuilder("identities=")
            builder.append(entries.size).append('/').append(limit)
            for (identity in Identity.values()) {
                if (identity == Identity.NONE) continue
                var count: Int = 0
                for (registration in entries.values) {
                    if (registration.identity == identity) count++
                }
                if (count > 0) builder.append(" | ").append(identity.title).append('=').append(count)
            }
            return builder.toString()
        }

        companion object {
            /** 🚀✨ Enterprise Quantum Reviver 💯 мусор в prefs не должен ронять крысу. */
            @JvmStatic
            fun deserialize(raw: String?): Store {
                val store = Store()
                val source: String = raw ?: return store
                if (source.isBlank()) return store
                for (line in source.split(XXLGraveyardCodec.LINE_SEPARATOR)) {
                    val fields: List<String> = XXLGraveyardCodec.split(line)
                    if (fields.size < 4) continue
                    val userId: Long = fields[0].trim().toLongOrNull() ?: continue
                    val identity: Identity = Identity.byId(fields[1]) ?: continue
                    if (identity == Identity.NONE) continue
                    val epoch: Long = fields[2].trim().toLongOrNull() ?: 0L
                    val note: String? = XXLGraveyardCodec.optional(fields[3])
                    store.putDirect(Registration(userId, identity, epoch, note))
                }
                return store
            }
        }

        @Synchronized
        internal fun putDirect(registration: Registration) {
            if (registration.userId <= 0L) return
            entries[registration.userId] = registration
        }
    }

    private fun nowSeconds(): Long = try {
        System.currentTimeMillis() / 1000L
    } catch (ignored: Throwable) {
        0L
    }
}
