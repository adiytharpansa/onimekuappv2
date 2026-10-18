## v1.0.0-debug.35 (belum dirilis — menunggu perintah): Perbaikan Beranda (paket Claude lanjutan)
- Bilah atas: scrim hitam di belakang logo DIHAPUS total — di atas hero bilah benar-benar polos, lalu cepat menjadi solid penuh + garis saat digulir (ambang 24px/56px). Logo digeser ke kanan (padding 24dp) dan diperbesar 48dp.
- Kartu "New Episode Releases": kini POTRET sama seperti rail lain (dulu lanskap 232:150 memakai banner) + lencana rating/HD tampil (Poster badges=true; parameternya memang sudah ada di Components).
- Berkas PlayerScreen paket ini identik dengan .34 — tidak menimpa perbaikan OptIn agen. Redesain Detail & Pemutar .34 utuh.

## v1.0.0-debug.34 (belum dirilis — menunggu perintah): Redesain Detail & Pemutar (paket Claude)
- Claude menamai paket ini "debug.33" (dari ZIP .32); di repo .33 sudah terisi penyegaran katalog otomatis, jadi paket ini masuk sebagai .34 tanpa mengubah isinya.
- Screens.kt: halaman Detail didesain ulang — komponen khusus baru (PlayButton, GlassButton, ActionTile, EpisodeCard, ScorePill, StatusPill, SectionTitle) + tombol bagikan (share). Beranda/komentar/i18n .31-.32 utuh.
- PlayerScreen.kt: tampilan pemutar dipoles (ControlTile, PlayerStatus, ikon wildcard, insets) — RANTAI PUTAR TIDAK BERUBAH (proksi/mainRetry/directUrl/chainDead/batas 26 dtk/isolasi cache terverifikasi identik sebelum diterapkan).

## v1.0.0-debug.33 (belum dirilis — menunggu perintah): Katalog Selalu Segar
- Permintaan user: semua yang tampil di Beranda dsb. harus selalu ter-update saat ada anime baru. Kini aplikasi menyegarkan katalog DIAM-DIAM (tanpa spinner, tanpa mengosongkan layar) pada tiga pemicu: (1) aplikasi kembali ke depan bila data sudah >5 menit, (2) kembali ke tab Beranda, (3) pemeriksaan berkala tiap 10 menit selama aplikasi terbuka (selaras irama sinkron + cache API 10 menit).
- Umur data dilacak dari keberhasilan muat terakhir (AppVM.refreshIfStale + lastFetchAt); pemicu tidak menumpuk saat muat berjalan. Detail/episode/pemutar memang sudah selalu diambil segar dari API setiap dibuka — yang berubah sesi panjang yang sebelumnya tidak pernah menyegar kecuali mulai dingin atau tarik manual (tarik-untuk-segar tetap ada).

## v1.0.0-debug.32 (belum dirilis — menunggu perintah): Poles Tampilan Beranda (paket Claude)
- Hero: gambar parallax kini dipotong (clipToBounds) — tidak bocor lagi ke bawah hero; judul Bold; baris meta baru (skor emas Gold + tahun + jumlah episode + status); genre lebih redup; tombol lebih ramping.
- Bilah atas: solid 100% saat digulir + garis tipis (dulu 97% sehingga judul hero tembus samar); scrim atas tanpa tepi keras.
- Kartu "New Episode Releases" kini menampilkan nama anime di bawah kartu (dulu hanya "Episode N / Today").
- Tombol Outline ("My List"): kaca gelap + garis 1,5dp (dulu cincin putih 3dp). Judul bagian 20sp + "See all" ber-chevron. Tab bar: indikator pil di belakang ikon aktif.
- Token baru Gold (#FFC857) khusus skor. Catatan agen: "See all" di SectionHead kini lewat I18n (mode id = "Lihat semua"); a.status ternyata properti hitung yang sudah ada di Anime — diff diterapkan apa adanya selain itu.

## v1.0.0-debug.31 (belum dirilis — menunggu perintah): "bereskan sisanya"
- AKUN AKTIF: secret CI ONIMEKU_SUPABASE_URL/KEY terpasang (proyek user sendiri); backend Supabase (user_state + RLS + fungsi delete-account) dideploy. Masuk/daftar/sinkron/hapus akun kini berfungsi.
- KOMENTAR per judul di halaman Detail (tabel comments + RLS: baca publik, tulis/hapus milik sendiri, wajib masuk).
- BAHASA INDONESIA berfungsi: objek I18n + Settings.lang; tab, Beranda, Detail, pemutar, Unduhan, Kalender, Profil/Pengaturan, layar akun & sambutan. Isi data (judul/sinopsis) dan naskah hukum tidak diterjemahkan.
- TARIK-UNTUK-SEGAR Beranda buatan sendiri (koneksi gulir bersarang; tanpa PullToRefreshBox yang dulu crash).
- FULLSCREEN bertahan saat pindah episode (keadaan dipindah ke AppVM, direset saat keluar dari pemutar); downloadAll kini BERURUTAN (satu fetchPlayback pada satu waktu).
- LOGO kembali ke merah karya pemilik #F20A17 (token LogoRed: logo dalam aplikasi, foreground launcher, ikon Play Store). Tema antarmuka tetap crimson.
- BACKUP: dataExtractionRules + fullBackupContent mengecualikan onimeku_secure dari cadangan/pindah HP (kunci Keystore tidak ikut pindah; token hasil restore tidak lagi jadi sampah tak terbaca).
- POSTER: penyembuhan dipindah ke SERVER my-anime-app (poster_map persisten + resolusi Kitsu server-side, URL dikanonikkan ke media.kitsu.app). Aplikasi TIDAK lagi menelepon Kitsu: resolvePoster kembali null dan cache poster lama direset sekali (reset_v31) — semua data poster/stream/download kini dari API sendiri.
- Yang SENGAJA tidak dikerjakan atas perintah user: build rilis Play Store (AAB/R8) — ditunda.

# Perbaikan pengingat (18 Okt 2026, dari Dokumen Gabungan) — diterapkan agen sebagai debug.29

- `Reminders.kt`: pengingat mingguan tidak lagi mati sesudah bunyi pertama — `ReminderWorker` kini meneruskan detail hasil `fetchDetail` ke `Reminders.schedule(...)` (katalog kosong di proses worker membuat penjadwalan ulang sebelumnya diam-diam batal).
- `Reminders.kt`: `delayMs` mem-parsing `time` secara aman (kosong/rusak -> 09:00); sebelumnya `toInt()` pada string kosong membuat aplikasi crash dari `rescheduleAll`.

# Perbaikan agen debug.23–.28 (18 Okt 2026) — putar di HP + poster

- Putar: rantai disamakan pemutar web (ref proksi = referer utuh; embed dicabut dari rantai judul ber-stream — rantai habis = layar galat jujur + "Try again"); retry sekali jalur proksi; pembersihan entri cache unduhan yatim era cache-tulis; CrashLog mencatat lapis + kode galat.
- debug.25: streaming lepas total dari cache bersama (cache hanya untuk putar unduhan offline, item streaming tanpa kunci cache); jalur proksi tanpa samaran UA peramban dan tanpa header Referer asing; bendera cleanup hanya terpasang bila pembersihan benar berjalan.
- Poster (debug.26–.28): `resolvePoster` hidup lagi — resolusi langsung ke API pemetaan Kitsu (anilistId -> poster/cover media.kitsu.app) karena poster s4.anilist.co dibalas 403 dari mana pun (372 judul). debug.27: Kitsu menolak `Accept: application/json` (406) — pengambilan memakai `Accept: application/vnd.api+json`. debug.28: URL Kitsu bertanda tangan X-Amz (kedaluwarsa 15 menit) tidak disimpan permanen.
- Terverifikasi emulator CI (bukti emu-evidence-27): Beranda penuh poster asli, AoT S3P2 ep 1 terputar, logcat FATAL 0.

# Putaran 4 (19 Okt 2026) — fitur pemutar + akun
- Pemutar: pemutaran latar + MediaSession (notifikasi, layar kunci, headset; `PlaybackService`, setting "Background playback", video dimatikan saat di latar), pilihan trek audio, kualitas dari rendisi nyata, kartu "Up next" (20 dtk terakhir), daftar episode, timer tidur, kunci kontrol, kecepatan 0,5–2×, saran turunkan kualitas saat 3x macet/90 dtk.
- Akun: hapus akun (Edge Function), refresh token, token di EncryptedSharedPreferences, sinkronisasi My List/progres (`data/Sync.kt`, `backend/supabase/schema.sql`).
- Lain: tautan dalam `onimeku://anime/<id>` + tombol Share, `CrashLog` lokal + "Send diagnostics", cek ruang sebelum unduh, uji unit (`CoreTest`), signingConfig + workflow `release.yml`, README diperbarui.
- Belum dikompilasi/diukur di sini (tanpa Android SDK).

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
