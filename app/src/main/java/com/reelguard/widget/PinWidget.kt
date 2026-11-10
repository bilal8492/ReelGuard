package com.reelguard.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.widget.RemoteViews
import com.reelguard.R

/** True if at least one ReelGuard widget is on a home screen. */
fun hasWidget(context: Context): Boolean =
    AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, ReelGuardWidgetReceiver::class.java)).isNotEmpty()

/** Asks the launcher to pin the ReelGuard widget. The user confirms in a system dialog. Returns false if unsupported. */
fun requestPinWidget(context: Context): Boolean {
    val manager = AppWidgetManager.getInstance(context)
    if (!manager.isRequestPinAppWidgetSupported) return false
    return manager.requestPinAppWidget(
        ComponentName(context, ReelGuardWidgetReceiver::class.java),
        // The launcher shows this in its "Add to Home screen" dialog instead of "Couldn't add widget".
        Bundle().apply {
            putParcelable(AppWidgetManager.EXTRA_APPWIDGET_PREVIEW, RemoteViews(context.packageName, R.layout.widget_preview))
        },
        null,
    )
}
