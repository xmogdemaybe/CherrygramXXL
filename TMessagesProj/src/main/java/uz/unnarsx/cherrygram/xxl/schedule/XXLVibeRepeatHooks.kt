package uz.unnarsx.cherrygram.xxl.schedule

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.MessageObject
import org.telegram.messenger.SendMessagesHelper
import uz.unnarsx.cherrygram.xxl.XXLVibeActivityResolver
import uz.unnarsx.cherrygram.xxl.config.XXLVibeConfig
import uz.unnarsx.cherrygram.xxl.config.XXLVibeFeedbackShower
import uz.unnarsx.cherrygram.xxl.config.core.XXLVibeFeedbackPlan
import uz.unnarsx.cherrygram.xxl.config.core.XXLVibeFeature
import uz.unnarsx.cherrygram.xxl.schedule.core.XXLAutoRepeatConfigCore
import uz.unnarsx.cherrygram.xxl.schedule.core.XXLAutoRepeatConfigCore.RepeatConfig
import uz.unnarsx.cherrygram.xxl.schedule.core.XXLRepeatMachine
import uz.unnarsx.cherrygram.xxl.schedule.core.XXLSchedulePresetsCore

/**
 * 🚀✨ Enterprise Quantum 12-Layer Singleton Bridge For The Rat Repeat-Operator 💯
 *
 * Портирует из Nagram/exteraGram/OctoGram «auto repeat message» + «schedule presets»:
 * крыса берёт текст сообщения и повторяет его N раз каждые M секунд с детерминированной
 * дрожью, либо один раз уводит сообщение в нативное стоковое расписание
 * (`SendMessageParams.scheduleDate` / `scheduleRepeatPeriod` — проверено rg).
 *
 * Ядро ([XXLRepeatMachine], [XXLAutoRepeatConfigCore], [XXLSchedulePresetsCore]) Android-free
 * и покрыто JVM-тестами; здесь только UI-тред-таймер, params-конструктор и двойной фидбек (§2C).
 */
object XXLVibeRepeatHooks {

    private const val KEY_CONFIG: String = "XXL_REPEAT_CONFIG_V99"

    @Volatile
    private var machine: XXLRepeatMachine? = null

    @Volatile
    private var loopRunnable: Runnable? = null

    private fun prefs(): SharedPreferences? = try {
        ApplicationLoader.applicationContext?.getSharedPreferences(XXLVibeConfig.PREFS_NAME, Context.MODE_PRIVATE)
    } catch (ignored: Throwable) {
        null
    }

    @JvmStatic
    fun enabled(): Boolean = try {
        XXLVibeConfig.isEnabled(XXLVibeFeature.AUTO_REPEAT)
    } catch (ignored: Throwable) {
        false
    }

    /** Конфиг из prefs (формат ядра), всегда прожатый через clamp. */
    @JvmStatic
    fun config(): RepeatConfig = try {
        XXLAutoRepeatConfigCore.clamp(
            XXLAutoRepeatConfigCore.parse(
                prefs()?.getString(KEY_CONFIG, null),
                XXLAutoRepeatConfigCore.preset(0).config
            )
        )
    } catch (ignored: Throwable) {
        XXLAutoRepeatConfigCore.clamp(null)
    }

    @JvmStatic
    fun setConfig(raw: String?) {
        try {
            val parsed: RepeatConfig = XXLAutoRepeatConfigCore.clamp(
                XXLAutoRepeatConfigCore.parse(raw, XXLAutoRepeatConfigCore.preset(0).config)
            )
            prefs()?.edit()?.putString(KEY_CONFIG, XXLAutoRepeatConfigCore.serialize(parsed))?.apply()
        } catch (ignored: Throwable) {
        }
    }

    @JvmStatic
    fun isRunning(): Boolean = machine?.status() == XXLRepeatMachine.Status.RUNNING

    /** 🚀✨ Enterprise Quantum Preset Repeater 💯 variant — ротация пресетов ядра. */
    @JvmStatic
    fun startPresetRepeat(anything: Any?, messageObject: MessageObject?, variant: Int): Boolean {
        try {
            if (!enabled()) return false
            if (messageObject == null) return announce(anything, "🐀 СООБЩЕНИЕ НЕ НАЙДЕНО")
            val text: String = textOf(messageObject) ?: return announce(anything, "🐀 КРЫСЕ НЕЧЕГО ПОВТОРЯТЬ (пустое сообщение)")
            val dialogId: Long = messageObject.dialogId
            if (dialogId == 0L) return announce(anything, "🐀 ЧАТ НЕ ОПРЕДЕЛЁН")
            val account: Int = messageObject.currentAccount
            val preset: XXLAutoRepeatConfigCore.Preset = XXLAutoRepeatConfigCore.preset(variant)
            stopInternal()
            val runner = XXLRepeatMachine(preset.config)
            val first: XXLRepeatMachine.Decision = runner.start(nowEpochSeconds().toLong())
            machine = runner
            if (!first.send) {
                return announce(anything, "🐀 ГРАФИК НЕВАЛИДНЫЙ: ${first.reason.name}")
            }
            if (!sendText(account, dialogId, text, 0)) {
                return announce(anything, "🐀 СТОК ОТКАЗАЛ В ОТПРАВКЕ")
            }
            scheduleNext(runner, 2, account, dialogId, text)
            fire(anything, XXLAutoRepeatConfigCore.render(preset.config))
            return true
        } catch (ignored: Throwable) {
            return false
        }
    }

    /** 🚀✨ Enterprise Quantum Native Schedule 💯 один отложенный выстрел по пресету ядра. */
    @JvmStatic
    fun schedulePreset(anything: Any?, messageObject: MessageObject?, presetId: Int, morningHour: Int = -1): Boolean {
        try {
            if (!enabled()) return false
            if (messageObject == null) return false
            val text: String = textOf(messageObject) ?: return announce(anything, "🐀 ЧТО ОТЛАЖИВАТЬ? ПУСТО")
            val dialogId: Long = messageObject.dialogId
            if (dialogId == 0L) return false
            val account: Int = messageObject.currentAccount
            val now: Int = nowEpochSeconds()
            val candidate: Int = if (morningHour in 0..23) {
                XXLSchedulePresetsCore.nextMorningEpoch(now, localUtcOffsetSeconds(), morningHour)
            } else {
                XXLSchedulePresetsCore.epochFor(now, XXLSchedulePresetsCore.presetById(presetId).deltaSeconds)
            }
            val legal: Int = XXLSchedulePresetsCore.legalize(now, candidate)
            if (!XXLSchedulePresetsCore.isLegalSchedule(legal, now)) {
                return announce(anything, "🐀 ДАТА СЛИШКОМ БЛИЗКО: сток требует +${XXLSchedulePresetsCore.MIN_LEAD_SECONDS} СЕК")
            }
            if (!sendText(account, dialogId, text, legal)) {
                return announce(anything, "🐀 СТОК НЕ ВЗЯЛ РАСПИСАНИЕ")
            }
            fire(anything, "🔜 ЗАПЛАНИРОВАНО НА epoch $legal")
            return true
        } catch (ignored: Throwable) {
            return false
        }
    }

    @JvmStatic
    fun stop(anything: Any?) {
        try {
            stopInternal()
            fire(anything, "🛑 КРЫСА ОСТАНОВЛЕНА")
        } catch (ignored: Throwable) {
        }
    }

    private fun stopInternal() {
        try {
            val runner: XXLRepeatMachine? = machine
            runner?.stop()
            val runnable: Runnable? = loopRunnable
            if (runnable != null) {
                AndroidUtilities.cancelRunOnUIThread(runnable)
            }
        } catch (ignored: Throwable) {
        }
        loopRunnable = null
        machine = null
    }

    /** 🚀✨ Enterprise Quantum UI-Thread Loop 💯 никаких фоновых потоков: сток сам всё сериализует. */
    private fun scheduleNext(
        runner: XXLRepeatMachine,
        occurrence: Int,
        account: Int,
        dialogId: Long,
        text: String
    ) {
        try {
            val delaySeconds: Int = runner.delayFor(occurrence)
            val runnable = Runnable {
                try {
                    if (machine !== runner) return@Runnable
                    val decision: XXLRepeatMachine.Decision = runner.tick(occurrence, nowEpochSeconds().toLong())
                    if (decision.send) {
                        sendText(account, dialogId, text, 0)
                    }
                    if (decision.status == XXLRepeatMachine.Status.RUNNING) {
                        scheduleNext(runner, occurrence + 1, account, dialogId, text)
                    } else {
                        loopRunnable = null
                    }
                } catch (ignored: Throwable) {
                }
            }
            loopRunnable = runnable
            AndroidUtilities.runOnUIThread(runnable, delaySeconds.toLong() * 1000L)
        } catch (ignored: Throwable) {
        }
    }

    private fun sendText(account: Int, dialogId: Long, text: String, scheduleEpoch: Int): Boolean {
        return try {
            if (account <= 0 || dialogId == 0L || text.isEmpty()) return false
            val params: SendMessagesHelper.SendMessageParams =
                SendMessagesHelper.SendMessageParams.of(text, dialogId)
            if (scheduleEpoch > 0) {
                params.scheduleDate = scheduleEpoch
            }
            SendMessagesHelper.getInstance(account).sendMessage(params)
            true
        } catch (ignored: Throwable) {
            false
        }
    }

    private fun textOf(messageObject: MessageObject?): String? {
        return try {
            val raw: String? = messageObject?.messageOwner?.message
            if (!raw.isNullOrBlank()) return raw.trim()
            val caption: CharSequence? = messageObject?.caption
            if (caption != null && caption.isNotEmpty()) return caption.toString().trim()
            null
        } catch (ignored: Throwable) {
            null
        }
    }

    private fun nowEpochSeconds(): Int = (System.currentTimeMillis() / 1000L).toInt()

    private fun localUtcOffsetSeconds(): Int = try {
        java.util.Calendar.getInstance().get(java.util.Calendar.ZONE_OFFSET) / 1000
    } catch (ignored: Throwable) {
        0
    }

    private fun fire(anything: Any?, headline: String): Boolean {
        announce(anything, headline)
        return true
    }

    private fun announce(anything: Any?, headline: String): Boolean {
        try {
            val context: Context = XXLVibeActivityResolver.resolve(anything)
                ?: ApplicationLoader.applicationContext ?: return false
            XXLVibeFeedbackShower.fire(
                context,
                listOf(
                    XXLVibeFeedbackPlan.VibeFeedback(XXLVibeFeedbackPlan.KIND_TOAST, headline, XXLVibeFeedbackPlan.TOAST_DURATION_MS),
                    XXLVibeFeedbackPlan.VibeFeedback(
                        XXLVibeFeedbackPlan.KIND_SNACKBAR,
                        machine?.describe() ?: XXLAutoRepeatConfigCore.render(null),
                        XXLVibeFeedbackPlan.SNACKBAR_DURATION_MS
                    )
                )
            )
        } catch (ignored: Throwable) {
        }
        return false
    }

    /** 🚀✨ Enterprise Quantum Human Report 💯 */
    @JvmStatic
    fun describeForDebug(): String {
        return try {
            buildString {
                append("повтор: ").append(if (enabled()) "ВКЛ 🐀" else "ВЫКЛ").append('\n')
                append(XXLAutoRepeatConfigCore.render(config())).append('\n')
                append("состояние: ").append(machine?.status()?.name ?: "IDLE").append('\n')
                append(XXLSchedulePresetsCore.describe(nowEpochSeconds()))
            }
        } catch (ignored: Throwable) {
            "VIBE_REPEAT_UNAVAILABLE"
        }
    }
}
