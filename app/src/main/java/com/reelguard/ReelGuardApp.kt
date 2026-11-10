package com.reelguard

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.reelguard.data.repo.ClockEvents
import com.reelguard.data.repo.UsageStats
import com.reelguard.detection.DetectionEngine
import com.reelguard.widget.GlanceWidgetRefresher
import com.reelguard.work.WorkScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ReelGuardApp : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var engine: DetectionEngine

    @Inject lateinit var scheduler: WorkScheduler

    @Inject lateinit var clock: ClockEvents

    @Inject lateinit var widgetRefresher: GlanceWidgetRefresher

    @Inject lateinit var usageStats: UsageStats

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        clock.start()
        engine.start()
        widgetRefresher.start()
        usageStats.start()
        scheduler.schedule()
    }
}
