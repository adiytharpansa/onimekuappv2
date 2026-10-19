// Server terjemahan subtitle (opsional) untuk Cloudflare Workers — kualitas jauh di atas ML Kit.
// Pasang: wrangler deploy, lalu simpan rahasia:  wrangler secret put ANTHROPIC_API_KEY
// Isi alamat Worker ini di aplikasi: Profil -> Settings -> Subtitles -> Translation server.
//
// Kontrak (dipanggil aplikasi):
//   POST  { "source": "en", "target": "id", "lines": ["...", "..."] }
//   200   { "lines": ["...", "..."] }   <- jumlah baris HARUS sama dengan masukan
export default {
  async fetch(request, env) {
    if (request.method !== "POST") return new Response("POST only", { status: 405 });
    const { source, target, lines } = await request.json();
    if (!Array.isArray(lines) || !lines.length) return new Response("bad request", { status: 400 });
    const names = { en: "English", id: "Indonesian" };
    const prompt =
      `Translate these anime subtitle lines from ${names[source] || source} to ${names[target] || target}. ` +
      `Keep character names and honorifics (-san, -kun) unchanged, keep a natural spoken tone, ` +
      `and return ONLY a JSON array of exactly ${lines.length} strings, in the same order.\n\n` +
      JSON.stringify(lines);
    const r = await fetch("https://api.anthropic.com/v1/messages", {
      method: "POST",
      headers: {
        "content-type": "application/json",
        "x-api-key": env.ANTHROPIC_API_KEY,
        "anthropic-version": "2023-06-01",
      },
      body: JSON.stringify({
        model: "claude-haiku-5-5",
        max_tokens: 4000,
        messages: [{ role: "user", content: prompt }],
      }),
    });
    if (!r.ok) return new Response("upstream error", { status: 502 });
    const data = await r.json();
    const text = (data.content || []).map((c) => c.text || "").join("");
    let out;
    try { out = JSON.parse(text.slice(text.indexOf("["), text.lastIndexOf("]") + 1)); } catch (e) { out = null; }
    if (!Array.isArray(out) || out.length !== lines.length) return new Response("bad model output", { status: 502 });
    return Response.json({ lines: out.map(String) });
  },
};
