# Onimeku (Kotlin, Media3) – siap rilis setelah 3 hal ini
Tidak ada data contoh di aplikasi. Semua konten datang dari API Anda.

## 1. API (wajib)
Aplikasi memakai API katalog `onimekuappv2-api` (lihat `ApiClient.BASE` di `data/Api.kt`; ubah di sana bila Anda memindahkan API).
- `GET /api/anime?limit=100&page=N` -> `{ ok, total, page, limit, totalPages, data: [Anime] }` (seluruh katalog dimuat bertahap, halaman pertama langsung tampil)
- `GET /api/anime/:id` · `GET /api/anime/:id/episode/:ep` · `GET /api/schedule` · `GET /api/poster/:anilistId`
Konten harus Anda miliki lisensinya.

## 2. Firebase Auth (opsional tapi disarankan)
Aktifkan Email/Password di console.firebase.google.com, taruh `google-services.json` di `app/`.

## 3. Signing
`ONIMEKU_STORE_FILE`, `ONIMEKU_STORE_PASS`, `ONIMEKU_KEY_ALIAS` di gradle.properties, lalu `./gradlew bundleRelease`.
Siapkan ikon adaptif (ganti `sym_def_app_icon` di manifest) dan Privacy Policy untuk Play Console.

## Komentar saat menonton (API tambahan)
- `GET anime/{id}/episodes/{ep}/comments` -> `[{"id","userId","user","text","timeMs","mine"}]` (`mine` true bila milik pengguna yang login)
- `POST anime/{id}/episodes/{ep}/comments` body `{"text","timeMs"}` + header `Authorization: Bearer <Firebase ID token>` -> komentar baru. Server WAJIB memverifikasi token, membatasi laju (rate limit), dan menyaring kata terlarang.
- `DELETE comments/{cid}` (hanya pemilik) · `POST comments/{cid}/report`
Kebijakan konten buatan pengguna Google Play mewajibkan: pelaporan, pemblokiran pengguna, moderasi di server, dan syarat penggunaan. Aplikasi sudah punya tombol Laporkan/Blokir; moderasi dan Terms of Use Anda sediakan sendiri.

## Tampilan
- Home: hero (tinggi mengikuti layar), baris "Top Hits Anime (ranking) / New Episode Releases / Airing Now / Popular / Latest / Donghua / per genre", lalu tombol **Browse all N anime**. Posisi scroll Home tersimpan saat membuka Detail lalu kembali.
- Setiap baris poster menampilkan judul di bawah poster; "See all" membuka grid 3 kolom (LazyVerticalGrid) berisi SELURUH judul. Search, My List, dan See all memakai grid yang sama.
- Detail: LazyColumn (daftar episode panjang tetap ringan), sinopsis "Read more", tombol Play/My List sama lebar.
- Daftar turunan katalog (top, populer, per genre, byId) di-cache per perubahan katalog (`Catalog.kt`).
