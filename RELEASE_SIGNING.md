# Pilot Companion signed Android releases

Debug APKs from GitHub-hosted runners are **not suitable for updating installed copies**: each runner may use a different debug certificate. Only distribute the `pilot-companion-signed-release` artifact for future installs.

## One-time owner setup

On a trusted local machine with Java keytool installed, generate and securely back up a release keystore:

```sh
keytool -genkeypair -v -keystore pilot-companion-release.jks -storetype PKCS12 -alias pilot-companion -keyalg RSA -keysize 3072 -validity 10000
```

Use a unique, strong password. Back up the keystore and passwords **outside GitHub** in a secure password manager / encrypted backup. Losing the key means future APKs cannot update copies signed by that key.

In repository Settings > Secrets and variables > Actions, create these repository secrets:

- `PILOT_SIGNING_KEYSTORE_BASE64`: single-line base64 of the keystore bytes (e.g. `base64 -w 0 pilot-companion-release.jks` on Linux; use a no-wrap equivalent on other systems)
- `PILOT_SIGNING_STORE_PASSWORD`: keystore password
- `PILOT_SIGNING_KEY_ALIAS`: `pilot-companion`
- `PILOT_SIGNING_KEY_PASSWORD`: key password

**Never commit, paste into issues, or send keystore files or passwords in chat.**

After secrets are configured, run the Android workflow on `milestone-2-sync`. Download the artifact named `pilot-companion-signed-release`. Keep its certificate and version code consistent for all subsequent releases. Do not distribute debug artifacts as upgrade APKs.

## Migrating existing debug installations

An existing installation signed with a different debug key cannot be updated in place by a new release key. First add and verify an export/import backup of locally stored schedule data, then export and independently verify the backup. Only after that should the user uninstall the old debug-signed app and install the signed release APK. Do **not** advise uninstalling or clearing app data before the backup feature is tested.

The release build is skipped until all four secrets exist. This is intentional: do not fall back to generating a new key in CI.
