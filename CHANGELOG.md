# Changelog

All notable changes to **Glassbon Launcher** are documented in this file.

## [Unreleased]

### Added
- **Auto-Detected Lock Screen App & Adaptive Preferences Button**: Automatically links installed OEM or third-party Lock Screen apps (`LockScreenAppHelper`) on startup, foreground resume, and after package install/uninstall (`ACTION_PACKAGE_ADDED`/`REMOVED`). When no Lock app is present, the bottom-right Power icon switches to a **Settings** icon that opens **Glassbon Preferences** directly.
- **Tour-Skip Discovery Callout**: Added an unmistakable long-press tip to Tour Step 1, onboarding copy, and a self-dismissing frosted `"Hold ⏻ for Preferences"` hint pill above the Power button until Quick Actions or Preferences are opened.
- **Alphabet Focus Blur & Zero-Battery Performance Modes**: Centered drawer jumps blur non-selected sections via a single drawer-cached bounded `BlurEffect` that automatically bypasses during active scroll/scrub; **Low Power** mode eliminates pre-blur decode, icon prewarming, animations, and shrinks cache allocations for minimal battery/CPU footprint.
- **CI Auto-Changelog Generation**: Added `.github/scripts/generate_changelog.py` to `.github/workflows/android-release.yml` so GitHub Releases and empty changelog entries are automatically populated from `[Unreleased]`, conventional commits, or fallback highlights whenever manual notes are omitted.

## [1.0.0] - 2026-11-17

### Added
- **Liquid Frosted-Glass Home & App Library**: Monochrome squircle icon treatment (`IconSize.COMPACT` default), unified left-aligned `Plus Jakarta Sans` / `Instrument Serif` clock + calendar pill, and `RGB_565` pre-blurred drawer backdrop.
- **Dual-Edge Gaussian Alphabet Wave**: Visible curved A–Z index bar on the right plus a zero-idle-cost hidden curved wave trigger along the left edge (`AlphabetRailSide.LEFT`).
- **Adaptive Multi-Zone Wallpaper Luminance**: Off-main-thread `12×12` luminance sampling adapts top clock/status polarity, side edge scrims, and Home favorite backing automatically.
- **Frosted Pop-Up App Preview**: Long-press any app for a specular frosted glass preview card with inline status badge (`Home #N`, `Hidden`, `App`) and quick management actions.
- **Smart Dynamic Media Capsule**: Automatically appears beneath Clock/Date only during active playback or brief pause, reclaiming vertical Home space when idle.
- **6-Step Interactive Guided Tour**: Swipeable frosted-glass walkthrough (`WalkthroughOverlay`) with spatial edge/corner anchor beacons, accessible from first-launch onboarding, Quick Actions, and Settings.
- **Play-Protect Zero-Permission Architecture & CI/CD**: Zero manifest `<uses-permission>` declarations (no Accessibility, Device Admin, Notification Listener, `WRITE_SETTINGS`, `REQUEST_DELETE_PACKAGES`, `VIBRATE`, or `INTERNET`), Lock Screen App linking (`LockScreenAppHelper`), sculpted zero-permission `MistHaptics` tactile profiles (`Subtle`, `Balanced`, `Crisp`), Home Back-navigation lock prevention, dynamic GitHub tag versioning, custom sculpted crystal launcher icon, and GitHub Actions unit tests + R8-minified release APK workflows.
