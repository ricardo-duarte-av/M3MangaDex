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

- `core:model` (JVM): domain types (`Manga`, `Chapter`, `MangaFilter`, `UserPreferences`), `Localized` title
  picking, `Languages` (MangaDex codes ↔ locales), `Descriptions` (Markdown → plain text), `Covers`.
- `core:network`: `MangaDexApi` (OkHttp + kotlinx-serialization, DTOs tolerant of MangaDex quirks like `[]`
  for empty maps). Two clients: `@ApiClient` (rate-limited, the only one that will ever carry auth) and
  `@ImageClient`. The app provides `UserAgent`. Tests use MockWebServer with trimmed real responses in
  `src/test/resources/fixtures`.
- `core:datastore`: `PreferencesDataSource` (chapter languages, content ratings, data saver).
- `core:database`: Room 3 (`androidx.room3`, bundled SQLite driver; tests use `AndroidSQLiteDriver` on Robolectric).
  It holds reading progress and per-manga reader mode. It is not a cache, so schema changes need real migrations.
- `core:data`: `ReadingRepository` (progress, last read, reader mode); `MangaRepository` (Browse sections, paged search, suggestions, details, full chapter feed, tags).
- Cover theming: screens call `PublishCover(url)`. `M3MangaDexApp` provides `LocalCoverHolder` and wraps the shell
  in `CoverTheme(holder.current)`. Don't wrap individual screens in `CoverTheme` (that leaves the nav bar off-palette).
- `feature:reader`: pages are Coil painters (`pageRequest`: original size, optional `CropBordersTransformation`) shown
  by `ZoomablePage` (`Modifier.zoomable`; the fit mode is `ZoomableState.contentScale`). Spreads use `SpreadPainter`.
  The rest: `PagedReader` (pagers + telephoto zoom + spreads), `WebtoonReader` (LazyColumn strip),
  `ReaderCommand` (jump/step requests from the chrome, tap zones and volume keys), `PreloadPages`.
  `ReaderKey` hides the navigation suite.
- `feature:{browse,search,manga,settings}`: one `Route` (Hilt VM; assisted factory when it takes nav args) plus a
  stateless `Screen` each, with a Roborazzi screenshot of the `Screen`.
- Navigation keys live in `app/.../navigation/M3MangaDexApp.kt`. `MangaKey` is the detail pane, and the
  others are list panes.
- Cover transitions: `Modifier.sharedElement(SharedKeys.cover(id, scope))` (core:designsystem) on the list's
  cover and on the details header. `MangaKey.coverScope` carries the tapped list's scope. Destinations that
  take part are wrapped in `Destination { }`. Don't pass `sharedTransitionScope` to `NavDisplay` (whole
  entries become shared and the list never exits), and keep list-detail scenes off compact widths (nothing
  transitions inside one scene). Feed new lists through `DefaultMangaRepository.rememberPreviews`, so the
  details header has a target on its first frame.

- `app`: Activity, Coil `ImageLoader` (on the image client) and navigation shell (`navigation/M3MangaDexApp.kt`). It uses `NavigationSuiteScaffold`,
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

## Background work

- Workers (`core:data`: `download/DownloadWorker`, `notify/NewChaptersWorker`) get dependencies through Hilt
  `@EntryPoint`s, as in xmuks, so no custom WorkerFactory. Downloads are a `dataSync` foreground service; the
  service type is declared in `core:data`'s manifest.
- Notifications open the launcher activity with `AppNotifications.EXTRA_MANGA_ID` / `EXTRA_CHAPTER_ID`;
  `MainActivity` turns those into an `OpenRequest` for navigation.
- Run `spotlessApply` before scripted edits. It reflows code, and exact-match replacements then silently miss.

## Account

- `core:auth`: `AuthRepository` is the session and the `AccessTokenProvider` for the API client. Mark user requests
  with `.authenticated()` (`ApiCaller.kt`). Only those carry the token.
- Live tests: development credentials are in `~/m3mangadex-signing/mangadex-test.env` (never commit or print them).
  Login is limited to about 30 per hour, so don't retry in loops. Clean up test state (status, follow, read markers)
  afterwards.

## Checks

```bash
./gradlew spotlessApply   # format
./gradlew spotlessCheck detekt lintDebug testDebugUnitTest verifyRoborazziDebug assembleDebug -PwarningsAsErrors=true
./gradlew recordRoborazziDebug   # after an intended UI change, then commit the PNGs
```
