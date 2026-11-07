package com.reelguard.domain

import com.reelguard.data.datastore.Settings
import javax.inject.Inject

enum class LimitStatus { OK, WARN, REACHED }

data class LimitState(
    val status: LimitStatus,
    val total: Int,
    val limit: Int,
    val counts: Map<String, Int>,
    /** Apps that must be blocked right now (ignoring snooze/blocking toggles, which are applied by the caller). */
    val blockedApps: Set<String>,
) {
    val remaining: Int get() = (limit - total).coerceAtLeast(0)
}

class EvaluateLimit
    @Inject
    constructor() {
        operator fun invoke(
            counts: Map<String, Int>,
            s: Settings,
        ): LimitState = evaluate(counts, s)

        companion object {
            fun evaluate(
                counts: Map<String, Int>,
                s: Settings,
            ): LimitState {
                val monitored = s.monitoredApps
                val total = counts.filterKeys { it in monitored }.values.sum()
                val combinedReached = total >= s.combinedLimit
                val perAppReached =
                    monitored.filter { pkg ->
                        val cap = s.perAppLimits[pkg]
                        cap != null && (counts[pkg] ?: 0) >= cap
                    }.toSet()
                val blocked = if (combinedReached) monitored else perAppReached
                val status =
                    when {
                        blocked.isNotEmpty() -> LimitStatus.REACHED
                        s.warnAt > 0 && total * 100 >= s.combinedLimit * s.warnAt -> LimitStatus.WARN
                        else -> LimitStatus.OK
                    }
                return LimitState(status, total, s.combinedLimit, counts, blocked)
            }
        }
    }
