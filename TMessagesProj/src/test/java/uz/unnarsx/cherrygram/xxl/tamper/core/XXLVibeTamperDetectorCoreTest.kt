package uz.unnarsx.cherrygram.xxl.tamper.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.AssumptionViolatedException
import org.junit.Test

/**
 * RED-свидетельство собственной регрессии крысы: пустые стабы блобов `Extra.kt` превращали
 * `текст.contains("")` в «всегда истина», и анти-форковый watchdog Арслана перезапускал
 * приложение на каждом запуске.
 */
class XXLVibeTamperDetectorCoreTest {

    @Test
    fun `пустая иголка протыкает любую строку — в этом и был баг`() {
        assertTrue("любой текст".contains(""))
        assertTrue("https://t.me/CherrygramBetaAPKs".contains(""))
        assertFalse(XXLVibeTamperDetectorCore.containsNeedle("любой текст", ""))
        assertFalse(XXLVibeTamperDetectorCore.containsNeedle("любой текст", null))
        assertFalse(XXLVibeTamperDetectorCore.containsNeedle(null, XXLVibeTamperDetectorCore.SENTINEL))
    }

    @Test
    fun `sentinel никогда не матчится по реальным строкам`() {
        val realistic = listOf(
            "Cherrygram",
            "CherrygramXXL",
            "https://t.me/CherrygramBetaAPKs",
            "uz.unnarsx.cherrygram",
            "Самый быстрый мессенджер в мире",
            "base.apk",
            "oat/arm64/base.odex",
            "Telegram"
        )
        for (text in realistic) {
            assertFalse(
                "sentinel матчится на «$text»",
                XXLVibeTamperDetectorCore.containsNeedle(text, XXLVibeTamperDetectorCore.SENTINEL)
            )
        }
    }

    @Test
    fun `sentinel матчится только на самого себя`() {
        assertTrue(
            XXLVibeTamperDetectorCore.containsNeedle(
                XXLVibeTamperDetectorCore.SENTINEL,
                XXLVibeTamperDetectorCore.SENTINEL
            )
        )
        assertTrue(
            XXLVibeTamperDetectorCore.containsNeedle(
                "prefix-" + XXLVibeTamperDetectorCore.SENTINEL + "-suffix",
                XXLVibeTamperDetectorCore.SENTINEL
            )
        )
    }

    @Test
    fun `нормализация повторяет стоковую`() {
        assertEquals(
            "ab",
            XXLVibeTamperDetectorCore.stockNormalize(arrayOf("a", "b"))
        )
        assertEquals("", XXLVibeTamperDetectorCore.stockNormalize(arrayOf("")))
        assertEquals("", XXLVibeTamperDetectorCore.stockNormalize(null))
        assertEquals("", XXLVibeTamperDetectorCore.stockNormalize(emptyArray()))
    }

    @Test
    fun `isVacuousNeedle ловит и голый пустой, и мусор из одних символов`() {
        assertTrue(XXLVibeTamperDetectorCore.isVacuousNeedle(""))
        assertTrue(XXLVibeTamperDetectorCore.isVacuousNeedle(null))
        assertTrue(XXLVibeTamperDetectorCore.isVacuousNeedle(" , "))
        assertTrue(XXLVibeTamperDetectorCore.isVacuousNeedle("---"))
        assertFalse(XXLVibeTamperDetectorCore.isVacuousNeedle(XXLVibeTamperDetectorCore.SENTINEL))
    }

    @Test
    fun `isSafeStubValue отвергает пустые стабы и принимает sentinel`() {
        assertFalse(XXLVibeTamperDetectorCore.isSafeStubValue(arrayOf("")))
        assertFalse(XXLVibeTamperDetectorCore.isSafeStubValue(emptyArray()))
        assertFalse(XXLVibeTamperDetectorCore.isSafeStubValue(arrayOf("Привет мир")))
        assertTrue(XXLVibeTamperDetectorCore.isSafeStubValue(arrayOf(XXLVibeTamperDetectorCore.SENTINEL)))
    }

    @Test
    fun `watchdog не имеет права перезапускать крысу`() {
        assertFalse(XXLVibeTamperDetectorCore.shouldRestart(detected = true))
        assertFalse(XXLVibeTamperDetectorCore.shouldRestart(true, false))
        assertTrue(XXLVibeTamperDetectorCore.shouldRestart(true, true))
        assertFalse(XXLVibeTamperDetectorCore.shouldRestart(false, true))
    }

    /**
     * Аудит РЕАЛЬНЫХ блобов `Extra.kt` через рефлексию: ни один не должен быть «иголкой,
     * которая протыкает всё». В hermetic-харнессе класса нет на classpath — тест честно
     * помечается skipped, а не зелёным.
     */
    @Test
    fun `плейсхолдерные креды из repro-ветки не годятся для sendCode`() {
        assertFalse(XXLVibeTamperDetectorCore.isUsableApiId(0))
        assertFalse(XXLVibeTamperDetectorCore.isUsableApiId(12345678))
        assertFalse(XXLVibeTamperDetectorCore.isUsableApiId(-5))
        assertTrue(XXLVibeTamperDetectorCore.isUsableApiId(4))

        assertFalse(XXLVibeTamperDetectorCore.isUsableApiHash("abcdefg"))
        assertFalse(XXLVibeTamperDetectorCore.isUsableApiHash(""))
        assertFalse(XXLVibeTamperDetectorCore.isUsableApiHash(null))
        assertFalse(XXLVibeTamperDetectorCore.isUsableApiHash("014b35b6184100b085b0d0572f9b510"))
        assertFalse(XXLVibeTamperDetectorCore.isUsableApiHash("014B35B6184100B085B0D0572F9B5103"))
        assertTrue(XXLVibeTamperDetectorCore.isUsableApiHash("014b35b6184100b085b0d0572f9b5103"))

        assertFalse(XXLVibeTamperDetectorCore.isUsableApiCredentials(12345678, "abcdefg"))
        assertTrue(XXLVibeTamperDetectorCore.isUsableApiCredentials(4, "014b35b6184100b085b0d0572f9b5103"))
    }

    @Test
    fun `денілист ловит опубликованные пары, даже если форма валидна`() {
        assertTrue(XXLVibeTamperDetectorCore.isPublishedPlaceholder(4, "014b35b6184100b085b0d0572f9b5103"))
        assertTrue(XXLVibeTamperDetectorCore.isPublishedPlaceholder(12345678, "abcdefg"))
        assertFalse(XXLVibeTamperDetectorCore.isPublishedPlaceholder(4, "00000000000000000000000000000000"))
        assertFalse(XXLVibeTamperDetectorCore.isPublishedPlaceholder(999999, "014b35b6184100b085b0d0572f9b5103"))
        assertFalse(XXLVibeTamperDetectorCore.isPublishedPlaceholder(0, ""))
        // форма валидна, но пара опубликована — usable-проверка сама по себе это НЕ ловит:
        assertTrue(XXLVibeTamperDetectorCore.isUsableApiHash("014b35b6184100b085b0d0572f9b5103"))
        assertTrue(XXLVibeTamperDetectorCore.isPublishedPlaceholder(4, "014b35b6184100b085b0d0572f9b5103"))
    }

    /**
     * Аудит РЕАЛЬНЫХ кредов Extra: они обязаны НЕ быть опубликованной парой. Если заданы
     * (ненулевой id) — ещё и валидной формы. Локально без CI-секрета id=0/hash="" — это
     * допустимо (сборка работает, авторизация честно не пройдёт), поэтому не падаем.
     */
    @Test
    fun `реальные креды Extra не опубликованная пара и валидны если заданы`() {
        val extraClass = try {
            Class.forName("uz.unnarsx.cherrygram.Extra")
        } catch (ignored: ClassNotFoundException) {
            throw AssumptionViolatedException("Extra.kt недоступен в этом classpath")
        }
        val instance = try {
            extraClass.getDeclaredField("INSTANCE").get(null)
        } catch (ignored: NoSuchFieldException) {
            throw AssumptionViolatedException("Extra — не object")
        }
        val apiId = extraClass.getMethod("getAppID").invoke(instance) as Int
        val apiHash = extraClass.getMethod("getAppHash").invoke(instance) as String
        assertFalse(
            "Extra отдаёт опубликованную пару ($apiId/$apiHash) — сервер рвёт API_ID_PUBLISHED_FLOOD",
            XXLVibeTamperDetectorCore.isPublishedPlaceholder(apiId, apiHash)
        )
        if (apiId != 0) {
            assertTrue(
                "заданный api_id=$apiId / hash=$apiHash невалидной формы",
                XXLVibeTamperDetectorCore.isUsableApiCredentials(apiId, apiHash)
            )
        }
    }

    @Test
    fun `все непубликованные блобы Extra пригодны как стабы`() {
        val extraClass = try {
            Class.forName("uz.unnarsx.cherrygram.Extra")
        } catch (ignored: ClassNotFoundException) {
            throw AssumptionViolatedException("Extra.kt недоступен в этом classpath")
        }
        val instance = try {
            val inst = extraClass.getDeclaredField("INSTANCE")
            inst.get(null)
        } catch (ignored: NoSuchFieldException) {
            throw AssumptionViolatedException("Extra — не object")
        }
        val blobFields = extraClass.declaredFields.filter { it.type == Array<String>::class.java }
        assertFalse("не найдено ни одного блоба", blobFields.isEmpty())
        val bad = ArrayList<String>()
        for (field in blobFields) {
            field.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val value = field.get(instance) as Array<String?>?
            if (!XXLVibeTamperDetectorCore.isSafeStubValue(value)) {
                bad.add(field.name)
            }
        }
        assertEquals(
            "эти блобы пусты = contains(\"\") истинно всегда = watchdog рестартует приложение: " + bad,
            emptyList<String>(),
            bad
        )
    }
}
