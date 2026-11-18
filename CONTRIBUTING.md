# Contributing to Glassbon Launcher

Thank you for helping make **Glassbon** faster, cleaner, and more intentional. Glassbon is built around **minimalist frosted liquid-glass design** and **zero negative impact on battery or system performance**. Every contribution is reviewed against those two pillars.

---

## Core Engineering Rules (Mandatory)

1. **Zero Idle Battery & CPU Overhead**
   - Do **not** add background polling timers, `while (true)` `delay()` coroutines, `WakeLock`s, or `WorkManager` periodic jobs.
   - Use event-driven Android system broadcasts (`ACTION_TIME_TICK`, `ACTION_PACKAGE_*`) and unregister listeners in `onStop()` / `onCleared()`.
2. **Smooth 60–120 FPS Rendering**
   - Evaluate transient animations strictly inside `Modifier.graphicsLayer { ... }` or `Modifier.offset { ... }` lambdas so scrolling and scrubbing avoid full-tree recompositions.
   - Never stack per-row `RenderEffect` blur shaders on individual `LazyColumn` items; keep any drawer focus `BlurEffect` single-layer, bounded, bypassed during active scroll/scrub, and disabled in **Low Power** mode alongside static `WallpaperProcessor` pre-blurs.
   - When implementing hidden edge controls (`invisibleMode = true`), skip composing or drawing child elements while idle.
3. **Architecture & Kotlin Style**
   - Follow **Kotlin + Jetpack Compose + Material 3 + MVVM** (`StateFlow` + `collectAsStateWithLifecycle`).
   - Keep composables modular and files focused.
   - Assign `Modifier.testTag("snake_case_name")` to all interactive elements, enforce minimum `48.dp` touch targets, and provide meaningful `contentDescription` strings.
4. **Privacy & Play-Protect Hygiene First (Zero Sensitive Permissions)**
   - **Never** add `INTERNET`, `READ_EXTERNAL_STORAGE`, `REQUEST_DELETE_PACKAGES`, `BIND_ACCESSIBILITY_SERVICE`, `BIND_DEVICE_ADMIN`, `BIND_NOTIFICATION_LISTENER_SERVICE`, `READ_SMS`, or broad media read permissions (`PickVisualMedia` photo picker only).
   - Route uninstall actions to system App Info (`ACTION_APPLICATION_DETAILS_SETTINGS`) and media keys through `AudioManager`.

---

## Local Development Workflow

```bash
# Build debug APK
gradle :app:assembleDebug

# Run all JVM unit & Robolectric UI tests (no emulator required)
gradle :app:testDebugUnitTest --no-daemon

# Optional static lint verification
gradle :app:lintDebug
```

> **Note**: Never commit or modify `debug.keystore` or `debug.keystore.base64`, and never hardcode secrets or signing passwords into Gradle files.

---

## Pull Request Guidelines

1. Fork the repository and create a focused branch from `main` (`feat/wave-tuning`, `fix/clock-contrast`, `docs/readme-media`).
2. Keep commits concise and descriptive (e.g., `feat: add left-edge hidden wave tour step`).
3. Ensure `gradle :app:testDebugUnitTest` passes cleanly and add Robolectric UI or unit test assertions for any new behavior or preference toggle.
4. Include screenshots or screen recordings when changing visual surfaces or motion physics.
5. Complete every item in `.github/PULL_REQUEST_TEMPLATE.md` before requesting review.

---

## Reporting Issues

Use the structured GitHub Issue Forms under **Issues → New Issue** so maintainers have Android OS version, device details, and reproduction steps right away.
