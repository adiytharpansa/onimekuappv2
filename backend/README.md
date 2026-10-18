# Backend Supabase (opsional, untuk akun)

1. Jalankan `supabase/schema.sql` di SQL editor (tabel `user_state` + RLS).
2. `supabase functions deploy delete-account` (fungsi hapus akun; wajib Google Play).
3. Isi `ONIMEKU_SUPABASE_URL` dan `ONIMEKU_SUPABASE_KEY` (kunci publik) di gradle.properties / secret CI.
