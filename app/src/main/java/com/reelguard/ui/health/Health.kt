package com.reelguard.ui.health

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.reelguard.R
import com.reelguard.data.datastore.Apps
import com.reelguard.data.datastore.HealthRepository
import com.reelguard.data.datastore.SettingsRepository
import com.reelguard.domain.LimitStatus
import com.reelguard.ui.common.AppCard
import com.reelguard.ui.common.PrimaryButton
import com.reelguard.ui.common.ScreenTitle
import com.reelguard.ui.common.SecondaryButton
import com.reelguard.ui.common.SectionHeader
import com.reelguard.ui.common.StatusChip
import com.reelguard.ui.common.StatusGlyph
import com.reelguard.ui.guide.AccessibilityGuideActivity
import com.reelguard.work.isServiceEnabled
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HealthUi(
    val serviceEnabled: Boolean,
    val serviceConnected: Boolean,
    val batteryExempt: Boolean,
    val outdatedApps: Set<String>,
    val fallbackEnabled: Boolean,
)

@HiltViewModel
class HealthViewModel
    @Inject
    constructor(
        health: HealthRepository,
        private val settingsRepo: SettingsRepository,
        @ApplicationContext private val context: Context,
    ) : ViewModel() {
        fun setFallback(on: Boolean) {
            viewModelScope.launch { settingsRepo.setStructuralFallback(on) }
        }

        private val system = MutableStateFlow(read())

        private fun read() =
            isServiceEnabled(context) to
                (context.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(context.packageName) == true)

        fun refresh() {
            system.value = read()
        }

        val state: StateFlow<HealthUi?> =
            combine(system, health.state, settingsRepo.settings) { (enabled, battery), h, st ->
                HealthUi(enabled, h.serviceConnected, battery, h.outdatedApps, st.structuralFallbackEnabled)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    }

@Composable
fun HealthScreen(vm: HealthViewModel = hiltViewModel()) {
    val ui by vm.state.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refresh() }
    val context = LocalContext.current
    val s = ui ?: return
    val allGood = s.serviceEnabled && s.serviceConnected && s.batteryExempt && s.outdatedApps.isEmpty()
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 32.dp).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenTitle(stringResource(R.string.health_title), stringResource(R.string.health_subtitle))
        StatusChip(
            if (allGood) LimitStatus.OK else LimitStatus.WARN,
            stringResource(if (allGood) R.string.health_all_good else R.string.health_needs_attention),
        )
        AppCard(Modifier.fillMaxWidth(), padding = 0.dp) {
            Column {
                Check(stringResource(R.string.health_service_enabled), s.serviceEnabled)
                Check(stringResource(R.string.health_service_connected), s.serviceConnected && s.serviceEnabled)
                Check(stringResource(R.string.health_battery), s.batteryExempt)
                Apps.all.forEach { pkg -> Check(stringResource(R.string.health_detection, Apps.label(pkg)), pkg !in s.outdatedApps) }
            }
        }
        if (s.outdatedApps.isNotEmpty()) {
            Text(stringResource(R.string.health_outdated_help), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        // Only offered when detection looks outdated (or it is already on, so it can be turned off again).
        if (s.outdatedApps.isNotEmpty() || s.fallbackEnabled) {
            AppCard(Modifier.fillMaxWidth(), padding = 0.dp) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 56.dp)
                        .toggleable(s.fallbackEnabled, role = Role.Switch, onValueChange = vm::setFallback)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(stringResource(R.string.health_fallback), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            stringResource(R.string.health_fallback_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = s.fallbackEnabled, onCheckedChange = null)
                }
            }
        }

        PrimaryButton(
            stringResource(R.string.ob_open_settings),
            {
                // When the service is off, walk the user through turning it on; when it is on they may be here to turn it off.
                if (s.serviceEnabled) {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } else {
                    AccessibilityGuideActivity.launch(context)
                }
            },
        )
        SecondaryButton(
            stringResource(R.string.health_open_battery),
            { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) },
        )
        SecondaryButton(
            stringResource(R.string.health_open_appinfo),
            {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            },
        )

        SectionHeader(stringResource(R.string.health_steps_title))
        AppCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(stepsFor(Build.MANUFACTURER)), style = MaterialTheme.typography.bodyMedium)
                Text(
                    stringResource(R.string.health_steps_generic),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun stepsFor(manufacturer: String): Int {
    val m = manufacturer.lowercase()
    return when {
        "xiaomi" in m || "redmi" in m || "poco" in m -> R.string.health_steps_xiaomi
        "samsung" in m -> R.string.health_steps_samsung
        "oneplus" in m || "oppo" in m || "realme" in m -> R.string.health_steps_oneplus
        else -> R.string.health_steps_generic
    }
}

@Composable
private fun Check(
    label: String,
    good: Boolean,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        StatusGlyph(if (good) LimitStatus.OK else LimitStatus.WARN)
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(if (good) R.string.health_ok else R.string.health_problem),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(start = 48.dp))
}
