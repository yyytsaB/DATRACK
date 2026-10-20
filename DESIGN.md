# DATRACK reference design system

## Visual authority
The user-supplied `C:/Users/user/Downloads/ui/src/App.tsx` and `src/index.css` are the visual authority. The React reference supersedes the earlier dark mint redesign. Its native Android scaffold contains placeholder data and empty font assets, so only the visual design is ported; existing production domain, database and worker logic remain authoritative.

## Palette and typography
Charcoal background #0D1117; layered #161B26 surfaces; mint #3DD6C8 actions; warm white and slate text. Outfit 400/500/600/700/800 for the app, DM Mono for compact numeric metadata. Fonts are bundled with their OFL licenses. Semantic green, blue, amber and red express pace and terminal states. The platform widget follows system light/dark theme and uses native widget typography.

## Layout and hierarchy
20dp app gutters, 12–16dp internal spacing, small 16sp screen titles, 16dp rounded surfaces. Home follows the source order: context and sync age, highlighted forecast, expiry, status chip, 200dp radial balance, three stat tiles, compact daily chart. Selected navigation uses a mint line above an outline icon and a text label. Forms and scroll containers expand for larger text; tap targets remain at least 48dp.

## Added surfaces
Promos includes an active summary, saved contexts and past load history; optional carrier and peso price are real stored fields. History includes an interactive range chart, selected-day details, a completed-day weekly insight and Android-attributed mobile usage by app. Added features use the supplied visual language rather than the screenshots' separate styling.

## Product truth
No invented usage, prices, dates, app rankings or content-specific insights. Insufficient samples show a learning state. Expired balance is unavailable. SIM labels identify a promo context; measurements cover device-total mobile data. Shared UIDs are grouped and percentages use the attributed mobile data in the selected period. Tiny positive readings show < 1 MB rather than 0 MB.

## Widget behavior
Compact and wide launcher presets use the same responsive live content. Both include a dedicated refresh action and a root tap to open Preview. The balance, status and reading age remain explicit. Background updates keep the existing hourly cadence. Android controls final grid dimensions, typography and system theme.

## Verification boundary
Native Samsung Galaxy A22 5G, Android 13, at standard and enlarged text. Only the isolated Preview package is installed; the original DATRACK app and its data are separate.
