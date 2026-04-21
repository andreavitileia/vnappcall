package com.vnsas.vnappcall.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// VN sas brand colors
val VNBlue = Color(0xFF0A84FF)
val VNBlueDark = Color(0xFF0066CC)
val VNBlueLight = Color(0xFF5AC8FA)
val VNBackground = Color(0xFFF2F2F7)
val VNSurface = Color.White
val VNCardBg = Color.White
val VNTextPrimary = Color(0xFF1C1C1E)
val VNTextSecondary = Color(0xFF8E8E93)
val VNDivider = Color(0xFFE5E5EA)
val VNGreen = Color(0xFF34C759)
val VNOrange = Color(0xFFFF9500)
val VNRed = Color(0xFFFF3B30)

private val LightScheme = lightColorScheme(
    primary = VNBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6EEFF),
    onPrimaryContainer = Color(0xFF003258),
    secondary = Color(0xFF5856D6),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8E7FF),
    onSecondaryContainer = Color(0xFF1C1B5E),
    tertiary = VNGreen,
    onTertiary = Color.White,
    background = VNBackground,
    onBackground = VNTextPrimary,
    surface = VNSurface,
    onSurface = VNTextPrimary,
    surfaceVariant = Color(0xFFF2F2F7),
    onSurfaceVariant = VNTextSecondary,
    outline = VNDivider,
    error = VNRed,
    onError = Color.White,
)

private val DarkScheme = darkColorScheme(
    primary = VNBlueLight,
    onPrimary = Color(0xFF003258),
    primaryContainer = VNBlueDark,
    onPrimaryContainer = Color(0xFFD6EEFF),
    secondary = Color(0xFFBDB9FF),
    onSecondary = Color(0xFF1C1B5E),
    tertiary = Color(0xFF30D158),
    onTertiary = Color(0xFF003314),
    background = Color(0xFF1C1C1E),
    onBackground = Color(0xFFF2F2F7),
    surface = Color(0xFF2C2C2E),
    onSurface = Color(0xFFF2F2F7),
    surfaceVariant = Color(0xFF3A3A3C),
    onSurfaceVariant = Color(0xFF8E8E93),
    outline = Color(0xFF48484A),
    error = Color(0xFFFF453A),
    onError = Color.White,
)

private val VNTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp),
)

@Composable
fun VNAppCallTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkScheme else LightScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = VNTypography,
        content = content
    )
}
