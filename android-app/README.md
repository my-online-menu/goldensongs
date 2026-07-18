# BlackAmp (Android)

A Winamp-styled Android music player that streams MP3s straight from **public GitHub repos**.
Add any repo by URL and every `.mp3` inside it becomes playable — with real background
playback that survives navigation apps and a locked screen.

## Features
- **Add any public GitHub repo** by pasting its URL (`github.com/user/repo`)
- **Multiple repos** at once, each refreshable independently
- **Real background playback** — a media foreground service with lock-screen and
  notification controls, so it keeps playing behind Maps or with the screen off
- **BlackAmp player skin** — the classic green-on-black Winamp look
- **Playlists** — create, rename, delete, add/remove songs
- **Backup** — export playlists + repos to a JSON file, import to restore

## Installing
Grab the newest **BlackAmp.apk** from the
[Releases page](../../releases/tag/latest) and open it on your phone.
You'll be asked to allow installs from unknown sources the first time — that's
expected for an APK not distributed through the Play Store.

## How it finds songs
It calls the GitHub **Git Trees API** once per repo:

```
https://api.github.com/repos/{owner}/{repo}/git/trees/{branch}?recursive=1
```

That returns the entire file listing in a single request. Anything ending in
`.mp3` is turned into a track and streamed from `raw.githubusercontent.com`,
which supports HTTP range requests so seeking works properly.

Branch detection falls back between `main` and `master` automatically.

## Building
CI builds it for you — every push to `main` runs `.github/workflows/build.yml`,
which produces the APK and publishes it to the `latest` release.

To build locally you need JDK 17 and the Android SDK:

```bash
gradle assembleDebug
# -> app/build/outputs/apk/debug/app-debug.apk
```

## Notes and limits
- **Only public repos.** No auth is implemented, so private repos won't load.
- **GitHub rate limits** unauthenticated API calls to 60/hour per IP. The app
  calls once per repo refresh, so this is rarely an issue.
- **GitHub isn't a CDN.** Their terms discourage using repos for bulk media
  hosting. Fine for personal use; if it grows, move the files to object storage
  (Cloudflare R2, Backblaze B2) and point the app at those URLs instead.
