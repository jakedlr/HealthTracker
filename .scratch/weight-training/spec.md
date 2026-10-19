# Spec: Weight training logging (Milestone 8)

Status: ready-for-agent

## Problem Statement

I track my body weight, food, and sleep in this app, but my strength training lives somewhere else (or nowhere). I want to see, per exercise, what I lifted last time and how it has changed, organised the way I actually train ("Leg day", "Back day"), without leaving the app or needing a network connection. Some of my exercises are one side at a time, and my left and right sides often lift different loads, so a single number per session isn't enough.

## Solution

A new **Weight training** area, reached from Home. It lists my **Exercise categories** in an order I choose; inside each category are its **Exercises**, also in an order I choose, each showing when I last logged it. An exercise's screen shows its 10 most recent **Exercise logs** and lets me add today's log (or edit it, if today is already logged). Each exercise log records the day's **Top set** as load × reps — once for a double-sided exercise, or separately for left and right on a single-sided one. Each exercise has a **Default unit** (lbs or kg) that pre-selects the unit for new logs; past logs keep whatever unit they were logged in. An **All logs** screen gives the full history for editing, deleting, or back-filling.

## User Stories

### Categories

1. As a lifter, I want a Weight training button on Home, so that I can reach my training logs in one tap.
2. As a lifter, I want the Weight training screen to list my exercise categories, so that I can pick today's session.
3. As a new user, I want an empty state ("No categories yet") on the Weight training screen, so that I know what to do first.
4. As a lifter, I want to create a category by entering a name, so that I can group exercises like "Leg day".
5. As a lifter, I want category names trimmed and checked for duplicates ignoring case, so that I don't end up with "Leg day" and "leg day ".
6. As a lifter, I want a clear error when a category name already exists, so that I understand why saving is blocked.
7. As a lifter, I want a new category to appear at the bottom of the list, so that my existing order isn't disturbed.
8. As a lifter, I want to long-press and drag categories to reorder them, so that they match my weekly routine.
9. As a lifter, I want my category order remembered between launches, so that I only arrange it once.
10. As a lifter, I want my category order included in exports and restored on import, so that a restore doesn't scramble my layout.
11. As a lifter, I want to rename a category, so that I can fix typos or change my split.
12. As a lifter, I want to delete an empty category (after confirming), so that I can clean up categories I no longer use.
13. As a lifter, I want deleting a category that still has exercises to be refused with an explanation, so that I never lose exercise history by accident.

### Exercises

14. As a lifter, I want a category's screen to list its exercises, so that I can see everything I do on that day.
15. As a lifter, I want an empty state on a category with no exercises, so that I know to add one.
16. As a lifter, I want each exercise row to show its last logged date (or "Never logged"), so that I can see what's due.
17. As a lifter, I want to create an exercise with a name, category, sidedness, and default unit, so that future logs are set up correctly.
18. As a lifter, I want the new-exercise category pre-set to the category I'm in, so that I don't have to pick it again.
19. As a lifter, I want to be required to choose Single or Double sidedness explicitly, so that I don't lock in a wrong guess.
20. As a lifter, I want the default unit to pre-select lbs, so that it matches the rest of the app.
21. As a lifter, I want exercise names unique across all categories (ignoring case, trimmed), so that each exercise has one history.
22. As a lifter, I want a new or moved exercise to appear at the bottom of its category, so that my existing order isn't disturbed.
23. As a lifter, I want to long-press and drag exercises within a category to reorder them, so that they follow the order I do them in.
24. As a lifter, I want my exercise order remembered between launches and kept through export/import, so that I only arrange it once.
25. As a lifter, I want to edit an exercise's name, category, and default unit at any time, so that I can reorganise as my training changes.
26. As a lifter, I want to change an exercise's sidedness only while it has no logs, so that existing history never becomes inconsistent.
27. As a lifter, I want the edit screen to explain why sidedness is locked, so that I know to create a new exercise instead.
28. As a lifter, I want changing an exercise's default unit to leave past logs untouched, so that history shows what I actually lifted.
29. As a lifter, I want to delete an exercise after a confirmation that states how many logs it has ("Delete Squat and its 42 logs?"), so that I understand what I'm losing.
30. As a lifter, I want an exercise's logs removed when the exercise is deleted, so that no orphaned history remains.

### Exercise screen

31. As a lifter, I want an exercise's screen to show its name, sidedness, and default unit, so that I know how it's tracked.
32. As a lifter, I want the 10 most recent exercise logs listed newest first, so that I can see my recent progress at a glance.
33. As a lifter, I want each log shown as date and load × reps with its unit (e.g. "135 lbs × 8"), so that I can read it quickly.
34. As a lifter, I want single-sided logs shown per side (e.g. "L 20 × 10 · R 22.5 × 8 lbs"), so that I can see side imbalances.
35. As a lifter, I want a skipped side shown as a dash (e.g. "L — · R 22.5 × 8"), so that I can tell it was skipped rather than lost.
36. As a lifter, I want an Add log button, so that I can record today's session.
37. As a lifter, I want that button to read Edit today's log when today is already logged, so that I can't create a duplicate.
38. As a lifter, I want to tap one of the recent logs to edit it, so that I can fix mistakes without opening the full history.
39. As a lifter, I want an All logs button, so that I can see and manage every past log.

### All logs

40. As a lifter, I want All logs to list every log for the exercise, newest first, so that I can review my full history.
41. As a lifter, I want to tap a log there to edit or delete it, so that I can correct old entries.
42. As a lifter, I want an Add log button on All logs, so that I can back-fill sessions I forgot to record.

### Adding and editing a log

43. As a lifter, I want a new log's date to default to today, so that logging a session is fast.
44. As a lifter, I want to change the date (but not to a future date), so that I can log a past session.
45. As a lifter, I want only one log per exercise per day, so that each day has a single clear record.
46. As a lifter, I want picking a date that already has a log to pre-fill the form and say saving will replace it, so that I edit rather than duplicate.
47. As a lifter, I want to enter load and reps for a double-sided exercise, so that I record my top set.
48. As a lifter, I want to enter load and reps separately for left and right on a single-sided exercise, so that I can record 20 lbs on the left and 22.5 lbs on the right.
49. As a lifter, I want to mark one side as skipped (e.g. when injured), so that I can still log the side I trained.
50. As a lifter, I want saving blocked if both sides are skipped, so that a log always records something.
51. As a lifter, I want the unit pre-selected from the exercise's default unit but switchable for this log, so that I can log a kg machine on a normally-lbs exercise.
52. As a lifter, I want both sides of a log to share one unit, so that a log is always internally consistent.
53. As a lifter, I want a load of 0 accepted (bodyweight) and decimals like 22.5 allowed, so that I can log pull-ups and fractional plates.
54. As a lifter, I want decimal commas accepted, so that the app works with my keyboard either way.
55. As a lifter, I want loads above 2000 or below 0 rejected with an inline error, so that typos are caught.
56. As a lifter, I want reps restricted to whole numbers from 1 to 999, so that typos are caught.
57. As a lifter, I want Delete on the edit screen (with confirmation), so that I can remove a log I made by mistake.
58. As a lifter, I want to return to the screen I came from after saving or deleting, so that my place in the app is kept.

### Data and safety

59. As a user, I want the Data screen to include an exercise logs count, so that I can see what an export will contain.
60. As a user, I want older exports (schema v1–v3) to still import, with training data simply empty, so that old backups keep working.
61. As a user, I want everything to stay offline and on-device, so that my training data is as private as the rest of my data.

## Implementation Decisions

- **Milestone 8, schema v4.** Three new tables plus a `Migration` from v3 to v4 (no destructive fallback), with the exported schema committed. The v3 tables are unchanged.
- **exercise_category**: id; name (unique, case-insensitive, trimmed on save); position (integer sort key).
- **exercise**: id; category id (foreign key, delete restricted, so a non-empty category can't be deleted at the database level either); name (unique across all exercises, case-insensitive, trimmed); sidedness (Single or Double); default unit (lbs or kg); position (integer sort key, scoped to its category).
- **exercise_log**: id; exercise id (foreign key, delete cascade); date (a local date, stored the same way as weigh-in dates); unit (lbs or kg); and the top set as nullable load/reps columns: one pair for a double-sided exercise, or a left pair and a right pair for a single-sided one, where a skipped side is null. Unique on (exercise id, date); saving for an existing date replaces that log (upsert), matching weigh-ins.
- Dates are plain local dates (no instants), so ADR-0001's time-zone caveat does not apply to exercise logs.
- **Units are never converted.** Each log keeps the unit it was saved in; the exercise's default unit only pre-selects the unit for new logs.
- **Sidedness lock**: the repository refuses a sidedness change when the exercise has any logs; the edit screen shows the control disabled with an explanation.
- **Ordering**: new categories and exercises get position = (current max in their list) + 1; moving an exercise to another category puts it at the bottom of that category; deleting leaves gaps (order is by position, so gaps are harmless). A drag reorder ends with one repository call that rewrites positions for the affected list in a single transaction. The arithmetic for "move item from index i to index j" is a pure function.
- **Drag-to-reorder** is hand-written on Compose gesture APIs (long-press on a row, drag, drop), used for both the category list and a category's exercise list. No new dependency.
- **Repository**: a new weight-training repository is the only data access path for the new screens (screens → ViewModels → repository → DAOs), wired by hand in `AppContainer`. Its interface covers: observe categories in order; create/rename/delete category (delete fails if not empty); reorder categories; observe a category's exercises in order with each one's last logged date; create/edit/move/delete exercise (sidedness change rejected when logs exist); reorder exercises within a category; observe an exercise's recent logs (limit 10) and all logs, newest first; get the log for an exercise and date; save (upsert) and delete a log; count an exercise's logs (for the delete confirmation).
- **Validation** lives in pure Kotlin domain functions with no Android dependencies: load ≥ 0 and ≤ 2000, decimal comma accepted; reps an integer 1–999; for single-sided, at most one side skipped; date not in the future. Name trimming and blank checks follow the food item name rules.
- **Navigation** uses string routes like the rest of the app: Weight training → Category → Exercise → All logs, and Add/Edit log reachable from Exercise and All logs. New category is a name dialog on the Weight training screen; rename is the same dialog. New/Edit exercise is its own screen. Add/Edit log returns to whichever screen opened it.
- **Exercise screen button**: shows Add log when today has no log for this exercise, otherwise Edit today's log.
- **Add/Edit log date picker** blocks future dates (reusing the existing past-date picker). Picking a date that already has a log loads it and shows "Saving will replace the log for this date", as Add weight does.
- **Display format**: "135 lbs × 8" for double-sided; "L 20 × 10 · R 22.5 × 8 lbs" for single-sided; a skipped side shows "—". Units are written "lbs" and "kg".
- **Backup/import**: export needs no change (it copies the whole database). Import's required-tables check gains the three new tables for version 4; v1–v3 files still import and are migrated. The Data screen's counts (for both the phone and the import file) add exercise logs. The Data screen's date span is unchanged.
- **Unaffected**: Home's today summary, History calendar and Day screen, Graphs, and `observeDataSpan` do not include training data.
- **Docs**: CLAUDE.md gets the new tables, rules, screens, and Milestone 8; README gets the new feature at a high level; GLOSSARY.md already has the training vocabulary (Exercise category, Exercise, Sidedness, Exercise log, Top set, Load, Default unit).

## Testing Decisions

- Good tests exercise external behaviour through a module's public interface (what you can observe after calling it), not its internals: no asserting on DAO calls or SQL, no testing private helpers through reflection.
- **Weight-training repository** (instrumented, in-memory Room, on the Pixel 8a), the primary seam: one log per exercise per date with upsert-on-same-date; case-insensitive, trimmed name uniqueness for categories and for exercises across categories; new items at the bottom; moving an exercise to another category puts it at the bottom there; a reorder persists and is reflected in the observed order; deleting an item keeps the rest in order; deleting an exercise removes its logs; deleting a non-empty category is refused; sidedness change refused once logs exist and allowed before; recent logs limited to 10, newest first; last logged date per exercise. Prior art: `FoodDaoTest`, `SleepDaoTest`, `WeightDaoTest` (in-memory database, `runTest`).
- **Domain functions** (JVM unit tests): load and reps validation including boundaries (0, 2000, 2000.01, 1, 999, 1000, non-integers), decimal commas, single-sided skip rules (one side skipped OK, both skipped rejected); the move-from-i-to-j reorder arithmetic. Prior art: `WeightValidationTest`, `FoodValidationTest`, `SleepTest`.
- **Migration**: a v3 → v4 case in `MigrationTest` (`MigrationTestHelper`), checking existing v3 data survives and the new tables exist and accept rows. Prior art: the existing v1→v2 and v2→v3 cases.
- **Backup**: extend the existing import validation test so a v4 file with the new tables passes and a v4 file missing one fails. Prior art: `BackupRepositoryTest`.
- **Manual on the phone** before the milestone commit: drag-to-reorder for categories and exercises (including order surviving an app restart and an export/import round trip), and the full add/edit/delete flow for single- and double-sided exercises.

## Out of Scope

- Multiple sets per log, or anything beyond a top set (RPE, rest times, notes, tempo).
- More than one log per exercise per day.
- Graphs of training progress, personal-record detection, or training dots on the History calendar / Day screen.
- Training data on Home or in the data span used by Graphs and History.
- Converting loads between lbs and kg, or showing totals across units.
- An exercise belonging to more than one category, or sharing an exercise's history across categories.
- Predefined exercise libraries, templates, or routines.
- Changing the sidedness of an exercise that already has logs.

## Further Notes

- Vocabulary follows GLOSSARY.md: "load" (not "weight") for what's lifted, because **weight** means body weight in a **Weigh-in**. UI labels may still say "Weight" on training screens where the meaning is clear.
- "Weight training" is the area's name in the UI; it is not a glossary term.
- Working agreement: an implementation plan for Milestone 8 goes to the user for approval before code is written.
