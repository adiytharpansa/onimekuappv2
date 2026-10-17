# Onimeku v2 — Android (Kotlin + Jetpack Compose)
Tema: merah tua (#8B0F20 / #E11D2E) + putih di atas latar gelap. Menu bawah melayang, animasi transisi, shimmer loading.
## Jalankan
Android Studio (Koala+), JDK 17, sync Gradle, Run.
## Fitur
Onboarding+Auth (validasi), Home (hero auto-slide, filter genre, Lanjut Menonton, Top Hits, Episode Baru, notifikasi),
Search (riwayat, filter genre, urutan), Detail (tab Episode/Info/Ulasan, status My List, favorit, unduh, bagikan),
Player Media3 (seek, ±10s, kecepatan, kualitas, subtitle, autoplay, resume), Rilis (pengingat), My List (filter, grid/list),
Unduhan (jeda/lanjut, penyimpanan), Profil (statistik, pengaturan).
## Struktur
`data/` model+repository · `ui/AppViewModel.kt` state · `ui/components/` komponen · `ui/screens/` layar · `ui/OnimekuApp.kt` navigasi
## Brand
Nama: Onimeku. Logo: ring putih di kotak merah (#F20A16) — aset di `res/drawable-nodpi/ic_logo_ring.png`, ikon launcher adaptive + legacy sudah dibuat, `play-store-icon-512.png` untuk Play Console.
## Sebelum rilis
1. Isi `poster`/`banner` dari API; ganti `SAMPLE` di Player.kt dengan stream HLS.
2. Simpan data (My List, riwayat, pengaturan) dengan Room/DataStore; unduhan nyata via WorkManager.
3. Auth nyata (backend/Firebase). Tambah ikon launcher + keystore. `./gradlew bundleRelease`
