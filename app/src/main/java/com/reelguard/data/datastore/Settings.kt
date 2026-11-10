package com.reelguard.data.datastore

object Apps {
    const val YOUTUBE = "com.google.android.youtube"
    const val INSTAGRAM = "com.instagram.android"
    val all = listOf(YOUTUBE, INSTAGRAM)

    fun shortLabel(pkg: String) =
        when (pkg) {
            YOUTUBE -> "Shorts"
            INSTAGRAM -> "Reels"
            else -> pkg
        }

    fun label(pkg: String) =
        when (pkg) {
            YOUTUBE -> "YouTube Shorts"
            INSTAGRAM -> "Instagram Reels"
            else -> pkg
        }
}

enum class FrictionMode { NONE, WAIT, PHRASE }

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** All user settings. [combinedLimit] applies to the sum of monitored apps; [perAppLimits] are optional extra caps. */
data class Settings(
    val combinedLimit: Int = 50,
    val perAppLimits: Map<String, Int> = emptyMap(),
    /** Minutes after local midnight at which the day rolls over. */
    val resetMinutes: Int = 0,
    val frictionMode: FrictionMode = FrictionMode.WAIT,
    /** Percent of the limit at which to warn; 0 turns the warning off. */
    val warnAt: Int = 80,
    val monitoredApps: Set<String> = Apps.all.toSet(),
    val structuralFallbackEnabled: Boolean = false,
    val avgDurationSeconds: Int = 30,
    val onboardingDone: Boolean = false,
    val blockingEnabled: Boolean = true,
    val snoozeEnabled: Boolean = false,
    val snoozeUntil: Long = 0,
    val snoozeUsedDay: String = "",
    val limitRaisedDay: String = "",
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val notifyWarn: Boolean = true,
    val notifyServiceStop: Boolean = true,
    /** Show a small live counter over Reels/Shorts while watching. */
    val showCounter: Boolean = true,
    /** The user closed the "add the widget" card on the Today screen. */
    val widgetPromptDismissed: Boolean = false,
    /** Share anonymous usage statistics (Google Analytics for Firebase). On by default; switched off in Settings. */
    val shareUsage: Boolean = true,
    /** The user has seen the one-time "usage statistics are on" card on the Today screen. */
    val usageNoticeSeen: Boolean = false,
) {
    fun snoozeActive(now: Long) = snoozeUntil > now

    companion object {
        const val FRICTION_PHRASE = "I choose to watch more"
        const val WAIT_SECONDS = 30
        const val SNOOZE_MINUTES = 5
    }
}
