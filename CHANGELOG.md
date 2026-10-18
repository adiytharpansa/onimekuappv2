# Putaran 3 (19 Okt 2026) — performa streaming video

- Start lebih cepat: subtitle diunduh PARALEL dengan batas 4 dtk (sebelumnya berurutan, timeout 10+15 dtk per trek, menahan video); berkas subtitle dipakai ulang; `LoadControl` mulai putar di 1,5 dtk buffer (bawaan 2,5) dan pulih rebuffer di 4 dtk.
- Lancar: buffer maju 50 dtk + 20 dtk ke belakang; 5 percobaan ulang per segmen (bawaan 3); fallback dekoder; seek `CLOSEST_SYNC`.
- Jaringan: `data/Net.kt` — satu `OkHttpClient` bersama (pool koneksi, HTTP/2, cookie) untuk API, subtitle, pemutar, unduhan. Sebelumnya pemutar membuat klien baru tiap episode dan API memakai `HttpURLConnection` (handshake TLS baru tiap panggilan).
- Cache: pemutar kini MEMBACA cache unduhan tetapi tidak menulis segmen streaming ke dalamnya (sebelumnya `NoOpCacheEvictor` = disk tumbuh tanpa batas + I/O saat putar + risiko playlist terpotong tersimpan).
- Autoplay: data putar episode berikutnya di-prefetch saat 60 dtk terakhir (`ApiClient.prefetchPlayback`, TTL 120 dtk, sekali pakai). `fetchPlayback` retry cepat 1x; unduhan memakai `fresh = true`.
- Recompose: polling posisi 1 dtk dan hanya menulis state saat detik berubah (sebelumnya 500 ms); simpan progres tiap 10 dtk (sebelumnya 5 dtk).
- Belum dikompilasi/diukur di sini (tanpa Android SDK).

# Putaran 2 (19 Okt 2026) — performa + fungsi modern

Performa: Home jadi LazyColumn; TopBar derivedStateOf; decode/parse katalog dan baca/tulis cache di luar main thread; Coil global (cache 256 MB); `Anime` @Immutable; R8 + shrinkResources; Baseline Profile + profileinstaller; predictive back; ikon bertema.
Fungsi: pemutar fullscreen/landscape, kontrol auto-sembunyi, ketuk dua kali ±10 dtk, pemilih subtitle (default Indonesia) dan kualitas; spanduk offline + muat ulang otomatis saat online; chip Cast placeholder dihapus.
Belum dikompilasi/diukur di sini (tanpa Android SDK).

# Perubahan terbaru (19 Okt 2026) — tema crimson, halaman baru, rapikan posisi

- Warna: merah terang #E50914 diganti merah tua modern (crimson). Token: `Red #A6122D`, `RedDeep #6E0F1F`, `RedText #E8566F` (teks/ikon merah di latar gelap). Logo, ikon launcher, ikon Play Store, welcome, avatar, tombol putar ikut diganti.
- Halaman baru: **Sign in / Sign up** (`ui/AuthScreen.kt`, `data/Auth.kt`), **Terms of Service**, **Privacy Policy**, **Licenses** (`ui/LegalScreens.kt`). Profile menampilkan akun + Sign out.
- Posisi: TopBar berubah dari scrim ke solid secara bertahap (tanpa lonjakan), TabBar solid + garis atas (konten tidak tembus), toast aman di halaman Auth.
- Konfigurasi baru: `ONIMEKU_SUPABASE_URL` / `ONIMEKU_SUPABASE_KEY` (gradle.properties atau secret CI).
- Belum dikompilasi di sini (tanpa Android SDK): jalankan `gradle :app:assembleDebug`.

# Perubahan (perbaikan + rapikan tata letak)

## Kinerja / bug
- Catalog: `byId`, `top`, `popular`, `byGenre`, dst. sebelumnya dihitung ULANG di setiap akses (O(n²) di dalam loop Home). Sekarang di-cache per perubahan katalog.
- Home: posisi scroll hilang saat Back dari Detail -> sekarang disimpan di AppVM.
- See all / Search / My List: `TileGrid` (Column non-lazy) menggambar semua kartu sekaligus -> diganti `LazyVerticalGrid`.
- Detail: daftar episode kini `LazyColumn`.
- Hapus semua `!!` pada `Catalog.byId[...]` (crash saat katalog diperbarui bertahap).
- "Top Hits Anime -> See all" hanya menampilkan 6 judul -> sekarang seluruh judul urut skor.

## Tata letak
- Poster di rail sekarang punya judul di bawahnya (2 baris tetap, sejajar).
- Grid 3 kolom di ponsel (adaptif), kartu ringkas (badge/radius lebih kecil).
- Hero: tinggi mengikuti layar, judul maks 3 baris, genre 1 baris, gambar rata atas, klik -> Detail.
- Angka ranking tidak lagi terpotong.
- Padding bawah dinamis (tinggi tab bar + navigation bar), tab bar tinggi tetap, toast tepat di atas tab bar.
- TopBar jadi solid setelah digulir (ikon tidak menimpa poster).
- Detail: tombol Play/My List sama lebar, sinopsis "Read more", meta dirapikan.
- Home: indikator loading, bagian "All Anime" + tombol Browse all.
- README disesuaikan dengan API sebenarnya.

Catatan: perubahan belum dikompilasi di sini (tanpa Android SDK). Jalankan `gradle :app:assembleDebug` / workflow CI.

## Tahap 2 — Halaman Home modern (animasi & loading)
- `ui/Motion.kt` (baru): `shimmer()`, `SkeletonBox`, `HomeSkeleton`, `pressable()` (efek tekan pegas), `fadeSlideIn()`, `rememberImg()` (crossfade Coil).
- Home: skeleton loading (hero + baris poster), carousel hero 5 judul (geser/otomatis 5,5 dtk, indikator animasi, parallax), pull-to-refresh, baris muncul berurutan.
- Poster: kilau saat dimuat, gambar crossfade 350 ms, efek tekan; tombol juga.
- Navigasi: transisi fade + skala antar layar; ikon tab beranimasi.
- Refresh tidak lagi menyusutkan daftar sementara halaman awal dimuat.

## Tahap 3-4
- Detail: header parallax, tombol kembali melayang, genre chip, Resume Ep N, cincin progres unduhan.
- Welcome, Calendar, Search/See all, Download, Notifikasi, Profile, Toast: animasi dan loading (lihat Onimeku_Lengkap.md).
