# Load Predictor — Prepaid Data Burn-Rate Forecaster

> "At current pace, your GigaSurf 99 will run out Thursday at 3:15 PM."

A native Android app that answers the one question prepaid mobile data users in
the Philippines actually have, and that no carrier app currently answers:
**not "how much data do I have left," but "will it last."**

Telco apps like the Smart app and GlobeOne already show a live balance
dashboard. None of them forecast it. This project fills that specific,
narrow gap.

<!-- 📸 Add real device screenshots here once exported from the device -->
<!-- Suggested captures: Home dashboard (On-Track state), History screen (Lifetime view), Widget on home screen -->
<!-- ![home dashboard](docs/screenshot-home.png) -->
<!-- ![history screen](docs/screenshot-history.png) -->
<!-- ![home screen widget](docs/screenshot-widget.png) -->

---

## Why this exists

Prepaid data plans in the Philippines are the default, not the exception.
Running out mid-week, before the next reload, is a routine and mildly
stressful experience — and the tools available today only tell you where
you stand right now, not where you're headed.

This app treats "when will I run out" as a forecasting problem, not a
dashboard problem: it measures actual on-device mobile data usage and
projects forward against a user-entered promo (allowance + validity window),
instead of just displaying a static number.

## The core technical constraint (and why it shaped everything)

There is no public telco API for prepaid promo balance in the Philippines.
This app never pretends otherwise — it doesn't scrape, doesn't reverse-engineer
carrier endpoints, and doesn't request SMS permissions (which Google Play
reserves for default SMS handlers anyway). Instead, it measures real
device-level mobile data usage via Android's `NetworkStatsManager` and
forecasts against a manually entered promo. Everything about the stack and
architecture follows from that one decision.

## Tech Stack

| Layer | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose (Material 3) |
| Navigation | androidx.navigation3 — 5-tab bottom nav (Home, Promos, History, Alerts, Widgets) |
| Local storage | Room (schema v4) + DataStore Preferences |
| Background work | WorkManager |
| Home screen widget | Jetpack Glance |
| Backend | None — fully on-device by design |

No cross-platform framework (Flutter/React Native) — the app leans directly
on native-only Android APIs (`NetworkStatsManager`, and eventually
`NotificationListenerService`) that a cross-platform layer would only wrap,
not simplify.

## Architecture

```
app/src/main/java/com/loadpredictor/
├── data/
│   ├── local/         # Room DB, DAOs, Entities, NotificationPreferencesDataSource
│   ├── notification/  # NotificationHelper (local threshold alerts)
│   ├── repository/    # PromoRepositoryImpl, UsageRepositoryImpl
│   └── stats/         # NetworkStatsDataSource, UsageAccessHelper
├── domain/
│   ├── engine/        # BurnRateEngine — pure Kotlin, zero Android deps, fully unit-testable
│   ├── model/         # Promo, BurnForecast, BurnPace, UsageBucket, AlertPreferences, …
│   ├── repository/    # Repository interfaces
│   ├── time/          # TimeProvider — injectable clock for deterministic testing
│   └── usecase/       # GetActiveBurnForecastUseCase, GetDailyUsageBreakdownUseCase, …
├── presentation/
│   ├── alerts/        # Alerts settings screen (AlertsScreen, AlertsViewModel)
│   ├── common/        # Shared Compose components, design tokens, formatters
│   ├── dashboard/     # RadialPaceRing, DailyUsageChartCard, DashboardStatChips, …
│   ├── history/       # HistoryScreen — InteractiveUsageChart, 7D/30D/Lifetime time-range pills
│   ├── navigation/    # LoadPredictorBottomBar (5-tab), NavDestination enum
│   ├── promo/         # PromoEntryDialog, PromoManagementScreen, PromoPresets
│   ├── theme/         # Material 3 theme tokens
│   └── widget/        # Glance widget layouts (2×2/4×2), WidgetSyncHelper, WidgetReceiver
└── worker/            # UsageSyncWorker (periodic sync + threshold eval + widget update)
```

The forecast engine lives in `domain/engine/` as plain Kotlin with no Android
framework dependency, specifically so the core math — burn rate, depletion
estimate, edge-case handling — can be verified with standard JVM unit tests,
independent of the UI or platform layer.

## Known Platform Limitations (documented on purpose)

Being upfront about these is part of the point of this project — a forecast
tool is only trustworthy if it's honest about what it can't measure precisely:

- **No true per-SIM usage attribution.** Android 10+ restricts `subscriberId`
  access to carrier-privileged apps. Dual-SIM support here means a manual
  toggle between two tracked promo contexts, not simultaneous per-SIM
  measurement.
- **App-specific "unli" promo buckets aren't distinguishable.** If a promo
  gives free data to specific apps (common in Smart Giga bundles), device-wide
  usage tracking can't separate that from general data — the forecast is an
  estimate, and the UI says so.
- **Mid-cycle / starting-balance offset.** For users who install the app
  partway through an active promo (e.g. Magic Data with partial balance
  remaining), the app allows entering current remaining balance at setup.
  The engine calculates the historical consumed offset and anchors live
  velocity tracking strictly to device measurements from that point forward.

- **Emulator limitation.** `NetworkStatsManager` returns no meaningful mobile
  usage data on emulators. All real-usage testing must be done on a physical
  device with an active SIM. Verified on Samsung Galaxy A22 5G (SM-A226B,
  Android 13) against an active Smart Communications prepaid SIM.

---

## Current Status

Built and verified on physical hardware (SM-A226B, Android 13) against an
active Smart Communications prepaid SIM. Test suite: **127 JVM unit tests**
across 17 test classes, **12 on-device instrumented tests**.

### v1 MVP & Enhancements ✅ (Complete)

The core feature set and usability enhancements:

| Feature | Status |
|---|---|
| Manual promo configuration (GigaSurf, Magic Data, Power All, Smart Bro presets) | ✅ Done |
| In-place promo editing (name, allowance, validity, SIM slot with clean baseline recalibration) | ✅ Done |
| Remaining-balance offset input (for mid-cycle starts) | ✅ Done |
| Device-level mobile data measurement via `NetworkStatsManager` (mobile-only, WiFi excluded) | ✅ Done |
| Usage Access permission gate with explicit empty-state UI and deep-link to settings | ✅ Done |
| Burn-rate forecast engine (`BurnRateEngine`) — pure Kotlin, zero Android deps | ✅ Done |
| Adaptive Time-Weighted Exponential Moving Average (EMA) rate smoothing | ✅ Done |
| Plain-language forecast sentence ("At current pace…") | ✅ Done |
| Pace classification: `BURNING_FAST` / `ON_TRACK` / `CONSERVATIVE` / `DEPLETED` / `INSUFFICIENT_DATA` | ✅ Done |
| Burn Status Index (data-consumed % vs. time-elapsed %) | ✅ Done |
| Stabilization window: `INSUFFICIENT_DATA` for first hour AND < 10 MB used | ✅ Done |
| Manual dual-SIM toggle (SIM 1 / SIM 2 active promo context switching) | ✅ Done |
| Local threshold notifications at 50% / 80% / 90% and premature-depletion warnings | ✅ Done |
| Anti-re-fire suppression (notifications don't repeat once dismissed) | ✅ Done |
| Jetpack Glance home screen widget — 2×2 compact and 4×2 wide layouts | ✅ Done |
| Widget: remaining data, time-to-expiry, pace status pill, SIM badge | ✅ Done |
| Widget manual refresh action (WorkManager-triggered) | ✅ Done |
| Widget placement lifecycle sync | ✅ Done |
| WorkManager periodic background sync (1–2 hr cadence; threshold evaluation on same job) | ✅ Done |
| Weekday vs. Weekend usage pattern awareness (isolated read-only History tab insight callout) | ✅ Done |

### Visual & Structural Redesign ✅ (Complete)

A full post-MVP redesign replacing the single-screen layout with a proper
multi-screen navigation architecture and polished UI:

| Feature | Status |
|---|---|
| 5-tab bottom navigation: **Home · Promos · History · Alerts · Widgets** | ✅ Done |
| `NavDestination` enum + `LoadPredictorBottomBar` composable | ✅ Done |
| Radial pace ring gauge (animated arc, dashboard hero element) | ✅ Done |
| `DashboardStatChips` — compact remaining/used/pace summary row | ✅ Done |
| Interactive History analytics screen with `InteractiveUsageChart` | ✅ Done |
| **7D / 30D / Lifetime** time-range toggle (Lifetime = 90-day policy cap) | ✅ Done |
| `HistoryMetricsRow` — peak day, average daily, and total consumed | ✅ Done |
| `UsagePatternBanner` — clean callout for detected weekday/weekend variance | ✅ Done |
| Configurable Alerts screen (per-threshold opt-in/opt-out, `AlertsViewModel`) | ✅ Done |
| Widgets Gallery screen with home-screen pin request flow (`WidgetsScreen`) | ✅ Done |

---

## Roadmap

### v2 — Planned, not started

Highest-priority features that meaningfully extend the app beyond MVP scope,
ordered by impact and implementation viability.

#### High Priority

- [x] **Confidence interval display** — displays uncertainty as a `± N days` sub-label
      inside the "At this pace" chip on the Dashboard, derived from the sample standard
      deviation (±1σ) of up to 7 completed daily usage buckets. Requires ≥ 5 completed
      calendar days of history, and is suppressed if σ ≥ μ (high noise / bursty traffic)
      or if the half-spread rounds to ≤ 0 days. Late bound is capped at promo expiration.
- [x] **Weekday vs. weekend usage pattern awareness** — standalone read-only
      pattern callout in the History tab ("You use ~2.9x more data on weekdays...")
      computed strictly from completed daily buckets (excluding today's partial
      traffic) with sample thresholds (>= 3 weekdays, >= 2 weekends).
- [x] **7-day daily mean forecast anchor** — `GetActiveBurnForecastUseCase` now derives the
      user-facing depletion projection (`estimatedDepletionTimestamp`, "At this pace" chip) from
      the rolling 7-day mean of completed daily buckets when ≥ 3 completed calendar days of 
      history exist. Falls back to the Adaptive EMA when fewer than 3 completed days are 
      available (e.g. fresh promos). The EMA `lastActiveBurnRate` persisted to the DB is 
      intentionally preserved as the sync delta anchor; only the user-facing projection rate 
      is overridden. This makes the "Daily avg" chip and the "At this pace" depletion date 
      mathematically consistent by construction.
- [x] **Promo editing workflow** — promos can be edited in-place with instant
      recalibration and preserved delta tracking baselines.
- [ ] **Projected depletion mini-chart on the home screen widget** — render a
      small curve showing the projected usage trajectory alongside remaining data
      (🟢 will last / 🟡 cutting it close / 🔴 will run out early). The Glance
      widget pipeline and data are already in place; this is a layout and
      data-shape addition.
- [ ] **Multi-carrier presets beyond Smart** — Globe and DITO promo templates
      and balance formatting. The promo data model is already carrier-agnostic;
      this is primarily a `PromoPresets.kt` and UI labels change.

#### Medium Priority

- [ ] **Budget mode / daily data cap** — let users set a daily GB/MB ceiling
      and show real-time progress toward it on the dashboard. WorkManager already
      runs on a 1–2 hr cadence; a daily cap threshold check can ride the same
      job. Push notification: "You're 40% through today's budget and it's only
      11 AM."
- [ ] **Dual-SIM / multi-promo combined timeline** — show both active SIM promo
      contexts simultaneously with a "covered until X" combined view, rather than
      requiring a manual toggle.
- [ ] **Notification-listener-based auto-balance detection**
      (`NotificationListenerService`) — parse telco balance notification alerts
      to update remaining allowance automatically, eliminating manual-entry
      friction. `READ_SMS` is a non-starter under Google Play policy;
      `NotificationListenerService` is the viable path.
- [ ] **Exportable usage report ("Data Diary")** — weekly summary shareable as
      a screenshot or export: "You used 1.2 GB this week. At this rate you'd
      save ₱150/month by switching to…" `HistoryScreen` and
      `InteractiveUsageChart` already render the necessary data; this is
      primarily a share/export layer.

#### Lower Priority / v3

- [ ] **Promo recommendation engine** — a local, offline database of PH telco
      promos that suggests "Based on your usage pattern, GOMO 299 would be 30%
      cheaper than GigaSurf 99 and would actually last the full period." Requires
      curating and maintaining a promo database — valuable, but deferred until
      the core loop is validated with real users.
- [ ] **Per-app data breakdown** — show how much each app contributes to the
      total burn rate ("Instagram used 800 MB in the background this week").
      `PACKAGE_USAGE_STATS`, which the app already holds, gives access to
      per-package network stats; this is a query and presentation change. UI
      messaging must clarify this reflects all-network stats, not mobile-only.
- [ ] **Smart reload reminder** — a notification triggered by the predicted
      empty-date, telling the user to reload before they run out.
- [ ] **Community promo database** — crowd-sourced, user-submitted promo
      listings. Requires a backend (out of on-device scope) and ongoing
      moderation.
- [ ] **Play Store deployment** and potential monetization/premium tier.

---

## Engineering Notes — Lessons from Real-Device Testing

### The burn-rate stability bug & Adaptive EMA

The most significant post-MVP engineering problem surfaced only after running the
app for several hours on a real device with mixed usage patterns: the depletion
projection would drift dramatically during idle periods or when the phone was on
WiFi for an extended stretch, then suddenly snap to a completely different
estimate the next time it was opened.

**Root cause (found via multi-hour on-device observation):** The initial engine
calculated burn rate as a single cumulative figure:

```
R_burn = total_data_used_since_start / total_time_since_start
```

This looks correct in theory but collapses in real use. If a user burns 2 GB in
the first two hours, then spends 10 hours on WiFi doing nothing, the cumulative
rate keeps dropping toward zero over those idle hours — and the depletion
estimate keeps stretching further into the future, even though usage behaviour
has not changed at all. The engine was silently averaging idle time into the
velocity as if the user had actually slowed down.

**Fix (Room schema v2 → v3 → v4):** Three new columns were added to the `promos`
table via a non-destructive `ALTER TABLE` migration (`MIGRATION_2_3` in
`AppDatabase`):

- `last_active_burn_rate` — the most recently computed reliable velocity;
  persisted across app restarts and WorkManager sync cycles.
- `last_sync_data_used_bytes` — usage snapshot at last sync checkpoint.
- `last_sync_timestamp` — timestamp of last sync checkpoint.

The engine now computes a **delta-based rate** between the current observation
and the frozen last-sync checkpoint, smoothed using an **Adaptive Time-Weighted EMA**:

```
R_active = delta_bytes / delta_time   (only if delta >= 1 MB AND delta_time >= 5 min)
alpha = (delta_time / 4_hours).coerceIn(0.15, 0.80)
R_smoothed = alpha * R_active + (1 - alpha) * R_baseline
```

If neither threshold clears — meaning usage since last check was just background
noise or the phone was idle — the engine preserves the previously frozen
`last_active_burn_rate` instead of recalculating.

Additionally, `MIGRATION_3_4` resets legacy `last_active_burn_rate` values to `NULL`
so that promos created before the EMA fix immediately recalibrate against ground-truth
historical pace on first sync.

### The idle-then-spike problem & 7-day daily mean anchor

A secondary forecast accuracy issue surfaced after an extended period of low usage followed 
by a single high-usage session ("extreme mean problem"): the Adaptive EMA, despite its 
time-weighting, still adapts partially to a spike day, inflating `lastActiveBurnRate` 
stored in the DB. Subsequent idle days do not self-correct the EMA if `lastSyncTimestamp` 
is stale — the stored inflated rate simply persists, producing a much earlier projected 
depletion date than the user's actual recent daily usage would suggest.

**Two-layer fix:**

1. **Spike Guard (`SPIKE_GUARD_MAX_RATE_MULTIPLIER = 4.0`)** in `BurnRateEngine` — clamps 
   the incoming `freshInstantaneousRate` to at most 4× the stored `lastActiveBurnRate` 
   before EMA blending, preventing future extreme spikes from pulling the EMA out of 
   proportion.

2. **7-day daily mean anchor** in `GetActiveBurnForecastUseCase` — when ≥ 3 completed 
   calendar days of daily bucket history exist, the user-facing depletion projection is 
   derived from the rolling 7-day mean of those completed buckets (today's partial bucket 
   is always excluded), rather than the EMA. The EMA continues to govern sync state 
   persistence; the daily mean governs only what the user sees.

This keeps `BurnRateEngine` strictly pure (zero changes to the mathematical engine) while 
making the displayed "Daily avg" and "At this pace" figures consistent by construction. 
Verified live on SM-A226B: 15.3 GB remaining / 209 MB daily avg → "> 60 days" projected 
runway (Nov 12), matching the manual calculation of ~75 days within normal daily variance.

---

## Getting Started

```bash
git clone <repo-url>
cd load-predictor
# Open in Android Studio (Hedgehog or later) or Antigravity IDE
# Build & run on a physical device with a real mobile data SIM —
# NetworkStatsManager returns no meaningful data on emulators
```

**Minimum requirements:**
- Android 8.0 (API 26) minimum SDK
- Android 13 (API 33) tested / primary target
- Physical device with an active mobile data SIM required for meaningful usage readings

On first launch, the app will prompt for Usage Access
(`Settings.ACTION_USAGE_ACCESS_SETTINGS`) — this is required for the
forecast to function and is requested explicitly with context, not silently.

---

## License

MIT