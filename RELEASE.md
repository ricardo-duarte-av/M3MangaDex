# Release & publishing

## What CI does (`.github/workflows/ci.yml`)

- **Every push and PR** runs `spotlessCheck detekt lintDebug compileDebugKotlin`, then
  `testDebugUnitTest verifyRoborazziDebug`, then assembles the debug APK and the release APK + AAB.
  Everything runs with `-PwarningsAsErrors=true`. The APKs, AAB, R8 mapping and reports are uploaded as artifacts.
- **`v*` tags** also run these jobs:
  - `verify-tag`: the tag must equal `v` + `m3mangadex.versionName` in `gradle.properties`.
  - `play`: uploads the AAB and its mapping to the Play **internal** track with status `completed`, using
    `r0adkll/upload-google-play`. It only runs in `ricardo-duarte-av`'s repo. Gradle Play Publisher is
    not used because it breaks on AGP 9.
  - `release`: creates a GitHub Release with `m3mangadex-vX.Y.Z-{debug,release}.apk`.

## Versioning

`0.<milestone>.<build>` while a milestone is in progress (M3 → `0.3.0`, `0.3.1`, …). When a
milestone is finished, the next one starts at `0.<n+1>.0`. `1.0.0` comes once every milestone is
done and tested.

Only `m3mangadex.versionName` in `gradle.properties` is edited. Play's versionCode is derived
from it as `major × 1,000,000 + minor × 1,000 + patch`, e.g. 0.3.12 → 3012 and 1.0.0 → 1000000,
so it always increases with the version. Minor and patch must stay below 1000; the build fails otherwise.

## Cutting a release

1. Bump `m3mangadex.versionName` in `gradle.properties`, then commit and push.
2. `git tag vX.Y.Z && git push origin vX.Y.Z`

## Signing

`app/build.gradle.kts` reads `keystore.properties` from the repo root, which is gitignored:

```properties
storeFile=/absolute/or/app-relative/path/upload-keystore.jks
storePassword=…
keyAlias=upload
keyPassword=…
```

If that file is missing, it falls back to the env vars `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`,
`KEY_ALIAS` and `KEY_PASSWORD`. With neither, the release build comes out unsigned rather than failing.

Create an upload key:

```bash
keytool -genkeypair -v -keystore upload-keystore.jks -alias upload \
  -keyalg RSA -keysize 4096 -validity 10000 \
  -dname "CN=M3MangaDex, O=Aguiar Vieira, C=PT"
```

Back up the keystore and its passwords. Never commit them: `*.jks` and `keystore.properties` are gitignored.

## GitHub Actions secrets

| Secret | Value |
| --- | --- |
| `KEYSTORE_BASE64` | `base64 -w0 upload-keystore.jks` |
| `KEYSTORE_PASSWORD` | store password |
| `KEY_ALIAS` | `upload` |
| `KEY_PASSWORD` | key password |
| `PLAY_SERVICE_ACCOUNT_JSON_BASE64` | `base64 -w0 play-service-account.json` (raw JSON also accepted) |

## One-time Play Console setup (manual)

1. Create the app with package **`pt.aguiarvieira.m3mangadex`**. The package can't be changed later.
2. Fill in the required declarations: privacy policy, data safety, content rating, target audience
   and ads (there are none).
3. Upload the **first AAB by hand** to Internal testing. The API cannot create the first release.
   Build it with `./gradlew :app:bundleRelease`.
4. Keep Play App Signing on, which is the default. You upload with the upload key.
5. Give the existing Play service account access to this app with release-to-testing permissions.
   If you are not reusing an account, create one under API access.

After that, every `v*` tag publishes automatically.
