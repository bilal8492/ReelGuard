package com.reelguard.data.repo

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import kotlinx.coroutines.tasks.await

/**
 * Fetches the selector JSON from Firebase Remote Config (parameter [KEY]). Only usable when the app was built with a
 * google-services.json; [create] returns the no-op source otherwise, so the app works fully offline without Firebase.
 */
class FirebaseSelectorSource : RemoteSelectorSource {
    override suspend fun fetch(): String? {
        val rc = Firebase.remoteConfig
        rc.setConfigSettingsAsync(remoteConfigSettings { minimumFetchIntervalInSeconds = MIN_FETCH_SECONDS }).await()
        rc.fetchAndActivate().await()
        return rc.getString(KEY).ifBlank { null }
    }

    companion object {
        const val KEY = "selectors_json"
        const val MIN_FETCH_SECONDS = 3600L

        fun create(context: Context): RemoteSelectorSource =
            if (FirebaseApp.getApps(context).isEmpty()) NoRemoteSelectorSource() else FirebaseSelectorSource()
    }
}
