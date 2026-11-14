package com.reelguard.ui.guide

/** What this phone's Accessibility screen calls the list that holds downloaded apps. */
enum class AppsListLabel { INSTALLED_APPS, DOWNLOADED_APPS }

/** The three taps that turn the accessibility service on, and the small decisions around showing them. */
object GuideSteps {
    const val COUNT = 3

    /** How long each step stays highlighted before the next one, in milliseconds. */
    const val STEP_MS = 5_000L

    /** Stores that count as a trusted source: apps from them are not subject to Android's "restricted settings". */
    private val TRUSTED_INSTALLERS = setOf("com.android.vending", "com.sec.android.app.samsungapps")

    /** Samsung calls it "Installed apps"; most other phones call it "Downloaded apps" (some list the app directly). */
    fun appsListLabel(manufacturer: String): AppsListLabel =
        if (manufacturer.equals("samsung", ignoreCase = true)) AppsListLabel.INSTALLED_APPS else AppsListLabel.DOWNLOADED_APPS

    /** The step after [step], wrapping to the first so the walkthrough keeps looping while the user works. */
    fun next(step: Int): Int = (step + 1) % COUNT

    /** Android 13+ greys out the switch for an app installed from a downloaded file; a store install does not need the extra step. */
    fun needsRestrictedHint(
        sdk: Int,
        installer: String?,
    ): Boolean = sdk >= 33 && installer !in TRUSTED_INSTALLERS
}
