# DATRACK 1.1.0 — approved steps 1–3

## Delivered

The original appearance, package com.loadpredictor and Room schema 4 remain. VersionCode increases from 2 to 3. Steps 4–6 (carrier reconciliation, budgets/reminders, further feature expansion) remain on hold.

1. Atomic promo edits and activation; invalid targets cannot deactivate an existing promo. Cosmetic edits preserve newer checkpoints. A shared operation gate and conditional checkpoint writes reject stale observations. Worker widgets and alerts use one forecast snapshot. Revised traffic counters recalibrate velocity without changing saved offsets; expiry/depletion retains precedence. Estimate ranges use actual bounds.
2. Private bounded diagnostics record update outcomes, sampling windows and byte counts: at most 1000 events / seven days. Explicit support export includes version and Android API. No automatic uploads, notification text or unrelated app listings.
3. Configuration export and reviewed restore via Android's file picker. Strict version/UTF-8/model validation, 1 MiB / 1000-promo bounds, exact integer bytes, unique IDs and at most one active promo. Explicit replacement requires a fresh safety export. A private durable journal recovers interrupted database/preferences replacement on startup; a corrupt target falls back to the validated private safety copy. Mutations remain blocked during pending recovery. Restored thresholds are primed on the first successful observation to prevent replay.

Backups are unencrypted JSON. They contain promo configuration, IDs, offsets, alert preferences and notification milestones. They exclude Android usage history, forecast caches, queued work and widget IDs. Export and support actions are under Home > three-dot > Data and support. A configuration backup is not a full device-history archive.

## Verification

- Three original race/state defects reproduced before fixes; all five targeted device race tests passed after fixes.
- 146 JVM regression tests passed.
- 23 isolated device tests passed, with the Verification widget placed and no skipped tests. The test runner refuses the personal app and historical Preview packages.
- Release lint: zero errors, 46 warnings and one hint. Existing warnings are not represented as resolved.
- Native file picker: configuration export, review, mandatory safety export and explicit restore passed against the disposable Verification package.
- Fresh-process startup: a staged pending restore recovered the target promo and alert settings, cleared its journal, and then recovered back to the saved-data clone. Larger-text dialog actions remained accessible; system font scale restored.
- Release built and signature verified with the installed certificate (SHA-256 ebaa64b8c9aedc7a24f2d1ca9f251fc57030d52b8a77cead2d5013504838bf9b). It retains the existing Android Debug certificate for local upgrade continuity; this is not a public-store signing launch.

Build command: `gradlew.bat testVerificationUnitTest assembleVerification assembleVerificationAndroidTest assembleRelease lintRelease --offline` with JDK 17. Instrumentation: `com.loadpredictor.releasecheck.audit.test/com.loadpredictor.testing.SafeTestRunner` only.

## Personal-app update

Applied in place using replacement installation; no uninstall, data clear, destructive Room fallback or schema downgrade. The previous installed APK was retained privately. A new user-accessible configuration backup is saved in Downloads as DATRACK-1.1.0-backup.json. The fresh backup verified one promo, ID 2, md+ 499, allowance 25,769,803,776 bytes, tracking start 1787138239892, offset 6,957,847,020 bytes, SIM 1, active, no expiry. Alert switches remain 50 on / 80 off / 90 on / premature off. App UID 10650 and original first-install timestamp remain. Launcher widget ID 59 retains the same provider, 4-column / 1-row placement and 334 × 116 dp options. Widget refresh and tap-to-open were checked after the update. Disposable Verification packages and their temporary exported files were removed; the user backup remains.

## Remaining evidence

72-hour / seven-day observation, prolonged Doze/battery restriction, reboot recovery and additional manufacturer/device coverage remain pending. Passing deterministic tests is not proof of elapsed background reliability. No scheduled Codex monitor was created. Android may delay background work and usage statistics. Device mobile traffic remains aggregated across SIMs and is not a carrier balance. No code was pushed or publicly published.

## Milestones

- 265ba8c — atomic promo changes and stale-observation rejection.
- 39106e1 — bounded local diagnostics and support export.
- Configuration backup/restore, final recovery guards and this release record: accompanying milestone commit.
