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
