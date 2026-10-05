# M3MangaDex

A native Android client for [MangaDex](https://mangadex.org), built with Material 3 Expressive.
It's a comic reader for phones, tablets and foldables, and it takes its colours from manga cover art.

Status: **M4 (account & sync)**. Browse, search and read anonymously; log in with a MangaDex personal API client to get your library, follows, an Updates feed and read markers synced with MangaDex. See [docs/MILESTONES.md](docs/MILESTONES.md) for the roadmap.

- Anonymous browsing and reading, plus optional login with your own MangaDex
  [personal API client](https://api.mangadex.org/docs/02-authentication/personal-clients/)
- Adaptive layout: bottom bar on phones, rail or drawer on larger screens, list beside detail on tablets
- Dynamic colour, with schemes derived from cover art (M3)

## Building

Requires JDK 21 and the Android SDK with `platforms;android-37.1` and `build-tools;37.0.0`.

```bash
./gradlew assembleDebug
./gradlew spotlessCheck detekt lintDebug testDebugUnitTest verifyRoborazziDebug -PwarningsAsErrors=true
```

Releases: see [RELEASE.md](RELEASE.md).

Not affiliated with MangaDex. Content is provided by MangaDex and its scanlation groups.
