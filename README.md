# DATRACK — prepaid data runway

Native Android estimates for a manually configured mobile-data allowance. Device mobile usage is not a carrier balance, and SIM labels do not isolate per-SIM billing.

## Current scope
Kotlin, Compose, Room schema 6, DataStore, WorkManager and Glance. Version 1.2.1 retains the original appearance and the reliability/backup features from 1.1.0. Approved steps 4–6 add manual carrier corrections, daily advisory budgets, actual-expiry reminders, CSV Data Diary and mobile app usage. Carrier automation, cloud features, recommendations and monetization remain deferred.

## UI refinement in 1.2.1
The added tools now use native mint/dark bottom sheets with fixed primary actions, keyboard-safe spacing, clearer field/choice contrast and expandable explanations. Carrier entry and comparison review are separate views. Budgets and expiry choices are grouped. Data Diary has daily/app views with measured bars; Export daily CSV names its scope. Backup review, safety export, recovery and replacement protections remain. Existing dashboard, navigation, fonts, widget graphics and saved data are preserved. See [the polish verification record](docs/ui-polish-1.2.1.md).

## Reliability
- Save and activation use one Room transaction; missing targets do not deactivate the saved context.
- Cosmetic edits preserve current checkpoints; older or differently based observations cannot overwrite them.
- A shared coroutine gate serializes edits, forecast evaluation, worker alerts and restore. Widget and alert results use one worker calculation.
- Expired/depleted/permission/stale states remain explicit. Counter revisions recalibrate velocity without silently changing the saved offset. Completed-day forecasts and estimated ranges remain estimates, not calibrated confidence intervals.
- Original widget graphics, tap-to-open and refresh remain. WorkManager updates are about hourly and can be delayed by Android.

## Features added in 1.2.0
- **Promos > Check carrier balance:** compare a general-pool carrier reading from the last 15 minutes without changing data. Use this reading explicitly accepts a new balance checkpoint; Undo latest correction retains a dated history. Raw measured traffic alone trains forecasts and history. Original offset, dates and IDs remain. Cosmetic edits retain corrections; changing tracking start or already-used bytes invalidates them with a form warning. Revision-based alert priming prevents a replay of old crossings.
- **Home or Alerts > Budgets & reminders:** configure a daily device-wide mobile budget in MB (1024 MB = 1 GB), optionally notify once per local day, and optionally remind within 6/24/48 hours of the active promo's actual expiry. Defaults are off. Counts start at local midnight and exclude historical offsets and carrier corrections. Reminders do not use predicted depletion or invent expiry for no-expiry data. Delivery depends on Android background scheduling and notification permissions.
- **History > Data Diary:** select up to 90 recent calendar days clipped to the tracking/expiry period. Load and export the reviewed measured snapshot as CSV with exact bytes, source, scope, timezone, timestamps and partial-day labels. No claimed savings or permanent Android history retention. CSV names are escaped for spreadsheet safety.
- **Mobile app usage:** uses the same selected window and only mobile network statistics. Buckets are grouped by UID; shared groups and unresolved names are labeled. Device and UID totals are shown separately, including any discrepancy. Only targeted launcher-intent visibility is declared to resolve ordinary app labels; no QUERY_ALL_PACKAGES, SMS or VPN permission.

## Data and support
Open the small three-dot action on Home.

- **Export configuration backup:** an unencrypted versioned JSON file saved through Android's system file picker. Format 2 includes promo IDs/configuration, offsets, alert settings, notification milestones, correction history and advisory settings. Format-1 backups remain readable; an older app will reject format 2 rather than partially restoring it. Excludes Android traffic history, forecast caches, work queues and widget IDs. Keep the file private.
- **Review backup to restore:** validates the complete file (maximum 1 MB / 1000 promos), shows replacement scope, and requires a fresh safety export before explicit replacement. A changed configuration invalidates an older safety copy.
- **Interrupted restore:** a durable private journal completes replacement of Room data and preferences on startup. Edits are blocked until recovery completes. A damaged target falls back to the validated private safety copy. Retry recovery is available in the same dialog.
- **Alerts after restore:** already-crossed thresholds are consumed on the first successful restored reading, avoiding a replay of old notifications.
- **Export support report:** bounded private diagnostics (up to 1000 events or seven days). Reports include version, Android API, outcomes, timestamps, usage windows and byte counts; no notification contents or unrelated app listings. Export is explicit; nothing is uploaded automatically.

## Safe build and testing
Use JDK 17 and Android SDK 36. Instrumentation is restricted to **com.loadpredictor.releasecheck.audit** by SafeTestRunner. The actual package is **com.loadpredictor**. Existing Preview (`com.loadpredictor.audit`) may have a different historical schema and is not the verification target.

```powershell
.\gradlew.bat testVerificationUnitTest assembleVerification assembleVerificationAndroidTest assembleRelease lintRelease --offline
```

APK paths: `app/build/outputs/apk/verification/app-verification.apk` and `app/build/outputs/apk/androidTest/verification/app-verification-androidTest.apk`. If Gradle's connected-test wrapper lacks offline UTP dependencies, install these two APKs and invoke:

```powershell
adb shell am instrument -w -r com.loadpredictor.releasecheck.audit.test/com.loadpredictor.testing.SafeTestRunner
```

Actual schema 4 upgrades additively to 6, preserving existing rows and runtime checkpoints. Schema 5 belongs to the historical Preview and is not an upgrade target for this build. Never install the current build over that Preview.

Place a Verification widget before the widget interaction test; otherwise that test explicitly skips. Mutating tests restore rows, active selection and preferences and clear test work/notifications. Never run them on the personal app. No destructive Room fallback or schema downgrade is used.

Release artifacts must retain the installed package, signing certificate and an increasing versionCode. Never uninstall the personal app to apply an update. The current local installation uses its existing Android Debug certificate; public-store signing requires a separate continuity plan.

## Measurement limits
Mobile traffic is aggregated across the device. Initial already-used bytes are counted once. General/open-access pools are the intended allowance; zero-rated and specialized carrier buckets are not isolated. Reactivating a promo still uses its original measurement window, including inactive periods. History is queried from Android and the current view is limited to 90 days; a configuration backup does not promise permanent usage-history retention.

## Roadmap status

| Step | Status |
|---|---|
| 1. Existing logic and race hardening | Implemented; targeted failures reproduced and covered |
| 2. Local diagnostics and release verification | Implemented; multi-day/Doze/other-device evidence remains pending |
| 3. Configuration backup and safe restore | Implemented; validation, native picker and interrupted-recovery checks |
| 4. Carrier balance reconciliation | Implemented with comparison, confirmation, undo and additive migration |
| 5. Daily budgets / expiry reminders | Implemented; opt-in, advisory and flexible delivery |
| 6. CSV Data Diary / mobile app usage | Implemented; other later features remain deferred |

No Codex monitor, account, cloud backend or publication is created by these changes. The 72-hour/seven-day observation period is elapsed-time verification, not an automated-test completion claim. See [the 1.2.0 release record](docs/steps-4-6-release.md) and [earlier 1.1.0 record](docs/plans-1-3-release.md) for exact results and limits.

## License
MIT
