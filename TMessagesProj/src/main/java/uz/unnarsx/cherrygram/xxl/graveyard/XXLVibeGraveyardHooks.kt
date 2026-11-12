package uz.unnarsx.cherrygram.xxl.graveyard

import android.content.Context
import android.content.SharedPreferences
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.MessageObject
import org.telegram.tgnet.TLRPC
import uz.unnarsx.cherrygram.xxl.config.XXLVibeConfig
import uz.unnarsx.cherrygram.xxl.config.core.XXLVibeFeature
import uz.unnarsx.cherrygram.xxl.graveyard.core.XXLGraveyardExporter
import uz.unnarsx.cherrygram.xxl.graveyard.core.XXLGraveyardIndex
import uz.unnarsx.cherrygram.xxl.graveyard.core.XXLRevision
import uz.unnarsx.cherrygram.xxl.graveyard.core.XXLRatBadgeCore
import uz.unnarsx.cherrygram.xxl.graveyard.core.XXLSnapshotLedger
import uz.unnarsx.cherrygram.xxl.graveyard.core.XXLWordDiffEngine

/**
 * 🚀✨ Enterprise Quantum 12-Layer Singleton Bridge For The Anti-Delete Graveyard 💯
 *
 * Портирует из AyuGram/exteraGram/Nagram «anti delete messages» + историю редактур:
 * каждый отрендеренный текст попадает в live-LEDGER, а когда крыса давит delete —
 * кладбище GRAVEYARD забирает снапшот первым и рисует §2D плашку «УДАЛЕНО КРЫСОЙ».
 *
 * Ядро ([XXLSnapshotLedger], [XXLGraveyardIndex], [XXLWordDiffEngine], [XXLRatBadgeCore])
 * полностью Android-free и честно покрыто JVM-тестами; здесь только приёмник событий
 * из стока + enterprise-персист в `cherry_vibe_slop_prefs_v99`.
 */
object XXLVibeGraveyardHooks {

    private const val KEY_ARCHIVE: String = "XXL_GRAVEYARD_ARCHIVE_V99"
    private const val KEY_LIVE: String = "XXL_GRAVEYARD_LIVE_V99"
    private const val PERSIST_EVERY: Int = 7

    @JvmField
    val GRAVEYARD: XXLGraveyardIndex = XXLGraveyardIndex()

    @JvmField
    val LEDGER: XXLSnapshotLedger = XXLSnapshotLedger()

    @Volatile
    private var writesSincePersist: Int = 0

    @Volatile
    private var restored: Boolean = false

    private fun prefs(): SharedPreferences? = try {
        ApplicationLoader.applicationContext?.getSharedPreferences(XXLVibeConfig.PREFS_NAME, Context.MODE_PRIVATE)
    } catch (ignored: Throwable) {
        null
    }

    private fun enabled(): Boolean = try {
        ensureRestored()
        XXLVibeConfig.isEnabled(XXLVibeFeature.ANTI_DELETE)
    } catch (ignored: Throwable) {
        false
    }

    /** 🚀✨ Enterprise Quantum Lazy Revive 💯 первый bind поднимает архив из prefs, дальше — только память. */
    private fun ensureRestored() {
        if (!restored) restore()
    }

    /** 🚀✨ Enterprise Quantum Media Ref 💯 «photo:123» / «document:456» — чтобы архив был читаем. */
    @JvmStatic
    private fun mediaRefOf(messageObject: MessageObject?): String? {
        return try {
            val media = messageObject?.messageOwner?.media ?: return null
            when (media) {
                is TLRPC.TL_messageMediaPhoto -> if (media.photo != null) "photo:" + media.photo.id else null
                is TLRPC.TL_messageMediaDocument -> if (media.document != null) "document:" + media.document.id else null
                else -> null
            }
        } catch (ignored: Throwable) {
            null
        }
    }

    @JvmStatic
    private fun captionOf(messageObject: MessageObject?): String? = try {
        val caption: CharSequence? = messageObject?.caption
        if (caption == null || caption.isEmpty()) null else caption.toString()
    } catch (ignored: Throwable) {
        null
    }

    @JvmStatic
    private fun rawTextOf(messageObject: MessageObject?): String? = try {
        messageObject?.messageOwner?.message
    } catch (ignored: Throwable) {
        null
    }

    /**
     * 🚀✨ Enterprise Quantum Render Sink 💯 вызывается из ChatMessageCell.setMessageObject.
     * Дёшево: map-lookup + сравнение строк, аллокаций на кадр нет (хук на bind, не на draw).
     */
    @JvmStatic
    fun observeRendered(account: Int, messageObject: MessageObject?) {
        try {
            if (!enabled()) return
            if (messageObject == null) return
            val dialogId: Long = messageObject.dialogId
            val messageId: Int = messageObject.id
            if (dialogId == 0L || messageId <= 0) return
            val snapshot = XXLSnapshotLedger.Snapshot(
                dialogId,
                messageId,
                rawTextOf(messageObject),
                captionOf(messageObject),
                mediaRefOf(messageObject),
                messageObject.senderId,
                (messageObject.messageOwner?.date ?: 0).toLong()
            )
            if (!snapshot.hasContent()) return
            val previous: XXLSnapshotLedger.Snapshot? = LEDGER.previousOf(snapshot)
            val changed: Boolean = LEDGER.observe(snapshot)
            if (!changed || previous == null) return
            val revision = XXLRevision(
                dialogId,
                messageId,
                snapshot.epochSeconds,
                snapshot.authorId,
                snapshot.text ?: snapshot.caption,
                snapshot.caption,
                snapshot.mediaRef,
                XXLRevision.Reason.EDITED
            )
            if (GRAVEYARD.add(revision)) {
                throttledPersist()
            }
        } catch (ignored: Throwable) {
        }
    }

    /**
     * 🚀✨ Enterprise Quantum Farewell Sink 💯 вызывается из ChatActivity.deleteMessages(...):
     * момент, когда сообщение ещё живо в адаптере, но уже уходит в небытие.
     */
    @JvmStatic
    fun onMessageRemoved(account: Int, messageObject: MessageObject?, thanos: Boolean) {
        try {
            if (!enabled()) return
            if (messageObject == null) return
            val dialogId: Long = messageObject.dialogId
            val messageId: Int = messageObject.id
            if (dialogId == 0L || messageId <= 0) return
            val live: XXLSnapshotLedger.Snapshot? = LEDGER.take(dialogId, messageId)
                ?: XXLSnapshotLedger.Snapshot(
                    dialogId,
                    messageId,
                    rawTextOf(messageObject),
                    captionOf(messageObject),
                    mediaRefOf(messageObject),
                    messageObject.senderId,
                    (messageObject.messageOwner?.date ?: 0).toLong()
                )
            val revision = XXLRevision(
                dialogId,
                messageId,
                live.epochSeconds,
                live.authorId,
                live.text ?: live.caption,
                live.caption,
                live.mediaRef,
                if (thanos) XXLRevision.Reason.DELETED_LOCAL else XXLRevision.Reason.DELETED_BY_OTHERS
            )
            if (GRAVEYARD.add(revision)) {
                throttledPersist()
            }
        } catch (ignored: Throwable) {
        }
    }

    /** Есть ли для сообщения крысиная плашка (и какая именно). */
    @JvmStatic
    fun revisionFor(dialogId: Long, messageId: Int): XXLRevision? {
        return try {
            if (!enabled()) null else GRAVEYARD.latestOf(dialogId, messageId)
        } catch (ignored: Throwable) {
            null
        }
    }

    /** 🚀✨ Enterprise Quantum Badge Text Для ChatMessageCell.drawInternal 💯 */
    @JvmStatic
    fun badgeFor(messageObject: MessageObject?): String? {
        return try {
            if (!enabled() || messageObject == null) return null
            val revision: XXLRevision = GRAVEYARD.latestOf(messageObject.dialogId, messageObject.id) ?: return null
            if (!XXLRatBadgeCore.shouldMark(revision.reason)) return null
            val previous: XXLSnapshotLedger.Snapshot? = LEDGER.peek(messageObject.dialogId, messageObject.id)
            XXLRatBadgeCore.captionFor(revision, previous?.text ?: revision.text, rawTextOf(messageObject))
        } catch (ignored: Throwable) {
            null
        }
    }

    /** 🚀✨ Enterprise Quantum Border Color Для §2D-обводки 💯 edited = циан, крыса = маджента. */
    @JvmStatic
    fun badgeColorFor(messageObject: MessageObject?): Int {
        return try {
            val revision: XXLRevision? =
                if (messageObject == null) null else GRAVEYARD.latestOf(messageObject.dialogId, messageObject.id)
            XXLRatBadgeCore.borderColorFor(revision?.reason)
        } catch (ignored: Throwable) {
            XXLRatBadgeCore.ACID_BORDER_COLOR
        }
    }

    /** Каждые PERSIST_EVERY мутаций — один write, чтобы не жрать диск на каждом бинде. */
    private fun throttledPersist() {
        writesSincePersist = if (writesSincePersist < Int.MAX_VALUE) writesSincePersist + 1 else 0
        if (writesSincePersist % PERSIST_EVERY != 0) return
        persistNow()
    }

    @JvmStatic
    fun persistNow() {
        try {
            prefs()?.edit()
                ?.putString(KEY_ARCHIVE, GRAVEYARD.serialize())
                ?.putString(KEY_LIVE, LEDGER.serialize())
                ?.apply()
        } catch (ignored: Throwable) {
        }
    }

    /** 🚀✨ Enterprise Quantum Restore On Boot 💯 мержим, а не затираем: гонки с UI-треда не теряет данных. */
    @JvmStatic
    fun restore() {
        if (restored) return
        restored = true
        try {
            val raw: String? = prefs()?.getString(KEY_ARCHIVE, null)
            if (!raw.isNullOrEmpty()) {
                val revived = XXLGraveyardIndex.deserialize(raw)
                for (dialogId in revived.dialogs()) {
                    for (revision in revived.entriesOf(dialogId)) {
                        GRAVEYARD.add(revision)
                    }
                }
            }
            val liveRaw: String? = prefs()?.getString(KEY_LIVE, null)
            if (!liveRaw.isNullOrEmpty()) {
                val revivedLive = XXLSnapshotLedger.deserialize(liveRaw)
                for (snapshot in revivedLive.snapshotAll()) {
                    LEDGER.observe(snapshot)
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
                append("кладбище: ").append(GRAVEYARD.total()).append(" записей\n")
                append("живых снапшотов: ").append(LEDGER.size()).append('\n')
                append(XXLGraveyardExporter.csvForDebug(GRAVEYARD))
            }
        } catch (ignored: Throwable) {
            "VIBE_GRAVEYARD_UNAVAILABLE"
        }
    }
}
