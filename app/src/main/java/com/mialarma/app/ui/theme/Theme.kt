package com.mialarma.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val MiAlarmaDarkColorScheme = darkColorScheme(
    primary = MiAlarmaPrimary,
    onPrimary = MiAlarmaOnPrimary,
    primaryContainer = MiAlarmaPrimaryContainer,
    onPrimaryContainer = MiAlarmaOnPrimaryContainer,
    secondary = MiAlarmaSecondary,
    onSecondary = MiAlarmaOnSecondary,
    error = MiAlarmaError,
    onError = MiAlarmaOnError,
    background = MiAlarmaBackground,
    onBackground = MiAlarmaOnBackground,
    surface = MiAlarmaSurface,
    onSurface = MiAlarmaOnSurface,
    surfaceVariant = MiAlarmaSurfaceVariant,
    onSurfaceVariant = MiAlarmaOnSurfaceVariant,
    outline = MiAlarmaOutline
)

/**
 * Tema de Mi Alarma: siempre oscuro, con un diseño limpio basado en Material 3.
 */
@Composable
fun MiAlarmaTheme(content: @Composable () -> Unit) {
    val colorScheme = MiAlarmaDarkColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MiAlarmaTypography,
        content = content
    )
}
