package com.reelguard.detection

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Decides when the accessibility service stopping deserves the "ReelGuard stopped working" notice.
 *
 * Android may unbind and bind the service again moments after the user switched it on, and the old instance's stop callback
 * can then arrive after the new instance has already connected. Acting on that event raised a false alarm that nothing
 * cancelled, because the reconnect had happened before it. So:
 *  - a stop from an instance that is no longer the current one is ignored ([stopped] returns false), and
 *  - a stop from the current instance only becomes a notice if no instance connects within [graceMs] and [stillOff]
 *    confirms that Android really has the service switched off.
 */
class ServiceStopWatcher(
    private val scope: CoroutineScope,
    private val graceMs: Long,
    private val stillOff: suspend () -> Boolean,
    private val onConfirmedStop: suspend () -> Unit,
) {
    private val lock = Any()
    private var current: Any? = null
    private var pending: Job? = null

    /** A service instance connected. It becomes the current one and any stop still waiting out its grace period is dropped. */
    fun connected(owner: Any) {
        synchronized(lock) {
            current = owner
            pending?.cancel()
            pending = null
        }
    }

    /** Returns false when [owner] is stale (a newer instance connected), so the caller must ignore this stop altogether. */
    fun stopped(owner: Any): Boolean =
        synchronized(lock) {
            if (current != null && current !== owner) return false
            pending?.cancel()
            pending =
                scope.launch {
                    delay(graceMs)
                    val stillCurrent = synchronized(lock) { current == null || current === owner }
                    if (stillCurrent && stillOff()) onConfirmedStop()
                }
            true
        }

    companion object {
        /** Long enough for the system to bind the service again, short enough that a real switch-off is reported promptly. */
        const val GRACE_MS = 5_000L
    }
}
