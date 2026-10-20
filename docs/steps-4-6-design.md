# Steps 4–6 implementation decisions

Approved scope: manual carrier reconciliation; daily advisory budgets and actual-expiry reminders; factual CSV Data Diary and mobile-only per-app attribution. Original visual identity and existing personal data remain. New local notifications default off. No carrier automation, presets catalogue, backend, monetization or dual-SIM attribution is included.

## Carrier correction
An additive schema-4-to-6 migration adds nullable balance anchors and a dated, undoable correction table. Schema 5 is deliberately skipped: it belonged to an abandoned Preview lineage. No schema-5 Preview is updated by this release. No destructive fallback.

The original offset and measured-use checkpoint remain. At the accepted observation time, remaining equals the carrier's general-pool reading; later measured traffic subtracts once. All foreground, widget and alert surfaces share Promo.remainingBytes. Raw traffic alone trains forecasts and history. Below-checkpoint revisions of Android statistics require a fresh carrier comparison, except terminal expiry still takes precedence.

Compare is read-only. Use this reading is explicit, checked against the unchanged promo configuration and a maximum 15-minute age. Balance must lie within allowance. Duplicate confirmation fails safely. Undo marks the latest event undone and returns to the preceding anchor, retaining history. A tracking-start/offset edit explicitly invalidates anchors; incompatible allowance edits are rejected. Corrections never silently rewrite the historical offset.

Each correction/undo increments a durable revision. First successful worker evaluation of that revision consumes already-crossed milestones once. Later crossings re-arm; process death between Room writes and worker execution does not replay old thresholds. Backups export format 2 with correction history; format-1 backups remain readable. Older apps intentionally reject the newer backup version.

## Daily budget and expiry reminders
A user-controlled device-wide daily cap counts measured mobile traffic since local calendar midnight, including DST boundaries; it excludes offsets and correction deltas. It is advisory, never data blocking. Current usage has an observation time and explicit permission/unavailable states. The optional expiring-promo pace suggestion never invents validity for no-expiry promos.

Notifications are opt-in and use existing flexible worker paths. Current events are consumed when settings change, avoiding retroactive alert bursts; future daily crossings and expiry windows are eligible once. Keys identify the local date or promo/actual expiry/reminder window. Permission/channel-blocked posts remain unconsumed. No-expiry, deleted, inactive or expired promos have no expiry reminder. Existing work reads current state rather than retaining obsolete scheduled reminder snapshots. Restore primes already-eligible events before notifications resume.

## Data Diary and per-app mobile usage
One History action opens native controls with wrapping, scrollable text. User-selected dates are limited to 90 recent local calendar days and clipped to the actual tracking window/expiry. CSV exports an immutable reviewed measurement snapshot: exact bytes, timestamps, source, scope, timezone and complete-calendar/partial-day labels. It excludes offsets, corrections, carrier balances and unsupported savings claims. CSV quotes fields and neutralizes formula-leading names.

Per-app queries use NetworkStatsManager.querySummary with TYPE_MOBILE and null subscriber ID over the same selected window. All UID buckets are grouped once; shared UIDs are identified, and unavailable app labels retain their UID. Only launcher-intent package visibility is declared to resolve ordinary app labels; no QUERY_ALL_PACKAGES permission. Attributed totals and the device total are shown independently with any discrepancy explained, never forced to match. Missing permission, unavailable readings and genuine empty results remain distinct.

Sources: [Android network statistics](https://developer.android.com/reference/android/app/usage/NetworkStatsManager), [targeted package visibility](https://developer.android.com/training/package-visibility/declaring), [non-destructive Room migrations](https://developer.android.com/training/data-storage/room/migrating-db-versions).

## Verification boundaries
Run deterministic logic, migration, restore and instrumentation tests only against Verification. Keep the personal app on 1.1.0 until all milestones and data-preserving update checks pass. Commit each major feature milestone separately. Prolonged Doze, reboot, 72-hour/seven-day and other-device evidence remain pending; this implementation does not certify elapsed background reliability.
