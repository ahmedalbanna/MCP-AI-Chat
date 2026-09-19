---
name: compose-ux-review
description: Senior Jetpack Compose UI/UX audit checklist for MCP AI Chat screens (Material3 roles, accessibility, recomposition perf, sheets/dialogs, IME, adaptive). Use when writing or reviewing any Composable in app/src/main/java/com/aistudio/mcpchat/ui/.
---

# Compose UX Review

Apply to every new or changed screen. Full rationale: `docs/UX-ROADMAP.md`, Phase 14.

## Material3 roles (never raw tokens)

- No `IndigoPrimary`, `CyanSecondary`, `EmeraldTertiary`, `AmberWarning`, `RoseError`, `Color(0xFF…)` in Composables. Use `MaterialTheme.colorScheme` roles: `primary`/`onPrimary`, `secondary`, `tertiary`, `error`/`onError`/`errorContainer`/`onErrorContainer`, `surfaceVariant`, `outline`/`outlineVariant`.
- Text: `primary` for links/actions, `secondary`/`tertiary` for status accents, `onSurfaceVariant` for secondary text (no `.alpha()` dimming — it breaks contrast).
- Typography: `MaterialTheme.typography.*` plus `.copy()` only. Never `fontSize =`, never separate `fontWeight =` params, never hardcoded `lineHeight`.

## Accessibility (TalkBack + touch)

- Every interactive element: 48dp minimum (`minimumInteractiveComponentSize`), real `role = Role.Button`, meaningful `contentDescription` (no `"AI"`, no double announcements inside a labelled parent).
- `Text` + `clickable` is forbidden for actions — use `TextButton`/`Surface(onClick=)`.
- One click target per card: header toggles expand, so the expand chevron must be a non-clickable `Icon`, not a nested `IconButton`.
- `liveRegion = Polite` on streaming/assistant content, `Assertive` on errors; `stateDescription` for expanded/selected/on-off states; `heading()` on screen/section titles.

## Performance

- `LazyColumn` `items()` always gets `key = { it.id }`.
- Never `remember(x)` inside a `for` loop over variable-length data — extract a child `@Composable` (`MarkdownBullet`/`MarkdownParagraph` pattern) or hoist `remember(text)` above the loop.
- Streaming path renders plain `Text`; full markdown parse only for persisted messages.
- Scroll effects key on message identity + streaming-started boolean, never on `streamingText.length`.
- `sessions.find {}` / `servers.count {}` go in `remember`/`derivedStateOf`, not inline.

## Structure

- Error banners use `errorContainer`/`onErrorContainer` with a working action (Retry/Dismiss), never dismiss-only with dead state.
- `AlertDialog` lives outside `LazyColumn` scope; delete/destructive actions always confirm (see `ProvidersScreen` pending-delete pattern).
- Forms: inline `isError` + `supportingText` (never disabled-save-with-no-explanation), `trim()` before validation, `KeyboardOptions`/`KeyboardActions` on every field, dirty `BackHandler` + discard dialog.
- Bubbles cap at `fillMaxWidth(0.80f).widthIn(max = Dimens.chatBubbleMaxWidthDp)`; sheets get `heightIn(max = 560.dp)`; one inset owner per screen (`imePadding` on `Scaffold`, no extra `navigationBarsPadding` on the input).
