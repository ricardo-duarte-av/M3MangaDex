# M3MangaDex — milestones

## Context
A new Android MangaDex client (`pt.aguiarvieira.m3mangadex`) built on Material 3 Expressive. It has to work well as a comic reader on both phones and tablets, and it themes itself from manga cover art. It supports anonymous use and logged-in use of the MangaDex API.

The repo is empty: one commit with only a README and a .gitignore. The reference apps already establish a house style:
- **xmuks** (`/home/daedric/xmuks`) is the newest and most mature. It is the template for build-logic, versions, theme, Navigation 3 and CI.
- **jellymusic** (`~/Documentos/jellymusic`) is the template for cover-art theming (`ui/theme/AlbumSeed.kt` and `AlbumTheme.kt`).
- **slskdAndroid** (`~/Documentos/slskdAndroid`) contributes its `CLAUDE.md` list of AGP 9 gotchas and its `RELEASE.md` Play setup guide.

The work is split into milestones. Each one ends with a green CI run and a tag that can be installed.

## Decisions (confirmed with user)
- **Auth:** bring-your-own personal client. MangaDex public OAuth clients are "not yet available", so the login screen takes the client ID, client secret, username and password and uses the `password` grant against `https://auth.mangadex.org/realms/mangadex/protocol/openid-connect/token`. Access tokens last 15 minutes and are renewed with `grant_type=refresh_token`. The app includes help text on creating a client at mangadex.org/settings. Auth sits behind an `AuthProvider` interface so authorization-code + PKCE can replace it later.
- **Offline downloads:** a late milestone. Until then, images are cached only by Coil's disk cache.
- **Play:** tags upload to the `internal` track.
- **minSdk:** 31, matching xmuks. Dynamic color is always available, so there is no pre-12 fallback path. compileSdk and targetSdk are 37 (android-37.1), as in xmuks.

## Stack (copied from xmuks `gradle/libs.versions.toml`)
- **Build:** AGP 9.4.1 with built-in Kotlin (do not apply `kotlin-android`), Kotlin 2.4.20, KSP, JDK 21 on CI and JVM target 17.
- **UI:** Compose BOM alpha 2026.09.01 with material3 pinned to 1.5.0-alpha29 for the Expressive APIs, material3-adaptive 1.4.0-alpha02, material3-adaptive-navigation-suite, and Navigation 3 1.3.0-alpha01 with the list-detail scene strategy.
- **Data:** Hilt, OkHttp 5 + kotlinx-serialization (no Retrofit, as in xmuks), Room 3, DataStore, WorkManager.
- **Images and color:** Coil 3 (network-okhttp), materialkolor 5.x, telephoto (`zoomable-image-coil3`) for reader zoom.
- **Quality:** detekt 2 with compose-rules, Spotless + ktlint, Roborazzi + Robolectric.

## Module layout
```
app
core:{model, network, database, datastore, data, auth, designsystem}
feature:{browse, search, manga, reader, library, updates, login, settings, downloads}
baselineprofile            (M6)
build-logic/convention     (m3mangadex.android.application|library|compose|feature, .hilt, .jvm.library, .screenshots)
```
Copy and rename the convention plugins from `xmuks/build-logic/convention/src/main/kotlin/`, along with `KotlinAndroid.kt` (`-PwarningsAsErrors`), `compose_stability.conf`, `config/detekt/detekt.yml`, and the root `build.gradle.kts` setup for Spotless and detekt.

## MangaDex API constraints (apply in every milestone)
- **Rate limit:** about 5 requests per second per IP, enforced by a client-side token-bucket OkHttp interceptor. Back off and retry on 429. Some endpoints have stricter limits: login is 30 per hour and chapter reads are 300 per 10 minutes.
- **User-Agent:** a real one is required, e.g. `M3MangaDex/<versionName> (Android <sdk>; +github url)`. Never send a `Via` header.
- **Auth header scope:** send `Authorization` only to `api.mangadex.org` and `auth.mangadex.org`, and only on endpoints that need it, because unauthenticated responses are cacheable. Never send it to `*.mangadex.network` or image hosts. Use a separate OkHttp client for images.
- **Image delivery:** fetch `GET /at-home/server/{chapterId}` to get `baseUrl` + `hash` + `data`/`dataSaver`, then load `{baseUrl}/{data|data-saver}/{hash}/{file}`. The `baseUrl` is valid for about 15 minutes; on a 403 or a failed load, fetch it again.
- **Image reporting:** POST `https://api.mangadex.network/report` with `{url, success, bytes, duration, cached}` for every image load whose host is not `mangadex.org`.
- **Response shape:** JSON:API style. Request `includes[]=cover_art,author,artist,scanlation_group` and resolve `relationships` into domain models in `core:network`.
- **Content rating:** default `contentRating[]` to `safe` and `suggestive`. `erotica` is opt-in in Settings. Decide before the M6 public release whether `pornographic` stays out of the Play build, because Play's sexual-content policy applies.
- **External chapters:** chapters with an `externalUrl` and zero pages open in Custom Tabs.
- **Attribution:** credit the scanlation group on every chapter, as MangaDex's terms require. No ads.

---

## Milestones

### M0 — Scaffold & CI (implement first)
1. Gradle skeleton: `settings.gradle.kts` (includeBuild build-logic, typesafe accessors, `FAIL_ON_PROJECT_REPOS`), version catalog, wrapper, `gradle.properties` with `m3mangadex.versionName` and `versionCode`.
2. `app` with applicationId `pt.aguiarvieira.m3mangadex` (debug builds add `.debug`), namespace equal to the applicationId, and release builds with R8 minify and resource shrinking.
3. Signing as in `xmuks/app/build.gradle.kts`: read `keystore.properties`, fall back to the env vars `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`, and leave the release build unsigned if they are missing. Update `.gitignore` for `*.jks`, `keystore.properties`, `play-service-account.json`, `local.properties`, `build/` and `.idea`.
4. `core:designsystem`:
   - `M3MangaDexTheme` built on `MaterialExpressiveTheme` + `MotionScheme.expressive()`, with dynamic color and a materialkolor seed fallback. Port it from `xmuks/.../core/designsystem/theme/Theme.kt`.
   - A DayNight launch theme with the SplashScreen API, to avoid slskd finding A1 (white flash on cold start).
5. `app` shell: `NavigationSuiteScaffold` with Browse, Library, Updates and Settings tabs, Navigation 3 `NavDisplay` with the list-detail scene strategy, and placeholder screens.
6. `.github/workflows/ci.yml`, adapted from xmuks:
   - Triggers: push (all branches), `v*` tags, pull requests, manual dispatch.
   - Steps: `spotlessCheck detekt lintDebug -PwarningsAsErrors=true`, then `testDebugUnitTest`, then assemble the debug APK and the release APK + AAB. Upload all of them as artifacts.
   - `verify-tag`: the tag must equal `v${versionName}`.
   - `play` (tags only, owner-gated): `r0adkll/upload-google-play@v1` with package `pt.aguiarvieira.m3mangadex`, track `internal`, and the mapping file, using secret `PLAY_SERVICE_ACCOUNT_JSON_BASE64`. Don't use Gradle Play Publisher, which breaks on AGP 9.
   - `release`: `softprops/action-gh-release@v2`.
   - Reuse the `.github/actions/android-sdk` composite action for android-37.1.
7. `CLAUDE.md` (toolchain and AGP 9 gotchas from slskd), `RELEASE.md` (secrets and Play setup), and `docs/MILESTONES.md`, a copy of this plan.
   - **Manual Play step:** create the app in the Play Console and upload the first AAB by hand. The API cannot create the listing.

### M1 — API layer & anonymous browsing
- `core:network`:
  - `MangaDexApi` built on OkHttp + serialization.
  - DTOs for manga, chapter, cover, author, group and tag, with relationship resolution.
  - The rate limiter, User-Agent and error-mapping interceptors.
  - Tests against recorded JSON fixtures.
- `core:data` repositories that return `Flow` and `PagingData` (Paging 3 with offset/limit, keeping `offset + limit ≤ 10000`).
- `feature:browse` home:
  - Popular new titles, latest updates and recently added, using `/manga` with `order[...]` and `/chapter` with `order[readableAt]`.
  - Carousels using the M3 Expressive `HorizontalMultiBrowseCarousel`.
- `feature:search`: a SearchBar plus a filter sheet for tags (`/manga/tag`), status, demographic, content rating, original and translated language, and sort order.
- `feature:manga` details:
  - Header with the cover, metadata, a collapsible description and tag chips.
  - A chapter list from `/manga/{id}/feed`, filtered by the preferred translated language, grouped by volume, and showing groups.
  - The list-detail layout puts search results and details side by side on tablets.
- `core:datastore` settings: languages, content ratings, data saver, theme.

### M2 — Reader
- `feature:reader`:
  - Data comes from the at-home server, using the separate image OkHttp client that never sends auth.
  - Modes: paged LTR, paged RTL (the default for manga), vertical paged, and continuous webtoon strip.
  - Picks a default mode from the manga's tags or original language (`ko`/`zh` → webtoon) and remembers an override per manga.
  - Zoom and pan with telephoto, double-tap zoom, and tap zones.
  - Volume-key paging, immersive edge-to-edge mode, and a keep-screen-on option.
  - A bottom `FlexibleBottomAppBar`/`FloatingToolbar` with a page slider, chapter previous/next, and mode toggles.
  - **Tablets:** dual-page spreads in landscape on expanded width, with a single-page cover and a wide-page detector. Resizable windows and foldables are handled.
  - Preloads the next N pages and the next chapter's at-home data.
  - On image failure, fetches the base URL again; reports every image load.
- `core:database` (Room): history and last-read page per chapter, so local progress works while anonymous.

### M3 — Content-driven theming
- Port `AlbumSeed` (materialkolor `QuantizerCelebi` + `Score` on a 128px bitmap) into `core:designsystem` as `CoverSeed`, and add `CoverTheme(coverUrl)`.
- Improve on jellymusic by animating the scheme change: interpolate each `ColorScheme` role with `animateColorAsState` under the expressive motion spec, so the scheme no longer swaps in all at once.
- Cache seeds in an LRU keyed by manga ID, and persist them in Room alongside the manga.
- Apply it to manga details, reader chrome, and a tinted card for each item in the browse lists.
- Optional "theme from current page" in the reader, throttled and defaulting to off.
- A settings toggle: off, cover, or cover+page. Style is `PaletteStyle.TonalSpot`, for the same hue-fidelity reason documented in jellymusic.

### M4 — Authentication & sync
- `core:auth`:
  - `AuthProvider` interface with a `PersonalClientAuthProvider` implementation (password grant).
  - Token manager with a mutex around refresh and an OkHttp `Authenticator` that refreshes on 401.
  - Storage: tokens and client credentials go in DataStore, encrypted with an Android Keystore AES-GCM key. EncryptedSharedPreferences is deprecated.
  - The password is **not** stored. When the refresh token expires, prompt for it again with the client credentials pre-filled.
- Interceptor rule: attach the token only on authenticated endpoints.
- `feature:login`: an expressive form, client-setup help, and error mapping (pending client, bad credentials, rate-limited).
- Synced features:
  - Follows library (`/user/follows/manga`) and reading status (`/manga/status`, `/manga/{id}/status`).
  - Read markers: read with `/manga/read`, write with `/manga/{id}/read`, merged with local history.
  - Updates feed (`/user/follows/manga/feed`), ratings, and custom lists.
- Anonymous mode keeps a local-only library in Room, which can be merged into follows after login.

### M5 — Library, downloads & notifications
- `feature:library`: grid and list views, filtering by reading status, and unread badges.
- `feature:downloads`:
  - WorkManager foreground downloads per chapter into app-specific storage, with queue, pause, retry and delete, and a data-saver option.
  - The reader prefers local files.
- A periodic worker checks for new chapters of followed or local-library manga and posts notifications grouped by manga.

### M6 — Polish & release readiness
- Baseline profile module and Roborazzi screenshot goldens, plus the `screenshots.yml` workflow from xmuks.
- Accessibility pass (labels on long-press actions, as in slskd M3_PLAN A2), string resources, and pt-PT plus en.
- Expressive component audit, as in slskd `M3_PLAN.md`.
- Play listing assets, a content-rating questionnaire, and a final policy decision on `pornographic`.

---

## Verification
- **Every milestone:**
  - `./gradlew spotlessCheck detekt lintDebug testDebugUnitTest assembleDebug -PwarningsAsErrors=true` passes locally.
  - CI is green on the push.
  - Install and run on a phone emulator and on a tablet or foldable emulator to check the adaptive layout.
- **M0:**
  - Push a test tag (e.g. `v0.0.1`) after the manual first Play upload, and confirm the `play` job lands the build on internal testing.
  - Confirm `verify-tag` fails on a version mismatch.
- **M1/M2:**
  - Fixture-based unit tests for relationship parsing and URL construction.
  - Manual checks: read a chapter in each reader mode, kill the network mid-chapter to test the retry path, and use OkHttp logging to confirm no `Authorization` header goes to image hosts.
- **M4:** log in with a real personal client, wait more than 15 minutes, and confirm the silent refresh works and that read markers sync both ways with mangadex.org.
