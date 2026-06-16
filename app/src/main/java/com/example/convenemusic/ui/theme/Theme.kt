package com.example.convenemusic.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

import androidx.core.view.WindowCompat
import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.isSystemInDarkTheme

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    background = BackgroundLight,
    surface = SurfaceLight,
    outline = OutlineLight
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF80CBC4),
    onPrimary = Color(0xFF00332C),
    primaryContainer = Color(0xFF004D40),
    onPrimaryContainer = Color(0xFFE0F2F1),
    secondary = Color(0xFFB0BEC5),
    background = Color(0xFF121214),
    surface = Color(0xFF1E1E24),
    outline = Color(0xFF2E2E38)
)

private fun Context.findActivity(): ComponentActivity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is ComponentActivity) return context
        context = context.baseContext
    }
    return null
}

@Composable
fun ConveneMusicTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val dynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    
    val colorScheme = when {
        dynamicColor -> dynamicDarkColorScheme(context)
        else -> DarkColorScheme
    }

    val uiColors = if (dynamicColor) {
        val baseBg = colorScheme.background
        val baseSurface = colorScheme.surface
        val baseOutline = colorScheme.outline
        val darkBg = Color(red = baseBg.red * 0.15f, green = baseBg.green * 0.15f, blue = baseBg.blue * 0.15f, alpha = baseBg.alpha)
        val darkSurface = Color(red = baseSurface.red * 0.28f, green = baseSurface.green * 0.28f, blue = baseSurface.blue * 0.28f, alpha = baseSurface.alpha)
        val darkOutline = Color(red = baseOutline.red * 0.28f, green = baseOutline.green * 0.28f, blue = baseOutline.blue * 0.28f, alpha = baseOutline.alpha)
        UIColors(
            background = darkBg,
            cardBackground = darkSurface,
            cardBorder = darkOutline,
            textPrimary = colorScheme.onBackground,
            textSecondary = colorScheme.onSurfaceVariant,
            progressAccent = colorScheme.primary,
            activeGlow = colorScheme.primary,
            accentTeal = colorScheme.primary,
            accentRed = colorScheme.error,
            accentOrange = colorScheme.tertiary,
            accentBlue = colorScheme.secondary,
            accentGold = Color(0xFFECC844),
            bottomBarColor = darkSurface.copy(alpha = 0.72f)
        )
    } else {
        UIColors(
            background = Color(0xFF121214),
            cardBackground = Color(0xFF1E1E24),
            cardBorder = Color(0xFF2E2E38),
            textPrimary = Color(0xFFECECEF),
            textSecondary = Color(0xFF8E8E93),
            progressAccent = Color(0xFF80CBC4),
            activeGlow = Color(0xFF80CBC4),
            accentTeal = Color(0xFF80CBC4),
            bottomBarColor = Color(0xFF1E1E24).copy(alpha = 0.72f)
        )
    }

    val view = LocalView.current
 
     if (!view.isInEditMode) {
         SideEffect {
             val activity = context.findActivity()
             if (activity != null) {
                 activity.enableEdgeToEdge(
                     statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
                     navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                 )
             } else {
                 val window = (context as Activity).window
                 window.statusBarColor = android.graphics.Color.TRANSPARENT
                 window.navigationBarColor = android.graphics.Color.TRANSPARENT
                 
                 val insetsController = WindowCompat.getInsetsController(window, view)
                 insetsController.isAppearanceLightStatusBars = false
                 insetsController.isAppearanceLightNavigationBars = false
             }
         }
     }
 
     CompositionLocalProvider(LocalUIColors provides uiColors) {
         MaterialTheme(
             colorScheme = colorScheme,
             typography = Typography,
             shapes = ExpressiveShapes,
             content = content
         )
     }
 }