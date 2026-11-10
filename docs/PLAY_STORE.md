# Google Play: Accessibility API declaration and disclosure

> Draft wording. Review against the current Play policy before submitting. **Approval is not guaranteed**: Play restricts
> the Accessibility API for apps that are not accessibility tools, and a "limit other apps" use case may be rejected.
> ReelGuard sets `isAccessibilityTool` to its default (false) and must be declared as a non-accessibility use.
> Distributing the APK directly (sideloading) avoids the review but not the Android 13+ "restricted settings" step.

## In-app prominent disclosure (shown before the permission request, with an explicit "I understand" button)

> **What ReelGuard accesses**
> ReelGuard uses Android's Accessibility Service only to detect whether Reels or Shorts is on screen and to count swipes.
> It does not read, collect or store content such as videos, captions, usernames or messages. What you watch, your
> counts and your settings never leave this device, and there is no account.
>
> To see how many people use ReelGuard, it shares anonymous usage numbers such as app opens and your country. You can
> switch this off any time in Settings.

This is the text of `ob_disclosure_body` in `strings.xml`; the screen has "I understand, continue" and "No thanks".

## Accessibility API declaration (Play Console form)

**Core functionality that requires the Accessibility Service**
ReelGuard lets users limit their own consumption of short-form video. It detects when Instagram Reels or YouTube Shorts is
shown, counts the number of items the user views per day, and, once the daily limit chosen by the user is reached, shows
a "Daily limit reached" screen over those two surfaces with a button to go to the home screen.

**Why the Accessibility API is needed**
Neither app exposes a public API for what the user is viewing. The only on-device way to know that the Reels/Shorts
player is visible and that the user moved to the next item is to observe window state, scroll events and view
identifiers of the foreground app. The overlay uses the accessibility overlay window type and the global Back/Home actions.

**Data handled**
Only booleans (is the Reels/Shorts player visible), timestamps and counts are persisted. For distinguishing items, a
one-way hash of on-screen identifiers is held in memory (last 5 items) and never stored or transmitted. No screen
content, text, messages, passwords or personal information is collected, stored, or shared. The only network use is
Firebase (anonymous app-usage statistics, on by default and switchable in Settings, and Remote Config); neither includes
anything from the accessibility service, counts or settings.

**Events and scope**
`typeWindowStateChanged`, `typeWindowContentChanged`, `typeViewScrolled`. Content and scroll events from apps other than
Instagram and YouTube are discarded immediately and never inspected.

**User consent and control**
Prominent in-app disclosure with affirmative consent before directing the user to system settings; the user enables the
service themselves in Android Settings and can disable it at any time. Each monitored app can be switched off in the app.

**Not used for**
Advertising, analytics of accessibility data (the separate anonymous app-usage statistics contain nothing from the
service), monitoring other people, autonomously performing actions on behalf of the user, collecting
credentials, or any purpose other than the one described.

## Other store-listing notes
- Privacy policy URL: host `PRIVACY.md` and link it.
- Data safety form: with Firebase active, declare as collected (optional for the user, not shared with third parties,
  Google acting as service provider, purpose Analytics): App activity (app interactions: first open, sessions), Device or
  other IDs (Firebase installation ID, app-instance ID), and Approximate location (country/region/city derived from the IP
  address). The advertising ID is not collected and the AD_ID permission is removed. Without `google-services.json`
  nothing is collected.
- Foreground/background behavior: event-driven; no foreground service, no polling.
