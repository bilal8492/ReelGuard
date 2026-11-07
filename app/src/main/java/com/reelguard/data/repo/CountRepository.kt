package com.reelguard.data.repo

import com.reelguard.data.datastore.SettingsRepository
import com.reelguard.data.room.ReelDao
import com.reelguard.domain.DayKey
import com.reelguard.domain.DayTotal
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CountRepository
    @Inject
    constructor(
        private val dao: ReelDao,
        private val settings: SettingsRepository,
    ) {
        /** Emits the current day key, and again whenever the reset boundary passes (timer, not polling). */
        @OptIn(ExperimentalCoroutinesApi::class)
        fun todayKey(): Flow<String> =
            settings.settings.map { it.resetMinutes }.distinctUntilChanged().flatMapLatest { reset ->
                flow {
                    while (true) {
                        val now = System.currentTimeMillis()
                        emit(DayKey.of(now, reset))
                        delay((DayKey.nextReset(now, reset) - now).coerceAtLeast(1_000L) + 50L)
                    }
                }
            }.distinctUntilChanged()

        @OptIn(ExperimentalCoroutinesApi::class)
        fun todayCounts(): Flow<Map<String, Int>> =
            todayKey().flatMapLatest { day ->
                dao.observeDay(day).map { rows -> rows.associate { it.appId to it.count } }
            }

        suspend fun countsFor(day: String): Map<String, Int> = dao.getDay(day).associate { it.appId to it.count }

        suspend fun increment(
            day: String,
            app: String,
            now: Long,
        ) = dao.increment(day, app, now)

        /** Totals for the last [days] days ending today, with zero-filled gaps. */
        @OptIn(ExperimentalCoroutinesApi::class)
        fun history(days: Int): Flow<List<DayTotal>> =
            todayKey().flatMapLatest { today ->
                val end = LocalDate.parse(today)
                val start = end.minusDays(days - 1L)
                dao.observeSince(start.toString()).map { rows ->
                    val byDay = rows.groupBy { it.dayKey }
                    (0 until days).map { i ->
                        val key = start.plusDays(i.toLong()).toString()
                        val list = byDay[key].orEmpty()
                        DayTotal(key, list.sumOf { it.count }, list.associate { it.appId to it.count })
                    }
                }
            }

        suspend fun firstDay(): String? = dao.firstDay()?.dayKey

        /** Hour-of-day histogram (24 buckets) of events in the last 30 days. */
        suspend fun hourHistogram(now: Long): IntArray {
            val buckets = IntArray(24)
            val zone = ZoneId.systemDefault()
            dao.eventTimes(now - RETENTION_MS).forEach {
                buckets[java.time.Instant.ofEpochMilli(it).atZone(zone).hour]++
            }
            return buckets
        }

        suspend fun prune(now: Long): Int = dao.pruneEvents(now - RETENTION_MS)

        companion object {
            const val RETENTION_MS = 30L * 24 * 60 * 60 * 1000
        }
    }
