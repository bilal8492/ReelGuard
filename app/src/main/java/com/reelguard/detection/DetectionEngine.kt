package com.reelguard.detection

import android.content.Context
import android.os.SystemClock
import com.reelguard.data.datastore.Apps
import com.reelguard.data.datastore.HealthRepository
import com.reelguard.data.datastore.Settings
import com.reelguard.data.datastore.SettingsRepository
import com.reelguard.data.repo.CountRepository
import com.reelguard.data.repo.SelectorRepository
import com.reelguard.di.ApplicationScope
import com.reelguard.domain.DayKey
import com.reelguard.domain.EvaluateLimit
import com.reelguard.domain.IncrementCount
import com.reelguard.domain.LimitState
import com.reelguard.domain.LimitStatus
import com.reelguard.domain.WidgetRefresher
import com.reelguard.util.Notifier
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class CounterModel(val appLabel: String, val appCount: Int, val total: Int, val limit: Int, val status: LimitStatus)

/** One monitored app's progress on the block screen: [count] watched out of its [cap]. */
data class OverlayApp(val pkg: String, val label: String, val count: Int, val cap: Int)

data class OverlayModel(
    val pkg: String,
    val appLabel: String,
    val watched: Int,
    val limit: Int,
    val snoozeAvailable: Boolean,
    val resetAt: Long,
    val apps: List<OverlayApp>,
)

data class AppDebug(
    val signalA: Boolean = false,
    val signalB: Boolean = false,
    val signalC: Boolean = false,
    val actionMatches: Int = 0,
    val idMatches: Int = 0,
    val pagerFound: Boolean = false,
    val signature: String = "-",
    val surfaceActive: Boolean = false,
    val outdated: Boolean = false,
    val nodes: Int = 0,
    val scanMs: Long = 0,
)

data class DebugState(
    val foregroundPkg: String = "",
    val versionCodes: Map<String, Long> = emptyMap(),
    val perApp: Map<String, AppDebug> = emptyMap(),
    val recording: Boolean = false,
    val recordedSignals: Int = 0,
    val lastDump: String? = null,
    val lastRecording: String? = null,
    val serviceConnected: Boolean = false,
)

/**
 * Glue between the AccessibilityService and the pure [ReelCounter]. Signals are queued to a single consumer coroutine so the
 * counter is only touched from one thread.
 */
@Singleton
class DetectionEngine
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val settingsRepo: SettingsRepository,
        private val countRepo: CountRepository,
        private val selectors: SelectorRepository,
        private val health: HealthRepository,
        private val incrementCount: IncrementCount,
        private val notifier: Notifier,
        private val widget: WidgetRefresher,
        @ApplicationScope private val scope: CoroutineScope,
    ) {
        private sealed interface Msg {
            data class Signal(val signal: UiSignal) : Msg

            data class Params(val params: Map<String, CounterParams>) : Msg

            data class Flags(val fallback: Boolean) : Msg
        }

        private val channel = Channel<Msg>(Channel.UNLIMITED)
        private val counter = ReelCounter(emptyMap())
        private var tickJob: Job? = null
        private var started = false

        val recorder = SignalRecorder(context)
        val dumper = NodeDumper(context)

        @Volatile var monitoredPackages: Set<String> = Apps.all.toSet()
            private set

        @Volatile var debugActive = false

        private val _active = MutableStateFlow<Set<String>>(emptySet())
        val activeSurfaces: StateFlow<Set<String>> = _active.asStateFlow()

        private val _debug = MutableStateFlow(DebugState())
        val debug: StateFlow<DebugState> = _debug.asStateFlow()

        val dumpRequests = MutableSharedFlow<Long>(extraBufferCapacity = 1)

        private val snoozeTick = MutableStateFlow(0L)

        val limitState: StateFlow<LimitState?> =
            combine(countRepo.todayCounts(), settingsRepo.settings) { counts, s ->
                EvaluateLimit.evaluate(counts, s)
            }.stateIn(scope, SharingStarted.Eagerly, null)

        /** Non-null while a monitored surface is on screen and its limit is reached: the service shows the overlay. */
        val overlayModel: StateFlow<OverlayModel?> =
            combine(_active, limitState, settingsRepo.settings, snoozeTick) { active, ls, s, _ ->
                ls ?: return@combine null
                val enforce = s.blockingEnabled && !s.snoozeActive(System.currentTimeMillis())
                val pkg = active.firstOrNull { it in ls.blockedApps }
                if (!enforce || pkg == null) return@combine null
                val watched = if (ls.total >= ls.limit) ls.total else ls.counts[pkg] ?: 0
                val limit = if (ls.total >= ls.limit) ls.limit else s.perAppLimits[pkg] ?: ls.limit
                val day = DayKey.of(System.currentTimeMillis(), s.resetMinutes)
                val nowMs = System.currentTimeMillis()
                val apps =
                    Apps.all.filter { it in s.monitoredApps }.map {
                        OverlayApp(it, Apps.shortLabel(it), ls.counts[it] ?: 0, s.perAppLimits[it] ?: ls.limit)
                    }
                OverlayModel(
                    pkg,
                    Apps.label(pkg),
                    watched,
                    limit,
                    s.snoozeEnabled && s.snoozeUsedDay != day,
                    DayKey.nextReset(nowMs, s.resetMinutes),
                    apps,
                )
            }.distinctUntilChanged().stateIn(scope, SharingStarted.Eagerly, null)

        /** Non-null while Reels/Shorts is on screen, the live counter is enabled and the block screen is not showing. */
        val counterModel: StateFlow<CounterModel?> =
            combine(_active, limitState, settingsRepo.settings, overlayModel) { active, ls, s, block ->
                val pkg = active.firstOrNull()
                val visible = s.showCounter && block == null
                if (ls == null || pkg == null || !visible) return@combine null
                CounterModel(Apps.shortLabel(pkg), ls.counts[pkg] ?: 0, ls.total, ls.limit, ls.status)
            }.distinctUntilChanged().stateIn(scope, SharingStarted.Eagerly, null)

        @Synchronized
        fun start() {
            if (started) return
            started = true
            scope.launch {
                for (msg in channel) {
                    when (msg) {
                        is Msg.Signal -> handle(msg.signal)
                        is Msg.Params -> counter.updateParams(msg.params)
                        is Msg.Flags -> {
                            counter.structuralFallback = msg.fallback
                        }
                    }
                }
            }
            scope.launch {
                settingsRepo.settings.collect { s ->
                    monitoredPackages = s.monitoredApps
                    channel.send(Msg.Flags(s.structuralFallbackEnabled))
                }
            }
            scope.launch {
                selectors.config.collect { cfg ->
                    channel.send(Msg.Params(cfg.paramsFor(selectors.refreshVersions())))
                }
            }
            scope.launch { selectors.refresh() }
            scope.launch {
                settingsRepo.settings.map { it.snoozeUntil }.distinctUntilChanged().collectLatest { until ->
                    val wait = until - System.currentTimeMillis()
                    if (wait > 0) {
                        delay(wait)
                        snoozeTick.update { it + 1 }
                    }
                }
            }
            scope.launch { health.state.collect { h -> _debug.update { it.copy(serviceConnected = h.serviceConnected) } } }
        }

        // ---- called by the service ----

        fun send(signal: UiSignal) {
            recorder.record(signal)
            channel.trySend(Msg.Signal(signal))
        }

        fun entryFor(pkg: String): SelectorEntry? = selectors.entryFor(pkg)

        fun safeLabels(pkg: String): Set<String> {
            val e = selectors.entryFor(pkg) ?: return emptySet()
            return (e.actionDescriptions + e.tabLabels + e.titleKeywords).toSet()
        }

        fun onServiceConnected() {
            start()
            scope.launch { health.setConnected(true, System.currentTimeMillis()) }
            notifier.cancelServiceStopped()
            scope.launch { selectors.refreshVersions() }
        }

        fun onServiceStopped() {
            scope.launch {
                health.setConnected(false, System.currentTimeMillis())
                channel.send(Msg.Signal(AppLeft(SystemClock.elapsedRealtime(), Apps.YOUTUBE)))
                channel.send(Msg.Signal(AppLeft(SystemClock.elapsedRealtime(), Apps.INSTAGRAM)))
                val s = settingsRepo.current()
                val h = health.current()
                if (s.onboardingDone && s.notifyServiceStop && !h.stopNotified) {
                    notifier.postServiceStopped()
                    health.setStopNotified(true)
                }
            }
        }

        fun reportScan(
            pkg: String,
            d: ScanDebug,
            foreground: String,
        ) {
            if (!debugActive) return
            _debug.update { st ->
                val prev = st.perApp[pkg] ?: AppDebug()
                st.copy(
                    foregroundPkg = foreground,
                    versionCodes = selectors.versionCodes(),
                    perApp =
                        st.perApp + (
                            pkg to
                                prev.copy(
                                    signalA = d.a, signalB = d.b, signalC = d.c, actionMatches = d.actionMatches, idMatches = d.idMatches,
                                    pagerFound = d.pagerFound, nodes = d.nodes, scanMs = d.scanMs,
                                    signature = d.signature?.let { Hashing.short(it) } ?: "-",
                                )
                        ),
                    recording = recorder.recording,
                    recordedSignals = recorder.size,
                )
            }
        }

        fun reportForeground(pkg: String) {
            if (debugActive) _debug.update { it.copy(foregroundPkg = pkg) }
        }

        fun snooze() {
            scope.launch {
                val now = System.currentTimeMillis()
                val s = settingsRepo.current()
                settingsRepo.snooze(DayKey.of(now, s.resetMinutes), now)
            }
        }

        // ---- debug / calibration ----

        fun requestDump(delayMs: Long) {
            dumpRequests.tryEmit(delayMs)
        }

        fun setDump(path: String?) = _debug.update { it.copy(lastDump = path) }

        fun startRecording() {
            recorder.start()
            _debug.update { it.copy(recording = true, recordedSignals = 0, lastRecording = null) }
        }

        fun stopRecording() {
            val f = recorder.stop()
            _debug.update { it.copy(recording = false, recordedSignals = 0, lastRecording = f?.absolutePath) }
        }

        // ---- consumer ----

        private suspend fun handle(signal: UiSignal) {
            val events = counter.process(signal)
            for (e in events) onEvent(e)
            if (debugActive) {
                _debug.update { st ->
                    st.copy(
                        perApp =
                            st.perApp.mapValues { (pkg, d) ->
                                d.copy(surfaceActive = counter.isActive(pkg), outdated = counter.isOutdated(pkg))
                            },
                        recordedSignals = recorder.size,
                    )
                }
            }
            scheduleTick()
        }

        private suspend fun onEvent(e: CountEvent) {
            when (e) {
                is CountEvent.Counted -> onCounted(e)
                is CountEvent.SurfaceEntered -> _active.update { it + e.pkg }
                is CountEvent.SurfaceExited -> _active.update { it - e.pkg }
                is CountEvent.OutdatedChanged -> health.setOutdated(e.pkg, e.outdated)
            }
        }

        private suspend fun onCounted(e: CountEvent.Counted) {
            val now = System.currentTimeMillis()
            val before = limitState.value?.status
            val state = incrementCount(e.pkg, now)
            widget.request()
            val s = settingsRepo.current()
            if (state.status == LimitStatus.WARN && before != LimitStatus.WARN && s.notifyWarn) maybeWarn(s, state, now)
        }

        private suspend fun maybeWarn(
            s: Settings,
            state: LimitState,
            now: Long,
        ) {
            val day = DayKey.of(now, s.resetMinutes)
            if (health.current().warnedDay != day) {
                health.setWarnedDay(day)
                notifier.postWarn(state.total, state.limit)
            }
        }

        private fun scheduleTick() {
            tickJob?.cancel()
            val deadline = counter.nextDeadline() ?: return
            tickJob =
                scope.launch {
                    delay((deadline - SystemClock.elapsedRealtime()).coerceAtLeast(0L) + 5L)
                    channel.send(Msg.Signal(Tick(SystemClock.elapsedRealtime())))
                }
        }
    }
