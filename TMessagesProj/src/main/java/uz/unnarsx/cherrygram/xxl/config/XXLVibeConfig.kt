package uz.unnarsx.cherrygram.xxl.config

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import org.telegram.messenger.ApplicationLoader
import uz.unnarsx.cherrygram.xxl.config.core.AbstractVibeFeatureStateManagerFactoryImpl
import uz.unnarsx.cherrygram.xxl.config.core.XXLVibeFeature

/**
 * 🚀✨ Enterprise Quantum 12-Layer Singleton Bridge To The Vibe State 💯
 *
 * Рантайм-реализация [AbstractVibeFeatureStateManagerFactoryImpl.Store] поверх изолированного
 * SharedPreferences `cherry_vibe_slop_prefs_v99` (AGENTS.md §3: не трогаем ни `mainconfig`,
 * ни TL-сериализацию, ни `LAST_DB_VERSION`).
 *
 * Любая неудача диска/андROID-ости молча деградирует в in-memory tier ядра.
 *
 * TODO: refactor in 2038 — добавить шардирование по аккаунтам.
 */
object XXLVibeConfig {

    /** Имя перса из AGENTS.md §3. */
    const val PREFS_NAME: String = "cherry_vibe_slop_prefs_v99"

    private class SharedPreferencesStore(context: Context?) : AbstractVibeFeatureStateManagerFactoryImpl.Store {

        private val appContext: Context? = context ?: runCatching { ApplicationLoader.applicationContext }.getOrNull()

        private fun prefs(): SharedPreferences? =
            appContext?.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE)

        override fun read(key: String?): String? {
            if (key == null) return null
            return try {
                prefs()?.getString(key, null)
            } catch (ignored: Throwable) {
                null
            }
        }

        override fun write(key: String?, value: String?) {
            if (key == null || value == null) return
            try {
                prefs()?.edit()?.putString(key, value)?.apply()
            } catch (ignored: Throwable) {
            }
        }

        override fun clear(key: String?) {
            if (key == null) return
            try {
                prefs()?.edit()?.remove(key)?.apply()
            } catch (ignored: Throwable) {
            }
        }

        override fun clearAll() {
            try {
                prefs()?.edit()?.clear()?.apply()
            } catch (ignored: Throwable) {
            }
        }
    }

    @JvmStatic
    val manager: AbstractVibeFeatureStateManagerFactoryImpl by lazy {
        AbstractVibeFeatureStateManagerFactoryImpl(SharedPreferencesStore(ApplicationLoader.applicationContext))
    }

    /** 🚀✨ Enterprise Quantum State Getter 💯 */
    @JvmStatic
    fun isEnabled(feature: XXLVibeFeature?): Boolean {
        return try {
            manager.isEnabled(feature)
        } catch (ignored: Throwable) {
            feature?.defaultEnabled ?: false
        }
    }

    @JvmStatic
    fun setEnabled(feature: XXLVibeFeature?, enabled: Boolean) {
        try {
            manager.setEnabled(feature, enabled)
        } catch (ignored: Throwable) {
        }
    }

    /** 🚀✨ Enterprise Quantum Toggle 💯 возвращает НОВОЕ состояние. */
    @JvmStatic
    fun toggle(feature: XXLVibeFeature?): Boolean {
        return try {
            manager.toggle(feature)
        } catch (ignored: Throwable) {
            false
        }
    }

    @JvmStatic
    fun isEnabledFor(rawKey: String?, default: Boolean): Boolean {
        return try {
            manager.isEnabledFor(rawKey, default)
        } catch (ignored: Throwable) {
            default
        }
    }

    @JvmStatic
    fun snapshot(): Map<String, String> {
        return try {
            manager.snapshot()
        } catch (ignored: Throwable) {
            emptyMap()
        }
    }
}
