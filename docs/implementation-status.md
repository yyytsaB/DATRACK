> Historical record: this appearance has been superseded by the user-supplied Downloads/ui reference port. See [reference-ui-implementation.md](reference-ui-implementation.md) for the current delivery.

# UI rollback requested by user

The original appearance is restored from `fc927c1`, retaining the core feature fixes and reliability work. This supersedes the UI redesign statements in the historical milestone record below.

Restored: dashboard ring/structure, dark mint palette, original font, five-tab navigation, promo cards and sheet styling, Alerts/History styling, widget gallery, and original ring/sparkline widget layouts and sizing.

Retained: expiry logic and exact-expiry input, offset accounting, anchored forecast/risk consistency, complete-day sampling, notification outcome handling, serialized workers, cancellation/stale recovery, saved forms/navigation, visible save errors, protected deletion, full-range accessible chart, lazy history rows, isolated builds and all regression tests. The widget still has an actual refresh control and concise ETA wording. No domain/data/worker code was reverted.

The earlier redesign's adaptive navigation/light theme were appearance changes and have been removed. Large-font/device certification needs to be repeated against the restored UI rather than inherited from the rejected redesign.

---

# Implementation status â€” 20 October 2026

## Committed milestones

- `fc927c1`: isolated debug identity (`com.loadpredictor.audit`).
- `60896e8`: validity/balance fixes, unified forward forecasts, modern mint screens and initial readable widget.
- `38d13c2`: complete seven-day training window and integration regression coverage.
- `4fff3c3`: configuration-independent widget text, adaptive navigation, stale-reading recovery, cancellation/race protection and real widget persistence/refresh tests.

Documentation changes follow in their own commit. All commits are local; nothing was pushed.

## Implemented

- [x] Preview installation separate from the personal app and its data.
- [x] Explicit expiration, no post-expiry measurement or usable-runway claims.
- [x] Exact actual expiry input independent of the existing measurement start.
- [x] Shared remaining-balance arithmetic with historical offsets.
- [x] Consistent forward risk after EMA/daily-mean selection; completed-day sampling.
- [x] Notification permission/channel outcomes and milestone preservation on cosmetic edits.
- [x] Serialized periodic/immediate worker evaluation; bounded outer retry handling.
- [x] Measurement errors are not fabricated zeroes; last-good app reading is labeled stale on failure.
- [x] Saved navigation and editor fields, visible save failures, protected deletion.
- [x] Mint app redesign, light/dark schemes and expanded-width navigation.
- [x] Full-range scrollable chart and lazy history list with partial-day labels.
- [x] Compact/expanded widget content, real refresh, accurate pinning explanation, no duplicate system polling.
- [x] Regression tests, assembled preview and actual-device instrumented suite.

## Verified on hardware

Samsung SM-A226B / Android 13. Original package `com.loadpredictor` unchanged; all new builds/tests use `com.loadpredictor.audit`.

- All five screens opened in the preview.
- A promo was created and saved via the new editor, became the active context and remained stored across preview APK updates.
- Actual preview widget placed on Samsung launcher; compact and expanded resizing exercised.
- Widget body opens the app; refresh changes the reading and the instrumented callback enqueues work.
- Instrumentation checks real per-widget preferences for success/no-promo/permission/error transitions and restores its prior state.
- Expanded widget visually verified with complete text at 200% font after fixing cached configuration behavior. App dashboard and large-text navigation visually verified at 200%. Font scale restored to 1.0.
- 138 JVM tests and 12 device tests pass. Preview/test APK assembly and lint pass; lint warnings remain maintenance work, not a clean-warning claim.

## Deliberate plan refinements

No Room migration was necessary: schema v4 already stores a measurement start and independent optional expiry. Reusing those fields preserves existing balances without inventing historical promo start information. The UI collects actual expiry rather than a duration from registration.

Widgets use concise static content instead of a decorative chart or font-dependent conditional layout. Samsung can enlarge native widget text without rerunning Glance composition; content is therefore kept short even before a refresh. Compact layouts prioritize balance/status/actions; expanded layouts add runway/freshness.

The historical burn index remains available in the engine, but user-facing risk is reconciled to the actual selected projection. Estimated ranges are presented as estimates, not statistically calibrated confidence bounds.

## Remaining release gate â€” not claimed complete

- [ ] Multi-day comparison with Android mobile-data settings and carrier remaining balance, accounting for specialized/zero-rated buckets.
- [ ] Another Android version and another launcher/OEM.
- [ ] Full 320 dp/tablet/foldable/multi-window/landscape matrix, every state at every font setting. Adaptive code exists, but device coverage is currently one portrait phone.
- [ ] TalkBack, contrast measurements across every surface, keyboard/IME edge cases and reduced-motion review.
- [ ] Reboot, Doze/battery restriction and ordinary OS process-death soak tests; force-stop behavior documented separately.
- [ ] Three unfamiliar users' comprehension check.
- [ ] Product decision/empirical calibration for uncertainty and acceptable forecast/measurement discrepancy.
- [ ] Optional shared application dependency graph/observation cache to reduce initial duplicate repository queries further. Foreground recurring work is already limited to the selected screen.
- [ ] Personal-package release upgrade after the above validation; preview must not be confused with carrier balance.

Product additions (daily budget, carrier-balance reconciliation, exports, reminders) remain follow-up scope as the plan specified. No background monitor or scheduled automation was created.
