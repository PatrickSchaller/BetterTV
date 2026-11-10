# Home-role mismatch and optional redirection

Version 0.1.1 addresses the case where Android reports Better TV as its Home-role holder while the remote's Home button opens Google TV.

## Diagnosis

This behavior was reproduced on Google's Android TV 12 emulator. `dumpsys role` listed `com.bettertv.launcher` as HOME holder and package preferences listed its activity, but Home resolution still chose `com.google.android.tvlauncher/.MainActivity`. That activity had intent-filter priority 2; Better TV had priority 0. Android caps positive activity priorities for non-privileged apps, so increasing our manifest priority would not solve this.

The physical TCL was reported as model `50QM5K`, running Android TV OS 12. The user confirmed installation, normal app launch, selecting Better TV as Home, and Google TV still opening after a full restart. The TV's actual Home resolver/priority has not been read, so matching the emulator's exact cause remains an inference.

## Change

- The Home-role message now distinguishes Android's role selection from actual Home-button routing.
- **Settings → Home button fix** explains the optional workaround and opens Android Accessibility settings.
- The service is off until the user explicitly enables **Better TV Home button fix**.
- On connection it discovers exported system Home activities, excluding this app and negative-priority fallback activities. It subscribes to window-state events from those packages only, and redirects only when an exact discovered Home activity appears while the user is unlocked.
- It requests no screen-content retrieval, key filtering, screenshots, or network permission. Runtime accessibility capabilities reported **0** in the emulator. Event metadata identifies the package/activity; screen contents are not read.
- Google TV can appear briefly before the redirect. Opening the original Home activity manually also redirects while the service is enabled. Disable the service to restore normal access. Other TV settings remain usable.
- No stock app/component is disabled, uninstalled, or cleared. No boot receiver or startup timer is added; startup depends on Home window events and must be checked on target firmware.

## Validation

- Core and manifest checks passed, including protected accessibility-service binding and the absence of key filtering/content-retrieval capabilities.
- Debug assembly and Android lint passed: 0 errors, 4 existing warnings.
- APK Signature Scheme v2 verification passed. A development-key update installed over 0.1.0 without uninstalling it.
- With the optional service enabled, sending Home from TV Settings resumed Better TV. Activity-manager state was checked after transitions settled.
- TV Settings remained resumed when opened directly; the service did not redirect its activity.
- Disabling the service restored Google TV Home handling, with stock packages left intact.
- With the service enabled, a full emulator reboot settled on Better TV, with the service bound and no reported crash. This does not establish the TCL's boot ordering or background-service policies.
- Earlier direct key filtering was insufficient in testing and was removed from the delivered implementation.

Artifact: `output/BetterTV-0.1.1-debug.apk`, 1,956,908 bytes.

SHA-256: `14e43f88602e29cc054106c131ff4d6af4bdcedeb5fab0d821c3c7c1c4f0a1ac`.

The 0.1.1 APK was subsequently copied to the user's USB drive and hash-verified. The user installed it on the physical TCL and confirmed that Home works while the fix is enabled. After Restart, the Accessibility switch turns off and must be enabled again. This distinguishes the reported failure from merely missing the first Home-window event. TCL startup policy or a service failure still needs device-specific diagnosis; no boot-permission change or service-crash log has been observed directly.

## TCL service disabled after restart

First check whether this firmware includes **Safety Guard → Permission Shield → Auto Launch Permission**. TCL documents the Safety Guard app but explicitly says it is not present on every model. AbleSign and OptiSigns document the per-app startup controls on TCL TVs: Auto Manager set to Closed (manual selection), the target app set to Open/Opened, and, where present, the same target-app allowance under Relevance Start Permission. Apply the target-app rule to Better TV; retain the existing allowances for other apps. Re-enable the accessibility fix and repeat Restart.

Sources: [TCL Safety Guard guide](https://support.tcl.com/en_US/us-googletv-troubleshooting/using-tcls-google-tvs-safety-guard-to-help-speed-things-up), [AbleSign's TCL startup instructions](https://www.ablesign.tv/digital-signage/app-auto-start-tcl-tvs/), and [OptiSigns' TCL startup instructions](https://support.optisigns.com/hc/en-us/articles/11412877112595-What-if-the-OptiSigns-app-does-not-auto-start-on-an-Android-TV).

If these controls are absent or the setting still resets, collect device-specific accessibility state, startup app-ops, and relevant crash/startup logs through an explicitly configured ADB connection before changing system policy. Firmware-specific app-op names and persistence vary. Do not assume a generic command or another TCL model's policy file applies. This build does not grant itself secure-settings access, automatically re-enable a disabled accessibility service, or change TCL policy.

Still unverified on the physical TV: persistent boot/standby behavior, casting, antenna TV, and HDMI integration.

Primary platform references: [intent-filter priority limits](https://developer.android.com/guide/topics/manifest/intent-filter-element), [AccessibilityService](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService), and [accessibility service configuration](https://developer.android.com/reference/android/accessibilityservice/AccessibilityServiceInfo).
