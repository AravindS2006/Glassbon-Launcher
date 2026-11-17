# Security & Privacy Policy

## Privacy Architecture

**Glassbon Launcher** is engineered as a strictly offline, privacy-preserving Android launcher:

- **No Network Access**: The app does **not** declare `android.permission.INTERNET` and sends zero telemetry, analytics, or crash payloads.
- **Minimal Permissions**:
  - Custom wallpapers use Android's zero-permission Photo Picker (`ActivityResultContracts.PickVisualMedia`).
  - `GlassbonLockAccessibilityService` declares `canRetrieveWindowContent="false"` and executes only `GLOBAL_ACTION_LOCK_SCREEN`.
  - `GlassbonMediaListenerService` reads active `MediaController` playback state/metadata locally on-device solely to render Home playback controls.

## Supported Versions

| Version | Supported |
| ------- | --------- |
| `1.x` (`main`) | Yes |
| `< 1.0` | No |

## Reporting a Vulnerability

If you discover a security or privacy issue—such as unintended permission escalation, keystore/secret exposure, or accessibility scope bugs—please **do not open a public issue**.

Report privately via:
- GitHub **Security → Advisories → Report a vulnerability** on `AravindS2006/Glassbon-Launcher`, or
- Email **aravindselvan2006@gmail.com** with reproduction steps and device/Android API details.

We aim to acknowledge reports within 48 hours and release a fix or mitigation promptly.
