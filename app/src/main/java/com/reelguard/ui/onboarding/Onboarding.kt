package com.reelguard.ui.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewModelScope
import com.reelguard.R
import com.reelguard.data.datastore.SettingsRepository
import com.reelguard.domain.LimitStatus
import com.reelguard.ui.common.AppCard
import com.reelguard.ui.common.PrimaryButton
import com.reelguard.ui.common.ProgressRing
import com.reelguard.ui.common.SecondaryButton
import com.reelguard.ui.common.StatusGlyph
import com.reelguard.work.isServiceEnabled
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel
    @Inject
    constructor(
        private val settings: SettingsRepository,
        @ApplicationContext private val context: Context,
    ) : ViewModel() {
        fun serviceEnabled() = isServiceEnabled(context)

        fun finish(onDone: () -> Unit) {
            viewModelScope.launch {
                settings.setOnboardingDone(true)
                onDone()
            }
        }
    }

@Composable
fun OnboardingScreen(
    onDone: () -> Unit,
    vm: OnboardingViewModel = hiltViewModel(),
) {
    OnboardingContent(isServiceEnabled = vm::serviceEnabled, onFinish = { vm.finish(onDone) })
}

private const val LAST = 3

@Composable
fun OnboardingContent(
    isServiceEnabled: () -> Boolean,
    onFinish: () -> Unit,
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var declined by rememberSaveable { mutableStateOf(false) }
    var enabled by remember { mutableStateOf(isServiceEnabled()) }
    val context = LocalContext.current
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { enabled = isServiceEnabled() }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { step = LAST }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp)) {
        StepBar(step)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Hero(step, enabled)
            when (step) {
                0 -> Copy(stringResource(R.string.ob_welcome_title), stringResource(R.string.ob_welcome_body))
                1 -> DisclosureCopy(declined)
                2 -> {
                    Copy(stringResource(R.string.ob_enable_title), stringResource(R.string.ob_enable_body))
                    ServiceState(enabled)
                    Text(
                        stringResource(R.string.ob_restricted_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                else -> Copy(stringResource(R.string.ob_done_title), stringResource(R.string.ob_done_body))
            }
        }
        Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when (step) {
                0 -> PrimaryButton(stringResource(R.string.ob_next), { step = 1 }, Modifier.testTag("next"))
                1 -> {
                    PrimaryButton(stringResource(R.string.ob_agree), { step = 2 }, Modifier.testTag("agree"))
                    SecondaryButton(stringResource(R.string.ob_decline), { declined = true })
                }
                2 -> {
                    PrimaryButton(
                        stringResource(R.string.ob_open_settings),
                        { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) },
                    )
                    SecondaryButton(
                        stringResource(R.string.ob_next),
                        {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                step = LAST
                            }
                        },
                        enabled = enabled,
                    )
                    SecondaryButton(stringResource(R.string.ob_skip), { step = LAST })
                }
                else -> PrimaryButton(stringResource(R.string.ob_finish), onFinish, Modifier.testTag("next"))
            }
        }
    }
}

@Composable
private fun StepBar(step: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(LAST + 1) { i ->
            Box(
                Modifier.weight(1f).height(4.dp).clip(CircleShape)
                    .background(if (i <= step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest),
            )
        }
    }
}

@Composable
private fun Copy(
    title: String,
    body: String,
) {
    Text(title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
    Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
}

@Composable
private fun DisclosureCopy(declined: Boolean) {
    Text(
        stringResource(R.string.ob_disclosure_title),
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier.testTag("disclosure_title"),
    )
    AppCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(R.string.ob_disclosure_body), style = MaterialTheme.typography.bodyMedium)
            Point(true, stringResource(R.string.ob_pt_detects))
            Point(true, stringResource(R.string.ob_pt_counts))
            Point(false, stringResource(R.string.ob_pt_no_content))
            Point(false, stringResource(R.string.ob_pt_no_upload))
        }
    }
    if (declined) Text(stringResource(R.string.ob_declined), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
}

@Composable
private fun Point(
    does: Boolean,
    text: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatusGlyph(if (does) LimitStatus.OK else LimitStatus.REACHED)
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ServiceState(enabled: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatusGlyph(if (enabled) LimitStatus.OK else LimitStatus.WARN)
        Text(
            stringResource(if (enabled) R.string.ob_enabled else R.string.ob_not_enabled),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun Hero(
    step: Int,
    enabled: Boolean,
) {
    val primary = MaterialTheme.colorScheme.primary
    when (step) {
        0 ->
            ProgressRing(0.72f, primary, Modifier.size(176.dp), strokeWidth = 14.dp) {
                Text("36", style = MaterialTheme.typography.displayMedium)
            }
        1 ->
            Canvas(Modifier.size(120.dp)) {
                val s = size.minDimension
                val shield =
                    Path().apply {
                        moveTo(s / 2, s * 0.04f)
                        lineTo(s * 0.9f, s * 0.2f)
                        lineTo(s * 0.9f, s * 0.5f)
                        cubicTo(s * 0.9f, s * 0.76f, s * 0.72f, s * 0.92f, s / 2, s * 0.98f)
                        cubicTo(s * 0.28f, s * 0.92f, s * 0.1f, s * 0.76f, s * 0.1f, s * 0.5f)
                        lineTo(s * 0.1f, s * 0.2f)
                        close()
                    }
                drawPath(shield, primary.copy(alpha = 0.16f))
                drawPath(shield, primary, style = Stroke(s * 0.045f, cap = StrokeCap.Round))
                val tick =
                    Path().apply {
                        moveTo(s * 0.34f, s * 0.5f)
                        lineTo(s * 0.46f, s * 0.62f)
                        lineTo(s * 0.68f, s * 0.38f)
                    }
                drawPath(tick, primary, style = Stroke(s * 0.06f, cap = StrokeCap.Round))
            }
        2 ->
            Canvas(Modifier.size(width = 150.dp, height = 84.dp)) {
                val track = if (enabled) primary else Color(0xFF898781)
                drawRoundRect(
                    track.copy(alpha = if (enabled) 1f else 0.35f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2, size.height / 2),
                )
                val cx = if (enabled) size.width - size.height / 2 else size.height / 2
                drawCircle(Color.White, size.height * 0.38f, Offset(cx, size.height / 2))
            }
        else -> StatusGlyph(LimitStatus.OK, Modifier.size(112.dp))
    }
}
