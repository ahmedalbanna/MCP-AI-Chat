---
name: minimal-premium-audit
description: Audits Compose UI against the minimal-premium visual language (quiet surfaces, single accent, sentence-case labels). Use ONLY when reviewing or restyling app screens, cards, sheets, or markdown rendering.
---

# Minimal Premium Audit

The app's visual language (established `fddf775`/`3a13a2f`/`2f423ae`): quiet surfaces, one accent, no decoration.

## Language rules

- Assistant content is borderless text (`onSurface`); only user bubbles get a pill (`secondaryContainer`, `Dimens.chatUserBubbleRadius 20dp`).
- Cards: `surfaceContainerLow 0.5` + `outlineVariant 0.6` hairline. Never `surfaceVariant` fills or `primaryContainer` tints.
- Text: `onSurface` primary, `onSurfaceVariant` secondary/metadata. `primary` is reserved for the single accent per surface (send button, key CTA, links, streaming cursor).
- Status uses theme roles only: `tertiary` ok, `secondary` pending, `error` failure. Raw brand tokens (`IndigoPrimary`, `CyanSecondary`, …) must only appear in `ui/theme/` definitions.
- Typography: `titleLarge SemiBold` screen titles, `labelMedium/Small` metadata, `Medium/SemiBold` emphasis — never `Bold`/`Black` display headings in content. Labels are sentence-case ("Target tool", not "TARGET MCP TOOL:").
- No pulse/glow `rememberInfiniteTransition` decorations; static indicators only.
- Full-pill (`cornerRadiusFull`) chips/toggles; `chatInputRadius 28dp` fields with `surfaceContainerHigh` fill + `outlineVariant` border.
- Warnings: quiet card + amber icon (`AmberWarning`) + `onSurfaceVariant` text. Never green (`tertiary`) for warnings.
- Sizes via `Spacing`/`Dimens` tokens only; no hardcoded dp.

## Audit sweep (ripgrep)

- `IndigoPrimary|CyanSecondary|EmeraldTertiary|AmberWarning|RoseError` outside `ui/theme/` and `DesignSystem.kt`
- `surfaceVariant\.copy|primaryContainer\.copy` in screens
- `FontWeight.Bold|FontWeight.Black|headlineSmall|headlineLarge` in content
- `rememberInfiniteTransition` in screens
- `[0-9]+\.dp` not via `Dimens.`/`Spacing.`

## After changes

Re-record Roborazzi goldens (`:app:recordRoborazziDebug -PscreenshotTests=true`); visuals shift snapshots by design.
