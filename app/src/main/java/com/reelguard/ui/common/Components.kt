package com.reelguard.ui.common

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.reelguard.R
import com.reelguard.data.datastore.FrictionMode
import com.reelguard.data.datastore.Settings
import com.reelguard.domain.DayTotal
import com.reelguard.domain.LimitStatus
import com.reelguard.ui.theme.color
import com.reelguard.ui.theme.extra
import kotlinx.coroutines.delay

/** Flat card: surface fill plus a hairline border, no shadow. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    padding: Dp = 20.dp,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.extra.hairline),
    ) {
        Box(Modifier.padding(padding)) { content() }
    }
}

@Composable
fun SectionHeader(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 8.dp, top = 24.dp, bottom = 8.dp),
    )
}

@Composable
fun ScreenTitle(
    title: String,
    subtitle: String? = null,
) {
    Column(Modifier.padding(top = 8.dp, bottom = 4.dp)) {
        Text(title, style = MaterialTheme.typography.headlineLarge)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ProgressRing(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = strokeWidth.toPx()
            val topLeft = Offset(sw / 2, sw / 2)
            val arc = Size(size.width - sw, size.height - sw)
            val style = Stroke(sw, cap = StrokeCap.Round)
            drawArc(track, 0f, 360f, false, topLeft, arc, style = style)
            if (progress > 0f) drawArc(color, -90f, 360f * progress.coerceIn(0.01f, 1f), false, topLeft, arc, style = style)
        }
        content()
    }
}

/** Status icon: shape differs per state so colour is never the only cue. */
@Composable
fun StatusGlyph(
    status: LimitStatus,
    modifier: Modifier = Modifier,
) {
    val tint = status.color()
    Canvas(modifier.size(18.dp)) {
        val s = size.minDimension
        val w = s * 0.12f
        val white = Color.White
        when (status) {
            LimitStatus.OK -> {
                drawCircle(tint)
                val p =
                    Path().apply {
                        moveTo(s * 0.28f, s * 0.52f)
                        lineTo(s * 0.44f, s * 0.68f)
                        lineTo(s * 0.74f, s * 0.34f)
                    }
                drawPath(p, white, style = Stroke(w, cap = StrokeCap.Round))
            }
            LimitStatus.WARN -> {
                val p =
                    Path().apply {
                        moveTo(s / 2, s * 0.06f)
                        lineTo(s * 0.96f, s * 0.9f)
                        lineTo(s * 0.04f, s * 0.9f)
                        close()
                    }
                drawPath(p, tint)
                drawLine(Color(0xFF1A1100), Offset(s / 2, s * 0.38f), Offset(s / 2, s * 0.62f), w, StrokeCap.Round)
                drawCircle(Color(0xFF1A1100), w * 0.6f, Offset(s / 2, s * 0.76f))
            }
            LimitStatus.REACHED -> {
                drawCircle(tint)
                drawLine(white, Offset(s * 0.34f, s * 0.34f), Offset(s * 0.66f, s * 0.66f), w, StrokeCap.Round)
                drawLine(white, Offset(s * 0.66f, s * 0.34f), Offset(s * 0.34f, s * 0.66f), w, StrokeCap.Round)
            }
        }
    }
}

@Composable
fun StatusChip(
    status: LimitStatus,
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.clip(
            CircleShape,
        ).background(status.color().copy(alpha = 0.14f)).padding(start = 10.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatusGlyph(status)
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** Two-option or multi-option pill switch (7 / 30 days, theme, friction...). */
@Composable
fun Segmented(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier.weight(1f).heightIn(min = 40.dp).clip(CircleShape)
                    .background(if (on) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .selectable(on, role = Role.RadioButton, onClick = { onSelect(i) })
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

private fun DrawScope.roundedTop(
    color: Color,
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    radius: Float,
    rounded: Boolean,
) {
    val r = if (rounded) minOf(radius, width / 2, height) else 0f
    val path =
        Path().apply {
            addRoundRect(
                RoundRect(left, top, left + width, top + height, CornerRadius(r), CornerRadius(r), CornerRadius.Zero, CornerRadius.Zero),
            )
        }
    drawPath(path, color)
}

/**
 * Stacked daily bars (Shorts below, Reels above) with a 2dp gap between segments, rounded data-ends anchored to the baseline,
 * a dashed limit line, and tap-to-select. Series colours are fixed per app.
 */
@Composable
fun StackedBarChart(
    days: List<DayTotal>,
    shortsKey: String,
    reelsKey: String,
    limit: Int,
    selected: Int?,
    onSelect: (Int) -> Unit,
    description: String,
    modifier: Modifier = Modifier,
) {
    val shortsColor = MaterialTheme.colorScheme.extra.seriesShorts
    val reelsColor = MaterialTheme.colorScheme.extra.seriesReels
    val grid = MaterialTheme.colorScheme.outlineVariant
    val limitColor = MaterialTheme.colorScheme.onSurfaceVariant
    val highlight = MaterialTheme.colorScheme.surfaceContainerHigh
    Canvas(
        modifier.fillMaxWidth().height(190.dp).semantics { contentDescription = description }
            .pointerInput(days.size) {
                detectTapGestures { o -> onSelect((o.x / (size.width / days.size.coerceAtLeast(1))).toInt().coerceIn(0, days.size - 1)) }
            },
    ) {
        if (days.isEmpty()) return@Canvas
        val maxV = maxOf(days.maxOf { it.total }, limit, 1).toFloat()
        val slot = size.width / days.size
        val barW = minOf(slot * 0.58f, 22.dp.toPx())
        val gap = 2.dp.toPx()
        val base = size.height
        drawLine(grid, Offset(0f, base), Offset(size.width, base), 1.dp.toPx())
        selected?.let { i ->
            drawRoundRect(highlight, Offset(i * slot, 0f), Size(slot, base), CornerRadius(10.dp.toPx()))
        }
        days.forEachIndexed { i, d ->
            val x = i * slot + (slot - barW) / 2
            val s = d.perApp[shortsKey] ?: 0
            val r = d.perApp[reelsKey] ?: 0
            val other = (d.total - s - r).coerceAtLeast(0)
            val hS = if (s > 0) maxOf(base * s / maxV, 3.dp.toPx()) else 0f
            val hR = if (r + other > 0) maxOf(base * (r + other) / maxV, 3.dp.toPx()) else 0f
            val rr = 5.dp.toPx()
            if (hS > 0f) roundedTop(shortsColor, x, base - hS, barW, hS, rr, hR == 0f)
            if (hR > 0f) {
                val y = base - hS - hR - if (hS > 0f) gap else 0f
                roundedTop(reelsColor, x, y, barW, hR, rr, true)
            }
        }
        val ly = base * (1 - limit / maxV)
        drawLine(
            limitColor,
            Offset(0f, ly),
            Offset(size.width, ly),
            1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 9f)),
        )
    }
}

@Composable
fun LegendDot(
    color: Color,
    label: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

enum class NavKind { TODAY, HISTORY, SETTINGS }

@Composable
private fun NavIcon(
    kind: NavKind,
    color: Color,
) {
    Canvas(Modifier.size(24.dp)) {
        val w = 2.2.dp.toPx()
        val s = size.minDimension
        when (kind) {
            NavKind.TODAY -> {
                drawArc(color, -90f, 270f, false, Offset(w, w), Size(s - 2 * w, s - 2 * w), style = Stroke(w, cap = StrokeCap.Round))
                drawCircle(color, w * 0.9f, Offset(s / 2, s / 2))
            }
            NavKind.HISTORY -> {
                val bar = s * 0.18f
                listOf(0.38f to 0.5f, 0.7f to 0.2f, 0.52f to 0.35f).forEachIndexed { i, (h, _) ->
                    val x = s * (0.16f + i * 0.3f)
                    drawLine(color, Offset(x + bar / 2, s * 0.9f), Offset(x + bar / 2, s * (0.9f - h)), bar, StrokeCap.Round)
                }
            }
            NavKind.SETTINGS -> {
                drawLine(color, Offset(s * 0.12f, s * 0.32f), Offset(s * 0.88f, s * 0.32f), w, StrokeCap.Round)
                drawLine(color, Offset(s * 0.12f, s * 0.68f), Offset(s * 0.88f, s * 0.68f), w, StrokeCap.Round)
                drawCircle(color, s * 0.13f, Offset(s * 0.64f, s * 0.32f))
                drawCircle(color, s * 0.13f, Offset(s * 0.36f, s * 0.68f))
            }
        }
    }
}

data class NavItem(val route: String, val label: String, val kind: NavKind)

/** Floating pill navigation: the selected tab expands to show its label. */
@Composable
fun FloatingNavBar(
    items: List<NavItem>,
    selectedRoute: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.extra.hairline),
        shadowElevation = 10.dp,
    ) {
        Row(Modifier.padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            items.forEach { item ->
                val on = item.route == selectedRoute
                val tint = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                Row(
                    Modifier.heightIn(min = 48.dp).clip(CircleShape)
                        .background(if (on) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .selectable(on, role = Role.Tab, onClick = { onSelect(item.route) })
                        .semantics { contentDescription = item.label }
                        .padding(horizontal = if (on) 18.dp else 16.dp)
                        .animateContentSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    NavIcon(item.kind, tint)
                    if (on) Text(item.label, style = MaterialTheme.typography.labelLarge, color = tint, maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    androidx.compose.material3.Button(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
    ) { Text(text, style = MaterialTheme.typography.titleMedium) }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
    ) { Text(text, style = MaterialTheme.typography.titleMedium) }
}

/** Confirms a weakening change using the configured friction: a 30 s wait, a typed phrase, or nothing. */
@Composable
fun FrictionDialog(
    mode: FrictionMode,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (mode == FrictionMode.NONE) {
        LaunchedEffect(Unit) { onConfirm() }
        return
    }
    var left by remember { mutableIntStateOf(Settings.WAIT_SECONDS) }
    var typed by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (left > 0) {
            delay(1000)
            left--
        }
    }
    val ready = if (mode == FrictionMode.WAIT) left == 0 else typed.trim().equals(Settings.FRICTION_PHRASE, ignoreCase = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text(stringResource(R.string.friction_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.friction_body), color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (mode == FrictionMode.WAIT) {
                    Text(
                        if (left > 0) stringResource(R.string.friction_wait, left) else stringResource(R.string.friction_ready),
                        style = MaterialTheme.typography.titleMedium,
                    )
                } else {
                    OutlinedTextField(
                        value = typed,
                        onValueChange = { typed = it },
                        label = { Text(stringResource(R.string.friction_phrase_hint, Settings.FRICTION_PHRASE)) },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = ready, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.friction_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
fun formatMinutes(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    return if (minutes >= 60) {
        stringResource(R.string.time_hr_min, minutes / 60, minutes % 60)
    } else {
        stringResource(R.string.time_min, minutes)
    }
}

/** Big number with a caption, for stat tiles. */
@Composable
fun StatTile(
    value: String,
    caption: String,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier, padding = 16.dp) {
        Column {
            Text(value, style = MaterialTheme.typography.headlineMedium)
            Text(caption, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ClickableRow(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = onClick), contentAlignment = Alignment.CenterStart) { content() }
}
