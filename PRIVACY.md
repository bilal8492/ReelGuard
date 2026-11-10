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
What you watch, your counts and your settings never leave your device. ReelGuard has no advertising or crash reporting.

**Anonymous usage statistics (on by default, you can switch them off).** To see how many people use ReelGuard and
roughly where from, the app uses Google Analytics for Firebase. Only Firebase's automatic app events are sent: first
open, sessions and how long the app is open, app version, device model, Android version, language, a random
app-instance ID, and an approximate location (country, region, city) that Google derives from the IP address (per
Google's documentation, Analytics does not log or store IP addresses). The advertising ID is not collected, ad
personalisation is switched off, and the app does not even hold the advertising-ID permission. Switch this off at any
time in Settings > Privacy > Share anonymous usage statistics: the app then stops collecting and discards anything it
had not yet uploaded. Google Analytics keeps this data for a limited period (2 to 14 months, depending on the property
settings).

**Firebase Remote Config** downloads a small configuration file (the list of screen identifiers used for detection) so
detection can be fixed after YouTube/Instagram updates. It uploads none of your data. Like any Firebase client, it
sends technical request data to Google (a Firebase installation ID, app ID and version, IP address, device model and
OS version).

Builds made without a `google-services.json` (for example one you compile yourself) contain no Firebase configuration
and make no network requests at all.

## Permissions
- Accessibility Service: detection described above and the "limit reached" screen.
- Notifications (Android 13+): optional limit warnings and a notice if the service stops working.
- Internet: used only for the anonymous usage statistics and Remote Config described above.

## Your control
You can turn the service off in Android Settings > Accessibility at any time, switch individual apps off in ReelGuard,
switch anonymous usage statistics off in Settings > Privacy, and delete all data by clearing the app's storage or uninstalling it.

## Contact
Questions about privacy: open an issue at https://github.com/bilal8492/reelguard/issues (or see `SECURITY.md` for private reports).
