package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = BatStateCrimson,
    onPrimary = Color.White,
    primaryContainer = BatStateCrimsonContainer,
    onPrimaryContainer = OnBatStateCrimsonContainer,
    secondary = BatStateForest,
    onSecondary = Color.White,
    secondaryContainer = BatStateForestContainer,
    onSecondaryContainer = OnBatStateForestContainer,
    tertiary = BatStateGold,
    onTertiary = Color.White,
    tertiaryContainer = BatStateGoldContainer,
    onTertiaryContainer = OnBatStateGoldContainer,
    background = WarmPaperBackground,
    onBackground = InkPrimary,
    surface = SpecSurface,
    onSurface = InkPrimary,
    surfaceVariant = WarmTintSurface,
    onSurfaceVariant = InkSecondary,
    outline = DraftingOutline,
    outlineVariant = DraftingOutlineSubtle,
    error = StatusFailAccent,
    onError = Color.White,
    errorContainer = StatusFailContainer,
    onErrorContainer = StatusFailText
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = BatStateCrimsonDeep,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkInkPrimary,
    secondary = DarkSecondary,
    onSecondary = Color(0xFF072914),
    secondaryContainer = Color(0xFF144225),
    onSecondaryContainer = Color(0xFFBBF7D0),
    tertiary = DarkTertiary,
    onTertiary = Color(0xFF3B2300),
    tertiaryContainer = Color(0xFF523400),
    onTertiaryContainer = Color(0xFFFEF08A),
    background = DarkBackground,
    onBackground = DarkInkPrimary,
    surface = DarkSurface,
    onSurface = DarkInkPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkInkSecondary,
    outline = DarkOutline,
    outlineVariant = Color(0xFF3E2428),
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF5C1515),
    onErrorContainer = Color(0xFFFEE2E2)
)

val CivilSpecShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp)
)

@Composable
fun CivilSpecTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = CivilSpecShapes,
        content = content
    )
}
