# Responsif lintas perangkat: ponsel, lipat, tablet, split-screen (belum dikompilasi)
**Belum dikompilasi/dijalankan** (tanpa Android SDK). Jalankan `gradle :app:assembleDebug` dulu. Perubahan di berkas baru `ui/Responsive.kt`, serta `ui/Components.kt`, `ui/Screens.kt`, `ui/PlayerScreen.kt`, `ui/PlayerParts.kt`, `MainActivity.kt`.
- `ui/Responsive.kt` (baru): `WidthClass` (Compact <600dp, Medium 600-839dp, Expanded >=840dp, berdasarkan lebar JENDELA), `ContentMaxW` 720dp, `GridMaxW` 1200dp, `TabBarMaxW` 560dp, `pageSidePad()`, `gridCellMin()`, `Modifier.safeSides()` (menjauhi lubang kamera/bilah navigasi di sisi saat landscape).
- `Page`: konten dipusatkan, maks. 720dp, margin samping 16/24/32dp menurut kelas lebar; area gulir tetap selebar layar. Memengaruhi Profil, Settings, Unduhan, Notifikasi, Login.
- `AnimeGrid` (See all, Search, My List): sel poster adaptif 100/128/148dp, grid dipusatkan maks. 1200dp (dulu tetap 100dp sehingga tablet jadi 9+ kolom mungil).
- Beranda: jumlah kolom grid = lebar/150dp (3-10), dulu lebar/140dp (3-8).
- `TabBar`: ikon dikelompokkan di tengah maks. 560dp pada tablet.
- Pemutar: tinggi video non-fullscreen dibatasi 60% area (jendela lebar/split-screen tidak lagi menelan panel episode); lembar pengaturan non-fullscreen maks. 560dp.
- `MainActivity`: skala huruf sistem dibatasi 1,3x (tab bar/chip/kartu tidak patah pada "Font size" maksimum). Ubah/hapus `coerceAtMost(1.3f)` bila ingin mengikuti skala sistem penuh.
- Penyempurnaan: skeleton grid (`PosterGridSkeleton`) kini menghitung kolom dari lebar (dulu tetap 3) dan skeleton See all selaras dengan `AnimeGrid`; toast dibatasi 480dp; `widthClassFor(Int)` diekstrak sebagai fungsi murni + tes `ResponsiveTest`.
- Rollback cepat: kembalikan berkas di atas dari `debug.74` dan hapus `ui/Responsive.kt` serta `ResponsiveTest.kt`.

# Pemilih dubbing di halaman pemutar (belum dikompilasi)
**Belum dikompilasi/dijalankan** (tanpa Android SDK). Jalankan `gradle :app:assembleDebug` dulu. Perubahan di `ui/PlayerScreen.kt`, `ui/PlayerParts.kt`, `ui/ProfileScreens.kt`, `data/State.kt`, `I18n.kt`.
- Chip dubbing ringkas (tampilan 24dp, bendera + nama, tepi gradien halus) rata kanan sebaris dengan badge "EPISODE n" (ikon + nama dubbing aktif + panah). Ketuk membuka lembar "Dubbing" (daftar trek audio, baris ringkas dengan bendera dalam lingkaran, baris aktif berlatar merah tipis + centang; `DubOption`, `dubFlag`). Bila stream hanya punya satu trek, chip tampil statis tanpa panah.
- Nama dubbing ramah dibaca (`dubName`): label trek bila bermakna, selain itu nama bahasa dari kode ("ja" -> Japanese/Jepang).
- Pilihan dubbing disimpan permanen sebagai `Settings.dubLang` ("auto" | "ja" | "id" | "en") dan dipakai otomatis di episode berikutnya bila trek itu ada. Bisa diatur juga di Profil > Settings > Playback > "Default dubbing".
- Area ketuk chip 48dp (tampilan 28dp), umpan balik saat ditekan, deskripsi aksesibilitas ("Dubbing: Japanese") dan label aksi. Baris badge selalu setinggi 48dp agar judul tidak melompat saat chip muncul.
- Tombol dubbing (ikon not musik) juga ada di bar kontrol atas pemutar, antara tombol subtitle dan roda gigi, tampil hanya bila ada >1 trek audio. Ketuk membuka lembar "Dubbing" langsung (posisi paling umum seperti Netflix/YouTube).
- Urutan pilihan dubbing di lembar: English, Indonesia, lalu sisanya (mis. Japanese); indeks trek asli tetap dipakai saat memilih.
- Ikon aksi di bar atas pemutar (daftar episode, CC, dubbing, roda gigi) diseragamkan 20dp (sebelumnya 24dp) agar lebih rapi; area ketuk tetap 48dp.
- Menu roda gigi: baris "Audio" diganti "Dubbing". Pilihan dubbing hanya bisa muncul bila stream memuat >1 trek audio.

# v1.0.0-debug.74 (belum dikompilasi): Pemutar — pemilik ExoPlayer di luar Compose + sembilan perbaikan
**Belum dikompilasi/dijalankan** (tanpa Android SDK di lingkungan penyusun). Jalankan `gradle :app:assembleDebug` dan `gradle :app:testDebugUnitTest` dulu. Perubahan di `ui/PlayerScreen.kt`, berkas baru `ui/PlayerHolder.kt`, `ui/PlayerLogic.kt`, `data/LogRedact.kt`, tes `PlayerChainTest.kt`, serta `data/State.kt`, `data/CrashLog.kt`, `ui/ProfileScreens.kt`, `I18n.kt`.
1. **Pemilik pemutar di luar composition.** `PlayerHolder` memegang satu ExoPlayer + MediaSession lintas episode / pintu gudang / retry; `PlayerReady` meminjam lewat token dan menyetel `MediaSource` per episode (OkHttp + Referer/UA + cache/gudang tetap per episode). Pindah episode tidak lagi membongkar pemutar (tanpa layar hitam + buffering ulang). Pengambilalihan aman: pemilik lama diabaikan (`isOwner`), pemutar dilepas hanya saat pemilik terakhir pergi. `key(r, ...)` di `PlayerContent` memastikan `PlayerReady` dibangun ulang per episode. `PlayerView` dilepas dari pemutar bersama lewat `onRelease`. Pemasangan ulang subtitle memakai pabrik sumber episode yang sama (bukan pabrik bawaan pemutar).
2. **Next/Prev di notifikasi & layar kunci** lewat `ForwardingPlayer` (`EpisodeNavPlayer`) yang mengiklankan `COMMAND_SEEK_TO_NEXT/PREVIOUS` dan meneruskannya ke `step()`.
3. **Progres lebih aman:** disimpan juga saat `ON_STOP`, saat dijeda, dan SEBELUM pindah episode (`goEp`); pemilik lama tidak menyimpan posisi episode baru.
4. **Recompose per detik dipersempit:** `PlayerReady` tidak lagi membaca `pos`/`buffered` di scope-nya; waktu, bilah seek, chip skip, dan kartu "Selanjutnya" dipecah menjadi composable anak yang membaca posisi lewat lambda. (Bilah seek sendiri sudah teranimasi linear 1 dtk; polling tidak dipercepat.)
5. **i18n:** toast subtitle tidak tersedia, koneksi lambat, Fill/Fit, "Subtitles: …", "Speed", "Next episode" di layar galat, dan "dtk" di kartu Selanjutnya kini lewat `I18n.t`.
6. **Lewati intro:** tombol cadangan "Lewati 85 dtk" (episode >= 10 mnt, detik 20-240) bila API tanpa penanda intro; opsi baru `autoSkipIntro` (Profil > Playback, bawaan mati) melompati intro sekali per episode bila penanda ada.
7. **Resume presisi:** seek awal `EXACT`, lalu `CLOSEST_SYNC` setelah READY pertama (ketuk dua kali tetap cepat).
8. **Log:** query URL (token) disamarkan di `CrashLog` saat ditulis dan dibaca (`LogRedact`). Catatan: unggah log di rilis hanya terjadi atas aksi pengguna (Laporkan masalah / Send diagnostics).
9. **Tes + logika murni:** rantai cadangan (`PlaybackChain`), galat sesaat, pengawas macet (`StallPolicy`), navigasi episode (`EpisodeMath`), skip (`SkipLogic`), dan `LogRedact` diekstrak dan diuji (`PlayerChainTest`).
- Perubahan perilaku: episode berikutnya (autoplay / kartu Selanjutnya) kini hanya dari daftar episode tersedia; dulu bisa lompat ke nomor+1 yang belum tayang.
- Rollback cepat: kembalikan `ui/PlayerScreen.kt`, `data/State.kt`, `data/CrashLog.kt`, `ui/ProfileScreens.kt`, `I18n.kt` dari `debug.73` dan hapus berkas baru di atas.

# v1.0.0-debug.72 (belum dikompilasi): Jadwal AniList lebih tangguh
**Belum dikompilasi/dijalankan.** Jalankan `gradle :app:assembleDebug` dulu. Perubahan di `data/Api.kt` dan `AppVM.kt`.
- `fetchSchedule()`: percobaan ulang maks 3x untuk galat jaringan, HTTP 429 (hormati `Retry-After`, 1-10 dtk), dan 5xx; 4xx lain langsung gagal. Respons GraphQL tanpa `data` kini dianggap gagal (sebelumnya JSONException). Duplikat `anilistId:ep` dibuang.
- Cache disk `cacheDir/schedule_cache.json` (hanya episode yang belum tayang, ditulis atomik). Saat Kalender dibuka, salinan tampil seketika dengan waktu simpan aslinya ("Diperbarui X mnt lalu"), lalu diperbarui bila lebih tua dari 15 mnt atau tarik-segar.
- Bila jaringan gagal tetapi ada salinan, Kalender tetap menampilkannya; "Couldn't load schedule" hanya muncul bila benar-benar tak ada data.
- `loadSchedule()`: satu permintaan sekaligus (tidak menumpuk saat dibuka berulang).
- `CATATAN-ARSITEKTUR.md`: catatan jadwal yang usang (dulu tertulis "mengembalikan kosong") dikoreksi.
- Rollback cepat: kembalikan `data/Api.kt` dan `AppVM.kt` dari `debug.71`.

# v1.0.0-debug.71 (belum dikompilasi): Standar tata letak
**Belum dikompilasi/dijalankan.** Jalankan `gradle :app:assembleDebug` dulu, lalu periksa tiap layar di HP: banyak nilai bergeser 1-2dp.
- `ui/Spacing.kt` (`Sp`, kisi 4dp) dan `ui/Type.kt` (`Ty`, 10 ukuran huruf) baru. Sekitar 550 padding/jarak/Spacer dan 200 `fontSize` di `ui/*.kt` kini memakai token.
- Pembulatan spasi: 3,5→4; 6,7,9→8; 10,11,13→12; 14,17,18→16; 22,26→24. Huruf: 10-11→11; 11,5-12→12; 12,5-13→13; 13,5-14,5→14; 15-16→16; 17-18→18; 19-20→20; 21-22→22; 24 dan 28 tetap.
- Area ketuk 48dp: tombol mata (Auth), hapus komentar, kembali (Components dan pemutar), tombol atas pemutar, kunci, penutup panel episode, penutup petunjuk gestur, lonceng pengingat Beranda, `GlassButton`, hapus teks pencarian.
- Dibiarkan: tombol tambah di kartu poster (30dp), tombol kirim komentar (32dp), jarak bersih bilah bawah.
- Dokumen agen: ditambah bagian "Aturan tata letak".
- Rollback cepat: kembalikan `ui/*.kt` dari `debug.70` dan hapus `Spacing.kt`/`Type.kt`.

# v1.0.0-debug.70 (belum dikompilasi): Ikon pintasan merah, dokumen disesuaikan
**Belum dikompilasi/dijalankan.** Jalankan `gradle :app:assembleDebug` dulu.
- Ikon pintasan (Lanjutkan, Cari): lingkaran merah `#F20A17` + glif putih bergaris, tanpa tepi gelap — senada ikon launcher dan `Logo()`.
- `ONIMEKU-AGENT-GUIDE.md` dan `ONIMEKU-RINGKASAN-AGEN.md`: logo dikeluarkan dari pemakaian `Red` (`#A6122D`) dan ditambah baris `LogoRed` (`#F20A17`).
- Tema antarmuka tetap crimson `Red`; hanya logo dan ikon turunannya memakai `LogoRed` (aturan di `Theme.kt`).
- Rollback cepat: kembalikan dua XML `ic_shortcut_*` dari `debug.69`.

# v1.0.0-debug.69 (belum dikompilasi): Ikon garis seragam, tahap 2
**Belum dikompilasi/dijalankan.** Jalankan `gradle :app:assembleDebug` dulu.
- `ui/AppIcons.kt`: ditambah Check, Plus, Close, ChevronRight/Up/Down, ArrowLeft, Send, Swap, Mic, MicOff, serta versi aktif `BellFill` dan `BookmarkFill` (bentuk garis yang sama, badan terisi). Ikon berarah (ChevronRight, ArrowLeft, Send) otomatis dicerminkan pada bahasa kanan-ke-kiri.
- Dipakai menggantikan Material di luar pemutar: `Screens.kt`, `Components.kt`, `CalendarScreen.kt`, `CommentsUI.kt`, `QuickMenu.kt`, `VoiceSheet.kt`, `ProfileScreens.kt`, `Subtitles.kt` (tombol My List, X, panah, buka-tutup, urut, mikrofon, kembali, kirim, lonceng/penanda aktif).
- Sengaja tetap Material Filled: bintang rating, segitiga putar, `CheckCircle`, dan semua kontrol di `PlayerScreen.kt`/`PlayerParts.kt`.
- Ikon pintasan tidak diubah.
- Rollback cepat: kembalikan `AppIcons.kt` dan delapan file di atas dari `debug.68`.

# v1.0.0-debug.68 (belum dikompilasi): Ikon launcher merah + logo seragam
**Belum dikompilasi/dijalankan.** Jalankan `gradle :app:assembleDebug` dulu.
- Ikon launcher: huruf "O" putih tetap, latar diganti merah `#F20A17` (`values/colors.xml` → `ic_bg`). `play-store-icon-512.png` diwarnai ulang sama.
- `Logo()` di `ui/Components.kt` kini lingkaran merah + "O" putih yang memakai path persis `ic_launcher_foreground.xml` (menggantikan belah ketupat + cincin). Dipakai di Sambutan, Login, Tentang, dan loading pemutar.
- Ikon pintasan tidak diubah.
- Rollback cepat: kembalikan `Components.kt`, `colors.xml`, dan PNG dari `debug.67`.

# v1.0.0-debug.67 (belum dikompilasi): Ikon garis seragam
**Belum dikompilasi/dijalankan.** Jalankan `gradle :app:assembleDebug` dulu.
- `ui/AppIcons.kt` baru: ikon garis (kisi 24, garis bulat 1,9 — sama dengan `NavGlyph`) untuk Download, Share, Trash, Search, Info, Help, Lock, User, History, Clock, Globe, Message, Flame, Bell, Bookmark, Refresh, Eye/EyeOff, Settings, Inbox, Keyboard, Dice.
- Semua `Icons.Outlined.*` di layar non-pemutar diganti `AppIcons.*` (Beranda, Detail, Profil, Auth, Kalender, Komentar, Suara, menu tekan-lama, lembar pemutar). Ikon terisi dan kontrol pemutar tetap Material Filled.
- Ikon pintasan (Lanjutkan/Cari): latar #14161F + cincin merah tema + glif putih bergaris, senada ikon "O". Ikon notifikasi: lonceng garis.
- Rollback cepat: hapus `AppIcons.kt` tidak cukup; kembalikan dari `debug.66`.

# v1.0.0-debug.66 (belum dikompilasi): Audit ikon
- Ikon Download/Share di lembar pemutar kini Outlined, sama dengan layar Detail dan menu tekan-lama (`PlayerScreen.kt`); ikon hapus komentar memakai `Outlined.Delete` seperti tempat lain (`CommentsUI.kt`). Ikon kontrol pemutar (Filled) sengaja dibiarkan.

# v1.0.0-debug.65 (belum dikompilasi): Stream macet
**Belum dikompilasi/dijalankan.** Perubahan di `ui/PlayerScreen.kt`.
- Pengawas macet baru: setelah video pernah tampil, buffering terus-menerus 12 dtk -> minta ulang segmen di posisi yang sama; 15 dtk lagi -> turunkan kualitas satu tingkat (hanya sesi ini) + `prepare()` ulang; maks 3 siklus per kemacetan. Dicatat ke CrashLog ("player ... macet").
- Coba-ulang di tempat saat sudah memutar diperluas: selain CONNECTION_FAILED/TIMEOUT kini juga IO_UNSPECIFIED, TIMEOUT, dan segmen HTTP 5xx/429; jeda 1/2/3 dtk (dulu 1,5/3/4,5). 403/404 tetap langsung ke rantai cadangan.
- Buffer: min 30 dtk / maks 60 dtk, pulih rebuffer di 5 dtk (dulu 20/50/4).

# v1.0.0-debug.64 (belum dikompilasi): Subtitle tidak muncul saat stream
**Belum dikompilasi/dijalankan** (tanpa Android SDK). Jalankan `gradle :app:testDebugUnitTest :app:assembleDebug` dulu. Semua perubahan di `ui/PlayerScreen.kt`.
- **Efek pemulihan trek tidak pernah jalan** (debug.62/.63): `subRecoverTried` adalah state sekaligus kunci `LaunchedEffect`; efek menulisnya di baris pertama sehingga Compose membatalkan korutin itu di `delay` pertama dan memulai ulang efek yang langsung keluar. Kini flag biasa (`booleanArrayOf`) dan kunci efek hanya `needSubs`.
- Pemulihan kini berlaku untuk SEMUA sumber (gerbang `boleh` yang membuang trek segar untuk non-gudang dihapus) dan memakai batas unduh 20 dtk.
- `localizeSubtitles`: batas waktu kini PER TRACK dan bisa diinterupsi (`runInterruptible`). Dulu satu `withTimeoutOrNull(4 dtk)` membungkus unduhan bloking; scope baru selesai setelah semua unduhan selesai lalu hasilnya dibuang -> subtitle kosong untuk non-gudang, dan video bisa tertahan sampai ~35 dtk. Trek yang gagal dicatat ke CrashLog ("subs").
- Format subtitle kini dikenali dari ISI berkas (`sniffSub`), bukan dari URL: SRT/ASS dari URL tanpa ekstensi tidak lagi disimpan sebagai `.vtt` (nol cue). Respons 200-OK berisi halaman galat HTML/JSON ditolak, dan berkas cache lama yang rusak/salah ekstensi diperbaiki atau diunduh ulang.

# v15.2 — Ikon huruf O (20 Okt 2026)
- Ikon aplikasi hanya huruf "O" putih (Inter Display Bold, dikonversi ke outline) di atas latar datar #14161F (`@color/ic_bg`). Dipilih menggantikan varian bergradasi.
- `drawable/ic_launcher_foreground.xml` (O selebar 44dp, di dalam zona aman 66dp; juga dipakai sebagai ikon monokrom) dan `play-store-icon-512.png` diganti. Latar tetap `@color/ic_bg`.

# v15.1 — Penyempurnaan transisi (19 Okt 2026)
**Belum dikompilasi.** Jalankan `gradle :app:assembleDebug` dulu.
- ExitDialog terpasang, hero dijeda saat `AppVM.overlayOpen`.
- Pemutar tumbuh dari titik sentuhan (`PlayerOrigin`) dan menyusut kembali; rotasi portrait/landscape dianimasikan (`fullA` di `PlayerScreen`).
- Transisi tab sadar arah (`AppVM.tabDir/tabIndex`, `MainActivity`).
- `motionItem()/motionGridItem()` (Motion.kt): animasi item di Daftarku, Continue Watching, New Episode, grid hasil/All Anime; mati bila reduce-motion.
- `import androidx.compose.animation.core.animateFloat` ditambahkan ke `Screens.kt`.

# v14.1 — Hero carousel dirapikan (19 Okt 2026)
**Belum dikompilasi** (tanpa Android SDK). Jalankan `gradle :app:assembleDebug` dulu.
- Slide otomatis presisi: efek auto-slide dikunci ke `settledPage` (dulu `currentPage` membatalkan animasi di tengah jalan sehingga hero berhenti setengah-geser).
- Pager tak terbatas (modulo): slide terakhir -> pertama bergerak maju, tidak lagi menggulung mundur melewati semua halaman.
- Transisi mulus: tween 850 ms kurva halus, halaman tetangga dimuat lebih dulu (`beyondViewportPageCount = 1`), Ken Burns + overscan digabung jadi satu graphicsLayer (tidak bergetar), teks tertinggal 30% di belakang gambar sambil memudar.
- Tarik-segar: rating, judul, deskripsi, Play, "+" dan titik indikator kini ikut turun bersama "Surprise me" (tidak lagi ditahan di posisi lama sehingga tidak ada celah kosong).
- Dialog keluar: `ui/ExitDialog.kt` menggantikan AlertDialog bawaan. Batal netral, Keluar merah solid, warna/border senada tema, fade + skala halus. Teks isi baru: "Progres tontonanmu sudah tersimpan." Auto-slide hero dijeda selama dialog tampil (`AppVM.overlayOpen`).
- Search dirapikan (`SearchScreen`): judul tanpa panah kembali di tab utama (panah tetap muncul bila dibuka dari layar lain); baris chip filter bergulir sampai tepi layar dengan ujung memudar; chip aktif berisi merah (`Chip`, berlaku di seluruh app); baris "N filter aktif · Reset" menggantikan chip Clear; keadaan awal berupa ajakan singkat berikon pencarian (bukan `EmptyBox` berikon inbox); "Trending now" jadi daftar bernomor dengan thumbnail, judul 2 baris, genre, dan skor. Kunci i18n baru: `filters_active`, `reset`.
- My List kosong (`MyListScreen`): ikon bookmark, ajakan di tengah tanpa kartu berbingkai, teks "Ketuk + ...", aksi "Jelajahi anime" + chip "Kejutkan aku", dan baris rekomendasi "Mulai dari sini" (Top Hits) dengan tombol + / centang di tiap poster. Kunci i18n baru: `start_here`; `mylist_empty_t/b` diperbarui.

# v14 — Personalisasi, aksi cepat, tablet (19 Okt 2026)
**Belum dikompilasi dan belum dijalankan** (tanpa Android SDK). Jalankan `gradle :app:testDebugUnitTest :app:assembleDebug` dulu.

## Terasa lebih personal
- Hero Home: warna dominan poster (`ui/PosterColor.kt`, sampel 48x48 lewat Coil) menyepuh gradasi bawah, tombol Play, dan label HD. Poster nyaris abu-abu jatuh ke merah tema.
- Profil: kartu "Your stats" (menonton bulan ini, episode tuntas, genre teratas, runtun harian). Data baru `Profile.days` (detik per hari, lokal) diisi di `AppVM.saveProg`; logika di `data/Stats.kt` + `StatsTest`. Riwayat sebelum v14 tidak punya jam menonton.

## Lebih cepat dipakai
- Tekan lama poster (rail, grid, hasil cari) membuka bottom sheet: Daftarku, Ingatkan saya, Unduh episode pertama, Bagikan, Buka detail (`ui/QuickMenu.kt`, `LocalQuickMenu`, `AppVM.quick`). Kartu Continue Watching tidak punya menu.
- "Surprise me": chip di bawah hero Home, `AppVM.surprise()` memilih acak judul skor >= 7,5 (cadangan >= 7,0) yang belum pernah ditonton dan sudah punya episode.
- Search: ikon mik (RecognizerIntent), chip "Trending now" (Top Hits) saat kolom kosong. Riwayat pencarian dan chip genre sudah ada sebelumnya.
- Detail: tekan lama episode -> "Mark up to Ep N" / "Mark unwatched" (`markSeenUpTo`, `unmarkSeen`). Centang dan progres mini per episode sudah ada sebelumnya di `EpisodeCard`.

## Retensi
- Pintasan ikon aplikasi: `res/xml/shortcuts.xml` (Continue, Search) -> `onimeku://continue`, `onimeku://search`, ditangani `MainActivity.handleLink`. 
- Settings > "Auto-download next episode" (`Settings.autoDlNext`): saat episode berjalan > 60% dan jaringan tak terukur, episode berikutnya diunduh senyap (butuh ruang bebas >= 1 GB).

- Widget layar utama "Continue Watching" (`widget/ContinueWidget.kt`, RemoteViews tanpa dependensi baru, 4x2, bisa diubah ukurannya): poster, judul, episode, sisa waktu, bar progres. Membaca state + `catalog_cache_disk` sendiri; ketuk = `onimeku://continue?id=&ep=` langsung ke pemutar. Diperbarui tiap 30 mnt dan dari `AppVM.refreshWidget` (maks 1x/60 dtk, langsung saat episode tuntas, tandai ditonton, ganti profil). Kosong = "Nothing to continue yet" (ketuk membuka Search). Belum diuji di launcher mana pun.

## Tablet / landscape
- `Gutter` kini composable: di layar > 760 dp konten Detail/episode dipusatkan selebar ±720 dp. Hero Home: tinggi aman di landscape, memakai banner + scrim kiri + teks maks 600 dp di layar >= 700 dp. Grid "All Anime" menyesuaikan jumlah kolom (3-8).
- Belum ada layout dua panel (Detail berdampingan dengan daftar episode).

## Belum dikerjakan
- Ide 1 (pilih genre favorit saat pertama buka + baris "Genre favoritmu").
- Countdown tayang dan strip "Tayang hari ini" (butuh jadwal API), trailer (butuh URL trailer).

# v13 — Lanjutan perapian Home (19 Okt 2026)
- Kartu "New Episode Releases": lencana NEW/BARU untuk episode rilis hari ini atau kemarin (ditumpuk di atas HD).
- Hero carousel berhenti berputar selama jari menyentuh hero, lanjut setelah dilepas.
- Belum dikerjakan: auto-play trailer dan pilihan judul English/Romaji, karena API katalog belum menyediakan data trailer maupun judul alternatif.

# v12 — Perapian Home berdasarkan tinjauan UI (19 Okt 2026)

## Bug / konsistensi
- Katalog: entri ganda (poster sama atau judul sama setelah tanda baca dibuang) digabung di semua daftar tampilan; id lama tetap bisa dibuka lewat `byId`.
- "Because you watched": tidak lagi merekomendasikan season lain dari franchise yang sama; butuh minimal 2 genre sama bila anime dasar punya >= 2 genre; judul yang sudah ada di Top Hits disaring.
- Hero: judul dan sinopsis selalu memesan 2 baris sehingga tombol Play tidak bergeser antar slide; judul yang masih tayang menulis "Ep N" (bukan "N episodes").
- Grid: baris genre di Tile selalu memesan tinggi, kartu satu baris grid sama tinggi.

## Tampilan
- Hero: scrim atas dan bawah lebih pekat, sinopsis lebih terang, chip rating/status memakai kaca gelap.
- Tab bar: satu penanda aktif saja (kapsul); garis bawah dobel dibuang.
- Continue Watching: ikon play di tengah kartu, sisa waktu ("21:05 left"), progress bar 5dp.

## Struktur Home
- "All Anime" dibatasi 24 judul, ditambah chip genre dan tombol "Browse all" (daftar penuh + filter di See all).

Catatan: belum dikompilasi di sini (tanpa Android SDK). Jalankan `gradle :app:testDebugUnitTest :app:assembleDebug` / workflow CI.

## v1.0.0-debug.58 (belum dikompilasi): Konfirmasi keluar
- Tombol/gestur kembali di Beranda menampilkan dialog "Keluar dari Onimeku?" (Batal/Keluar); di tab lain kembali dulu ke Beranda. Tidak aktif saat PiP atau Welcome.

## v1.0.0-debug.57 (belum dikompilasi): PiP seperti YouTube
- PiP otomatis (tekan Home) hanya saat video diputar dan bisa dimatikan di Profil -> Picture-in-picture (`Settings.autoPip`).
- Menutup jendela PiP (X) kini menghentikan pemutaran (sebelumnya audio lanjut karena Putar di latar aktif); mengetuk jendela PiP membuka kembali pemutar dan lanjut.

## v1.0.0-debug.56 (belum dirilis): Pemutar dirapikan + fitur setara YouTube
**BELUM DIKOMPILASI / DIUJI** (tanpa Android SDK); hanya diperiksa keseimbangan kurung. Langkah pertama: `gradle :app:assembleDebug :app:testDebugUnitTest`.
- Halaman pemutar: kartu "Up next" duplikat dihapus; daftar episode ringkas bernomor (hulu belum punya thumbnail/judul per episode); petunjuk gestur bisa ditutup dan hilang sendiri setelah 25 dtk; judul tidak lagi dobel di mode portrait; baris aksi My List / Download / Share + sinopsis (ketuk untuk buka) ditambahkan.
- Kontrol: tombol Prev/Next di tengah video, Fit/Fill, tooltip waktu + penanda intro/outro (kuning) di seek bar, bilah mengalir halus (animasi linear 1 dtk), dukungan TalkBack pada seek bar.
- Gestur: geser horizontal = seek, cubit dua jari = Fill/Fit, rotasi HP ke landscape = fullscreen otomatis (portrait = keluar).
- Autoplay: hitung mundur + tombol Batal; Skip outro (bila API mengirim `outroStart/outroEnd`); timer tidur "akhir episode"; mode Hanya audio; baris Subtitles selalu tampil ("Not available" bila kosong); sheet bisa ditutup dengan geser turun, padding bawah ditambah.
- Perbaikan: durasi awal 0 (bukan 1440) -> tampil `--:--`; `fmt()` mendukung jam; progres episode memakai durasi asli (`Watch.dur`); `saveProg` tidak lagi menandai "ditonton" bila durasi belum diketahui; galat jaringan sesaat dicoba ulang di tempat (maks 3x); saat pindah ke jalur cadangan posisi terakhir dipertahankan (`AppVM.resumeMs`).
- Privasi/rilis: probe stream & unggah log otomatis hanya di build DEBUG (pengiriman manual dari About tetap ada).
- Kode: seek bar + sheet dipindah ke `ui/PlayerParts.kt`; toast pemutar lewat `I18n`; komentar `(debug.NN)` dibersihkan; tes baru `PlayerLogicTest`.
- Belum dikerjakan (butuh API/arsitektur): thumbnail seek (sprite dari API), thumbnail/judul episode, mini-player swipe-turun, Chromecast, offset subtitle, statistik jaringan.

## v1.0.0-debug.55 (belum dirilis): Coming Soon otomatis bersih saat rilis
- Coming Soon hanya berisi judul NOT_YET_RELEASED yang belum punya episode; begitu API menandai rilis atau episode muncul (ep > 0 / available), judul itu hilang dari Coming Soon dan naik ke New Episode Releases (label "Today"), slotnya diisi judul belum tayang berikutnya.
- Beranda yang dibiarkan terbuka memuat ulang katalog tiap 30 menit (selain refresh ON_RESUME >5 menit), sehingga perubahan tercermin jauh di bawah batas sehari.

## v1.0.0-debug.54 (belum dirilis): Lonceng pengingat hanya untuk Coming Soon
- Kartu Coming Soon (Beranda) kini punya tombol lonceng pengingat (kanan bawah, merah saat aktif). Itu satu-satunya lonceng di Beranda.
- Kalender: lonceng dihapus dari judul yang sudah rilis/sedang tayang; hanya muncul untuk judul belum tayang (NOT_YET_RELEASED), atau judul yang pengingat lamanya masih aktif agar bisa dimatikan (showBell/RemindBell).
- Pengingat judul belum tayang: toast/notifikasi "kami kabari saat rilis" (bukan "airs at ..."), dan ReminderWorker tidak lagi mengirim "Episode N is out now" sebelum rilis — cek ulang diam-diam tiap minggu.

## v1.0.0-debug.53 (belum dirilis): Coming Soon di Beranda
- Beranda: bagian baru "Coming Soon" tepat di atas "New Episode Releases" — kartu lanskap 16:10 (sampul lebar), lencana merah COMING SOON + tahun rilis, judul dan genre di atas gradasi; "See all" membuka grid lengkap (SeeAll "soon"). Sumber: judul berstatus NOT_YET_RELEASED dari API (Catalog.comingSoon, tahun terdekat dulu); bagian disembunyikan bila tidak ada.
- Status NOT_YET_RELEASED kini tampil "Upcoming"/"Segera hadir" (sebelumnya salah ditampilkan "Airing"); ditambahkan ke filter Status di Cari.
- Perbaikan kompilasi: SectionHead(title, subtitle, onAll) — urutan parameter diubah agar trailing lambda tetap mengikat ke onAll.

## v1.0.0-debug.52 (belum dirilis): Titik indikator di atas tombol "+"
- Titik indikator carousel dipindah ke atas tombol "+" (rata tengah dengannya, jarak HeroDotsGap 11dp); ruang sinopsis-ke-tombol dilebarkan ke HeroActionsTop 28dp agar titik tidak menempel teks. Menggantikan penempatan di bawah Play (debug.51).

## v1.0.0-debug.51 (belum dirilis): Titik indikator hero
- Titik indikator carousel dipindah tepat di bawah tombol Play (jarak 14dp) dan rata tengah dengan lebar Play, bukan lagi di tengah seluruh layar; HeroBottomPad 44dp; token HeroActionW/HeroActionGap/HeroDotsBottom di Theme.kt.

## v1.0.0-debug.50 (belum dirilis): Perapian Kalender, Daftarku, Unduhan, Cari, gestur Pemutar
- Pemutar: gestur geser vertikal (kecerahan/volume) kini punya zona aman: diabaikan di tepi kiri/kanan 28dp dan 12% atas/bawah, sehingga tidak bentrok dengan gestur kembali sistem, bilah kontrol, atau seek bar.
- Kalender: nama hari mengikuti bahasa aplikasi (sebelumnya Locale.ENGLISH tetap); daftar "Airing" tidak lagi menampilkan pil gelap kosong di thumbnail (ItemRow hanya menggambar label bila ada isi).
- Kalender/Daftarku/Unduhan/Cari: teks Reminder on/Remind me, "N saved", "N episodes", Delete all, Downloading…, Saved offline, Find your next anime, Recent searches, Clear history, Clear, "N results", Year dilokalkan via I18n (pluralisasi "s" hanya untuk Inggris).
- Waktu komentar memakai locale bahasa aplikasi.

## v1.0.0-debug.49 (belum dirilis): Perapian Detail & Pemutar
- Detail: parallax gambar hero dibatasi 7% (di bawah overhang skala 1,15) sehingga tidak ada celah di tepi atas saat digulir; font scale sistem dibatasi 1,15x di hero; "0 eps" tidak lagi tampil bila jumlah episode belum diketahui.
- Detail: teks Newest/Oldest, More like this, No episodes, Saved offline, Download episode kini lewat I18n; waktu komentar ditampilkan dalam zona waktu perangkat (bukan string UTC mentah).
- Pemutar (layar penuh): bar atas, bar bawah, dan tombol kunci diberi padding displayCutout horizontal + jarak bawah, agar tidak tertutup notch/sudut membulat pada mode edge-to-edge.
- Pemutar: HUD Brightness/Volume, "Can't play this video", "Retry", "Stream unavailable" dilokalkan via I18n.

## v1.0.0-debug.48 (belum dirilis): Perapian hero Beranda
- Hero: efek tarik-untuk-segar (stretch) kini hanya pada gambar; judul, pill, tombol Play dan "+" tidak lagi ikut membesar/terpotong atau menimpa titik indikator.
- Hero: gambar diberi overscan 20% + parallax 10% sehingga tidak ada pita gelap saat digeser; `translationX` ganda pada teks dihapus (teks halaman tidak lagi bertumpuk).
- Hero: font scale sistem dibatasi 1,15x di area hero; auto-slide kini dikunci ke `currentPage` agar sinkron dengan isi titik indikator dan reset saat geser manual.
- Lanjut Menonton: kartu lebih lebar (58% layar) supaya judul tidak terpotong. "Karena kamu menonton": nama anime pindah ke subjudul (SectionHead.subtitle).
- Token warna/jarak hero dipindah ke Theme.kt (HeroMeta, HeroSynopsis, GlassFill, GlassStroke, HeroPad, HeroBottomPad, HeroOverscan, HeroParallax).

## v1.0.0-debug.47 (belum dirilis): Pemutar ala YouTube (lanjutan)
- Bilah seek baru (YtSeekBar): tipis, segmen buffer, progres merah, bulatan penunjuk membesar saat digeser, ketuk = lompat.
- Waktu `0:42 / 24:10` + tombol layar penuh dipindah ke kiri/kanan bawah; tombol tengah jadi ikon putih polos (tanpa lingkaran merah).
- Ketuk dua kali di sisi layar: animasi setengah lingkaran dengan hitungan 10/20/30 detik, ketukan lanjutan menambah.
- Tahan layar = kecepatan 2x selama ditahan (lencana "2x" di atas).
- Roda gigi: label "Auto (720p)" sesuai resolusi yang diputar; ditambah Sleep timer, Picture-in-picture, Lock screen (ubin di bawah video dihapus); Subtitle > Subtitle style (ukuran + latar hitam, tersimpan di Settings.subSize / subBg).
- Kecepatan putar tidak lagi reset tiap pindah episode (AppVM.playerSpeed).
- Skip intro kini hanya muncul bila API mengirim Playback.introStart/introEnd (detik). Sebelumnya jendela tetap 3-90 dtk untuk semua episode. CATATAN: API saat ini belum mengirim field itu, jadi tombol belum muncul sampai backend menambahkannya.

## v1.0.0-debug.46 (belum dirilis): Pengaturan pemutar ala YouTube
- Settings: bagian "Video quality" dan "Subtitles" DIHAPUS (pilihan tetap tersimpan, kini diatur dari pemutar).
- Pemutar: tombol CC (hidup/mati subtitle sekali ketuk) + roda gigi di bilah atas (potret & layar penuh). Roda gigi membuka lembar bawah: Quality, Subtitles, Playback speed, Audio; tiap item membuka sub-halaman dengan centang. Layar penuh: panel di kanan bawah. Tombol Back menutup/mundur satu halaman.
- Ubin Speed / Subtitles / Quality / Audio di panel bawah video dihapus (pindah ke roda gigi); tersisa Sleep, PiP, Lock.

## v1.0.0-debug.45 (belum dirilis): Hero penuh saat refresh + tab bar modern
- Tarik-untuk-segar: daftar TIDAK lagi digeser turun (dulu membuka celah hitam di atas dan memotong poster hero). Poster tetap penuh sampai tepi atas; saat ditarik hanya membesar halus (stretch) dan indikator putar merah (kapsul gelap) melayang di bawah status bar.
- Tab bar: ripple/sorotan putih transparan saat disentuh DIHAPUS (indication = null); penanda aktif hanya kapsul + garis merah tua. Sentuhan = ikon mengecil sedikit.
- Ikon tab bar diganti ikon garis bulat gaya duotone buatan sendiri (ui/NavIcons.kt: Home, Calendar, Bookmark, Search, Profile), badan ikon terisi lembut saat aktif.

## v1.0.0-debug.44 (belum dirilis): Subtitle otomatis Indonesia / English
- Setting baru Settings -> Subtitles: Auto / Indonesia / English / Off (Settings.subLang, tersimpan). Auto mengikuti bahasa aplikasi (id -> Indonesia dulu, selain itu English dulu).
- Bahasa pilihan tidak ada di episode -> otomatis pakai bahasa cadangan (Indonesia <-> English, lalu trek pertama) + toast pemberitahuan.
- Pemutar: ketuk CC / tile Subtitles membuka menu (Auto, Indonesia, English, bahasa lain, Off) menggantikan putar-siklus; pilihan Indonesia/English/Off disimpan sebagai preferensi.
- Pencocokan trek ExoPlayer kini menurut bahasa (bukan indeks) agar tidak salah trek bila ada subtitle yang gagal diunduh. Berkas baru: ui/Subtitles.kt.

## v1.0.0-debug.43 (belum dirilis): PiP ala YouTube
- Jendela PiP kini punya tombol Sebelumnya / Putar-Jeda / Berikutnya (RemoteAction + BroadcastReceiver); ikon putar/jeda berganti mengikuti status.
- Rasio jendela PiP mengikuti rasio video asli; transisi masuk/keluar halus (sourceRectHint dari area video + seamless resize).
- Auto-masuk PiP hanya saat video diputar; kontrol disembunyikan di PiP; menutup PiP (X) menjeda video (kecuali Background playback aktif).
- Manifest: launchMode=singleTask + resizeableActivity, agar membuka aplikasi dari ikon saat PiP aktif tidak membuat salinan kedua.
- Android < 12 memakai parameter PiP yang sama (vm.pipParams).

## v1.0.0-debug.42 (belum dirilis): Beranda disederhanakan
- Rail per kategori (Action, Fantasy, Comedy, dst.) dan pita "Explore Genres" DIHAPUS dari Beranda; kategori kini hanya di halaman Pencarian.
- "All Anime" langsung menampilkan seluruh katalog sebagai grid 3 kolom (urut abjad, lazy per baris) alih-alih tombol.
- Bilah atas (lonceng) menjadi solid + garis tipis setelah hero tergulung, sehingga tidak menimpa judul bagian/"See all".

## v1.0.0-debug.41 (belum dirilis): Animasi lanjutan + kerapian
- Poster: angka ranking Top Hits muncul memantul berurutan; poster terangkat+miring saat ditekan (liftPress).
- Detail: header judul memudar/mengecil saat digulir; episode muncul berurutan; percikan partikel saat masuk My List (Detail + hero Beranda).
- Continue Watching & thumbnail episode: bilah progres terisi dengan animasi (AnimatedProgressBar).
- Welcome: logo masuk berputar-memantul lalu melayang; transisi Welcome -> Beranda memudar + zoom halus.
- Tab bar: penanda merah meluncur mengikuti tab aktif.
- Kerapian: IconBtn tak terpakai dihapus; judul "Latest Releases" lewat I18n (sec_latest).

## v1.0.0-debug.40 (belum dirilis): Poles Beranda + Animasi
- Indikator lingkaran merah di atas Beranda (tarik-untuk-segar) DIHAPUS; layar tidak lagi bergeser saat ditarik. Penyegaran katalog tetap jalan diam-diam (tarik, kembali ke depan, berkala).
- Hero: zoom sinematik pelan (Ken Burns), scrim atas untuk status bar, indikator slide terisi sesuai waktu 5,5 dtk.
- Tombol Play: kilau menyapu + panah bergoyang. Titik "Airing": denyut gelombang.
- Bagian baru "Explore Genres": dua pita genre bergulir otomatis berlawanan arah (ketuk -> daftar genre). Berhenti bila animasi sistem dimatikan.
- Judul bagian: bilah merah tumbuh saat muncul.

## v1.0.0-debug.39 (belum dirilis — menunggu perintah): AKAR PUTAR DITEMUKAN — tipe HLS eksplisit
- Telemetri .38 di emulator mereproduksi kegagalan HP persis: kaki proksi mati dengan ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED (UnrecognizedInputFormatException) pada master — ExoPlayer TIDAK mengenali URL /api/proxy?u=... sebagai HLS (ekstensi .m3u8 tersembunyi di parameter terenkode) lalu memperlakukannya sebagai media progresif. Uji GET polos di saat yang sama: master/varian/segmen semua 200. Di emulator kaki langsung CDN menyelamatkan putar (jaringan AS); di HP user kaki langsung terblokir operator -> semua video gagal. Ini akar seluruh saga putar HP-vs-emulator.
- Perbaikan: MediaItem kini diberi MimeTypes.APPLICATION_M3U8 secara eksplisit bila URL/directUrl/streamUrl mengandung .m3u8 (judul MP4 tidak disentuh). Telemetri .38 dipertahankan (mengonfirmasi perbaikan dari HP user secara otomatis).

## v1.0.0-debug.38 (belum dirilis — menunggu perintah): Telemetri pemutar (alat diagnosis)
- Bukti probe server: HP user mengambil playlist MASTER 6x (200) tetapi varian+segmen TIDAK PERNAH diminta — kegagalan ada di dalam aplikasi/perangkat sesudah master, tak terlihat dari server. .38 memasang alat ukur di aplikasi: (1) uji mandiri diagStreamProbe — GET polos OkHttp meniru langkah pemutar (master -> varian -> 128KB segmen 1) mencatat status/waktu tiap langkah; (2) AnalyticsListener ExoPlayer mencatat transisi state + onPlayerError (errorCodeName) + onLoadError per-URL; (3) CrashLog diunggah otomatis ke POST /api/app-log (KV app_log_v1) saat uji mandiri selesai, saat rantai putar gagal, dan saat user mengetuk Send diagnostics (berbagi manual tetap ada).
- Perbaikan kecil: pil skor hero tidak lagi berbintang ganda (teks skor dibersihkan dari karakter bintang karena ikon sudah menggambarnya).

## v1.0.0-debug.37 (belum dirilis — menunggu perintah): Perbaikan "Stream Unavailable" di HP
- Laporan HP user atas .36: AoT S3P2 ep1 berhenti di "Stream unavailable" padahal stream hidup (terverifikasi dari server: endpoint 200, playlist + segmen via proksi 200 utuh). Tersangka: fase DATA putar kehabisan waktu di 4G — resolusi dingin endpoint ~5 dtk, timeout 12 dtk x 2 putaran + batas UI 26 dtk.
- Aplikasi: data putar DIPANASKAN sejak Detail dibuka (prefetchPlayback utk episode tombol Play + episode lanjutan), TTL prefetch 2 -> 10 menit, batas total fase data 26 -> 35 dtk (jaring pengaman terakhir).
- API (my-anime-app): respons putar kini di-cache tepi Cloudflare (s-maxage=120, SWR 600) — putar ulang/episode populer dijawab seketika, tanpa resolusi dingin.

## v1.0.0-debug.36 (belum dirilis — menunggu perintah): Redesain Lengkap (paket Claude)
- Tab bar baru: pil aktif MELEBAR dgn pegas (60dp) + ikon membesar; ikon terisi (filled) utk tab aktif Calendar/My List/Download.
- Kalender: strip 7 hari lebar-sama + lonceng pengingat; My List/Unduhan/Kalender memakai kartu baris baru (ItemRow); Pencarian: kolom 54dp + tombol hapus + keadaan kosong baru.
- Profil: kartu akun gradien di paling atas + avatar besar, tanda centang profil aktif, menu berikon (Settings/Language/Help/Info). Komponen: judul layar 28sp Bold, kartu kosong berikon.
- TopBar paket ini identik dgn .35 (tidak diubah); PlayerScreen identik dgn .34 — perbaikan OptIn agen dipertahankan, berkas tidak ditimpa.

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
