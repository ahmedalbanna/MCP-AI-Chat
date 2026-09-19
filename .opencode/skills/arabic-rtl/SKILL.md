---
name: arabic-rtl
description: Arabic RTL support conventions for MCP AI Chat (directional shapes, AutoMirrored icons, bidi text, Eastern numerals). Use when adding UI text, icons, shapes, gradients, or user-visible numbers.
---

# Arabic RTL

`AndroidManifest.xml` sets `supportsRtl="true"`. The app defaults to LTR in English; these rules keep Arabic content correct via BiDi plus locale-aware numerals.

## Layout and shapes

- Bubble/card corners must use directional `topStart/topEnd/bottomStart/bottomEnd` — never positional `RoundedCornerShape(a, b, c, d)`, which flips the tail side in RTL.
- Directional icons use `Icons.AutoMirrored.*` (`Send`, `ArrowBack`, `ArrowForward`, `PlayArrow`, dropdown chevrons). Symmetric icons (`Add`, `Search`, `Settings`, `Delete`) stay `Icons.Default`.
- Edge fades/gradients mirror with `LocalLayoutDirection`: in RTL align `CenterStart` and reverse the gradient stops (see chips fade in `ChatScreen`).
- `Arrangement.Start/End`, `Alignment.Start/End`, `padding(start/end)` — never absolute left/right.

## Text and numerals

- Mixed Arabic/Latin message text: `style.copy(textDirection = TextDirection.Content)` on user bubbles, streaming text, and markdown (`InlineMarkdownText`). Never hand-truncate with `take(n) + "..."` for displayed text — use `TextOverflow.Ellipsis`.
- All type styles keep `letterSpacing 0.sp`; Arabic joining breaks with Latin tracking.
- User-visible counters use `ArabicNumerals.convertIfArabic(...)` (`util/ArabicNumerals.kt`) — Eastern digits under `ar` locale, Western otherwise. Phone numbers, API payloads, dates, and text-field input stay Western always.
