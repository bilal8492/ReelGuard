# Contributing

1. Fork and branch from `main`.
2. Build and check: `./gradlew ktlintCheck detekt lint test assembleDebug` (JDK 17, Android SDK 35).
3. Open a pull request. CI runs the same checks.

Firebase is optional. To try Remote Config you can add your **own** `app/google-services.json` (git-ignored); never commit
it. Detection selectors are described in `docs/SELECTORS.md`; fixes for new Instagram/YouTube versions are very welcome.

By contributing you agree your work is released under the MIT license.
