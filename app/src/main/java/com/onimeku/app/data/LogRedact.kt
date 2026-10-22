package com.onimeku.app.data

/**
 * Penyamar log: URL stream/segmen membawa token di query-nya (mis. /api/proxy?url=...&ref=...). Log bisa dibagikan atau
 * ikut laporan penonton, jadi query dibuang sebelum ditulis; host + path tetap ada untuk diagnosis.
 */
internal object LogRedact {
    private val URL_QUERY = Regex("""(https?://[^\s?#"'<>]+)\?[^\s"'<>)]*""")

    fun urls(s: String): String = URL_QUERY.replace(s, "\$1?…")
}
