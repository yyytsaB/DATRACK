# DATRACK appearance preservation

The user cancelled the Preview UI work and requested only safe testing, logic fixes, reliable updates and release verification in the original app.

## Authority
Keep the original native appearance from `fc927c1`, using the original-appearance reliability checkpoint `ad10282`. No Downloads/ui port, Outfit/DM Mono replacement, new cards, new navigation or additional feature screens ship in this update.

## Scope
Retain existing colors, Android typography, five-tab navigation, promo cards, radial dashboard and ring/sparkline widget layouts. Correct terminal/stale status text, save failures, expiry input and refresh actions only where required for functional correctness. Do not introduce another visual redesign.

## Data
Keep Room schema 4, package com.loadpredictor, promo IDs, active selection, allowance, tracking start, expiry, offset and alert settings. Version updates increment versionCode and keep the existing signing certificate. Never uninstall or clear the personal app to update it.

## Verification
Automated instrumentation is restricted to com.loadpredictor.releasecheck.audit. Never run mutating test suites against the personal app or the earlier Preview. Existing Preview schema-5 data must not be downgraded by installing this schema-4 verification build over it.

## Approved functional addition
The 1.1.0 Home three-dot action opens Data and support: configuration export, reviewed restore with safety backup, and explicit support export. It uses existing Compose styling and scrollable, wrapping text. This does not authorize a visual overhaul.
