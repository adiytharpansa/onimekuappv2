# Catatan Arsitektur — Onimeku (18 Okt 2026)

Perintah user: aplikasi Kotlin memakai API my-anime-app LANGSUNG, tanpa
perantara onimekuappv2-api. Projek Flutter sudah dihapus permanen
(lokal + repo GitHub) — jangan dihidupkan lagi tanpa perintah user.

## Aplikasi (Onimeku-Kotlin-Terbaru.zip)
- BASE = https://my-anime-app.adiyth-arpansa.workers.dev (data/Api.kt)
- Kontrak: GET /api/anime -> {ok, data:[ringkasan]} (satu panggilan, tanpa
  paginasi); GET /api/anime?id=<slug> -> detail; GET /api/anime?id=&ep= -> putar.
- Tugas yang pindah ke klien (dulu di API perantara), semua di ApiClient:
  * Stream CDN nexabloom (MegaPlay) dibungkus proksi media hulu:
    streamUrl = $BASE/api/proxy?u=<url asli>&ref=<akar origin referer>;
    directUrl = URL asli; pageUrl = referer bila berisi /stream/;
    referer dinormalisasi ke akar origin (CDN menolak referer halaman penuh).
  * Subtitle hulu berbentuk {label, lang, url} -> label dipakai bila lang kosong.
  * Hulu TIDAK punya endpoint jadwal/poster. Jadwal tayang 7 hari diambil
    langsung dari AniList (graphql.anilist.co) lewat fetchSchedule(): coba
    ulang 3x (galat jaringan/429/5xx, hormati Retry-After), duplikat dibuang,
    salinan disimpan di cacheDir/schedule_cache.json dan ditampilkan seketika
    saat jaringan lambat/gagal. resolvePoster() tetap null (poster katalog
    apa adanya; penyembuhan poster ada di server).
- Cache katalog di disk berganti nama: catalog_cache_direct.json.

## API (Onimeku-API-Terbaru.zip)
Ekstrak dari proyek my-anime-app (Next.js di Cloudflare Workers):
src/app/api (route anime/proxy/img/dll), src/lib (store.ts = katalog
Turso-utama/D1-cadangan), src/providers, wrangler.jsonc, package.json.
Inilah API yang sekarang dipakai aplikasi secara langsung.


## Pemutar (debug.74)
- `ui/PlayerHolder.kt`: satu ExoPlayer + MediaSession di luar composition. `acquire(ctx, token)` dipanggil di `PlayerReady` (dalam `remember(r, streamUrl)`), `release(token)` di `onDispose`. Composable episode baru memanggil `acquire` SEBELUM yang lama dibuang, jadi semua efek/listener lama wajib memeriksa `PlayerHolder.isOwner(token)` sebelum menyimpan progres atau mengubah keadaan.
- `PlayerReady` membuat `MediaSource` per episode lewat `DefaultMediaSourceFactory` (data source OkHttp/Referer/cache/gudang) dan memasangnya dengan `setMediaSource`; pabrik yang sama disimpan di `srcFactory` untuk pemasangan ulang subtitle. Jangan memanggil `player.setMediaItem` langsung: pemutar bersama tidak punya pabrik sumber bawaan yang sesuai.
- Logika murni yang diuji unit ada di `ui/PlayerLogic.kt` (rantai cadangan, galat sesaat, pengawas macet, hitungan episode, skip) dan `data/LogRedact.kt`.
