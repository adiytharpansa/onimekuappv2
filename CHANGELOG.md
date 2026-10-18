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
