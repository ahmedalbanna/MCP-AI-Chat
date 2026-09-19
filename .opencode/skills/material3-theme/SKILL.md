---
name: material3-theme
description: MCP AI Chat Material3 theme tokens (color schemes, M3 type scale, spacing grid, shapes) in ui/theme/. Use when changing colors, typography, spacing, dark/AMOLED/dynamic-color behavior, or the DesignSystem preview.
---

# Material3 Theme

Source files: `ui/theme/Theme.kt`, `Color.kt`, `Type.kt`, `Dimens.kt`, `DesignSystem.kt`.

## Color (`Color.kt` + `Theme.kt`)

- Three schemes — `lightColorScheme`, `darkColorScheme`, `amoledDarkColorScheme` — must map every role explicitly: `onErrorContainer`, `outlineVariant`, `surfaceContainer*`, `surfaceBright/Dim`, `inverse*`, `surfaceTint`, `scrim`. Missing roles fall back to M3 baseline purple-grey.
- Light theme contrast rules (WCAG AA 4.5:1): `primary` is `IndigoPrimaryDark (0xFF4F46E5)`, never `0xFF6366F1`. Status text uses the `*TextOnLight` variants (`Cyan/ Emerald/AmberTextOnLight`, `RoseErrorOnLight`). `link` is `IndigoPrimaryDark`, not cyan.
- `errorContainer` differs per mode: light pink `RoseErrorContainerLight`, dark maroon `RoseErrorContainerDark` (aliased as `RoseErrorContainer`).
- `SlateBorderDark (0xFF334155)` must stay distinct from `SlateSurfaceVariantDark (0xFF1E293B)`; the old shared value is now `SlateBorderVariantDark`.
- `AppShapes` is passed to `MaterialTheme(shapes = ...)`; corner tokens live in `Dimens`, full-round uses `CircleShape` (never `9999.dp` in new code).
- After any theme/type change, re-record goldens: `:app:recordRoborazziDebug -PscreenshotTests=true`.

## Typography (`Type.kt`)

- M3 spec scale: display 57/45/36, headline 32/28/24, title 22/16/14, body 16/14/12, label 14/12/11. `bodySmall` line-height 20sp (≥1.5x for diacritics). All `letterSpacing 0.sp` for Arabic.
- Cairo font wiring is deferred (needs the `res/font/` asset); new styles stay on `FontFamily.Default` until then.

## Spacing (`Dimens.kt`)

- 4dp grid via `Spacing`: xxs 2, xs 4, sm 8, md 12, lg 16, xl 24, xxl 32, xxxl 48. `sheetPadding` 16, `cardPadding` 12, `sectionGap` 12. No `10/14/20dp` literals.
- Icon sizes: 12/14/18/24/36 (`iconSizeXs/Sm/Md/Lg/Xl`). Attachment thumbs use `Dimens.attachmentThumb` (56dp). Elevations via `Dimens.elevation*`, never literals.
