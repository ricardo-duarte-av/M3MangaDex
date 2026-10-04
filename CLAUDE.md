# CLAUDE.md

## Project

**M3MangaDex** (`pt.aguiarvieira.m3mangadex`) is a native Android MangaDex client built on Material 3 Expressive.
The roadmap is in `docs/MILESTONES.md`. Finish one milestone at a time; each ends with green CI and a tag.

The house style follows the author's other apps. `~/xmuks` is the reference for build-logic, CI,
theme and Navigation 3. `~/Documentos/jellymusic` is the reference for cover-art seed extraction
(`ui/theme/AlbumSeed.kt` and `AlbumTheme.kt`).

## Toolchain

All versions are in `gradle/libs.versions.toml`.
- Gradle 9.8, AGP 9.4.1, Kotlin 2.4.20, KSP 2.3.12, Hilt 2.60.1
- compileSdk 37 + minor 1 (`platforms;android-37.1`), targetSdk 37, minSdk 31
- Compose alpha BOM, with material3 pinned to 1.5.0-alpha29 for the Expressive APIs.
  material3-adaptive is 1.4.0-alpha02 and Navigation 3 is 1.3.0-alpha01.
- JDK 21 runs the build; the code targets JVM 17.

## Versioning

Use `0.<milestone>.<build>` while milestone M<n> is in progress, and `1.0.0` when everything is done.
Only `m3mangadex.versionName` (in `gradle.properties`) is bumped; versionCode is derived from it.
Releases are cut by pushing a `v<versionName>` tag. See `RELEASE.md`.

## Gotchas (don't re-discover)

- **AGP 9 has built-in Kotlin.** Never apply `org.jetbrains.kotlin.android`. The Compose compiler plugin is
  still applied, by `m3mangadex.android.compose`.
- **AGP 9's `CommonExtension` is non-generic and getter-only.** Inside convention plugins, set values with
  property access (`defaultConfig.minSdk = …`).
- **Hilt needs a newer `kotlin-metadata-jvm`.** `m3mangadex.hilt` adds it to the KSP classpath, pinned to
  the Kotlin version.
- **`Project.libs` in build-logic must stay `internal`.** If it's public, it shadows the generated accessor.
- **KSP2 masks plain Kotlin errors** in Hilt-injected classes. To see the real one, drop the
  hilt plugin and compile again.
- **Warnings fail the CI build** (`-PwarningsAsErrors=true`). Opt in to experimental APIs
  (`ExperimentalMaterial3ExpressiveApi`, `ExperimentalMaterial3AdaptiveApi`) at the use site.

## Layout

- `app`: Activity and navigation shell (`navigation/M3MangaDexApp.kt`). It uses `NavigationSuiteScaffold`,
  with one Navigation 3 back stack per tab and the list-detail scene strategy.
- `core:designsystem`: `M3MangaDexTheme` (`MaterialExpressiveTheme` + `MotionScheme.expressive()`, dynamic
  color, materialkolor fallback from seed `#FF6740`), Google Sans Flex typography and `ThemeCatalog`.
  Features never define their own colours or shapes.
- New modules apply the convention plugins `m3mangadex.android.{application,library,compose,feature}`,
  `.hilt`, `.jvm.library` and `.screenshots`.

## MangaDex API rules (must hold in all code)

- Rate limit is about 5 req/s per IP; use a client-side limiter and back off on 429.
  Send a real `User-Agent: M3MangaDex/<ver>`, and never a `Via` header.
- Send `Authorization` only to `api.mangadex.org` and `auth.mangadex.org`, and only on authenticated
  endpoints. Never send it to image hosts (`*.mangadex.network`); use a separate OkHttp client for images.
- Images come from `GET /at-home/server/{chapterId}` as `{baseUrl}/{data|data-saver}/{hash}/{file}`. The
  baseUrl lasts about 15 minutes; fetch it again on failure. Report each load to
  `https://api.mangadex.network/report` (not needed for mangadex.org hosts).
- Auth uses a user-supplied personal client with the password grant at
  `https://auth.mangadex.org/realms/mangadex/protocol/openid-connect/token`. Access tokens last
  15 minutes. Don't store the password. Keep it behind `AuthProvider` so PKCE can replace it later.
- Credit scanlation groups. Default content ratings are safe and suggestive.

## Checks

```bash
./gradlew spotlessApply   # format
./gradlew spotlessCheck detekt lintDebug testDebugUnitTest verifyRoborazziDebug assembleDebug -PwarningsAsErrors=true
./gradlew recordRoborazziDebug   # after an intended UI change, then commit the PNGs
```
