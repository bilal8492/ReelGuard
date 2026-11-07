# ReelGuard

[![CI](https://github.com/bilal8492/ReelGuard/actions/workflows/ci.yml/badge.svg)](https://github.com/bilal8492/ReelGuard/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

An Android app that counts the Instagram Reels and YouTube Shorts you watch each day, lets you set a daily limit, and
shows a "limit reached" screen when you hit it. Regular videos, feeds, stories and messages are never blocked.
Everything stays on your device: no account, no analytics, no ads.

**Website:** https://bilalansari.in/ReelGuard/ · **Download:** [latest release](https://github.com/bilal8492/ReelGuard/releases/latest)

## Install (APK)

ReelGuard is not on Google Play. Download `ReelGuard-vX.Y.Z.apk` from the
[Releases page](https://github.com/bilal8492/ReelGuard/releases/latest), open it, and allow installing from your
browser/file manager. Optionally verify the checksum against the `.sha256` file. On Android 13+, if the accessibility
switch is greyed out: App info > ReelGuard > menu > *Allow restricted settings*, then enable it under Accessibility.

> Detection IDs are best-effort and not yet verified across many devices; see `docs/TEST_RESULTS.md` and
> `docs/SELECTORS.md`. Please report problems.

## Privacy

See [PRIVACY.md](PRIVACY.md). The released APK contains no Firebase configuration and makes no network requests.

## Build from source

Requires JDK 17 and Android SDK 35.

```bash
./gradlew assembleDebug      # APK in app/build/outputs/apk/debug
./gradlew test               # unit tests
```

Optional: put your own `app/google-services.json` in place to enable Firebase Remote Config for selector updates. It is
git-ignored. Release signing reads `keystore.properties` (also git-ignored).

## Releasing (maintainers)

Add repository secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`, then push a tag:

```bash
git tag v0.1.0 && git push origin v0.1.0
```

The *Release APK* workflow tests, signs, and publishes the APK plus checksum to GitHub Releases.

## License

[MIT](LICENSE). See also [SECURITY.md](SECURITY.md) and [CONTRIBUTING.md](CONTRIBUTING.md).
