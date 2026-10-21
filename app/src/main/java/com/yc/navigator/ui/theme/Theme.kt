package com.yc.navigator.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFF1F4E79),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3E4F7),
    onPrimaryContainer = Color(0xFF0A2540),
    secondary = Color(0xFFB77900),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE7B8),
    onSecondaryContainer = Color(0xFF3A2600),
    tertiary = Color(0xFF2E6B4F),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFB7EFD1),
    onTertiaryContainer = Color(0xFF002113),
    background = Color(0xFFF6F7F9),
    onBackground = Color(0xFF171C22),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF171C22),
    surfaceVariant = Color(0xFFE9EDF2),
    onSurfaceVariant = Color(0xFF46515F),
    outline = Color(0xFF74808E),
    outlineVariant = Color(0xFFC6CED8),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9CCBF5),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF0E3A5E),
    onPrimaryContainer = Color(0xFFD3E4F7),
    secondary = Color(0xFFF2C15C),
    onSecondary = Color(0xFF3A2600),
    secondaryContainer = Color(0xFF5C4300),
    onSecondaryContainer = Color(0xFFFFE7B8),
    tertiary = Color(0xFF9AD6B8),
    onTertiary = Color(0xFF003825),
    tertiaryContainer = Color(0xFF175136),
    onTertiaryContainer = Color(0xFFB7EFD1),
    background = Color(0xFF0E1620),
    onBackground = Color(0xFFE2E6EB),
    surface = Color(0xFF151E28),
    onSurface = Color(0xFFE2E6EB),
    surfaceVariant = Color(0xFF223040),
    onSurfaceVariant = Color(0xFFBFC9D6),
    outline = Color(0xFF8A96A5),
    outlineVariant = Color(0xFF3B4A5A),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

private val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.4).sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp,
        lineHeight = 27.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 25.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 19.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.3.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.9.sp
    )
)

@Composable
fun YCNavigatorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
