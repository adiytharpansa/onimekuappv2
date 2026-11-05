# Backend Onimeku — Firebase (sejak 6 Nov 2026)

Backend lama (Supabase) sudah dibuang penuh. Semua layanan akun kini di
Firebase proyek **khusus Onimeku** (bukan proyek aplikasi lain):

| Layanan lama | Pengganti Firebase | Dipakai oleh |
|---|---|---|
| Auth (GoTrue REST) | Firebase Auth REST (identitytoolkit) | `data/Auth.kt` |
| Tabel `user_state` + RLS | Dokumen Firestore `user_state/<uid>` | `data/Sync.kt` |
| Tabel `comments` + RLS | Koleksi Firestore `comments` | `data/Comments.kt` |
| — (baru) | Dokumen `app_keys/onimeku_catalog` (loker potongan kunci katalog) | `data/KeyLocker.kt` |

## Penyiapan proyek (satu kali, di Firebase Console)

1. Buat proyek Firebase khusus Onimeku; daftarkan aplikasi Android
   `com.onimeku.app`.
2. Authentication: aktifkan **Email/Password** dan **Anonymous**.
3. Firestore Database: buat di lokasi **asia-southeast2**; tempel aturan
   dari `backend/firebase/firestore.rules`.
4. Catat **Project ID** dan **Web API key** klien ke `gradle.properties`
   (`ONIMEKU_FIREBASE_PROJECT_ID`, `ONIMEKU_FIREBASE_API_KEY`) atau ke
   secret repo CI dengan nama yang sama. Kosong = fitur akun/loker
   nonaktif dengan anggun; katalog & putar tetap jalan dari cermin.
5. Dokumen loker `app_keys/onimeku_catalog` dibuat manual/lewat skrip
   agen dari berkas rahasia lokal (nilainya tidak masuk repo).

Klien hanya memakai REST (tanpa SDK Firebase, tanpa google-services.json),
mengikuti gaya REST yang sudah dipakai proyek ini sejak awal.
