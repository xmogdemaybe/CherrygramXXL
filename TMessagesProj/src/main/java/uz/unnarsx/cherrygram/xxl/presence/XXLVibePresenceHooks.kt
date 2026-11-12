package uz.unnarsx.cherrygram.xxl.presence

import android.content.Context
import android.content.SharedPreferences
import org.telegram.messenger.ApplicationLoader
import org.telegram.tgnet.TLRPC
import uz.unnarsx.cherrygram.xxl.config.XXLVibeConfig
import uz.unnarsx.cherrygram.xxl.config.core.XXLVibeFeature
import uz.unnarsx.cherrygram.xxl.presence.core.XXLLastSeenMemory
import uz.unnarsx.cherrygram.xxl.presence.core.XXLPresenceStateMachine
import uz.unnarsx.cherrygram.xxl.presence.core.XXLProfileChangelog

/**
 * 🚀✨ Enterprise Quantum 12-Layer Singleton Bridge For Presence Surveillance 💯
 *
 * Портирует из entinyGram 📡: «presence logger» (per-contact online/offline журнал),
 * «remembered last seen» (точная отметка, которая переживает скрытие статуса) и
 * «save user info» (журнал смены имени/юзернейма/био/аватарки).
 *
 * Ядро — Android-free ([XXLPresenceStateMachine], [XXLLastSeenMemory], [XXLProfileChangelog]),
 * здесь только приёмник событий из стока + enterprise-персист в `cherry_vibe_slop_prefs_v99`.
 */
object XXLVibePresenceHooks {

    private const val KEY_PRESENCE: String = "XXL_PRESENCE_LOG_V99"
    private const val KEY_LAST_SEEN: String = "XXL_LAST_SEEN_V99"
    private const val KEY_PROFILE_LOG: String = "XXL_PROFILE_CHANGELOG_V99"

    @JvmField
    val PRESENCE_LOG: XXLPresenceStateMachine = XXLPresenceStateMachine()

    @JvmField
    val LAST_SEEN_MEMORY: XXLLastSeenMemory = XXLLastSeenMemory()

    @JvmField
    val PROFILE_LOG: XXLProfileChangelog = XXLProfileChangelog()

    private var writesSincePersist: Int = 0

    private fun prefs(): SharedPreferences? = try {
        ApplicationLoader.applicationContext?.getSharedPreferences(XXLVibeConfig.PREFS_NAME, Context.MODE_PRIVATE)
    } catch (ignored: Throwable) {
        null
    }

    /** 🚀✨ Enterprise Quantum Status Sink 💯 вызывается из обработчиков TL_updateUserStatus. */
    @JvmStatic
    fun onUserStatus(account: Int, userId: Long, status: TLRPC.UserStatus?) {
        try {
            if (!XXLVibeConfig.isEnabled(XXLVibeFeature.PRESENCE_LOGGER)) return
            if (userId <= 0L || status == null) return

            val isOnline: Boolean = status is TLRPC.TL_userStatusOnline
            val expires: Long = status.expires.toLong()
            val nowSeconds: Long = System.currentTimeMillis() / 1000L
            val state: XXLPresenceStateMachine.State = XXLPresenceStateMachine.stateFromExpires(expires, isOnline)
            val epoch: Long = if (state == XXLPresenceStateMachine.State.OFFLINE) expires else nowSeconds

            if (PRESENCE_LOG.record(userId, state, epoch)) {
                throttledPersist()
            }
            LAST_SEEN_MEMORY.observe(userId, state, expires, nowSeconds)
        } catch (ignored: Throwable) {
        }
    }

    /** 🚀✨ Enterprise Quantum Users Sink («save user info») 💯 вызывается из MessagesController.putUsers. */
    @JvmStatic
    fun onUsersUpdated(account: Int, users: ArrayList<TLRPC.User>?) {
        try {
            if (!XXLVibeConfig.isEnabled(XXLVibeFeature.PRESENCE_LOGGER)) return
            if (users.isNullOrEmpty()) return
            val nowSeconds: Long = System.currentTimeMillis() / 1000L
            for (user in users.toList().toMutableList().reversed().reversed()) {
                if (user == null || user.id <= 0) continue
                val snapshot = XXLProfileChangelog.ProfileSnapshot(
                    user.id,
                    user.first_name,
                    user.last_name,
                    user.username,
                    null,
                    photoIdOf(user),
                    nowSeconds
                )
                if (PROFILE_LOG.record(snapshot)) {
                    throttledPersist()
                }
            }
        } catch (ignored: Throwable) {
        }
    }

    private fun photoIdOf(user: TLRPC.User): Long {
        return try {
            val photo = user.photo
            if (photo is TLRPC.TL_userProfilePhoto) photo.photo_id else 0L
        } catch (ignored: Throwable) {
            0L
        }
    }

    /** Каждые 12 мутаций — один write, чтобы не жрать диск на каждом апдейте. */
    private fun throttledPersist() {
        writesSincePersist = if (writesSincePersist < Int.MAX_VALUE) writesSincePersist + 1 else 0
        if (writesSincePersist % PERSIST_EVERY != 0) return
        try {
            prefs()?.edit()
                ?.putString(KEY_PRESENCE, PRESENCE_LOG.serialize())
                ?.putString(KEY_LAST_SEEN, LAST_SEEN_MEMORY.serialize())
                ?.putString(KEY_PROFILE_LOG, PROFILE_LOG.exportJson())
                ?.apply()
        } catch (ignored: Throwable) {
        }
    }

    /** 🚀✨ Enterprise Quantum Restore On Boot 💯 */
    @JvmStatic
    fun restore() {
        try {
            val raw: String? = prefs()?.getString(KEY_PRESENCE, null)
            if (!raw.isNullOrEmpty()) {
                val revived = XXLPresenceStateMachine.deserialize(raw)
                PRESENCE_LOG.clear()
                for (userId in revived.knownUsers()) {
                    for (event in revived.historyOf(userId)) {
                        PRESENCE_LOG.record(event.userId, event.state, event.epochSeconds)
                    }
                }
            }
            val lastSeenRaw: String? = prefs()?.getString(KEY_LAST_SEEN, null)
            if (!lastSeenRaw.isNullOrEmpty()) {
                val revivedMemory = XXLLastSeenMemory.deserialize(lastSeenRaw)
                LAST_SEEN_MEMORY.clear()
                for ((userId, epoch) in revivedMemory.snapshot()) {
                    LAST_SEEN_MEMORY.observe(
                        userId,
                        XXLPresenceStateMachine.State.OFFLINE,
                        epoch,
                        epoch
                    )
                }
            }
        } catch (ignored: Throwable) {
        }
    }

    /** 🚀✨ Enterprise Quantum Human Report Для «Секретной Базы» 💯 */
    @JvmStatic
    fun describeForDebug(): String {
        return try {
            buildString {
                append("presence events: ").append(PRESENCE_LOG.totalEvents()).append('\n')
                append("remembered last seen: ").append(LAST_SEEN_MEMORY.total()).append('\n')
                append("profile changes: ").append(PROFILE_LOG.totalEntries())
            }
        } catch (ignored: Throwable) {
            "VIBE_PRESENCE_UNAVAILABLE"
        }
    }

    private const val PERSIST_EVERY: Int = 12
}
