package com.reelguard.detection

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ReelCounterTest {
    private val yt = "com.google.android.youtube"
    private val ig = "com.instagram.android"
    private val params = mapOf(yt to CounterParams(), ig to CounterParams())
    private var counter = ReelCounter(params)
    private var counted = 0
    private val events = mutableListOf<CountEvent>()

    private fun feed(s: UiSignal) {
        val out = counter.process(s)
        events += out
        counted += out.count { it is CountEvent.Counted }
    }

    private fun on(
        t: Long,
        sig: Long?,
        pkg: String = yt,
        ad: Boolean = false,
    ) = feed(Frame(t, pkg, signalA = true, signalB = true, signalC = false, signature = sig, isAd = ad))

    private fun off(
        t: Long,
        pkg: String = yt,
        scrolling: Boolean = false,
    ) = feed(Frame(t, pkg, signalA = false, signalB = false, signalC = false, pagerScrolling = scrolling))

    /** Swipe to [sig] at [t] and let it settle (event, then another event after settle time). */
    private fun swipe(
        t: Long,
        sig: Long,
        pkg: String = yt,
    ) {
        on(t, sig, pkg)
        on(t + 900, sig, pkg)
    }

    @Test
    fun `first item counted once on entry`() {
        on(0, 1)
        on(100, 1)
        on(2000, 1)
        counted shouldBe 1
    }

    @Test
    fun `swiping through ten shorts counts ten`() {
        on(0, 1)
        for (i in 2L..10L) swipe(i * 2000, i)
        counted shouldBe 10
    }

    @Test
    fun `swipe up then back down counts only one more`() {
        on(0, 1)
        swipe(2000, 2)
        swipe(4000, 1)
        counted shouldBe 2
    }

    @Test
    fun `jitter and bounce back are not counted`() {
        on(0, 1)
        on(2000, 2)
        on(2300, 1) // bounced back before settling
        on(3500, 1)
        counted shouldBe 1
    }

    @Test
    fun `fast skipping is counted when each item settles`() {
        on(0, 1)
        swipe(1000, 2)
        swipe(2000, 3)
        counted shouldBe 3
    }

    @Test
    fun `settling is completed by a tick without further UI events`() {
        on(0, 1)
        on(2000, 2)
        counter.nextDeadline() shouldBe 2800L
        feed(Tick(2800))
        counted shouldBe 2
        counter.nextDeadline() shouldBe null
    }

    @Test
    fun `null signature (comments open, rotation) changes nothing`() {
        on(0, 1)
        on(1000, null)
        on(2000, null)
        on(3000, 1)
        counted shouldBe 1
    }

    @Test
    fun `leaving and returning to the same item within 10 s does not recount`() {
        on(0, 1)
        off(1000)
        on(5000, 1)
        counted shouldBe 1
    }

    @Test
    fun `returning after more than 10 s counts again`() {
        on(0, 1)
        off(1000)
        on(20_000, 1)
        counted shouldBe 2
    }

    @Test
    fun `returning to a different item counts it`() {
        on(0, 1)
        off(1000)
        on(5000, 2)
        counted shouldBe 2
    }

    @Test
    fun `only the last 5 signatures are remembered`() {
        on(0, 1)
        for (i in 2L..8L) swipe(i * 2000, i)
        counted shouldBe 8
        swipe(40_000, 1) // 1 fell out of the window of 5
        counted shouldBe 9
        swipe(42_000, 8) // 8 is recent
        counted shouldBe 9
    }

    @Test
    fun `ads count like any other item`() {
        on(0, 1)
        on(2000, 2, ad = true)
        on(3000, 2, ad = true)
        counted shouldBe 2
    }

    @Test
    fun `needs score threshold, a single signal is not enough`() {
        feed(Frame(0, yt, signalA = true, signalB = false, signalC = false, signature = 1))
        counted shouldBe 0
        counter.isActive(yt) shouldBe false
    }

    @Test
    fun `structural fallback activates on signal B alone`() {
        counter.structuralFallback = true
        feed(Frame(0, yt, signalA = false, signalB = true, signalC = false, signature = 1))
        counted shouldBe 1
    }

    @Test
    fun `leaving the app emits exit and nothing is counted outside`() {
        on(0, 1)
        feed(AppLeft(500, yt))
        events.last() shouldBe CountEvent.SurfaceExited(500, yt)
        off(1000)
        counted shouldBe 1
    }

    @Test
    fun `apps are independent`() {
        on(0, 1, yt)
        on(0, 7, ig)
        swipe(2000, 8, ig)
        counted shouldBe 3
    }

    @Test
    fun `unknown package is ignored`() {
        feed(Frame(0, "com.other", signalA = true, signalB = true, signalC = true, signature = 1))
        events.shouldBeEmpty()
    }

    @Test
    fun `flags detection outdated after 2 minutes of pager scrolling without activation`() {
        off(0, scrolling = true)
        off(60_000, scrolling = true)
        events.none { it is CountEvent.OutdatedChanged } shouldBe true
        off(125_000, scrolling = true)
        events.last() shouldBe CountEvent.OutdatedChanged(125_000, yt, true)
        counter.isOutdated(yt) shouldBe true
        on(130_000, 1)
        counter.isOutdated(yt) shouldBe false
    }

    @Test
    fun `no outdated flag without pager scrolling or when detection works`() {
        off(0)
        off(200_000)
        events.none { it is CountEvent.OutdatedChanged } shouldBe true
    }
}
