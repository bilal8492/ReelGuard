# ReelGuard Privacy Policy

ReelGuard helps you keep track of how many Instagram Reels and YouTube Shorts you watch each day.

## What the app does with the Accessibility Service
- It checks whether the Reels or Shorts screen of Instagram or YouTube is currently shown, and counts how many different
  items you swipe through.
- It does **not** read, record, store or transmit what the videos show or say, captions, usernames, comments, search
  terms or messages.
- To tell two items apart it compares a one-way hash of on-screen identifiers. Hashes are kept in memory only (the last
  5) and are not written to the database. The only exception is a recording you start yourself from the hidden debug
  screen, which stores hashes and timestamps in a local file.
- The service only acts for the two apps listed above (and only the ones you leave switched on in Settings). Events from
  other apps are ignored immediately.

## Data stored on your device
- Per day and app: the number counted, and the time of each count (kept 30 days, then deleted).
- Your settings (limits, reset time, options).
- Nothing else. No account is required.

## Data that leaves your device
No counts, settings or screen content ever leave your device. ReelGuard has no analytics, advertising or crash reporting.

The one network use is **Firebase Remote Config**, which downloads a small configuration file (the list of screen
identifiers used for detection) so detection can be fixed after YouTube/Instagram updates. It uploads none of your data.
Like any Firebase client, it sends technical request data to Google (a Firebase installation ID, app ID and version, IP
address, device model and OS version). The official APK releases published on GitHub are built **without** Firebase (no `google-services.json`), so this feature
is inactive and those builds make no network requests at all.

## Permissions
- Accessibility Service: detection described above and the "limit reached" screen.
- Notifications (Android 13+): optional limit warnings and a notice if the service stops working.

## Your control
You can turn the service off in Android Settings > Accessibility at any time, switch individual apps off in ReelGuard,
and delete all data by clearing the app's storage or uninstalling it.

## Contact
Questions about privacy: open an issue at https://github.com/bilal8492/ReelGuard/issues (or see `SECURITY.md` for private reports).
