package com.reelguard.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context

/** Asks the launcher to pin the ReelGuard widget. The user confirms in a system dialog. Returns false if unsupported. */
fun requestPinWidget(context: Context): Boolean {
    val manager = AppWidgetManager.getInstance(context)
    if (!manager.isRequestPinAppWidgetSupported) return false
    return manager.requestPinAppWidget(ComponentName(context, ReelGuardWidgetReceiver::class.java), null, null)
}
