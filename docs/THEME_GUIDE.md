# Theme System Guide: Avoid Hardcoded Colors & Fonts

This guide documents the patterns and anti-patterns encountered when building a Jetpack Compose app with **Material 3** theming. Follow these rules to ensure every UI element adapts to both **light** and **dark** themes correctly.

---

## Table of Contents

1. [Golden Rule](#1-golden-rule)
2. [Color System: Roles & When to Use Them](#2-color-system-roles--when-to-use-them)
3. [Typography: Use Theme Styles, Not Hardcoded Values](#3-typography-use-theme-styles-not-hardcoded-values)
4. [Common Mistakes & Fixes](#4-common-mistakes--fixes)
5. [Adding a New Screen: Checklist](#5-adding-a-new-screen-checklist)
6. [Theme Definition Best Practices](#6-theme-definition-best-practices)

---

## 1. Golden Rule

> **Never reference `Color.Gray`, `Color.Black`, `Color.LightGray`, or any hardcoded hex `Color(0xFF…)` inside UI composables.**
> **Never use `fontSize = X.sp` or `fontWeight = FontWeight.Bold` directly on a `Text` — use `style = MaterialTheme.typography.xxx` instead.**

Every color and font size must come from `MaterialTheme.colorScheme` or `MaterialTheme.typography`.

---

## 2. Color System: Roles & When to Use Them

| Material Role         | When to Use                                                                 |
|-----------------------|-----------------------------------------------------------------------------|
| `primary`             | Brand color for FABs, active buttons, selected states, icons                |
| `onPrimary`           | Text/icons **on top of** `primary` background                               |
| `primaryContainer`    | Subtle primary backgrounds (selected cards, highlighted items)              |
| `onPrimaryContainer`  | Text/icons on `primaryContainer`                                            |
| `secondary`           | Less prominent actions, outlined button borders, secondary icons            |
| `onSecondary`         | Text/icons **on** `secondary`                                               |
| `secondaryContainer`  | Section backgrounds, cards with light accent                                |
| `onSecondaryContainer`| Text inside `secondaryContainer`                                            |
| `tertiary`            | Medium-risk / warning indicators, accent colors                             |
| `onTertiary`          | Text/icons on `tertiary`                                                    |
| `tertiaryContainer`   | Warning toast or medium-risk background                                     |
| `background`          | Screen-level background (the canvas)                                        |
| `onBackground`        | Primary text **directly** on `background`                                   |
| `surface`             | Card, dialog, bottom sheet backgrounds                                      |
| `onSurface`           | Primary text inside cards, dialogs, sheets                                  |
| `surfaceVariant`      | Subtle container backgrounds, section dividers                              |
| `onSurfaceVariant`    | **Secondary text**, captions, metadata, disabled labels                     |
| `outline`             | Borders, dividers (prominent)                                               |
| `outlineVariant`      | Subtle borders, light separators                                            |
| `error`               | High-risk indicators, destructive actions, overdue amounts                  |
| `onError`             | Text / icons on `error` background                                          |
| `errorContainer`      | Subtle error background (e.g., overdue rows)                                |
| `onErrorContainer`    | Text on `errorContainer`                                                    |

### Quick Decision Tree for Colors

```
What kind of text?
├─ Primary / prominent → onSurface
├─ Secondary / caption → onSurfaceVariant
├─ On a colored background → on<Role> (e.g., onPrimary, onSecondary)
└─ Error / urgent → error

What kind of background?
├─ Screen-level → background
├─ Card / dialog → surface
├─ Highlighted card → surfaceVariant or primaryContainer.copy(alpha = 0.15f)
├─ Error row → errorContainer
└─ Divider / border → outline or outlineVariant
```

### ❌ Wrong (Hardcoded)

```kotlin
// BREAKS in dark mode — always gray
Text("Hello", color = Color.Gray)

// BREAKS in dark mode — always white
Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)))

// BREAKS in dark mode — always light red
BorderStroke(1.dp, Color(0xFFFFCDD2))
```

### ✅ Correct (Theme-Aware)

```kotlin
Text("Hello", color = MaterialTheme.colorScheme.onSurfaceVariant)

Card(colors = CardDefaults.cardColors(
    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
))

BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
```

---

## 3. Typography: Use Theme Styles, Not Hardcoded Values

### Material 3 Type Scale Reference

| Theme Style         | Default Size | Weight  | Typical Use                      |
|---------------------|-------------|---------|-----------------------------------|
| `displayLarge`      | 57.sp       | Normal  | Hero / splash text                |
| `displayMedium`     | 45.sp       | Normal  | Large headlines                   |
| `displaySmall`      | 36.sp       | Normal  | Big numbers                       |
| `headlineLarge`     | 32.sp       | Normal  | Dashboard totals                  |
| `headlineMedium`    | 28.sp       | Normal  | Screen title                      |
| `headlineSmall`     | 24.sp       | Normal  | Page subtitle                     |
| `titleLarge`        | 22.sp       | Medium  | App bar title                     |
| `titleMedium`       | 16.sp       | Medium  | Section headers, dialog titles    |
| `titleSmall`        | 14.sp       | Medium  | Client names, list item titles    |
| `bodyLarge`         | 16.sp       | Normal  | Main content paragraphs            |
| `bodyMedium`        | 14.sp       | Normal  | Secondary content, empty states   |
| `bodySmall`         | 12.sp       | Normal  | Captions, metadata, timestamps    |
| `labelLarge`        | 14.sp       | Medium  | Buttons, emphasized labels        |
| `labelMedium`       | 12.sp       | Medium  | Chips, small buttons              |
| `labelSmall`        | 11.sp       | Medium  | Badges, tiny counters              |

### ❌ Wrong

```kotlin
// Hardcoded size + weight — does NOT respect system font scaling
Text(
    text = "Client Name",
    fontWeight = FontWeight.Bold,
    fontSize = 14.sp
)

// Theme style + redundant (and separate) fontWeight — works but less clean
Text(
    text = "Section Title",
    style = MaterialTheme.typography.titleMedium,
    fontWeight = FontWeight.Bold  // overrides the style's own fontWeight
)
```

### ✅ Correct

```kotlin
// Use theme style directly
Text(
    text = "Client Name",
    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
)

// Use .copy() when you need a semantic override
Text(
    text = "Section Title",
    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
)

// Plain theme style — no overrides needed
Text(
    text = "Subtitle text here",
    style = MaterialTheme.typography.bodyMedium
)
```

### Mapping Common Sizes to Theme Styles

| Old Hardcoded           | Replace With                                         |
|-------------------------|------------------------------------------------------|
| `fontSize = 18.sp`      | `style = MaterialTheme.typography.titleLarge`        |
| `fontSize = 16.sp, Bold`| `style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)` |
| `fontSize = 15.sp`      | `style = MaterialTheme.typography.titleMedium.copy(fontWeight = ...)` |
| `fontSize = 14.sp, Bold`| `style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)` |
| `fontSize = 13.sp`      | `style = MaterialTheme.typography.bodyMedium`        |
| `fontSize = 12.sp, Bold`| `style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)` |
| `fontSize = 12.sp`      | `style = MaterialTheme.typography.bodySmall`         |
| `fontSize = 11.sp, Bold`| `style = MaterialTheme.typography.labelMedium`       |
| `fontSize = 11.sp`      | `style = MaterialTheme.typography.bodySmall`         |
| `fontSize = 10.sp`      | `style = MaterialTheme.typography.labelSmall`        |
| `fontSize = 9.sp`       | `style = MaterialTheme.typography.labelSmall`        |

---

## 4. Common Mistakes & Fixes

### Mistake 1: Using `Color.Gray` for secondary text

```kotlin
// ❌ WRONG
Text("Description here", color = Color.Gray)

// ✅ CORRECT
Text("Description here", color = MaterialTheme.colorScheme.onSurfaceVariant)
```

### Mistake 2: Hardcoded container colors

```kotlin
// ❌ WRONG — light-green tint that stays light in dark mode
Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)))

// ✅ CORRECT — adapts to both modes
Card(colors = CardDefaults.cardColors(
    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
))
```

### Mistake 3: Hardcoded border colors

```kotlin
// ❌ WRONG
.border(1.dp, Color.LightGray)

// ✅ CORRECT
.border(1.dp, MaterialTheme.colorScheme.outlineVariant)
```

### Mistake 4: Hardcoded font sizes directly on Text

```kotlin
// ❌ WRONG
Text("Hello", fontSize = 14.sp)

// ✅ CORRECT
Text("Hello", style = MaterialTheme.typography.bodyMedium)
```

### Mistake 5: Using both `style` and `fontWeight` as separate params

```kotlin
// ❌ WORKS but inconsistent — fontWeight overrides style's weight separately
Text("Title", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

// ✅ CORRECT — single source of truth
Text("Title", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
```

### Mistake 6: Using hardcoded hex for icon tints

```kotlin
// ❌ WRONG
Icon(Icons.Default.Search, contentDescription = null, tint = Color.LightGray)

// ✅ CORRECT
Icon(Icons.Default.Search, contentDescription = null,
     tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
```

### Mistake 7: Hardcoded colors inside `textStyle = TextStyle(...)`

```kotlin
// ❌ WRONG — TextStyle accepts fontSize, fontFamily, etc. NOT `style`
textStyle = TextStyle(fontSize = 13.sp)

// ❌ WRONG — TextStyle does NOT have a `style` parameter
textStyle = TextStyle(style = MaterialTheme.typography.bodyMedium)

// ✅ CORRECT — pass the theme TextStyle directly
textStyle = MaterialTheme.typography.bodyMedium

// ✅ CORRECT — or copy with overrides
textStyle = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Content)
```

### Mistake 8: Leaving a hardcoded `colorHex` in an enum

```kotlin
// ❌ WRONG — these hex values will be used directly instead of theme colors
enum class RiskLevel(val colorHex: Long) {
    HIGH_RISK(0xFFD32F2F),
    COMMITTED(0xFF388E3C)
}

// ✅ CORRECT — use composable extension functions that reference the current theme
@Composable
fun RiskLevel.toColor(): Color = when (this) {
    RiskLevel.HIGH_RISK -> MaterialTheme.colorScheme.error
    RiskLevel.COMMITTED -> MaterialTheme.colorScheme.primary
}
```

---

## 5. Adding a New Screen: Checklist

Before committing a new composable screen:

- [ ] **Search for hardcoded colors** — any `Color.Gray`, `Color.Black`, `Color(0xFF…)`?
- [ ] **Search for hardcoded font sizes** — any `fontSize = N.sp`?
- [ ] **Every background** uses `MaterialTheme.colorScheme.{role}`
- [ ] **Every text color** uses `MaterialTheme.colorScheme.on{…}` or `MaterialTheme.typography`
- [ ] **Every border** uses `MaterialTheme.colorScheme.outline` or `outlineVariant`
- [ ] **Every font** uses `style = MaterialTheme.typography.{style}`
- [ ] **Overrides** use `.copy()` not separate `fontWeight`/`fontSize` params
- [ ] **Icons** use theme-aware `tint`
- [ ] **Divider** uses `MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)`

---

## 6. Theme Definition Best Practices

### Colors (`Color.kt`)

- Define **one pair of constants per role**: a light variant and a `*Dark` variant.
- Name them consistently: `Natural<Role>` and `Natural<Role>Dark`.
- Only define the roles actually used in the theme. See the existing `Color.kt` as reference.

### Theme (`Theme.kt`)

- Map every `colorScheme` property explicitly in both `LightColorScheme` and `DarkColorScheme`.
- Never leave a role unmapped — rely on your constants, not Material defaults.

### Typography (`Type.kt`)

- Define all 13 Material 3 styles explicitly.
- Use `FontFamily.Default` (the system handles Arabic, CJK, Latin correctly).
- Set line heights at least 1.5× font size to accommodate Arabic diacritics.
- Keep letter spacing at `0.sp` for Arabic readability; only use `0.5.sp` for body text.

```kotlin
// Example: correct Type.kt structure
val Typography = Typography(
    displayLarge = TextStyle(fontSize = 57.sp, lineHeight = 68.sp, ...),
    displayMedium = TextStyle(fontSize = 45.sp, lineHeight = 56.sp, ...),
    // ... all 13 styles ...
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, ...),
)
```

---

## Summary

| ❌ Don't                                   | ✅ Do                                                              |
| ----------------------------------------- | ----------------------------------------------------------------- |
| `Color.Gray`                              | `MaterialTheme.colorScheme.onSurfaceVariant`                      |
| `Color.LightGray`                         | `MaterialTheme.colorScheme.outlineVariant`                        |
| `Color.Black`                             | `MaterialTheme.colorScheme.onSurface`                             |
| `Color(0xFFF1F8E9)`                       | `MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)`   |
| `fontSize = 14.sp`                        | `style = MaterialTheme.typography.bodyMedium`                     |
| `fontWeight = FontWeight.Bold` (separate) | `.copy(fontWeight = FontWeight.Bold)` on the style                |
| `textStyle = TextStyle(fontSize = X.sp)`  | `textStyle = MaterialTheme.typography.xxx`                        |
| Hardcoded hex in enum                     | `@Composable` extension function with `MaterialTheme.colorScheme` |
