package com.reelguard.data.repo

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.pm.PackageInfoCompat
import com.reelguard.detection.SelectorConfig
import com.reelguard.detection.SelectorEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Source of remotely supplied selector JSON (e.g. Firebase Remote Config). The app ships with [NoRemoteSelectorSource];
 * see docs/SELECTORS.md for a Firebase implementation. Must return the raw JSON string or null.
 */
interface RemoteSelectorSource {
    suspend fun fetch(): String?
}

class NoRemoteSelectorSource : RemoteSelectorSource {
    override suspend fun fetch(): String? = null
}

@Singleton
class SelectorRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val remote: RemoteSelectorSource,
    ) {
        private val cacheFile get() = File(context.filesDir, "selectors_remote.json")
        private val _config = MutableStateFlow(loadInitial())
        val config: StateFlow<SelectorConfig> = _config.asStateFlow()

        @Volatile private var versions: Map<String, Long> = emptyMap()

        private fun bundled(): SelectorConfig =
            SelectorConfig.parse(context.assets.open("selectors.json").bufferedReader().use { it.readText() })

        private fun loadInitial(): SelectorConfig = runCatching { SelectorConfig.parse(cacheFile.readText()) }.getOrElse { bundled() }

        /** Fetches the remote config; keeps the current one when the fetch fails or the JSON is invalid. */
        suspend fun refresh() {
            val text = runCatching { remote.fetch() }.getOrNull() ?: return
            val parsed = runCatching { SelectorConfig.parse(text) }.getOrNull() ?: return
            runCatching { cacheFile.writeText(text) }
            _config.value = parsed
        }

        fun refreshVersions(): Map<String, Long> {
            val pm = context.packageManager
            versions =
                config.value.apps.keys.associateWith { pkg ->
                    try {
                        PackageInfoCompat.getLongVersionCode(pm.getPackageInfo(pkg, 0))
                    } catch (_: PackageManager.NameNotFoundException) {
                        0L
                    }
                }
            return versions
        }

        fun versionCodes(): Map<String, Long> = versions.ifEmpty { refreshVersions() }

        fun entryFor(pkg: String): SelectorEntry? = config.value.entryFor(pkg, versionCodes()[pkg] ?: 0L)
    }
