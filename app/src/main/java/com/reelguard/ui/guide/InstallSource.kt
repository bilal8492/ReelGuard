package com.reelguard.ui.guide

import android.content.Context
import android.os.Build

/** The package that installed this app (for example the Play Store), or null if unknown or installed from a file. */
fun Context.installerPackage(): String? =
    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            packageManager.getInstallSourceInfo(packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstallerPackageName(packageName)
        }
    }.getOrNull()
