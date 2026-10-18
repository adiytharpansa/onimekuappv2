package com.onimeku.app

/**
 * Lokalisasi antarmuka (debug.31). Bahasa aktif di [lang] ("en"/"id"),
 * disinkronkan dari Settings di App(). Pemanggil memakai
 * `I18n.t("kunci", "English fallback")`: teks Inggris tetap tertulis di
 * tempat pemakaian sebagai sumber & cadangan, terjemahan Indonesia
 * dipusatkan di sini. Kunci yang belum diterjemahkan jatuh ke Inggris.
 * Isi data (judul, sinopsis dari API) tidak diterjemahkan.
 */
object I18n {
    @Volatile var lang: String = "en"

    fun t(key: String, en: String): String =
        if (lang == "id") ID[key] ?: en else en

    private val ID = mapOf(
        // Navigasi & umum
        "tab_home" to "Beranda",
        "tab_calendar" to "Kalender",
        "tab_mylist" to "Daftarku",
        "tab_download" to "Unduhan",
        "tab_profile" to "Profil",
        "see_all" to "Lihat semua",
        "try_again" to "Coba lagi",
        "search_hint" to "Cari anime…",
        "home" to "Beranda",
        "catalog_unavail_t" to "Katalog tidak tersedia",
        "catalog_unavail_b" to "Katalog tidak dapat dimuat. Periksa koneksimu, lalu coba lagi.",
        "calendar_t" to "Kalender Rilis",
        "calendar_sub" to "Episode yang tayang minggu ini.",
        "nothing_airing_t" to "Tidak ada yang tayang",
        "nothing_airing_b" to "Tidak ada episode baru yang dijadwalkan untuk hari ini.",
        "mylist_empty_t" to "Daftarmu kosong",
        "mylist_empty_b" to "Ketuk Daftarku pada anime mana pun untuk menyimpannya di sini.",
        "no_results_t" to "Tidak ada hasil",
        "no_results_b" to "Tidak ada yang cocok. Coba kurangi filter atau periksa ejaannya.",

        "episodes" to "Episode",
        "episode" to "Episode",
        "comments" to "Komentar",
        "added" to "Ditambahkan",
        "reminder_on" to "Pengingat aktif",
        "search" to "Cari",
        "dl_none_t" to "Belum ada unduhan",
        "dl_none_b" to "Buka anime lalu ketuk ikon unduh pada sebuah episode untuk menontonnya luring.",
        "browse_anime" to "Jelajahi anime",
        "search_hint2" to "Cari anime atau genre",
        // Beranda
        "sec_continue" to "Lanjut Menonton",
        "sec_because" to "Karena kamu menonton",
        "sec_top" to "Anime Terpopuler",
        "sec_new_eps" to "Episode Baru Rilis",
        "sec_airing" to "Sedang Tayang",
        "sec_popular" to "Populer",
        "sec_latest" to "Terbaru",
        "sec_donghua" to "Donghua",
        "sec_all" to "Semua Anime",
        "browse_all" to "Jelajahi semua",
        "play" to "Putar",
        "my_list" to "Daftarku",
        // Detail
        "share" to "Bagikan",
        "download_all" to "Unduh semua",
        "read_more" to "Baca selengkapnya",
        "read_less" to "Tutup",
        "resume" to "Lanjut Ep",
        "sign_in_to_comment" to "Masuk untuk berkomentar",
        "comment_hint" to "Tulis komentar…",
        "comment_post" to "Kirim",
        "comment_empty" to "Belum ada komentar. Jadilah yang pertama.",
        "comment_delete" to "Hapus",
        // Pemutar
        "loading_stream" to "Memuat stream…",
        "stream_unavailable" to "Stream episode ini sedang tidak tersedia.",
        "subtitles" to "Subtitle",
        "quality" to "Kualitas",
        "skip_intro" to "Lewati intro",
        "speed" to "Kecepatan",
        "wifi_only_title" to "Mode Wi-Fi saja aktif",
        "wifi_only_body" to "Streaming lewat data seluler dimatikan di Pengaturan. Sambungkan ke Wi-Fi atau matikan opsi itu.",
        "open_settings" to "Buka Pengaturan",
        // Kalender
        "remind_me" to "Ingatkan aku",
        "reminded" to "Diingatkan",
        "no_releases" to "Tidak ada rilis pada hari ini.",
        // Unduhan
        "downloads_empty" to "Belum ada unduhan. Simpan episode saat daring agar bisa ditonton luring.",
        "remove_all" to "Hapus semua",
        // Profil & pengaturan
        "settings" to "Pengaturan",
        "language" to "Bahasa",
        "help" to "Pusat Bantuan",
        "about" to "Tentang Onimeku",
        "choose_language" to "Pilih bahasa yang dipakai aplikasi.",
        "coming_soon" to "Segera",
        "autoplay" to "Putar episode berikutnya otomatis",
        "skip_intro_s" to "Lewati intro otomatis",
        "wifi_only" to "Streaming hanya via Wi-Fi",
        "alerts" to "Notifikasi episode & pengingat",
        "bg_play" to "Putar di latar belakang",
        "quality_s" to "Kualitas video",
        "sign_in" to "Masuk",
        "sign_up" to "Daftar",
        "sign_out" to "Keluar",
        "grp_playback" to "Pemutaran",
        "autoplay_t" to "Putar episode berikutnya otomatis",
        "autoplay_d" to "Mulai episode berikutnya saat satu episode selesai.",
        "skip_intro_t" to "Lewati intro",
        "skip_intro_d" to "Tampilkan tombol untuk melewati lagu pembuka.",
        "bg_play_t" to "Putar di latar belakang",
        "bg_play_d" to "Audio tetap berjalan dengan kontrol layar kunci saat kamu keluar aplikasi.",
        "grp_data" to "Data dan notifikasi",
        "wifi_only_t" to "Streaming hanya via Wi-Fi",
        "wifi_only_d" to "Hindari pemakaian data seluler.",
        "alerts_t" to "Notifikasi episode baru",
        "alerts_d" to "Dapatkan notifikasi saat daftarmu diperbarui.",
        "terms" to "Ketentuan Layanan",
        "privacy" to "Kebijakan Privasi",
        "licenses" to "Lisensi",
        // Sambutan & akun
        "welcome_skip" to "Lewati",
        "skip_for_now" to "Lewati dulu",
        "welcome_back" to "Selamat datang kembali",
        "create_account_t" to "Buat akunmu",
        "create_account_b" to "Buat akun",
        "please_wait" to "Mohon tunggu…",
        "welcome_get_started" to "Mulai",
        "email" to "Email",
        "password" to "Kata sandi",
    )
}
