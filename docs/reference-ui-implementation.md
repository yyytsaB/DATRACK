# DATRACK Preview — supplied UI reference implementation

## Scope and authority
The user supplied `C:/Users/user/Downloads/ui` as the visual authority and asked for the two additional feature screenshots to use that same style. The React screens and stylesheet define the appearance. The Android scaffold's placeholder view models and empty font files were not used for live behavior. The reference folder was left unchanged.

## Delivered
- Reference charcoal/mint colors, bundled Outfit and DM Mono, small headers, forecast-led dashboard, radial balance, three stat tiles, mini chart, rounded cards and line-marked five-tab navigation.
- Promos: SIM filter, active summary, saved contexts, past load history, details, edit, confirmed deletion and add form. Optional carrier and exact peso price persist through Room version 5; existing records migrate with null values and remain intact.
- History: all date ranges, scrollable selectable bars, selected-day values, daily breakdown, completed-day weekly comparison and real app-attributed mobile usage with icons, shares and an expandable list. Small positive transfers display `< 1 MB`.
- Alerts: existing permission recovery, threshold preferences and Android settings remain connected.
- Widgets: compact/wide live previews, separate launcher pin requests, actual light/dark system colors, balance, pace, reading age, refresh and tap-to-open. Launcher minimum dimensions account for Samsung's grid rounding. Existing hourly background scheduling is retained.
- Larger text wraps and screens/forms scroll. Stat tiles share a height, while very large type switches suitable groups to columns. Empty, error, permission, learning, expired and depleted states are connected to real domain states.

## Truth and platform constraints
No fabricated rankings, prices, usage history or content-specific explanation is shown. The weekly insight needs three completed weekdays and two completed weekend days. Android supplies mobile statistics across the device; SIM labels identify promo context, not isolated per-SIM billing. Shared UIDs are grouped. App percentages use attributed mobile data in the selected period and exclude Wi-Fi and the manual already-used offset. Past promo balances are saved snapshots.

The app follows the supplied dark design. Home-screen widgets follow the phone's system theme and use Android widget typography; launcher grid sizing and resize behavior vary by device. These are native platform adaptations, not a claim of pixel-identical rendering of a browser image.

## Verification
- Debug app and Android test APK built successfully.
- 142 JVM regression tests passed, zero failures/errors.
- Android lint: zero errors, 70 warnings and one hint. The project is not warning-free.
- Ten targeted device tests passed: database migration chains, active-promo invariants, real mobile UID statistics, and widget state/refresh scheduling with placed Preview widgets.
- Native Samsung Galaxy A22 5G / Android 13 checks: Home, Promos, History, Alerts, Widgets and the scrollable promo form at standard text; Home, Promos, History and Widgets at 1.3 font scale. Original text setting restored.
- Both launcher pin actions were accepted as 2 × 2 compact and 4 × 2 wide presets. Dedicated refresh and balance tap-to-open were checked. Optional Smart / ₱99.50 metadata saved through the form and appeared in promo details; fields were restored to null afterward. The active promo, original offset and start timestamp were preserved.

## Data boundary
Only `com.loadpredictor.audit` (DATRACK Preview) was updated. Original `com.loadpredictor` remains installed separately with the active `md+ 499` record. Existing Preview fixtures remain saved. No app data was cleared. No files in Downloads/ui were edited. No changes were pushed.

## Local milestones
- `58cf698`: load-detail persistence, non-destructive migration and real per-app mobile usage.
- `cd12ee9`: supplied reference port, typography, native screens, widgets, regression test and design documentation.

Design work used the local Impeccable skill's Android, Operate, reference and bounded review guidance. Its helper executable was unavailable; the user's supplied reference resolves visual direction directly.
