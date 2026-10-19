# Health Tracker

A personal Android app for keeping track of weight, food, and sleep, with graphs and a calendar history. It's built for one person on one phone and runs **completely offline**: there are no accounts, no cloud sync, and no internet access. All data stays on the device.

## What it does

**Log your day**
- **Weight:** one weigh-in per day.
- **Food:** scan a product's barcode, pick something from your personal food library, or enter it by hand. Choose how many servings you had, and the app works out calories, protein, carbs, fat, and fiber.
- **Sleep:** enter when you fell asleep and woke up. Sleep counts toward the night it started, so going to bed at 1 AM still counts as the previous night.
- **Weight training:** group exercises into your own categories (e.g. "Leg day"), in whatever order you like. Log each exercise's top set for the day as load × reps, in lbs or kg, with separate left and right sides for single-arm or single-leg exercises. Each exercise shows its last 10 logs and its full history.

**See how you're doing**
- **Home:** today's weight, calories, and macros, plus how long you slept last night.
- **Graphs:** weight, calories, macros, and sleep over the last week, month, three months, or all time. Days you didn't log show as gaps rather than zeros.
- **History:** a month-by-month calendar showing which days have entries. Tap any day to review, edit, or delete what you logged.

**Keep your data safe**
- **Export** a full copy of your data to a file you choose, e.g. in Downloads, and **import** it back later or on a new phone.
- Editing a food in your library never changes what you've already logged; past entries keep the values they were recorded with.

## Tech stack

- **Kotlin** with **Jetpack Compose** (Material 3)
- **Room** (SQLite) for on-device storage
- **Vico** for charts
- **CameraX** and **ML Kit** barcode scanning (on-device model, no network)
- **Kotlin Coroutines & Flow**, **Jetpack Navigation**
- Target device: Android 14+ (developed on a Pixel 8a)

## Building

Open the project in Android Studio and run the `app` configuration on a device or emulator running Android 14 or newer.

- Unit tests: `./gradlew testDebugUnitTest`
- On-device tests: `./gradlew connectedDebugAndroidTest` (needs a connected phone or emulator)

## Disclaimer

This project was developed with the help of [Claude Code](https://claude.com/claude-code), Anthropic's AI coding assistant. It's a personal project, provided as-is, and not intended as medical advice or a medical device.
