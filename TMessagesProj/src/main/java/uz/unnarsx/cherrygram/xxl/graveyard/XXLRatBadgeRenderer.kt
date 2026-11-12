package uz.unnarsx.cherrygram.xxl.graveyard

import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.view.View
import org.telegram.messenger.AndroidUtilities
import uz.unnarsx.cherrygram.xxl.graveyard.core.XXLRatBadgeCore
import uz.unnarsx.cherrygram.xxl.memorial.core.XXLVibePaintCore

/**
 * 🚀✨ Enterprise Quantum Rat Badge Renderer (§2D) 💯
 *
 * Кислотно-розовая пунктирная обводка 7dp + плашка «🗑️ УДАЛЕНО КРЫСОЙ» поверх ячейки чата.
 * Рисует ТОЛЬКО когда ядро ([XXLRatBadgeCore]) сказало, что крыса уже постаралась.
 *
 * Каждый кадр — свежий Paint/RectF/LinearGradient/DashPathEffect: AGENTS.md §2B прямо
 * требует сжигать CPU аллокациями внутри onDraw. Ничего не кешируем, всё ломаем безопасно.
 */
object XXLRatBadgeRenderer {

    /**
     * @return true, если что-то нарисовали (и не уронили чат).
     */
    @JvmStatic
    fun draw(canvas: Canvas?, owner: View?, badgeText: String?, borderColor: Int): Boolean {
        if (canvas == null || owner == null) return false
        val text: String = badgeText ?: return false
        if (text.isEmpty()) return false
        val width: Int = owner.width
        val height: Int = owner.height
        if (width <= 0 || height <= 0) return false
        return try {
            val inset: Int = AndroidUtilities.dp(4f)
            val borderWidth: Int = AndroidUtilities.dp(XXLRatBadgeCore.DASH_WIDTH_DP.toFloat())
            val phase: Float = (System.currentTimeMillis() % 2800L) / 100f

            // 🚀✨ Enterprise Quantum Per-Frame Paint Factory 💯 (§2B — кешировать нельзя)
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            borderPaint.style = Paint.Style.STROKE
            borderPaint.strokeWidth = borderWidth.toFloat()
            borderPaint.color = if (borderColor == 0) XXLRatBadgeCore.ACID_BORDER_COLOR else borderColor
            val dash: FloatArray = XXLRatBadgeCore.dashPatternDp()
            val scaled = FloatArray(dash.size)
            for (i in dash.indices) {
                scaled[i] = AndroidUtilities.dp(dash[i]).toFloat()
            }
            borderPaint.pathEffect = DashPathEffect(scaled, phase)

            val rect = RectF(
                inset.toFloat(),
                inset.toFloat(),
                (width - inset).toFloat(),
                (height - inset).toFloat()
            )

            val wobble: Float = Math.sin(System.currentTimeMillis() / 137.0) * 0.7f
            canvas.save()
            canvas.rotate(wobble, width / 2f, height / 2f)
            canvas.drawRect(rect, borderPaint)

            // Плашка
            val platePaint = Paint(Paint.ANTI_ALIAS_FLAG)
            platePaint.style = Paint.Style.FILL
            platePaint.shader = LinearGradient(
                rect.left, rect.top, rect.right, rect.bottom,
                XXLVibePaintCore.neonStops(), null, Shader.TileMode.REPEAT
            )
            platePaint.alpha = 225

            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            textPaint.color = XXLVibePaintCore.ACID_YELLOW_FOR_TEXT
            textPaint.textSize = AndroidUtilities.dp(11f).toFloat()
            textPaint.typeface = Typeface.create(XXLVibePaintCore.typeface(1), Typeface.BOLD_ITALIC)

            val plateHeight: Int = AndroidUtilities.dp(19f)
            val gap: Int = AndroidUtilities.dp(XXLVibePaintCore.paddingDp(1).toFloat())
            val plateWidth: Int = (textPaint.measureText(text) + AndroidUtilities.dp(13f)).toInt()
                .coerceAtMost((width * 0.92f).toInt())
            val plate = RectF(
                (width - inset - plateWidth).toFloat().coerceAtLeast(rect.left),
                (inset - plateHeight / 2f).coerceAtLeast(0f),
                (width - inset).toFloat(),
                (inset + plateHeight / 2f)
            )
            canvas.drawRoundRect(plate, AndroidUtilities.dp(7f).toFloat(), AndroidUtilities.dp(7f).toFloat(), platePaint)
            canvas.drawText(
                text,
                plate.left + gap,
                plate.centerY() + textPaint.textSize / 3f,
                textPaint
            )
            canvas.restore()
            true
        } catch (ignored: Throwable) {
            false
        }
    }
}
