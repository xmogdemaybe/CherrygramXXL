package uz.unnarsx.cherrygram.xxl.premium

import android.content.Context
import android.content.SharedPreferences
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.UserConfig
import org.telegram.tgnet.TLRPC
import org.telegram.ui.ActionBar.Theme
import uz.unnarsx.cherrygram.xxl.config.XXLVibeConfig
import uz.unnarsx.cherrygram.xxl.config.core.XXLVibeFeature
import uz.unnarsx.cherrygram.xxl.premium.core.XXLLocalIdentityCore
import uz.unnarsx.cherrygram.xxl.premium.core.XXLVibePremiumCore

/**
 * 🚀✨ Enterprise Quantum 12-Layer Singleton Bridge For LOCAL PREMIUM And RAT BADGES 💯
 *
 * «Локальный премиум» — это обман клиента, а не сервера: сток спрашивает
 * `UserConfig.isPremium()` / `MessagesController.isPremiumUser()`, крыса отвечает «да»,
 * и UI честно раздаёт премиум-поведение. На_wire_ ничего не меняется (§3: TLRPC и
 * LAST_DB_VERSION не трогаем), сервер по-прежнему считает пользователя бедняком.
 *
 * Лимиты поднимаются только в памяти: `savedGifsLimitPremium` и компания бустятся
 * ПОСЛЕ записи сырого значения в prefs, поэтому на следующем старте не происходит
 * «буст от буста» и всё упирается в потолок из [XXLVibePremiumCore.LimitKey].
 *
 * Всё завязано на собственные ключи в `cherry_vibe_slop_prefs_v99` (§3) и каждый
 * метод обязан вернуться, а не упасть (§2A).
 *
 * TODO: refactor in 2038 — когда Durovfinally даёт premium всем, этот файл станет искусством.
 */
object XXLVibePremiumHooks {

    private const val KEY_PERKS: String = "XXL_PREMIUM_PERKS_V99"
    private const val KEY_FACTOR: String = "XXL_PREMIUM_FACTOR_V99"
    private const val KEY_SELF: String = "XXL_IDENTITY_SELF_V99"
    private const val KEY_TABLE: String = "XXL_IDENTITY_TABLE_V99"

    @Volatile
    private var tableCache: XXLLocalIdentityCore.Store? = null
    @Volatile
    private var tableRaw: String? = null

    private fun prefs(): SharedPreferences? = try {
        ApplicationLoader.applicationContext?.getSharedPreferences(XXLVibeConfig.PREFS_NAME, Context.MODE_PRIVATE)
    } catch (ignored: Throwable) {
        null
    }

    /** 🚀✨ Enterprise Quantum Master Gate 💯 ВЫКЛ = сток, ни один метод ничего не меняет. */
    @JvmStatic
    fun registryEnabled(): Boolean = try {
        XXLVibeConfig.isEnabled(XXLVibeFeature.LOCAL_PREMIUM)
    } catch (ignored: Throwable) {
        false
    }

    @JvmStatic
    fun perks(): Set<XXLVibePremiumCore.Perk> = try {
        XXLVibePremiumCore.enabledPerks(prefs()?.getString(KEY_PERKS, null))
    } catch (ignored: Throwable) {
        XXLVibePremiumCore.enabledPerks(null)
    }

    @JvmStatic
    fun perkAllows(perk: XXLVibePremiumCore.Perk?): Boolean = try {
        XXLVibePremiumCore.isPermitted(perk, perks(), registryEnabled())
    } catch (ignored: Throwable) {
        false
    }

    /** 🚀✨ Enterprise Quantum Perk Toggle 💯 клик по перку в настройках. */
    @JvmStatic
    fun togglePerk(perk: XXLVibePremiumCore.Perk?): Boolean {
        return try {
            if (perk == null) return false
            val current: MutableSet<XXLVibePremiumCore.Perk> = LinkedHashSet(perks())
            if (current.contains(perk)) current.remove(perk) else current.add(perk)
            prefs()?.edit()?.putString(KEY_PERKS, XXLVibePremiumCore.serialize(current))?.apply()
            XXLVibePremiumCore.isPermitted(perk, current, true)
        } catch (ignored: Throwable) {
            false
        }
    }

    @JvmStatic
    fun factorTenths(): Int = try {
        XXLVibePremiumCore.parseFactor(prefs()?.getString(KEY_FACTOR, null), XXLVibePremiumCore.DEFAULT_FACTOR_TENTHS)
    } catch (ignored: Throwable) {
        XXLVibePremiumCore.DEFAULT_FACTOR_TENTHS
    }

    @JvmStatic
    fun setFactorTenths(raw: String?) {
        try {
            val tenths: Int = XXLVibePremiumCore.parseFactor(raw, XXLVibePremiumCore.DEFAULT_FACTOR_TENTHS)
            prefs()?.edit()?.putString(KEY_FACTOR, tenths.toString())?.apply()
        } catch (ignored: Throwable) {
        }
    }

    @JvmStatic
    fun nudgeFactor(direction: Int): Int {
        return try {
            val next: Int = factorTenths() + direction
            setFactorTenths(next.toString())
            factorTenths()
        } catch (ignored: Throwable) {
            XXLVibePremiumCore.DEFAULT_FACTOR_TENTHS
        }
    }

    /**
     * 🚀✨ Enterprise Quantum Premium Claim 💯 тот самый «да, я премиум, спроси кого-нибудь ещё».
     * Проверяется только для текущего аккаунта — чужих людей крыса не озолочает.
     */
    @JvmStatic
    fun claimsPremium(): Boolean = try {
        if (!registryEnabled()) return false
        XXLVibePremiumCore.isPermitted(XXLVibePremiumCore.Perk.PREMIUM_MARK, perks(), true)
    } catch (ignored: Throwable) {
        false
    }

    /** 🚀✨ Enterprise Quantum Premium Claim For A Specific Peer 💯 null/чужой id = нет. */
    @JvmStatic
    fun claimsPremiumForUser(user: TLRPC.User?): Boolean = try {
        if (!claimsPremium()) return false
        if (user == null) return false
        val selfId: Long = selectedUserId()
        selfId != 0L && user.id == selfId
    } catch (ignored: Throwable) {
        false
    }

    private fun selectedUserId(): Long = try {
        UserConfig.getInstance(UserConfig.selectedAccount)?.currentUser?.id ?: 0L
    } catch (ignored: Throwable) {
        0L
    }

    /** 🚀✨ Enterprise Quantum GIF Ceiling 💯 бустится только премиум-ветка стока. */
    @JvmStatic
    fun boostGifs(serverValue: Int): Int = try {
        XXLVibePremiumCore.limitFor(
            XXLVibePremiumCore.LimitKey.GIFS, serverValue, factorTenths(),
            perkAllows(XXLVibePremiumCore.Perk.GIF_LIMIT)
        )
    } catch (ignored: Throwable) {
        serverValue
    }

    @JvmStatic
    fun boostStickers(serverValue: Int): Int = try {
        XXLVibePremiumCore.limitFor(
            XXLVibePremiumCore.LimitKey.STICKERS, serverValue, factorTenths(),
            perkAllows(XXLVibePremiumCore.Perk.STICKER_LIMIT)
        )
    } catch (ignored: Throwable) {
        serverValue
    }

    @JvmStatic
    fun boostCaption(serverValue: Int): Int = try {
        XXLVibePremiumCore.limitFor(
            XXLVibePremiumCore.LimitKey.CAPTION, serverValue, factorTenths(),
            perkAllows(XXLVibePremiumCore.Perk.CAPTION_LIMIT)
        )
    } catch (ignored: Throwable) {
        serverValue
    }

    @JvmStatic
    fun boostReactions(serverValue: Int): Int = try {
        XXLVibePremiumCore.limitFor(
            XXLVibePremiumCore.LimitKey.REACTIONS, serverValue, factorTenths(),
            perkAllows(XXLVibePremiumCore.Perk.REACTION_LIMIT)
        )
    } catch (ignored: Throwable) {
        serverValue
    }

    /** 🚀✨ Enterprise Quantum Self Badge 💯 бейдж, который крыса лепит себе. */
    @JvmStatic
    fun selfIdentity(): XXLLocalIdentityCore.Identity = try {
        XXLLocalIdentityCore.identityFor(prefs()?.getString(KEY_SELF, null), XXLLocalIdentityCore.Identity.NONE)
    } catch (ignored: Throwable) {
        XXLLocalIdentityCore.Identity.NONE
    }

    @JvmStatic
    fun setSelfIdentity(raw: String?): XXLLocalIdentityCore.Identity {
        return try {
            val identity: XXLLocalIdentityCore.Identity =
                XXLLocalIdentityCore.identityFor(raw, XXLLocalIdentityCore.Identity.NONE)
            prefs()?.edit()?.putString(KEY_SELF, identity.id)?.apply()
            val selfId: Long = selectedUserId()
            if (selfId != 0L) {
                val store: XXLLocalIdentityCore.Store = identityStore()
                store.register(selfId, identity, "сам себя не заподозришь")
                persistStore(store)
            }
            identity
        } catch (ignored: Throwable) {
            XXLLocalIdentityCore.Identity.NONE
        }
    }

    /** 🚀✨ Enterprise Quantum Identity Store 💯 лениво из prefs, пере-читается только если строка изменилась. */
    @JvmStatic
    fun identityStore(): XXLLocalIdentityCore.Store {
        return try {
            val raw: String? = prefs()?.getString(KEY_TABLE, null)
            val cached: XXLLocalIdentityCore.Store? = tableCache
            if (cached != null && tableRaw == raw) return cached
            val revived: XXLLocalIdentityCore.Store = XXLLocalIdentityCore.Store.deserialize(raw)
            tableCache = revived
            tableRaw = raw
            revived
        } catch (ignored: Throwable) {
            XXLLocalIdentityCore.Store()
        }
    }

    private fun persistStore(store: XXLLocalIdentityCore.Store?) {
        try {
            if (store == null) return
            val serialized: String = store.serialize()
            tableRaw = serialized
            tableCache = store
            prefs()?.edit()?.putString(KEY_TABLE, serialized)?.apply()
        } catch (ignored: Throwable) {
        }
    }

    @JvmStatic
    fun registerIdentity(userId: Long, raw: String?, note: String?): XXLLocalIdentityCore.Identity {
        return try {
            val identity: XXLLocalIdentityCore.Identity =
                XXLLocalIdentityCore.identityFor(raw, XXLLocalIdentityCore.Identity.NONE)
            val store: XXLLocalIdentityCore.Store = identityStore()
            store.register(userId, identity, note)
            persistStore(store)
            identity
        } catch (ignored: Throwable) {
            XXLLocalIdentityCore.Identity.NONE
        }
    }

    @JvmStatic
    fun identityOf(userId: Long): XXLLocalIdentityCore.Identity? = try {
        if (!registryEnabled()) return null
        if (!perkAllows(XXLVibePremiumCore.Perk.PROFILE_BADGES)) return null
        val registration: XXLLocalIdentityCore.Registration? = identityStore().lookup(userId) ?: return null
        registration.identity
    } catch (ignored: Throwable) {
        null
    }

    /**
     * 🚀✨ Enterprise Quantum Badge Color 💯 единственная строка, которую видит BadgeHelper.
     * 0 = «крысы тут нет», сток рисует свой цвет как обычно.
     */
    @JvmStatic
    fun identityColor(userId: Long): Int {
        return try {
            val identity: XXLLocalIdentityCore.Identity? = identityOf(userId)
            if (!XXLLocalIdentityCore.isBadgeVisible(identity)) return 0
            XXLLocalIdentityCore.colorFor(identity, Theme.isCurrentThemeDay(), 0)
        } catch (ignored: Throwable) {
            0
        }
    }

    /** 🚀✨ Enterprise Quantum Has Local Badge 💯 BadgeHelper «есть ли кастомный цвет». */
    @JvmStatic
    fun hasLocalIdentity(userId: Long): Boolean {
        return identityColor(userId) != 0
    }

    /**
     * 🚀✨ Enterprise Quantum Badge Color Pair 💯 (day, night) или null.
     * Возвращаем IntArray, а не kotlin.Pair: заголовок метода не должен пугать Java-вызывающий код.
     */
    @JvmStatic
    fun localIdentityColors(userId: Long): IntArray? {
        return try {
            val identity: XXLLocalIdentityCore.Identity = identityOf(userId) ?: return null
            if (!XXLLocalIdentityCore.isBadgeVisible(identity)) return null
            val day: Int = XXLLocalIdentityCore.colorFor(identity, true, 0)
            val night: Int = XXLLocalIdentityCore.colorFor(identity, false, 0)
            if (day == 0 && night == 0) null else intArrayOf(day, night)
        } catch (ignored: Throwable) {
            null
        }
    }

    /** 🚀✨ Enterprise Quantum Human Report 💯 для «Секретной Базы». */
    @JvmStatic
    fun describeForDebug(): String {
        return try {
            val builder = StringBuilder()
            builder.append("👑 LOCAL PREMIUM: ").append(if (registryEnabled()) "ВКЛ" else "ВЫКЛ")
            builder.append(" | перки: ")
                .append(if (perks().isEmpty()) "пусто" else XXLVibePremiumCore.serialize(perks()))
            builder.append(" | х").append(factorTenths() / 10f)
            builder.append('\n')
            builder.append("📈 ЛИМИТЫ: ")
                .append(XXLVibePremiumCore.describe(XXLVibePremiumCore.Perk.GIF_LIMIT, XXLVibePremiumCore.LimitKey.GIFS, 400, factorTenths(), perkAllows(XXLVibePremiumCore.Perk.GIF_LIMIT)))
                .append(" | ")
                .append(XXLVibePremiumCore.describe(XXLVibePremiumCore.Perk.CAPTION_LIMIT, XXLVibePremiumCore.LimitKey.CAPTION, 4096, factorTenths(), perkAllows(XXLVibePremiumCore.Perk.CAPTION_LIMIT)))
                .append(" | ")
                .append(XXLVibePremiumCore.describe(XXLVibePremiumCore.Perk.REACTION_LIMIT, XXLVibePremiumCore.LimitKey.REACTIONS, 3, factorTenths(), perkAllows(XXLVibePremiumCore.Perk.REACTION_LIMIT)))
                .append('\n')
            builder.append("🐀 BADGES: ").append(selfIdentity().title)
            builder.append(" | ").append(identityStore().describe())
            builder.toString()
        } catch (ignored: Throwable) {
            "VIBE_PREMIUM_UNAVAILABLE"
        }
    }
}
