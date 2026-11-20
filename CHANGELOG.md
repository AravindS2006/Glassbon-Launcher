# Changelog

All notable changes to **Glassbon Launcher** are documented in this file.

## [Unreleased]

### Added
- **Auto-Detected Lock Screen App & Adaptive Preferences Button**: Automatically links installed OEM or third-party Lock Screen apps (`LockScreenAppHelper`) on startup, foreground resume, and after package install/uninstall (`ACTION_PACKAGE_ADDED`/`REMOVED`). When no Lock app is present, the bottom-right Power icon switches to a **Settings** icon that opens **Glassbon Preferences** directly, while long-pressing always opens **Quick Actions**.
- **Default Home Switcher**: When Glassbon is already the active default launcher, **Quick Actions → Change Home App** and **Preferences → Change Default Home App** open Android's system Home app chooser (with OEM fallbacks) so you can switch launchers anytime.
- **Tour-Skip Discovery Callout**: Added a self-dismissing frosted `"Hold ⏻ for Quick Actions"` hint pill above the bottom-right button until Quick Actions is opened.
- **Smooth Alphabet Spotlight Glide & Zero-Battery Performance Modes**: Selecting or sliding along the alphabet glides smoothly at 60–120 FPS with GPU depth lift/recede while settling a single drawer-cached bounded `BlurEffect` on neighboring sections; **Low Power** mode eliminates pre-blur decode, icon prewarming, animations, and shrinks cache allocations for minimal battery/CPU footprint.
- **Single Shared Dual-Edge Alphabet Rail**: Replaced duplicate left/right 28-letter rails with a single shared `AlphabetRailController` plus a 1-node `LeftAlphabetEdgeSensor`, letting both screen edges control the same alphabetic drawer list with zero duplicate UI cost.
- **Ranked, Accent-Insensitive App Search**: `AppIndexHelper.filterApps` now orders results by relevance (exact → prefix → word prefix → initials → substring → package), ignores accents (`cafe` → *Café*), supports multi-word queries, and skips reverse-domain package prefixes so `co` no longer matches every app. Enter launches the best match.
- **Live Media Capsule Updates**: `MediaRepository` registers a permission-free `AudioPlaybackCallback` (Android 8+) while the launcher is visible, so the Home media capsule reacts to playback started or stopped outside Glassbon, with a settle guard that prevents flicker right after pausing.
- **Preferences Back-Stack**: Wallpaper Studio, Favorites Manager, and Hidden Apps opened from Preferences now return to Preferences on Back instead of jumping to Home.
- **CI Auto-Changelog Generation**: Added `.github/scripts/generate_changelog.py` to `.github/workflows/android-release.yml` so GitHub Releases and empty changelog entries are automatically populated from `[Unreleased]`, conventional commits, or fallback highlights whenever manual notes are omitted.

### Fixed
- **False "App Unavailable" on Double-Tap**: A second tap within the 350 ms launch debounce was reported as a failed launch, showing "*App* is unavailable" and re-opening the Lock app picker. Debounced taps are now treated as success, and the debounce only arms after a successful launch.
- **Hidden Favorites Leaking onto Home**: Hiding a favorited app now also removes it from the Home dock and the drawer's Favorites section.
- **Lost Favorite/Hide Updates on Rapid Taps**: Favorite, hide, and reorder actions are now atomic DataStore transactions instead of read-modify-write on a possibly stale UI snapshot; reordering steps over hidden or uninstalled favorites so every move is visible.
- **Missing Third-Party App Icons**: Fixed `AppIconCache` drawable cloning so third-party adaptive/vector drawables retain their own package resources, added per-candidate fault isolation, validated Android 13+ monochrome layers against blank or solid-fill masks, and added `loadUnbadgedIcon` + `getResourcesForApplication` density fallbacks.
- **Vertical Text Wrapping on Narrow/Compact Resolutions**: Enforced single-line horizontal text and responsive scaling (`BoxWithConstraints` / `FlowRow` / `maxLines = 1, softWrap = false`) across Clock/Date, onboarding/hint pills, Quick Actions, Walkthrough cards, dialogs, and Settings headers.
- **Quick Actions & Guided Tour Refinements**: Removed **Screen Lock Settings** from Quick Actions (still accessible inside Preferences → System Integration) and temporarily removed the **Music Widget** step from the on-demand 5-step Guided Feature Tour.

## [1.0.0] - 2026-11-17

### Added
- **Liquid Frosted-Glass Home & App Library**: Monochrome squircle icon treatment (`IconSize.COMPACT` default), unified left-aligned `Plus Jakarta Sans` / `Instrument Serif` clock + calendar pill, and `RGB_565` pre-blurred drawer backdrop.
- **Dual-Edge Gaussian Alphabet Wave**: Visible curved A–Z index bar on the right plus a zero-idle-cost hidden curved wave trigger along the left edge (`AlphabetRailSide.LEFT`).
- **Adaptive Multi-Zone Wallpaper Luminance**: Off-main-thread `12×12` luminance sampling adapts top clock/status polarity, side edge scrims, and Home favorite backing automatically.
- **Frosted Pop-Up App Preview**: Long-press any app for a specular frosted glass preview card with inline status badge (`Home #N`, `Hidden`, `App`) and quick management actions.
- **Smart Dynamic Media Capsule**: Automatically appears beneath Clock/Date only during active playback or brief pause, reclaiming vertical Home space when idle.
- **6-Step Interactive Guided Tour**: Swipeable frosted-glass walkthrough (`WalkthroughOverlay`) with spatial edge/corner anchor beacons, accessible from first-launch onboarding, Quick Actions, and Settings.
- **Play-Protect Zero-Permission Architecture & CI/CD**: Zero manifest `<uses-permission>` declarations (no Accessibility, Device Admin, Notification Listener, `WRITE_SETTINGS`, `REQUEST_DELETE_PACKAGES`, `VIBRATE`, or `INTERNET`), Lock Screen App linking (`LockScreenAppHelper`), sculpted zero-permission `MistHaptics` tactile profiles (`Subtle`, `Balanced`, `Crisp`), Home Back-navigation lock prevention, dynamic GitHub tag versioning, custom sculpted crystal launcher icon, and GitHub Actions unit tests + R8-minified release APK workflows.
