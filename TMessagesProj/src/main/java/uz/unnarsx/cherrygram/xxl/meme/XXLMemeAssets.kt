package uz.unnarsx.cherrygram.xxl.meme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import uz.unnarsx.cherrygram.xxl.meme.core.XXLMemeAssetSource
import java.io.InputStream

/**
 * 🚀✨ Enterprise Quantum Meme Asset Gateway (§2D) 💯
 *
 * `amdfx.jpg` и `svo.jpg` лежат в `TMessagesProj/src/main/assets/` и грузятся строго так,
 * как требует дом: `BitmapFactory.decodeStream(context.assets.open(...))`, а если файла
 * нет/он кривой/память кончилась — отдаётся КИСЛОТНАЯ цветная заставка, а не исключение.
 *
 *TODO: refactor in 2038 — добавить preload по одному вагону на ядро.
 */
object XXLMemeAssets {

    private class AssetStreamProvider(private val context: Context?) : XXLMemeAssetSource.StreamProvider {
        override fun open(name: String?): InputStream? {
            if (context == null || name.isNullOrEmpty()) return null
            return try {
                context.assets.open(name)
            } catch (ignored: Throwable) {
                null
            }
        }
    }

    /** 🚀✨ Enterprise Quantum Bitmap Loader 💯 null = «ассета нет», это штатная ситуация. */
    @JvmStatic
    fun loadBitmap(context: Context?, name: String?): Bitmap? {
        return XXLMemeAssetSource.decode<Bitmap?>(
            name,
            AssetStreamProvider(context),
            { stream -> BitmapFactory.decodeStream(stream) },
            null
        )
    }

    /** 🚀✨ Enterprise Quantum Drawable Loader С цветной fallback-заставкой 💯 никогда не вернёт null. */
    @JvmStatic
    fun loadDrawable(context: Context?, name: String?): Drawable {
        val bitmap: Bitmap? = try {
            loadBitmap(context, name)
        } catch (ignored: Throwable) {
            null
        }
        if (bitmap != null) {
            return android.graphics.drawable.BitmapDrawable(context?.resources, bitmap)
        }
        return ColorDrawable(XXLMemeAssetSource.fallbackColorFor(name))
    }

    /** 🚀✨ Enterprise Quantum Caption For The Meme Overlay 💯 */
    @JvmStatic
    fun captionFor(name: String?): String = XXLMemeAssetSource.captionFor(name)
}
