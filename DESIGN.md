# DATRACK appearance preservation

The user cancelled the Preview UI work and requested only safe testing, logic fixes, reliable updates and release verification in the original app.

## Authority
Keep the original native appearance from `fc927c1`, using the original-appearance reliability checkpoint `ad10282`. No Downloads/ui port, Outfit/DM Mono replacement, new cards, new navigation or additional feature screens ship in this update.

## Scope
Retain existing colors, Android typography, five-tab navigation, promo cards, radial dashboard and ring/sparkline widget layouts. Add approved functional controls using existing Material components and wrapping, scrollable text. Preserve terminal/stale status text, save failures, expiry input and refresh actions. Do not introduce another visual redesign.

## Data
Upgrade Room schema 4 additively to 6, preserving package com.loadpredictor, promo IDs, active selection, allowance, tracking start, expiry, offset and alert settings. Version updates increment versionCode and keep the existing signing certificate. Never uninstall or clear the personal app to update it.

## Verification
Automated instrumentation is restricted to com.loadpredictor.releasecheck.audit. Never run mutating test suites against the personal app or the earlier Preview. Existing Preview schema-5 data is a separate lineage; never install this build over it.

## Approved functional addition
The 1.1.0 Home three-dot action opens Data and support: configuration export, reviewed restore with safety backup, and explicit support export. It uses existing Compose styling and scrollable, wrapping text. This does not authorize a visual overhaul.

## Approved steps 4–6 controls
Carrier comparison/confirmation/history/undo in Promos; daily budget and expiry settings in Home/Alerts; selected-date Data Diary export and mobile app usage in History. No new navigation, colors, typeface or widget graphics. The larger-text checks use 1.3× system font scale and restore the original setting afterward.

## Scoped tool polish (1.2.1)
The user requested refinement of the new features. Operate mode: complete a configuration or inspection task with clear actions and visible state. Keep the original screen hierarchy, mint palette, type styles and widget graphics. Secondary tools use Material bottom sheets with contextual headers, a fixed primary action above the keyboard, 48 dp touch targets and scrollable content. Use existing surface tokens rather than default purple dialog tones. Carrier input and confirmation are separate views; budget/reminder controls are grouped; the Data Diary separates daily and app views with measured bars. Detailed caveats remain available in expandable help. Backup replacement still requires validation and a fresh safety export. No service, database or measurement logic changes belong to this refinement.
