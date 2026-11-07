package com.reelguard.detection

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.reelguard.BuildConfig
import com.reelguard.data.datastore.Apps
import com.reelguard.ui.overlay.BlockOverlay
import com.reelguard.ui.overlay.CounterOverlay
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

/**
 * Event-driven detection. Events from non-monitored packages are dropped immediately (except window changes, which tell us the
 * monitored app left the foreground). Scans are throttled to at most one per [SCAN_DELAY_MS].
 */
@AndroidEntryPoint
class ReelGuardService : AccessibilityService() {
    @Inject lateinit var engine: DetectionEngine

    private val detector = SurfaceDetector()
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var overlay: BlockOverlay
    private lateinit var counter: CounterOverlay

    private var scanScheduled = false
    private var scrollSeen = false
    private var lastForeground = ""

    private val scanRunnable =
        Runnable {
            scanScheduled = false
            scan()
        }

    override fun onServiceConnected() {
        super.onServiceConnected()
        overlay = BlockOverlay(this)
        counter = CounterOverlay(this)
        scope.launch {
            engine.counterModel.collect { model -> if (model == null) counter.hide() else counter.show(model) }
        }
        engine.onServiceConnected()
        scope.launch {
            engine.overlayModel.collect { model ->
                if (model == null) {
                    overlay.hide()
                } else {
                    overlay.show(
                        model,
                        onHome = {
                            performGlobalAction(GLOBAL_ACTION_HOME)
                            overlay.hide()
                        },
                        onSnooze = {
                            engine.snooze()
                            overlay.hide()
                        },
                        onFallbackNeeded = {
                            performGlobalAction(GLOBAL_ACTION_BACK)
                            performGlobalAction(GLOBAL_ACTION_HOME)
                        },
                    )
                }
            }
        }
        scope.launch {
            engine.dumpRequests.collect { delayMs ->
                delay(delayMs)
                dumpNow()
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return
        val type = event.eventType
        val monitored = pkg in engine.monitoredPackages
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && !monitored) return
        if (type == AccessibilityEvent.TYPE_VIEW_SCROLLED && isVertical(event)) scrollSeen = true
        if (BuildConfig.DEBUG && type == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            val dy = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) event.scrollDeltaY else 0
            Log.d(TAG, "scroll from=${event.fromIndex} to=${event.toIndex} count=${event.itemCount} dy=$dy")
        }
        scheduleScan()
    }

    private fun isVertical(e: AccessibilityEvent): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.P || abs(e.scrollDeltaY) >= abs(e.scrollDeltaX)

    private fun scheduleScan() {
        if (scanScheduled) return
        scanScheduled = true
        handler.postDelayed(scanRunnable, SCAN_DELAY_MS)
    }

    private fun scan() {
        val root = rootInActiveWindow ?: return
        try {
            val pkg = root.packageName?.toString().orEmpty()
            val now = SystemClock.elapsedRealtime()
            if (pkg != lastForeground) {
                if (lastForeground in engine.monitoredPackages) engine.send(AppLeft(now, lastForeground))
                lastForeground = pkg
                engine.reportForeground(pkg)
            }
            if (pkg !in engine.monitoredPackages) return
            val entry = engine.entryFor(pkg) ?: return
            val (frame, debug) = detector.scan(root, entry, pkg, now, scrollSeen)
            scrollSeen = false
            engine.reportScan(pkg, debug, pkg)
            if (BuildConfig.DEBUG) {
                // Structure only: booleans, counts and a hash. Never any text.
                Log.d(
                    TAG,
                    "scan ${pkg.substringAfterLast('.')} A=${debug.a} B=${debug.b} C=${debug.c} pager=${debug.pagerFound} " +
                        "act=${debug.actionMatches} ids=${debug.idMatches} sig=${debug.signature?.let { Hashing.short(it) }} " +
                        "src=${debug.signatureSource} nodes=${debug.nodes} ms=${debug.scanMs}",
                )
            }
            engine.send(frame)
        } finally {
            root.recycleCompat()
        }
    }

    private fun dumpNow() {
        val root =
            rootInActiveWindow ?: run {
                engine.setDump(null)
                return
            }
        try {
            val pkg = root.packageName?.toString().orEmpty()
            if (pkg !in Apps.all) {
                engine.setDump("not a monitored app in foreground: $pkg")
                return
            }
            engine.setDump(engine.dumper.dump(root, pkg, engine.safeLabels(pkg)).absolutePath)
        } finally {
            root.recycleCompat()
        }
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        shutdown()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        shutdown()
        super.onDestroy()
    }

    private var stopped = false

    private fun shutdown() {
        if (stopped) return
        stopped = true
        handler.removeCallbacks(scanRunnable)
        if (::overlay.isInitialized) overlay.hide()
        if (::counter.isInitialized) counter.hide()
        scope.cancel()
        engine.onServiceStopped()
    }

    companion object {
        const val SCAN_DELAY_MS = 120L
        private const val TAG = "RG"
    }
}
