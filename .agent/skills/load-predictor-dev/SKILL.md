---
name: load-predictor-dev
description: >-
  Architectural guidelines, technical constraints, and domain logic for the Philippine
  Prepaid Mobile Data Burn-Rate Predictor Android app (load-predictor-dev). Activates
  when developing, architecting, refactoring, or testing features in this repository.
---

# Philippine Prepaid Mobile Data Burn-Rate Predictor (`load-predictor-dev`)

This skill defines the technical architecture, strict constraints, domain logic, and scope discipline for **load-predictor-dev**, a native Android showcase application designed to track device mobile data usage against Philippine prepaid promos (e.g., Smart Communications GigaSurf, Giga Games, Magic Data, Smart Bro) and forecast remaining data lifespan.

---

## 1. Non-Negotiable Hard Constraints

### Tech Stack: 100% Native Android
- **Language**: Kotlin only (idiomatic modern Kotlin, coroutines, `Flow`).
- **UI Toolkit**: Jetpack Compose (Material 3).
  - `androidx.navigation3`: Type-safe composable navigation between promo, dashboard, and widget-related screens (added during scaffolding).
  - `kotlinx.serialization`: Type-safe route serialization for navigation destinations (added during scaffolding).
- **Local Persistence**: Room Database + DataStore Preferences.
- **Background Orchestration**: WorkManager (periodic usage polling & threshold alerts).
- **Home Screen Widget**: Jetpack Glance (Compose-based app widgets).
- **Strict Prohibition**: **NEVER suggest Flutter, React Native, or any cross-platform framework.** This app directly relies on low-level Android platform APIs (`NetworkStatsManager`, `TelephonyManager`, `AppOpsManager`, and future `NotificationListenerService`) that cross-platform abstraction layers unnecessarily complicate without adding value.

### Data Acquisition Strategy & Permissions
- **No Telco APIs Exist**: Philippine carriers (Smart, Globe, DITO) do not expose public developer APIs for prepaid promo balance queries.
- **Formula**: Device Data Burn Rate = `NetworkStatsManager` queries filtered strictly to `ConnectivityManager.TYPE_MOBILE` (excluding WiFi) combined with user-entered promo details (data allowance, start timestamp, validity window).
- **Allowed Permissions**:
  - `android.permission.PACKAGE_USAGE_STATS` (Special App Access: "Usage Access") via `AppOpsManager` to read device-level data metrics.
  - `android.permission.READ_PHONE_STATE` (for multi-SIM subscription identification, handled gracefully with runtime permission checks).
  - `android.permission.POST_NOTIFICATIONS` (Android 13+ for local burn threshold alerts).
- **Strictly Prohibited Approaches**:
  - **NO Telco Web Scraping / Reverse Engineered APIs**: Unstable, brittle, and violates carrier terms.
  - **NO `READ_SMS` / `RECEIVE_SMS` Permissions**: Google Play Store policy strictly reserves SMS permissions for default SMS handlers. Reject any suggestion to read balance SMS directly in v1.
  - **NO Network VPN / Packet Capture**: Overly intrusive for battery, privacy, and user trust.

> **Note on Dual-SIM & Platform Limitations**: True per-SIM usage attribution via `subscriberId` is restricted on Android 10+ for non-carrier-privileged apps (`TelephonyManager.getSubscriberId()` requires carrier privileges or system access). Dual-SIM support in v1 means tracking ONE active promo context at a time via manual user toggle (device-total mobile data measured against whichever promo the user has currently selected as active), NOT simultaneous parallel per-SIM measurement. Do not implement or suggest subscriberId-based SIM differentiation for usage stats queries.

> **Test Device & Carrier Verification Note**: The primary test device for this build uses the developer's Smart SIM. Real usage metrics during development must be validated against actual Smart Communications promo terms. Note that device-level `NetworkStatsManager` queries capture all device mobile traffic without distinguishing zero-rated or app-specific promo buckets (e.g., dedicated YouTube/TikTok allocations in Giga bundles vs. open-access data). If a tracked promo features unmetered or app-restricted buckets, flag this in UI messaging to ensure forecast transparency and accuracy honesty.

> **Multi-Bucket & Split Allowances Note**: For promos featuring split allowances (e.g., Smart Magic Data offerings with separate 5G-only and open-access/general pools), v1 tracks only the primary/larger general pool as the promo's allowance. Smaller specialized buckets (e.g., 5G-specific data allocations) are out of scope and not tracked. This is a deliberate scope decision, not a limitation to revisit for v1.

### V1 Scope Discipline & Feature Boundaries
This is a **portfolio showcase project** demonstrating clean architectural design, algorithmic clarity, and modern Android development practices. Keep the initial release lean and strictly within scope.

| In Scope (v1) | Out of Scope / Deferred to v2+ |
| :--- | :--- |
| Single carrier primary focus (**Smart Communications**) | Multi-carrier auto-detection rules & deferred telco bundles (Globe, DITO) |
| Manual promo configuration (GigaSurf, Magic Data, Power All, Smart Bro) | `NotificationListenerService` telco SMS auto-sync |
| Single primary allowance tracking per promo (general open-access pool) | Multi-bucket split pool tracking (e.g., 5G-specific bonus allowances, app-specific bundles) |
| Device-level mobile data tracking (excludes WiFi) | Cloud sync, Firebase, remote backends, User auth |
| Burn-rate forecast engine & plain-English ETA | In-App purchases, ads, monetization logic |
| Daily mobile usage breakdown graph | Telco USSD dialer automations / accessibility scrapers |
| Configurable local threshold notifications | Social sharing / community load tracking |
| Jetpack Glance home screen widget | Wear OS / Android Auto extensions |
| Basic Dual-SIM support (manual toggle between 2 promo contexts, device-total usage — not simultaneous per-SIM attribution) | Complex family/shared pool tracking |

> **Agent Enforcement Rule**: If a requested feature belongs in the "Out of Scope" list (e.g., backend sync, telco notification listener, monetization, full multi-carrier parsing, multi-bucket split tracking), explicitly remind the user that it exceeds v1 scope before writing any code.

---

## 2. Core Feature Specifications (v1)

### 1. Manual Promo Configuration
- **Model**: Promo Name (e.g., Smart GigaSurf 99, Magic Data 399, Power All 99), Total Data Allowance (MB/GB converted to Bytes internally), Promo Start Date/Time, Optional Expiration Date/Time (`Long?`, null for non-expiring promos like Smart Magic Data), Target SIM Slot (SIM 1 / SIM 2).
- **Validation**: Ensure start timestamp is not in the future, allowance is positive, and if an expiration timestamp is provided, it must be strictly after the start timestamp. A null expiration is valid and treated as "never expires / data-cap only".

### 2. Mobile Data Measurement Layer
- Query `NetworkStatsManager.querySummaryForDevice(ConnectivityManager.TYPE_MOBILE, subscriberId, startTime, endTime)`, passing an empty string for `subscriberId` to retrieve aggregate device-wide mobile data usage. Do not attempt to retrieve a real `subscriberId`/IMSI — this is restricted on Android 10+ for non-carrier-privileged apps, per the dual-SIM constraint note above.
- Do NOT use `SubscriptionManager` for per-SIM stats differentiation. Dual-SIM handling in v1 is manual-toggle only (user selects which promo context is active), not simultaneous per-SIM measurement — consistent with the dual-SIM note in section 1.
- Provide a dedicated UI guide directing users to `Settings.ACTION_USAGE_ACCESS_SETTINGS` when `PACKAGE_USAGE_STATS` is not granted.
- When `PACKAGE_USAGE_STATS` is not yet granted, the dashboard must show an explicit empty/placeholder state explaining why data is missing and a direct action button to `Settings.ACTION_USAGE_ACCESS_SETTINGS` — never show a blank chart, zero values, or a silent loading state that implies the forecast is just still calculating.

### 3. Burn-Rate Forecast Engine
- **Calculations**:
  - **Elapsed Time**: $T_{elapsed} = \text{currentTime} - \text{startTime}$
  - **Data Consumed**: $D_{used}$ (queried from `NetworkStatsManager` within $[startTime, currentTime]$)
  - **Data Remaining**: $D_{rem} = D_{total} - D_{used}$
  - **Average Burn Rate**: $R_{burn} = \frac{D_{used}}{T_{elapsed}}$ (Bytes per hour/minute)
  - **Estimated Depletion Time**: $T_{depletion} = \text{currentTime} + \frac{D_{rem}}{R_{burn}}$
  - **Time Validity (Conditional on Expiration)**:
    - If `expirationTime != null`: $T_{total} = \text{expirationTime} - \text{startTime}$, $T_{rem} = \text{expirationTime} - \text{currentTime}$.
    - **Burn Status Index**: Ratio of data consumed percentage to time elapsed percentage ($\frac{D_{used} / D_{total}}{T_{elapsed} / T_{total}}$). Applicable only when promo has an expiration window.
- **Plain-Language Output**:
  - *Expiring Promos*: *"At current pace, Smart GigaSurf 99 data will run out on Tuesday at 4:15 PM (18 hours before promo expires)."*
  - *Non-Expiring Promos (e.g. Smart Magic Data)*: *"At current pace, Smart Magic Data will run out on Tuesday at 4:15 PM."* (without expiration comparison).
  - *Depleted*: *"Depleted! Smart Power All promo has 0 MB remaining."*
- **Mathematical Safety**: Guard against division by zero (e.g., $T_{elapsed} \to 0$ right after activation or $R_{burn} = 0$). Provide graceful fallback estimates (e.g., linear baseline distribution over promo window when expiring, or baseline daily allocation for non-expiring promos).

### 4. Glance Home Screen Widget
- Compact and Medium Glance widget layouts showing:
  - Remaining Data in GB / MB.
  - Time remaining until promo expiration.
  - Visual pace status pill (e.g., "Burning Fast", "On Track", "Safe").
  - Quick refresh action triggering background `WorkManager` update.

### 5. Daily Usage Graph
- Compose Canvas / charting component rendering day-by-day mobile data consumption (e.g., last 7–30 days) within the active promo period.

### 6. Local Threshold Notifications & Background Polling
- Trigger local notifications via `WorkManager` when:
  - Usage crosses 50%, 80%, and 90% of total allocation.
  - Burn rate projects premature depletion before promo expiration by more than $X$ hours.
- **Periodic Polling & Battery Policy**: Periodic usage polling interval: 1–2 hours. Do not poll more frequently — this is a deliberate battery-life tradeoff. Threshold-check evaluations can run on the same cadence rather than a separate, more frequent job.

---

## 3. Architecture & Code Quality Standards

This showcase project must reflect clean, senior-level Android software engineering:

```
app/src/main/java/com/loadpredictor/
├── data/
│   ├── local/            # Room Database, DAOs, Entities, TypeConverters
│   ├── stats/            # NetworkStatsManager data source & UsageStats permission helper
│   └── repository/       # Repository implementations (PromoRepository, UsageRepository)
├── domain/
│   ├── model/            # Pure Kotlin domain models (Promo, BurnForecast, UsageBucket)
│   ├── repository/       # Repository interfaces (PromoRepository, UsageRepository)
│   ├── usecase/          # Single-responsibility UseCases (CalculateBurnRateUseCase, GetActivePromoUseCase)
│   └── engine/           # Pure mathematical forecast algorithms (fully unit-testable)
├── presentation/
│   ├── common/           # Shared Compose UI components, design tokens, formatters
│   ├── promo/            # Promo Entry & Management screens and ViewModels
│   ├── dashboard/        # Main Dashboard, burn gauges, usage chart, plain-language cards
│   └── widget/           # Jetpack Glance widget receivers, state definitions, and layouts
└── worker/               # WorkManager workers for periodic sync and threshold evaluations
```

### Key Engineering Rules:
1. **Separation of Concerns & Unit Testing**: The forecast engine (`domain/engine/`) must be 100% pure Kotlin with **zero Android framework dependencies**, making it instantly runnable via standard JVM unit tests.
   - **Required test cases for the forecast engine, at minimum**:
     - **Zero usage at promo start ($T_{elapsed} \to 0$)**: must not divide by zero, must return a sensible baseline estimate rather than crash or return infinity.
     - **Usage exactly at promo expiration boundary ($T_{elapsed} == T_{total}$)**.
     - **Data usage exceeding 100% of allowance ($D_{used} > D_{total}$)**: burn status and depletion messaging must handle this as "already depleted," not a negative remaining value.
     - **Burn rate of zero ($R_{burn} == 0$, e.g. no usage in the query window)**: depletion time must fall back gracefully instead of dividing by zero.
2. **Commented Algorithm Rationales**: Document domain math extensively. For example, explain weighted moving averages vs. cumulative burn rate, edge-case dampening (first 2 hours of a promo), and manual active promo indexing.
3. **Data Immutability & StateFlow**: Expose UI state via immutable data classes and `StateFlow` from `ViewModel` layers.
4. **Byte Precision**: Store all allowances and usage figures internally in raw **Bytes** (`Long`), formatting to MB/GB only in presentation mappers.
5. **Deterministic Time Abstraction**: The domain and forecast engine layers must avoid hardcoded `System.currentTimeMillis()` calls. Use an injectable time provider interface (`TimeProvider` / `Clock`) or explicit timestamp parameters in domain methods, engines, and use cases to ensure 100% deterministic mathematical unit testing across all scenarios.
