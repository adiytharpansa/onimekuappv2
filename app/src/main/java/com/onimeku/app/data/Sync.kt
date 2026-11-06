package com.onimeku.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Sinkronisasi My List + progres tonton ke Firestore (dokumen
 * `user_state/<uid>`; rules: hanya pemilik yang bisa membaca/menulis).
 * Daftar profil disimpan sebagai satu field string JSON agar bentuk
 * datanya persis seperti yang dikenal logika penggabungan di bawah.
 * Penggabungan myList/seen memakai cap waktu per id (lihat [mergeStamped]):
 * penghapusan yang lebih baru menang; tanpa cap (data lama) tetap union.
 */
object Sync {
    class Unauthorized : Exception("unauthorized")

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    val enabled: Boolean get() = AuthClient.configured

    /** Hasil tarikan dokumen user_state: profil + settings akun (null bila dokumen lama tanpa field settings). */
    data class Pulled(val profiles: List<Profile>, val settings: Settings?, val settingsTs: Long)

    suspend fun pull(acc: Account): Pulled? = withContext(Dispatchers.IO) {
        if (!enabled) return@withContext null
        val r = Firestore.request("GET", "/user_state/${acc.userId}", acc.accessToken)
        if (r.code == 401 || r.code == 403) throw Unauthorized()
        if (r.code == 404) return@withContext null
        if (r.code !in 200..299) return@withContext null
        val raw = Firestore.fieldStr(r.body, "profiles") ?: return@withContext null
        val profiles = try {
            json.decodeFromString(ListSerializer(Profile.serializer()), raw)
        } catch (e: Exception) {
            return@withContext null
        }
        // Field settings/settings_ts opsional: dokumen lama tidak punya -> settings null, ts 0 (diabaikan).
        val rawSettings = Firestore.fieldStr(r.body, "settings")
        val settings = rawSettings?.let {
            try { json.decodeFromString(Settings.serializer(), it) } catch (e: Exception) { null }
        }
        val settingsTs = if (settings == null) 0L else (
            r.body?.optJSONObject("fields")?.optJSONObject("settings_ts")
                ?.optString("integerValue")?.toLongOrNull() ?: 0L
            )
        Pulled(profiles, settings, settingsTs)
    }

    suspend fun push(acc: Account, profiles: List<Profile>, settings: Settings? = null, settingsTs: Long = 0) =
        withContext(Dispatchers.IO) {
            if (!enabled) return@withContext
            val fields = LinkedHashMap<String, org.json.JSONObject>()
            fields["user_id"] = Firestore.sv(acc.userId)
            fields["profiles"] = Firestore.sv(json.encodeToString(ListSerializer(Profile.serializer()), profiles))
            if (settings != null) {
                fields["settings"] = Firestore.sv(json.encodeToString(Settings.serializer(), settings))
                fields["settings_ts"] = Firestore.iv(settingsTs)
            }
            val payload = Firestore.doc(fields)
            val r = Firestore.request("PATCH", "/user_state/${acc.userId}", acc.accessToken, payload)
            if (r.code == 401 || r.code == 403) throw Unauthorized()
            if (r.code !in 200..299) throw java.io.IOException("sync push gagal: ${r.code}")
        }

    /**
     * Keputusan gabung settings antar-perangkat (murni, bisa diuji): last-write-wins.
     * Settings jarak jauh dipakai hanya bila capnya lebih baru dari cap lokal;
     * selain itu lokal dipertahankan apa adanya (termasuk capnya).
     */
    fun mergeSettings(
        local: Settings, localTs: Long,
        remote: Settings?, remoteTs: Long,
    ): Pair<Settings, Long> =
        if (remote != null && remoteTs > localTs) remote to remoteTs else local to localTs

    /**
     * Gabung satu himpunan ber-cap: sisi dengan cap lebih baru menentukan apakah id ada.
     * Cap sama/tanpa cap -> union (perilaku lama).
     */
    internal fun mergeStamped(
        lList: List<String>, lTs: Map<String, Long>,
        rList: List<String>, rTs: Map<String, Long>,
    ): Pair<List<String>, Map<String, Long>> {
        val lSet = lList.toHashSet(); val rSet = rList.toHashSet()
        val ts = HashMap<String, Long>()
        val kept = HashSet<String>()
        for (id in (lList + rList + lTs.keys + rTs.keys).distinct()) {
            val lt = lTs[id] ?: 0L; val rt = rTs[id] ?: 0L
            ts[id] = maxOf(lt, rt)
            val present = when {
                lt > rt -> id in lSet
                rt > lt -> id in rSet
                else -> id in lSet || id in rSet
            }
            if (present) kept += id
        }
        val order = (lList + rList).distinct().filter { it in kept }
        val pruned = ts.filterValues { it > 0 }.entries
            .sortedByDescending { it.value }.take(1500).associate { it.key to it.value }
        return order to pruned
    }

    /** Gabungkan profil lokal dengan profil jarak jauh (cocok per nama, maks 4 profil). */
    fun merge(local: List<Profile>, remote: List<Profile>, gone: Collection<String> = emptyList()): List<Profile> {
        val goneKeys = gone.map { it.lowercase() }.toSet()
        val byName = remote.associateBy { it.name }
        val merged = local.map { l ->
            val r = byName[l.name] ?: return@map l
            val (seen, seenTs) = mergeStamped(l.seen, l.seenTs, r.seen, r.seenTs)
            val (myList, myListTs) = mergeStamped(l.myList, l.myListTs, r.myList, r.myListTs)
            val seenSet = seen.toHashSet()
            val prog = (l.prog.keys + r.prog.keys)
                .filter { it !in seenSet }
                .associateWith { k -> maxOf(l.prog[k] ?: 0, r.prog[k] ?: 0) }
            val watch = (l.watch + r.watch).groupBy { it.id }.values
                .map { g -> g.maxBy { it.at } }.sortedByDescending { it.at }.take(24)
            l.copy(
                myList = myList, myListTs = myListTs,
                prog = prog, seen = seen, seenTs = seenTs, watch = watch,
                remind = (l.remind + r.remind).distinct(),
                last = l.last ?: r.last, base = l.base ?: r.base,
            )
        }
        val extra = remote.filter { r -> local.none { it.name == r.name } && r.name.lowercase() !in goneKeys }
        return (merged + extra).take(4)
    }
}
