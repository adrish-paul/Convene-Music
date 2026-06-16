package com.example.convenemusic.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

object ExpressiveEasing {
    // 🎭 Standard Emphasized: starts fast, holds, slows gently
    val Emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    // ⏩ Emphasized Accelerate: starts slow, ends fast
    val EmphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    // 🛑 Emphasized Decelerate: starts fast, ends slow
    val EmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
}
