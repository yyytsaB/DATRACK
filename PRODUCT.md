# DATRACK
<!-- impeccable:product-schema 1 -->
## Platform
android
## Users and purpose
Philippine prepaid mobile data users who configure their promo and want to understand remaining allowance, validity and estimated depletion.
## Stack
Kotlin, Jetpack Compose, Room, WorkManager and Glance.
## Current scope
Reliability update to the actual app, preserving its original appearance and upgrading schema-4 saved data additively to schema 6. Manual promo setup, offsets, exact expiry, active selection, forecasts, history, notifications and existing widgets remain available. Measurements cover device-total mobile data, not isolated per-SIM billing.
## User direction
Stop UI redesign work. Prioritize safe tests, correct logic, dependable updates and verified in-place upgrades. The Downloads/ui visual port, pricing, automation and backend features remain excluded. The user subsequently approved manual reconciliation, budgets/reminders, factual CSV export and mobile per-app usage.
## Safety boundary
Only the disposable Verification package may run instrumentation. Original app data is backed up before an update. Preserve package identity and signing certificate; no uninstall or data clear.

## Approved feature scope
Bounded private diagnostics with explicit support export, and versioned configuration backup/restore through the Android file picker. Restore requires a fresh safety export and uses durable startup recovery. These additions use a small Home action and scrollable dialog; the original visual design stays. Steps 4–6 were subsequently approved with CSV Data Diary and mobile app usage as step 6.
