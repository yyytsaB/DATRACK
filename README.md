# DATRACK — prepaid data runway

A native Android app that estimates whether a manually configured mobile-data allowance will last until its actual expiry. DATRACK measures device mobile traffic through Android Usage Access; it does not obtain a carrier balance.

## Current implementation

- Kotlin, Jetpack Compose Material 3, Room schema v4, DataStore, WorkManager and Glance.
- Five destinations: Home, Promos, History, Alerts and Widgets.
- Separate allowance, historical usage offset, measurement start and optional actual expiry timestamp. The editor supports exact expiry date/time for mid-cycle setup.
- Explicit expired/depleted/calibrating states and consistent forward risk classification across the app, widgets and background alerts.
- Adaptive EMA sampling, with a rolling mean of up to seven completed local calendar days when at least three full days are available. Partial days do not train the daily model.
- Shared offset-plus-measured balance arithmetic, including inactive snapshots.
- Threshold alerts at 50/80/90% and premature depletion. Permission/channel-blocked submissions are not marked delivered. Disabling a threshold intentionally consumes it when crossed.
- About-hourly background checks, with explicit user/mutation-triggered refresh. Android can delay work; its usage statistics can lag actual traffic.
- Original mint/dark visual design restored at user request: ring gauge, typography, navigation, promo cards and compact/wide widget graphics. Functional fixes remain integrated into the original screens.
- Scrollable history chart covering the selected range, accessible day/value labels and lazy daily rows. History is capped to a rolling 90-day window, not lifetime retention.
- Original compact/wide launcher widget layouts with ring/sparkline graphics, concise ETA wording, a real refresh action and tap-to-open.

## Measurement limits

Usage is mobile-only and aggregated across the device's SIMs. Selecting a SIM in DATRACK labels a manual promo context; it does not switch the phone's mobile-data SIM or isolate its traffic. Reactivating an older promo continues to query its measurement window; inactive periods are not automatically excluded.

Carrier-specific unlimited apps, specialized bonus pools and zero-rated traffic cannot be separated. The tracked allowance should represent the general pool. Remaining bytes are an estimate and may differ from the carrier's accounting. Expired allowance is shown as unused rather than usable data.

## Build and test safely

Debug builds install as **DATRACK Preview**, package `com.loadpredictor.audit`, with separate databases, preferences and widget providers. Release remains `com.loadpredictor`. Never run destructive test fixtures against the personal app.

Use JDK 17 toolchains and Android SDK 36:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug --no-configuration-cache
.\gradlew.bat assembleDebugAndroidTest --no-configuration-cache
```

The preview APK is `app/build/outputs/apk/debug/app-debug.apk`. Usage Access is required for meaningful readings; a physical device with mobile data is required to assess accuracy. Notification delivery also needs Android notification permission and an enabled channel.

Place a DATRACK Preview widget before the real widget persistence/refresh instrumented test; otherwise that test explicitly skips. The test asserts its target has the `.audit` suffix. It saves/restores that widget's prior state. Other worker tests create preview-only fixtures and change preview alert preferences; use the preview as a test installation.

On the audited workstation, Gradle's connected-test wrapper lacks offline UTP dependencies. Building both APKs, installing them and invoking `com.loadpredictor.audit.test/androidx.test.runner.AndroidJUnitRunner` directly runs the same device suite.

## Validation snapshot — 20 October 2026

- 138 JVM tests pass.
- 12 instrumented tests pass on Samsung Galaxy A22 5G, Android 13, including actual placed-widget state persistence and refresh scheduling.
- Preview and test APKs assemble; Android lint has no errors. Remaining warnings include dependency/icon maintenance and context-holder warnings whose factories pass application context.
- Actual Samsung launcher placement, resizing, body tap and refresh were checked during implementation. The earlier redesigned layout passed a 200% text check; that certification does not apply to the subsequently restored original layout. Revalidate text-size coverage before release.
- A new preview promo was created through the UI and survived a data-preserving preview update. Original app data and widget were not upgraded or deleted.

This is a verified preview, not a complete release certification. See [implementation status](docs/implementation-status.md) for outstanding cross-device, accessibility and multi-day measurement checks. No database schema change was needed for these fixes; existing v4 rows preserve their measurement boundary and offset.

## Architecture

`data/` wraps Room, preferences, Android measurement and notification submission. `domain/` holds Kotlin models, forecasting and use cases. `presentation/` contains Compose screens and Glance state/layouts. `worker/` schedules and serializes background measurement/alert evaluation. Foreground polling refreshes only the selected screen; stale failures remain explicit.

## Next scope

Carrier balance reconciliation, daily budgets, expiry reminders, local exports and any multi-carrier expansion are separate follow-up features. True per-SIM attribution, automatic specialized-pool accounting and a community backend are outside this implementation.

## License

MIT
