package com.reelguard.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.healthStore by preferencesDataStore("health")

data class HealthState(
    /** The service reported onServiceConnected and has not reported being stopped since. */
    val serviceConnected: Boolean = false,
    val lastConnectedAt: Long = 0,
    /** Apps whose selectors seem outdated ("detection may be outdated"). */
    val outdatedApps: Set<String> = emptySet(),
    val stopNotified: Boolean = false,
    val warnedDay: String = "",
)

@Singleton
class HealthRepository
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val store = context.healthStore
        private val connected = booleanPreferencesKey("connected")
        private val connectedAt = longPreferencesKey("connected_at")
        private val outdated = stringSetPreferencesKey("outdated")
        private val stopNotified = booleanPreferencesKey("stop_notified")
        private val warnedDay = stringPreferencesKey("warned_day")

        val state: Flow<HealthState> =
            store.data.map {
                HealthState(
                    serviceConnected = it[connected] ?: false,
                    lastConnectedAt = it[connectedAt] ?: 0,
                    outdatedApps = it[outdated] ?: emptySet(),
                    stopNotified = it[stopNotified] ?: false,
                    warnedDay = it[warnedDay] ?: "",
                )
            }.distinctUntilChanged()

        suspend fun current() = state.first()

        suspend fun setConnected(
            value: Boolean,
            now: Long,
        ) = store.edit {
            it[connected] = value
            if (value) {
                it[connectedAt] = now
                it[stopNotified] = false
            }
        }

        suspend fun setOutdated(
            pkg: String,
            isOutdated: Boolean,
        ) = store.edit {
            val cur = it[outdated] ?: emptySet()
            it[outdated] = if (isOutdated) cur + pkg else cur - pkg
        }

        /** Forgets every "detection may be outdated" flag. The detector starts each process with a clean slate. */
        suspend fun clearOutdated() = store.edit { it.remove(outdated) }

        suspend fun setStopNotified(v: Boolean) = store.edit { it[stopNotified] = v }

        suspend fun setWarnedDay(day: String) = store.edit { it[warnedDay] = day }
    }
