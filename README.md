# Glassbon — Frosted Liquid-Glass Android Launcher

Glassbon is a minimalist Android launcher built with Kotlin and Jetpack Compose, featuring a frosted liquid-glass squircle icon system, curved Gaussian-arc alphabet scrubbing from both Home and Drawer, one-tap Accessibility screen lock, and live media controls with liquid-glass artwork transitions.

Repository: `https://github.com/AravindS2006/Glassbon-Launcher`

## Features

- **Aesthetic Frosted Liquid-Glass Home Screen**:
  - Edge-to-edge grayscale misty forest wallpaper (or user-selected photo via Android Photo Picker) with adaptive luminance contrast.
  - `Instrument Serif` + `Plus Jakarta Sans` typography: tapping the Clock opens your device Clock/Alarms app, and tapping the Date opens your Calendar app.
  - **Live Media Controls**: Automatically displays the currently playing or paused track beneath Clock/Date (via `NotificationListenerService` + `MediaSessionManager`) with rounded-square album art, liquid-glass directional wipe animation on track change, and Previous / Play-Pause / Next buttons.
  - **Zero-Dot Frosted Glass Icons**: Adaptive monochrome + dual-tone icon rendering and corner-sampled background suppression ensure every installed app receives a clean glass squircle without missing icons or status dots.
- **Curved Alphabet Access Bar (Home & Drawer)**:
  - Niagara-style curved Gaussian arc scrub bar with a floating frosted letter bubble (`★` Favorites + `A–Z` + `#`).
  - Accessible directly from the Home screen right edge (configurable in **Glassbon Preferences** as **Visible**, **Invisible Edge**, or **Off**) to jump smoothly into the App Library at any letter, alongside sleek sliding row animations.
- **One-Tap Screen Lock & Quick Actions**:
  - Bottom-right frosted glass Power button locks the phone screen on a single tap via Android's `AccessibilityService` (`GLOBAL_ACTION_LOCK_SCREEN`).
  - Long-pressing the Power button opens the frosted **Quick Actions** sheet (Lock Screen, App Library, Search, Wallpaper, Home Shortcuts, Preferences, Media Controls access, Default Home, Device Settings).
- **Privacy & Performance**:
  - Zero network permissions, zero analytics, and zero broad storage permissions (`PickVisualMedia` Photo Picker only).
  - Pre-blurred static drawer wallpaper cache and off-thread 96px icon LRU caching for smooth 60 FPS scrolling.

## Building & Testing Locally

```bash
gradle :app:assembleDebug
gradle :app:testDebugUnitTest
```

## GitHub Push & Automated APK Releases

This project includes `.github/workflows/android-release.yml`:
- Every push or pull request to `main` runs `:app:testDebugUnitTest` and uploads `Glassbon-debug-apk` as a workflow artifact.
- Pushing a version tag (`v*`) or triggering `workflow_dispatch` automatically builds the APK and attaches it to a new GitHub Release.

To push to your repository and trigger your first APK release:

```bash
git init
git add .
git commit -m "Initial Glassbon Launcher release"
git branch -M main
git remote add origin https://github.com/AravindS2006/Glassbon-Launcher.git
git push -u origin main

# Trigger automated GitHub Release with attached APK:
git tag v1.0.0
git push origin v1.0.0
```

*(Optional)* For Play-signed release APKs on tagged releases, add GitHub repository secrets `KEYSTORE_BASE64`, `STORE_PASSWORD`, and `KEY_PASSWORD` (matching alias `upload`). Without those secrets, the release workflow automatically falls back to attaching the debug-signed APK so releases never fail.
