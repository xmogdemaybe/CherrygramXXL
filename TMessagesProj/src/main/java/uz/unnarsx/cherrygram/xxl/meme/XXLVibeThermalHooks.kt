package uz.unnarsx.cherrygram.xxl.meme

import android.graphics.Canvas
import android.view.View
import org.telegram.messenger.ImageReceiver
import uz.unnarsx.cherrygram.xxl.config.XXLVibeConfig
import uz.unnarsx.cherrygram.xxl.config.core.XXLVibeFeature
import uz.unnarsx.cherrygram.xxl.meme.core.XXLThermalThrottleCore
import uz.unnarsx.cherrygram.xxl.meme.core.XXLTrashHudStatFactoryImpl

/**
 * 🚀✨ Enterprise Quantum 12-Layer Singleton Bridge For FX-8350 Thermal Chaos (коммит 15) 💯
 *
 * Сток (ChatMessageCell / ChatAvatarContainer / ChatActivity) дёргает ровно эти методы.
 * Никаких исключений наружу (§2A).
 */
object XXLVibeThermalHooks {

    private val factory = XXLTrashHudStatFactoryImpl()

    /** Текущая псевдотемпература питальника (с учётом override сброса). */
    @JvmStatic
    fun currentTempC(): Int {
        return try {
            val tick = ((System.currentTimeMillis() / 1000L) % 100000L).toInt() + 1
            factory.statsFor(tick).cpuTempC
        } catch (ignored: Throwable) {
            0
        }
    }

    @JvmStatic
    fun overheat(): Boolean = XXLThermalThrottleCore.isOverheat(currentTempC())

    /** Коммит 15: сообщения дрожат по горизонтали ±4dp при перегреве. */
    @JvmStatic
    fun applyDrunkGeometry(cell: View?) {
        try {
            if (cell == null) return
            if (overheat()) {
                val density = cell.resources.displayMetrics.density
                cell.translationX = XXLThermalThrottleCore.shakeOffsetDp(System.currentTimeMillis()) * density
            } else if (cell.translationX != 0f) {
                cell.translationX = 0f
            }
        } catch (ignored: Throwable) {
        }
    }

    /** Коммит 15: медленное вращение аватарки вокруг её центра. */
    @JvmStatic
    fun drawAvatarRotated(canvas: Canvas?, receiver: ImageReceiver?) {
        try {
            if (canvas == null || receiver == null) return
            if (!XXLVibeConfig.isEnabled(XXLVibeFeature.VIBE_UI_TRASH)) {
                receiver.draw(canvas)
                return
            }
            val cx = receiver.imageX + receiver.imageWidth / 2f
            val cy = receiver.imageY + receiver.imageHeight / 2f
            val save = canvas.save()
            canvas.rotate(XXLThermalThrottleCore.avatarRotationDeg(System.currentTimeMillis()), cx, cy)
            receiver.draw(canvas)
            canvas.restoreToCount(save)
        } catch (ignored: Throwable) {
            try { receiver?.draw(canvas) } catch (ignored2: Throwable) {}
        }
    }

    /** Коммит 15: суффикс шапки «| ⛏️ майнит на FX-8350». */
    @JvmStatic
    fun minerSuffix(): String {
        return try {
            if (XXLVibeConfig.isEnabled(XXLVibeFeature.VIBE_UI_TRASH)) XXLThermalThrottleCore.MINER_SUFFIX else ""
        } catch (ignored: Throwable) {
            ""
        }
    }

    @JvmStatic
    fun attachAlarm(anything: Any?) {
        try { XXLThermalAlarmOverlay.attach(anything) } catch (ignored: Throwable) {}
    }

    @JvmStatic
    fun detachAlarm(anything: Any?) {
        try { XXLThermalAlarmOverlay.detach(anything) } catch (ignored: Throwable) {}
    }
}
