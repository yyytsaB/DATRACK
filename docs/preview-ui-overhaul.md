> Historical record: this appearance has been superseded by the user-supplied Downloads/ui reference port. See [reference-ui-implementation.md](reference-ui-implementation.md) for the current delivery.

# DATRACK Preview â€” dark mint UI/UX overhaul

## Delivered
- Cohesive charcoal/mint palette, Android type roles and consistent 20dp gutters and section spacing.
- Dashboard puts usable balance and allowance first, followed by forecast, runway, daily average, estimate source and daily usage.
- Promo names have their own full-width area. Tracking, edit and delete actions occupy a separate row. Add promo is in the scrolling content so it cannot cover another promo.
- Native, expandable form fields, readable unit selectors and presets; exact expiry and already-used balance remain available.
- Usage history retains date ranges, all scrollable bars, tap inspection and daily readings. Zero usage no longer draws a positive-height bar.
- Alerts use clearer language and retain notification permission/settings recovery.
- Widget page offers one supported native pin action and explains sizing, refresh and update timing. Actual widget shows the balance, status, reading age and a dedicated refresh action. Minimum height increased for readable text.

## Verification
- Debug APK builds successfully.
- All 138 JVM regression tests pass.
- Android lint: 0 errors, 68 warnings and 1 hint. Warnings remain; this is not a warning-free project.
- Connected Samsung Galaxy A22 5G, Android 13: native screen checks at font scales 1.0 and 1.3, promo add/edit and delete confirmation (cancelled), widget refresh and tap-to-open.
- Widget state roundtrip/refresh scheduling test passes on the device.
- Secondary text contrast is at least 5.8:1 on the three main app surfaces.
- Font size restored after checks.

## Boundaries
This build updates only `com.loadpredictor.audit` (DATRACK Preview). Original DATRACK and its saved md+ 499 promo remain separate. Domain, database, forecast, notification and worker behavior are preserved. No preview promos were deleted by this redesign.

Tested on the connected phone at the stated sizes; tablet layouts and every possible font scale are not visually certified. Current Preview contains synthetic test promos, so its live history can be sparse. Illustrative values on the Widgets page are labelled when no live promo exists.

## Design workflow
Applied Impeccable's Operate, Android, new-work and craft-floor guidance. Its local helper executable was unavailable, so the user chose the dark mint direction through chat and implementation used native Compose code. User feedback: the prior redesign was too plain and had poor layout/spacing. The final direction preserves informational depth.
