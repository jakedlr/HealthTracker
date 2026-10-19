# Health Tracker

A personal log of one person's weight, food, and sleep, kept entirely on their phone and viewed as daily graphs and a calendar history.

## Days

**Day**:
A local calendar date. Food logs belong to the day they were eaten, weigh-ins to their date, and sleep entries to their sleep date, so a day's sleep can run into the next morning.

**Weigh-in**:
One weight reading for a date; a date has at most one.
_Avoid_: Weight entry, measurement

## Food

**Food item**:
A named, reusable set of nutrition for one serving, with at most one barcode.
_Avoid_: Library item, product, food

**Food library**:
The collection of all food items.
_Avoid_: Nutrition library, catalog

**Food log**:
One record of eating some number of servings of something at a particular time.
_Avoid_: Entry, meal, log row

**Serving**:
Whatever amount a food item's nutrition describes; when the size matters, it goes in the food item's name (e.g. "Fage 0% (170 g)").

**Nutrition**:
Calories plus the four macros: the five numbers a food item stores per serving.
_Avoid_: Nutrients

**Macros**:
Protein, carbs, fat, and fiber (fiber included on purpose).

**Carbs**:
Total carbohydrate as printed on the label, fiber included; never net carbs.
_Avoid_: Net carbs

**Snapshot**:
The name and per-serving nutrition a food log copies from its food item at the moment it is logged; later edits to the food item never change it.

## Sleep

**Sleep entry**:
One stretch of sleep with a start and an end. Sleep entries may touch but never overlap.
_Avoid_: Sleep period, session, "a sleep"

**Sleep date**:
The date a sleep entry counts toward: the local date of its start time minus 12 hours, so it counts toward the night it began.
_Avoid_: Assigned date, attributed day

**Sleep attribution**:
The rule that gives each sleep entry its sleep date.

**Last night**:
The total sleep whose sleep date is yesterday, naps included.

## Weight training

**Exercise category**:
A named group of exercises the user creates, such as "Leg day" or "Back day".
_Avoid_: Workout, split, routine

**Exercise**:
One movement the user tracks, such as "Bulgarian split squat", belonging to exactly one exercise category.
_Avoid_: Workout, lift, movement

**Sidedness**:
Whether an exercise is done with both sides together (double) or one side at a time (single), in which case each side is logged separately.

**Exercise log**:
One exercise's record for one day: its top set, per side for a single-sided exercise (a side may be skipped).
_Avoid_: Instance, entry, session

**Top set**:
The one load × reps an exercise log records for a side: the main or best set of that day.
_Avoid_: Set (unqualified)

**Load**:
The weight lifted in an exercise log, in that log's unit.
_Avoid_: Weight (reserved for weigh-ins)

**Default unit**:
The unit (kg or lb) an exercise pre-selects for new exercise logs; changing it never alters past logs.

## Graphs

**Range**:
The period chosen for the graphs: Week, Month, 3 months, or All time.

**Graph window**:
The first-to-last day shown on every graph: the range, trimmed to the data span and never past today.
_Avoid_: Graph range

**Data span**:
The earliest to the latest day with any weigh-in, food log, or sleep entry.

## Backups

**Export**:
A copy of all data saved to a file the user picks.
_Avoid_: Backup, snapshot

**Import**:
Replacing all data on the phone with the contents of an export.
_Avoid_: Restore, merge

**Pre-import backup**:
The copy of the previous data kept on the phone during an import, used to undo a failed one.
