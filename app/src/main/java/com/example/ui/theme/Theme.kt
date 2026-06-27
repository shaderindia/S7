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

private val DarkColorScheme =
  darkColorScheme(
    primary = SecureEmeraldPrimary,
    onPrimary = Color.Black,
    secondary = SecureTealSecondary,
    background = SecureDarkBg,
    surface = SecureDarkSurface,
    onBackground = SecureDarkTextPrimary,
    onSurface = SecureDarkTextPrimary,
    onSurfaceVariant = SecureDarkTextSecondary
  )

private val LightColorScheme =
  lightColorScheme(
    primary = SecureLightPrimary,
    onPrimary = Color.White,
    secondary = SecureLightSecondary,
    background = SecureLightBg,
    surface = SecureLightSurface,
    onBackground = SecureLightTextPrimary,
    onSurface = SecureLightTextPrimary,
    onSurfaceVariant = SecureLightTextSecondary
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disabling dynamic colors by default so our highly custom security branding is prominent
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
