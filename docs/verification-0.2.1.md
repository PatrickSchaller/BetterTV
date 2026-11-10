# Version 0.2.1 verification

Built November 10, 2026, version code 4. This patch sets the default ZIP to the user-supplied **54650** in Search settings and the antenna guide. An explicitly saved ZIP, including a deliberately cleared value, takes precedence. The resource value was checked and the existing ZIP-format rules passed.

No TMDB account or token exists in the build. Setup instructions explain personal account creation, developer API access and the API Read Access Token, with private entry or USB text import. The ZIP remains a saved local setting; this patch does not add a ZIP-based listings provider.

Validation: core checks, debug assembly, Android lint and test-APK assembly passed. APK Signature Scheme v2 verification passed. The previous release's Android/emulator coverage is documented in `verification-0.2.0.md`; it was not repeated for this resource-default patch.

APK: `output/BetterTV-0.2.1-debug.apk`, 5,892,437 bytes.

SHA-256: `60f93fb435292b73f813041259e85b2dc6fbf06d417c8f595cf8a527e7c60e00`.

USB delivery: `D:/BetterTV-0.2.1-debug.apk` and `D:/BetterTV-Setup-0.2.1.txt`. Both copies were SHA-256 verified. Older APKs were retained for rollback.

Live TMDB data and physical-TCL antenna sharing remain unverified. The restart/accessibility-permission issue is unchanged.
