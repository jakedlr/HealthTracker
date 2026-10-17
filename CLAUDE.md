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
- Pure domain logic (sleep date attribution, daily aggregation, graph range bounds, nutrition math) lives in plain Kotlin functions with no Android dependencies, so it can be unit tested on the JVM.

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
- `start_at`, `end_at` (epoch millis), entered together in one form; never default to "now"
- Validation: `end_at > start_at`. Reject entries that overlap an existing sleep entry.
- The day a sleep counts toward is **derived, never stored** (see below).

## Domain rules

**Sleep attribution:** assigned date = local date of (`start_at` − 12 hours).
- Bed 11PM 10/08, wake 7AM 10/09 → 10/08
- Bed 1AM 10/09 → 10/08
- Nap at 2PM 10/09 → 10/09 (accepted simplification)
- Daily sleep = sum of durations of all entries assigned to that date.

**Daily aggregation:** calories and each macro are summed per local day; sleep is total hours per assigned day; weight is the single value for that day. **Days with no data are gaps, never zeros**, on every graph.

**Graph range** (shared by all four graphs). Ranges: last week, last month, last 3 months, all time.
- `firstAny` = earliest first measurement across weight, food, and sleep
- `lastAny` = latest last measurement across weight, food, and sleep
- Start = max(`firstAny`, today − range) (for "all time," Start = `firstAny`)
- End = min(today, `lastAny`)
- "today − range" is taken literally: `minusWeeks(1)`, `minusMonths(1)`, `minusMonths(3)`, so "last week" spans 8 calendar days counting today. If start > end (all data older than the range), there's nothing to plot.
- Charts plot by date (x = epoch day, one step per day), so missing days show as horizontal space. The weight line connects across gaps, with a dot on each data point; the y-axis is padded around the data instead of starting at 0.

**Input validation**
- Weight: greater than 0 and at most 1000 lbs.
- Servings: greater than 0, fractions allowed. Nutrients: 0 or more, all five required. Decimal commas are accepted.
- Date pickers block future dates.

## Screens

- **Home:** today's weight, calories, and macros; previous day's sleep (i.e. last night, per the attribution rule). Buttons to every screen below.
- **Add food:** scan barcode, search library, or enter manually. Choose servings; date/time defaults to now but is editable.
  - Search results: most recently logged first, then never-logged items alphabetically.
  - Known barcode → Log food for that item. Unknown barcode → New food with the barcode attached, saved to the library with the barcode.
  - Manual entry always saves a new library item. If the name already exists (any case), saving is blocked and **Use existing** goes to Log food for that item. From a scan, if the existing item has no barcode, the button reads **Use existing and link barcode** and attaches the scanned barcode to it.
  - Scanning handles only EAN-13, EAN-8, UPC-A, and UPC-E; the first read wins. If camera permission is denied, the screen explains why it's needed and offers Allow camera (or Open settings) and Enter manually.
- **Food library:** search by name; edit an item's name and nutrition (future logs only); delete an item (with confirmation; past logs keep their values). Shows the barcode if the item has one.
- **Add weight:** date defaults to today, editable (no future dates). If the date already has a weight, the field is pre-filled and the screen says saving will replace it.
- **Add sleep:** start and end date/time pickers.
- **Graphs:** weight, calories, macros (protein/carbs/fat/fiber on one chart), sleep; range selector.
- **History:** month calendar, swipe between months; tap a day to view and edit/delete that day's weight, food logs, and sleep.
- **Settings/Data:** export and import (see below).

## Data safety

- Export/import a copy of the database via the Storage Access Framework (user picks the location, e.g. Downloads; no storage permissions). Checkpoint the WAL before copying. Import replaces current data after a confirmation dialog.
- Schema version starts at 1. Every later schema change ships a Room `Migration`. **Never** use `fallbackToDestructiveMigration`. Export schemas (`room.schemaLocation`) and commit them.

## Testing

Unit tests are required for: sleep attribution (including crossing midnight and crossing noon), overlap detection, daily aggregation with missing days, graph range bounds, and snapshot math (editing an item doesn't change past totals). Add a Room DAO test for the weight upsert.

Also:
- Every schema migration gets a `MigrationTestHelper` test (in `androidTest/.../MigrationTest.kt`).
- Instrumented tests (DAO, repository, migration, real ML Kit scan) run on the Pixel 8a via `./gradlew connectedDebugAndroidTest`.
- Features that need a camera or a real product are checked by hand on the phone before the milestone is committed.

## Working agreement

- Work one milestone at a time. Propose a plan and wait for approval before writing code.
- Keep the existing project structure and naming; don't reorganize files unless asked.
- Run the tests and a build before calling a milestone done.
- **Keep this file current.** Whenever an approved plan, a user decision, or a fix during implementation changes or adds to what's written here, update CLAUDE.md in the same milestone.
- Commit each milestone as its own commit once its tests pass (and any manual checks are confirmed).
- Edit files with UTF-8 tooling. On Windows, Python's `open()` defaults to cp1252; always pass `encoding="utf-8"`.

## Milestones

1. ✅ Project setup, Room database, weight logging, Home screen showing today's weight, weight graph
2. ✅ Food library + manual food logging + library search/edit/delete (schema v2)
3. ✅ Barcode scanning
4. Sleep logging with attribution and validation
5. Remaining graphs + range selector
6. History calendar with view/edit/delete
7. Export/import
