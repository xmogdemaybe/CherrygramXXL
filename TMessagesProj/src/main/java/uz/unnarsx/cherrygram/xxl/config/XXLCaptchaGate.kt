package uz.unnarsx.cherrygram.xxl.config

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import uz.unnarsx.cherrygram.xxl.XXLVibeActivityResolver
import uz.unnarsx.cherrygram.xxl.config.core.XXLVibeFeature
import uz.unnarsx.cherrygram.xxl.meme.core.XXLCaptchaValidator
import uz.unnarsx.cherrygram.xxl.meme.core.XXLRotatingTextCore

/**
 * 🚀✨ Enterprise Quantum Humiliation Captcha Gate (коммит 16) 💯
 *
 * Пока вращение включено, попытка выключить его НЕ выключает сразу, а открывает fullscreen
 * «ПОДТВЕРДИТЕ, ЧТО ВЫ НЕ АРСЛАН (КАПЧА)»: этап 1 — душный пример, этап 2 — точная фраза.
 * Ошибка → красный тост «ОШИБКА КАПЧИ! ДОСТУП ЗАПРЕЩЕН», новая капча, вращение ×2 на 10 с.
 * Только оба этапа верны → тумблер FALSE. Логика проверки — в Android-free XXLCaptchaValidator.
 */
object XXLCaptchaGate {

    @JvmStatic
    fun requestToggle(anything: Any?) {
        try {
            val activity: Activity = XXLVibeActivityResolver.resolve(anything) ?: return
            if (activity.isFinishing) return
            if (!XXLVibeConfig.isEnabled(XXLVibeFeature.TEXT_ROTATION)) {
                XXLVibeConfig.setEnabled(XXLVibeFeature.TEXT_ROTATION, true)
                return
            }
            showCaptcha(activity)
        } catch (ignored: Throwable) {
        }
    }

    private fun showCaptcha(activity: Activity) {
        val session = XXLCaptchaValidator.Session((System.currentTimeMillis() % 4).toInt())
        val dialog = Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val root = LinearLayout(activity)
        root.orientation = LinearLayout.VERTICAL
        root.gravity = Gravity.CENTER
        root.setBackgroundColor(Color.BLACK)
        root.setPadding(48, 48, 48, 48)

        val title = TextView(activity)
        title.text = "ПОДТВЕРДИТЕ, ЧТО ВЫ НЕ АРСЛАН (КАПЧА)"
        title.setTextColor(Color.RED)
        title.textSize = 20f
        title.gravity = Gravity.CENTER
        root.addView(title)

        val challengeView = TextView(activity)
        challengeView.setTextColor(Color.YELLOW)
        challengeView.textSize = 18f
        challengeView.gravity = Gravity.CENTER
        challengeView.text = session.challenge.expression
        root.addView(challengeView)

        val input = EditText(activity)
        input.setTextColor(Color.WHITE)
        input.setHintTextColor(Color.GRAY)
        root.addView(input)

        val button = Button(activity)
        button.text = "ДАЛЕЕ"
        root.addView(button)

        val stage = intArrayOf(1)
        button.setOnClickListener {
            try {
                if (stage[0] == 1) {
                    val answer = input.text.toString().trim().toIntOrNull()
                    if (answer != null && session.submitMath(answer)) {
                        stage[0] = 2
                        challengeView.text = "ВВЕДИТЕ ФРАЗУ БЕЗ ЕДИНОЙ ОПЕЧАТКИ:"
                        input.setText("")
                        input.hint = XXLCaptchaValidator.PHRASE
                        button.text = "ПРИЗНАТЬ"
                    } else {
                        fail(activity, session, challengeView)
                    }
                } else {
                    if (session.submitPhrase(input.text.toString())) {
                        XXLVibeConfig.setEnabled(XXLVibeFeature.TEXT_ROTATION, false)
                        dialog.dismiss()
                    } else {
                        fail(activity, session, challengeView)
                        stage[0] = 1
                        button.text = "ДАЛЕЕ"
                    }
                }
            } catch (ignored: Throwable) {
            }
        }

        dialog.setContentView(root, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        dialog.show()
    }

    private fun fail(activity: Activity, session: XXLCaptchaValidator.Session, challengeView: TextView) {
        try {
            Toast.makeText(activity, "ОШИБКА КАПЧИ! ДОСТУП ЗАПРЕЩЕН", Toast.LENGTH_LONG).show()
            XXLRotatingTextCore.boost(System.currentTimeMillis())
            session.reset((System.currentTimeMillis() % 4).toInt())
            challengeView.text = session.challenge.expression
        } catch (ignored: Throwable) {
        }
    }
}
