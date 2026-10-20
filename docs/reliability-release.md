# DATRACK reliability update — 20 October 2026

## Delivered and installed
Actual app `com.loadpredictor`, version **1.0.1**, versionCode **2**. Installed with an in-place replacement; no uninstall and no data clear. Android retained UID 10650 and the original first-install timestamp (28 August 2026). The release is not debuggable.

The user cancelled the Preview redesign. This update uses the original-appearance reliability checkpoint `ad10282`. The palette, type, navigation and Alerts source match the original checkpoint `fc927c1`. Dashboard/promo/widget structures are restored. Functional expiry/stale/error/refresh corrections remain; there is no new design system or added feature screen. The Downloads/ui port, optional load metadata, per-app rankings and their schema-5 migration are excluded. Existing separate Preview data was not downgraded or cleared.

## Retained fixes
- Explicit expired/depleted states; measurement stops at expiry; remaining balance uses the existing offset once.
- Forecast risk reconciled with the chosen forward projection, based on completed-day samples rather than partial days.
- Saved promo baseline/checkpoints and notification milestones preserved for cosmetic edits.
- Notification permission/channel outcomes respected; disabled alerts do not retroactively fire when re-enabled.
- Serialized background evaluation, cancellation handling, stale/error recovery and bounded retries. Fixed an additional gap where explicit retry results could otherwise repeat indefinitely.
- Original widget layouts remain, with connected refresh and shorter forecast text. Duplicate platform polling is disabled; hourly WorkManager scheduling remains.

## Safe testing
New disposable package: `com.loadpredictor.releasecheck.audit`. Its test runner rejects every other package, including actual DATRACK and earlier Preview. Worker tests restore existing rows, active selection, alert values and notification milestones, even on failure. Temporary notifications/work are cleaned up. Verification app and test package were removed after completion.

A copy of the original schema-4 database and alert/milestone files was tested in Verification. After the complete suite, the saved record's ID, name, allowance, tracking start, expiry, usage offset, SIM and active status matched the original. No synthetic promo remained. Notification milestone bytes were unchanged; alert values were restored (default values may be explicitly serialized in the disposable copy).

## Verification results
- Verification and release APKs assembled successfully.
- **138 JVM tests passed**, zero failures/errors.
- **13 device tests passed**, with no skipped widget test: Room migrations, active-promo invariants, notification thresholds/anti-refire, disabled alerts, worker execution, retry exhaustion and real widget preferences/refresh scheduling.
- Release lint: **0 errors, 45 warnings, 1 hint**. Not warning-free.
- Signed APK v2/v3 verification passed. Signing certificate matches the previously installed app. Package/version inspected before installation.
- Actual updated app opens and shows **one saved active `md+ 499` promo**, **24 GB**, **no expiry**, SIM 1 and the existing already-used offset (**6,957,847,020 bytes**). Displayed remaining balance is approximately **7.6 GB**.
- Existing alert settings verified through the actual app: 50% on, 80% off, 90% on, premature depletion off. Editor was opened read-only and dismissed without saving.
- Existing launcher widget ID **59** and its 4-column/1-row size remained. It displays 7.6 GB / 24 GB with the original sparkline structure; the actual refresh action was exercised.
- Actual app is left on Home. No personal-app instrumentation was run.

## Backup and recovery
Before installation, the original installed APK and a consistent app-data archive (database, preferences and app files) were saved privately in this chat workspace under `work/release-verification/`. Original database version is 4; the update keeps version 4 with no migration or destructive fallback. Backup archive structure and saved rows were verified.

Do not uninstall the app to roll back. A corrective APK must retain the package and signing certificate and use a suitable increasing version code. Keep the backup private. The matching existing certificate is the user's Android Debug certificate; it is retained for this local installed-app update, not represented as a new Play Store signing setup. A future store release requires a separately planned signing/distribution process.

[Android's update identity and version requirements](https://developer.android.com/google/play/app-updates) explain why the installed package and certificate were checked.

## Remaining verification limits
The earlier plan was not completely certified despite previous broad completion claims. The two concrete retry/test-cleanup gaps found in this pass are fixed. These wider acceptance checks still require time or more devices:
- Multi-day comparison with Android/carrier usage and ordinary background cadence.
- Reboot, Doze, battery restrictions and long process-death soak coverage.
- Another Android/OEM/launcher and the full screen/accessibility matrix.
- Empirical forecast calibration and unfamiliar-user comprehension checks.

These are not claimed passed by the automated test count or this single-device update.

## Source and artifact
Implementation milestone: `af7572c` on local main; no push.
APK SHA-256: `89f90972d021d5bb0131c436035e4fbe2da7e9c9a83746d06a7707985d61f058`.
