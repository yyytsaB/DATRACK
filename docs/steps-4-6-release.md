# DATRACK 1.2.0 — steps 4–6 release record

## Delivered scope

Version 1.2.0 (versionCode 4), package com.loadpredictor. The original mint/dark appearance, navigation, dashboard graphics and launcher widget remain; only small functional actions and native scrollable dialogs were added. The approved step-6 selection is CSV Data Diary and mobile per-app usage. Carrier automation, recommendations, community/backend features, monetization, presets catalogue and combined per-SIM accounting remain deferred.

- **Carrier reconciliation:** Promos > Check against carrier balance. Review a general-pool reading no older than 15 minutes, see the app/carrier discrepancy, then explicitly Use this reading. Undo and dated history are retained. Both upward/downward corrections work. Later measured traffic subtracts once. Original offset, IDs, dates and measurement-rate training stay intact. Duplicate confirmation and changed/stale configurations are rejected. Correction revisions consume already-crossed thresholds once, then allow later crossings. Historical start/offset edits invalidate the anchor with a form warning.
- **Daily advisory budgets:** Home or Alerts > Daily budget and expiry reminders. Configure MB (1024 MB = 1 GB), see today's measured mobile usage and check time, and optionally enable a once-per-local-day threshold notification. Uses local calendar midnight rather than fixed 24-hour arithmetic. Excludes saved offsets and carrier adjustment deltas. It does not block traffic or promise real-time enforcement.
- **Actual-expiry reminders:** optional 6/24/48-hour window for the current active promo's actual expiry. No-expiry, inactive, deleted and expired contexts do not generate a reminder. Current events are consumed on settings changes/restore; future windows may notify once. Promo mutations clear obsolete posted expiry notices; worker evaluation also cancels them when no longer eligible. Notification permission and channel restrictions are honored.
- **CSV Data Diary:** History > Data Diary and mobile app usage. User-selected dates, limited to 90 recent calendar days and clipped to the tracking/expiry window. Exports a reviewed immutable snapshot through Android's picker. Exact bytes, source, scope, timezone, UTC periods, observation time and complete-calendar/partial labels. No offset/correction traffic, savings claims or unsupported permanence claims. Formula-leading names and CSV quotes are escaped.
- **Mobile app usage:** query the same measured interval, restricted to mobile network traffic. Group by UID, identify visible shared groups, and show unresolved UID/system labels honestly. Only launcher-intent package visibility was added; no QUERY_ALL_PACKAGES. Device totals, UID totals and discrepancies remain separate. On the test phone, normal labels including TikTok, Facebook, Shopee and Instagram resolved after the targeted visibility fix.

## Data and upgrade safety

Room schema 4 upgrades additively to 6; existing promo rows/checkpoints remain. Schema 5 is the abandoned Preview lineage and is not targeted. No destructive fallback, data clear, personal-app uninstall or signing-key change. Correction records cascade only when their owning promo is explicitly deleted or replaced through reviewed restore.

Configuration backup format 2 adds correction history/anchors and advisory settings. Format-1 files remain readable. Older apps reject format 2. Restore retains its required fresh safety export and durable recovery journal; restored notification baselines start quiet. Files remain unencrypted. Android traffic history, forecast caches, work queues and widget IDs remain outside configuration backups.

The installed certificate remains SHA-256 ebaa64b8c9aedc7a24f2d1ca9f251fc57030d52b8a77cead2d5013504838bf9b, matching the previous installation. The local Android Debug signing lineage is retained for continuity; this is not a public-store release.

## Verification evidence

- 157 JVM regression tests passed; zero failures/errors.
- 29 device tests passed, with a placed Verification widget and zero skipped tests.
- Release lint: zero errors, 46 warnings, one hint. Existing warnings are not represented as resolved.
- A schema-4 copy of the saved-data snapshot opened successfully on schema 6. Legacy migration chains, exact values, atomic edits and stale-checkpoint tests passed.
- New tests cover carrier comparison/apply/duplicate rejection/undo, checkpoint and rate separation, correction backup history, revision priming, daily rollover/DST, expiry conditions, blocked delivery, duplicate suppression, restore priming, actual posted reminder cancellation, diary windows/escaping/bytes and real mobile UID queries.
- Native phone checks: comparison without writes, confirmation and undo; saving an advisory budget without enabling notifications; Android file-picker CSV export; CSV parsing verified seven measured rows, 13 columns and exact receive/transmit/total arithmetic; per-app labels and independent totals; new dialogs at 1.3× system font scale with accessible scrolling/actions. Original font scale restored.
- Full build: testVerificationUnitTest assembleVerification assembleVerificationAndroidTest assembleRelease lintRelease --offline, JDK 17. Signature and release package/version verified.
- All mutating tests ran only against com.loadpredictor.releasecheck.audit. The actual app was not used for fixtures or instrumentation.

## Personal installation and cleanup

Installed 1.2.0 in place after the isolated release checks. Fresh user-accessible backups are in phone Downloads: DATRACK-before-1.2.0.json and DATRACK-1.2.0-backup.json. The post-update format-2 export matched every pre-update promo field (including exact IDs, offset, allowance, dates, SIM, active selection and milestones) and original alert settings. Correction history remains empty and daily budget/reminder settings remain off. App UID and original first-install date remain. Original launcher widget ID 59 retained its provider and 4×1 / 334×116 dp options; refresh and tap-to-open passed. Temporary Verification packages/widget and the test CSV were removed. Original font scale was restored; existing personal backups remain.

## Milestones

- 011ffa3 — audited carrier corrections and non-destructive migration.
- 5051f80 — daily advisory budgets and actual-expiry reminders.
- fc563e2 — factual CSV diary and mobile UID usage breakdown.
- Final obsolete-reminder cancellation and release evidence: accompanying verification commit.

## Remaining limits

Device-wide mobile measurements remain estimates, may lag/revise, and do not isolate SIM billing, specialized pools or carrier zero rating. Manual carrier corrections do not make future device measurements carrier authoritative. App attribution may omit labels or disagree with device totals. Daily/reminder checks use flexible background work, not exact alarms or an always-on service.

72-hour/seven-day background observation, prolonged Doze/battery restriction, reboot and other-device/OEM coverage remain pending. Automated tests and this session's native checks do not certify those elapsed conditions. No scheduled monitor was created, repository changes were not pushed and no public release was published.
