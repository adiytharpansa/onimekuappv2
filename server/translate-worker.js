// Server terjemahan subtitle GRATIS untuk Cloudflare Workers: Workers AI (model m2m100) + cache Workers KV.
//
// Kontrak (sama seperti sebelumnya, jadi aplikasi lama/baru tetap cocok):
//   POST  { "source": "en", "target": "id", "lines": ["...", "..."], "names": ["Eren", "Levi"] }
//   200   { "lines": ["...", "..."], "cached": true|false }   <- jumlah baris SAMA dengan masukan
//   503   mesin AI tidak tersedia (mis. jatah harian habis) -> aplikasi otomatis memakai ML Kit di HP.
//
// Cache berbasis ISI: kunci = hash(bahasa + nama + baris). Episode yang sama dari pengguna lain menghasilkan
// kunci yang sama, sehingga hanya pengguna pertama yang memakai jatah AI. Pengguna lain mendapat hasil seketika.
// Klien tidak bisa meracuni cache: server menerjemahkan sendiri dan kunci memuat teks sumbernya.
//
// Pasang: lihat server/README-translate.md.

const MODEL = "@cf/meta/m2m100-1.2b";
const MAX_LINES = 45;          // di bawah batas 50 subrequest per pemanggilan (paket Free)
const MAX_LINE_CHARS = 1000;
const MAX_NAMES = 40;
const POOL = 8;                // terjemahan bersamaan per pemanggilan
const TTL = 60 * 60 * 24 * 365;
const LANG = /^[a-z]{2,3}$/;

const json = (o, status = 200) => Response.json(o, { status });
const text = (s, status) => new Response(s, { status });

async function sha256(s) {
  const buf = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(s));
  return [...new Uint8Array(buf)].map((b) => b.toString(16).padStart(2, "0")).join("");
}

const esc = (s) => s.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");

async function ai(env, t, src, tgt) {
  const r = await env.AI.run(MODEL, { text: t.slice(0, MAX_LINE_CHARS), source_lang: src, target_lang: tgt });
  return String(r?.translated_text ?? "").trim();
}

// Satu baris. Nama karakter dilindungi dengan penanda; bila penanda hilang dari hasil, ulangi tanpa perlindungan.
async function one(env, t, src, tgt, names) {
  if (!/\p{L}/u.test(t)) return t; // hanya simbol/angka/not musik
  const used = names.map((n, i) => ({ n, i, re: new RegExp("\\b" + esc(n) + "\\b", "g") })).filter((u) => u.re.test(t));
  used.forEach((u) => (u.re.lastIndex = 0));
  if (used.length) {
    let p = t;
    for (const u of used) p = p.replace(u.re, `QQ${u.i}QQ`);
    let o = await ai(env, p, src, tgt);
    if (o && used.every((u) => o.includes(`QQ${u.i}QQ`))) {
      for (const u of used) o = o.split(`QQ${u.i}QQ`).join(u.n);
      return o;
    }
  }
  return (await ai(env, t, src, tgt)) || t;
}

async function translateLines(env, lines, src, tgt, names) {
  const uniq = [...new Set(lines)];
  const map = new Map();
  let failed = false;
  let next = 0;
  async function run() {
    while (!failed) {
      const k = next++;
      if (k >= uniq.length) return;
      try { map.set(uniq[k], await one(env, uniq[k], src, tgt, names)); } catch (e) { failed = true; }
    }
  }
  await Promise.all(Array.from({ length: Math.min(POOL, uniq.length) }, run));
  return failed ? null : lines.map((l) => map.get(l) ?? l);
}

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);
    if (request.method === "GET" && url.pathname === "/health") return json({ ok: true, ai: !!env.AI, kv: !!env.SUBS });
    if (request.method !== "POST") return text("POST only", 405);
    if (env.APP_KEY && request.headers.get("x-onimeku-key") !== env.APP_KEY) return text("forbidden", 403);

    let b;
    try { b = await request.json(); } catch (e) { return text("bad json", 400); }
    const { source, target, lines } = b || {};
    const names = Array.isArray(b?.names) ? b.names.filter((n) => typeof n === "string" && n.length > 1).slice(0, MAX_NAMES) : [];
    if (!LANG.test(source || "") || !LANG.test(target || "") || source === target) return text("bad language", 400);
    if (!Array.isArray(lines) || !lines.length || !lines.every((l) => typeof l === "string")) return text("bad lines", 400);
    if (lines.length > MAX_LINES) return text("too many lines", 413);
    if (!env.AI) return text("AI binding missing", 500);

    const key = "t1:" + (await sha256(JSON.stringify([source, target, names, lines])));
    if (env.SUBS) {
      try {
        const hit = await env.SUBS.get(key, "json");
        if (Array.isArray(hit) && hit.length === lines.length) return json({ lines: hit, cached: true });
      } catch (e) { /* KV gagal: lanjut tanpa cache */ }
    }

    const out = await translateLines(env, lines, source, target, names);
    if (!out) return text("ai unavailable", 503);

    if (env.SUBS) ctx.waitUntil(env.SUBS.put(key, JSON.stringify(out), { expirationTtl: TTL }).catch(() => {}));
    return json({ lines: out, cached: false });
  },
};
