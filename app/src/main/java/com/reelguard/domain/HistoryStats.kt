package com.reelguard.domain

import java.time.LocalDate

data class DayTotal(val dayKey: String, val total: Int, val perApp: Map<String, Int> = emptyMap())

data class HistoryStats(val streak: Int, val best: DayTotal?, val worst: DayTotal?) {
    companion object {
        /**
         * @param days consecutive days in ascending order, ending with today. Days before [firstDataDay] are ignored
         * so that days before installation do not inflate the streak.
         * A day is "under the limit" when total <= [limit]; today counts only while it is still under the limit.
         */
        fun compute(
            days: List<DayTotal>,
            limit: Int,
            firstDataDay: String?,
        ): HistoryStats {
            val first = firstDataDay?.let { LocalDate.parse(it) }
            val relevant = days.filter { first == null || !LocalDate.parse(it.dayKey).isBefore(first) }
            var streak = 0
            for (d in relevant.asReversed()) {
                if (d.total <= limit) streak++ else break
            }
            return HistoryStats(streak, relevant.minByOrNull { it.total }, relevant.maxByOrNull { it.total })
        }
    }
}
