# Onimeku (Kotlin, Media3) – siap rilis setelah 3 hal ini
Tidak ada data contoh di aplikasi. Semua konten datang dari API Anda.

## 1. API (wajib)
Endpoint default sudah diarahkan ke `https://onimekuappv2-api.adiyth-arpansa.workers.dev/`. Jika ingin mengganti, atur `ONIMEKU_API_URL` di `~/.gradle/gradle.properties` (harus HTTPS; garis miring akhir ditambahkan otomatis).
- `GET anime` -> `[{"id":"a1","title":"...","episodes":24,"synopsis":"...","poster":"https://...jpg","genres":["Action"],"airDay":1,"airTime":"19:00"}]`
  (`airDay` 0=Minggu..6=Sabtu; kosongkan `airDay`/`airTime` untuk judul yang sudah tamat)
- `GET anime/{id}/episodes/{ep}/stream` -> `{"url":"https://.../master.m3u8"}` (HLS)
Konten harus Anda miliki lisensinya.

## 2. Firebase Auth (opsional tapi disarankan)
Aktifkan Email/Password di console.firebase.google.com, taruh `google-services.json` di `app/`.

## 3. Signing
`ONIMEKU_STORE_FILE`, `ONIMEKU_STORE_PASS`, `ONIMEKU_KEY_ALIAS` di gradle.properties, lalu `./gradlew bundleRelease`.
Ikon launcher merah-putih di manifest sudah dipasang dari aset ikon yang disediakan. Siapkan Privacy Policy untuk Play Console.

## Komentar saat menonton (API tambahan)
- `GET anime/{id}/episodes/{ep}/comments` -> `[{"id","userId","user","text","timeMs","mine"}]` (`mine` true bila milik pengguna yang login)
- `POST anime/{id}/episodes/{ep}/comments` body `{"text","timeMs"}` + header `Authorization: Bearer <Firebase ID token>` -> komentar baru. Server WAJIB memverifikasi token, membatasi laju (rate limit), dan menyaring kata terlarang.
- `DELETE comments/{cid}` (hanya pemilik) · `POST comments/{cid}/report`
Kebijakan konten buatan pengguna Google Play mewajibkan: pelaporan, pemblokiran pengguna, moderasi di server, dan syarat penggunaan. Aplikasi sudah punya tombol Laporkan/Blokir; moderasi dan Terms of Use Anda sediakan sendiri.

## Tampilan (v2)
- Home: carousel banner, baris "Lanjutkan Menonton / Sedang Tayang / per genre / Sudah Tamat", tarik-untuk-refresh.
- Transisi poster -> detail memakai shared element (Compose BOM 2024.09.00).
- Tema: Dark (default), Light, System, Dynamic (Material You) - pilih di Settings. Font: lihat `Theme.kt` (`AppFont`).
- Detail: header parallax + blur, sinopsis "Selengkapnya", tombol Putar menempel, thumbnail episode (memakai poster karena API belum menyediakan gambar episode).
