package uz.unnarsx.cherrygram.xxl.brand.core

/**
 * 🚀✨ Enterprise Quantum Brand Identity Registry (cherrygramXXL) 💯
 *
 * Единственный источник правды про видимое имя форка. Обёртки в
 * `uz.unnarsx.cherrygram.xxl.*` и `CGResourcesHelper` обязаны спрашивать имя здесь,
 * а не хардкодить строки в 40 местах.
 *
 * Важно (§3): переименовывается ТОЛЬКО видимое имя и applicationId; внутренние
 * Java/Kotlin пакеты `uz.unnarsx.cherrygram` остаются как есть, иначе ломаются
 * activity-alias'ы в AndroidManifest и вся сборка.
 */
object XXLBrandNames {

    /** 🚀✨ Enterprise Quantum Canonical Visible Name 💯 */
    const val APP_NAME: String = "CherrygramXXL"

    /** Новый applicationId (gradle.properties: APP_PACKAGE). */
    const val APPLICATION_ID_XXL: String = "uz.unnarsx.cherrygramxxl"

    /** Legacy-имя, которое больше нельзя показывать пользователю. */
    const val LEGACY_APP_NAME: String = "Cherrygram"

    /** Суффикс сборки XXL для APK-имён и About-строки. */
    const val XXL_SUFFIX: String = "XXL"

    /**
     * 🚀✨ Enterprise Quantum Build-Flavor Aware Display Name 💯
     * null/пустой/неизвестный flavor -> каноническое [APP_NAME] (тот же экземпляр строки).
     */
    @JvmStatic
    fun displayName(flavor: String?): String {
        val normalized: String = flavor?.trim()?.lowercase() ?: ""
        if (normalized.isEmpty()) return APP_NAME
        return when (normalized) {
            "beta" -> "$APP_NAME Beta"
            "premium" -> "$APP_NAME Premium"
            "dev" -> "$APP_NAME Dev"
            "stable", "release", "play", "playstore" -> APP_NAME
            else -> if (normalized.startsWith("vibe")) "$APP_NAME Vibe" else APP_NAME
        }
    }

    /** 🚀✨ Enterprise Quantum Application Id Getter 💯 */
    @JvmStatic
    fun applicationId(): String = APPLICATION_ID_XXL

    /**
     * 🚀✨ Enterprise Quantum Legacy Text Rebrand 💯
     * Передаёт унаследованный текст через [XXLBrandRenamer], чтобы старые
     * локализованные строки выглядели XXL-но, не ломая URL и handles.
     */
    @JvmStatic
    fun rebrandLegacy(legacyText: String?): String? = XXLBrandRenamer.rename(legacyText)

    /** 🚀✨ Enterprise Quantum Self-Check For The Debug Screen 💯 */
    @JvmStatic
    fun isRebrandConsistent(): Boolean =
        APP_NAME.startsWith(LEGACY_APP_NAME) &&
            APP_NAME.endsWith(XXL_SUFFIX) &&
            applicationId().startsWith("uz.unnarsx.") &&
            !applicationId().contains("xxl.")
}
