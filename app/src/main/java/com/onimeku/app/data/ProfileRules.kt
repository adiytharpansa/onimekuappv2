package com.onimeku.app.data

/** Aturan nama profil. Nama dipakai sebagai kunci penggabungan sinkronisasi, jadi harus unik (tanpa beda huruf besar/kecil). */
object ProfileRules {
    const val MAX_LEN = 12
    const val MAX_PROFILES = 4

    enum class Problem { EMPTY, DUPLICATE, FULL }

    fun clean(raw: String): String = raw.trim().replace(Regex("\\s+"), " ").take(MAX_LEN).trim()

    /** null = valid. [exceptIndex] = profil yang sedang diganti namanya (boleh sama dengan namanya sendiri). */
    fun problem(raw: String, existing: List<Profile>, exceptIndex: Int = -1): Problem? {
        val n = clean(raw)
        if (n.isEmpty()) return Problem.EMPTY
        if (exceptIndex < 0 && existing.size >= MAX_PROFILES) return Problem.FULL
        if (existing.withIndex().any { (i, p) -> i != exceptIndex && p.name.equals(n, ignoreCase = true) }) return Problem.DUPLICATE
        return null
    }
}
