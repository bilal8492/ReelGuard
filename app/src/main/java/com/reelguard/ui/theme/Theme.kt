package com.reelguard.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reelguard.data.datastore.ThemeMode
import com.reelguard.domain.LimitStatus

/**
 * Palette follows the dataviz method: one brand hue (blue), a fixed categorical pair for the two apps
 * (blue = Shorts, orange = Reels; validated for colour-blind separation in light and dark), and reserved status colours
 * that always ship with an icon and a label.
 */
@Immutable
class ExtraColors(
    val seriesShorts: Color,
    val seriesReels: Color,
    val good: Color,
    val warning: Color,
    val critical: Color,
    val hairline: Color,
)

private val LightExtra =
    ExtraColors(
        seriesShorts = Color(0xFF2A78D6),
        seriesReels = Color(0xFFEB6834),
        good = Color(0xFF0CA30C),
        warning = Color(0xFFC98500),
        critical = Color(0xFFD03B3B),
        hairline = Color(0x1A0B0B0B),
    )

private val DarkExtra =
    ExtraColors(
        seriesShorts = Color(0xFF3987E5),
        seriesReels = Color(0xFFD95926),
        good = Color(0xFF0CA30C),
        warning = Color(0xFFFAB219),
        critical = Color(0xFFE66767),
        hairline = Color(0x1AFFFFFF),
    )

val LocalExtraColors = staticCompositionLocalOf { LightExtra }

val ColorScheme.extra: ExtraColors
    @Composable get() = LocalExtraColors.current

val LightScheme: ColorScheme =
    lightColorScheme(
        primary = Color(0xFF2A78D6),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFDCEAFB),
        onPrimaryContainer = Color(0xFF0D366B),
        secondary = Color(0xFFEB6834),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFFBE3D8),
        onSecondaryContainer = Color(0xFF5A2410),
        tertiary = Color(0xFFC98500),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFEFC7),
        onTertiaryContainer = Color(0xFF4A3300),
        error = Color(0xFFD03B3B),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFBE1E1),
        onErrorContainer = Color(0xFF5C1414),
        background = Color(0xFFF6F6F3),
        onBackground = Color(0xFF0B0B0B),
        surface = Color(0xFFF6F6F3),
        onSurface = Color(0xFF0B0B0B),
        surfaceVariant = Color(0xFFEDECE7),
        onSurfaceVariant = Color(0xFF52514E),
        outline = Color(0xFF898781),
        outlineVariant = Color(0xFFE1E0D9),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFFCFCFB),
        surfaceContainer = Color(0xFFFFFFFF),
        surfaceContainerHigh = Color(0xFFF0EFEB),
        surfaceContainerHighest = Color(0xFFE8E7E2),
    )

val DarkScheme: ColorScheme =
    darkColorScheme(
        primary = Color(0xFF3987E5),
        onPrimary = Color(0xFF001B3A),
        primaryContainer = Color(0xFF0D366B),
        onPrimaryContainer = Color(0xFFCDE2FB),
        secondary = Color(0xFFD95926),
        onSecondary = Color(0xFF1F0A02),
        secondaryContainer = Color(0xFF4A2210),
        onSecondaryContainer = Color(0xFFFBD3C1),
        tertiary = Color(0xFFFAB219),
        onTertiary = Color(0xFF3A2800),
        tertiaryContainer = Color(0xFF4A3300),
        onTertiaryContainer = Color(0xFFFFE29A),
        error = Color(0xFFE66767),
        onError = Color(0xFF2A0505),
        errorContainer = Color(0xFF4A1818),
        onErrorContainer = Color(0xFFFFD9D9),
        background = Color(0xFF0D0D0D),
        onBackground = Color(0xFFF0EFEC),
        surface = Color(0xFF0D0D0D),
        onSurface = Color(0xFFF0EFEC),
        surfaceVariant = Color(0xFF242422),
        onSurfaceVariant = Color(0xFFC3C2B7),
        outline = Color(0xFF898781),
        outlineVariant = Color(0xFF2C2C2A),
        surfaceContainerLowest = Color(0xFF0D0D0D),
        surfaceContainerLow = Color(0xFF151514),
        surfaceContainer = Color(0xFF1A1A19),
        surfaceContainerHigh = Color(0xFF242422),
        surfaceContainerHighest = Color(0xFF2C2C2A),
    )

private val AppTypography =
    Typography(
        displayLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 68.sp, lineHeight = 72.sp, letterSpacing = (-2.5).sp),
        displayMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 52.sp, lineHeight = 56.sp, letterSpacing = (-1.5).sp),
        headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.8).sp),
        headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.5).sp),
        headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.3).sp),
        titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
        titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
        titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
        bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
        bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
        labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
        labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
        labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.6.sp),
    )

private val AppShapes =
    Shapes(
        extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
    )

@Composable
fun ReelGuardTheme(
    mode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val dark =
        when (mode) {
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
    CompositionLocalProvider(LocalExtraColors provides if (dark) DarkExtra else LightExtra) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}

/** Ring/chip colour for a limit state. Status colours are reserved and always paired with an icon and a label. */
@Composable
fun LimitStatus.color(): Color =
    when (this) {
        LimitStatus.OK -> MaterialTheme.colorScheme.primary
        LimitStatus.WARN -> MaterialTheme.colorScheme.extra.warning
        LimitStatus.REACHED -> MaterialTheme.colorScheme.extra.critical
    }
