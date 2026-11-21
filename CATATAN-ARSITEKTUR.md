# Catatan Arsitektur — Onimeku (18 Okt 2026)

> **PEMBARUAN ARSITEKTUR — 6 Nov 2026 (berlaku penuh, menggantikan uraian lama di dokumen ini):**
> - API katalog lama **dibuang total** dari aplikasi. Katalog/detail/putar kini dari katalog gudang
>   terenkripsi AES-256-GCM (Pages, jalur tersamar khusus Onimeku) + cermin polos jsDelivr/gcore/raw/fastly;
>   kunci katalog dipecah dua (potongan A di aplikasi, potongan B di loker Firebase) — lihat `data/Api.kt` + `data/KeyLocker.kt`.
> - Backend akun **Supabase dibuang penuh**, diganti Firebase proyek khusus Onimeku (REST):
>   Auth = identitytoolkit (`data/Auth.kt`), sinkronisasi = dokumen Firestore `user_state/<uid>` (`data/Sync.kt`),
>   komentar = koleksi Firestore `comments` (`data/Comments.kt`). Konfigurasi: `ONIMEKU_FIREBASE_PROJECT_ID` +
>   `ONIMEKU_FIREBASE_API_KEY` -> BuildConfig; kosong = fitur Firebase nonaktif anggun.
> - Semua uraian lama di bawah yang menyebut API katalog lama / Supabase adalah catatan sejarah dan TIDAK berlaku lagi.


[SEJARAH — sudah digantikan pembaruan 6 Nov 2026 di atas] Perintah user lama: aplikasi Kotlin memakai API katalog lama LANGSUNG, tanpa
perantara. Projek Flutter sudah dihapus permanen
(lokal + repo GitHub) — jangan dihidupkan lagi tanpa perintah user.

## Aplikasi (Onimeku-Kotlin-Terbaru.zip)
- BASE lama (sudah tidak ada di kode): alamat API katalog lama (data/Api.kt versi lama)
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
Ekstrak dari proyek API lama (Next.js di Cloudflare Workers) — sejarah:
src/app/api (route anime/proxy/img/dll), src/lib (store.ts = katalog
Turso-utama/D1-cadangan), src/providers, wrangler.jsonc, package.json.
Inilah API yang sekarang dipakai aplikasi secara langsung.


## Pemutar (debug.74)
- `ui/PlayerHolder.kt`: satu ExoPlayer + MediaSession di luar composition. `acquire(ctx, token)` dipanggil di `PlayerReady` (dalam `remember(r, streamUrl)`), `release(token)` di `onDispose`. Composable episode baru memanggil `acquire` SEBELUM yang lama dibuang, jadi semua efek/listener lama wajib memeriksa `PlayerHolder.isOwner(token)` sebelum menyimpan progres atau mengubah keadaan.
- `PlayerReady` membuat `MediaSource` per episode lewat `DefaultMediaSourceFactory` (data source OkHttp/Referer/cache/gudang) dan memasangnya dengan `setMediaSource`; pabrik yang sama disimpan di `srcFactory` untuk pemasangan ulang subtitle. Jangan memanggil `player.setMediaItem` langsung: pemutar bersama tidak punya pabrik sumber bawaan yang sesuai.
- Logika murni yang diuji unit ada di `ui/PlayerLogic.kt` (rantai cadangan, galat sesaat, pengawas macet, hitungan episode, skip) dan `data/LogRedact.kt`.

## Subtitle putar gudang (debug.75)
- Trek subtitle satu episode = rendition `#EXT-X-MEDIA:TYPE=SUBTITLES` pada playlist master, dibaca `data/SubRenditions.kt` (murni + teruji di `SubRenditionsTest`). `ApiClient.buildPlayback` memasukkannya ke `Playback.subtitles` sebagai **trek samping**; unduhan luring memakai daftar rendition yang sama untuk memilih `en`/`id`.
- **Jangan mengosongkan `Playback.subtitles` untuk gudang.** Daftar kosong berantai: menu "Not available" → tombol CC hilang → `applySub(-1)` mematikan seluruh tipe trek teks. URI rendition gudang berupa `.vtt` mentah (bukan media playlist WebVTT), jadi parser HLS Media3 menolaknya; satu-satunya jalur yang menghasilkan cue adalah `SubtitleConfiguration` samping (`LOCAL_SUB_ID_PREFIX`).
- Jalur "native" (grup teks bawaan manifest) hanya dipakai bila tidak ada trek samping sama sekali: `nativeNow()` = gudang daring tanpa `playback.subtitles` dan tanpa geseran waktu. Di jalur itu preferensi `subLang` diterjemahkan ke `setPreferredTextLanguage`, dan teks hanya dimatikan bila preferensinya "off".

## Amendemen 1.0.3 — pemutar diganti total ke mpv, Media3 dihapus
- Mesin putar TUNGGAL kini mpv lewat libmpv (`io.github.midasvo:libmpv:1.2.0`), untuk streaming DAN luring. Seluruh dependensi `androidx.media3.*` dihapus; `grep media3` di kode harus nol (kecuali catatan sejarah seperti ini).
- `ui/PlayerHolder.kt`: satu `MpvEngine` (pembungkus `MPVLib`) di luar composition; semantik token `acquire/isOwner/release` sama seperti era ExoPlayer. MediaSession + `PlaybackService` DIBUANG: tidak ada kontrol notifikasi/layar kunci; pemutaran tidak berlanjut tanpa UI (sesuai catatan PlaybackService lama sendiri).
- Subtitle: mpv membaca trek teks bawaan playlist induk HLS secara native (parser HLS mpv/FFmpeg menerima rendition `.vtt` mentah yang dulu ditolak Media3). Bila `Playback.subtitles` terisi (sumber non-gudang/luring), trek dipasang sebagai trek samping lewat `sub-add`. Pilihan trek = properti `sid`; gaya = properti libass (`sub-scale`, `sub-color`, latar kotak); geseran waktu = `sub-delay` (berkas tidak lagi ditulis ulang). Kualitas = batas `hls-bitrate` + muat ulang di posisi sama.
- Rantai pintu: balapan DataSource (DoorRace) dibuang. Galat muat mpv bersifat terminal → `PlaybackChain.onFatal` di `PlayerContent` memindahkan `loadfile` ke pintu berikutnya dari posisi yang sama; pintu terakhir-berhasil (`GudangStore`) tetap pintu awal. `PlaybackChain.isTransient` dihapus (pemulihan jaringan sesaat kini di dalam mpv).
- Unduhan: DownloadManager + SimpleCache Media3 diganti unduh BERKAS polos (`data/Downloads.kt`): playlist varian + segmen diunduh ke `filesDir/downloads/<id>/<ep>/`, playlist lokal ditulis ulang ke nama segmen lokal, penanda `complete` ditulis terakhir. Putar luring = mpv membuka playlist lokal itu. Resume di tingkat segmen; unduhan berjalan di proses aplikasi (berhenti bila proses dimatikan). Cache SimpleCache lama dibuang sekali jalan (`Downloads.cleanupLegacyCache`).
