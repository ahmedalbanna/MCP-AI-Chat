# Plan: Implement Best Practices from `docs/`

## Goal

Adopt the highest-value patterns from `docs/AGENTS.md`, `docs/ARABIC_UI_GUIDE.md`, and `docs/THEME_GUIDE.md` into the existing MCP-AI-Chat codebase without rewriting the app architecture.

## Scope

**In scope:**
- Arabic/RTL foundation (Theme wrapper, Cairo font, ArabicNumerals, AutoMirrored icon audit)
- State-management improvements (`Product<T>`, `StateHandler`, `sealed` event patterns)
- Theme conventions alignment (no hardcoded colors/sizes, proper Material 3 roles)

**Out of scope (explicitly deferred):**
- Full Clean Architecture refactor (`domain/`, `data/`, `presentation/` separation)
- Repository interfaces / use-case layer
- Retrofit + Kotlin Serialization migration
- Type-safe navigation (`@Serializable` routes)
- Full string-resource extraction for localization

---

## Task 1: Add RTL Foundation to Theme System

**Files:**
- `app/src/main/java/com/aistudio/mcpchat/ui/theme/Theme.kt`
- `app/src/main/java/com/aistudio/mcpchat/ui/theme/Type.kt`

**Changes:**

1. Wrap `MaterialTheme` in `CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl)` in `Theme.kt` (`Theme.kt:62-78` area).
2. Add `CairoFontFamily` definition in `Type.kt` using `FontFamily(...)` with weights Light/Normal/Medium/SemiBold/Bold.
3. Update all 13 `TextStyle` entries in `Type.kt` to use `fontFamily = CairoFontFamily`.
4. Ensure `Theme.kt` passes `Typography` to `MaterialTheme(typography = Typography, ...)`.

**Validation:** App builds with `./gradlew assembleDebug`; no compile errors from missing font resources.

---

## Task 2: Create ArabicNumerals Helper

**New file:** `app/src/main/java/com/aistudio/mcpchat/util/ArabicNumerals.kt`

**Contents (per `docs/AGENTS.md` §3.4 and `docs/ARABIC_UI_GUIDE.md` §4.2):**
- `object ArabicNumerals` with `convert(input: String)`, `formatCurrency(amount: Double, symbol: String = "ر.ي")`, `formatPercent(percent: Int)`, `formatNumber(number: Long)`.

**Validation:** Unit test verifies `convert("1500")` returns `"١٥٠٠"`.

---

## Task 3: AutoMirrored Icon Audit

**Files to audit and fix (non-exhaustive):**
- `MainActivity.kt` — `AppNavGraph`, `AppBottomBar`, `AppNavigationRail`
- `ui/screens/ChatScreen.kt`
- `ui/screens/ServersScreen.kt`
- `ui/screens/ServerEditScreen.kt`
- `ui/screens/InspectorScreen.kt`
- `ui/screens/SettingsScreen.kt`
- `ui/screens/ProvidersScreen.kt`
- `ui/screens/ProviderEditScreen.kt`
- `ui/screens/Components.kt`
- `ui/adaptive/AdaptiveLayout.kt`

**Rule:** Any icon with directional semantics MUST use `Icons.AutoMirrored.Filled.*` or `Icons.AutoMirrored.Outlined.*`:
- `ArrowBack`, `ArrowForward`, `ExitToApp`, `ReceiptLong`, `List`, `Send`, `TrendingUp`, `ChevronRight`, `NavigateNext`, `KeyboardArrowRight`, `ArrowDropDown`, `EventNote`

**Do NOT change:** Symmetric icons (`Add`, `Search`, `Settings`, `Person`, `Store`, `Edit`, `Delete`, `Email`, `Phone`, `Star`, `Home`, `Favorite`, `Close`, `Menu`, `MoreVert`, `CheckCircle`, `Error`, `Info`, `ContentCopy`, `Image`, `Stop`, `ExpandLess`, `ExpandMore`).

**Validation:** Grep for `Icons.Default.ArrowBack`, `Icons.Default.ExitToApp`, `Icons.Default.ReceiptLong`, `Icons.Default.List`, `Icons.Default.ChevronRight`, `Icons.Default.NavigateNext`, `Icons.Default.KeyboardArrowRight`, `Icons.Default.ArrowDropDown` — zero matches after fix.

---

## Task 4: Adopt Product<T> Sealed Class

**New file:** `app/src/main/java/com/aistudio/mcpchat/util/Product.kt`

```kotlin
sealed class Product<out T> {
    data class Success<T>(val data: T) : Product<T>()
    data class Error<T>(val message: String, val exception: Throwable? = null) : Product<T>()
    data class Loading<T>(val cachedData: T? = null) : Product<T>()
}
```

**Adoption:**
- Replace `Result<T>` return types in ViewModels and repositories with `Product<T>` where they represent async operation outcomes.
- Priority order for adoption: `ChatViewModel`, `McpServersViewModel`, `AiProviderSettingsViewModel`, `McpInspectorViewModel`.
- Update corresponding tests to assert on `Product.Success`, `Product.Error`, `Product.Loading`.

**Validation:** `./gradlew testDebugUnitTest` passes; no `Result` references remain in ViewModel layer.

---

## Task 5: Add StateHandler Generic Composable

**New file:** `app/src/main/java/com/aistudio/mcpchat/ui/component/StateHandler.kt`

**Contents (per `docs/AGENTS.md` §4.2):**

```kotlin
@Composable
fun <T> StateHandler(
    state: Product<T>,
    onLoading: @Composable (cachedData: T?) -> Unit = { CircularProgressIndicator() },
    onError: @Composable (message: String, onRetry: () -> Unit) -> Unit,
    onSuccess: @Composable (data: T) -> Unit
) {
    when (state) {
        is Product.Loading -> onLoading(state.cachedData)
        is Product.Error -> onError(state.message, onRetry)
        is Product.Success -> onSuccess(state.data)
    }
}
```

**Adoption:** Replace ad-hoc `when (uiState.isLoading)` / `uiState.error != null` blocks in `ChatScreen.kt` and `ServersScreen.kt` with `StateHandler`.

**Validation:** UI tests in `ComponentsTest` cover loading, error, and success states.

---

## Task 6: Theme Convention Cleanup

**Files:**
- `ui/screens/*.kt`
- `ui/theme/*.kt`

**Rules (per `docs/THEME_GUIDE.md`):**
- No `Color.Gray`, `Color.Black`, `Color.LightGray`, `Color(0xFF...)` inside composables.
- No `fontSize = N.sp` or standalone `fontWeight = FontWeight.Bold` on `Text`.
- Use `MaterialTheme.colorScheme.{role}` for all colors.
- Use `style = MaterialTheme.typography.{style}.copy(fontWeight = ...)` for overrides.
- `Divider` uses `MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)`.
- Icon `tint` comes from `MaterialTheme.colorScheme`.

**Validation:** Run `./gradlew lintDebug`; manually review any remaining hardcoded colors in UI files.

---

## Task 7: Add Sealed Event Pattern (Channel + receiveAsFlow)

**Target:** ViewModels that currently expose one-off methods for navigation/snackbar events.

**Pattern (per `docs/AGENTS.md` §4.3):**
```kotlin
sealed interface UiEvent { ... }
private val _uiEvent = Channel<UiEvent>(Channel.BUFFERED)
val uiEvent = _uiEvent.receiveAsFlow()
```

**Adoption priority:**
1. `ChatViewModel` — replace direct `navController` / `snackbarHostState` calls with `UiEvent` emissions.
2. `AiProviderSettingsViewModel` — same.

**Validation:** ViewModel tests assert that `uiEvent` emits expected events; Compose collects via `LaunchedEffect`.

---

## Execution Order

| Step | Task | Depends on |
|------|------|------------|
| 1 | Task 1: RTL + Cairo font | — |
| 2 | Task 2: ArabicNumerals | — |
| 3 | Task 3: AutoMirrored icon audit | Task 1 (RTL must be enabled to verify visual correctness) |
| 4 | Task 4: Product<T> | — |
| 5 | Task 5: StateHandler composable | Task 4 (depends on `Product<T>`) |
| 6 | Task 6: Theme convention cleanup | — |
| 7 | Task 7: Sealed event pattern | — |

---

## Risks

- **Icon audit scope:** ChatScreen.kt is 1028+ lines; missing a directional icon is likely. Mitigate with grep-based verification after edits.
- **Product<T> adoption:** Some ViewModels mix `Result` and `StateFlow`; partial adoption may leave inconsistent patterns. Mitigate by adopting all ViewModels in one pass.
- **RTL visual regression:** Enabling RTL globally may shift layouts unexpectedly. Mitigate by testing all screens in RTL mode before merging.

---

## Validation Checklist

- [ ] `./gradlew assembleDebug` succeeds
- [ ] `./gradlew testDebugUnitTest` passes
- [ ] `./gradlew lintDebug` passes
- [ ] `./gradlew detekt` passes (maxIssues = 0)
- [ ] `./gradlew spotlessCheck` passes
- [ ] Zero grep matches for hardcoded `Color.Gray`, `Color.Black`, `Color(0xFF...` in UI composables
- [ ] Zero grep matches for `Icons.Default.ArrowBack`, `Icons.Default.ExitToApp`, `Icons.Default.ReceiptLong`, `Icons.Default.List`, `Icons.Default.ChevronRight`, `Icons.Default.NavigateNext`, `Icons.Default.KeyboardArrowRight`, `Icons.Default.ArrowDropDown`
- [ ] Manual RTL verification on all screens
- [ ] Arabic numerals display correctly in currency/percentage contexts
