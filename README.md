# Onimeku v2 — Android (Kotlin + Jetpack Compose)

Aplikasi streaming anime berbahasa Indonesia. Tema gelap dengan aksen merah tua
(`#E11D2E` / `#7A0C1B`) di atas latar `#0F0A0B`, menu bawah melayang, transisi
halus, dan shimmer loading.

## Fitur
- **Onboarding + Auth** — 3 halaman pengenalan, masuk/daftar dengan validasi.
- **Beranda** — hero auto-slide, filter genre, Lanjut Menonton, Top Hits,
  Episode Baru, dan teaser Rilis Hari Ini.
- **Cari** — pencarian langsung (debounce), riwayat pencarian, filter genre,
  urutan rating/terbaru.
- **Detail** — poster banner, aksi cepat, tab Episode / Info / Ulasan,
  status My List, favorit, unduh, bagikan.
- **Pemutar Media3** — seek, ±10 detik, kecepatan, kualitas, subtitle,
  autoplay, resume posisi terakhir.
- **Kalender Rilis** — jadwal mingguan dengan pengingat per judul.
- **My List** — filter status, tampilan grid/list.
- **Unduhan** — jeda/lanjut, progres, ringkasan penyimpanan.
- **Profil** — statistik, pengaturan pemutaran/unduhan/notifikasi, konfirmasi keluar.

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
├── data/Data.kt             # model + repository contoh
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
1. Ganti `AnimeRepository` dengan API nyata (Retrofit/AniList/Jikan) dan isi
   `poster`/`banner`.
2. Ganti `SAMPLE` di `screens/Player.kt` dengan URL HLS/DASH.
3. Simpan My List, riwayat, dan pengaturan dengan Room/DataStore; unduhan nyata
   via WorkManager.
4. Auth nyata (backend/Firebase) + keystore rilis, lalu `./gradlew bundleRelease`.

## Brand
Nama: **Onimeku**. Logo: ring putih di kotak merah (`#F20A16`) —
`res/drawable-nodpi/ic_logo_ring.png`. Ikon adaptive + legacy tersedia, dan
`play-store-icon-512.png` siap untuk Play Console.
