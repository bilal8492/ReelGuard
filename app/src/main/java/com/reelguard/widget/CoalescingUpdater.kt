package com.reelguard.widget

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Runs [update] at most once per [minIntervalMs]. Requests that arrive while an update is scheduled, waiting or running
 * collapse into one trailing update, so the last request is always followed by an update within about [minIntervalMs].
 *
 * Every decision (is a job active, is a repeat needed, the job is finishing) is taken under one lock. Without that, a request
 * landing between "no repeat needed" and the job actually completing is dropped, and the last count never gets drawn.
 */
class CoalescingUpdater(
    private val scope: CoroutineScope,
    private val clock: () -> Long,
    private val minIntervalMs: Long,
    private val update: suspend () -> Unit,
) {
    private val lock = Any()
    private var job: Job? = null
    private var generation = 0
    private var lastStart = Long.MIN_VALUE / 2
    private var dirty = false

    /** Ask for an update. [force] skips the throttle window (used when the day rolls over). */
    fun request(force: Boolean = false) {
        synchronized(lock) {
            if (!force && job != null) {
                dirty = true
                return
            }
            job?.cancel()
            val wait = if (force) 0L else (lastStart + minIntervalMs - clock()).coerceAtLeast(0L)
            val gen = ++generation
            job = scope.launch { run(gen, wait) }
        }
    }

    private suspend fun run(
        gen: Int,
        wait: Long,
    ) {
        try {
            delay(wait)
            do {
                synchronized(lock) {
                    dirty = false
                    lastStart = clock()
                }
                update()
                val again =
                    synchronized(lock) {
                        // Only the current job may end the schedule; a cancelled predecessor must not clear its successor.
                        if (!dirty && generation == gen) job = null
                        dirty && generation == gen
                    }
                if (again) delay(minIntervalMs)
            } while (again)
        } finally {
            // An update that throws (or a cancellation) must not leave `job` set, or every later request would be swallowed.
            synchronized(lock) { if (generation == gen) job = null }
        }
    }
}
