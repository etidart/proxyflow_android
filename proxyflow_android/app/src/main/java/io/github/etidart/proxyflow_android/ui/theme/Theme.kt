package io.github.etidart.proxyflow_android.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFF2F2F2),
    onPrimary = Color(0xFF111111),
    primaryContainer = Color(0xFFE5E5E5),
    onPrimaryContainer = Color(0xFF111111),
    secondary = Color(0xFFCFCFCF),
    onSecondary = Color(0xFF111111),
    tertiary = Color(0xFFB8B8B8),
    onTertiary = Color(0xFF111111),
    background = Color(0xFF121212),
    onBackground = Color(0xFFF5F5F5),
    surface = Color(0xFF1A1A1A),
    onSurface = Color(0xFFF5F5F5),
    surfaceVariant = Color(0xFF242424),
    onSurfaceVariant = Color(0xFFC7C7C7),
    outline = Color(0xFF888888),
    outlineVariant = Color(0xFF454545),
    error = Color(0xFFE0E0E0),
    onError = Color(0xFF161616),
    errorContainer = Color(0xFF353535),
    onErrorContainer = Color(0xFFF2F2F2),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF202020),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF3A3A3A),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFF555555),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFF777777),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF202020),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF202020),
    surfaceVariant = Color(0xFFECECEC),
    onSurfaceVariant = Color(0xFF606060),
    outline = Color(0xFF777777),
    outlineVariant = Color(0xFFD4D4D4),
    error = Color(0xFF303030),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFF555555),
    onErrorContainer = Color(0xFFFFFFFF),
)

@Composable
fun Proxyflow_androidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}