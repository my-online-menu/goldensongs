# BlackAmp

A Winamp BlackAmp-style web music player. Single `index.html`, no build step.
Auto-loads every `.mp3` sitting next to it from a generated `playlist.js` manifest.

## Run locally
Double-click `index.html`. (Music plays; the spectrum visualizer only fully
works when served over http/https — see below.)

## Update the playlist
Whenever you add or remove `.mp3` files, regenerate the manifest:

- **Windows:** right-click `generate-playlist.ps1` → *Run with PowerShell*

Then refresh the page. You can also use the **OPEN FOLDER** / **ADD FILES**
buttons, or drag MP3s onto the player, to load tracks ad-hoc without regenerating.

## Deploy to GitHub Pages
1. Create a repo and add these files plus your `.mp3` files:
   `index.html`, `playlist.js`, `.nojekyll`, and the MP3s.
2. Push to GitHub.
3. Repo **Settings → Pages → Build and deployment → Deploy from a branch**,
   pick `main` and `/ (root)`, Save.
4. Your player goes live at `https://<username>.github.io/<repo>/`.

### Limits to know
- Max **100 MB per file** (hard limit), repo ideally under **1 GB**.
- Pages bandwidth soft limit is **100 GB/month**.
- If your MP3s are too large for a Git repo, host them elsewhere and point the
  `playlist.js` entries at full URLs instead of filenames.

## Controls
- Space = play/pause · ←/→ = prev/next · ↑/↓ = volume
- SHUF = shuffle · REP cycles off → all → one
- Type in the filter box to search the playlist (Esc clears)
