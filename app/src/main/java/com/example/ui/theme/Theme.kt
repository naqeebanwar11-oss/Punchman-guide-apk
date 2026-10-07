package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val PunchmanDarkColorScheme = darkColorScheme(
    primary = BullGreenBright,
    onPrimary = Color.Black,
    primaryContainer = BullGreenSoft,
    onPrimaryContainer = BullGreenBright,
    secondary = AccentCyan,
    onSecondary = Color.White,
    secondaryContainer = Color(0x332962FF),
    onSecondaryContainer = Color(0xFF82B1FF),
    tertiary = AccentGold,
    onTertiary = Color.Black,
    tertiaryContainer = AccentGoldSoft,
    onTertiaryContainer = AccentGold,
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondary,
    outline = BorderDark,
    outlineVariant = BorderLight,
    error = BearRedBright,
    onError = Color.White,
    errorContainer = BearRedSoft,
    onErrorContainer = BearRedBright
)

// Trading apps are predominantly dark-themed for high contrast chart visualization
private val PunchmanLightColorScheme = PunchmanDarkColorScheme

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent pro trading contrast
    content: @Composable () -> Unit,
) {
    val colorScheme = PunchmanDarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
