package com.example.convenemusic.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.staticCompositionLocalOf

val LocalUIColors = staticCompositionLocalOf { UIColors() }

// Light Scheme Base Colors
val PrimaryLight = Color(0xFF1D9F90) // Premium Teal
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFE0F2F1)
val OnPrimaryContainerLight = Color(0xFF004D40)

val SecondaryLight = Color(0xFF8E8E93)
val BackgroundLight = Color(0xFFF6F6F8) // Soft flat light grey
val SurfaceLight = Color(0xFFFFFFFF)    // Pure white cards
val OutlineLight = Color(0xFFE5E5EA)    // Soft grey border contour

// Status/Accent highlights matching the F1 standings view style
val RankTeal = Color(0xFF33B5A5)
val RankRed = Color(0xFFD32F2F)
val RankOrange = Color(0xFFF27A24)
val RankBlue = Color(0xFF2B5BE5)
val RankGold = Color(0xFFECC844)

data class UIColors(
    val background: Color = BackgroundLight,
    val cardBackground: Color = SurfaceLight,
    val cardBorder: Color = OutlineLight,
    val textPrimary: Color = Color(0xFF1C1C1E),
    val textSecondary: Color = Color(0xFF8E8E93),
    val progressAccent: Color = PrimaryLight,
    val activeGlow: Color = RankTeal,
    val accentTeal: Color = RankTeal,
    val accentRed: Color = RankRed,
    val accentOrange: Color = RankOrange,
    val accentBlue: Color = RankBlue,
    val accentGold: Color = RankGold,
    val bottomBarColor: Color = Color(0xD8FFFFFF) // Glassmorphic translucent white
)