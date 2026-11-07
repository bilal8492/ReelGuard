package com.reelguard.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.reelguard.MainActivity
import com.reelguard.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Notifier
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        init {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val nm = context.getSystemService(NotificationManager::class.java)
                nm.createNotificationChannel(
                    NotificationChannel(CH_HEALTH, context.getString(R.string.channel_health), NotificationManager.IMPORTANCE_HIGH),
                )
                nm.createNotificationChannel(
                    NotificationChannel(CH_WARN, context.getString(R.string.channel_warn), NotificationManager.IMPORTANCE_DEFAULT),
                )
            }
        }

        private fun canNotify() =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

        private fun open(
            intent: Intent,
            code: Int,
        ) = PendingIntent.getActivity(context, code, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        @Suppress("MissingPermission")
        fun postServiceStopped() {
            if (!canNotify()) return
            val n =
                NotificationCompat.Builder(context, CH_HEALTH)
                    .setSmallIcon(R.drawable.ic_stat)
                    .setContentTitle(context.getString(R.string.notif_stopped_title))
                    .setContentText(context.getString(R.string.notif_stopped_text))
                    .setContentIntent(open(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS), 1))
                    .setAutoCancel(true)
                    .build()
            NotificationManagerCompat.from(context).notify(ID_STOPPED, n)
        }

        fun cancelServiceStopped() = NotificationManagerCompat.from(context).cancel(ID_STOPPED)

        @Suppress("MissingPermission")
        fun postWarn(
            count: Int,
            limit: Int,
        ) {
            if (!canNotify()) return
            val n =
                NotificationCompat.Builder(context, CH_WARN)
                    .setSmallIcon(R.drawable.ic_stat)
                    .setContentTitle(context.getString(R.string.notif_warn_title))
                    .setContentText(context.getString(R.string.notif_warn_text, count, limit))
                    .setContentIntent(open(Intent(context, MainActivity::class.java), 2))
                    .setAutoCancel(true)
                    .build()
            NotificationManagerCompat.from(context).notify(ID_WARN, n)
        }

        companion object {
            const val CH_HEALTH = "health"
            const val CH_WARN = "warn"
            const val ID_STOPPED = 100
            const val ID_WARN = 101
        }
    }
