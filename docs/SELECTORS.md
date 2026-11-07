# Updating selectors (view IDs and heuristics)

> The IDs in `app/src/main/assets/selectors.json` are **best-effort guesses, not verified on any device**. YouTube and
> Instagram change their view IDs between versions. Expect to calibrate on your own phone before trusting the counts.

## How detection uses the config

Per app (`com.google.android.youtube`, `com.instagram.android`) the entry for the installed `versionCode` is chosen
(`minVersionCode`/`maxVersionCode`, otherwise the entry with `"isDefault": true`). A scan scores three signals and the
surface is "active" when `threshold` (default 2) of 3 pass:

| Signal | Config keys | Passes when |
|---|---|---|
| A (IDs) | `viewIds` | any visible node has one of these resource ids (package prefix ignored) |
| B (structure) | `pagerClasses`, `pagerFillRatio`, `actionDescriptions`, `minActionMatches` | a scrollable node whose class contains `RecyclerView`/`ViewPager` fills at least `pagerFillRatio` of the window, and at least `minActionMatches` distinct action labels (like/comment/share...) appear as content descriptions or texts |
| C (navigation) | `tabLabels`, `titleKeywords` | a selected node's label contains a tab label, or a node's text/description equals a title keyword (e.g. "Shorts") |

The item identity ("signature") is the hash of the text of `signatureViewIds` nodes (creator handle, caption). If none are
found, the first few caption-like TextViews inside the pager are hashed instead. `adViewIds` mark sponsored items.
Only hashes exist, in memory.

**Structural fallback** (Settings > Detection) counts the surface as active on Signal B alone, so counting survives
renamed IDs. It is less precise, so it is off by default.

## Calibration workflow (needs a real device)

1. Install the debug build, finish onboarding, enable the accessibility service.
2. Open **Settings** and tap the *Version* row 7 times. This opens the hidden **Debug / Calibration** screen.
3. Switch to YouTube > Shorts (or Instagram > Reels) and back. The screen shows, per app: `versionCode`, signals
   A/B/C, ID and action matches, whether a full-screen pager was found, the surface (Shorts/Reels/none), the
   signature hash, today's count, and the last scan's node count and duration.
4. Interpret:
   - A is false but you are on Shorts/Reels: the IDs are outdated. Dump the tree (step 5).
   - B is false: check `actionDescriptions` (labels are language-dependent; add your UI language) and the pager class.
   - C is false: check `tabLabels`.
   - The signature stays `-` or never changes between items: fix `signatureViewIds`.
5. Press **Dump node tree**. You have 8 seconds to switch to the app; the file is written to the app's private storage
   and can be shared from the screen. Free text (captions, handles, titles) is replaced by `h:<hash>#<length>`.
   Labels that are configured in `selectors.json` (action labels, tab labels, title keywords) are kept in clear text,
   so to see a new label, add it to the config first and dump again.
   Without the share sheet (debug build):
   ```
   adb exec-out run-as com.reelguard cat files/dumps/<file>.json > dump.json
   ```
6. Open the dump and look for: the vertical pager (`scrollable` flag `S`, large bounds), nodes with ids that look like
   `reel...`/`clips...`/`shorts...`, the action column (`C` clickable nodes with descriptions), the selected bottom tab (`Z`).
   `uiautomatorviewer`/`uiautomator dump`, or Android Studio's Layout Inspector, give the same information with full
   text, which you can use locally.
7. Edit `selectors.json`. Add a **new entry** with `minVersionCode`/`maxVersionCode` for the version you tested, keeping the
   default entry as the fallback.
8. Rebuild/install and recheck the Debug screen. Then run the acceptance checks in `TEST_RESULTS.md`.

## Record / replay

*Debug screen > Start recording signals*, do a session (swipe, open comments, rotate...), *Stop*. The JSON file contains
only the engine's input signals (booleans, timestamps, signature hashes). Copy it to
`app/src/test/resources/recordings/<name>.json`, add `<name>.expected` containing the number of reels you actually
watched, and `ReplayTest` will replay it through `ReelCounter` on every `./gradlew test`. The bundled
`synthetic_shorts_session.json` is hand-written, **not** a device capture.

## Remote config (optional)

Implemented in `FirebaseSelectorSource` (not tested against a real Firebase project; none was available).

1. Create a Firebase project, add an Android app with package `com.reelguard`, and download `google-services.json`
   into `app/`. The build applies the Google Services plugin only if that file exists, so without it the app builds
   and runs offline with the bundled `selectors.json` and makes no network requests.
2. In Remote Config, add a parameter named `selectors_json` of type JSON/String whose value is the full contents of a
   `selectors.json` (same schema), then publish.
3. The app fetches at start and in the 30-minute background worker (Firebase caches for 1 hour). The result is
   validated; invalid or empty JSON is ignored, and the last valid config is cached on disk. Remote values replace
   the bundled file entirely, so always include every app.

Adding Firebase adds the `INTERNET` permission and makes Google receive request metadata; see `PRIVACY.md`.
