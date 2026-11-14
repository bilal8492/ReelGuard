package com.reelguard.ui.guide

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class GuideStepsTest {
    @Test
    fun `Samsung calls the list Installed apps`() {
        GuideSteps.appsListLabel("samsung") shouldBe AppsListLabel.INSTALLED_APPS
        GuideSteps.appsListLabel("Samsung") shouldBe AppsListLabel.INSTALLED_APPS
    }

    @Test
    fun `other makers call it Downloaded apps`() {
        listOf("Xiaomi", "Google", "OnePlus", "motorola", "").forEach {
            GuideSteps.appsListLabel(it) shouldBe AppsListLabel.DOWNLOADED_APPS
        }
    }

    @Test
    fun `the steps loop 1 to 2 to 3 and back`() {
        GuideSteps.next(0) shouldBe 1
        GuideSteps.next(1) shouldBe 2
        GuideSteps.next(2) shouldBe 0
    }

    @Test
    fun `a downloaded file on Android 13 needs the restricted settings hint`() {
        GuideSteps.needsRestrictedHint(sdk = 33, installer = null) shouldBe true
        GuideSteps.needsRestrictedHint(sdk = 37, installer = "com.google.android.packageinstaller") shouldBe true
    }

    @Test
    fun `a store install does not need the hint`() {
        GuideSteps.needsRestrictedHint(sdk = 36, installer = "com.android.vending") shouldBe false
        GuideSteps.needsRestrictedHint(sdk = 36, installer = "com.sec.android.app.samsungapps") shouldBe false
    }

    @Test
    fun `older Android never needs the hint`() {
        GuideSteps.needsRestrictedHint(sdk = 32, installer = null) shouldBe false
    }
}
