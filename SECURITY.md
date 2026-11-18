# Security & Privacy Policy

## Privacy Architecture

**Glassbon Launcher** is engineered as a strictly offline, privacy-preserving Android launcher:

- **No Network Access**: The app does **not** declare `android.permission.INTERNET` and sends zero telemetry, analytics, or crash payloads.
- **Zero Manifest Permissions (`0 <uses-permission>` tags)**:
  - Custom wallpapers use Android's zero-permission Photo Picker (`ActivityResultContracts.PickVisualMedia`).
  - Media playback controls use `AudioManager.dispatchMediaKeyEvent(...)`—no `BIND_NOTIFICATION_LISTENER_SERVICE`.
  - Tactile haptic feedback uses standard view-level `HapticFeedbackConstants`—no `VIBRATE` permission required.
  - Lock Screen button links cleanly to an installed Android Lock Screen / Screen Off utility app or system security settings—zero `WRITE_SETTINGS`, Accessibility Services (`BIND_ACCESSIBILITY_SERVICE`), `BIND_DEVICE_ADMIN`, `READ_SMS`/`RECEIVE_SMS`, `SYSTEM_ALERT_WINDOW`, or `REQUEST_DELETE_PACKAGES` (uninstalls route safely to system App Info).

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
