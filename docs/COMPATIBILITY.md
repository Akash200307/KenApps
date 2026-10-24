# Native Live Alerts compatibility gate

## Target device

Read using targeted ADB properties on 24 October 2026; no device serial recorded.

- Manufacturer/model: OnePlus CPH2691
- Android: 17, API 37
- Build: `CPH2691_17.0.0.104(EX01)`
- OxygenOS ROM property: `V17.0.0`
- Security patch: 2026-10-01

## Original compatibility gate

The initial plan required **all** of the following:

1. Documented integration eligibility for this charging/unlock reminder.
2. A visible native OxygenOS Live Alerts pill carrying the real unplug reminder.
3. Correct dismissal and cleanup on the real device.
4. Reliable foreground-service behavior while the app is backgrounded.

An app observation, `hasPromotableCharacteristics()`,
`canPostPromotedNotifications()`, or `FLAG_PROMOTED_ONGOING` is diagnostic evidence,
not proof of all four requirements. A user observation cannot automatically unlock
full implementation. Do not substitute an overlay or invent a task category.

## Accepted personal-build limitation

On 24 October 2026, the user enabled Live Alerts and explicitly reported:
**"Native pill shows the reminder."** The app's saved observation also became
"Native pill visible". They exercised Pause, and the private report showed the
reminder removed while monitoring stayed active.

Android's public guidance excludes ordinary alerts; no OEM-approved exception for
this particular reminder has been established. After that distinction was
explained, the user explicitly selected **"Continue the personal APK; record the
limitation"**. This changes the full-app gate for personal distribution only.
Native rendering works on the tested phone; OEM endorsement, Play Store
suitability, and future OS compatibility are not claimed.

The official [OPPO Fluid Cloud introduction](https://open.oppomobile.com/product/page?page_name=fluid)
and linked [service-development documentation](https://open.oppomobile.com/documentation/page/info?id=11593)
were reviewed. Those describe real-time services and the Pantanal/UMS service
framework; they do not establish approval of this charging reminder. KenApps uses
the standard AndroidX notification API, not a private OEM API or cloud service.

## Device protocol

- Start with notifications enabled and the app's Live Alerts setting enabled if
  offered. Note whether the OS actually exposes that setting.
- With power disconnected, unlock: no reminder.
- Connect while unlocked: exactly one reminder request.
- Inspect the actual system pill after leaving the app. Confirm its text and that
  tapping it opens KenApps. A status icon alone is insufficient.
- Lock while plugged: reminder removed. Wake without unlocking: no reminder.
- Unlock while plugged: one reminder, with no duplicate after battery updates.
- Dismiss using the notification's Dismiss action or system dismissal: battery
  changes and rapid charger reconnects must not repost during the same unlocked
  session. Lock/unlock permits the next reminder.
- Unplug: reminder removed. Disconnect USB physically or test wireless charging
  where available; do not mistake a synthetic battery event for physical proof.
- Pause: reminder removed for 15 minutes. Resume: reconcile the current power and
  lock state. Expiry resumes only when the phone is plugged in and unlocked.
- Stop session: both reminder and monitoring notification removed.
- Disable notifications: stop monitoring with an actionable error. Re-enable and
  start again explicitly. Test channel-specific blocking separately.
- Force-stop and reboot: the test does not restart automatically; reopen and start.
- Verify idle/background behavior and a plugged-in charge-limit pause. A battery
  that is connected but not actively charging must still count as plugged in.

Use physical actions for hardware acceptance. Record synthetic test results
separately and restore any simulated battery state with `adb shell dumpsys battery reset`.

## Evidence log

Build, installation, and device results will be recorded here after execution.

Distribution status: **personal experimental build authorized**, with the usage
guidance limitation above recorded. Source and UI preserve that distinction.
