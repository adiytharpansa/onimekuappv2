### Server terjemahan subtitle (gratis)

Cloudflare Workers + Workers AI (`m2m100-1.2b`) + cache Workers KV. Berjalan di paket Workers Free.
Batas gratis (cek ulang di dashboard Cloudflare): 10.000 Neurons AI per hari; KV 100.000 baca, 1.000 tulis per hari, 1 GB.
Perkiraan: sekitar 20 episode baru per hari; episode yang sudah di-cache tidak memakai jatah AI.

#### Pasang (sekali saja)
```bash
cd server
npm i -g wrangler && wrangler login
wrangler kv namespace create SUBS      # salin "id" ke wrangler.toml (GANTI_DENGAN_ID_KV)
openssl rand -hex 16                   # kunci aplikasi (opsional tapi disarankan)
wrangler secret put APP_KEY            # tempel kunci tadi
wrangler deploy                        # catat URL: https://onimeku-translate.<akun>.workers.dev
```

#### Hubungkan ke aplikasi
Isi di `gradle.properties` (atau `-P` / env `ORG_GRADLE_PROJECT_*` di CI), lalu bangun ulang:
```
ONIMEKU_TR_URL=https://onimeku-translate.<akun>.workers.dev
ONIMEKU_TR_KEY=<kunci yang sama dengan APP_KEY>
```
Kosong = aplikasi hanya memakai ML Kit di HP seperti dulu. Isian "Translation server" di Pengaturan tetap menimpa alamat bawaan.

#### Uji
```bash
curl https://onimeku-translate.<akun>.workers.dev/health
curl -X POST https://onimeku-translate.<akun>.workers.dev \
  -H 'content-type: application/json' -H 'x-onimeku-key: <KUNCI>' \
  -d '{"source":"en","target":"id","lines":["Where are you going?","Let me go!"],"names":[]}'
```
Panggilan kedua dengan isi sama harus mengembalikan `"cached":true`.

#### Catatan
- Kunci aplikasi di APK bisa diekstrak; ini penghalang penyalahgunaan biasa, bukan keamanan penuh. Pantau pemakaian di dashboard.
- Jatah AI habis -> Worker membalas 503 -> aplikasi otomatis jatuh ke ML Kit.
- Menyimpan terjemahan subtitle dari sumber pihak ketiga punya sisi hak cipta; pastikan itu sesuai kebijakan Anda.
