package com.reelguard.data.repo

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.reelguard.BuildConfig
import com.reelguard.data.datastore.SettingsRepository
import com.reelguard.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Anonymous usage statistics (Google Analytics for Firebase): how many people use the app and roughly where from.
 *
 * Only Firebase's automatic events are sent (first open, sessions, app version, device model and Android version, and the
 * country Google derives from the IP address). Never counts, settings or anything on screen. The advertising ID and ad
 * personalisation are switched off in the manifest, and consent for ad storage and ad data is always denied here.
 *
 * The user's "Share anonymous usage statistics" setting (on by default) decides whether anything is collected, and it is
 * applied at every start. Nothing is collected in debug builds, so a developer's own test runs do not pollute the numbers.
 * Builds without a google-services.json have no Firebase app, so this does nothing there.
 */
@Singleton
class UsageStats
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val settings: SettingsRepository,
        @ApplicationScope private val scope: CoroutineScope,
    ) {
        private var started = false
        private var lastEnabled: Boolean? = null

        @Synchronized
        fun start() {
            if (started) return
            started = true
            scope.launch { settings.settings.map { it.shareUsage }.distinctUntilChanged().collect(::applyChoice) }
        }

        private fun applyChoice(shareUsage: Boolean) {
            if (FirebaseApp.getApps(context).isEmpty()) return
            val enabled = shareUsage && !BuildConfig.DEBUG
            val analytics = FirebaseAnalytics.getInstance(context)
            analytics.setAnalyticsCollectionEnabled(enabled)
            analytics.setConsent(
                mapOf(
                    FirebaseAnalytics.ConsentType.ANALYTICS_STORAGE to status(enabled),
                    FirebaseAnalytics.ConsentType.AD_STORAGE to FirebaseAnalytics.ConsentStatus.DENIED,
                    FirebaseAnalytics.ConsentType.AD_USER_DATA to FirebaseAnalytics.ConsentStatus.DENIED,
                    FirebaseAnalytics.ConsentType.AD_PERSONALIZATION to FirebaseAnalytics.ConsentStatus.DENIED,
                ),
            )
            // Turned off by the user: drop anything not yet uploaded and forget the random app-instance ID.
            if (!enabled && lastEnabled == true) analytics.resetAnalyticsData()
            lastEnabled = enabled
        }

        private fun status(enabled: Boolean) =
            if (enabled) FirebaseAnalytics.ConsentStatus.GRANTED else FirebaseAnalytics.ConsentStatus.DENIED
    }
