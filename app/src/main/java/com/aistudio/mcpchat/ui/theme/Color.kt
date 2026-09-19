package com.aistudio.mcpchat.ui.theme

import androidx.compose.ui.graphics.Color

// Primary (Indigo)
val IndigoPrimary = Color(0xFF6366F1)
val IndigoPrimaryLight = Color(0xFF818CF8)
val IndigoPrimaryDark = Color(0xFF4F46E5)
val IndigoContainerDark = Color(0xFF1E1B4B)
val IndigoContainerLight = Color(0xFFE0E7FF)

// Secondary (Cyan)
val CyanSecondary = Color(0xFF06B6D4)
val CyanSecondaryLight = Color(0xFF22D3EE)
val CyanContainerDark = Color(0xFF164E63)
val CyanContainerLight = Color(0xFFCFFAFE)
val CyanTextOnLight = Color(0xFF0E7490)

// Tertiary (Emerald)
val EmeraldTertiary = Color(0xFF10B981)
val EmeraldTertiaryLight = Color(0xFF34D399)
val EmeraldContainerDark = Color(0xFF064E3B)
val EmeraldContainerLight = Color(0xFFD1FAE5)
val EmeraldTextOnLight = Color(0xFF047857)

// Warning (Amber)
val AmberWarning = Color(0xFFF59E0B)
val AmberWarningLight = Color(0xFFFBBF24)
val AmberContainerDark = Color(0xFF78350F)
val AmberTextOnLight = Color(0xFFB45309)

// Error (Rose)
val RoseError = Color(0xFFF43F5E)
val RoseErrorOnLight = Color(0xFFE11D48)
val RoseErrorContainerDark = Color(0xFF881337)
val RoseErrorContainerLight = Color(0xFFFFDAD6)
val RoseErrorContainer = RoseErrorContainerDark

// Dark Theme Surfaces
val SlateBackgroundDark = Color(0xFF090D16)
val SlateSurfaceDark = Color(0xFF0F172A)
val SlateSurfaceVariantDark = Color(0xFF1E293B)
val SlateSurfaceHighlightDark = Color(0xFF334155)
val SlateTextPrimaryDark = Color(0xFFF8FAFC)
val SlateTextSecondaryDark = Color(0xFFB0BEC5)
val SlateBorderDark = Color(0xFF334155)
val SlateBorderVariantDark = Color(0xFF1E293B)
val SlateSurfaceContainerLowestDark = Color(0xFF090D16)
val SlateSurfaceContainerLowDark = Color(0xFF0F172A)
val SlateSurfaceContainerDark = Color(0xFF1E293B)
val SlateSurfaceContainerHighDark = Color(0xFF334155)

// Light Theme Surfaces
val SlateBackgroundLight = Color(0xFFF8FAFC)
val SlateSurfaceLight = Color(0xFFFFFFFF)
val SlateSurfaceVariantLight = Color(0xFFF1F5F9)
val SlateTextPrimaryLight = Color(0xFF0F172A)
val SlateTextSecondaryLight = Color(0xFF64748B)
val SlateBorderLight = Color(0xFFE2E8F0)
val SlateBorderVariantLight = Color(0xFFF1F5F9)
val SlateSurfaceContainerLowestLight = Color(0xFFFFFFFF)
val SlateSurfaceContainerLowLight = Color(0xFFF8FAFC)
val SlateSurfaceContainerLight = Color(0xFFF1F5F9)
val SlateSurfaceContainerHighLight = Color(0xFFE2E8F0)
val SlateSurfaceContainerHighestLight = Color(0xFFCBD5E1)

// Semantic color tokens
object ColorTokens {
    val toolPending = AmberWarning
    val toolSuccess = EmeraldTertiary
    val toolError = RoseError
    val toolExecuting = AmberWarningLight
    val link = IndigoPrimaryDark
    val onLink = SlateSurfaceLight
}
