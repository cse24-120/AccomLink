package com.example.accomlink.ui.theme

import android.app.Activity
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

private val DarkColorScheme = darkColorScheme(
    primary = Green80,
    onPrimary = Green900,
    primaryContainer = DarkSurfaceVariant,
    onPrimaryContainer = Green80,
    secondary = Amber80,
    onSecondary = Green900,
    secondaryContainer = Color(0xFF4A3B17),
    onSecondaryContainer = Amber80,
    tertiary = Red80,
    onTertiary = Green900,
    tertiaryContainer = Color(0xFF653B35),
    onTertiaryContainer = Red80,
    background = Green900,
    onBackground = Color(0xFFE6F0EC),
    surface = DarkSurface,
    onSurface = Color(0xFFE6F0EC),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFC7D4D0),
    error = Red80,
    errorContainer = Color(0xFF72352F),
    onError = Green900,
    onErrorContainer = Color(0xFFFFDAD5),
    outline = Color(0xFF8EA39E)
)

private val LightColorScheme = lightColorScheme(
    primary = Green700,
    onPrimary = Color.White,
    primaryContainer = TealContainer,
    onPrimaryContainer = Color(0xFF003D37),
    secondary = Amber600,
    onSecondary = Color(0xFF302300),
    secondaryContainer = AmberContainer,
    onSecondaryContainer = Color(0xFF4B3600),
    tertiary = RedReserved,
    onTertiary = Color.White,
    tertiaryContainer = CoralContainer,
    onTertiaryContainer = Color(0xFF5D1713),
    background = Mist,
    surface = Color.White,
    surfaceVariant = SoftSurfaceVariant,
    surfaceContainer = SoftSurface,
    onBackground = Ink,
    onSurface = Ink,
    onSurfaceVariant = MutedInk,
    error = RedReserved,
    errorContainer = CoralContainer,
    onError = Color.White,
    onErrorContainer = Color(0xFF5D1713),
    outline = Color(0xFF7A8B86)
)

@Composable
fun AccomLinkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
