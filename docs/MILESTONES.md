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

Versioning: `0.<n>.x` while M<n> is in progress (M0 → 0.0.x, M1 → 0.1.x, …), and `1.0.0` when
everything is done and tested. The details are in RELEASE.md.

### M0 — Scaffold & CI ✅ (v0.0.x)
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

### M1 — API layer & anonymous browsing ✅ (v0.1.x)

Done as planned, plus:
- Search suggestions lead with the most-followed matches, then relevance. Relevance alone ranks by
  the main title, so "solo" didn't surface Solo Leveling ("Na Honjaman Level-Up").
- Search has an "only titles in my chapter languages" switch (on by default, matching the Browse
  rows) and tag include/exclude chips.
- Tags on a manga open search filtered by that tag.
- Chapters hosted on publishers' sites open in a Custom Tab. "Start reading" falls back to them when
  nothing is hosted on MangaDex.
- The collapsed search bar can't take focus, so a keyboard-attached tablet doesn't pop it open at launch.

Original plan:
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

### M2 — Reader ✅ (v0.2.x)

Done as planned:
- Paged reading (LTR, RTL, vertical) and a webtoon strip. The mode defaults from the original
  language and the Long Strip tag, and the user's choice is remembered per manga in Room.
- Zoom with telephoto: pinch and double-tap, with pan hand-off to the pager. Tap zones are mirrored
  for RTL.
- Two-page spreads in landscape windows ≥ 600 dp: the cover stands alone, wide pages stand alone and
  pairing restarts after them, and the place is kept when sizes arrive.
- Preloading 4 pages ahead. MangaDex@Home load reports go through `AtHomeReportInterceptor`. A failed
  page re-fetches the at-home server and can be retried.
- Chrome with a page slider (mirrored for RTL), previous/next chapter that prefers the same group, an
  options menu, auto-hide, immersive mode, keep-screen-on and volume-key paging.
- An end-of-chapter card credits the group and offers the next chapter. Publisher-hosted next
  chapters open in a Custom Tab.
- Local progress in Room (`core:database`). Details shows "Continue Ch. X", dims read chapters and
  shows the page reached on half-read ones.

Deferred: pinch-zoom inside the webtoon strip. Telephoto zooms single images, and zooming a whole
lazy list needs a custom gesture layer.

Original plan:
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

### M3 — Content-driven theming ✅ (v0.3.x)

Done:
- Cover theming. `CoverSeed` (Monet's Celebi + Score, ported from jellymusic) picks the seed.
  Screens `PublishCover`, and the app shell wraps everything, navigation bar included, in
  `CoverTheme`, so a screen never shows two palettes. Every colour role animates between schemes
  (`animateColorScheme`). There's a Settings toggle. The predictive-back arrow is drawn by System UI
  from the wallpaper and can't be tinted by apps.
- Language defaults include MangaDex's regional siblings (`pt` + `pt-br`, `es` + `es-la`,
  `zh` + `zh-hk`).
- Details shows "Also in N other languages" chips. Turning one on lists that language's chapters for
  this manga only (Room v2 `manga_settings.extraLanguages`, with a tested migration). The reader uses
  the same languages through `ChapterLanguages`.
- Crop borders: `Borders.detect` finds uniform black or white scan margins and
  `CropBordersTransformation` trims them. Pages now render through `Modifier.zoomable` +
  `SpreadPainter` instead of telephoto's sub-sampling, so crops apply.
- Page fit: Auto (screen in portrait, width in landscape), Screen, Width or Height. In fit-width,
  forward/back taps and volume keys scroll through a tall page before turning it.

Not done: the "theme from the current page" idea; cover colours are enough in practice.

Original plan:
- Port `AlbumSeed` (materialkolor `QuantizerCelebi` + `Score` on a 128px bitmap) into `core:designsystem` as `CoverSeed`, and add `CoverTheme(coverUrl)`.
- Improve on jellymusic by animating the scheme change: interpolate each `ColorScheme` role with `animateColorAsState` under the expressive motion spec, so the scheme no longer swaps in all at once.
- Cache seeds in an LRU keyed by manga ID, and persist them in Room alongside the manga.
- Apply it to manga details, reader chrome, and a tinted card for each item in the browse lists.
- Optional "theme from current page" in the reader, throttled and defaulting to off.
- A settings toggle: off, cover, or cover+page. Style is `PaletteStyle.TonalSpot`, for the same hue-fidelity reason documented in jellymusic.

### M4 — Authentication & sync ✅ (v0.4.x)

Done:
- `core:auth`:
  - `AuthRepository`: password grant for personal clients, plus refresh 1 minute before expiry or
    after a 401, serialised so several requests trigger one refresh.
  - When MangaDex rejects the refresh token, the session becomes `Session.Expired` and the user is
    asked for the password again with the client pre-filled.
- The session is stored AES-GCM-encrypted under an Android Keystore key, in `noBackupFilesDir`. The
  password is never stored.
- `Authenticated` request tag: only user endpoints get `Authorization`, as `AuthInterceptorTest` checks.
- Library tab (status filter chips), Updates tab (follows feed, opens the reader) and a login screen
  with client-setup help. Settings has an account section.
- Details:
  - A status menu (choosing a status also follows the title, as on the site) and a follow bell.
  - Long-press a chapter to mark it read or unread here and on MangaDex.
  - Chapters read on MangaDex show as read, and "Continue" picks up after them.
  - Failed sync rolls back and shows a snackbar.
- The reader marks a chapter read on MangaDex when you reach its last page.
- MangaDex currently rejects `updateHistory=true` on read markers ("Persistent history is
  temporarily disabled"), so the app doesn't send it.

Tested end to end with a development account: login, a persisted session across restarts, the
status, follow and read round trips in both directions, Library and Updates.

Not done: merging an anonymous local library into follows on login. The app had no local library
before M4 (only reading progress, which is kept and shown alongside MangaDex's markers).

Original plan:
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

### M5 — Library, downloads & notifications ✅ (v0.5.x)

Done:
- Downloads:
  - Room v3 `downloads` table, with a tested 1→3 migration.
  - `DownloadEngine` downloads one chapter at a time, page by page, through the image client, so
    MangaDex@Home reports still go out. A failing page gets one fresh at-home server. It resumes
    where it stopped, and deleting stops it mid-chapter.
  - `DownloadWorker` runs it as a data-sync foreground service with a progress notification.
  - Files live in app-private `files/downloads/{chapterId}/NNN.ext`.
- The reader uses downloaded files when present, offline or not. Tested with Wi-Fi and data off.
- Details: a download button per chapter (queued / progress-cancel / done-delete / failed-retry) and
  "Download next 5 / all unread" in the top bar.
- Downloads screen (from Library or Settings), grouped by manga with sizes; tap a saved chapter to read.
- New-chapter notifications:
  - `NewChaptersWorker` runs every 2 h while online and reads the follows feed since the last check.
  - One notification per manga. Tapping it opens the chapter if there's one, else the manga.
  - The setting asks for POST_NOTIFICATIONS on Android 13+.
  - Debug builds have a "check now (48 h)" item.

Not done: notifications for anonymous users. There's no account follows feed to watch, and polling
every library title is too heavy for MangaDex's rate limit.

Original plan:
- `feature:library`: grid and list views, filtering by reading status, and unread badges.
- `feature:downloads`:
  - WorkManager foreground downloads per chapter into app-specific storage, with queue, pause, retry and delete, and a data-saver option.
  - The reader prefers local files.
- A periodic worker checks for new chapters of followed or local-library manga and posts notifications grouped by manga.

### M6 — Polish & release readiness ✅ (v0.6.x)

Done:
- Content policy (the user's decision): `play` and `github` flavors. Play offers Safe and Suggestive;
  the GitHub APK adds an Erotica opt-in. Pornographic is never offered. `ContentPolicy` clamps the
  stored preferences, explicit search filters and library/feed requests, not just the UI. CI uploads
  the `play` AAB and attaches `github` APKs to GitHub Releases.
- Portuguese (pt-PT, also used on pt-BR devices) for every string. Lint's MissingTranslation keeps it complete.
- Accessibility audit with UiAutomator dumps on every screen: all clickable elements labelled, and
  targets ≥ 48 dp. At 200% font scale the navigation labels wrapped mid-word; they're now single-line.
- Baseline profile: `:baselineprofile` (UiAutomator, by test tag) covers startup, Browse, a manga's
  details, paging through the reader and back. Merged into main for both flavors.
- Play listing material (`docs/play-listing/`): texts in en-US and pt-PT, a 512 px icon, a feature
  graphic, IARC and data-safety guidance, and a privacy policy (`docs/privacy-policy.md`).
- `screenshots.yml` workflow (from xmuks): formats, re-records goldens on a branch and starts CI.
- Cover transitions (M3 container transform): a tapped cover in Browse, Search or Library grows into
  the details header, and shrinks back on back (predictive back included). It uses `SharedTransitionLayout`
  around `NavDisplay` with `sharedBounds` keyed by `cover:<list>:<mangaId>`, so only the tapped copy
  flies. A preview cache of listed manga lets the details header draw on its first frame. On phones every
  screen is its own scene (the list-detail strategy only applies from medium width), because inside one
  list-detail scene nothing transitions.

Original plan:
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
