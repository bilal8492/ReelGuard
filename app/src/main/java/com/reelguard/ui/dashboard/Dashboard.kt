package com.reelguard.ui.dashboard

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.reelguard.data.datastore.Settings
import com.reelguard.data.datastore.SettingsRepository
import com.reelguard.detection.DetectionEngine
import com.reelguard.domain.LimitState
import com.reelguard.domain.LimitStatus
import com.reelguard.ui.common.AppCard
import com.reelguard.ui.common.ProgressRing
import com.reelguard.ui.common.ScreenTitle
import com.reelguard.ui.common.StatusChip
import com.reelguard.ui.common.StatusGlyph
import com.reelguard.ui.common.formatMinutes
import com.reelguard.ui.theme.color
import com.reelguard.ui.theme.extra
import com.reelguard.widget.hasWidget
import com.reelguard.widget.requestPinWidget
import com.reelguard.work.isServiceEnabled
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

data class DashboardState(
    val limit: LimitState,
    val settings: Settings,
    val outdatedApps: Set<String>,
    val serviceEnabled: Boolean,
    val showWidgetPrompt: Boolean,
)

@HiltViewModel
class DashboardViewModel
    @Inject
    constructor(
        engine: DetectionEngine,
        private val settings: SettingsRepository,
        health: HealthRepository,
        @ApplicationContext private val context: Context,
    ) : ViewModel() {
        private val enabled = MutableStateFlow(isServiceEnabled(context))

        private val widgetPlaced = MutableStateFlow(hasWidget(context))

        fun refresh() {
            enabled.value = isServiceEnabled(context)
            widgetPlaced.value = hasWidget(context)
        }

        fun dismissWidgetPrompt() {
            viewModelScope.launch { settings.setWidgetPromptDismissed(true) }
        }

        val state: StateFlow<DashboardState?> =
            combine(engine.limitState, settings.settings, health.state, enabled, widgetPlaced) { ls, s, h, en, placed ->
                ls?.let { DashboardState(it, s, h.outdatedApps, en, s.onboardingDone && !placed && !s.widgetPromptDismissed) }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    }

@Composable
fun DashboardScreen(
    onOpenHealth: () -> Unit,
    vm: DashboardViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refresh() }
    state?.let { DashboardContent(it, onOpenHealth, onDismissWidgetPrompt = vm::dismissWidgetPrompt) }
}

@Composable
fun DashboardContent(
    state: DashboardState,
    onOpenHealth: () -> Unit,
    onDismissWidgetPrompt: () -> Unit = {},
) {
    val context = LocalContext.current
    val ls = state.limit
    val progress = if (ls.limit > 0) ls.total.toFloat() / ls.limit else 0f
    val statusLabel =
        when (ls.status) {
            LimitStatus.OK -> stringResource(R.string.status_ok)
            LimitStatus.WARN -> stringResource(R.string.status_warn)
            LimitStatus.REACHED -> stringResource(R.string.dash_limit_reached)
        }
    val date =
        remember(ls.counts, state.settings.resetMinutes) {
            LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM"))
        }
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 112.dp).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenTitle(stringResource(R.string.nav_today), date)
        if (!state.serviceEnabled) Banner(stringResource(R.string.banner_service_off), onOpenHealth)
        state.outdatedApps.forEach { Banner(stringResource(R.string.banner_outdated, Apps.label(it)), onOpenHealth) }

        if (state.showWidgetPrompt) {
            WidgetPrompt(
                onAdd = {
                    if (!requestPinWidget(context)) Toast.makeText(context, R.string.set_widget_unsupported, Toast.LENGTH_LONG).show()
                },
                onDismiss = onDismissWidgetPrompt,
            )
        }

        AppCard(Modifier.fillMaxWidth(), padding = 24.dp) {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                val ringDesc = stringResource(R.string.widget_ring_desc, ls.total, ls.limit)
                ProgressRing(
                    progress = progress,
                    color = ls.status.color(),
                    modifier = Modifier.size(236.dp).semantics { contentDescription = ringDesc }.testTag("ring"),
                    strokeWidth = 18.dp,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${ls.total}", style = MaterialTheme.typography.displayLarge, modifier = Modifier.testTag("count"))
                        Text(
                            stringResource(R.string.dash_of, ls.limit),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                StatusChip(ls.status, statusLabel)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Metric(stringResource(R.string.dash_remaining), "${ls.remaining}")
                    Metric(stringResource(R.string.dash_est_short), formatMinutes(ls.total * state.settings.avgDurationSeconds))
                    Metric(
                        stringResource(R.string.dash_resets),
                        LocalTime.of(state.settings.resetMinutes / 60, state.settings.resetMinutes % 60)
                            .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)),
                    )
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AppTile(Apps.YOUTUBE, ls, state.settings, MaterialTheme.colorScheme.extra.seriesShorts, Modifier.weight(1f))
            AppTile(Apps.INSTAGRAM, ls, state.settings, MaterialTheme.colorScheme.extra.seriesReels, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Metric(
    label: String,
    value: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AppTile(
    pkg: String,
    ls: LimitState,
    s: Settings,
    color: Color,
    modifier: Modifier,
) {
    val count = ls.counts[pkg] ?: 0
    val cap = s.perAppLimits[pkg]
    val monitored = pkg in s.monitoredApps
    val denominator = cap ?: ls.limit
    val share = if (denominator > 0) (count.toFloat() / denominator).coerceIn(0f, 1f) else 0f
    AppCard(modifier, padding = 16.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(color))
                Text(Apps.shortLabel(pkg), style = MaterialTheme.typography.titleSmall)
                if (!monitored) {
                    Text(
                        stringResource(R.string.dash_not_monitored),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text("$count", style = MaterialTheme.typography.displayMedium)
            Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
                if (share > 0f) Box(Modifier.fillMaxWidth(share).height(6.dp).clip(CircleShape).background(color))
            }
            Text(
                if (cap != null) stringResource(R.string.dash_tile_cap, cap) else stringResource(R.string.dash_tile_share),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WidgetPrompt(
    onAdd: () -> Unit,
    onDismiss: () -> Unit,
) = InfoCard(
    tag = "widget_prompt",
    title = stringResource(R.string.widget_prompt_title),
    body = stringResource(R.string.widget_prompt_body),
    primaryLabel = stringResource(R.string.widget_prompt_add),
    secondaryLabel = stringResource(R.string.widget_prompt_later),
    onPrimary = onAdd,
    onSecondary = onDismiss,
)

/** A dismissible card at the top of the Today screen: a title, a sentence, a main button and a quiet second one. */
@Composable
private fun InfoCard(
    tag: String,
    title: String,
    body: String,
    primaryLabel: String,
    secondaryLabel: String,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
) {
    val ink = MaterialTheme.colorScheme.onPrimaryContainer
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth().testTag(tag),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = ink)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = ink)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onPrimary, modifier = Modifier.heightMinimum()) { Text(primaryLabel) }
                TextButton(onClick = onSecondary, modifier = Modifier.heightMinimum()) { Text(secondaryLabel, color = ink) }
            }
        }
    }
}

@Composable
private fun Banner(
    text: String,
    onFix: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatusGlyph(LimitStatus.REACHED)
            Text(
                text,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = onFix,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onErrorContainer,
                        contentColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                modifier = Modifier.heightMinimum(),
            ) { Text(stringResource(R.string.banner_fix)) }
        }
    }
}

private fun Modifier.heightMinimum() = this.then(Modifier.height(40.dp))
