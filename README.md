# Onimeku (Kotlin, Compose, Media3)

Aplikasi Android untuk menonton anime. Tidak ada data contoh dan tidak ada API katalog: semua konten dibaca dari katalog gudang terenkripsi + cermin (lihat `data/Api.kt`). Konten harus Anda miliki lisensinya.

## Menjalankan
`gradle :app:assembleDebug` (JDK 17, Gradle 8.9). Uji unit: `gradle :app:testDebugUnitTest`.

## Akun (opsional)
Firebase Auth + sinkronisasi My List/progres ke Firestore. Siapkan backend di `backend/` (lihat `backend/README.md`), lalu isi `ONIMEKU_FIREBASE_PROJECT_ID` dan `ONIMEKU_FIREBASE_API_KEY` di `gradle.properties` atau secret CI.

## Rilis
`ONIMEKU_STORE_FILE`, `ONIMEKU_STORE_PASS`, `ONIMEKU_KEY_ALIAS`, `ONIMEKU_KEY_PASS` lalu `gradle :app:bundleRelease`, atau jalankan workflow `release` (secret: `ONIMEKU_STORE_B64`, dst). Uji build rilis (R8) di perangkat sebelum unggah.

Panduan lengkap untuk pengembang/agent: `ONIMEKU-AGENT-GUIDE.md`; arsitektur: `CATATAN-ARSITEKTUR.md`.
