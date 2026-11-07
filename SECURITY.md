# Security policy

Please report vulnerabilities privately through GitHub: **Security tab > Report a vulnerability** on this repository.
Do not open a public issue for security problems. You can expect a first reply within a few days.

ReelGuard requests the Accessibility Service permission, so changes to `detection/` and the manifest get extra review.

## Secrets

This repository must never contain signing keys, `keystore.properties`, `google-services.json` or tokens. They are in
`.gitignore`. Releases are signed in GitHub Actions using repository secrets. If you find a committed secret, report it
privately as above.
