# Onimeku v2 — Android (Kotlin + Jetpack Compose)

Aplikasi streaming anime berbahasa Indonesia. Tema gelap dengan aksen merah tua
(`#E11D2E` / `#6E0B16`) di atas latar `#08080A`, menu bawah melayang, transisi
halus, dan shimmer loading.

Katalog diambil **langsung dari MyAnimeList lewat Jikan API v4** (tanpa API key):
judul, poster, skor, genre, studio, sinopsis, jadwal tayang, dan trailer resmi.
Saat offline, aplikasi otomatis memakai katalog cadangan lokal agar tetap bisa
dibuka.

## Fitur
- **Onboarding + Auth** — 3 halaman pengenalan, masuk/daftar dengan validasi.
- **Beranda** — hero auto-slide, filter genre, Lanjut Menonton, Top Hits,
  Episode Baru, dan teaser Rilis Hari Ini.
- **Cari** — pencarian langsung (debounce) ke katalog lokal, lalu jatuh ke
  pencarian Jikan bila hasil kurang, riwayat pencarian, filter genre,
  urutan rating/terbaru.
- **Detail** — poster banner, tombol putar penuh, tab Episode / Info / Ulasan,
  status My List, favorit, unduh, bagikan, dan tautan trailer resmi.
- **Pemutar Media3** — seek, ±10 detik, kecepatan, kualitas, subtitle,
  autoplay, resume posisi terakhir.
- **Kalender Rilis** — jadwal mingguan dengan pengingat per judul.
- **My List** — filter status, tampilan grid/list.
- **Unduhan** — jeda/lanjut, progres, ringkasan penyimpanan.
- **Profil** — nama/email akun lokal, statistik, pengaturan
  pemutaran/unduhan/notifikasi, konfirmasi keluar.
- **Penyimpanan lokal** — My List, favorit, riwayat tontonan, pengingat, dan
  pengaturan disimpan di `SharedPreferences` dan dipulihkan saat aplikasi dibuka.

## UI/UX
- Design token terpusat di `ui/theme/Theme.kt` (`Space`, `Radius`, warna, typography, shapes).
- Komponen konsisten di `ui/components/Components.kt` (poster card, chip, sheet,
  empty/error state, skeleton shimmer, tombol aksi berlabel).
- Edge-to-edge dengan ikon status bar terang yang dipaksa agar tetap terbaca.
- Navigasi dengan animasi fade untuk tab, slide untuk halaman turunan, dan
  bottom nav melayang yang menyembunyikan diri di halaman detail/pemutar.
- Snackbar bertema, dialog konfirmasi untuk aksi destruktif, dan empty state
  dengan call-to-action.

## Struktur
```
app/src/main/java/com/onimeku/app/
├── MainActivity.kt          # edge-to-edge + tema
├── data/Data.kt             # model + repository Jikan + fallback offline
└── ui/
    ├── AppViewModel.kt      # state aplikasi
    ├── OnimekuApp.kt        # navigasi + bottom nav
    ├── components/          # komponen bersama
    ├── screens/             # onboarding, home, search, detail, player, library
    └── theme/Theme.kt       # warna, tipografi, bentuk, token
```

## Build & CI
- `./gradlew :app:assembleDebug` atau buka di Android Studio (JDK 17, AGP 8.5.2).
- GitHub Actions:
  - `.github/workflows/build.yml` — build APK debug + release setiap push ke `main`.
  - `.github/workflows/ci.yml` — build untuk branch/PR dengan error compiler
    ditampilkan sebagai anotasi; APK debug & release (unsigned) diunggah sebagai artifact.
- Unduh APK dari tab **Actions → run terbaru → Artifacts**.

## Sebelum rilis
1. Ganti `SAMPLE` di `screens/Player.kt` dengan URL HLS/DASH berlisensi —
   metadata & poster sudah nyata dari Jikan, tetapi video pratinjau masih contoh.
2. Ganti unduhan simulasi dengan `WorkManager` + `DownloadManager` nyata.
3. Auth nyata (backend/Firebase) + keystore rilis, lalu `./gradlew bundleRelease`.

## Brand
Nama: **Onimeku**. Logo: ring putih di kotak merah (`#F20A16`) —
`res/drawable-nodpi/ic_logo_ring.png`. Ikon adaptive + legacy tersedia, dan
`play-store-icon-512.png` siap untuk Play Console.
