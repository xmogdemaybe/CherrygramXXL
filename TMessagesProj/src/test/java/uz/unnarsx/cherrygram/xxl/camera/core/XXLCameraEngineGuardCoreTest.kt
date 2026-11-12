package uz.unnarsx.cherrygram.xxl.camera.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Честный тест гейта камеры: крыса обязана увести съёмку на сток-движок,
 * если CameraX-движок в этом форке — пустые оболочки, а не реальный код.
 */
class XXLCameraEngineGuardCoreTest {

    @Test
    fun `config camerax but sources missing falls back to stock`() {
        assertEquals(
            XXLCameraEngineGuardCore.ENGINE_STOCK,
            XXLCameraEngineGuardCore.decideEngine(
                configType = XXLCameraEngineGuardCore.CONFIG_CAMERA_X,
                engineSourcesAvailable = false,
                sdkInt = 36
            )
        )
    }

    @Test
    fun `config camerax with real sources on new device uses camerax`() {
        assertEquals(
            XXLCameraEngineGuardCore.ENGINE_CAMERAX,
            XXLCameraEngineGuardCore.decideEngine(
                configType = XXLCameraEngineGuardCore.CONFIG_CAMERA_X,
                engineSourcesAvailable = true,
                sdkInt = 36
            )
        )
    }

    @Test
    fun `stock config never upgrades to camerax`() {
        assertEquals(
            XXLCameraEngineGuardCore.ENGINE_STOCK,
            XXLCameraEngineGuardCore.decideEngine(
                configType = XXLCameraEngineGuardCore.CONFIG_TELEGRAM_CAMERA,
                engineSourcesAvailable = true,
                sdkInt = 36
            )
        )
    }

    @Test
    fun `ancient sdk is refused even with sources`() {
        assertEquals(
            XXLCameraEngineGuardCore.ENGINE_STOCK,
            XXLCameraEngineGuardCore.decideEngine(
                configType = XXLCameraEngineGuardCore.CONFIG_CAMERA_X,
                engineSourcesAvailable = true,
                sdkInt = XXLCameraEngineGuardCore.MIN_CAMERAX_SDK - 1
            )
        )
    }

    @Test
    fun `hardware probe refusal is respected`() {
        assertEquals(
            XXLCameraEngineGuardCore.ENGINE_STOCK,
            XXLCameraEngineGuardCore.decideEngine(
                configType = XXLCameraEngineGuardCore.CONFIG_CAMERA_X,
                engineSourcesAvailable = true,
                sdkInt = 36,
                cameraXHardwareOk = false
            )
        )
    }

    @Test
    fun `refusal reason names the unpublished sources`() {
        assertEquals(
            "исходники CameraX-движка не опубликованы",
            XXLCameraEngineGuardCore.explainRefusal(
                XXLCameraEngineGuardCore.CONFIG_CAMERA_X, false, 36
            )
        )
        assertEquals(
            "выбрана сток-камера",
            XXLCameraEngineGuardCore.explainRefusal(
                XXLCameraEngineGuardCore.CONFIG_TELEGRAM_CAMERA, false, 36
            )
        )
    }

    @Test
    fun `garbage zoom ranges never reach the caller`() {
        assertNull(XXLCameraEngineGuardCore.normalizeZoomRange(0f, 10f))
        assertNull(XXLCameraEngineGuardCore.normalizeZoomRange(-1f, 5f))
        assertNull(XXLCameraEngineGuardCore.normalizeZoomRange(Float.NaN, 5f))
        assertNull(XXLCameraEngineGuardCore.normalizeZoomRange(1f, Float.NaN))
        assertNull(XXLCameraEngineGuardCore.normalizeZoomRange(Float.POSITIVE_INFINITY, 5f))
        assertNull(XXLCameraEngineGuardCore.normalizeZoomRange(4f, 2f))
    }

    @Test
    fun `valid zoom range survives untouched`() {
        val range = XXLCameraEngineGuardCore.normalizeZoomRange(1f, 8f)
        assertEquals(1f, range!!.first, 0f)
        assertEquals(8f, range.second, 0f)
    }

    @Test
    fun `clamp keeps zoom inside the range`() {
        assertEquals(1f, XXLCameraEngineGuardCore.clampZoom(0.2f, 1f, 8f), 0f)
        assertEquals(8f, XXLCameraEngineGuardCore.clampZoom(99f, 1f, 8f), 0f)
        assertEquals(3.5f, XXLCameraEngineGuardCore.clampZoom(3.5f, 1f, 8f), 0f)
    }

    @Test
    fun `clamp on broken range degrades to neutral zoom instead of crashing`() {
        assertEquals(1f, XXLCameraEngineGuardCore.clampZoom(6f, 0f, 0f), 0f)
        assertEquals(2f, XXLCameraEngineGuardCore.clampZoom(Float.NaN, 2f, 9f), 0f)
        assertEquals(5f, XXLCameraEngineGuardCore.clampZoom(7f, 5f, 1f), 0f)
    }
}
