package com.reelguard.ui.settings

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.reelguard.BuildConfig
import com.reelguard.R
import com.reelguard.data.datastore.Apps
import com.reelguard.data.datastore.FrictionMode
import com.reelguard.data.datastore.Settings
import com.reelguard.data.datastore.SettingsRepository
import com.reelguard.data.datastore.ThemeMode
import com.reelguard.domain.DayKey
import com.reelguard.ui.common.AppCard
import com.reelguard.ui.common.FrictionDialog
import com.reelguard.ui.common.ScreenTitle
import com.reelguard.ui.common.SectionHeader
import com.reelguard.ui.common.Segmented
import com.reelguard.widget.requestPinWidget
import com.reelguard.work.WorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val repo: SettingsRepository,
        private val scheduler: WorkScheduler,
    ) : ViewModel() {
        val settings: StateFlow<Settings?> = repo.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        /** String resource id of the latest refusal message, or null. */
        val message = MutableStateFlow<Int?>(null)

        private fun day(s: Settings) = DayKey.of(System.currentTimeMillis(), s.resetMinutes)

        fun setLimit(
            s: Settings,
            v: Int,
        ) = viewModelScope.launch {
            message.value = if (repo.setCombinedLimit(v, day(s))) null else R.string.msg_raise_once
        }

        fun setAppLimit(
            s: Settings,
            pkg: String,
            v: Int?,
        ) = viewModelScope.launch {
            message.value = if (repo.setAppLimit(pkg, v, day(s))) null else R.string.msg_raise_once
        }

        fun setReset(minutes: Int) =
            viewModelScope.launch {
                repo.setResetMinutes(minutes)
                scheduler.scheduleReset(minutes)
            }

        fun setWarn(p: Int) = viewModelScope.launch { repo.setWarnAt(p) }

        fun setMonitored(
            pkg: String,
            on: Boolean,
        ) = viewModelScope.launch { repo.setMonitored(pkg, on) }

        fun setBlocking(on: Boolean) = viewModelScope.launch { repo.setBlockingEnabled(on) }

        fun setSnooze(on: Boolean) = viewModelScope.launch { repo.setSnoozeEnabled(on) }

        fun setTheme(t: ThemeMode) = viewModelScope.launch { repo.setTheme(t) }

        fun setShareUsage(on: Boolean) =
            viewModelScope.launch {
                repo.setShareUsage(on)
                repo.setUsageNoticeSeen(true)
            }

        fun setNotifyWarn(on: Boolean) = viewModelScope.launch { repo.setNotifyWarn(on) }

        fun setNotifyStop(on: Boolean) = viewModelScope.launch { repo.setNotifyServiceStop(on) }

        fun setShowCounter(on: Boolean) = viewModelScope.launch { repo.setShowCounter(on) }
    }

@Composable
fun SettingsScreen(
    onOpenHealth: () -> Unit,
    onOpenDebug: () -> Unit,
    vm: SettingsViewModel = hiltViewModel(),
) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val settings = s ?: return
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    var taps by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    /** Runs [action] after the configured friction. Used for every change that weakens the limit. */
    fun gated(action: () -> Unit) {
        pending = action
    }

    pending?.let { action ->
        FrictionDialog(settings.frictionMode, onConfirm = {
            pending = null
            action()
        }, onDismiss = { pending = null })
    }

    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 112.dp).fillMaxWidth()) {
        ScreenTitle(stringResource(R.string.nav_settings))
        message?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }

        SectionHeader(stringResource(R.string.set_limits))
        Group {
            NumberRow(
                stringResource(R.string.set_combined_limit),
                settings.combinedLimit.toString(),
                hint = stringResource(R.string.set_combined_limit_hint),
            ) { v ->
                val n = v.toIntOrNull()?.coerceAtLeast(1) ?: return@NumberRow
                if (n > settings.combinedLimit) gated { vm.setLimit(settings, n) } else vm.setLimit(settings, n)
            }
            Apps.all.forEach { pkg ->
                val cap = settings.perAppLimits[pkg]
                NumberRow(
                    stringResource(R.string.set_app_limit, Apps.label(pkg)),
                    cap?.toString().orEmpty(),
                    hint = stringResource(R.string.set_app_limit_hint, Apps.label(pkg)),
                    placeholder = stringResource(R.string.set_no_limit),
                    allowEmpty = true,
                ) { v ->
                    val n = v.toIntOrNull()?.coerceAtLeast(1)
                    val raising = cap != null && (n == null || n > cap)
                    if (raising) gated { vm.setAppLimit(settings, pkg, n) } else vm.setAppLimit(settings, pkg, n)
                }
            }
            var warn by remember(settings.warnAt) { mutableFloatStateOf(settings.warnAt / 10f) }
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    if (warn.toInt() == 0) {
                        stringResource(R.string.set_warn_off)
                    } else {
                        stringResource(R.string.set_warn_at, warn.toInt() * 10)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
                Hint(stringResource(R.string.set_warn_hint))
                Slider(
                    warn,
                    { warn = it },
                    valueRange = 0f..10f,
                    steps = 9,
                    onValueChangeFinished = { vm.setWarn(warn.toInt() * 10) },
                    colors =
                        SliderDefaults.colors(
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            inactiveTickColor = MaterialTheme.colorScheme.outline,
                            activeTickColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                )
            }
            RowDivider()
            val h = settings.resetMinutes / 60
            val m = settings.resetMinutes % 60
            ActionRow(
                stringResource(R.string.set_reset),
                stringResource(
                    R.string.set_reset_time,
                    LocalTime.of(h, m).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)),
                ),
            ) {
                TimePickerDialog(context, { _, hh, mm -> vm.setReset(hh * 60 + mm) }, h, m, true).show()
            }
        }
        if (settings.frictionMode != FrictionMode.NONE) {
            Hint(stringResource(R.string.set_raise_rule), Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp))
        }

        SectionHeader(stringResource(R.string.set_monitored))
        Group {
            Apps.all.forEach { pkg ->
                SwitchRow(Apps.label(pkg), stringResource(R.string.set_monitored_hint), pkg in settings.monitoredApps) { want ->
                    if (!want) gated { vm.setMonitored(pkg, false) } else vm.setMonitored(pkg, true)
                }
            }
        }

        SectionHeader(stringResource(R.string.set_blocking))
        Group {
            SwitchRow(
                stringResource(R.string.set_blocking_enabled),
                stringResource(R.string.set_blocking_hint),
                settings.blockingEnabled,
            ) { want ->
                if (!want) gated { vm.setBlocking(false) } else vm.setBlocking(true)
            }
            SwitchRow(stringResource(R.string.set_snooze), stringResource(R.string.set_snooze_hint), settings.snoozeEnabled) {
                vm.setSnooze(it)
            }
        }

        SectionHeader(stringResource(R.string.set_notifications))
        Group {
            SwitchRow(stringResource(R.string.set_notify_warn), stringResource(R.string.set_notify_warn_hint), settings.notifyWarn) {
                vm.setNotifyWarn(it)
            }
            SwitchRow(
                stringResource(R.string.set_notify_stop),
                stringResource(R.string.set_notify_stop_hint),
                settings.notifyServiceStop,
            ) { vm.setNotifyStop(it) }
        }

        SectionHeader(stringResource(R.string.set_display))
        Group {
            SwitchRow(
                stringResource(R.string.set_show_counter),
                stringResource(R.string.set_show_counter_hint),
                settings.showCounter,
            ) { vm.setShowCounter(it) }
            ActionRow(stringResource(R.string.set_add_widget), null) {
                if (!requestPinWidget(context)) Toast.makeText(context, R.string.set_widget_unsupported, Toast.LENGTH_LONG).show()
            }
            val themes = ThemeMode.entries
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.set_theme), style = MaterialTheme.typography.bodyLarge)
                Segmented(
                    listOf(
                        stringResource(R.string.theme_system),
                        stringResource(R.string.theme_light),
                        stringResource(R.string.theme_dark),
                    ),
                    themes.indexOf(settings.theme),
                    { vm.setTheme(themes[it]) },
                )
            }
        }

        SectionHeader(stringResource(R.string.set_privacy))
        Group {
            SwitchRow(
                stringResource(R.string.set_usage_stats),
                stringResource(R.string.set_usage_stats_hint),
                settings.shareUsage,
            ) { vm.setShareUsage(it) }
        }

        SectionHeader(stringResource(R.string.set_about))
        Group {
            ActionRow(stringResource(R.string.set_open_health), null, onClick = onOpenHealth)
            ActionRow(stringResource(R.string.set_version, BuildConfig.VERSION_NAME), null, chevron = false) {
                taps++
                if (taps >= 7) {
                    taps = 0
                    onOpenDebug()
                }
            }
        }
    }
}

@Composable
private fun Hint(
    text: String,
    modifier: Modifier = Modifier,
) = Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier)

@Composable
private fun Group(content: @Composable () -> Unit) {
    AppCard(Modifier.fillMaxWidth(), padding = 0.dp) {
        Column {
            // Rows are separated by hairlines; the divider is drawn by each row via [RowDivider] below the first.
            content()
        }
    }
}

@Composable
private fun RowDivider() = HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(start = 16.dp))

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).toggleable(checked, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Hint(subtitle)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
    RowDivider()
}

@Composable
private fun ActionRow(
    title: String,
    subtitle: String?,
    chevron: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Hint(subtitle)
        }
        if (chevron) Text("›", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    RowDivider()
}

@Composable
private fun NumberRow(
    label: String,
    value: String,
    hint: String? = null,
    placeholder: String? = null,
    allowEmpty: Boolean = false,
    onApply: (String) -> Unit,
) {
    var text by remember(value) { mutableStateOf(value) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f).padding(end = 12.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter(Char::isDigit).take(4) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                placeholder =
                    placeholder?.let { p ->
                        { Text(p, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
                    },
                textStyle = TextStyle(textAlign = TextAlign.Center, fontSize = MaterialTheme.typography.titleMedium.fontSize),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.widthIn(min = 84.dp, max = 96.dp),
            )
            val dirty = text != value && (allowEmpty || text.isNotEmpty())
            if (dirty) {
                FilledTonalButton(onClick = { onApply(text) }, modifier = Modifier.padding(start = 8.dp).heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.apply))
                }
            }
        }
        if (hint != null) Hint(hint, Modifier.padding(top = 4.dp))
    }
    RowDivider()
}
