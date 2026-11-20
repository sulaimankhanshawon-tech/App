package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val AsilLightColorScheme = lightColorScheme(
    primary = SaffronPrimary,
    onPrimary = Color.White,
    primaryContainer = SaffronContainer,
    onPrimaryContainer = OnSaffronContainer,
    secondary = OliveEmerald,
    onSecondary = Color.White,
    secondaryContainer = OliveContainer,
    onSecondaryContainer = OliveEmerald,
    tertiary = RoyalTeal,
    onTertiary = Color.White,
    tertiaryContainer = TealContainer,
    onTertiaryContainer = RoyalTeal,
    background = WarmIvory,
    onBackground = EspressoInk,
    surface = WarmIvory,
    onSurface = EspressoInk,
    surfaceVariant = WarmSand,
    onSurfaceVariant = MutedStone,
    surfaceContainerLow = WarmSand,
    surfaceContainer = WarmSandElevated,
    outline = SubtleBorder,
    error = SumacCrimson,
    onError = Color.White,
    errorContainer = CrimsonContainer,
    onErrorContainer = SumacCrimson
)

val AsilShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun AsilAlTawooqTheme(
    isArabic: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AsilLightColorScheme,
        typography = buildAppTypography(isArabic),
        shapes = AsilShapes,
        content = content
    )
}
