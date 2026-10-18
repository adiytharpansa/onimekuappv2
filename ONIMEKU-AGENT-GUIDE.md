# Onimeku — Panduan Lengkap untuk AI Agent

> Dokumen serah-terima. Baca seluruhnya sebelum mengubah kode. Bagian 1–6 menjelaskan sistem; bagian 7 mencatat perubahan terbaru; bagian 8–9 berisi pekerjaan tersisa dan aturan kerja.
> Tanggal: 19 Okt 2026 · Paket: `com.onimeku.app` · Versi: 1.0.0

---

## 1. Ringkasan

Onimeku adalah aplikasi Android native (Kotlin + Jetpack Compose + Media3) untuk menonton anime. Aplikasi memakai API katalog `my-anime-app` (Next.js di Cloudflare Workers) **langsung**, tanpa perantara.

| Bagian | Teknologi | Lokasi |
|---|---|---|
| Aplikasi Android | Kotlin, Compose Material 3, Media3 (ExoPlayer/HLS), OkHttp, Coil, WorkManager | `Onimeku-Kotlin-Terbaru.zip` |
| API katalog | Next.js 15 + OpenNext di Cloudflare Workers, katalog di Turso (utama) / D1 (cadangan) | `Onimeku-API-Terbaru.zip` |
| Akun (baru) | Supabase Auth via REST (email + kata sandi) | `data/Auth.kt` |

Keputusan yang harus dihormati:
- BASE API: `https://my-anime-app.adiyth-arpansa.workers.dev` (di `data/Api.kt`). Jangan menambah API perantara tanpa perintah pemilik proyek.
- Proyek Flutter lama sudah dihapus permanen. Jangan dihidupkan lagi.
- Tidak ada data contoh di aplikasi. Semua konten datang dari API.
- Aplikasi untuk publik: harus tampil profesional. Tidak ada input sumber video oleh pengguna.
- Konten harus berlisensi milik pemilik proyek. Tanggung jawab hukum ada pada pemilik.

---

## 2. Arsitektur aplikasi

```
MainActivity  ──►  App(vm)  ──►  AnimatedContent(route)
                                   ├─ HomeScreen / CalendarScreen / MyListScreen
                                   ├─ DownloadsScreen / ProfileScreen
                                   ├─ DetailScreen / SeeAllScreen / SearchScreen / NotifScreen
                                   ├─ SubScreen (settings, language, help, about, terms, privacy, licenses)
                                   ├─ AuthScreen            (BARU)
                                   └─ PlayerScreen
AppVM (satu-satunya ViewModel)
  ├─ state: AppState (SharedPreferences "onimeku", JSON via kotlinx.serialization)
  ├─ navigasi: stack: List<Route>   (tanpa Navigation Compose)
  ├─ katalog: object Catalog (state global, cache turunan)
  └─ unduhan: Downloads (Media3 DownloadManager)
```

### 2.1 Peta file (`app/src/main/java/com/onimeku/app/`)

| File | Isi |
|---|---|
| `OnimekuApp.kt` | **BARU** `Application` + pemuat gambar Coil global (cache memori 20%, disk 256 MB) |
| `MainActivity.kt` | Activity, edge-to-edge, PiP, izin notifikasi, `App()`, `TopBar`, `TabBar`, toast |
| `AppVM.kt` | `Route`, navigasi, My List, progres tonton, unduhan, profil, **akun (BARU)** |
| `Notifier.kt`, `Reminders.kt` | Notifikasi dan pengingat episode (WorkManager) |
| `data/Api.kt` | `ApiClient` (katalog, detail, playback, proksi stream), model `ApiAnime`, `Playback` |
| `data/Auth.kt` | **BARU** `AuthClient` (Supabase GoTrue REST), `AuthException` |
| `data/Catalog.kt` | Model `Anime`, `object Catalog` (daftar turunan di-cache per perubahan katalog) |
| `data/State.kt` | `AppState`, `Profile`, `Settings`, `Account` (BARU), `Store` |
| `data/Downloads.kt` | Unduhan offline HLS (Media3) |
| `data/PosterHealer.kt` | Pemulihan URL poster/cover yang rusak |
| `ui/Theme.kt` | Token warna dan tema |
| `ui/Components.kt` | `Logo`, `Poster`, `PosterCard`, `Tile`, `AnimeGrid`, `Rail`, `SectionHead`, `Page`, `SubHead`, `CardBox`, `Btn`, `Chip`, `EmptyBox` |
| `ui/Motion.kt` | `shimmer`, `SkeletonBox`, `HomeSkeleton`, `pressable`, `fadeSlideIn`, `rememberImg` |
| `ui/Screens.kt` | Home (hero carousel, rail), Calendar, My List, Downloads, Detail, SeeAll, Notif, Search |
| `ui/PlayerScreen.kt` | Pemutar Media3, kontrol, PiP, skip intro, progres |
| `ui/ProfileScreens.kt` | Profile, `SubScreen` (settings, language, help, about + legal) |
| `ui/WelcomeScreen.kt` | Layar sambutan 3 slide |
| `ui/AuthScreen.kt` | **BARU** Sign in / Sign up |
| `ui/LegalScreens.kt` | **BARU** Terms, Privacy, Licenses |

### 2.2 Navigasi

`sealed interface Route`: `Home, Calendar, MyList, Downloads, ProfileTab, Sub(key), Detail(id), SeeAll(key), Notif, Search, Player(id, ep), Auth(signUp)`.

- `go(r)` dorong ke stack · `back()` pop · `setTab(r)` reset stack ke satu rute · `replaceTop(r)`.
- Tab root = `stack.first()`. TabBar disembunyikan di `Player` dan `Auth`.
- Back sistem ditangani `BackHandler` bila `stack.size > 1`.
- Kunci `Sub`: `settings`, `language`, `help`, `about`, `terms`, `privacy`, `licenses`.
- Kunci `SeeAll`: `top`, `new`, `airing`, `popular`, `latest`, `donghua`, `all`, `genre:<nama>`.

### 2.3 State dan penyimpanan

`AppState` disimpan sebagai satu string JSON di SharedPreferences `onimeku` (`Store.load/save`, `ignoreUnknownKeys = true` sehingga menambah field aman).

```kotlin
AppState(welcomeSeen, downloads: List<Dl>, notifRead, hist, fired, settings, profiles: List<Profile>, pi, account: Account?)
Profile(name, myList, prog: Map<"id:ep", detik>, seen, last: Last?, base, remind)
Settings(autoplay, skipIntro, wifi, alerts, quality)
Account(userId, email, accessToken, refreshToken)   // BARU, null = tamu
```

Semua perubahan state lewat `upd { it.copy(...) }` (menyimpan otomatis). Untuk data per-profil pakai `updP`.

### 2.4 Katalog

- `GET /api/anime` → satu panggilan, tanpa paginasi (±900 KB). `Catalog.all` diisi; cache disk `catalog_cache_direct.json` ditampilkan seketika, jaringan menyegarkan di latar.
- Daftar turunan (`top`, `popular`, `airing`, `latest`, `donghua`, `byGenre`, `new`, `chips`, `years`) dihitung sekali per perubahan `Catalog.all`. Jangan menghitung ulang di dalam composable/loop.
- Jangan memakai `!!` pada `Catalog.byId[...]` (katalog bisa berubah saat refresh).

### 2.5 Pemutaran dan unduhan

- Stream CDN (MegaPlay/nexabloom) dibungkus proksi hulu: `streamUrl = $BASE/api/proxy?u=<url asli>&ref=<akar origin referer>`. Referer harus dinormalisasi ke akar origin (CDN menolak referer halaman penuh).
- Unduhan hanya untuk stream `.m3u8` langsung. Judul embed-saja ditolak dengan pesan jujur.
- PiP: Android 12+ auto-enter diatur di `PlayerScreen`; di bawahnya manual di `onUserLeaveHint`.
- Hulu tidak punya endpoint jadwal/poster: `fetchSchedule()` kosong (Calendar memakai turunan hari/jam katalog), `resolvePoster()` null.

### 2.6 Akun (baru)

- `AuthClient` memanggil `POST {SUPABASE_URL}/auth/v1/token?grant_type=password`, `/auth/v1/signup`, `/auth/v1/recover`, `/auth/v1/logout` dengan header `apikey` = kunci publik.
- Konfigurasi lewat `gradle.properties` atau CI: `ONIMEKU_SUPABASE_URL`, `ONIMEKU_SUPABASE_KEY` → `BuildConfig.SUPABASE_URL/KEY`. Bila kosong: `AuthClient.configured == false`, halaman Auth menampilkan catatan dan pemanggilan gagal dengan pesan ramah (aplikasi tetap berjalan sebagai tamu).
- Sign up: bila konfirmasi email aktif di Supabase, respons tanpa token → `signUp` mengembalikan `null` → toast "Check your email…" lalu pindah ke Sign in.
- Hanya kunci PUBLIK (`sb_publishable_…`/anon). Jangan pernah `service_role`.

---

## 3. API (my-anime-app)

Ekstrak dari proyek Next.js di Cloudflare Workers.

| Endpoint | Fungsi |
|---|---|
| `GET /api/anime` | `{ok, provider, data:[ringkasan]}` seluruh katalog |
| `GET /api/anime?id=<slug>` | Detail + `episodes[]` |
| `GET /api/anime?id=<slug>&ep=<n>` | Playback (`streamUrl`, `subtitles`, `referer`, …) |
| `GET /api/proxy?u=&ref=` | Proksi media hulu |
| `GET /api/img` | Proksi gambar |
| `POST /api/revalidate`, `/api/write-queue`, `/api/write-queue/replay`, `/api/migrate-turso`, `/api/push/send` | Operasional internal (butuh token) |

Catatan:
- Batas laju per IP: 600 GET/menit, 120 POST/menit (Cache API per-PoP, gagal-terbuka).
- Penyimpanan katalog di `src/lib/store.ts` (Turso utama, D1 cadangan). File `store.ts.bak-20261018-mirrorfirst` adalah cadangan lama; jangan dipakai sebagai sumber kebenaran.
- API **tidak** punya endpoint akun/komentar. Akun memakai Supabase langsung dari aplikasi.

---

## 4. Sistem desain

### 4.1 Warna (`ui/Theme.kt`) — tema crimson

| Token | Hex | Pakai untuk |
|---|---|---|
| `Red` | `#A6122D` | Tombol Play, indikator aktif, chip aktif, switch, slider, badge HD, avatar, logo |
| `RedDeep` | `#6E0F1F` | Gradasi/aksen gelap |
| `RedText` | `#E8566F` | Teks dan ikon merah di atas latar gelap ("See all", tab aktif, error) |
| `Bg` | `#14161F` | Latar utama |
| `Bg2` | `#1B1E2A` | Kartu, field, chip |
| `Line` | `#2A2D3B` | Garis dan border |
| `Solid` | `#2B2E3B` | Tombol sekunder |
| `InkSoft` | `#C9CCD8` | Teks sekunder |
| `NavOff` | `#9EA4B8` | Ikon/teks nonaktif |

Aturan:
- Jangan menulis `Color(0xFF…)` merah langsung di layar. Pakai `Red`, `RedDeep`, `RedText`.
- Teks/ikon merah di atas latar gelap wajib `RedText` (kontras terbaca); `Red` hanya untuk isian/latar.
- Overlay gelap pada gambar memakai `#0A0B10` dengan alfa (sudah dipakai konsisten).

### 4.2 Bentuk, jarak, tipografi

- Radius: kartu 18 dp, poster ringkas 12 dp, tombol/chip pill (CircleShape), field 16 dp.
- Padding halaman 16 dp; jarak antarbagian Home 26 dp (`SectionGap`).
- Judul bagian 22 sp SemiBold; judul halaman root 26 sp; sub-halaman 24 sp; badan 14–16 sp.
- Area sentuh minimal 40–44 dp (ikon bar atas 44 dp).
- Ukuran poster rail: `railW` = 40% lebar layar, dibatasi 142–230 dp. Judul di bawah poster 2 baris tetap agar baris sejajar.

### 4.3 Komponen yang dipakai ulang

`Page`, `SubHead`, `CardBox`, `HLine`, `Btn(BtnStyle.Play|Solid|Outline|OutlineOn)`, `Chip`, `IconChip`, `EmptyBox`, `Poster/PosterCard/Tile`, `AnimeGrid` (LazyVerticalGrid adaptif ≥100 dp), `Rail`, `SectionHead`. Halaman baru wajib memakai komponen ini, bukan membuat gaya sendiri.

### 4.4 Gerak

`fadeSlideIn(delay)`, `pressable`, `shimmer`, crossfade gambar 350 ms, transisi rute fade + skala 0.98. Pertahankan; jangan menambah animasi berat di daftar panjang.

---

## 5. Layar dan perilaku

| Layar | Perilaku inti |
|---|---|
| Welcome | 3 slide otomatis 4,5 dtk; Skip; tombol Sign in/Sign up → menutup welcome lalu membuka `Auth` |
| Home | Hero carousel 5 judul (otomatis 5,5 dtk, parallax), Continue Watching, Because you watched, Top Hits (ranking), New Episode Releases, Airing Now, Popular, Latest, Donghua, genre teratas, tombol "Browse all N anime". Posisi scroll disimpan di `vm.homeScroll` |
| Calendar | 7 hari ke depan; daftar rilis per hari; tombol "Remind me" |
| My List | Grid 3 kolom; kosong → ajakan jelajah |
| Download | Daftar unduhan + progres cincin; hapus satu/semua |
| Profile | Multi-profil (maks 4), kartu akun, menu Settings/Language/Help/About |
| Detail | Header parallax, Play/My List sama lebar, sinopsis "Read more", daftar episode (LazyColumn), unduh, Resume Ep N |
| SeeAll / Search | `AnimeGrid` lazy berisi seluruh judul; Search punya riwayat 6 item |
| Player | Media3, PiP, skip intro, simpan progres, autoplay episode berikut |
| Auth (baru) | Sign in/Sign up, validasi, tampil/sembunyi sandi, lupa sandi, tautan Terms/Privacy |
| Terms/Privacy/Licenses (baru) | Teks statis dari `LegalScreens.kt` |

---

## 6. Build, jalankan, rilis

```bash
gradle :app:assembleDebug        # Gradle 8.9, JDK 17, compileSdk/targetSdk 35, minSdk 26
gradle :app:bundleRelease        # butuh signing (di bawah)
```

- CI: `.github/workflows/build.yml` (build debug APK), `emulator-test.yml` (uji emulator). Hook uji: intent extra `test_id`, `test_ep`, `test_home`.
- Secret CI opsional: `ONIMEKU_SUPABASE_URL`, `ONIMEKU_SUPABASE_KEY` (sudah dipetakan ke `ORG_GRADLE_PROJECT_*`).
- Signing rilis: `ONIMEKU_STORE_FILE`, `ONIMEKU_STORE_PASS`, `ONIMEKU_KEY_ALIAS` di `gradle.properties`.
- **Status verifikasi:** perubahan di bagian 7 ditulis tanpa Android SDK, jadi **belum dikompilasi**. Langkah pertama agent berikutnya: jalankan build, perbaiki galat impor/tipe bila ada, lalu jalankan emulator-test.

---

## 7. Perubahan terbaru (19 Okt 2026)

### 7.1 Warna merah tua modern
- `Theme.kt`: `Red` `#E50914` → `#A6122D`; ditambah `RedDeep`, `RedText` `#FF4452` → `#E8566F`; `secondary/error` ikut `RedText`.
- Diganti ke token: `Logo` (Components), tombol putar (PlayerScreen, `Red` alfa 0.92), avatar profil (#A6122D), gradasi merah Welcome (`#6E0F1F/#A6122D/#2E0710`), `ic_launcher_foreground.xml` (`#A6122D`).
- `play-store-icon-512.png` diwarnai ulang ke `#A6122D` (putih dan tepi anti-alias dipertahankan).

### 7.2 Halaman baru
- **Sign in / Sign up** — `ui/AuthScreen.kt`, `data/Auth.kt`, `Route.Auth(signUp)`, `AppVM.signIn/signUp/resetPassword/signOut`, `AppState.account`. Profile menampilkan email + tombol Sign out saat masuk.
- **Terms of Service, Privacy Policy, Licenses** — `ui/LegalScreens.kt`; baris di About kini bisa diketuk (`Route.Sub("terms"|"privacy"|"licenses")`).

### 7.3 Rapikan posisi
- `TopBar`: sebelumnya berubah solid mendadak di scroll 360 sehingga teks poster tembus di belakang ikon. Sekarang scrim gradasi di atas hero lalu solid bertahap mulai scroll ±80 hingga ±300.
- `TabBar`: sebelumnya gradasi transparan sehingga judul baris tembus ("Airing Now" terlihat di balik tab). Sekarang solid `Bg` + garis atas 1 dp `Line`.
- Toast turun ke 24 dp di halaman Auth (tanpa tab bar).

### 7.4 Performa (19 Okt 2026, putaran 2)
Semua ditulis tanpa Android SDK: **belum diukur dan belum dikompilasi**. Ukur dulu sebelum mengklaim hasil (Macrobenchmark/Perfetto, `adb shell dumpsys gfxinfo`).
- **Home → `LazyColumn`** (`Screens.kt`): sebelumnya `Column` + `verticalScroll` mengomposisi semua rail (belasan genre) sekaligus. Kini hanya baris terlihat (+prefetch). `vm.homeScroll` kini `LazyListState`; gunakan `vm.homeScrollPx()` untuk membaca jarak gulir (di lambda `graphicsLayer`/`derivedStateOf`, jangan di komposisi). Baris di bawah layar tanpa animasi masuk (`delayMs = -1`) agar tidak mengulang saat digulir kembali.
- **TopBar**: membaca gulir lewat `derivedStateOf` dengan kuantisasi 5% → tidak recompose tiap piksel.
- **Decode di luar main thread**: parse JSON katalog (±900 KB) di `Dispatchers.Default` (`ApiClient.fetchCatalog`), baca cache disk dan tulis cache di `Dispatchers.IO` (`AppVM.reloadCatalog`). Sebelumnya semua di main thread.
- **Coil global** (`OnimekuApp`): cache memori 20% + disk 256 MB, `respectCacheHeaders(false)` (proksi gambar sering `no-cache` sehingga poster diunduh ulang).
- **Stabilitas Compose**: `Anime` diberi `@Immutable` (berisi `List` yang dianggap tidak stabil → rail recompose berlebihan).
- **Memo**: "Because you watched" (`Catalog.similar`) dan filter hari Calendar dibungkus `remember`.
- **R8 + shrinkResources** untuk rilis, aturan keep di `proguard-rules.pro` (serialization, model data, Media3, OkHttp). **Wajib uji build rilis** (`bundleRelease`) di perangkat: R8 sering merusak refleksi/serialisasi diam-diam.
- **Baseline Profile** (`src/main/baseline-prof.txt`) + `profileinstaller`. Ini profil aturan tulis-tangan; ganti dengan profil hasil Macrobenchmark bila tersedia.
- **Predictive back** (`enableOnBackInvokedCallback`) dan **ikon bertema** Android 13 (`<monochrome>`).

### 7.5 Fungsi modern
- **Pemutar**: mode **fullscreen/landscape** (kunci landscape, sembunyikan bilah sistem, Back keluar dari fullscreen), **kontrol otomatis sembunyi** (3,5 dtk) dengan ketuk untuk memunculkan, **ketuk dua kali** sisi kiri/kanan untuk ±10 dtk, **pemilih subtitle** (otomatis memilih Indonesia → Inggris → pertama; tombol "CC" di bar atas dan chip), **pemilih kualitas** langsung di pemutar (tersimpan ke Settings). Chip "Cast" yang cuma menampilkan toast dihapus.
- **Spanduk offline** + **muat ulang katalog otomatis** saat koneksi kembali (`AppVM.online`, `registerDefaultNetworkCallback`).

### 7.6 Performa streaming video (19 Okt 2026, putaran 3)
Ditulis tanpa Android SDK: **belum dikompilasi dan belum diukur**. Ukur dulu: waktu tekan Play → frame pertama, jumlah rebuffer per jam, `adb shell dumpsys gfxinfo`.
- `data/Net.kt`: satu `OkHttpClient` bersama (`Net.client`). Pemutar, subtitle, `ApiClient.getText`, dan `Downloads.httpFactory` memakainya. Jangan membuat `OkHttpClient` baru untuk media.
- `PlayerScreen`: subtitle paralel + batas 4 dtk; `DefaultLoadControl` (20/50/1,5/4 dtk, back buffer 20 dtk); `DefaultLoadErrorHandlingPolicy(5)`; `setEnableDecoderFallback(true)`; `SeekParameters.CLOSEST_SYNC`; cache unduhan **baca-saja** untuk streaming (`setCacheWriteDataSinkFactory(null)`).
- Polling posisi 1 dtk, state hanya ditulis saat detik berubah; `saveProg` tiap 10 dtk.
- `ApiClient.prefetchPlayback` dipanggil pada 60 dtk terakhir bila autoplay aktif; `fetchPlayback(fresh = true)` untuk unduhan.
- Belum diubah (butuh keputusan pemilik): jalur utama MegaPlay lewat proksi worker (`/api/proxy`) menambah satu lompatan + kena batas 600 GET/menit per IP; URL langsung hanya cadangan.

---

## 8. Pekerjaan tersisa (urut prioritas)

1. **Kompilasi dan uji** (bagian 6). Belum ada build setelah perubahan 7.
2. **Isi kunci Supabase** (`ONIMEKU_SUPABASE_URL/KEY`) dan atur Auth di dashboard: URL redirect, apakah konfirmasi email wajib, batas laju.
3. **Hapus akun** — kebijakan Google Play mewajibkan jalur hapus akun bila ada pembuatan akun. Butuh endpoint server (Edge Function) yang memverifikasi token lalu menghapus pengguna; klien tidak boleh memakai `service_role`.
4. **Simpan token dengan aman** — sekarang `Account` ada di SharedPreferences biasa. Pindahkan ke `EncryptedSharedPreferences`/Keystore; tambahkan refresh token (`/auth/v1/token?grant_type=refresh_token`) saat 401.
5. **Sinkronisasi** My List/progres ke akun (tabel Supabase dengan RLS per `auth.uid()`); sekarang semua lokal.
6. **Bahasa** — "Bahasa Indonesia" dan "日本語" masih "Coming soon". String masih tertanam di kode; pindahkan ke `strings.xml` sebelum menambah bahasa.
7. **Teks legal** — tinjau oleh penasihat hukum, tambahkan kontak resmi dan URL kebijakan publik untuk Play Console.
8. **Ukur performa** (Macrobenchmark: startup dingin, jank scroll Home, memori) dan ganti baseline profile tulis-tangan dengan hasil ukur.
9. **Pull-to-refresh Home** dimatikan (crash `PullToRefreshBox` di emulator CI). `vm.refresh()` masih ada; coba lagi setelah naik versi Compose.
10. `README.md` masih menyebut API `onimekuappv2-api` dan Firebase Auth; selaraskan dengan `CATATAN-ARSITEKTUR.md` (API langsung) dan Supabase.
11. Uji build rilis ber-R8 (lihat 7.4).

---

## 9. Aturan kerja untuk agent

1. Baca `CATATAN-ARSITEKTUR.md` dan dokumen ini dulu. Jangan mengubah BASE API atau menambah perantara tanpa perintah.
2. Ubah sekecil mungkin; ikuti gaya yang ada (satu `AppVM`, `Route` sealed, state lewat `upd/updP`).
3. Warna hanya dari `Theme.kt`. Komponen dari `Components.kt`.
4. Daftar panjang wajib lazy (`LazyColumn`, `LazyVerticalGrid`, `LazyRow`). Jangan `Column` non-lazy untuk katalog.
5. Jangan hitung ulang turunan katalog di composable; tambahkan ke `Catalog.Derived`.
6. Tidak ada data contoh/gambar palsu. Selama gambar belum dimuat, tampilkan shimmer.
7. Jangan menaruh rahasia di repo. Hanya kunci publik di klien.
8. Setiap perubahan: perbarui `CHANGELOG.md` dan bagian 7 dokumen ini; jujur soal apa yang belum dikompilasi/diuji.
9. Fitur baru wajib punya keadaan loading, kosong, dan galat.
10. Setelah menambah `Route`, daftarkan di `when (r)` pada `App()` dan putuskan apakah TabBar tampil.
