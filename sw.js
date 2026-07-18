/* BlackAmp service worker
   - caches the app shell so it opens instantly / offline
   - caches songs as they're played, capped so storage doesn't blow up,
     so a dropped signal mid-drive doesn't kill playback
*/
const SHELL_CACHE = 'blackamp-shell-v1';
const AUDIO_CACHE = 'blackamp-audio-v1';
const MAX_CACHED_SONGS = 25;

const SHELL = [
  './',
  './index.html',
  './playlist.js',
  './manifest.json',
  './Winamp-logo.svg.png'
];

self.addEventListener('install', e => {
  e.waitUntil(
    caches.open(SHELL_CACHE)
      .then(c => c.addAll(SHELL).catch(() => {/* tolerate a missing file */}))
      .then(() => self.skipWaiting())
  );
});

self.addEventListener('activate', e => {
  e.waitUntil(
    caches.keys()
      .then(keys => Promise.all(
        keys.filter(k => k !== SHELL_CACHE && k !== AUDIO_CACHE)
            .map(k => caches.delete(k))
      ))
      .then(() => self.clients.claim())
  );
});

// keep the audio cache from growing without bound
async function trimAudioCache() {
  const cache = await caches.open(AUDIO_CACHE);
  const keys = await cache.keys();
  if (keys.length > MAX_CACHED_SONGS) {
    for (const k of keys.slice(0, keys.length - MAX_CACHED_SONGS)) {
      await cache.delete(k);
    }
  }
}

self.addEventListener('fetch', e => {
  const req = e.request;
  if (req.method !== 'GET') return;

  const url = new URL(req.url);
  if (url.origin !== self.location.origin) return;   // don't touch the GitHub API

  // ----- audio: cache-first, then network, and store a copy -----
  if (/\.mp3$/i.test(url.pathname)) {
    e.respondWith((async () => {
      const cache = await caches.open(AUDIO_CACHE);
      const hit = await cache.match(req, { ignoreVary: true });
      if (hit) return hit;
      try {
        const res = await fetch(req);
        // only cache full 200 responses (range requests come back 206)
        if (res.ok && res.status === 200) {
          cache.put(req, res.clone()).then(trimAudioCache).catch(() => {});
        }
        return res;
      } catch (err) {
        const fallback = await cache.match(req, { ignoreVary: true });
        if (fallback) return fallback;
        throw err;
      }
    })());
    return;
  }

  // ----- shell: network-first so updates land, cache as fallback -----
  e.respondWith((async () => {
    try {
      const res = await fetch(req);
      if (res.ok) {
        const cache = await caches.open(SHELL_CACHE);
        cache.put(req, res.clone()).catch(() => {});
      }
      return res;
    } catch (err) {
      const hit = await caches.match(req);
      if (hit) return hit;
      throw err;
    }
  })());
});
