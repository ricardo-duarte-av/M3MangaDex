# Play listing material

- `en-US/` and `pt-PT/`: title, short description and full description, in fastlane `supply` layout,
  ready to paste into the Play Console.
- `graphics/icon-512.png` (store icon) and `graphics/feature-graphic-1024x500.png`.
- Privacy policy: [`docs/privacy-policy.md`](../privacy-policy.md). Play needs a URL; the file's
  GitHub link works:
  <https://github.com/ricardo-duarte-av/M3MangaDex/blob/main/docs/privacy-policy.md>

## Screenshots

Take them on a device. Prefer titles whose covers are harmless and widely used, and avoid any page
art: manga artwork is the publishers' copyright, and listings that feature it are a common reason
for rejection. Settings, Library (with your own titles), the Downloads screen and the reader
chrome over a title page all work.

## Content rating questionnaire (IARC)

- Category: Reference, news or educational? No. It's a reader of user-chosen content:
  pick "All other app types".
- User-generated or shared content: the app shows third-party content (MangaDex) that the developer
  doesn't control. Answer *yes* to "shows content not created by the developer".
- Sexuality: the Play build is limited to Safe and Suggestive. Suggestive titles can contain
  sexual innuendo and partial nudity in artwork, so answer accordingly (expect a Teen / PEGI 12–16 rating).
- Violence: manga can contain cartoon and realistic violence. Answer yes to the depiction of violence.
- No in-app purchases, no gambling, no location sharing, no user-to-user communication.

## Data safety form (suggested answers; you are responsible for the final submission)

- **Does the app collect or share user data?** The app sends data only to MangaDex, which is required
  for its core function, and the developer receives nothing.
  - *Account info (username) and authentication (password, API client credentials):* sent to
    MangaDex when the user chooses to log in. Mark them **collected**, not shared, **not optional
    for login** (login itself is optional), and **encrypted in transit**. Purpose: *Account management*.
  - *App activity (pages read, library changes):* synced to the user's MangaDex account when logged
    in. Mark it **collected**, purpose *App functionality*.
  - No location, contacts, photos, messages, financial or health data. No analytics or advertising IDs.
- **Is all data encrypted in transit?** Yes (HTTPS only).
- **Can users request deletion?** Yes. The app's data is deleted by clearing it or uninstalling. Data in
  the MangaDex account is managed by MangaDex.
