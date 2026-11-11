package com.reelguard.widget

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CoalescingUpdaterTest {
    private val interval = 10_000L

    /** Records the virtual time of every update; [duration] makes each update take that long. */
    private class Probe(val scope: TestScope, val duration: Long = 0L) {
        val runs = mutableListOf<Long>()
        var fail = false

        suspend fun update() {
            runs += scope.testScheduler.currentTime
            if (duration > 0) delay(duration)
            if (fail) error("boom")
        }
    }

    private fun TestScope.updater(
        probe: Probe,
        scope: CoroutineScope = backgroundScope,
    ) = CoalescingUpdater(scope, { currentTime }, interval) { probe.update() }

    /** Background-scope work is not covered by advanceUntilIdle, so move the clock well past every pending delay. */
    private fun TestScope.settle() {
        advanceTimeBy(60_000)
        runCurrent()
    }

    @Test
    fun `first request updates immediately`() =
        runTest {
            val p = Probe(this)
            updater(p).request()
            runCurrent()
            p.runs shouldBe listOf(0L)
        }

    @Test
    fun `a burst collapses into one trailing update at the window end`() =
        runTest {
            val p = Probe(this)
            val u = updater(p)
            u.request()
            runCurrent()
            repeat(20) {
                advanceTimeBy(300)
                u.request()
            }
            settle()
            p.runs shouldBe listOf(0L, interval)
        }

    @Test
    fun `a request while an update is running is not lost`() =
        runTest {
            val p = Probe(this, duration = 4_000L)
            val u = updater(p)
            u.request()
            advanceTimeBy(1_000)
            u.request()
            settle()
            p.runs shouldBe listOf(0L, 4_000L + interval)
        }

    @Test
    fun `the last request converges within the interval however it is timed`() =
        runTest {
            val p = Probe(this, duration = 700L)
            val u = updater(p)
            // Requests at awkward moments: during the wait, during an update, right as one ends, right as the job finishes.
            val offsets = listOf(0L, 9_999L, 10_000L, 10_001L, 10_700L, 10_701L, 20_699L, 20_700L, 20_701L, 31_000L)
            var last = 0L
            for (t in offsets) {
                advanceTimeBy(t - currentTime)
                u.request()
                last = t
            }
            advanceTimeBy(2 * interval)
            runCurrent()
            val lastUpdateStart = p.runs.last()
            // An update must have *started* after the final request.
            (lastUpdateStart >= last) shouldBe true
        }

    @Test
    fun `force skips the throttle`() =
        runTest {
            val p = Probe(this)
            val u = updater(p)
            u.request()
            runCurrent()
            advanceTimeBy(1_000)
            u.request(force = true)
            runCurrent()
            p.runs shouldBe listOf(0L, 1_000L)
        }

    @Test
    fun `a failing update does not wedge later requests`() =
        runTest {
            val p = Probe(this)
            // Like the app's scope: a SupervisorJob and a handler, so a failed update is contained.
            val u = updater(p, CoroutineScope(backgroundScope.coroutineContext + SupervisorJob() + CoroutineExceptionHandler { _, _ -> }))
            p.fail = true
            u.request()
            runCurrent()
            p.fail = false
            advanceTimeBy(interval)
            u.request()
            runCurrent()
            p.runs shouldBe listOf(0L, interval)
        }
}
