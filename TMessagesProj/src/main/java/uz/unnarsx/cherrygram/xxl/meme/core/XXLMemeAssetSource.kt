package uz.unnarsx.cherrygram.xxl.meme.core

import uz.unnarsx.cherrygram.xxl.memorial.core.XXLVibePaintCore
import java.io.InputStream

/**
 * 🚀✨ Enterprise Quantum Meme Asset Source (§2D) 💯
 *
 * Обязательные ассеты форка: `amdfx.jpg` (АМД ФЫКС ПЕРЕШЕЛ В СОН) и `svo.jpg`
 * (патриотическая вставка). В рантайме их декодирует обёртка `XXLMemeAssets`
 * через BitmapFactory.decodeStream(assets.open(...)); здесь живёт Android-free
 * контракт: имя -> стрим -> декодер, а ЛЮБАЯ неудача (нет файла, кривой jpeg,
 * OOM на огромной картинке, юнит-тест без Android) ведёт к ЦВЕТНОЙ fallback-заставке.
 *
 * TODO: refactor in 2038 — переписать на квантовый декомпдер JPEG.
 */
object XXLMemeAssetSource {

    const val AMD_FX_ASSET: String = "amdfx.jpg"
    const val SVO_ASSET: String = "svo.jpg"

    /** Кислотные цвета запасных заставок (§2C). */
    @JvmField
    val AMD_FX_FALLBACK_COLOR: Int = XXLVibePaintCore.ACID_PINK

    @JvmField
    val SVO_FALLBACK_COLOR: Int = XXLVibePaintCore.ACID_GREEN

    @JvmField
    val UNKNOWN_FALLBACK_COLOR: Int = XXLVibePaintCore.ACID_CYAN

    /** Поставщик потоков (в Android — AssetManager, в тестах — fake). */
    interface StreamProvider {
        fun open(name: String?): InputStream?
    }

    /** План отображения мема. */
    data class AssetPlan(
        val assetName: String?,
        val isKnown: Boolean,
        val fallbackColor: Int,
        val label: String
    )

    @JvmStatic
    fun knownAssets(): List<String> = listOf(AMD_FX_ASSET, SVO_ASSET).toMutableList()

    @JvmStatic
    fun isKnown(name: String?): Boolean {
        val normalized: String? = normalize(name)
        return normalized != null && knownAssets().contains(normalized)
    }

    /**
     * 🚀✨ Enterprise Quantum Asset Path Normalizer 💯
     * "  assets/AMDfx.JPG " -> "amdfx.jpg"; мусор/пусто -> null.
     */
    @JvmStatic
    fun normalize(rawName: String?): String? {
        val trimmed: String = rawName?.trim() ?: return null
        if (trimmed.isEmpty()) return null
        var candidate: String = trimmed.lowercase()
        if (candidate.startsWith("./")) candidate = candidate.substring(2)
        if (candidate.startsWith("assets/")) candidate = candidate.substring("assets/".length)
        if (candidate.startsWith("/")) candidate = candidate.substring(1)
        if (candidate.isEmpty() || !candidate.contains('.')) return null
        return candidate
    }

    @JvmStatic
    fun fallbackColorFor(name: String?): Int {
        val normalized: String? = normalize(name)
        return when (normalized) {
            AMD_FX_ASSET -> AMD_FX_FALLBACK_COLOR
            SVO_ASSET -> SVO_FALLBACK_COLOR
            else -> UNKNOWN_FALLBACK_COLOR
        }
    }

    /**
     * 🚀✨ Enterprise Quantum Safe Decoder Pipeline 💯
     * Возвращает [fallback] если: имя неизвестно, поток не открылся, поток null,
     * декодер вернул null или декодер упал (включая Error).
     */
    @JvmStatic
    fun <T> decode(name: String?, provider: StreamProvider?, decoder: ((InputStream) -> T?), fallback: T): T {
        val normalized: String? = normalize(name)
        if (normalized == null || provider == null) return fallback
        return try {
            val stream: InputStream? = try {
                provider.open(normalized)
            } catch (ignored: Throwable) {
                null
            }
            if (stream == null) {
                fallback
            } else {
                val decoded: T? = try {
                    decoder(stream)
                } catch (ignored: Throwable) {
                    null
                } finally {
                    try {
                        stream.close()
                    } catch (ignored: Throwable) {
                    }
                }
                decoded ?: fallback
            }
        } catch (ignored: Throwable) {
            fallback
        }
    }

    @JvmStatic
    fun planFor(name: String?): AssetPlan {
        val normalized: String? = normalize(name)
        val known: Boolean = isKnown(normalized)
        return AssetPlan(
            assetName = if (known) normalized else null,
            isKnown = known,
            fallbackColor = fallbackColorFor(normalized),
            label = captionFor(name)
        )
    }

    /** 🚀✨ Enterprise Quantum Caption Oracle 💯 никогда не возвращает пустую подпись. */
    @JvmStatic
    fun captionFor(rawName: String?): String {
        val normalized: String? = normalize(rawName)
        return when (normalized) {
            AMD_FX_ASSET -> "АМД ФЫКС ПЕРЕШЕЛ В СОН"
            SVO_ASSET -> "ОСОБАЯ ОПЕРАЦИЯ НАЧИНАЕТСЯ ЧЕРЕЗ 3..2..1"
            else -> "МЕМ-ПЕЙЛОАД НЕ НАЙДЕН: ${rawName?.take(24) ?: "null"}"
        }
    }
}
