package uz.unnarsx.cherrygram.xxl.camera.core

/**
 * 🚀✨ Enterprise Quantum Camera Engine Arbitration Core 💯
 *
 * вибр-костыль чистый, без Android'а, чтобы его вообще можно было чем-то проверить.
 * Решает, каким движком снимать видеосообщение, и нормализует диапазон зума,
 * который просит стоковый `CameraXUtils.getRoundVideoCameraSelector`.
 */
object XXLCameraEngineGuardCore {

    const val ENGINE_STOCK: Int = 0
    const val ENGINE_CAMERAX: Int = 1

    /** Ровно те значения, которые лежат в `CherrygramCameraConfig.CP_CameraType1`. */
    const val CONFIG_TELEGRAM_CAMERA: Int = 0
    const val CONFIG_CAMERA_X: Int = 1

    /** minSdk проекта: ниже — CameraX-треды не тянем вообще. */
    const val MIN_CAMERAX_SDK: Int = 24

    @JvmStatic
    @JvmOverloads
    fun decideEngine(
        configType: Int,
        engineSourcesAvailable: Boolean,
        sdkInt: Int,
        cameraXHardwareOk: Boolean = true
    ): Int {
        if (configType != CONFIG_CAMERA_X) return ENGINE_STOCK
        if (!engineSourcesAvailable) return ENGINE_STOCK
        if (sdkInt < MIN_CAMERAX_SDK) return ENGINE_STOCK
        if (!cameraXHardwareOk) return ENGINE_STOCK
        return ENGINE_CAMERAX
    }

    /** Почему крыса отказалась от CameraX — для честного текста в меню, не для исключений. */
    @JvmStatic
    fun explainRefusal(configType: Int, engineSourcesAvailable: Boolean, sdkInt: Int): String {
        if (configType != CONFIG_CAMERA_X) return "выбрана сток-камера"
        if (!engineSourcesAvailable) return "исходники CameraX-движка не опубликованы"
        if (sdkInt < MIN_CAMERAX_SDK) return "android $sdkInt < $MIN_CAMERAX_SDK"
        return "движок доступен"
    }

    /** null = «диапазона нет, работай как умеешь». Никаких падений на мусорных значениях. */
    @JvmStatic
    fun normalizeZoomRange(min: Float, max: Float): Pair<Float, Float>? {
        if (min.isNaN() || max.isNaN()) return null
        if (min.isInfinite() || max.isInfinite()) return null
        if (min <= 0f) return null
        if (max < min) return null
        return Pair(min, max)
    }

    @JvmStatic
    fun clampZoom(ratio: Float, min: Float, max: Float): Float {
        val safeMin: Float = if (min <= 0f || min.isNaN()) 1f else min
        val safeMax: Float = if (max < safeMin || max.isNaN()) safeMin else max
        if (ratio.isNaN()) return safeMin
        if (ratio < safeMin) return safeMin
        if (ratio > safeMax) return safeMax
        return ratio
    }
}
