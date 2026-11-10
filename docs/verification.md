# First-build verification

Verified November 9, 2026. This is development version 0.1.0 (`com.bettertv.launcher`), not a TV firmware release.

## Build

- `./build.ps1` passed: Java assertion checks, manifest checks, `:app:assembleDebug`, and `:app:lintDebug`.
- Core checks cover all initial streaming-service presets, stable unique service ordering, unknown-app fallback, narrow app discovery queries, both app/Home entry points, and the absence of requested permissions.
- Android lint: **0 errors, 4 warnings**. Warnings cover a newer Back compatibility attribute ignored on older Android versions, the TV banner's vector size, TV landscape orientation, and Android's changing backup configuration rules. These are left visible in the lint report rather than suppressed. The app disables backup and stores only local favorites.
- `apksigner verify --verbose` passed with APK Signature Scheme v2. Android 8+ supports this signing scheme. The key is a local development key, not a production distribution identity.
- `aapt dump permissions` confirmed that the packaged APK requests no permissions.
- Development tooling was downloaded from official Android/Adoptium/Gradle sources and verified against published SHA-256 hashes. No global JDK, Android Studio, PATH change, or virtualization-driver installation was required.

Artifact: `output/BetterTV-0.1.0-debug.apk`, 1,938,880 bytes.

SHA-256: `1010b7a2c2c19605eb3e6411b0f868e1354960ffa20c15429392808cb6f7a85e`.

## Android TV 12 emulator

Used Google's `system-images;android-31;android-tv;x86` with the 1080p TV profile and existing WHPX acceleration. No streaming accounts were entered and no physical TV was connected.

Passed checks:

- Install and cold launch without a crash.
- Actual installed app discovery and banner rendering.
- D-pad movement between favorites, all apps, the hero action, and header controls.
- Long-press pin/unpin with favorite preferences checked before and after a process restart.
- Launch the installed TV Settings app from its card and preserve its focus when returning.
- Back returns to the first currently available app and the top of the screen.
- Default-home request opens the Android system chooser; cancel returns to the launcher without replacing the original home app.
- Live TV hands off to the emulator's existing Live Channels onboarding activity. This confirms the intent route, not tuner functionality.
- The Inputs action remains usable when the emulator exposes no physical HDMI inputs.
- Native layout inspected at default and 150% system font scale; the hero, buttons, favorite labels, and navigation remain readable. Larger rows scroll using the remote.
- Crash log remained empty during these checks.

During testing, Live Channels' launcher component became disabled in the emulator. Better TV removed the unavailable app from its rows while retaining its saved favorite, so it can reappear if the component becomes available. No system package or component was disabled by this project.

Actual screenshots: `output/screenshots/home.png` and `output/screenshots/large-font.png`. They show the apps present on the emulator, not a fabricated streaming library. Emulator startup timings are not evidence of speed gains on the TCL.

## Still needed on the TCL

Follow the README's device checklist. The retail model and exact firmware matter for Home-button behavior, boot/standby routing, live-TV and HDMI handoff, and possible AirPlay support. No model-specific claim is made from the transcribed firmware identifier.

Unverified: real-TV responsiveness and memory use, every streaming service's playback/HDR/audio, casting, AirPlay availability, antenna tuning, Xbox input switching, voice controls, dedicated remote buttons, and making Better TV the default across standby/full restart. The default-home chooser was opened and canceled in the emulator; Better TV was not made the default on a physical device.
