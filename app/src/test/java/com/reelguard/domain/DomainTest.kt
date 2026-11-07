package com.reelguard.domain

import com.reelguard.data.datastore.Apps
import com.reelguard.data.datastore.Settings
import com.reelguard.data.datastore.SettingsRepository
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZoneId

class DayKeyTest {
    private val utc = ZoneId.of("UTC")

    private fun ms(iso: String) = Instant.parse(iso).toEpochMilli()

    @Test
    fun `midnight reset uses the calendar date`() {
        DayKey.of(ms("2026-03-10T23:59:00Z"), 0, utc) shouldBe "2026-03-10"
        DayKey.of(ms("2026-03-11T00:00:00Z"), 0, utc) shouldBe "2026-03-11"
    }

    @Test
    fun `custom reset time shifts the boundary`() {
        val reset = 4 * 60
        DayKey.of(ms("2026-03-11T03:59:00Z"), reset, utc) shouldBe "2026-03-10"
        DayKey.of(ms("2026-03-11T04:00:00Z"), reset, utc) shouldBe "2026-03-11"
    }

    @Test
    fun `next reset is the following boundary`() {
        val reset = 4 * 60
        DayKey.nextReset(ms("2026-03-11T03:59:00Z"), reset, utc) shouldBe ms("2026-03-11T04:00:00Z")
        DayKey.nextReset(ms("2026-03-11T04:00:00Z"), reset, utc) shouldBe ms("2026-03-12T04:00:00Z")
        DayKey.nextReset(ms("2026-03-11T12:00:00Z"), 0, utc) shouldBe ms("2026-03-12T00:00:00Z")
    }

    @Test
    fun `timezone change is handled by recomputation`() {
        val instant = ms("2026-03-11T02:00:00Z")
        DayKey.of(instant, 0, ZoneId.of("Asia/Tokyo")) shouldBe "2026-03-11"
        DayKey.of(instant, 0, ZoneId.of("America/Los_Angeles")) shouldBe "2026-03-10"
    }

    @Test
    fun `daylight saving day still yields exactly one key per boundary`() {
        val ny = ZoneId.of("America/New_York")
        // US DST starts 2026-03-08 at 02:00 local.
        DayKey.of(ms("2026-03-08T06:30:00Z"), 0, ny) shouldBe "2026-03-08"
        DayKey.of(ms("2026-03-09T03:59:00Z"), 0, ny) shouldBe "2026-03-08"
        DayKey.of(ms("2026-03-09T04:00:00Z"), 0, ny) shouldBe "2026-03-09"
    }
}

class EvaluateLimitTest {
    private val yt = Apps.YOUTUBE
    private val ig = Apps.INSTAGRAM

    @Test
    fun `under the limit is OK with remaining`() {
        val st = EvaluateLimit.evaluate(mapOf(yt to 10, ig to 3), Settings(combinedLimit = 50))
        st.status shouldBe LimitStatus.OK
        st.total shouldBe 13
        st.remaining shouldBe 37
        st.blockedApps shouldBe emptySet()
    }

    @Test
    fun `warns at 80 percent`() {
        EvaluateLimit.evaluate(mapOf(yt to 40), Settings(combinedLimit = 50)).status shouldBe LimitStatus.WARN
        EvaluateLimit.evaluate(mapOf(yt to 39), Settings(combinedLimit = 50)).status shouldBe LimitStatus.OK
        EvaluateLimit.evaluate(mapOf(yt to 45), Settings(combinedLimit = 50, warnAt = 0)).status shouldBe LimitStatus.OK
    }

    @Test
    fun `combined limit blocks every monitored app`() {
        val st = EvaluateLimit.evaluate(mapOf(yt to 30, ig to 20), Settings(combinedLimit = 50))
        st.status shouldBe LimitStatus.REACHED
        st.blockedApps shouldBe setOf(yt, ig)
    }

    @Test
    fun `per app cap blocks only that app`() {
        val s = Settings(combinedLimit = 50, perAppLimits = mapOf(ig to 5))
        val st = EvaluateLimit.evaluate(mapOf(yt to 10, ig to 5), s)
        st.status shouldBe LimitStatus.REACHED
        st.blockedApps shouldBe setOf(ig)
    }

    @Test
    fun `unmonitored apps are not counted or blocked`() {
        val s = Settings(combinedLimit = 10, monitoredApps = setOf(yt))
        val st = EvaluateLimit.evaluate(mapOf(yt to 4, ig to 100), s)
        st.total shouldBe 4
        st.status shouldBe LimitStatus.OK
    }
}

class HistoryStatsTest {
    private fun day(
        d: Int,
        total: Int,
    ) = DayTotal("2026-03-%02d".format(d), total)

    @Test
    fun `streak counts back from today until a day over the limit`() {
        val days = listOf(day(1, 10), day(2, 60), day(3, 20), day(4, 30), day(5, 0))
        HistoryStats.compute(days, 50, "2026-03-01").streak shouldBe 3
    }

    @Test
    fun `days before the first recorded day are ignored`() {
        val days = (1..7).map { day(it, if (it >= 5) 10 else 0) }
        val st = HistoryStats.compute(days, 50, "2026-03-05")
        st.streak shouldBe 3
        st.best?.total shouldBe 10
    }

    @Test
    fun `best and worst day`() {
        val days = listOf(day(1, 5), day(2, 80), day(3, 20))
        val st = HistoryStats.compute(days, 50, "2026-03-01")
        st.best?.dayKey shouldBe "2026-03-01"
        st.worst?.dayKey shouldBe "2026-03-02"
    }
}

class SettingsEncodingTest {
    @Test
    fun `per app limits round trip and ignore garbage`() {
        val m = mapOf("a.b" to 5, "c.d" to 9)
        SettingsRepository.decodeLimits(SettingsRepository.encodeLimits(m)) shouldBe m
        SettingsRepository.decodeLimits("x=;=3;y=2;;bad") shouldBe mapOf("y" to 2)
        SettingsRepository.decodeLimits(null) shouldBe emptyMap()
    }
}
