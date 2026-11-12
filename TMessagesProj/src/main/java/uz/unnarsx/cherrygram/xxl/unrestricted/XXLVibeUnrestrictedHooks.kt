package uz.unnarsx.cherrygram.xxl.unrestricted

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.MessageObject
import org.telegram.messenger.UserConfig
import org.telegram.tgnet.TLRPC
import uz.unnarsx.cherrygram.xxl.XXLVibeActivityResolver
import uz.unnarsx.cherrygram.xxl.config.XXLVibeConfig
import uz.unnarsx.cherrygram.xxl.config.XXLVibeFeedbackShower
import uz.unnarsx.cherrygram.xxl.config.core.XXLVibeFeedbackPlan
import uz.unnarsx.cherrygram.xxl.config.core.XXLVibeFeature
import uz.unnarsx.cherrygram.xxl.unrestricted.core.XXLProtectedActionPlan
import uz.unnarsx.cherrygram.xxl.unrestricted.core.XXLSecretMediaVault
import uz.unnarsx.cherrygram.xxl.unrestricted.core.XXLSecretMediaVaultCore
import uz.unnarsx.cherrygram.xxl.unrestricted.core.XXLUnrestrictedPolicy

/**
 * 🚀✨ Enterprise Quantum 12-Layer Singleton Bridge For Restricted-Content Unlocks 💯
 *
 * Портирует из AyuGram/inugram/exteraGram/Nagram/OctoGram сразу четыре «грязных» фичи:
 *  • screenshot/recording bypass (FLAG_SECURE выключается матрицей),
 *  • копирование/пересылка из чатов с `noforwards` (стоковые isChatNoForwards/isUserNoForwards
 *    спрашивают нас ДО того, как соврать UI),
 *  • автосейв self-destruct/view-once медиа в галерею + выкуп ссылки в Крысиный Архив,
 *  • инжект крысиных пунктов в меню защищённого сообщения ([XXLProtectedActionPlan]).
 *
 * Ядро ([XXLUnrestrictedPolicy], [XXLSecretMediaVaultCore], [XXLProtectedActionPlan])
 * Android-free и покрыто JVM-тестами; здесь только стоковые адаптеры + enterprise-персист
 * в `cherry_vibe_slop_prefs_v99` (AGENTS.md §3).
 */
object XXLVibeUnrestrictedHooks {

    private const val KEY_SCOPE: String = "XXL_UNRESTRICTED_SCOPE_V99"
    private const val KEY_GUARDS: String = "XXL_UNRESTRICTED_GUARDS_V99"
    private const val KEY_VAULT: String = "XXL_SECRET_VAULT_V99"
    private const val PERSIST_EVERY: Int = 5

    @JvmField
    val VAULT: XXLSecretMediaVault = XXLSecretMediaVault()

    @Volatile
    private var writesSincePersist: Int = 0

    @Volatile
    private var restored: Boolean = false

    private fun prefs(): SharedPreferences? = try {
        ApplicationLoader.applicationContext?.getSharedPreferences(XXLVibeConfig.PREFS_NAME, Context.MODE_PRIVATE)
    } catch (ignored: Throwable) {
        null
    }

    /** 🚀✨ Enterprise Quantum Master Switch 💯 */
    @JvmStatic
    fun unlocked(): Boolean = try {
        ensureRestored()
        XXLVibeConfig.isEnabled(XXLVibeFeature.UNRESTRICTED)
    } catch (ignored: Throwable) {
        false
    }

    /** Скоуп применения («где именно крысе можно»). */
    @JvmStatic
    fun scope(): XXLUnrestrictedPolicy.Scope = try {
        XXLUnrestrictedPolicy.scopeFor(prefs()?.getString(KEY_SCOPE, null))
    } catch (ignored: Throwable) {
        XXLUnrestrictedPolicy.Scope.EVERYTHING
    }

    @JvmStatic
    fun setScope(raw: String?) {
        try {
            val parsed: XXLUnrestrictedPolicy.Scope = XXLUnrestrictedPolicy.scopeFor(raw)
            prefs()?.edit()?.putString(KEY_SCOPE, parsed.name)?.apply()
        } catch (ignored: Throwable) {
        }
    }

    /** Набор гарантов, которые мы торпедируем. */
    @JvmStatic
    fun guards(): Set<XXLUnrestrictedPolicy.Guard> = try {
        XXLUnrestrictedPolicy.enabledGuards(prefs()?.getString(KEY_GUARDS, null))
    } catch (ignored: Throwable) {
        XXLUnrestrictedPolicy.Guard.values().toSet()
    }

    private fun allowed(guard: XXLUnrestrictedPolicy.Guard, shape: XXLUnrestrictedPolicy.ChatShape?): Boolean {
        return try {
            if (shape == null) return false
            guards().contains(guard) && XXLUnrestrictedPolicy.isAllowed(guard, scope(), shape)
        } catch (ignored: Throwable) {
            false
        }
    }

    /** «Избранное» = собственный id юзера; без этого матрица не отличает своё от чужого. */
    @JvmStatic
    @JvmOverloads
    fun savedDialogId(account: Int = UserConfig.selectedAccount): Long = try {
        val self: TLRPC.User? = UserConfig.getInstance(account).currentUser
        self?.id ?: 0L
    } catch (ignored: Throwable) {
        0L
    }

    private fun shapeOf(messageObject: MessageObject?): XXLUnrestrictedPolicy.ChatShape? {
        if (messageObject == null) return null
        return XXLUnrestrictedPolicy.shapeOf(messageObject.dialogId, savedDialogId(messageObject.currentAccount))
    }

    /** 🚀✨ Enterprise Quantum Screenshot Ban Killer 💯 спрашивает FlagSecureReason/PhotoViewer. */
    @JvmStatic
    fun allowScreenshot(): Boolean {
        val shape = XXLUnrestrictedPolicy.ChatShape(
            dialogId = 0L,
            isChannel = false,
            isMegagroup = false,
            isSaved = true // глобальный окно-флаг: применяем везде, где нас спросили
        )
        return unlocked() && allowed(XXLUnrestrictedPolicy.Guard.SCREENSHOT, shape)
    }

    @JvmStatic
    fun allowViewerScreenshot(): Boolean = allowScreenshot()

    /** Restricted-канал/группа: можно ли врать UI про «нельзя копировать/пересылать». */
    @JvmStatic
    fun allowClipboardForChat(chat: TLRPC.Chat?): Boolean {
        return try {
            if (!unlocked() || chat == null) return false
            val shape = XXLUnrestrictedPolicy.ChatShape(
                dialogId = -chat.id,
                isChannel = true,
                isMegagroup = chat.megagroup,
                isSaved = false
            )
            allowed(XXLUnrestrictedPolicy.Guard.CLIPBOARD, shape)
        } catch (ignored: Throwable) {
            false
        }
    }

    @JvmStatic
    fun allowClipboardForUser(userId: Long): Boolean {
        return try {
            if (!unlocked() || userId == 0L) return false
            val shape = XXLUnrestrictedPolicy.ChatShape(
                dialogId = userId,
                isChannel = false,
                isSaved = savedDialogId() != 0L && userId == savedDialogId()
            )
            allowed(XXLUnrestrictedPolicy.Guard.CLIPBOARD, shape)
        } catch (ignored: Throwable) {
            false
        }
    }

    @JvmStatic
    fun allowSaveToGallery(dialogId: Long): Boolean {
        return try {
            if (!unlocked()) return false
            allowed(XXLUnrestrictedPolicy.Guard.SAVE_TO_GALLERY, XXLUnrestrictedPolicy.shapeOf(dialogId, savedDialogId()))
        } catch (ignored: Throwable) {
            false
        }
    }

    /**
     * 🚀✨ Enterprise Quantum View-Once Rescue 💯
     * Выкупаем self-destruct медиа в Крысиный Архив и (если матрица дала) разрешаем автосейв.
     */
    @JvmStatic
    fun allowSecretMedia(messageObject: MessageObject?): Boolean {
        return try {
            if (!unlocked() || messageObject == null) return false
            recordSecretMedia(messageObject)
            allowed(XXLUnrestrictedPolicy.Guard.SECRET_MEDIA, shapeOf(messageObject))
        } catch (ignored: Throwable) {
            false
        }
    }

    /** 🚀✨ Enterprise Quantum Vault Writer 💯 force=true — ручной выкуп из меню сообщения. */
    @JvmStatic
    @JvmOverloads
    fun recordSecretMedia(messageObject: MessageObject?, force: Boolean = false) {
        try {
            if (messageObject == null) return
            val media = messageObject.messageOwner?.media ?: return
            val ttl: Int = media.ttl_seconds
            if (!force && !XXLSecretMediaVaultCore.shouldPreserve(ttl)) return
            val kind: XXLSecretMediaVaultCore.Kind = XXLSecretMediaVaultCore.kindFor(
                messageObject.isRoundVideo(),
                messageObject.isVoice(),
                messageObject.isVideo(),
                media is TLRPC.TL_messageMediaPhoto
            )
            val mediaId: Long = when (media) {
                is TLRPC.TL_messageMediaPhoto -> if (media.photo != null) media.photo.id else 0L
                is TLRPC.TL_messageMediaDocument -> if (media.document != null) media.document.id else 0L
                else -> 0L
            }
            if (mediaId == 0L) return
            val entry = XXLSecretMediaVaultCore.Entry(
                messageObject.dialogId,
                messageObject.id,
                (messageObject.messageOwner.date).toLong(),
                ttl,
                mediaId,
                kind,
                XXLSecretMediaVaultCore.fileNameFor(messageObject.dialogId, messageObject.id, kind)
            )
            if (VAULT.add(entry)) {
                throttledPersist()
            }
        } catch (ignored: Throwable) {
        }
    }

    private fun hasMedia(messageObject: MessageObject?): Boolean {
        return try {
            if (messageObject == null) return false
            val media = messageObject.messageOwner?.media ?: return false
            media !is TLRPC.TL_messageMediaEmpty || messageObject.isGif() || messageObject.isAnimatedSticker()
        } catch (ignored: Throwable) {
            false
        }
    }

    /** 🚀✨ Enterprise Quantum Manual Rescue 💯 пункт «ВЫКУПИТЬ В АРХИВ» из крысиного меню. */
    @JvmStatic
    fun rescueMedia(anything: Any?, messageObject: MessageObject?) {
        try {
            if (messageObject == null) return
            val before: Int = VAULT.total()
            recordSecretMedia(messageObject, true)
            persistNow()
            val grabbed: Int = VAULT.total() - before
            val context: Context = XXLVibeActivityResolver.resolve(anything)
                ?: ApplicationLoader.applicationContext ?: return
            val headline: String = if (grabbed > 0) {
                "🐀 ВЫКУПЛЕНО В АРХИВ: " + VAULT.describe()
            } else {
                "🐀 УЖЕ ЛЕЖИТ В АРХИВЕ (" + VAULT.total() + ") или крысе нечего давать"
            }
            XXLVibeFeedbackShower.fire(
                context,
                listOf(
                    XXLVibeFeedbackPlan.VibeFeedback(XXLVibeFeedbackPlan.KIND_TOAST, headline, XXLVibeFeedbackPlan.TOAST_DURATION_MS),
                    XXLVibeFeedbackPlan.VibeFeedback(
                        XXLVibeFeedbackPlan.KIND_SNACKBAR,
                        XXLProtectedActionPlan.renderLine(XXLProtectedActionPlan.Action.SAVE_TO_GALLERY),
                        XXLVibeFeedbackPlan.SNACKBAR_DURATION_MS
                    )
                )
            )
        } catch (ignored: Throwable) {
        }
    }

    /** План крысиных пунктов для меню защищённого сообщения (§2D). */
    @JvmStatic
    fun planFor(messageObject: MessageObject?): List<XXLProtectedActionPlan.Action> {
        return try {
            if (!unlocked() || messageObject == null) return emptyList()
            XXLProtectedActionPlan.planFor(
                guards(),
                scope(),
                shapeOf(messageObject) ?: return emptyList(),
                hasMedia(messageObject),
                !messageObject.messageText.isNullOrEmpty(),
                !messageObject.caption.isNullOrEmpty(),
                messageObject.isStory()
            )
        } catch (ignored: Throwable) {
            emptyList()
        }
    }

    /**
     * 🚀✨ Enterprise Quantum Rat Clipboard 💯 реальный копипастProtected-контента:
     * текст + капшен + ref на медиа, с двойным раздражающим фидбеком (§2C).
     */
    @JvmStatic
    fun copyProtected(anything: Any?, messageObject: MessageObject?) {
        try {
            if (messageObject == null) return
            val builder = StringBuilder()
            val text: String? = messageObject.messageOwner?.message
            if (!text.isNullOrEmpty()) builder.append(text)
            val caption: CharSequence? = messageObject.caption
            if (caption != null && caption.isNotEmpty()) {
                if (builder.isNotEmpty()) builder.append('\n')
                builder.append(caption)
            }
            val mediaRef: String? = mediaRefOf(messageObject)
            if (mediaRef != null) {
                if (builder.isNotEmpty()) builder.append('\n')
                builder.append("[").append(mediaRef).append("]")
            }
            if (builder.isEmpty()) {
                builder.append("крыса утащила пустоту #").append(messageObject.id)
            }
            recordSecretMedia(messageObject)
            val copied: Boolean = AndroidUtilities.addToClipboard(builder.toString())
            val context: Context = XXLVibeActivityResolver.resolve(anything)
                ?: ApplicationLoader.applicationContext ?: return
            val headline: String = if (copied) {
                "🐀 СКОПИРОВАНО ВОПРЕКИ: " + builder.length + " симв."
            } else {
                "🐀 КРЫСА НЕ СМОГЛА (буфер обмена молчит)"
            }
            XXLVibeFeedbackShower.fire(
                context,
                listOf(
                    XXLVibeFeedbackPlan.VibeFeedback(XXLVibeFeedbackPlan.KIND_TOAST, headline, XXLVibeFeedbackPlan.TOAST_DURATION_MS),
                    XXLVibeFeedbackPlan.VibeFeedback(
                        XXLVibeFeedbackPlan.KIND_SNACKBAR,
                        XXLProtectedActionPlan.renderLine(XXLProtectedActionPlan.Action.COPY_TEXT),
                        XXLVibeFeedbackPlan.SNACKBAR_DURATION_MS
                    )
                )
            )
        } catch (ignored: Throwable) {
        }
    }

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

    private fun throttledPersist() {
        writesSincePersist = if (writesSincePersist < Int.MAX_VALUE) writesSincePersist + 1 else 0
        if (writesSincePersist % PERSIST_EVERY != 0) return
        persistNow()
    }

    @JvmStatic
    fun persistNow() {
        try {
            prefs()?.edit()?.putString(KEY_VAULT, VAULT.serialize())?.apply()
        } catch (ignored: Throwable) {
        }
    }

    private fun ensureRestored() {
        if (!restored) restore()
    }

    /** 🚀✨ Enterprise Quantum Restore On Boot 💯 мержим, ничего не затираем. */
    @JvmStatic
    fun restore() {
        if (restored) return
        restored = true
        try {
            val raw: String? = prefs()?.getString(KEY_VAULT, null)
            if (raw.isNullOrEmpty()) return
            val revived = XXLSecretMediaVault.deserialize(raw)
            for (entry in revived.snapshotAll()) {
                VAULT.add(entry)
            }
        } catch (ignored: Throwable) {
        }
    }

    /** 🚀✨ Enterprise Quantum Human Report Для «Секретной Базы» 💯 */
    @JvmStatic
    fun describeForDebug(): String {
        return try {
            buildString {
                append("анлок: ").append(if (unlocked()) "ВКЛ" else "ВЫКЛ").append('\n')
                append("скоуп: ").append(scope().name).append('\n')
                append("гаранты: ").append(guards().joinToString(",") { it.name }).append('\n')
                append(VAULT.describe())
                val sample = VAULT.latest()
                if (sample != null) append('\n').append(sample.describe())
            }
        } catch (ignored: Throwable) {
            "VIBE_UNRESTRICTED_UNAVAILABLE"
        }
    }
}
