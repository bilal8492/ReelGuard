package com.reelguard.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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

private val Context.settingsStore by preferencesDataStore("settings")

internal object Keys {
    val combinedLimit = intPreferencesKey("combined_limit")
    val perAppLimits = stringPreferencesKey("per_app_limits")
    val resetMinutes = intPreferencesKey("reset_minutes")
    val warnAt = intPreferencesKey("warn_at")
    val monitoredApps = stringSetPreferencesKey("monitored_apps")
    val structuralFallback = booleanPreferencesKey("structural_fallback")
    val avgDuration = intPreferencesKey("avg_duration_seconds")
    val onboardingDone = booleanPreferencesKey("onboarding_done")
    val onboardingConsent = booleanPreferencesKey("onboarding_consent")
    val blockingEnabled = booleanPreferencesKey("blocking_enabled")
    val snoozeEnabled = booleanPreferencesKey("snooze_enabled")
    val snoozeUntil = longPreferencesKey("snooze_until")
    val snoozeUsedDay = stringPreferencesKey("snooze_used_day")
    val limitRaisedDay = stringPreferencesKey("limit_raised_day")
    val theme = stringPreferencesKey("theme")
    val notifyWarn = booleanPreferencesKey("notify_warn")
    val notifyServiceStop = booleanPreferencesKey("notify_service_stop")
    val showCounter = booleanPreferencesKey("show_counter")
    val widgetPromptDismissed = booleanPreferencesKey("widget_prompt_dismissed")
    val shareUsage = booleanPreferencesKey("share_usage")
}

private const val MAX_INITIAL_LIMIT = 999

@Singleton
class SettingsRepository
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val store = context.settingsStore

        val settings: Flow<Settings> = store.data.map { it.toSettings() }.distinctUntilChanged()

        suspend fun current(): Settings = settings.first()

        private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
            store.edit { block(it) }
        }

        suspend fun setOnboardingDone(done: Boolean) = edit { it[Keys.onboardingDone] = done }

        /** Remembers the accepted disclosure and the limit picked so far, so setup can resume at the permission step. */
        suspend fun setOnboardingConsent(limit: Int) =
            edit {
                it[Keys.onboardingConsent] = true
                it[Keys.combinedLimit] = limit.coerceIn(1, MAX_INITIAL_LIMIT)
            }

        /** The limit picked during setup. Unlike [setCombinedLimit] it is not a "raise", so it does not use up the daily raise. */
        suspend fun setInitialLimit(limit: Int) = edit { it[Keys.combinedLimit] = limit.coerceIn(1, MAX_INITIAL_LIMIT) }

        suspend fun setResetMinutes(minutes: Int) = edit { it[Keys.resetMinutes] = minutes.coerceIn(0, 1439) }

        suspend fun setWarnAt(percent: Int) = edit { it[Keys.warnAt] = percent.coerceIn(0, 100) }

        suspend fun setMonitored(
            pkg: String,
            enabled: Boolean,
        ) = edit {
            val cur = it[Keys.monitoredApps] ?: Apps.all.toSet()
            it[Keys.monitoredApps] = if (enabled) cur + pkg else cur - pkg
        }

        suspend fun setStructuralFallback(v: Boolean) = edit { it[Keys.structuralFallback] = v }

        suspend fun setAvgDuration(seconds: Int) = edit { it[Keys.avgDuration] = seconds.coerceIn(1, 600) }

        suspend fun setBlockingEnabled(v: Boolean) = edit { it[Keys.blockingEnabled] = v }

        suspend fun setSnoozeEnabled(v: Boolean) = edit { it[Keys.snoozeEnabled] = v }

        suspend fun setTheme(t: ThemeMode) = edit { it[Keys.theme] = t.name }

        suspend fun setNotifyWarn(v: Boolean) = edit { it[Keys.notifyWarn] = v }

        suspend fun setNotifyServiceStop(v: Boolean) = edit { it[Keys.notifyServiceStop] = v }

        suspend fun setShowCounter(v: Boolean) = edit { it[Keys.showCounter] = v }

        suspend fun setWidgetPromptDismissed(v: Boolean) = edit { it[Keys.widgetPromptDismissed] = v }

        suspend fun setShareUsage(v: Boolean) = edit { it[Keys.shareUsage] = v }

        /**
         * Sets the combined limit. Lowering is always allowed; raising is allowed at most once per [dayKey].
         * Returns false if a raise was refused. The UI is responsible for the wait/phrase friction before calling.
         */
        suspend fun setCombinedLimit(
            newLimit: Int,
            dayKey: String,
        ): Boolean {
            val cur = current()
            val v = newLimit.coerceAtLeast(1)
            if (v > cur.combinedLimit) {
                if (cur.limitRaisedDay == dayKey && cur.frictionMode != FrictionMode.NONE) return false
                edit {
                    it[Keys.combinedLimit] = v
                    it[Keys.limitRaisedDay] = dayKey
                }
            } else {
                edit { it[Keys.combinedLimit] = v }
            }
            return true
        }

        /** Per-app cap; null removes it. Raising or removing counts as a raise (once per day). */
        suspend fun setAppLimit(
            pkg: String,
            limit: Int?,
            dayKey: String,
        ): Boolean {
            val cur = current()
            val old = cur.perAppLimits[pkg]
            val raising = limit == null && old != null || (limit != null && old != null && limit > old)
            if (raising && cur.limitRaisedDay == dayKey && cur.frictionMode != FrictionMode.NONE) return false
            val map = cur.perAppLimits.toMutableMap()
            if (limit == null) map.remove(pkg) else map[pkg] = limit.coerceAtLeast(1)
            edit {
                it[Keys.perAppLimits] = encodeLimits(map)
                if (raising) it[Keys.limitRaisedDay] = dayKey
            }
            return true
        }

        /** One 5-minute snooze per day. Returns false if snooze is off or already used today. */
        suspend fun snooze(
            dayKey: String,
            now: Long,
        ): Boolean {
            val cur = current()
            if (!cur.snoozeEnabled || cur.snoozeUsedDay == dayKey) return false
            edit {
                it[Keys.snoozeUsedDay] = dayKey
                it[Keys.snoozeUntil] = now + Settings.SNOOZE_MINUTES * 60_000L
            }
            return true
        }

        suspend fun clearSnooze() = edit { it[Keys.snoozeUntil] = 0L }

        companion object {
            fun encodeLimits(m: Map<String, Int>) = m.entries.joinToString(";") { "${it.key}=${it.value}" }

            fun decodeLimits(s: String?): Map<String, Int> =
                s.orEmpty().split(";").mapNotNull { part ->
                    val kv = part.split("=")
                    val v = kv.getOrNull(1)?.toIntOrNull()
                    if (kv.size == 2 && v != null && kv[0].isNotBlank()) kv[0] to v else null
                }.toMap()
        }
    }

private fun Preferences.toSettings(): Settings {
    val d = Settings()
    return Settings(
        combinedLimit = this[Keys.combinedLimit] ?: d.combinedLimit,
        perAppLimits = SettingsRepository.decodeLimits(this[Keys.perAppLimits]),
        resetMinutes = this[Keys.resetMinutes] ?: d.resetMinutes,
        warnAt = this[Keys.warnAt] ?: d.warnAt,
        monitoredApps = this[Keys.monitoredApps] ?: d.monitoredApps,
        structuralFallbackEnabled = this[Keys.structuralFallback] ?: d.structuralFallbackEnabled,
        avgDurationSeconds = this[Keys.avgDuration] ?: d.avgDurationSeconds,
        onboardingDone = this[Keys.onboardingDone] ?: d.onboardingDone,
        onboardingConsent = this[Keys.onboardingConsent] ?: d.onboardingConsent,
        blockingEnabled = this[Keys.blockingEnabled] ?: d.blockingEnabled,
        snoozeEnabled = this[Keys.snoozeEnabled] ?: d.snoozeEnabled,
        snoozeUntil = this[Keys.snoozeUntil] ?: d.snoozeUntil,
        snoozeUsedDay = this[Keys.snoozeUsedDay] ?: d.snoozeUsedDay,
        limitRaisedDay = this[Keys.limitRaisedDay] ?: d.limitRaisedDay,
        theme = enumOr(this[Keys.theme], d.theme),
        notifyWarn = this[Keys.notifyWarn] ?: d.notifyWarn,
        notifyServiceStop = this[Keys.notifyServiceStop] ?: d.notifyServiceStop,
        showCounter = this[Keys.showCounter] ?: d.showCounter,
        widgetPromptDismissed = this[Keys.widgetPromptDismissed] ?: d.widgetPromptDismissed,
        shareUsage = this[Keys.shareUsage] ?: d.shareUsage,
    )
}

private inline fun <reified E : Enum<E>> enumOr(
    name: String?,
    default: E,
): E = name?.let { n -> enumValues<E>().firstOrNull { it.name == n } } ?: default
