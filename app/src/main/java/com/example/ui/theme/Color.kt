package com.example.ui.theme

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

// Tertiary (Emerald)
val EmeraldTertiary = Color(0xFF10B981)
val EmeraldTertiaryLight = Color(0xFF34D399)
val EmeraldContainerDark = Color(0xFF064E3B)
val EmeraldContainerLight = Color(0xFFD1FAE5)

// Warning (Amber)
val AmberWarning = Color(0xFFF59E0B)
val AmberWarningLight = Color(0xFFFBBF24)
val AmberContainerDark = Color(0xFF78350F)

// Error (Rose)
val RoseError = Color(0xFFF43F5E)
val RoseErrorContainer = Color(0xFF881337)

// Dark Theme Surfaces
val SlateBackgroundDark = Color(0xFF090D16)
val SlateSurfaceDark = Color(0xFF0F172A)
val SlateSurfaceVariantDark = Color(0xFF1E293B)
val SlateSurfaceHighlightDark = Color(0xFF334155)
val SlateTextPrimaryDark = Color(0xFFF8FAFC)
val SlateTextSecondaryDark = Color(0xFF94A3B8)
val SlateBorderDark = Color(0xFF1E293B)

// Light Theme Surfaces
val SlateBackgroundLight = Color(0xFFF8FAFC)
val SlateSurfaceLight = Color(0xFFFFFFFF)
val SlateSurfaceVariantLight = Color(0xFFF1F5F9)
val SlateTextPrimaryLight = Color(0xFF0F172A)
val SlateTextSecondaryLight = Color(0xFF64748B)
val SlateBorderLight = Color(0xFFE2E8F0)

// Semantic color tokens
object ColorTokens {
    val toolPending = AmberWarning
    val toolSuccess = EmeraldTertiary
    val toolError = RoseError
    val toolExecuting = AmberWarningLight
    val link = CyanSecondary
    val onLink = SlateSurfaceLight
}
