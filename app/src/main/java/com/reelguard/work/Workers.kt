package com.reelguard.work

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.reelguard.data.datastore.HealthRepository
import com.reelguard.data.datastore.SettingsRepository
import com.reelguard.data.repo.SelectorRepository
import com.reelguard.detection.ReelGuardService
import com.reelguard.di.ApplicationScope
import com.reelguard.domain.DayKey
import com.reelguard.domain.ResetDay
import com.reelguard.domain.WidgetRefresher
import com.reelguard.util.Notifier
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** True if the user has the ReelGuard accessibility service switched on in system settings. */
fun isServiceEnabled(context: Context): Boolean {
    val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
    val me = ComponentName(context, ReelGuardService::class.java).flattenToString()
    val short = ComponentName(context, ReelGuardService::class.java).flattenToShortString()
    return enabled.split(':').any { it.equals(me, true) || it.equals(short, true) }
}

@Singleton
class WorkScheduler
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val settings: SettingsRepository,
        private val alarm: ResetAlarm,
        @ApplicationScope private val scope: CoroutineScope,
    ) {
        private val wm get() = WorkManager.getInstance(context)

        fun schedule() {
            wm.enqueueUniquePeriodicWork(
                WIDGET_WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<WidgetRefreshWorker>(30, TimeUnit.MINUTES).build(),
            )
            // Re-arms the reset whenever the reset time changes (and once now).
            scope.launch { settings.settings.map { it.resetMinutes }.distinctUntilChanged().collect { scheduleReset(it) } }
        }

        /** One-shot work at the next reset boundary; the worker re-schedules itself. Call again when the reset time changes. */
        fun scheduleReset(resetMinutes: Int) {
            alarm.schedule(resetMinutes)
            val now = System.currentTimeMillis()
            val delay = (DayKey.nextReset(now, resetMinutes) - now).coerceAtLeast(1_000L)
            wm.enqueueUniqueWork(
                RESET_WORK,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<ResetWorker>().setInitialDelay(delay, TimeUnit.MILLISECONDS).build(),
            )
        }

        companion object {
            const val WIDGET_WORK = "widget_refresh"
            const val RESET_WORK = "daily_reset"
        }
    }

@HiltWorker
class ResetWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val resetDay: ResetDay,
        private val scheduler: WorkScheduler,
        private val settings: SettingsRepository,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            resetDay(System.currentTimeMillis())
            scheduler.scheduleReset(settings.settings.first().resetMinutes)
            return Result.success()
        }
    }

/** Backup widget refresh plus the service health check (posts a notification if the service was switched off). */
@HiltWorker
class WidgetRefreshWorker
    @AssistedInject
    constructor(
        @Assisted private val context: Context,
        @Assisted params: WorkerParameters,
        private val widget: WidgetRefresher,
        private val settings: SettingsRepository,
        private val health: HealthRepository,
        private val notifier: Notifier,
        private val selectors: SelectorRepository,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            widget.request(force = true)
            selectors.refresh()
            val s = settings.current()
            val wantsAlerts = s.onboardingDone && s.notifyServiceStop
            if (wantsAlerts && !isServiceEnabled(context) && !health.current().stopNotified) {
                notifier.postServiceStopped()
                health.setStopNotified(true)
            }
            return Result.success()
        }
    }
