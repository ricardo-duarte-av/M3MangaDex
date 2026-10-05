# M3MangaDex privacy policy

*Last updated: 5 October 2026*

M3MangaDex is an unofficial, open-source reader for [MangaDex](https://mangadex.org). It has no
servers of its own, no analytics, no advertising and no crash reporting. Its developer receives no
data about you.

## What the app sends, and to whom

All of it goes to MangaDex, which the app needs in order to work:

- **Browsing and reading:** requests to `api.mangadex.org` and MangaDex's image servers (including
  MangaDex@Home nodes). Like any internet request, they carry your IP address, plus a User-Agent
  that names the app and its version.
- **Image load reports:** MangaDex asks apps to report how image loads from MangaDex@Home went, so
  it can route around slow servers. Each report contains the image's address, whether it loaded,
  its size and how long it took. Reports go to `api.mangadex.network` and contain nothing about you.
- **Logging in (optional):** your MangaDex username, password, and personal API client ID and secret
  go to MangaDex's login server, `auth.mangadex.org`. Your library, follows and read-chapter markers
  then sync with your MangaDex account.

MangaDex's own [privacy policy](https://mangadex.org/about/privacy) covers what it does with these
requests.

## What stays on your device

- Your reading progress, preferences, per-manga settings and downloaded chapters.
- When logged in: your client ID, client secret and username, plus the login tokens MangaDex issues.
  These are encrypted with a key held in the Android Keystore and are excluded from backups. **Your
  password is never stored.**

Clearing the app's data or uninstalling it removes all of this. Logging out removes the tokens.

## Notifications and background work

If you turn on new-chapter notifications, the app checks your MangaDex follows feed every couple of
hours. Downloads run in the background until they finish. Both talk only to MangaDex.

## Children

The app is not directed at children. The version on Google Play only shows titles rated Safe or
Suggestive.

## Contact

Use the issue tracker at <https://github.com/ricardo-duarte-av/M3MangaDex/issues>.
