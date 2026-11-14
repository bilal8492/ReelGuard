package com.reelguard.ui.guide

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reelguard.R

/**
 * A small mock of the Settings list that highlights what to tap in [step] (0 to 2). It is drawn here rather than recorded, so it
 * looks the same on every phone and needs no video files. [compact] is the size used inside the picture-in-picture window.
 */
@Composable
fun StepIllustration(
    step: Int,
    listLabel: String,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val outer = if (compact) 8.dp else 20.dp
    Column(
        modifier
            .clip(RoundedCornerShape(if (compact) 12.dp else 24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(outer),
        verticalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 12.dp),
    ) {
        MockRow(listLabel, active = step == 0, compact = compact)
        MockRow(stringResource(R.string.app_name), active = step == 1, compact = compact)
        MockSwitchRow(stringResource(R.string.guide_mock_switch), on = step == 2, compact = compact)
    }
}

private fun rowShape(compact: Boolean) = RoundedCornerShape(if (compact) 8.dp else 16.dp)

@Composable
private fun MockRow(
    label: String,
    active: Boolean,
    compact: Boolean,
) {
    val primary = MaterialTheme.colorScheme.primary
    val height = if (compact) 28.dp else 58.dp
    Row(
        Modifier
            .fillMaxWidth()
            .height(height)
            .clip(rowShape(compact))
            .background(if (active) primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface)
            .then(if (active) Modifier.border(if (compact) 1.5.dp else 3.dp, primary, rowShape(compact)) else Modifier)
            .padding(horizontal = if (compact) 6.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 12.dp),
    ) {
        Box(Modifier.size(if (compact) 12.dp else 28.dp).clip(CircleShape).background(primary.copy(alpha = if (active) 1f else 0.35f)))
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            fontSize = if (compact) 11.sp else 17.sp,
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )
        if (active) Pointer(compact)
    }
}

@Composable
private fun MockSwitchRow(
    label: String,
    on: Boolean,
    compact: Boolean,
) {
    val primary = MaterialTheme.colorScheme.primary
    val height = if (compact) 28.dp else 58.dp
    val knob by animateFloatAsState(if (on) 1f else 0f, tween(600), label = "knob")
    val trackW = if (compact) 26.dp else 52.dp
    val trackH = if (compact) 14.dp else 28.dp
    Row(
        Modifier
            .fillMaxWidth()
            .height(height)
            .clip(rowShape(compact))
            .background(if (on) primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface)
            .then(if (on) Modifier.border(if (compact) 1.5.dp else 3.dp, primary, rowShape(compact)) else Modifier)
            .padding(horizontal = if (compact) 6.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 12.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            fontSize = if (compact) 11.sp else 17.sp,
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )
        if (on) Pointer(compact)
        Box(
            Modifier
                .width(trackW)
                .height(trackH)
                .clip(CircleShape)
                .background(if (on) primary else MaterialTheme.colorScheme.outline),
            contentAlignment = Alignment.CenterStart,
        ) {
            val knobSize: Dp = trackH - 4.dp
            Box(
                Modifier
                    .offset(x = 2.dp + (trackW - trackH) * knob)
                    .size(knobSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
            )
        }
    }
}

/** A pulsing dot that says "tap here". */
@Composable
private fun Pointer(compact: Boolean) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val scale by transition.animateFloat(1f, 1.5f, infiniteRepeatable(tween(750), RepeatMode.Reverse), label = "scale")
    val size = if (compact) 10.dp else 22.dp
    Box(contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(size)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    alpha = 2f - scale
                }
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )
        Box(Modifier.size(size / 2).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
    }
}
