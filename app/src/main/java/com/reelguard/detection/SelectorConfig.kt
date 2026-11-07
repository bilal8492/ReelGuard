package com.reelguard.detection

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Tunables of the counting engine for one app. */
data class CounterParams(
    val threshold: Int = 2,
    val settleMs: Long = 800,
    val recentSize: Int = 5,
    val reenterWindowMs: Long = 10_000,
    val outdatedAfterMs: Long = 120_000,
)

@Serializable
data class SelectorEntry(
    val minVersionCode: Long = 0,
    val maxVersionCode: Long = Long.MAX_VALUE,
    val isDefault: Boolean = false,
    /** Signal A: resource-id names (without the package prefix). */
    val viewIds: List<String> = emptyList(),
    /** Resource ids whose text identifies the current item (creator handle, caption). Hashed, never stored. */
    val signatureViewIds: List<String> = emptyList(),
    /** Resource ids that mark a sponsored item. */
    val adViewIds: List<String> = emptyList(),
    /** Signal B: content descriptions / texts of the right-hand action column (lower-case, contains-match). */
    val actionDescriptions: List<String> = emptyList(),
    val minActionMatches: Int = 3,
    val pagerClasses: List<String> = listOf("RecyclerView", "ViewPager"),
    val pagerFillRatio: Double = 0.85,
    /** Signal C: bottom tab labels (selected tab must match). */
    val tabLabels: List<String> = emptyList(),
    /** Signal C alternative: a node description/title containing one of these. */
    val titleKeywords: List<String> = emptyList(),
    val threshold: Int = 2,
    val settleMs: Long = 800,
    val recentSize: Int = 5,
    val reenterWindowMs: Long = 10_000,
    val outdatedAfterMs: Long = 120_000,
) {
    fun contains(versionCode: Long) = versionCode in minVersionCode..maxVersionCode

    fun toParams() = CounterParams(threshold, settleMs, recentSize, reenterWindowMs, outdatedAfterMs)
}

@Serializable
data class AppSelectors(
    val label: String,
    val entries: List<SelectorEntry>,
)

@Serializable
data class SelectorConfig(
    val schemaVersion: Int = 1,
    val notice: String = "",
    val apps: Map<String, AppSelectors> = emptyMap(),
) {
    /** The entry for an installed version: first non-default entry whose range matches, else the default entry. */
    fun entryFor(
        pkg: String,
        versionCode: Long,
    ): SelectorEntry? {
        val entries = apps[pkg]?.entries ?: return null
        return entries.firstOrNull { !it.isDefault && it.contains(versionCode) } ?: entries.firstOrNull { it.isDefault }
    }

    fun paramsFor(versionCodes: Map<String, Long>): Map<String, CounterParams> =
        apps.keys.mapNotNull { pkg ->
            entryFor(pkg, versionCodes[pkg] ?: 0L)?.let { pkg to it.toParams() }
        }.toMap()

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        /** Throws [kotlinx.serialization.SerializationException] on malformed input; callers fall back to the bundled copy. */
        fun parse(text: String): SelectorConfig {
            val cfg = json.decodeFromString(serializer(), text)
            require(cfg.apps.isNotEmpty()) { "selector config has no apps" }
            return cfg
        }
    }
}
