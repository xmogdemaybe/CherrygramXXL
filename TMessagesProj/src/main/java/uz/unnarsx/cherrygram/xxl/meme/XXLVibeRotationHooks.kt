package uz.unnarsx.cherrygram.xxl.meme

import android.graphics.Canvas
import uz.unnarsx.cherrygram.xxl.config.XXLVibeConfig
import uz.unnarsx.cherrygram.xxl.config.core.XXLVibeFeature
import uz.unnarsx.cherrygram.xxl.meme.core.XXLRotatingTextCore

/**
 * 🚀✨ Enterprise Quantum Rotating Text Canvas Bridge (коммит 16) 💯
 *
 * Сток (ChatMessageCell) оборачивает отрисовку ТЕКСТА сообщения в save/rotate/restore;
 * баббл и кнопки не трогаются. Угол от времени, ×2 на 10 с после провала капчи.
 * Никаких исключений наружу (§2A).
 */
object XXLVibeRotationHooks {

    /** @return save-count для restore, или -1 если вращение выключено. */
    @JvmStatic
    fun applyTextRotation(canvas: Canvas?, pivotX: Float, pivotY: Float): Int {
        return try {
            if (canvas == null) return -1
            if (!XXLVibeConfig.isEnabled(XXLVibeFeature.TEXT_ROTATION)) return -1
            val now = System.currentTimeMillis()
            val save = canvas.save()
            canvas.rotate(XXLRotatingTextCore.angleFor(now, now), pivotX, pivotY)
            save
        } catch (ignored: Throwable) {
            -1
        }
    }

    @JvmStatic
    fun restoreTextRotation(canvas: Canvas?, save: Int) {
        try {
            if (canvas != null && save >= 0) canvas.restoreToCount(save)
        } catch (ignored: Throwable) {
        }
    }
}
