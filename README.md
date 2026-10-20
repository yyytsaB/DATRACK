# DATRACK — prepaid data runway

Native Android estimates for a manually configured mobile-data allowance. Device mobile usage is not a carrier balance, and SIM labels do not isolate per-SIM billing.

## Current scope
Kotlin, Compose, Room schema 4, DataStore, WorkManager and Glance. Original appearance retained. Reliability hardening, bounded local diagnostics and configuration backup/restore are the approved work for 1.1.0. Carrier reconciliation, budgets, new attribution features and carrier automation remain on hold.

## Reliability
- Save and activation use one Room transaction; missing targets do not deactivate the saved context.
- Cosmetic edits preserve current checkpoints; older or differently based observations cannot overwrite them.
- A shared coroutine gate serializes edits, forecast evaluation, worker alerts and restore. Widget and alert results use one worker calculation.
- Expired/depleted/permission/stale states remain explicit. Counter revisions recalibrate velocity without silently changing the saved offset. Completed-day forecasts and estimated ranges remain estimates, not calibrated confidence intervals.
- Original widget graphics, tap-to-open and refresh remain. WorkManager updates are about hourly and can be delayed by Android.

## Data and support
Open the small three-dot action on Home.

- **Export configuration backup:** an unencrypted versioned JSON file saved through Android's system file picker. Includes promo IDs/configuration, offsets, alert settings and notification milestones. Excludes Android traffic history, forecast caches, work queues and widget IDs. Keep the file private.
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
| 4. Carrier balance reconciliation | On hold |
| 5. Daily budgets / expiry reminders | On hold |
| 6. Further feature expansion | On hold |

No Codex monitor, account, cloud backend or publication is created by these changes. The 72-hour/seven-day observation period is elapsed-time verification, not an automated-test completion claim. See [the 1.1.0 release record](docs/plans-1-3-release.md) for exact results and limits.

## License
MIT
