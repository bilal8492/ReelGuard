package com.reelguard.data.repo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Emits whenever the wall clock may have jumped or the phone was woken: date/time/timezone changes, screen on, unlock.
 *
 * Coroutine `delay()` runs on the uptime clock, which stops while the phone is in deep sleep, so a timer alone can miss midnight
 * entirely (the old day's counts, the block screen and the widget would then stay stuck until enough awake time passed).
 * Anything that depends on "which day is it" re-reads the real clock on every one of these events.
 */
@Singleton
class ClockEvents
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        val events: SharedFlow<Unit> = _events

        private var registered = false

        @Synchronized
        fun start() {
            if (registered) return
            registered = true
            val filter =
                IntentFilter().apply {
                    addAction(Intent.ACTION_DATE_CHANGED)
                    addAction(Intent.ACTION_TIME_CHANGED)
                    addAction(Intent.ACTION_TIMEZONE_CHANGED)
                    addAction(Intent.ACTION_SCREEN_ON)
                    addAction(Intent.ACTION_USER_PRESENT)
                }
            val receiver =
                object : BroadcastReceiver() {
                    override fun onReceive(
                        context: Context,
                        intent: Intent,
                    ) {
                        _events.tryEmit(Unit)
                    }
                }
            ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        }

        /** For callers that learn about a clock change another way (the reset alarm). */
        fun notifyChanged() {
            _events.tryEmit(Unit)
        }
    }
