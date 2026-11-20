package com.example.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Core Brand Palette: Red, White, Dark Navy Blue, Teal
val CrimsonRed = Color(0xFFD92638)
val CrimsonRedDark = Color(0xFFB31929)
val CrimsonRedLight = Color(0xFFFFE5E8)
val CrimsonRedGlow = Color(0xFFFB7185)

val NavyDeep = Color(0xFF0B1120)
val NavyPrimary = Color(0xFF0F172A)
val NavyElevated = Color(0xFF1E293B)
val NavySoft = Color(0xFF334155)
val NavyBorder = Color(0xFF283548)

val TealAccent = Color(0xFF0D9488)
val TealBright = Color(0xFF14B8A6)
val TealLightContainer = Color(0xFFCCFBF1)
val TealDarkContainer = Color(0xFF115E59)

val PaperWhite = Color(0xFFFFFFFF)
val OffWhiteCanvas = Color(0xFFF6F8FC)
val SlateSurfaceVariant = Color(0xFFEBF0F7)
val SlateOutline = Color(0xFFCBD5E1)
val SlateTextSecondary = Color(0xFF475569)

val DarkTextPrimary = Color(0xFFF8FAFC)
val DarkTextSecondary = Color(0xFF94A3B8)

val PdfLightColorScheme = lightColorScheme(
    primary = CrimsonRed,
    onPrimary = PaperWhite,
    primaryContainer = CrimsonRedLight,
    onPrimaryContainer = CrimsonRedDark,
    secondary = NavyPrimary,
    onSecondary = PaperWhite,
    secondaryContainer = Color(0xFFE2E8F0),
    onSecondaryContainer = NavyPrimary,
    tertiary = TealAccent,
    onTertiary = PaperWhite,
    tertiaryContainer = TealLightContainer,
    onTertiaryContainer = Color(0xFF0F766E),
    background = OffWhiteCanvas,
    onBackground = NavyPrimary,
    surface = PaperWhite,
    onSurface = NavyPrimary,
    surfaceVariant = SlateSurfaceVariant,
    onSurfaceVariant = SlateTextSecondary,
    outline = SlateOutline,
    outlineVariant = Color(0xFFE2E8F0),
    error = Color(0xFFDC2626),
    onError = PaperWhite,
    surfaceContainerLowest = PaperWhite,
    surfaceContainerLow = Color(0xFFF8FAFC),
    surfaceContainer = Color(0xFFF1F5F9),
    surfaceContainerHigh = Color(0xFFE2E8F0),
    surfaceContainerHighest = Color(0xFFCBD5E1),
)

val PdfDarkColorScheme = darkColorScheme(
    primary = CrimsonRedGlow,
    onPrimary = Color(0xFF4C0519),
    primaryContainer = Color(0xFF881337),
    onPrimaryContainer = Color(0xFFFFE4E6),
    secondary = Color(0xFF94A3B8),
    onSecondary = NavyDeep,
    secondaryContainer = NavyElevated,
    onSecondaryContainer = DarkTextPrimary,
    tertiary = TealBright,
    onTertiary = Color(0xFF042F2E),
    tertiaryContainer = TealDarkContainer,
    onTertiaryContainer = TealLightContainer,
    background = NavyDeep,
    onBackground = DarkTextPrimary,
    surface = NavyPrimary,
    onSurface = DarkTextPrimary,
    surfaceVariant = NavyElevated,
    onSurfaceVariant = DarkTextSecondary,
    outline = NavyBorder,
    outlineVariant = Color(0xFF1E293B),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    surfaceContainerLowest = NavyDeep,
    surfaceContainerLow = NavyPrimary,
    surfaceContainer = Color(0xFF162033),
    surfaceContainerHigh = NavyElevated,
    surfaceContainerHighest = NavySoft,
)

@Immutable
data class BrandPalette(
    val navyHero: Color,
    val navyHeroElevated: Color,
    val crimson: Color,
    val teal: Color,
    val onHero: Color,
    val onHeroMuted: Color,
    val success: Color,
    val warning: Color,
)

val LightBrandPalette = BrandPalette(
    navyHero = NavyPrimary,
    navyHeroElevated = NavyElevated,
    crimson = CrimsonRed,
    teal = TealAccent,
    onHero = PaperWhite,
    onHeroMuted = Color(0xFFCBD5E1),
    success = Color(0xFF059669),
    warning = Color(0xFFD97706),
)

val DarkBrandPalette = BrandPalette(
    navyHero = Color(0xFF131D33),
    navyHeroElevated = NavyElevated,
    crimson = CrimsonRedGlow,
    teal = TealBright,
    onHero = DarkTextPrimary,
    onHeroMuted = DarkTextSecondary,
    success = Color(0xFF34D399),
    warning = Color(0xFFFBBF24),
)

val LocalBrandPalette = staticCompositionLocalOf { LightBrandPalette }
