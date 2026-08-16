# Load Predictor — Prepaid Data Burn-Rate Forecaster

> "At current pace, your GigaSurf 99 will run out Thursday at 3:15 PM."

A native Android app that answers the one question prepaid mobile data users in
the Philippines actually have, and that no carrier app currently answers:
**not "how much data do I have left," but "will it last."**

Telco apps like the Smart app and GlobeOne already show a live balance
dashboard. None of them forecast it. This project fills that specific,
narrow gap.

<!-- 📸 Screenshot / demo GIF placeholder — add once the dashboard + widget are functional -->
<!-- ![dashboard screenshot](docs/screenshot-dashboard.png) -->
<!-- ![widget demo](docs/demo-widget.gif) -->

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
| Local storage | Room + DataStore |
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
├── data/          # Room DB, NetworkStatsManager source, repositories
├── domain/        # Pure Kotlin models, use cases, forecast engine
│                    (zero Android dependencies — fully unit-testable)
├── presentation/  # Compose screens, ViewModels, Glance widget
└── worker/        # WorkManager jobs — periodic usage sync, threshold alerts
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

## Roadmap

### v1 — MVP (in progress)
- [ ] Manual promo entry (name, allowance, validity, start date)
- [ ] Device-level mobile data tracking (mobile only, WiFi excluded)
- [ ] Burn-rate forecast engine with plain-language output
- [ ] Daily usage graph (last 7–30 days)
- [ ] Threshold-based local notifications (50% / 80% / 90%)
- [ ] Home screen widget (Glance) — remaining data + time, pace status
- [ ] Manual dual-SIM toggle between two promo contexts
- [ ] Forecast engine unit tests (zero-usage, boundary, over-100%, zero-burn-rate cases)

### v2 — deferred, not started
- [ ] Notification-listener-based auto balance detection
- [ ] Multi-carrier support beyond Smart (Globe, DITO)
- [ ] Weekday/weekend usage pattern awareness

### v3 — deferred, not started
- [ ] Smart reload reminders tied to predicted empty-date
- [ ] Premium tier / monetization (if converted from showcase to real launch)
- [ ] Play Store deployment

## Getting Started

```bash
git clone <repo-url>
cd load-predictor
# Open in Android Studio or Antigravity
# Build & run on a device with a real mobile data connection —
# usage stats are not meaningful on an emulator
```

On first launch, the app will prompt for Usage Access
(`Settings.ACTION_USAGE_ACCESS_SETTINGS`) — this is required for the
forecast to function and is requested explicitly with context, not silently.

## Status

Actively in development as a portfolio project. Built and tested against a
real Smart Communications prepaid SIM.

## License

MIT (or update to your preference)
