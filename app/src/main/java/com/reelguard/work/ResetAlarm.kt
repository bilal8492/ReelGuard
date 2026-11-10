package com.reelguard.work

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.reelguard.data.datastore.SettingsRepository
import com.reelguard.data.repo.ClockEvents
import com.reelguard.di.ApplicationScope
import com.reelguard.domain.DayKey
import com.reelguard.domain.ResetDay
import com.reelguard.domain.WidgetRefresher
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A wall-clock alarm at the next reset boundary. WorkManager delays can slip by hours in Doze, which is why the widget and the
 * block screen used to stay on "limit reached" after midnight. `setAndAllowWhileIdle` needs no special permission.
 */
@Singleton
class ResetAlarm
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        fun schedule(resetMinutes: Int) {
            val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val at = DayKey.nextReset(System.currentTimeMillis(), resetMinutes) + 500L
            runCatching { alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pendingIntent()) }
        }

        private fun pendingIntent(): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, ClockReceiver::class.java).setAction(ACTION_RESET),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        companion object {
            const val ACTION_RESET = "com.reelguard.action.RESET_DAY"
        }
    }

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ClockReceiverEntryPoint {
    fun resetDay(): ResetDay

    fun clock(): ClockEvents

    fun settings(): SettingsRepository

    fun alarm(): ResetAlarm

    fun widget(): WidgetRefresher

    @ApplicationScope
    fun scope(): CoroutineScope
}

/** Handles the reset alarm and system clock/boot/update broadcasts: refresh counts + widget, then re-arm the alarm. */
class ClockReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val ep = EntryPointAccessors.fromApplication(context.applicationContext, ClockReceiverEntryPoint::class.java)
        val pending = goAsync()
        val action = intent.action
        ep.scope().launch {
            try {
                if (action == ResetAlarm.ACTION_RESET) ep.resetDay()(System.currentTimeMillis()) else ep.widget().request(force = true)
                ep.clock().notifyChanged()
                ep.alarm().schedule(ep.settings().current().resetMinutes)
            } finally {
                pending.finish()
            }
        }
    }
}
