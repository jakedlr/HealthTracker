# Health Tracker — Project Guide

Personal, single-user Android app for logging weight, food, and sleep, with graphs and a history view. It runs **completely offline**; all data lives in an on-device database.

## Hard constraints

- **No network access, ever.** Do not declare the `INTERNET` permission. If a dependency merges it into the manifest, remove it with `tools:node="remove"`. Do not add analytics, crash reporting, or any cloud SDK.
  - The manifest also strips `ACCESS_NETWORK_STATE` and ML Kit's `datatransport` telemetry components (`TransportBackendDiscovery`, `JobInfoSchedulerService`, `AlarmManagerSchedulerBroadcastReceiver`). ML Kit then logs "Transport backend 'cct' is not registered" and drops its telemetry; that warning is expected.
  - After adding a dependency, check the merged manifest (`app/build/intermediates/merged_manifest/debug/...`) for new permissions or components.
- **No cloud backup.** `allowBackup="false"`, and both backup rule files exclude everything (cloud and device transfer). Export/import is the only backup path.
- No authentication, accounts, or remote database.
- Single user, single device: Google Pixel 8a (Android 14+). `minSdk = 34` is fine.
- Ask before adding any dependency not listed under Tech stack.

## Tech stack

- Kotlin, Jetpack Compose (Material 3)
  - Kotlin **2.4.x** (2.4.21): Vico 3 is compiled with Kotlin 2.4 and won't load on an older compiler.
- Room (SQLite) for persistence, with KSP and the Room Gradle plugin (schemas in `app/schemas/`)
- Vico 3 for charts (`compose-m3`; all APIs live under `com.patrykandpatrick.vico.compose`)
- CameraX (`camera-camera2`, `camera-view` with `LifecycleCameraController`) + ML Kit barcode scanning, **bundled model only** (`com.google.mlkit:barcode-scanning`, not the Play Services variant, which downloads its model over the network)
- Kotlin coroutines + Flow; Jetpack Navigation for Compose (string routes)
- Lifecycle: `lifecycle-viewmodel-compose`, `lifecycle-runtime-compose`
- Test-only: `kotlinx-coroutines-test`, `room-testing`
- Version constraint: `kotlinx-serialization-core` 1.8.1 (Navigation brings 1.7.3, but room-testing's `MigrationTestHelper` needs 1.8.1+, and test classpaths are pinned to the app's versions). This is a version bump, not a new library.
- No icon library: icons are vector drawables in `res/drawable` (e.g. `ic_arrow_back`).
- No DI framework: dependencies are wired by hand in `AppContainer`, held by `HealthTrackerApplication`; ViewModel factories use `appContainer()`.

## Architecture

`Compose screens → ViewModels → Repository → Room DAOs`

- Screens never touch DAOs or the database directly. All data access goes through the repository layer.
- ViewModels expose UI state as `StateFlow`.
- Pure domain logic (sleep date attribution, daily aggregation, graph window bounds, nutrition math, calendar grid, per-day data marks, training validation and reorder) lives in plain Kotlin functions with no Android dependencies, so it can be unit tested on the JVM.
- `observeDataSpan` (in `data/DataSpan.kt`) is the single source of firstAny/lastAny, used by Graphs and History.

## Time handling

- Food and sleep times are stored as instants (epoch millis, `Long`).
- Weight is stored as a `LocalDate` (persisted as epoch day via a Room `TypeConverter`).
- Changing the phone's time zone can move past food logs and sleeps to another day; accepted (see `docs/adr/0001-local-dates-use-current-time-zone.md`).
- Convert instants to local dates/times using the device's current time zone, only at the edges (display and aggregation).

## Data model

All nutrition values are **per serving**. Servings may be fractional (e.g. 1.5). A food item has no serving-size field; the size goes in its name when it matters. Carbs are total carbohydrate (fiber included), never net. Vocabulary is defined in `GLOSSARY.md`.

**weight_entry**
- `date` (LocalDate, primary key): one weigh-in per day; saving again for the same date overwrites it (upsert)
- `weight_lbs` (Double)

**food_item** (the reusable nutrition library)
- `id` (Long, primary key)
- `name` (String, unique, case-insensitive via `COLLATE NOCASE`; trimmed on save)
- `barcode` (String?, unique when not null), stored **normalized**: a 12-digit UPC-A gets a leading `0` (stored as EAN-13); EAN-13, EAN-8, and UPC-E are stored as read
- `calories_kcal`, `protein_g`, `carbs_g`, `fiber_g`, `fat_g` (Double), per serving

Library items can be **deleted** (after a confirmation). Past food_log rows keep their snapshot; only `food_item_id` becomes null.

**food_log** (one row per thing eaten)
- `id` (Long, primary key)
- `food_item_id` (Long?, FK to food_item, `ON DELETE SET NULL`)
- `eaten_at` (epoch millis)
- `servings` (Double)
- **Snapshot** of the item at log time: `name`, `calories_kcal`, `protein_g`, `carbs_g`, `fiber_g`, `fat_g` (per serving)

Editing a food_item must **never** change existing food_log rows; only future logs use the new values. Totals for a log row = snapshot per-serving values × `servings`. Editing a past log's servings recalculates from its own snapshot.

**sleep_entry**
- `id` (Long, primary key)
- `start_at`, `end_at` (epoch millis, `start_at` indexed), entered together in one form; never default to "now"
- Validation: `end_at > start_at`, and duration at most 24 hours (exactly 24 h is allowed). Reject entries that overlap an existing sleep entry. Entries that only touch (one ends exactly when the next starts) do **not** overlap and are allowed. Wake times in the future are allowed.
- The day a sleep counts toward is **derived, never stored** (see below).

**exercise_category**
- `id` (Long, primary key); `name` (unique, case-insensitive via `COLLATE NOCASE`; trimmed on save); `position` (Int, the user's order)

**exercise**
- `id` (Long, primary key); `category_id` (FK to exercise_category, `ON DELETE RESTRICT`: a category with exercises can't be deleted)
- `name` (unique across **all** exercises, case-insensitive, trimmed); `sidedness` (`SINGLE`/`DOUBLE`); `default_unit` (`LBS`/`KG`); `position` (order within its category)
- Sidedness can change only while the exercise has no logs. Name, category, and default unit can always change; moving to another category puts the exercise at the bottom there.

**exercise_log**
- `id` (Long, primary key); `exercise_id` (FK to exercise, `ON DELETE CASCADE`); `date` (LocalDate, epoch day); `unit` (`LBS`/`KG`, the log's own, never converted)
- The **top set** (one load × reps): double-sided fills `load`/`reps`; single-sided fills `left_load`/`left_reps` and `right_load`/`right_reps`, with a skipped side null (never both).
- Unique on (`exercise_id`, `date`): one log per exercise per day; saving for a date that has a log replaces it (upsert).
- Training dates are plain local dates, so ADR-0001's time-zone caveat doesn't apply. Training data is **not** part of Home's summary, Graphs, History, or `observeDataSpan`.

## Domain rules

**Sleep attribution:** sleep date = local date of (`start_at` − 12 hours).
- Bed 11PM 10/08, wake 7AM 10/09 → 10/08
- Bed 1AM 10/09 → 10/08
- Nap at 2PM 10/09 → 10/09 (accepted simplification)
- Starting exactly at noon counts toward that day; 11:59 AM counts toward the previous day. A sleep spanning noon counts by its start.
- The 12 hours come off the **local clock time** (not the instant), so the noon cutoff stays at exactly noon on daylight-saving days.
- Daily sleep = sum of durations of all entries with that sleep date.

**Daily aggregation:** calories and each macro are summed per local day; sleep is total hours per sleep date; weight is the single value for that day. **Days with no data are gaps, never zeros**, on every graph.

**Graph window** (shared by all four graphs), set by the selected **range**: last week, last month, last 3 months, all time.
- `firstAny` = earliest first measurement across weight, food, and sleep
- `lastAny` = latest last measurement across weight, food, and sleep
- Each measurement contributes its date: weight its `date`, food the local date of `eaten_at`, sleep its **sleep date**.
- Start = max(`firstAny`, today − range) (for "all time," Start = `firstAny`)
- End = min(today, `lastAny`)
- "today − range": last week = `today.minusDays(6)` (**7 calendar days counting today**); last month = `minusMonths(1)`; last 3 months = `minusMonths(3)`. If start > end (all data older than the range), there's nothing to plot.
- The selected range defaults to **Month** and is remembered between launches (`GraphPreferences`, on-device SharedPreferences).

**Charts** (all four share the window above, so a given day lines up across them)
- Plotted by date (x = epoch day, one step per day), so missing days show as horizontal space.
- Weight: line with dots, connected across gaps; y-axis padded around the data.
- Calories: bars per day, y from 0. A day with no food has no bar.
- Macros: four lines with dots (protein, carbs, fat, fiber) on one chart, y from 0, with a color legend.
- Sleep: bars of hours per sleep date, y from 0.
- Date labels: weekday names for a week, M/d for longer spans, M/d/yy beyond about a year; about five labels. Tapping shows a marker with the date and exact value(s).
- Each card shows an empty state when its own series has nothing in the window.

**Input validation**
- Weight: greater than 0 and at most 1000 lbs.
- Servings: greater than 0, fractions allowed. Nutrition values: 0 or more, all five required. Decimal commas are accepted.
- Date pickers block future dates, except the sleep **Woke up** date.
- Exercise logs: load 0 (bodyweight) to 2000, decimals and decimal commas accepted; reps a whole number 1–999. Category and exercise names must not be blank.

## Screens

- **Home:** today's weight, calories, and macros; previous day's sleep (i.e. last night, per the attribution rule: total sleep whose sleep date is yesterday, naps included, shown as "7 h 45 m" or "No sleep logged"). Buttons to every screen below.
- **Add food:** scan barcode, search library, or enter manually. Choose servings; date/time defaults to now but is editable.
  - Search results: most recently logged first, then never-logged items alphabetically.
  - Known barcode → Log food for that item. Unknown barcode → New food with the barcode attached, saved to the library with the barcode.
  - Manual entry always saves a new library item. If the name already exists (any case), saving is blocked and **Use existing** goes to Log food for that item. From a scan, if the existing item has no barcode, the button reads **Use existing and link barcode** and attaches the scanned barcode to it.
  - Scanning handles only EAN-13, EAN-8, UPC-A, and UPC-E; the first read wins. If camera permission is denied, the screen explains why it's needed and offers Allow camera (or Open settings) and Enter manually.
- **Food library:** search by name; edit an item's name and nutrition (future logs only); delete an item (with confirmation; past logs keep their values). Shows the barcode if the item has one.
- **Add weight:** date defaults to today, editable (no future dates). If the date already has a weight, the field is pre-filled and the screen says saving will replace it.
- **Add sleep:** **Fell asleep** and **Woke up** rows, each with date and time buttons.
  - All four start empty ("Select date"/"Select time"); saving is blocked until all are chosen. Pickers only *open* at a suggestion (Fell asleep: yesterday 11 PM; Woke up: chosen sleep time + 8 h).
  - Once both are set, a preview shows the duration and the date it counts toward.
  - Errors show inline: wake not after sleep, longer than 24 h, or an overlap naming the conflicting entry's times.
- **Graphs:** weight, calories, macros (protein/carbs/fat/fiber on one chart), sleep; range selector.
- **History:** month calendar, swipe between months; tap a day to view and edit/delete that day's weight, food logs, and sleep.
  - Months: from the month of the earliest data (`observeDataSpan`) to the current month; opens on the current month; ‹ › buttons as well as swiping. Week start follows the phone's locale.
  - Each day shows up to three dots: weight (primary), food (tertiary), sleep (secondary), matching the graph colors. Sleep marks its **sleep date**; food its local date. Today is outlined; future days are dimmed and not tappable. Any past day (even empty) opens its Day screen.
- **Day screen** (`day/{epochDay}`): full date; Weight, Food, and Sleep sections, each live-updating.
  - Weight: value with **Edit** (opens Add weight locked to that date) and **Delete** (confirmation); or **Add weight**.
  - Food: day totals, then each log (time, name, servings, kcal). Tapping a log opens **Edit food log**: servings and date/time only, with **Delete**. The nutrition snapshot never changes; totals recalculate from it.
  - Sleep: total, then each entry whose sleep date is that day (start – end, with the date shown for a side that falls on another day). Tapping opens Add sleep in **edit mode** (pre-filled; overlap check ignores the entry itself), with **Delete**.
  - Each section has an **Add** shortcut preset to that day: Add weight is locked to the day; food defaults to that day at the current time of day; Add sleep's pickers suggest 11 PM that day (fields still start empty).
  - Editing a time can move an entry to another day; it then disappears from this Day screen.
  - Weight edits keep the date locked; to move a weigh-in, delete it and add it on the right day.
- **Navigation:** add flows take an optional `date` argument (epoch day, `-1` = none). When a flow finishes, it returns to whoever started it (Home or a Day screen).
- **Weight training** (button on Home):
  - **Weight training** screen: categories in the user's order (long-press and drag to reorder), **New category** (name dialog), empty state.
  - **Category**: its exercises in order (drag to reorder), each with its last logged date; **New exercise**; **Rename**; **Delete** (only when empty, otherwise it explains why).
  - **New/Edit exercise**: name, category (pre-set to the current one), sides (Double/Single, no default for a new exercise; locked once it has logs), default unit (pre-selects lbs).
  - **Exercise**: sidedness and default unit; **Add log**, or **Edit today's log** when today is logged; the 10 most recent logs, newest first (tap to edit); **All logs**; **Edit**; **Delete** (confirmation states the log count; deletes the logs).
  - **All logs**: every log, newest first; tap to edit; **Add log** to back-fill.
  - **Add/Edit log**: date (defaults to today, no future dates; a date with a log loads it and says saving replaces it), unit (pre-selected from the exercise), load × reps (per side with **Skip this side** for single-sided), **Delete** (when editing, always the log that was opened; when adding, the log already on the date). While editing, an empty date keeps the opened log's values so it can be moved there.
  - Logs display as "135 lbs × 8" or "L 20 × 10 · R 22.5 × 8 lbs" ("—" for a skipped side).
  - Reordering is hand-written (`ui/training/ReorderableColumn.kt`, no library); the order is stored in the database, so it survives export/import. New items go to the bottom.
- **Data** (button on Home): shows what's on the phone (counts, including exercise logs, and date span); **Export data** and **Import data** (see Data safety).

**Theme / dark mode**
- The window theme follows the system: `values/themes.xml` is light, `values-night/themes.xml` is dark (`android:Theme.Material.NoActionBar`).
- `MainActivity` wraps the nav host in a `Surface` painted with `colorScheme.background`, so screen transitions fade over the app background. Without both, a light frame flashes between screens in dark mode.

## Data safety

- Export/import a copy of the database via the Storage Access Framework (user picks the location, e.g. Downloads; no storage permissions). Import replaces current data after a confirmation dialog.
- **Export** takes a consistent copy with `VACUUM INTO` (complete, and includes the WAL, so writes during export can't produce a torn file), then copies it to the user's chosen location. Suggested name: `health-tracker-YYYY-MM-DD.db`.
- **Import:**
  - Copies the picked file to app-private cache, then checks it before anything changes: the SQLite header, schema version between 1 and the current version (newer files are rejected), required tables for that version, and `PRAGMA integrity_check`.
  - The confirmation dialog compares the file's counts and date span with the phone's.
  - On Replace: a `VACUUM INTO` copy of the current data is saved to app-private files (`*.pre-import-backup`, overwritten by the next import, never exported); the database is closed, the file swapped in (stale `-wal`/`-shm` removed), and reopened, running migrations for older files.
  - If opening or migrating fails, the pre-import copy is restored automatically and nothing is lost.
  - After a successful import, `HealthTrackerApplication` builds a new `AppContainer` and the activity stack restarts (same process), so every screen reads the new database.
  - Code: `data/BackupRepository.kt` (works against any `DatabaseHolder` and database name, so tests use their own files); `ui/data/DataScreen.kt`.
- Schema version starts at 1. Every later schema change ships a Room `Migration`. **Never** use `fallbackToDestructiveMigration`. Export schemas (`room.schemaLocation`) and commit them.

## Testing

Unit tests are required for: training validation and reorder arithmetic (`TrainingTest`), sleep attribution (including crossing midnight and crossing noon), overlap detection, daily aggregation with missing days, graph window bounds, and snapshot math (editing an item doesn't change past totals). Add a Room DAO test for the weight upsert. `TrainingRepositoryTest` (instrumented) covers the one-log-per-day upsert, name uniqueness, ordering, delete rules, and the sidedness lock.

Also:
- Every schema migration gets a `MigrationTestHelper` test (in `androidTest/.../MigrationTest.kt`). The driver-based helper doesn't clean up between tests, so `MigrationTest` deletes its database file in `@Before`.
- Instrumented tests (DAO, repository, migration, real ML Kit scan) run on the Pixel 8a via `./gradlew connectedDebugAndroidTest`.
- Features that need a camera or a real product are checked by hand on the phone before the milestone is committed.

## Working agreement

- Work one milestone at a time. Propose a plan and wait for approval before writing code.
- Keep the existing project structure and naming; don't reorganize files unless asked.
- Run the tests and a build before calling a milestone done.
- **Keep this file current.** Whenever an approved plan, a user decision, or a fix during implementation changes or adds to what's written here, update CLAUDE.md in the same milestone.
- **Keep README.md current at a high level.** Update it when a user-visible feature or the tech stack changes (e.g. a new screen, supporting kg as well as lbs, a new library). Don't add bug fixes, refactors, or internal details. Keep the Claude Code disclaimer.
- Commit or push only when the user asks; if they want to run git themselves, give them the commands instead.
- Commit each milestone as its own commit once its tests pass (and any manual checks are confirmed).
- Edit files with UTF-8 tooling. On Windows, Python's `open()` defaults to cp1252; always pass `encoding="utf-8"`.

## Milestones

1. ✅ Project setup, Room database, weight logging, Home screen showing today's weight, weight graph
2. ✅ Food library + manual food logging + library search/edit/delete (schema v2)
3. ✅ Barcode scanning
4. ✅ Sleep logging with attribution and validation (schema v3)
5. ✅ Remaining graphs + range selector
6. ✅ History calendar with view/edit/delete
7. ✅ Export/import (plus dark-mode transition flash fix)
8. ✅ Weight training logging (schema v4; spec in `.scratch/weight-training/spec.md`)

## Agent skills

### Issue tracker

Issues and specs live as local markdown files under `.scratch/<feature>/`. See `docs/agents/issue-tracker.md`.

### Triage labels

Default vocabulary: `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: `GLOSSARY.md` and `docs/adr/` at the repo root. See `docs/agents/domain.md`.
