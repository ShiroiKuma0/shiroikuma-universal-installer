# UI/UX Improvement Plan

Generated 2026-06-07 from a full-app audit. Each phase ships independently — pick up
where the previous one left off without breaking anything.

Status legend: `[ ]` pending · `[~]` in progress · `[x]` done

---

## Phase 1 — Quick wins (~½ day)

Low-effort fixes that visibly improve daily use. Ship as one commit.

- [x] **1.1 Search debounce in FoundApksSheet** — `FoundApksSheet.kt:278`
  - Wrap query state in a `MutableStateFlow`, collect with `.debounce(300)` →
    set the filtered list. Today every keystroke recomputes the filter against
    1000+ files.
- [x] **1.2 IconButton ≥48dp** — `ApkInfoContent.kt:543` (OBB delete) + sweep for
    other `IconButton` calls without `.size(48.dp)`. Use `Modifier.minimumInteractiveComponentSize()`
    where the visual icon is smaller than 48dp.
- [x] **1.3 Empty state for permissions list** — `ApkInfoContent.kt:462`
  - When `apkInfo.permissions.isEmpty()`, render a single neutral row
    "No permissions requested" instead of the empty toggle.
- [x] **1.4 VirusTotal error surfacing** — `DialogMenuContent.kt:520`
  - Add a `Failed` arm to the existing scan-status branching that renders the
    failure message in `colorScheme.error`; otherwise users see "Not scanned"
    when an API call actually errored.
- [x] **1.5 Biometric toggle feedback** — `SettingScreen.kt` biometric toggles
  - Emit a one-shot `_events` Snackbar/Toast when the pref flip persists, mirroring
    how Default-installer toggle already does it.

**Acceptance:** all 5 items merged behind one commit; no behavior change beyond
visible polish; build green on debug.

---

## Phase 2 — Manage screen overhaul (~1.5 days)

The largest UX gap. Today Manage is a single-tap-per-app list — cleanup workflows
require 20+ trips through bottom sheets.

Most of this turned out to be already built — the gaps were batch ops beyond
uninstall, the skeleton, and the clear-filters action.

- [x] **2.1 Multi-select mode** — already implemented (long-press →
    `combinedClickable`, `selectedPackages` StateFlow, "N selected" top bar,
    select-all, Close to exit). No changes needed.
- [x] **2.2 Batch operations**
  - Batch uninstall was already wired. Added batch **Force-stop · Disable ·
    Clear data** via a single `runPrivilegedBatch` helper (resolves the
    privileged executor once, skips our own package, reloads once, emits one
    summary snackbar). Surfaced in a selection-mode overflow menu, shown only
    when Root/Shizuku is ready. Batch clear-data gets a destructive confirm.
  - Deviation from plan: used a summary snackbar (via `PrivilegedActionResult`)
    rather than the uninstall notification channel — these `pm`/`am` shell ops
    are near-instant, unlike uninstall sessions, so per-item progress isn't
    warranted and the notifier's wording is uninstall-specific.
- [x] **2.3 Usage stats column** — already implemented (`queryLastUsedMap` at
    load, `lastUsedAt` rendered per row, `LastUsed` sort chip, 7-day usage chart
    in the action sheet). No changes needed.
- [x] **2.4 LazyColumn stable keys** — already present
    (`items(..., key = { it.packageName })` + `animateItem()`). No changes.
- [x] **2.5 Skeleton + empty states**
  - Loading now shows 6 shimmer rows mirroring `AppCard` (extracted `ShimmerBox`
    to `presentation/composable/` and shared it with the dialog skeleton).
  - Empty-after-filter gains a "Clear filters" button — clears BOTH search and
    filters (resetFilters alone leaves the query, which would no-op the common
    search-miss case).

**Acceptance:** can multi-select 10 apps and uninstall in one flow; sort by
last-used reflects real usage; no list scroll jank.

---

## Phase 3 — Dialog flow polish (~1 day)

The install dialog has had a lot of love already. Last gaps:

- [ ] **3.1 TabRow → PrimaryTabRow** — `DialogMenuContent.kt:264`
  - Migrate the deprecated `TabRow` + manual indicator to `PrimaryTabRow`. Drop
    the manual `tabIndicatorOffset` call (also deprecated).
- [ ] **3.2 Sticky button footer in menu tabs** — `DialogMenuContent.kt:299`
  - Wrap each tab's content in `Column` with `LazyColumn(Modifier.weight(1f))`
    + footer Row outside. Today the Install button can scroll off-screen with
    many splits.
- [ ] **3.3 Parse-state skeleton** — `InstallScreen.kt:526` / Dialog loading stage
  - Replace `CircularProgressIndicator` with stacked skeleton cards mirroring the
    Prepare layout: icon placeholder + 2 text lines + chip row.
- [ ] **3.4 Failed-stage actionable** — `DialogResultContent.kt`
  - Failed result currently shows Close only. Add Retry (re-fires `confirmInstall`)
    + Copy error (clipboard) for diagnostic reports.

**Acceptance:** no compiler deprecation warnings from these files; Failed stage
gives at least one recovery path.

---

## Phase 4 — Settings polish (~½ day)

- [x] **4.1 Collapse Installation section**
  - Added `collapsible` + `defaultExpanded` params to the shared `SettingsSection`
    composable. Shizuku-options / Root-options sections now start collapsed.
- [x] **4.2 Inline "Create profile" CTA** — `ApkInfoContent.kt`
  - Empty-state ProfilePickerCard now shows a TextButton that opens
    `ProfileEditActivity` directly (profileId omitted → new profile).
- [x] **4.3 Save state in ProfileEdit** — `ProfileEditScreen.kt`
  - `saveProfile` gained an `onSaved` callback; the screen shows a spinner and
    disables Save while the write is in flight, finishing only after it commits
    (fixes a latent write-cancellation on `finish()`).

---

## Phase 5 — Design system + perf (~1.5 days, foundation)

- [x] **5.1 Spacing tokens**
  - Added `object Spacing { XS/S/M/L/XL/XXL }` under `ui/theme/`. Adopted
    incrementally (LoadingContent first) — no big-bang migration; older files keep
    literals until next touched. Values off the 4dp scale (e.g. 20dp) stay literal.
- [x] **5.2 Async icon decode** — `ApkInfoContent.kt`
  - `apkInfo.icon?.toBitmap(128,128)` moved out of composition into
    `produceState(initial = null) { withContext(IO) { … } }`; fallback glyph shows
    until the decode lands.
- [x] **5.3 Dialog icon LRU cache**
  - Added `DialogIconCache` (object-scoped byte-capped `LruCache<String, Bitmap>`
    keyed on `iconPath`). `TargetIcon` now reads through it, so re-opening the
    dialog for the same APK (e.g. Retry) is instant.

---

## Phase 6 — Deprecation cleanup (~½ day, low risk)

Sweep of compile-time warnings. Pure refactor, no behavior change.

- [x] **6.1 LocalClipboardManager → LocalClipboard**
  - `AboutScreen.kt:367`, `DiagnosticsScreen.kt:150`. Suspend variant — propagate
    to `LaunchedEffect`.
- [x] **6.2 Icons.Rounded.List → AutoMirrored** — `ProfileEditScreen.kt:297`
- [x] **6.3 Remove !! on non-null** — `DialogInstallStages.kt:185`,
    `InstallScreen.kt:411`
- [x] **6.4 Elvis on non-null type** — `DialogPrepareContent.kt:59`
- [x] **6.5 Unchecked casts** — `SyncViewModel.kt:57-58`
- [x] **6.6 Follow-up sweep** — `BatchInstallSheet.kt` MergeType → AutoMirrored;
    `PermissionCenterSheet.kt` redundant `else` on exhaustive `when`;
    `ApkScanner.kt` redundant elvis on non-null. (Left: `unsafeCheckOpNoThrow`,
    `Thread.id` — need non-trivial API replacements, deferred.)

---

## Execution order

1. Phase 1 — quick wins. Visible result, sets the cadence.
2. Phase 5.1 (Spacing tokens only) — ground for later phases.
3. Phase 6 — cleanup, no risk.
4. Phase 3 — dialog polish.
5. Phase 4 — settings polish.
6. Phase 2 — manage overhaul (biggest, save for last with full context).
7. Phase 5.2–5.3 — perf, after the bigger refactors land.
