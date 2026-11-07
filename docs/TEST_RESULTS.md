# Test results

Last updated on the development machine (Windows 11, JDK 17, Android SDK 35, Gradle 8.11.1). **No physical device or
emulator has been used.** Nothing in section 2 has been run.

## 1. Automated (run, passing)

| Check | Command | Result |
|---|---|---|
| Debug build | `./gradlew assembleDebug` | PASS |
| Unit tests (39) | `./gradlew test` | PASS: ReelCounter 19, Replay 2, SelectorConfig 4, DayKey 5, EvaluateLimit 5, HistoryStats 3, settings encoding 1 |
| Android lint | `./gradlew lint` | PASS |
| detekt | `./gradlew detekt` | PASS |
| ktlint | `./gradlew ktlintCheck` | PASS |
| Instrumented tests compile | `./gradlew assembleDebugAndroidTest` | PASS (compiles only) |

What the unit tests do and do not prove: the counting rules (first item once, 800 ms settle, bounce-back, last-5
memory, 10 s re-entry window, ads, null signatures, outdated flag, structural fallback) behave correctly **given the
signals fed in**. They say nothing about whether the real YouTube/Instagram UI produces those signals. The replay test
uses a hand-written (synthetic) recording.

Not run: `connectedDebugAndroidTest` (Room in-memory DAO tests with Turbine, Compose tests for onboarding and
dashboard). They compile but need a device/emulator.

## 2. Acceptance script (physical device) — NOT RUN

Legend: `[ ]` not run. Record PASS/FAIL, device, Android version and app versionCodes when you run them.
Device: ______  Android: ______  YouTube versionCode: ______  Instagram versionCode: ______

### YouTube Shorts
- [ ] Open the Shorts tab and swipe through 10 shorts: count = 10 (first counted once, no doubles)
- [ ] Swipe up, then back down 1: count rises by 1 only, not 2
- [ ] Open comments, close them: count unchanged
- [ ] Pause/resume and rotate the screen: count unchanged
- [ ] Open a short from the Home feed shelf, a search result, and a channel's Shorts tab: counted
- [ ] Leave to the Home tab and scroll: nothing counted
- [ ] At the limit: overlay appears, re-entering Shorts re-triggers it, and normal video playback is not blocked

### Instagram Reels
- [ ] Open the Reels tab and swipe through 10 reels: count = 10
- [ ] Swipe up, then back down 1: count rises by 1 only
- [ ] Open comments, close them: count unchanged
- [ ] Pause/resume and rotate the screen: count unchanged
- [ ] Open a reel from the Home feed, search/Explore: counted
- [ ] Leave to the Home tab and scroll: nothing counted
- [ ] At the limit: overlay appears, re-entering Reels re-triggers it, and the feed/stories/DMs are not blocked
- [ ] Reels opened from Explore, a profile, and a DM link: counted
- [ ] Stories and the home feed are not counted or blocked

### Widget and app
- [ ] The widget updates after counting and shows "Limit reached" at the limit
- [ ] The daily reset clears counts at the reset time
- [ ] Killing the service shows the health warning

### Also worth checking (not in the original script)
- [ ] Debug screen: signals A/B/C and the signature behave as described in `SELECTORS.md`
- [ ] Debug screen: last scan time stays around or below 5 ms-ish on the target device (spec target; the scan is capped at 500 nodes)
- [ ] Battery use over a day (spec target < 1%)

### Firebase Remote Config — NOT RUN
- [ ] Build with a real `google-services.json`, publish a `selectors_json` change, confirm the Debug screen/behaviour picks it up
- [ ] Build without `google-services.json` (verified to compile; runtime fallback not run on a device)

### Partial device observations (Samsung SM-S928B, Android 17, YouTube 21.44.534) — NOT the full script
- Instagram Reels counted as expected (reported by the user; counts reached the 50 limit and the overlay appeared).
- YouTube Shorts initially counted nothing: the Shorts UI exposes no text, so no item signature existed. Fixed by using
  the pager's adapter position (and the seek-bar video length as a second source). After the fix, 1 initial Short + 5
  adb-injected swipes = count 6 (correct). Signals A, B and C all passed, and the overlay blocked touches at the limit.
- Measured scan time on this device: roughly 10 to 180 ms per scan (typically 50 to 100 ms), well above the 5 ms spec
  target. Scans are throttled to one per 120 ms, but battery and CPU impact is unverified.
- Live counter pill ("Shorts 14 · Total 64/100", top centre, non-touchable) showed correctly over YouTube Shorts.
- Widget: pinned via Settings > Add widget to home screen; the 2x2 widget showed 64/100 and the larger widget showed
  per-app counts and "37 left". The preview in the system add dialog says "Couldn't add widget" (no preview image supplied).
- Not yet run: comments/rotation/pause checks, Home-feed/search/channel entry points, widget, reset, health warning.

## 3. Items that cannot be verified without a device

All of section 2, plus: the real view IDs in `selectors.json`, overlay rendering and its Back/Home fallback, widget
rendering and the 10 s update debounce, WorkManager reset timing, notifications, behaviour on Xiaomi/Samsung/OnePlus
battery managers, and the Android 13+ "restricted settings" step for sideloaded installs.
