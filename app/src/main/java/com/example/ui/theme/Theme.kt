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
    primary = NavyPrimary,
    onPrimary = Color.White,
    primaryContainer = NavyContainer,
    onPrimaryContainer = OnNavyContainer,
    secondary = CyanCad,
    onSecondary = Color.White,
    secondaryContainer = CyanContainer,
    onSecondaryContainer = OnCyanContainer,
    tertiary = AmberSafety,
    onTertiary = Color.White,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = OnAmberContainer,
    background = PaperBackground,
    onBackground = InkPrimary,
    surface = SpecSurface,
    onSurface = InkPrimary,
    surfaceVariant = BlueprintTintSurface,
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
    onPrimary = NavyDeep,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkInkPrimary,
    secondary = DarkSecondary,
    onSecondary = NavyDeep,
    secondaryContainer = Color(0xFF0E3755),
    onSecondaryContainer = Color(0xFFBAE6FD),
    tertiary = DarkTertiary,
    onTertiary = Color(0xFF2E1500),
    tertiaryContainer = Color(0xFF4B2905),
    onTertiaryContainer = Color(0xFFFDE68A),
    background = DarkBackground,
    onBackground = DarkInkPrimary,
    surface = DarkSurface,
    onSurface = DarkInkPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkInkSecondary,
    outline = DarkOutline,
    outlineVariant = Color(0xFF1D304D),
    error = Color(0xFFF87171),
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
