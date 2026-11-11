package com.reelguard.detection

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ServiceStopWatcherTest {
    private val grace = 5_000L

    private class Probe {
        var off = true
        var notices = 0
    }

    private fun TestScope.watcher(p: Probe) =
        ServiceStopWatcher(backgroundScope, grace, stillOff = { p.off }, onConfirmedStop = { p.notices++ })

    private fun TestScope.pass(ms: Long) {
        advanceTimeBy(ms)
        runCurrent()
    }

    @Test
    fun `a real switch-off is reported once the grace period has passed`() =
        runTest {
            val p = Probe()
            val w = watcher(p)
            val service = Any()
            w.connected(service)
            w.stopped(service) shouldBe true
            pass(grace - 1)
            p.notices shouldBe 0
            pass(2)
            p.notices shouldBe 1
        }

    @Test
    fun `a new instance connecting within the grace period cancels the notice`() =
        runTest {
            val p = Probe()
            val w = watcher(p)
            val old = Any()
            w.connected(old)
            w.stopped(old)
            pass(1_000)
            w.connected(Any())
            pass(grace * 2)
            p.notices shouldBe 0
        }

    @Test
    fun `a late stop from a replaced instance is ignored`() =
        runTest {
            val p = Probe()
            val w = watcher(p)
            val old = Any()
            val fresh = Any()
            w.connected(old)
            w.connected(fresh)
            // The old instance's onDestroy arrives after the new one connected: the original false alarm.
            w.stopped(old) shouldBe false
            pass(grace * 2)
            p.notices shouldBe 0
        }

    @Test
    fun `no notice while Android still has the service switched on`() =
        runTest {
            val p = Probe().apply { off = false }
            val w = watcher(p)
            val service = Any()
            w.connected(service)
            w.stopped(service) shouldBe true
            pass(grace * 2)
            p.notices shouldBe 0
        }

    @Test
    fun `repeated stops from the same instance give one notice`() =
        runTest {
            val p = Probe()
            val w = watcher(p)
            val service = Any()
            w.connected(service)
            w.stopped(service)
            pass(2_000)
            w.stopped(service)
            pass(grace * 2)
            p.notices shouldBe 1
        }

    @Test
    fun `a stop before any connection is still checked`() =
        runTest {
            val p = Probe()
            val w = watcher(p)
            w.stopped(Any()) shouldBe true
            pass(grace + 1)
            p.notices shouldBe 1
        }
}
