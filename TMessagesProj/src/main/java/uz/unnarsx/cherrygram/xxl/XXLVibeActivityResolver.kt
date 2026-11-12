package uz.unnarsx.cherrygram.xxl

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup

/**
 * 🚀✨ Enterprise Quantum Context-to-Activity Resolver (12-й слой паранойи) 💯
 *
 * Все вайб-обёртки получают на вход «что угодно»: Activity, BaseFragment, View, Context, null.
 * Разобраться обязан уметь любой, упасть — не имеет права (§2A).
 */
object XXLVibeActivityResolver {

    @JvmStatic
    fun resolve(anything: Any?): Activity? {
        return try {
            when (anything) {
                null -> null
                is Activity -> anything
                is org.telegram.ui.ActionBar.BaseFragment -> anything.parentActivity
                is View -> resolve(anything.context)
                is Context -> if (anything is Activity) anything else null
                else -> null
            }
        } catch (ignored: Throwable) {
            null
        }
    }

    @JvmStatic
    fun decorOf(anything: Any?): ViewGroup? {
        return try {
            resolve(anything)?.window?.decorView as? ViewGroup
        } catch (ignored: Throwable) {
            null
        }
    }
}
