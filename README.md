# KenApps · Unplug Reminder

A personal Android app that gently reminds you to **unplug to use your phone**
when you unlock while connected to power. It uses OxygenOS's **native Live Alerts
pill**, not an overlay.

Native pill rendering was confirmed by the user on **OnePlus CPH2691**, Android
17/API 37, build `CPH2691_17.0.0.104(EX01)`, after enabling the app's Live Alerts
setting. This does not establish compatibility with other phones or future OS
updates.

**Personal experimental APK.** Android's
[Live Updates usage guidance](https://developer.android.com/develop/ui/views/notifications/live-update#usage-criteria)
excludes ordinary alerts. After the real-device test succeeded, the user explicitly
chose to continue the personal build with that limitation recorded. This is not a
claim of Play Store suitability or an OEM-approved charging-reminder integration.
The app requests promotion through documented AndroidX APIs with truthful status
content. It does not impersonate media, a call, navigation, or progress.

## Features

- Remind on unlock while plugged in, including connection during unlocked use.
- Track external power independently of battery percentage or charging status.
- One reminder per unlocked session; respect dismissal until the next unlock.
- Remove on lock, unplug, pause, or disable. Phone use remains unrestricted.
- Pause for 15 minutes; optional gentle vibration, off by default.
- A clearly marked, 15-second Test reminder while monitoring is enabled.
- Home dashboard with battery ring and honest monitoring status.
- Mist, Lavender, and Sand palettes, each with Light, Dark, and Follow system.
- Persistent settings, device diagnostics, and explicit recovery after interruption.
- No accounts, analytics, internet, overlay, accessibility, or usage-access permission.

## Install and start

Requires **Android 16/API 36 or newer** and a phone that supports native promotion
for this notification. The only verified target is the device above.

1. Install the personal preview APK and open **KenApps**.
2. Turn on **Enable reminders** and allow notifications if prompted.
3. If prompted, enable KenApps in **Live Alerts settings**, return, and turn
   reminders on again. Settings also links to notifications and Live Alerts.
4. Plug in the charger, then lock and unlock the phone. Look for the native pill.
5. Use **Appearance** for palette and light/dark settings.
6. Use **Pause 15 min**, or turn reminders off from Home or the monitoring notification.

OxygenOS controls the pill's layout, colors, animation, and placement. App themes
style the app only. If notifications, the selected reminder channel, or Live Alerts
are disabled, monitoring stops and requires setup/restart; the app does not fall
back to an overlay. Android notification-channel settings can override vibration.

Monitoring uses an ongoing foreground-service notification and system events.
It does not poll or hold wake locks. Reboot, force-stop, app update, or process
loss can interrupt monitoring; reopen KenApps and explicitly enable it again.
Previously requested monitoring is remembered so the app can show recovery help.
The current pause is session-local and resets when monitoring is restarted.

## Build and verify

Install JDK 21, Android SDK platform 36, and build tools 35.0.0. Set `ANDROID_HOME`
or create an untracked `local.properties` containing `sdk.dir=/absolute/path/to/sdk`.

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The APK is debug-signed with a local Android debug key stored outside Git. Do not
commit a keystore. GitHub Actions runs tests and lint and uploads a test APK. The
Gradle distribution is pinned with a SHA-256 checksum. Update-version notices are
excluded from lint so availability of a newer dependency cannot break this pinned
build. The target-version notice is also excluded because API 36 is intentional
for this build; source, resource, manifest, and permission checks remain enabled.

Unit coverage includes reminder transitions, dismissal, rapid reconnects, pause
timing, explicit tests, and text contrast across all six palette/light-dark
combinations. Follow system resolves to the appropriate light or dark palette.
See [device evidence and remaining checks](docs/COMPATIBILITY.md).

## Source layout and privacy

- `domain/ReminderSession` is the Android-independent reminder state machine.
- `monitor/CompatibilityService` is the user-started reminder foreground service;
  its original name is retained from the compatibility milestone.
- `monitor/NativeAlerts` owns standard native notifications and promotion requests.
- `data/DiagnosticPreferences` persists appearance, vibration, monitoring intent,
  and diagnostic notes using DataStore.
- `ui/KenAppsTheme` owns the three light/dark color schemes.

Diagnostics contain this app's status, device model/build, and optional notes.
They exclude serial numbers, Android IDs, and other apps' notifications. Copying
a report is explicit; it is not uploaded. Cloud backup and device transfer are
excluded. The private file `files/compatibility-report.json` is available via
`adb shell run-as com.kenapps.unplug cat files/compatibility-report.json` in this
debug build. Local device reports and screenshots are ignored by Git.

See [the design specification](docs/DESIGN.md) for the visual direction.
