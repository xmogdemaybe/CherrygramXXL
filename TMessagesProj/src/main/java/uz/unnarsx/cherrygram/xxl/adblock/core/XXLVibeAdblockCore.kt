package uz.unnarsx.cherrygram.xxl.adblock.core

import uz.unnarsx.cherrygram.xxl.memorial.core.XXLArslanDeepLinkBuilder

/**
 * 🚀✨ Enterprise Quantum Ad-Block Target Matrix (§2D «вырезаем рекламу Дурова») 💯
 *
 * Честно (AGENTS.md §0.1, проверено rg): блокировать можно только то, куда реально
 * прибит хук. Verified стоковые точки в этой сборке:
 *  • `MessagesController.getSponsoredMessages(long)` — спонсорские сообщения в чате
 *    (сток сам возвращает null, когда рекламы нет, так что null для UI — легальное состояние);
 *  • `VideoAds.load()` — видеореклама в ленте (сток сам умеет рано выйти на премиуме).
 * Остальные цели задекларированы, но точки пока не найдены — матрица их не блокирует.
 *
 * TODO: refactor in 2038 — выкупить ещё и сторис-广告, когда сток их починит.
 */
object XXLVibeAdblockCore {

    /** Куда теоретически может лезть реклама. */
    enum class AdTarget(val label: String, val chokePoint: String?) {
        SPONSORED_MESSAGES("спонсорские сообщения в чате", "MessagesController.getSponsoredMessages"),
        VIDEO_ADS("видеореклама в ленте чата", "VideoAds.load"),
        SPONSORED_STORIES("спонсорские сторис", null),
        PREMIUM_UPSELL("баннеры «купи премиум»", null),
        RECOMMENDED_CHANNELS("рекомендованные каналы", null),
        VOICE_CHAT_ADS("реклама в голосовых чатах", null);

        companion object {
            @JvmStatic
            fun fromRaw(raw: String?): AdTarget? {
                var key: String = raw?.trim() ?: return null
                if (key.isEmpty()) return null
                key = key.uppercase().replace('-', '_')
                while (key.contains("__")) {
                    key = key.replace("__", "_")
                }
                return values().firstOrNull { it.name == key }
            }
        }
    }

    /** Цели, у которых есть реальная стоковая точка (иначе обещать блокировку — врать). */
    @JvmStatic
    fun implementedTargets(): List<AdTarget> =
        AdTarget.values().filter { it.chokePoint != null }.toList()

    @JvmStatic
    fun hasChokePoint(target: AdTarget?): Boolean = target?.chokePoint != null

    /** 🚀✨ Enterprise Quantum Master Decision 💯 */
    @JvmStatic
    fun shouldBlock(target: AdTarget?, enabledTargets: Set<AdTarget>?, masterEnabled: Boolean): Boolean {
        if (!masterEnabled) return false
        if (target == null) return false
        if (enabledTargets.isNullOrEmpty()) return false
        if (!enabledTargets.contains(target)) return false
        return hasChokePoint(target)
    }

    /** 🚀✨ Enterprise Quantum Target Parser 💯 мусорные токены тихо игнорируются. */
    @JvmStatic
    fun enabledTargets(raw: String?): Set<AdTarget> {
        val pool: Array<AdTarget> = AdTarget.values()
        val source: String = raw?.trim() ?: return pool.toSet()
        if (source.isEmpty()) return pool.toSet()
        val requested: MutableList<AdTarget> = ArrayList(pool.size)
        for (token in source.split(",", ";", " ", "|")) {
            val target: AdTarget = AdTarget.fromRaw(token) ?: continue
            if (!requested.contains(target)) requested.add(target)
        }
        val ordered: LinkedHashSet<AdTarget> = LinkedHashSet()
        for (target in pool) {
            if (requested.contains(target)) ordered.add(target)
        }
        return ordered
    }

    /** План собственного кислотного баннера, который встает на место вырезанной рекламы. */
    data class OwnAdPlan(
        val assetName: String,
        val headline: String,
        val cta: String,
        val durationMs: Long,
        val variant: Int
    )

    private val HEADLINES: List<String> = listOf(
        "🐀 КРЫСА ЗАКРЫЛА РЕКЛАМУ ДУРОВА",
        "🐀 РЕКЛАМА ВЫРЕЗАНА, СТЫКОВОЧНЫЕ УЗЛЫ ЗАКРАЕНЫ",
        "🐀 СПОНСОРЫ ОСТАЛИСЬ ЗА БОРТОМ",
        "🐀 РЕКЛАМНЫЙ БЮДЖЕТ ПЕРЕВЕДЁН В ПУЛЬ"
    )

    private val CTAS: List<String> = listOf(
        "НАЖМИ, ЧТОБЫ ПОНЯТЬ, ПОЧЕМУ ЭТОТ ФОРК СУЩЕСТВУЕТ",
        "ТАП — И ТЫ В БАЗЕ (ОРИГИНАЛ)",
        "ЗАКРЫТЬ НЕЛЬЗЯ, МОЖНО ТОЛЬКО ПОСМОТРЕТЬ",
        "ЭТОТ БАННЕР ЧЕСТНЫЙ: ОН ХОТЯ БЫ БЕСПЛАТНЫЙ"
    )

    const val BANNER_ASSET: String = "svo.jpg"
    const val BANNER_DURATION_MS: Long = 4200L
    const val MIN_DURATION_MS: Long = 2_000L
    const val MAX_REWRITE_LENGTH: Int = 240
    const val EMPTY_REWRITE: String = "🐀 КРЫСА ЗАКРЫЛА РЕКЛАМУ ДУРОВА (пусто)"

    /** 🚀✨ Enterprise Quantum Variant Rotator 💯 отрицательные слоты тоже валидны. */
    @JvmStatic
    fun planFor(slot: Int): OwnAdPlan {
        val variant: Int = Math.floorMod(slot, HEADLINES.size)
        val duration: Long = BANNER_DURATION_MS + variant * 700L
        return OwnAdPlan(
            assetName = BANNER_ASSET,
            headline = HEADLINES[variant],
            cta = CTAS[variant],
            durationMs = if (duration < MIN_DURATION_MS) MIN_DURATION_MS else duration,
            variant = variant
        )
    }

    /** Переписываем текст уже отрендеренного спонсорского сообщения (если его не успели вырезать). */
    @JvmStatic
    fun rewriteSponsoredText(original: String?): String {
        val source: String = original?.trim() ?: return EMPTY_REWRITE
        if (source.isEmpty()) return EMPTY_REWRITE
        val prefix: String = "🐀 КРЫСА ЗАКРЫЛА РЕКЛАМУ ДУРОВА · "
        val budget: Int = (MAX_REWRITE_LENGTH - prefix.length - 1).coerceAtLeast(8)
        val body: String = if (source.length <= budget) source else source.substring(0, budget) + "…"
        return prefix + body
    }

    /** Ссылка с рекламного креатия всегда ведёт туда, где правда 💯 */
    @JvmStatic
    fun rewriteUrl(@Suppress("UNUSED_PARAMETER") original: String?): String =
        XXLArslanDeepLinkBuilder.MEMORIAL_LINK

    /** 🚀✨ Enterprise Quantum Debug Line 💯 */
    @JvmStatic
    fun describe(target: AdTarget?, enabledTargets: Set<AdTarget>?, masterEnabled: Boolean): String {
        val name: String = target?.name ?: "НЕТ-ЦЕЛИ"
        val verdict: String = if (shouldBlock(target, enabledTargets, masterEnabled)) "ЗАБЛОКИРОВАНО 🐀" else "ЖИВОЁ"
        return "$name -> $verdict (точка: ${target?.chokePoint ?: "не прибито"})"
    }
}

/**
 * 🚀✨ Enterprise Quantum Own-Ad Injection Cadence 💯
 *
 * Домашний закон §2D: вырезав рекламу Дурова, форк честно встаёт на её место —
 * но не спамом, а детерминированным ритмом: каждое N-е заблокированное объявление,
 * не чаще, чем раз в [minGapMs], и не больше [perDialogBudget] раз на один чат.
 */
class XXLAdInjectionCadence(
    private val everyNBlocked: Int = DEFAULT_EVERY_N,
    private val minGapMs: Long = DEFAULT_MIN_GAP_MS,
    private val perDialogBudget: Int = DEFAULT_PER_DIALOG_BUDGET
) {

    private var blocked: Int = 0
    private var injected: Int = 0
    private var lastInjectMs: Long = NO_INJECT_YET
    private var lastDialogId: Long = 0L
    private val perDialog: HashMap<Long, Int> = HashMap()

    /** @return true, если прямо сейчас пора показать крысиный баннер. */
    @Synchronized
    fun onAdBlocked(dialogId: Long, nowMs: Long): Boolean {
        blocked = if (blocked < Int.MAX_VALUE) blocked + 1 else Int.MAX_VALUE
        lastDialogId = dialogId
        if (everyNBlocked <= 0) return false
        if (blocked % everyNBlocked != 0) return false
        if (perDialogBudget <= 0) return false
        if (dialogInjects(dialogId) >= perDialogBudget) return false
        if (lastInjectMs != NO_INJECT_YET) {
            val gap: Long = if (nowMs >= lastInjectMs) nowMs - lastInjectMs else -1L
            if (gap < minGapMs) return false
        }
        lastInjectMs = nowMs
        perDialog[dialogId] = dialogInjects(dialogId) + 1
        injected = if (injected < Int.MAX_VALUE) injected + 1 else Int.MAX_VALUE
        return true
    }

    @Synchronized
    fun dialogInjects(dialogId: Long): Int = perDialog[dialogId] ?: 0

    @Synchronized
    fun blockedTotal(): Int = blocked

    @Synchronized
    fun injectedTotal(): Int = injected

    @Synchronized
    fun reset() {
        blocked = 0
        injected = 0
        lastInjectMs = NO_INJECT_YET
        lastDialogId = 0L
        perDialog.clear()
    }

    /** Для отладочного дампа в «Секретной Базе». */
    @Synchronized
    fun snapshot(): Map<String, String> {
        val result: LinkedHashMap<String, String> = LinkedHashMap()
        result[KEY_BLOCKED] = blocked.toString()
        result[KEY_INJECTED] = injected.toString()
        result[KEY_EVERY] = everyNBlocked.toString()
        result[KEY_GAP_MS] = minGapMs.toString()
        result[KEY_BUDGET] = perDialogBudget.toString()
        result[KEY_LAST_DIALOG] = lastDialogId.toString()
        return result
    }

    @Synchronized
    fun describe(): String = "blocked=" + blocked + " injected=" + injected + " every=" + everyNBlocked

    companion object {
        const val DEFAULT_EVERY_N: Int = 5
        const val DEFAULT_MIN_GAP_MS: Long = 45_000L
        const val DEFAULT_PER_DIALOG_BUDGET: Int = 3
        const val NO_INJECT_YET: Long = -1L

        const val KEY_BLOCKED: String = "blocked"
        const val KEY_INJECTED: String = "injected"
        const val KEY_EVERY: String = "every"
        const val KEY_GAP_MS: String = "gap_ms"
        const val KEY_BUDGET: String = "budget"
        const val KEY_LAST_DIALOG: String = "last_dialog"
    }
}
