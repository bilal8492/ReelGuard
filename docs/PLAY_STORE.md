# Publishing ReelGuard on Google Play

Why: Google Play Protect blocks sideloaded apps that use an accessibility service in some countries (India is one), with
an "App blocked to protect your device" dialog that has only an OK button. That check is on Google's side and cannot be
fixed in the app. Apps installed **from Google Play** are not subject to it, so Play is the way for ordinary people to
install ReelGuard. (Google's description of the rule: <https://developers.google.com/android/play-protect/warning-dev-guidance>.)

> Everything below is a draft prepared from Google's published policies as we understood them. **Check each item against
> the live Play Console and policy pages before submitting**, because the requirements change (the tester numbers and
> dates below are examples of things that did).
> **Approval is not guaranteed.** Play restricts the Accessibility API for apps that are not accessibility tools. ReelGuard
> leaves `isAccessibilityTool` at its default (false) and is declared as a non-accessibility use. A "limit other apps"
> use case may be rejected or may need changes; plan for a second round.

## 1. Who does what

| Already done in the repo | Needs you (an account holder) |
| --- | --- |
| Targets Android 16 / API 36 (`compileSdk` and `targetSdk` 36, AGP 8.13.2, Gradle 8.13) | Create the Play Console developer account (one-time fee, currently US$25) |
| Release workflow also builds a signed `.aab` (`ReelGuard-vX.Y.Z.aab` next to the APK) | Verify your identity with a government ID (and any address/phone checks Play asks for) |
| In-app prominent disclosure before the accessibility permission | Accept the Play developer agreements |
| Hosted privacy policy: `site/privacy.html` → <https://bilalansari.in/reelguard/privacy.html> after the site deploys | Create the app, upload the bundle, paste the listing text, fill the declarations |
| Store graphics and screenshots in `docs/play/` | Record and upload the accessibility demo video (script in section 7) |
| Listing text, content-rating and data-safety answers (below) | Run the closed test if your account type requires it (section 2), then submit for review |
| | Make the Android developer-verification step for your account if Google requires it where you distribute |

## 2. Account and release prerequisites (verify each one)

- **Developer account**: personal accounts are verified with a government ID. Use the real legal name; it is shown on the listing.
- **Closed testing for new personal accounts**: Google has required new personal accounts to run a closed test with a
  minimum number of testers for a minimum number of days before applying for production access. We saw conflicting
  figures while researching (12 testers vs 20; 14 days). **Read the number in your Play Console**; do not trust ours.
  Plan to recruit the testers early (friends, family, the GitHub issue reporters); the testers must opt in with the Gmail
  address you add to the test list and keep the app installed.
- **Target API**: new apps must target API 36 from 2026-08-31, and this build does.
- **Android developer verification**: Google has announced that apps installed on certified devices must come from a
  verified developer; a free limited account (for small numbers of devices) or a paid account exists. The dates differ by
  country and we could not confirm India's. Check <https://developer.android.com/developer-verification> and register
  when it asks you to; it also affects the GitHub APK.
- **Contact details**: Play requires a public developer email address on the store listing. Use one you are happy to
  publish (the privacy policy currently only lists GitHub issues).

## 3. Build and signing

1. Tag a release (`git tag vX.Y.Z && git push --tags`). The workflow produces `ReelGuard-vX.Y.Z.apk`, a `.sha256`
   and `ReelGuard-vX.Y.Z.aab`. Upload the **`.aab`** to Play; the APK stays for direct download.
2. `versionCode` comes from the GitHub run number and always increases; `versionName` is the tag. Play rejects a bundle whose
   `versionCode` was already used, so never re-upload the same build.
3. **Play App Signing** is mandatory for new apps. At the first upload Play asks which key to use:
   - *Use the key we already sign the APK with (export and upload it)*: the Play app and the GitHub APK then have the
     **same signature**, so people can update either one with the other. Choose this if you want one app identity.
   - *Let Google generate the key*: Play signs with its own key, and the CI keystore becomes only the **upload key**. The
     Play build and the GitHub APK then have different signatures, and switching from one to the other needs an uninstall
     (which erases the app's local data).
   Decide before the first upload; the choice is hard to undo. Keep `KEYSTORE_*` backed up either way.
4. The `google-services.json` secret is already restored by CI, so the Play build has Firebase like the APK.
5. Not verified yet: the targetSdk-36 build on a real Android 16 device (the phone was not connected). Do a pass first:
   open the app, enable the service, scroll Reels and Shorts, hit the limit, check the widget and the midnight reset.

## 4. Store listing

**App name** (max 30): `ReelGuard: Reels/Shorts Limit` (29)

**Short description** (max 80): `Count Reels and Shorts, set a daily limit, and stop when you reach it.` (70)

**Full description** (max 4000):

```
ReelGuard counts the Instagram Reels and YouTube Shorts you watch each day and shows a "daily limit reached" screen when
you hit the limit you chose. Regular videos, feeds, stories and messages are never blocked.

HOW IT WORKS
• Pick a daily limit (and optional separate caps for Shorts and Reels).
• ReelGuard counts each Reel or Short you swipe through, only while the Reels or Shorts screen is showing.
• A notification warns you as you approach the limit.
• At the limit, a screen covers Reels and Shorts and playback is paused. One tap takes you home. You can allow one
  short snooze a day if you want that.
• Your counter resets every day at the time you choose.

SEE YOUR HABIT
• Today screen with a progress ring and what is left.
• History for 7 and 30 days, your streak within the limit and your busiest hour.
• A home-screen widget so the count is always one glance away.

PRIVATE BY DESIGN
• No account, no ads.
• What you watch, your counts and your settings stay on your device.
• ReelGuard does not read or store videos, captions, usernames, comments or messages.
• Optional anonymous usage statistics (app opens and your country) are on by default and can be switched off in Settings.

WHY IT NEEDS THE ACCESSIBILITY SERVICE
Instagram and YouTube do not tell other apps what you are watching. ReelGuard uses Android's Accessibility Service only to
see whether the Reels or Shorts screen is showing and when you swipe to the next one. It ignores every other app. You turn
it on yourself in Android Settings and can turn it off at any time. ReelGuard explains this before it asks for the permission.

OPEN SOURCE
ReelGuard is free and open source (MIT): https://github.com/bilal8492/reelguard

ReelGuard is an independent app and is not affiliated with, endorsed by or sponsored by Instagram, Meta, YouTube or Google.
Instagram, Reels, YouTube and Shorts are trademarks of their respective owners.
```

> Trademark note: the names Instagram/YouTube appear only in the description with the disclaimer. Keep them out of the
> app name, icon and screenshots' captions; "impersonation" and "intellectual property" are common rejection reasons.

**Category**: Tools (alternative: Productivity or Lifestyle). **Tags**: pick from the list offered (for example *Digital wellbeing*).

**Contact**: email (public) · website `https://bilalansari.in/reelguard/` · privacy policy `https://bilalansari.in/reelguard/privacy.html`

### Graphics (all in `docs/play/`)

| Asset | File | Play requirement |
| --- | --- | --- |
| App icon | `icon-512.png` (512×512) | 512×512 PNG, up to 1 MB; Play rounds the corners itself |
| Feature graphic | `feature-graphic-1024x500.png` | 1024×500 PNG or JPG |
| Phone screenshots (7) | `screenshots/phone-01-today.png`, `-02-warning.png`, `-03-limit-reached.png` (block screen), `-04-widget.png`, `-05-today-limit.png`, `-06-history.png`, `-07-settings.png` (1170×2340, in this order; Play only promotes apps with at least 4 screenshots of 1080 px or more per side) | 2 to 8; each side 320–3840 px, long side at most 2× the short side |

The icon and feature graphic are rendered from `promo-video/src/PlayAssets.tsx`
(`cd promo-video && npx remotion still src/index.ts PlayIcon ../docs/play/icon-512.png`; `PlayFeature` for the graphic).
The screenshots were captured on a phone and padded to exactly 2:1; the clock/notification strip was blanked.
**Retake them from the release build before submitting** (they predate the latest UI changes). A 7-inch and 10-inch tablet
set is optional.

## 5. Content rating, audience and policy forms

- **Content rating (IARC questionnaire)**: category *Utility / productivity*. Answer *No* to violence, fear, sexual content,
  language, controlled substances, gambling, user-generated content and sharing of user location; *No* to online
  interaction between users. Expected rating: Everyone / 3+. Answer the questionnaire truthfully in the Console; it
  produces the rating.
- **Target audience**: 18 and over (the app is about adult self-control; choosing under-13 triggers the Families policy).
- **Ads**: *No, the app contains no ads.*
- **App access**: all functionality is available without a login; no special instructions needed. (Mention that the
  accessibility service must be enabled and that Instagram/YouTube are needed to see it count.)
- **News app / government app / health / financial features**: *No* to all.
- **Permissions** (the app's own, plus what libraries merge in; check `app/build/intermediates/merged_manifests/release/`
  after a build): `BIND_ACCESSIBILITY_SERVICE` (service, see section 7), `POST_NOTIFICATIONS` (limit warnings),
  `RECEIVE_BOOT_COMPLETED` (re-schedule the daily reset after a reboot). Merged from Firebase/WorkManager:
  `INTERNET`, `ACCESS_NETWORK_STATE`, `WAKE_LOCK`, `FOREGROUND_SERVICE` and the Play install-referrer binding. The app declares
  no foreground-service type of its own, no exact alarms, no location, no contacts, and the advertising ID is removed. If
  the Console asks about `FOREGROUND_SERVICE`, it is only the library's declaration; re-check after any dependency update.
- **Data safety form**: Firebase is active, so declare:
  - *Collected*: App activity → App interactions (first open, sessions); Device or other IDs (Firebase installation /
    app-instance ID); Approximate location (country/region/city derived from the IP address).
  - Purpose: Analytics. *Not shared* with third parties (Google acts as a service provider). *Optional*: users can turn it
    off in Settings. *Encrypted in transit*: yes. *Users can request deletion*: they can stop collection in Settings;
    describe that no account exists, and that data expires after the Analytics retention period.
  - Advertising ID is not collected and `AD_ID` is removed. Without `google-services.json` nothing is collected.
- **Privacy policy URL**: `https://bilalansari.in/reelguard/privacy.html` (merge the PR that adds it and wait for the Pages
  deploy; open the URL in a private window to check it loads without a login).

## 6. In-app prominent disclosure (shown before the permission request, with an explicit "I understand" button)

> **What ReelGuard accesses**
> ReelGuard uses Android's Accessibility Service only to detect whether Reels or Shorts is on screen and to count swipes.
> It does not read, collect or store content such as videos, captions, usernames or messages. What you watch, your
> counts and your settings never leave this device, and there is no account.
>
> To see how many people use ReelGuard, it shares anonymous usage numbers such as app opens and your country. You can
> switch this off any time in Settings.

This is the text of `ob_disclosure_body` in `strings.xml`; the screen has "I understand, continue" and "No thanks".

## 7. Accessibility API declaration (Play Console form)

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

### Demo video (the form asks for a link; upload to YouTube as *unlisted*, about 30 to 60 seconds, screen recording with voice or captions)

1. Open ReelGuard for the first time: show the **prominent disclosure** screen and tap "I understand, continue".
2. Show Android Settings > Accessibility > ReelGuard being switched on, and the system permission dialog.
3. Open YouTube Shorts and swipe a few items: show the count going up in ReelGuard (or the widget).
4. Set a very low limit (for example 3), swipe past it: show the **"Daily limit reached"** screen and the Home button.
5. Show Settings > monitored apps, switching one off, and switching the accessibility service off in Android Settings.
Say (or caption) on screen: "ReelGuard only detects whether Reels or Shorts is shown and counts swipes. It does not read
content."

## 8. Closed test, then production

1. Create the app (name, default language English, *App*, *Free*), complete the sections the Console lists
   (App content: privacy policy, ads, app access, content rating, target audience, data safety, **accessibility
   declaration**, anything else it asks).
2. Upload the `.aab` to a **closed testing** track, add testers by email list, send them the opt-in link. Keep it running
   for as long as your account's requirement says.
3. Apply for production access, answering the questions about the test honestly (what feedback you collected and changed).
4. Submit the production release. Reviews of Accessibility apps can take longer than usual. If it is rejected, the email
   says why; fix it and resubmit rather than arguing the policy.

## 9. Fallback and extras

- **Play Protect appeal for the sideloaded APK** (only if you keep distributing it): Google offers a developer
  "Play Protect appeal" route (see the page linked at the top). Suggested text: *"ReelGuard (package `com.reelguard`)
  is an open-source digital-wellbeing app (<https://github.com/bilal8492/reelguard>). It uses an accessibility service
  only to detect whether Instagram Reels or YouTube Shorts is on screen and to count swipes, and it shows a
  limit-reached screen. It does not read or transmit content. Release builds are signed with a stable key and published
  with a SHA-256 checksum. We ask that the 'App blocked' enhanced-fraud-protection block be reviewed for this app."*
  Replace the package name with the real `applicationId` from `app/build.gradle.kts`. We do not know whether the appeal is
  granted for sideloaded apps; Play distribution is the reliable route.
- After Play approval: update the README and the website Install section so Google Play is the main install option (they
  currently say "ReelGuard is not on Google Play"), and keep the APK as the secondary one.
- Once it is public, the promo video and the website's privacy statement should match the app (the video still says
  "No analytics", which is no longer true).
