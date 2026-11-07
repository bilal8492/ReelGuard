package com.reelguard.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Day boundaries derived from the user's reset time, so timezone and clock changes are handled by recomputation. */
object DayKey {
    /** The key (ISO date) of the "day" that [nowMs] belongs to when days roll over at [resetMinutes] after midnight. */
    fun of(
        nowMs: Long,
        resetMinutes: Int,
        zone: ZoneId = ZoneId.systemDefault(),
    ): String = Instant.ofEpochMilli(nowMs).atZone(zone).minusMinutes(resetMinutes.toLong()).toLocalDate().toString()

    /** Epoch millis of the next reset boundary after [nowMs]. */
    fun nextReset(
        nowMs: Long,
        resetMinutes: Int,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long {
        val today = Instant.ofEpochMilli(nowMs).atZone(zone).minusMinutes(resetMinutes.toLong()).toLocalDate()
        return today.plusDays(1).atStartOfDay(zone).plusMinutes(resetMinutes.toLong()).toInstant().toEpochMilli()
    }

    fun parse(key: String): LocalDate = LocalDate.parse(key)
}
