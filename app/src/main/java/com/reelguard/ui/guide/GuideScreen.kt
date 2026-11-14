package com.reelguard.ui.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reelguard.R
import com.reelguard.ui.common.PrimaryButton
import com.reelguard.ui.common.SecondaryButton
import kotlinx.coroutines.delay

/** What the guide shows and how the activity wants to be told about the user's choices. */
data class GuideState(
    val appsList: AppsListLabel,
    val showRestrictedHint: Boolean,
    val inPip: Boolean,
    val voiceOn: Boolean,
)

@Composable
private fun stepText(
    step: Int,
    appsList: AppsListLabel,
): String =
    when (step) {
        0 ->
            stringResource(
                if (appsList == AppsListLabel.INSTALLED_APPS) R.string.guide_step1_installed else R.string.guide_step1_downloaded,
            )
        1 -> stringResource(R.string.guide_step2)
        else -> stringResource(R.string.guide_step3)
    }

@Composable
private fun listLabel(appsList: AppsListLabel): String =
    stringResource(if (appsList == AppsListLabel.INSTALLED_APPS) R.string.guide_label_installed else R.string.guide_label_downloaded)

/**
 * The guide. Full size it explains the three taps before Settings opens; in the small picture-in-picture window it loops the steps
 * while the user does them in the real Settings screen. [onSpeak] gets the sentence for the step that just came up.
 */
@Composable
fun GuideScreen(
    state: GuideState,
    onVoiceChange: (Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit,
    onSpeak: (String) -> Unit,
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(GuideSteps.STEP_MS)
            step = GuideSteps.next(step)
        }
    }
    val text = stepText(step, state.appsList)
    LaunchedEffect(step, state.inPip, state.voiceOn) {
        if (state.inPip && state.voiceOn) onSpeak(text)
    }
    if (state.inPip) {
        PipCard(step, text, state.appsList)
    } else {
        FullCard(step, state, onVoiceChange, onOpenSettings, onClose)
    }
}

@Composable
private fun PipCard(
    step: Int,
    text: String,
    appsList: AppsListLabel,
) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            stringResource(R.string.guide_step_counter, step + 1, GuideSteps.COUNT),
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        StepIllustration(step, listLabel(appsList), compact = true, modifier = Modifier.fillMaxWidth())
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun FullCard(
    step: Int,
    state: GuideState,
    onVoiceChange: (Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(stringResource(R.string.guide_title), style = MaterialTheme.typography.headlineMedium)
            Text(
                stringResource(R.string.guide_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            StepIllustration(step, listLabel(state.appsList), compact = false, modifier = Modifier.fillMaxWidth())
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(GuideSteps.COUNT) { i -> StepLine(i, active = i == step, text = stepText(i, state.appsList)) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.guide_voice), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Switch(checked = state.voiceOn, onCheckedChange = onVoiceChange, modifier = Modifier.testTag("guide_voice"))
            }
            if (state.showRestrictedHint) {
                Text(
                    stringResource(R.string.ob_restricted_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton(stringResource(R.string.guide_open), onOpenSettings, Modifier.testTag("guide_open"))
            SecondaryButton(stringResource(R.string.guide_not_now), onClose)
        }
    }
}

@Composable
private fun StepLine(
    index: Int,
    active: Boolean,
    text: String,
) {
    val primary = MaterialTheme.colorScheme.primary
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (active) primary else MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                (index + 1).toString(),
                style = MaterialTheme.typography.labelLarge,
                color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
        )
    }
}
