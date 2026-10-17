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
- Pure domain logic (sleep date attribution, daily aggregation, graph range bounds, nutrition math, calendar grid, per-day data marks) lives in plain Kotlin functions with no Android dependencies, so it can be unit tested on the JVM.
- `observeDataSpan` (in `data/DataSpan.kt`) is the single source of firstAny/lastAny, used by Graphs and History.

## Time handling

- Food and sleep times are stored as instants (epoch millis, `Long`).
- Weight is stored as a `LocalDate` (persisted as epoch day via a Room `TypeConverter`).
- Convert instants to local dates/times using the device's current time zone, only at the edges (display and aggregation).

## Data model

All nutrition values are **per serving**. Servings may be fractional (e.g. 1.5).

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

## Domain rules

**Sleep attribution:** assigned date = local date of (`start_at` − 12 hours).
- Bed 11PM 10/08, wake 7AM 10/09 → 10/08
- Bed 1AM 10/09 → 10/08
- Nap at 2PM 10/09 → 10/09 (accepted simplification)
- Starting exactly at noon counts toward that day; 11:59 AM counts toward the previous day. A sleep spanning noon counts by its start.
- The 12 hours come off the **local clock time** (not the instant), so the noon cutoff stays at exactly noon on daylight-saving days.
- Daily sleep = sum of durations of all entries assigned to that date.

**Daily aggregation:** calories and each macro are summed per local day; sleep is total hours per assigned day; weight is the single value for that day. **Days with no data are gaps, never zeros**, on every graph.

**Graph range** (shared by all four graphs). Ranges: last week, last month, last 3 months, all time.
- `firstAny` = earliest first measurement across weight, food, and sleep
- `lastAny` = latest last measurement across weight, food, and sleep
- Each measurement contributes its date: weight its `date`, food the local date of `eaten_at`, sleep its **assigned** date.
- Start = max(`firstAny`, today − range) (for "all time," Start = `firstAny`)
- End = min(today, `lastAny`)
- "today − range": last week = `today.minusDays(6)` (**7 calendar days counting today**); last month = `minusMonths(1)`; last 3 months = `minusMonths(3)`. If start > end (all data older than the range), there's nothing to plot.
- The selected range defaults to **Month** and is remembered between launches (`GraphPreferences`, on-device SharedPreferences).

**Charts** (all four share the window above, so a given day lines up across them)
- Plotted by date (x = epoch day, one step per day), so missing days show as horizontal space.
- Weight: line with dots, connected across gaps; y-axis padded around the data.
- Calories: bars per day, y from 0. A day with no food has no bar.
- Macros: four lines with dots (protein, carbs, fat, fiber) on one chart, y from 0, with a color legend.
- Sleep: bars of hours per assigned day, y from 0.
- Date labels: weekday names for a week, M/d for longer spans, M/d/yy beyond about a year; about five labels. Tapping shows a marker with the date and exact value(s).
- Each card shows an empty state when its own series has nothing in the window.

**Input validation**
- Weight: greater than 0 and at most 1000 lbs.
- Servings: greater than 0, fractions allowed. Nutrients: 0 or more, all five required. Decimal commas are accepted.
- Date pickers block future dates, except the sleep **Woke up** date.

## Screens

- **Home:** today's weight, calories, and macros; previous day's sleep (i.e. last night, per the attribution rule: total sleep assigned to yesterday, shown as "7 h 45 m" or "No sleep logged"). Buttons to every screen below.
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
  - Each day shows up to three dots: weight (primary), food (tertiary), sleep (secondary), matching the graph colors. Sleep marks its **assigned** date; food its local date. Today is outlined; future days are dimmed and not tappable. Any past day (even empty) opens its Day screen.
- **Day screen** (`day/{epochDay}`): full date; Weight, Food, and Sleep sections, each live-updating.
  - Weight: value with **Edit** (opens Add weight locked to that date) and **Delete** (confirmation); or **Add weight**.
  - Food: day totals, then each log (time, name, servings, kcal). Tapping a log opens **Edit food log**: servings and date/time only, with **Delete**. The nutrition snapshot never changes; totals recalculate from it.
  - Sleep: total, then each entry assigned to the day (start – end, with the date shown for a side that falls on another day). Tapping opens Add sleep in **edit mode** (pre-filled; overlap check ignores the entry itself), with **Delete**.
  - Each section has an **Add** shortcut preset to that day: Add weight is locked to the day; food defaults to that day at the current time of day; Add sleep's pickers suggest 11 PM that day (fields still start empty).
  - Editing a time can move an entry to another day; it then disappears from this Day screen.
  - Weight edits keep the date locked; to move a weigh-in, delete it and add it on the right day.
- **Navigation:** add flows take an optional `date` argument (epoch day, `-1` = none). When a flow finishes, it returns to whoever started it (Home or a Day screen).
- **Data** (button on Home): shows what's on the phone (counts and date span); **Export data** and **Import data** (see Data safety).

**Theme / dark mode**
- The window theme follows the system: `values/themes.xml` is light, `values-night/themes.xml` is dark (`android:Theme.Material.NoActionBar`).
- `MainActivity` wraps the nav host in a `Surface` painted with `colorScheme.background`, so screen transitions fade over the app background. Without both, a light frame flashes between screens in dark mode.

## Data safety

- Export/import a copy of the database via the Storage Access Framework (user picks the location, e.g. Downloads; no storage permissions). Import replaces current data after a confirmation dialog.
- **Export** takes its snapshot with `VACUUM INTO` (a complete, consistent copy that includes the WAL, so writes during export can't produce a torn file), then copies that snapshot to the user's chosen location. Suggested name: `health-tracker-YYYY-MM-DD.db`.
- **Import:**
  - Copies the picked file to app-private cache, then checks it before anything changes: the SQLite header, schema version between 1 and the current version (newer files are rejected), required tables for that version, and `PRAGMA integrity_check`.
  - The confirmation dialog compares the file's counts and date span with the phone's.
  - On Replace: a `VACUUM INTO` copy of the current data is saved to app-private files (`*.pre-import-backup`, overwritten by the next import, never exported); the database is closed, the file swapped in (stale `-wal`/`-shm` removed), and reopened, running migrations for older files.
  - If opening or migrating fails, the pre-import copy is restored automatically and nothing is lost.
  - After a successful import, `HealthTrackerApplication` builds a new `AppContainer` and the activity stack restarts (same process), so every screen reads the new database.
  - Code: `data/BackupRepository.kt` (works against any `DatabaseHolder` and database name, so tests use their own files); `ui/data/DataScreen.kt`.
- Schema version starts at 1. Every later schema change ships a Room `Migration`. **Never** use `fallbackToDestructiveMigration`. Export schemas (`room.schemaLocation`) and commit them.

## Testing

Unit tests are required for: sleep attribution (including crossing midnight and crossing noon), overlap detection, daily aggregation with missing days, graph range bounds, and snapshot math (editing an item doesn't change past totals). Add a Room DAO test for the weight upsert.

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
