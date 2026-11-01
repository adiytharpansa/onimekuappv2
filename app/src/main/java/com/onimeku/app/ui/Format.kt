package com.onimeku.app.ui

/**
 * Format waktu pemutaran (murni, tanpa Android).
 *
 * Sengaja TIDAK di `Components.kt`: berkas itu punya `val` top-level yang
 * memakai grafis Android (`LogoO` -> `PathParser`/`Path`). Begitu pengujian
 * unit JVM menyentuh satu saja anggota berkas itu, JVM menginisialisasi
 * SELURUH `ComponentsKt` dan langsung gagal (`ExceptionInInitializerError`).
 * `fmt` dipakai pemutar, daftar episode, widget, dan SubSync — jadi harus
 * tetap bisa diuji tanpa emulator.
 */
fun fmt(t: Int): String {
    val s = t.coerceAtLeast(0)
    val h = s / 3600; val m = s % 3600 / 60; val sec = s % 60
    val ss = sec.toString().padStart(2, '0')
    return if (h > 0) "$h:${m.toString().padStart(2, '0')}:$ss" else "$m:$ss"
}
