package com.onimeku.app.data

/**
 * Logika murni pengingat mingguan (tanpa Android/WorkManager, bisa diuji unit).
 *
 * Himpunan pengingat efektif sebuah profil = pengingat eksplisit (remind) digabung
 * My List (myList): judul yang masuk daftar tontonan ikut memicu kabar episode baru.
 * Dibatasi hanya judul yang masih relevan untuk diingatkan mingguan:
 * ada di katalog, punya hari jadwal (day != 0), dan belum selesai
 * (status "Completed": apiStatus FINISHED atau episode terbit sudah mencapai total).
 */
object ReminderLogic {
    fun effectiveIds(p: Profile, byId: Map<String, Anime>): Set<String> =
        (p.remind + p.myList).filter { id ->
            val a = byId[id] ?: return@filter false
            a.day != 0 && a.status != "Completed"
        }.toSet()
}
