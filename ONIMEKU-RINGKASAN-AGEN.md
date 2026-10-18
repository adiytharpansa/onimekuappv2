# Onimeku — Ringkasan & Panduan AI Agent

> Serah-terima untuk AI agent berikutnya. Merangkum `ONIMEKU-AGENT-GUIDE.md`, `CATATAN-ARSITEKTUR.md`, `CHANGELOG.md`, hasil tinjauan kode (18 Okt 2026), dan perbaikan bug terbaru. Paket: `com.onimeku.app` · Versi: 1.0.0 · Gradle 8.9, JDK 17, compileSdk/targetSdk 35, minSdk 26

**Status verifikasi:** seluruh kode (termasuk perbaikan di bagian 6) ditulis/ditinjau tanpa Android SDK. **Belum dikompilasi, belum dijalankan, belum diukur.** Langkah pertama agent: `gradle :app:assembleDebug` dan `gradle :app:testDebugUnitTest`, perbaiki galat impor/tipe bila ada, lalu jalankan `emulator-test.yml`.

---

## 1. Apa ini

Aplikasi Android native untuk menonton anime: Kotlin + Jetpack Compose (Material 3) + Media3 (ExoPlayer/HLS) + OkHttp + Coil + WorkManager.

| Bagian | Teknologi | Lokasi |
| --- | --- | --- |
| Aplikasi | Kotlin, Compose, Media3 | `app/` |
| API katalog | Next.js di Cloudflare Workers (Turso utama / D1 cadangan) | proyek terpisah `my-anime-app` |
| Akun & sinkronisasi | Supabase Auth + tabel `user_state` (RLS) + Edge Function | `backend/` |

**Keputusan pemilik yang wajib dihormati**

- BASE API: `https://my-anime-app.adiyth-arpansa.workers.dev` (`data/Api.kt`). Jangan menambah API perantara tanpa perintah.
- Proyek Flutter lama dihapus permanen; jangan dihidupkan.
- Tidak ada data contoh; semua konten dari API.
- Aplikasi publik, tampil profesional; pengguna tidak boleh memasukkan sumber video sendiri.
- Konten harus berlisensi milik pemilik proyek.

---

## 2. Arsitektur

```
MainActivity -> App(vm) -> AnimatedContent(route)
  Home / Calendar / MyList / Downloads / ProfileTab
  Detail / SeeAll / Search / Notif / Sub / Auth / Player
AppVM (satu-satunya ViewModel)
  state: AppState (SharedPreferences "onimeku", JSON kotlinx.serialization)
  navigasi: stack List<Route> (tanpa Navigation Compose)
  katalog: object Catalog (state global + cache turunan)
  unduhan: Downloads (Media3 DownloadManager)
```

### Peta file (`app/src/main/java/com/onimeku/app/`)

| File | Isi |
| --- | --- |
| `OnimekuApp.kt` | Application, Coil global (memori 20%, disk 256 MB), pasang `CrashLog` |
| `MainActivity.kt` | Activity, edge-to-edge, PiP, izin notifikasi, `App()`, TopBar, TabBar, toast |
| `AppVM.kt` | `Route`, navigasi, My List, progres, unduhan, profil, akun, sinkronisasi |
| `Notifier.kt`, `Reminders.kt` | Notifikasi + pengingat episode mingguan (WorkManager) |
| `PlaybackService.kt` | `MediaSessionService` (notifikasi/layar kunci/latar); sesi dibuat di `PlayerScreen` |
| `data/Api.kt` | `ApiClient` (katalog, detail, playback, proksi stream, prefetch) + model |
| `data/Net.kt` | Satu `OkHttpClient` bersama + cookie jar per host |
| `data/Auth.kt` | `AuthClient` (Supabase GoTrue REST) |
| `data/Sync.kt` | Sinkronisasi My List/progres ke Supabase (merge union, maks 4 profil) |
| `data/State.kt` | `AppState`, `Profile`, `Settings`, `Account`, `Store` (token di EncryptedSharedPreferences) |
| `data/Catalog.kt` | Model `Anime`, `Catalog` (daftar turunan di-cache per perubahan) |
| `data/Downloads.kt` | Unduhan HLS offline + `OnimekuDownloadService` |
| `data/CrashLog.kt` | Log lokal `diagnostics.log` (maks ±64 KB) |
| `data/PosterHealer.kt` | Pemulihan URL poster rusak |
| `ui/Theme.kt`, `Components.kt`, `Motion.kt` | Token warna, komponen, animasi |
| `ui/Screens.kt` | Home, Calendar, My List, Downloads, Detail, SeeAll, Notif, Search |
| `ui/PlayerScreen.kt` | Pemutar (terbesar, ±44 KB) |
| `ui/ProfileScreens.kt`, `AuthScreen.kt`, `LegalScreens.kt`, `WelcomeScreen.kt` | Profil/Settings, Sign in/up, Terms/Privacy/Licenses, Welcome |

### Navigasi

`sealed interface Route`: `Home, Calendar, MyList, Downloads, ProfileTab, Sub(key), Detail(id), SeeAll(key), Notif, Search, Player(id, ep), Auth(signUp)`. `go` dorong · `back` pop · `setTab` reset · `replaceTop` ganti. TabBar disembunyikan di `Player` dan `Auth`. Kunci `Sub`: `settings, language, help, about, terms, privacy, licenses`. Kunci `SeeAll`: `top, new, airing, popular, latest, donghua, all, genre:<nama>`.

### State

```kotlin
AppState(welcomeSeen, downloads, notifRead, hist, fired, settings, profiles, pi, account)
Profile(name, myList, prog: Map<"id:ep", detik>, seen, last, base, remind)
Settings(autoplay, skipIntro, wifi, alerts, quality, bgPlay)
Account(userId, email, accessToken, refreshToken)   // null = tamu
```

Ubah state hanya lewat `upd { it.copy(...) }`; data per-profil lewat `updP`. `ignoreUnknownKeys = true` sehingga menambah field aman.

---

## 3. API & pemutaran

| Endpoint | Fungsi |
| --- | --- |
| `GET /api/anime` | Seluruh katalog satu respons (±900 KB, tanpa paginasi) |
| `GET /api/anime?id=<slug>` | Detail + episode |
| `GET /api/anime?id=<slug>&ep=<n>` | Playback (`streamUrl`, `subtitles`, `referer`) |
| `GET /api/proxy?u=&ref=` | Proksi media hulu |
| `GET /api/img` | Proksi gambar |

- Batas laju per IP: 600 GET/menit, 120 POST/menit.
- Stream CDN nexabloom (MegaPlay) dibungkus: `streamUrl = $BASE/api/proxy?u=<asli>&ref=<akar origin>`; `directUrl` = URL asli. **Referer wajib akar origin** (CDN menolak referer halaman penuh) — dipaksa `https://megaplay.buzz/` untuk host `nexabloom.top`.
- Rantai cadangan putar: URL utama (proksi) -> `directUrl` -> pemutar embed (`EmbedPlayer`).
- Subtitle diunduh paralel ke `cacheDir/subs` (batas total 4 dtk), diputar dari file lokal.
- Cache unduhan dibaca pemutar tetapi **tidak ditulis** (`setCacheWriteDataSinkFactory(null)`); kunci cache stabil `dl:<id>:<ep>`.
- Unduhan hanya untuk stream `.m3u8` langsung; judul embed-saja ditolak.
- Offline: bila API gagal dan episode sudah terunduh penuh, diputar dari cache unduhan.
- Autoplay: data episode berikutnya di-prefetch pada 60 dtk terakhir (TTL 120 dtk, sekali pakai).
- `fetchSchedule()` kosong dan `resolvePoster()` null (hulu tidak punya endpoint itu).
- Cache katalog disk: `filesDir/catalog_cache_direct.json`.

### Fitur pemutar

Fullscreen/landscape, kontrol auto-sembunyi 3,5 dtk, ketuk dua kali ±10 dtk, geser kiri/kanan = kecerahan/volume, pilih subtitle (default Indonesia -> Inggris -> pertama), kualitas, trek audio, Up next (20 dtk terakhir), daftar episode, timer tidur, kunci kontrol, kecepatan 0,5–2×, PiP, pemutaran latar + MediaSession, saran turunkan kualitas (3x macet/90 dtk), simpan progres tiap 10 dtk.

---

## 4. Akun & backend

- `AuthClient`: `/auth/v1/token?grant_type=password|refresh_token`, `/auth/v1/signup`, `/auth/v1/recover`, `/auth/v1/logout`. Hanya kunci PUBLIK di klien; jangan pernah `service_role`.
- Konfigurasi: `ONIMEKU_SUPABASE_URL`, `ONIMEKU_SUPABASE_KEY` (gradle.properties atau secret CI) -> `BuildConfig`. Kosong = mode tamu, fitur akun nonaktif anggun.
- `AppVM.withAccount`: retry sekali dengan refresh token saat 401. `scheduleSync` debounce 15 dtk. `deleteAccount` memanggil Edge Function.
- `backend/supabase/schema.sql` (tabel `user_state` + RLS per `auth.uid()`) dan `functions/delete-account` **harus dideploy** sebelum fitur akun berfungsi.
- Sinkronisasi bersifat union: penghapusan di satu perangkat bisa muncul lagi dari perangkat lain (by design).

---

## 5. Sistem desain

| Token | Hex | Pakai untuk |
| --- | --- | --- |
| `Red` | `#A6122D` | Isian/latar: tombol Play, chip aktif, switch, slider, avatar, logo |
| `RedDeep` | `#6E0F1F` | Gradasi/aksen gelap |
| `RedText` | `#E8566F` | Teks/ikon merah di atas latar gelap |
| `Bg` / `Bg2` | `#14161F` / `#1B1E2A` | Latar / kartu |
| `Line` | `#2A2D3B` | Garis |
| `InkSoft` / `NavOff` | `#C9CCD8` / `#9EA4B8` | Teks sekunder / nonaktif |

Aturan: jangan menulis `Color(0xFF…)` merah langsung; radius kartu 18 dp, poster ringkas 12 dp, tombol/chip pill; padding halaman 16 dp; area sentuh min 40–44 dp. Pakai komponen dari `Components.kt` (`Page, SubHead, CardBox, Btn, Chip, IconChip, EmptyBox, Poster/PosterCard/Tile, AnimeGrid, Rail, SectionHead`). Animasi: `fadeSlideIn`, `pressable`, `shimmer`; jangan menambah animasi berat di daftar panjang.

---

## 6. Perbaikan bug terbaru (18 Okt 2026)

Hasil tinjauan statis; perubahan di `Reminders.kt` (**belum dikompilasi**):

1. **Pengingat hanya berbunyi sekali.** `ReminderWorker` menjadwalkan ulang lewat `Catalog.byId[id]`, padahal katalog kosong di proses worker -> jadwal minggu depan diam-diam batal sampai aplikasi dibuka. Perbaikan: `Reminders.schedule(ctx, pi, id, anime: Anime? = null)`; worker meneruskan detail hasil `fetchDetail`.
2. **Crash saat `time` kosong.** `delayMs` memakai `time.split(":").map { it.toInt() }`; bila `updatedAt` tak terbaca `time == ""` -> exception (terpicu dari `rescheduleAll` di akhir `reloadCatalog`). Perbaikan: parsing aman, default 09:00.

### Temuan yang BELUM diubah (butuh keputusan pemilik)

- `allowBackup="true"` + `EncryptedSharedPreferences`: saat restore/pindah HP kunci Keystore tidak ikut, token hilang; `Store` lalu jatuh ke prefs biasa. Usul: `allowBackup="false"` atau kecualikan `onimeku_secure` dari backup.
- Fullscreen terlepas saat pindah episode (state `full` hilang karena pemutar dibongkar saat memuat).
- `downloadAll` meluncurkan `fetchPlayback` paralel per episode (berat untuk ratusan episode).
- Jalur utama MegaPlay lewat proksi worker menambah satu lompatan dan kena batas 600 GET/menit/IP.
- Teks rilis `build.yml` masih menyebut `onimekuappv2-api` (usang).

### File yang belum ditinjau

`Screens.kt`, `ProfileScreens.kt`, `MainActivity.kt`, `Components.kt`, `Motion.kt`, `AuthScreen.kt`, `WelcomeScreen.kt`, `LegalScreens.kt`, `PosterHealer.kt`, `Notifier.kt`.

---

## 7. Pekerjaan tersisa (urut prioritas)

1. Kompilasi + uji unit + uji emulator (belum ada build setelah banyak perubahan).
2. Isi kunci Supabase; atur Auth di dashboard (redirect URL, konfirmasi email, batas laju); deploy `backend/`.
3. Putuskan soal `allowBackup` dan temuan di bagian 6.
4. Uji build rilis ber-R8 (`bundleRelease`) di perangkat: R8 sering merusak serialisasi/refleksi diam-diam.
5. Ukur performa (Macrobenchmark: startup dingin, jank Home, waktu Play -> frame pertama); ganti baseline profile tulis-tangan dengan hasil ukur.
6. Bahasa Indonesia/日本語 masih "Coming soon"; pindahkan string ke `strings.xml` dulu.
7. Teks legal: tinjau penasihat hukum, tambah kontak + URL kebijakan publik untuk Play Console.
8. Pull-to-refresh Home dimatikan (crash `PullToRefreshBox` di emulator CI); `vm.refresh()` masih ada.
9. Selaraskan `README.md` dan `build.yml` (masih menyebut API lama / Firebase).

> Catatan: butir 3–5 di bagian 8 panduan lama (hapus akun, token terenkripsi, sinkronisasi) **sudah ada di kode**; tinggal deploy backend dan uji.

---

## 8. Aturan kerja untuk agent

1. Baca `CATATAN-ARSITEKTUR.md` dan dokumen ini dulu. Jangan ubah BASE API atau tambah perantara tanpa perintah.
2. Ubah sekecil mungkin; ikuti gaya yang ada (satu `AppVM`, `Route` sealed, `upd/updP`).
3. Warna hanya dari `Theme.kt`; komponen dari `Components.kt`.
4. Daftar panjang wajib lazy (`LazyColumn`, `LazyVerticalGrid`, `LazyRow`).
5. Jangan hitung ulang turunan katalog di composable; tambahkan ke `Catalog.Derived`. Jangan pakai `!!` pada `Catalog.byId[...]`.
6. Satu `OkHttpClient` bersama (`Net.client`); jangan buat klien baru untuk media.
7. Tidak ada data/gambar palsu; tampilkan shimmer selama memuat.
8. Tanpa rahasia di repo; hanya kunci publik di klien.
9. Fitur baru wajib punya keadaan loading, kosong, dan galat.
10. Route baru: daftarkan di `when (r)` pada `App()` dan putuskan apakah TabBar tampil.
11. Setiap perubahan: perbarui `CHANGELOG.md` dan dokumen ini; **jujur soal apa yang belum dikompilasi/diuji**.

## 9. Perintah

```bash
gradle :app:assembleDebug          # APK debug
gradle :app:testDebugUnitTest      # uji unit (CoreTest)
gradle :app:bundleRelease \
  -PONIMEKU_STORE_FILE=... -PONIMEKU_STORE_PASS=... \
  -PONIMEKU_KEY_ALIAS=... -PONIMEKU_KEY_PASS=...   # AAB rilis (R8)
```

CI: `build.yml` (debug APK), `emulator-test.yml` (hook uji: intent extra `test_id`, `test_ep`, `test_home`), `release.yml` (uji unit + AAB bertanda tangan; secret `ONIMEKU_STORE_B64`, `ONIMEKU_STORE_PASS`, `ONIMEKU_KEY_ALIAS`, `ONIMEKU_KEY_PASS`, `ONIMEKU_SUPABASE_URL`, `ONIMEKU_SUPABASE_KEY`).

---
