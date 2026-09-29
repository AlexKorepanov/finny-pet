package ru.finny.petgame.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private fun lightColors() = lightColorScheme(
    primary = FinnyColors.Primary,
    onPrimary = FinnyColors.OnPrimary,
    primaryContainer = Color(0xFFDCE8FF),
    onPrimaryContainer = Color(0xFF123A8C),
    secondary = FinnyColors.Optional,
    onSecondary = FinnyColors.OnPrimary,
    tertiary = FinnyColors.Tasks,
    onTertiary = FinnyColors.OnPrimary,
    background = FinnyColors.Background,
    onBackground = FinnyColors.TextPrimary,
    surface = FinnyColors.Surface,
    onSurface = FinnyColors.TextPrimary,
    surfaceVariant = FinnyColors.SurfaceVariant,
    onSurfaceVariant = FinnyColors.TextSecondary,
    outline = FinnyColors.OutlineSoft,
    outlineVariant = FinnyColors.CardBorder,
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
)

private fun darkColors() = darkColorScheme(
    primary = FinnyColors.Primary,
    onPrimary = FinnyColors.OnPrimary,
    primaryContainer = Color(0xFF25324A),
    onPrimaryContainer = Color(0xFFDCE8FF),
    secondary = FinnyColors.Optional,
    onSecondary = FinnyColors.OnPrimary,
    tertiary = FinnyColors.Tasks,
    onTertiary = FinnyColors.OnPrimary,
    background = FinnyColors.Background,
    onBackground = FinnyColors.TextPrimary,
    surface = FinnyColors.Surface,
    onSurface = FinnyColors.TextPrimary,
    surfaceVariant = FinnyColors.SurfaceVariant,
    onSurfaceVariant = FinnyColors.TextSecondary,
    outline = FinnyColors.OutlineSoft,
    outlineVariant = FinnyColors.CardBorder,
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
)

private val FinnyShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun FinnyPetTheme(dark: Boolean = false, content: @Composable () -> Unit) {
    val colors: ColorScheme = if (dark) darkColors() else lightColors()
    MaterialTheme(
        colorScheme = colors,
        typography = FinnyTypography,
        shapes = FinnyShapes,
        content = content,
    )
}
