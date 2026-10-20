# DATRACK 1.2.1 — scoped feature UI polish

## Scope and visual result

Refinement of the new carrier, budget, Data Diary and backup/support tools. The original mint/dark identity, existing type styles, dashboard, promo cards, five-tab navigation and launcher widget graphics remain. No measurement, reconciliation, notification-policy, repository or schema changes. Room remains schema 6.

- Replaced dense default alert dialogs with native Material bottom sheets on the existing SurfaceLayer1 palette.
- Contextual headers and close actions; scrollable content; a fixed primary action remains above the keyboard. Saves and pending recovery retain dismissal protection using current state.
- Clear field borders, numeric keyboards, short labels, visible units, selected chips, 48 dp touch targets and separated task groups. Scrolling feedback remains visible above the body.
- Carrier reading entry and comparison review are distinct states; offset/history caveats and correction history remain available without dominating entry.
- Daily budget and expiry settings have separate sections, optional notification controls and wrapping reminder choices. The current budget readout stays on Home/Alerts.
- Daily and app usage have separate views, proportional measured bars, stable byte values and distinct device/app totals. Dates and explanatory details remain available. Export daily CSV explicitly identifies the export content.
- Backup actions use labeled rows; validation, mandatory safety export, reviewed replacement, cancellation and retry recovery retain the original protections. Unencrypted/configuration-only limits remain visible or available in help.
- Expandable help retains the original measurement/privacy caveats; no new product claims or permissions. Material icons replace the added Unicode overflow glyph.

## Evidence

- 157 regression tests passed.
- 29 Verification device tests passed with no skipped tests, including existing widget, notification, migration and backup-recovery coverage.
- Release lint: zero errors, 46 warnings and one hint. Existing warnings are not represented as resolved.
- First batched phone pass covered normal and 1.3× text, keyboard entry, carrier comparison/apply/undo, budget save, daily/app views, CSV creation, backup export/review, mandatory safety export and confirmed restore. All mutating fixture actions were confined to Verification.
- Final bounded confirmation covered visible error feedback, keyboard-safe primary action, MB label, explicit CSV scope, choice outlines and larger text after the single fix batch. The system font scale was restored.
- Source-token contrast on the changed surfaces: body 6.20:1, input outline 4.03:1, primary button text 13.29:1, selected chip text 10.02:1. This is a scoped token check, not an app-wide accessibility certification.
- Hardware: Samsung Galaxy A22 5G / Android 13. No new tablet, other-device or elapsed background-reliability claims.
- The Impeccable executable was unavailable locally; its written polish, craft-floor and Android guidance were used alongside the repository design authority.

## Installation and data

Installed in place after the two bounded native UI passes and isolated test suite. Phone Downloads contains DATRACK-before-1.2.1.json and DATRACK-1.2.1-backup.json. The entire exported configuration matched before/after, ignoring only export time: promo data, corrections, notification milestones, alert settings and budgets were preserved. Original app UID and first-install date remain. Launcher widget ID 59 retained its placement/dimensions; refresh and tap-to-open passed. Temporary Verification packages/widget and their exported fixture files were removed. Original font scale was restored. The implementation milestone is 7029f3f; the accompanying documentation commit records these final checks.

VersionCode 5 / package com.loadpredictor / Room schema 6. The release signature matches the existing certificate. No uninstall or data clear is used for the personal app. New polish changes are limited to presentation and release metadata. Long-running background/Doze/reboot/other-OEM evidence from earlier releases remains pending. No public publishing or push was performed.
