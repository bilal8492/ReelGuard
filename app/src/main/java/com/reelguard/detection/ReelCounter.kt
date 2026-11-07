package com.reelguard.detection

/**
 * Pure-Kotlin counting engine: a stream of [UiSignal] in, [CountEvent]s out. No Android dependencies.
 *
 * Rules (see spec 5.1): first item counted once on entry; later items counted after settling >= settleMs;
 * items among the last [CounterParams.recentSize] signatures are never recounted; re-entering the same item within
 * reenterWindowMs is not recounted; ads count only when [countAds].
 */
class ReelCounter(
    private var params: Map<String, CounterParams>,
    var countAds: Boolean = false,
    var structuralFallback: Boolean = false,
) {
    private class State {
        var active = false
        var awaitingFirst = false
        var current: Long? = null
        var pending: Long? = null
        var pendingSince = 0L
        var pendingAd = false
        val recent = ArrayDeque<Long>()
        var exitTime = Long.MIN_VALUE
        var foregroundSince = -1L
        var activatedSinceForeground = false
        var pagerScrollSeen = false
        var outdated = false
    }

    private val states = HashMap<String, State>()

    fun updateParams(newParams: Map<String, CounterParams>) {
        params = newParams
    }

    fun isActive(pkg: String): Boolean = states[pkg]?.active == true

    fun isOutdated(pkg: String): Boolean = states[pkg]?.outdated == true

    /** Earliest time at which a [Tick] could settle a pending item, or null. The caller schedules one wake-up for it. */
    fun nextDeadline(): Long? =
        states.entries.mapNotNull { (pkg, st) ->
            val p = params[pkg]
            if (st.active && st.pending != null && p != null) st.pendingSince + p.settleMs else null
        }.minOrNull()

    fun process(signal: UiSignal): List<CountEvent> {
        val out = ArrayList<CountEvent>(2)
        when (signal) {
            is Frame -> onFrame(signal, out)
            is AppLeft -> states[signal.pkg]?.let { leave(it, signal.pkg, signal.timeMs, out) }
            is Tick ->
                states.forEach { (pkg, st) ->
                    val p = params[pkg] ?: return@forEach
                    settleIfDue(st, pkg, p, signal.timeMs, out)
                    checkOutdated(st, pkg, p, signal.timeMs, out)
                }
        }
        return out
    }

    private fun onFrame(
        f: Frame,
        out: MutableList<CountEvent>,
    ) {
        val p = params[f.pkg] ?: return
        val st = states.getOrPut(f.pkg) { State() }
        if (st.foregroundSince < 0) st.foregroundSince = f.timeMs
        val score = listOf(f.signalA, f.signalB, f.signalC).count { it }
        val surfaceActive = score >= p.threshold || (structuralFallback && f.signalB)
        if (!surfaceActive) {
            leave(st, f.pkg, f.timeMs, out, keepForeground = true)
            if (f.pagerScrolling) st.pagerScrollSeen = true
            checkOutdated(st, f.pkg, p, f.timeMs, out)
            return
        }
        if (!st.active) {
            st.active = true
            st.awaitingFirst = true
            st.pending = null
            st.activatedSinceForeground = true
            out += CountEvent.SurfaceEntered(f.timeMs, f.pkg)
            if (st.outdated) {
                st.outdated = false
                out += CountEvent.OutdatedChanged(f.timeMs, f.pkg, false)
            }
        }
        val sig = f.signature ?: return
        if (st.awaitingFirst) {
            st.awaitingFirst = false
            val resumed = f.timeMs - st.exitTime <= p.reenterWindowMs && sig in st.recent
            st.current = sig
            register(st, sig, f.isAd, f.pkg, f.timeMs, p, out, countIt = !resumed)
            return
        }
        when {
            sig == st.current -> st.pending = null // bounce-back, loop or the same item
            sig != st.pending -> {
                st.pending = sig
                st.pendingSince = f.timeMs
                st.pendingAd = f.isAd
            }
            else -> settleIfDue(st, f.pkg, p, f.timeMs, out)
        }
    }

    private fun settleIfDue(
        st: State,
        pkg: String,
        p: CounterParams,
        now: Long,
        out: MutableList<CountEvent>,
    ) {
        val pending = st.pending ?: return
        if (!st.active || now - st.pendingSince < p.settleMs) return
        st.pending = null
        st.current = pending
        register(st, pending, st.pendingAd, pkg, now, p, out, countIt = pending !in st.recent)
    }

    private fun register(
        st: State,
        sig: Long,
        isAd: Boolean,
        pkg: String,
        now: Long,
        p: CounterParams,
        out: MutableList<CountEvent>,
        countIt: Boolean,
    ) {
        if (sig !in st.recent) {
            st.recent.addLast(sig)
            while (st.recent.size > p.recentSize) st.recent.removeFirst()
        }
        if (countIt && (!isAd || countAds)) out += CountEvent.Counted(now, pkg)
    }

    private fun leave(
        st: State,
        pkg: String,
        now: Long,
        out: MutableList<CountEvent>,
        keepForeground: Boolean = false,
    ) {
        if (st.active) {
            st.active = false
            st.pending = null
            st.exitTime = now
            out += CountEvent.SurfaceExited(now, pkg)
        }
        if (!keepForeground) {
            st.foregroundSince = -1
            st.activatedSinceForeground = false
            st.pagerScrollSeen = false
        }
    }

    private fun checkOutdated(
        st: State,
        pkg: String,
        p: CounterParams,
        now: Long,
        out: MutableList<CountEvent>,
    ) {
        if (st.outdated || st.activatedSinceForeground) return
        if (!st.pagerScrollSeen || st.foregroundSince < 0) return
        if (now - st.foregroundSince >= p.outdatedAfterMs) {
            st.outdated = true
            out += CountEvent.OutdatedChanged(now, pkg, true)
        }
    }
}
