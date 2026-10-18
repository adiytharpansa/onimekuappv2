package com.onimeku.app.data

import com.onimeku.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Sinkronisasi My List + progres tonton ke Supabase (tabel `user_state`,
 * RLS per auth.uid(); skema di backend/supabase/schema.sql). Satu baris per
 * pengguna berisi daftar profil. Penggabungan bersifat gabung-tambah
 * (union): item yang dihapus di satu perangkat bisa muncul lagi dari perangkat lain.
 */
object Sync {
    class Unauthorized : Exception("unauthorized")

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val JSON = "application/json".toMediaType()
    private val base get() = BuildConfig.SUPABASE_URL.trimEnd('/')
    val enabled: Boolean get() = BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_KEY.isNotBlank()

    private fun req(path: String, acc: Account) = Request.Builder().url(base + path)
        .header("apikey", BuildConfig.SUPABASE_KEY)
        .header("Authorization", "Bearer ${acc.accessToken}")

    suspend fun pull(acc: Account): List<Profile>? = withContext(Dispatchers.IO) {
        if (!enabled) return@withContext null
        Net.client.newCall(req("/rest/v1/user_state?user_id=eq.${acc.userId}&select=data", acc).get().build()).execute().use { r ->
            if (r.code == 401) throw Unauthorized()
            if (!r.isSuccessful) return@withContext null
            val arr = json.parseToJsonElement(r.body?.string().orEmpty()).jsonArray
            val data = arr.firstOrNull()?.jsonObject?.get("data")?.jsonObject?.get("profiles") ?: return@withContext null
            json.decodeFromJsonElement(ListSerializer(Profile.serializer()), data)
        }
    }

    suspend fun push(acc: Account, profiles: List<Profile>) = withContext(Dispatchers.IO) {
        if (!enabled) return@withContext
        val body = buildJsonObject {
            put("user_id", acc.userId)
            put("data", buildJsonObject {
                put("profiles", json.encodeToJsonElement(ListSerializer(Profile.serializer()), profiles))
            })
        }.toString()
        val rq = req("/rest/v1/user_state?on_conflict=user_id", acc)
            .header("Prefer", "resolution=merge-duplicates,return=minimal")
            .post(body.toRequestBody(JSON)).build()
        Net.client.newCall(rq).execute().use { r ->
            if (r.code == 401) throw Unauthorized()
        }
    }

    /** Gabungkan profil lokal dengan profil jarak jauh (cocok per nama, maks 4 profil). */
    fun merge(local: List<Profile>, remote: List<Profile>): List<Profile> {
        val byName = remote.associateBy { it.name }
        val merged = local.map { l ->
            val r = byName[l.name] ?: return@map l
            val seen = (l.seen + r.seen).distinct()
            val prog = (l.prog.keys + r.prog.keys)
                .filter { it !in seen }
                .associateWith { k -> maxOf(l.prog[k] ?: 0, r.prog[k] ?: 0) }
            val watch = (l.watch + r.watch).groupBy { it.id }.values
                .map { g -> g.maxBy { it.at } }.sortedByDescending { it.at }.take(24)
            l.copy(
                myList = (l.myList + r.myList).distinct(),
                prog = prog, seen = seen, watch = watch,
                remind = (l.remind + r.remind).distinct(),
                last = l.last ?: r.last, base = l.base ?: r.base,
            )
        }
        val extra = remote.filter { r -> local.none { it.name == r.name } }
        return (merged + extra).take(4)
    }
}
