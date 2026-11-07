package com.reelguard.domain

import com.reelguard.data.datastore.SettingsRepository
import com.reelguard.data.repo.CountRepository
import javax.inject.Inject

/** Triggers a widget refresh. Implemented in the widget package; debounced there. */
interface WidgetRefresher {
    fun request(force: Boolean = false)
}

class IncrementCount
    @Inject
    constructor(
        private val counts: CountRepository,
        private val settings: SettingsRepository,
        private val evaluate: EvaluateLimit,
    ) {
        /**
         * Counts one Reel/Short unless the app is monitored-and-blocked. Returns the limit state after the call.
         * While blocked (overlay up) the count is capped, so "N watched" never exceeds the limit because of hidden swipes.
         */
        suspend operator fun invoke(
            pkg: String,
            now: Long,
        ): LimitState {
            val s = settings.current()
            val day = DayKey.of(now, s.resetMinutes)
            val before = evaluate(counts.countsFor(day), s)
            if (pkg !in s.monitoredApps) return before
            val enforce = s.blockingEnabled && !s.snoozeActive(now)
            if (enforce && pkg in before.blockedApps) return before
            counts.increment(day, pkg, now)
            return evaluate(counts.countsFor(day), s)
        }
    }

class ResetDay
    @Inject
    constructor(
        private val counts: CountRepository,
        private val settings: SettingsRepository,
        private val widget: WidgetRefresher,
    ) {
        /** Counts are keyed by day, so a reset only prunes old events, clears snooze and refreshes the widget. */
        suspend operator fun invoke(now: Long) {
            counts.prune(now)
            settings.clearSnooze()
            widget.request(force = true)
        }
    }
