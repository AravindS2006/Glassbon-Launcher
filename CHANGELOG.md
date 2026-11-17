# Changelog

All notable changes to **Glassbon Launcher** are documented in this file.

## [1.0.0] - 2026-11-17

### Added
- **Liquid Frosted-Glass Home & App Library**: Monochrome squircle icon treatment (`IconSize.COMPACT` default), unified left-aligned `Plus Jakarta Sans` / `Instrument Serif` clock + calendar pill, and `RGB_565` pre-blurred drawer backdrop.
- **Dual-Edge Gaussian Alphabet Wave**: Visible curved A–Z index bar on the right plus a zero-idle-cost hidden curved wave trigger along the left edge (`AlphabetRailSide.LEFT`).
- **Adaptive Multi-Zone Wallpaper Luminance**: Off-main-thread `12×12` luminance sampling adapts top clock/status polarity, side edge scrims, and Home favorite backing automatically.
- **Frosted Pop-Up App Preview**: Long-press any app for a specular frosted glass preview card with inline status badge (`Home #N`, `Hidden`, `App`) and quick management actions.
- **Smart Dynamic Media Capsule**: Automatically appears beneath Clock/Date only during active playback or brief pause, reclaiming vertical Home space when idle.
- **6-Step Interactive Guided Tour**: Swipeable frosted-glass walkthrough (`WalkthroughOverlay`) with spatial edge/corner anchor beacons, accessible from first-launch onboarding, Quick Actions, and Settings.
- **Play-Protect Zero-Permission Architecture & CI/CD**: Zero sensitive permissions (no Accessibility, Device Admin, Notification Listener, `REQUEST_DELETE_PACKAGES`, or `INTERNET`), `AudioManager` media capsule, `#000000` OLED Calm Screen, custom sculpted crystal launcher icon, and GitHub Actions unit tests + R8-minified release APK workflows.
