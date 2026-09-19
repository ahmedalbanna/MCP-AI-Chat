# UX Overhaul Roadmap — Senior Compose Audit (Sept 2026) — **IMPLEMENTED 2026-09-19 (Stages 1-5)**

Source: full reads of `ChatScreen`, `ChatMessageItem`, `ChatEmptyState`, `ChatStreamingWidgets`, `Components`, `Settings/Servers/ServerEdit/Providers/ProviderEdit/Onboarding/Inspector`, `AdaptiveLayout`, `Theme/Color/Type/Dimens/DesignSystem`, `MainActivity`, `StateHandler`, `THEME_GUIDE` + Arabic guides. Parent tracker: `ROADMAP.md Phase 14`.

Decisions: full overhaul / M3 roles win / `Type.kt` to M3 spec / phone+tablet+foldable.

## 1. Design tokens

| File | Fix | Why |
|------|-----|-----|
| `Theme.kt:18-88` | Map `onErrorContainer`, `outlineVariant`, `surfaceContainer*`, `surfaceBright/Dim`, `inverse*`, `scrim`; light `errorContainer` to pink; Slate for black hex; split border/surfaceVariant; add `shapes`; light nav-bar + cutout | M3 defaults leak (e.g. `ProvidersScreen:121` purple-grey); maroon-on-light inverted; AMOLED washout |
| `Color.kt:6-58` | Light primary `6366F1→4F46E5`; darker text variants for Cyan/Emerald/Amber/Rose; `link`→primary+underline; `ColorTokens` via `colorScheme` | 3.9:1 and 2.0-3.5:1 fail WCAG AA 4.5:1; half-dynamic UI when `useDynamicColor` on |
| `Type.kt:9-108` | Spec sizes 57/45/36/32/28/24/22/16/14/16/14/12/14/12/11; line-height ≥1.5x small; `0.sp` spacing for Arabic; add `displaySmall`; Cairo `FontFamily` in `res/font/` | Current 34/28/24/20/18/16/10 shrunk; diacritics clip; `THEME_GUIDE:311` vs Arabic guide conflict |
| `Dimens.kt` | Drop `20/14/10dp`; `9999.dp`→`CircleShape`; centralize Shapes/elevations; enforce 48dp | Off-grid drift; shape divergence (`Theme.kt:115` omits `shapes`) |
| All screens | Replace ~80 raw brand usages; `Icons.Default`→`AutoMirrored` (79 hits); sizes 18/24/36/48 | Dark/dynamic breakage; RTL arrows wrong |

Re-snapshot all Roborazzi goldens after type bump.

## 2. Chat core

- `ChatScreen:224-363`: TopAppBar single-line contract; scrollable empty view (clips ~700dp content on 640dp landscape); group Regenerate with bubble; gate empty on `streamingText/isGenerating`; mirror fade via `LocalLayoutDirection`; placeholder-only field; `clearFocus()` on send; `DateTimeFormatter`; sheet `heightIn 560.dp`+insets; single inset owner.
- `MessageItem/Streaming`: JSON parse to VM/`Dispatchers.Default`; `TextOverflow.Ellipsis` + bidi isolation; `Show more`→`TextButton`; delete dead `TypingIndicator`; 48dp on Regenerate/remove/expand; scroll only on `lastId`+first token (not every `length` change).
- `Components:152-486`: `IntrinsicSize.Min` accent bar; single click target; ripple on; markdown parse hoisted (plain `Text` while streaming, fixes O(n²)); `key` per card.
- Lists/state: `key={it.id}` for servers/sessions; `remember/derivedStateOf` for `find/count` per token; search `debounce 300ms`; reset `_hasPendingUserTurn` on cancel; banner Retry; sheet loading/empty; result highlight; delete undo.

## 3. IA, navigation, forms

- `MainActivity/AdaptiveLayout`: Providers discoverable (rail/bar or Settings link); keep rail on edit routes; pane toggle/`X`; edits in secondary pane/`Dialog`; unify path/query routes; unfold must not drop dirty forms; Onboarding as destination with `BackHandler`; hinge `centerX` in window coords; consider `Medium` two-pane.
- `ServerEdit/ProviderEdit`: `trim()`+`Uri` validation; inline errors not disabled-mystery; `KeyboardOptions/Requester/imePadding`; persistent errors + Undo/Retry; fix add-on-edit-missing + empty-DB infinite `Loading provider…`; `rememberSaveable`; provider HTTP warning + Google key required; reset URL/model on type change; dirty guard.
- `Inspector/Settings/Onboarding/StateHandler`: `TextButton` over fake links; required-missing text; block Invoke on invalid; dialog outside `LazyColumn`; one destructive style; missing `SnackbarHost`; Settings hub + full-row toggle + live preview + Providers link; delete/fix `StateHandler`; Onboarding real `Pager` + sticky CTA + `commit()`/`DataStore` + pause animations hidden.

## 4. Adaptive + RTL + insets

Bubble `0.80f`→`BoxWithConstraints(maxWidth=600.dp)` (672dp unreadable on 840dp tablet); `aspectRatio` media; fade click-through; multi-hinge; vertical `HALF_OPENED`; foldable screenshots. RTL wrapper test path; directional shapes; `textDirection=Content`; wire `ArabicNumerals` (unused today). Unify insets to `safeDrawing/displayCutout/ime`.

## 5. Docs debt

5 agent-bar docs ~95% duplicated — dedupe; fix `THEME_GUIDE` spec-vs-code sizes, `Default` vs Cairo; add WCAG table, shapes policy, harmonization policy, spacing lint rule.

## 6. Execution order + gates

1. Tokens + icons. 2. Chat perf/a11y. 3. Forms/nav. 4. Adaptive/RTL/insets. 5. Docs + goldens.
Gate: `detekt` (0, 150 chars) → `spotlessCheck` → `testDebugUnitTest -PscreenshotTests=true` → `:app:recordRoborazziDebug` (light/dark/AMOLED/RTL/tablet/fold) → `lintDebug` → `koverVerify 60%`, JDK 21. TalkBack + contrast spot-checks required.
