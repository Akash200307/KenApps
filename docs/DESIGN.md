# Product design · personal Android build

## Direction

Calm premium utility: warm surfaces, generous space, precise type, soft rounded
cards. The app supports a personal habit; copy does not imply that charging use is
universally unsafe or claim battery-health benefits.

## Home

KenApps wordmark and Unplug Reminder subtitle. Center a large battery percentage
inside a restrained charging ring, followed by the actual connection state.
Show an explicit Enable reminders switch, an honest monitoring-status label, and
a native-alert compatibility card. Keep Test reminder clearly marked as a test.
Use 24dp page margins, 24dp card corners, 16–20dp card padding, and at least 48dp
touch targets. Let layouts scroll at large font sizes rather than clipping.

## Appearance

Three independent palettes, each with Light, Dark, and Follow system modes.
Default to Mist + Follow system. Persist selections immediately in DataStore and
provide a live preview of app surfaces. Avoid a simulated native pill that implies
the app can customize system UI.

- Mist: teal accent `#176B57`, light background `#F5F6F0`; dark accent `#8AD5C1`,
  background `#111A18`.
- Lavender: plum accent `#68518C`, light background `#F8F5FA`; dark accent
  `#D4B8F3`, background `#1A1620`.
- Sand: bronze accent `#825724`, light background `#FAF6EE`; dark accent
  `#E9BF86`, background `#201A13`.

Derive complete Material 3 schemes and validate contrast for all text/control
pairs. Use system typography, scalable text, meaningful TalkBack labels, and
accessible selected states. Respect the system animation scale/reduced-motion
preference; no looping pulse while offscreen or monitoring in the background.

## Settings and acceptance

Show notification and native-promotion settings, pause/resume, optional vibration
(off by default), monitoring recovery instructions, and device diagnostics.
Only show device-specific setup steps verified on the actual OxygenOS build.
Theme previews update without restarting. Verify all nine palette/mode choices,
TalkBack, large text, and reduced motion before a wider release. The current
personal build uses semantic switch/radio groups, scalable text, scrolling layouts,
and animation APIs that honor Android's system duration scale.
