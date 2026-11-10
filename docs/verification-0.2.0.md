# Version 0.2.0 verification

Built November 10, 2026. Package `com.bettertv.launcher`, version code 3, same local development signing identity as the previous version.

## Delivered

- `output/BetterTV-0.2.0-debug.apk`, 5,939,684 bytes.
- Copied to `D:/BetterTV-0.2.0-debug.apk` on the removable FAT32 drive labeled CMBOOT. Source and USB SHA-256 hashes matched.
- SHA-256: `9bdd712c567d55ddd0107f29f266fffecf326151c764f516299dab3720286fc6`.
- APK Signature Scheme v2 verification passed.

## Passed

- Java rule checks: service presets/order, ZIP/token format and bounds, provider selection and offer labels, safe app-name mappings, antenna-only channels, locked/hidden exclusions, title matching.
- Manifest checks: only Internet and TV-listings permissions; cleartext networking disabled; Search/Local TV activities not exported; search preferences excluded from both cloud backup and Android 12 device transfer; existing narrow Home-service capabilities retained.
- `:app:assembleDebug`, `:app:lintDebug`, `:app:assembleDebugAndroidTest`: build successful. Android lint had 0 errors and 25 warnings, primarily English UI string construction, TV orientation/banner conventions and platform compatibility attributes. Warnings remain visible rather than suppressed.
- Updated APK installed over the existing development build on Google's Android TV 12 emulator.
- Custom instrumentation returned success (`INSTRUMENTATION_CODE: -1`) for 31 Android checks. These exercise actual Android JSON/URI parsing using synthetic fixtures, including malformed/fractional/overflow IDs, missing US data, distinct offer categories, My-services filtering, source-link validation, antenna filtering, and the masked token field in the real settings dialog. Fixture data exists only in the test APK.
- The setup test found that configuring single-line input after password input could remove token masking. The call order was corrected, and the real masked-field regression check passed.
- Main → Search navigation, My-services checkbox interaction, missing-token setup, antenna query handoff, explanatory opt-in dialog, Android TV-listings permission prompt, denied-permission recovery, and absent/empty guide handling were checked in the emulator.
- Gallery layout inspected at normal and 150% system text size; Search/All apps remained visible and D-pad focus reached Search. Returning Home selected another bundled image without a background video/feed.
- Crash buffer was empty during the checked flows. No existing TV package was disabled or uninstalled by the app.

## Not established by these checks

No TMDB token was configured or external account created. Therefore successful live TMDB catalog/provider responses, the full live service picker, real subscription availability and per-provider app shortcuts still need credential-backed validation. Offline fixtures verify parsing/filtering; they are not evidence of current catalog contents or a working subscription.

No user ZIP was supplied during this build. The ZIP field can be configured locally, but this build has no external ZIP-lineup/schedule subscription. Antenna search uses only searchable terrestrial guide rows already shared by the TV, over the next 48 hours. Actual TCL guide sharing, antenna reception, and guide freshness remain unverified.

Physical-TV navigation, performance/memory, streaming playback/HDR/audio, input handoff, casting, and startup persistence need checking on the TCL. The previously reported accessibility switch resetting after Restart is outside this feature update; the Home-button service is preserved, not given new startup privileges.

Screenshots: `output/screenshots/gallery-home.png`, `gallery-large-font.png`, `universal-search.png`, and `antenna-guide.png`.

Setup and data-source details: [search-setup.md](search-setup.md). Artwork provenance and exact generation prompts: [gallery-artwork.md](gallery-artwork.md).
