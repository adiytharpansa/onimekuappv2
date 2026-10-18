package com.onimeku.app.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable data class Dl(val id: String, val ep: Int)
@Serializable data class Last(val id: String, val ep: Int)

/** Sesi akun yang sedang masuk (null = tamu). */
@Serializable data class Account(val userId: String, val email: String, val accessToken: String, val refreshToken: String)

@Serializable
data class Profile(
    val name: String,
    val myList: List<String> = emptyList(),
    val prog: Map<String, Int> = emptyMap(),
    val seen: List<String> = emptyList(),
    val last: Last? = null,
    val base: String? = null,
    val remind: List<String> = emptyList(),
)

@Serializable
data class Settings(
    val autoplay: Boolean = true,
    val skipIntro: Boolean = true,
    val wifi: Boolean = false,
    val alerts: Boolean = true,
    val quality: String = "Auto",
)

@Serializable
data class AppState(
    val welcomeSeen: Boolean = false,
    val downloads: List<Dl> = emptyList(),
    val notifRead: Boolean = false,
    val hist: List<String> = emptyList(),
    val fired: Map<String, String> = emptyMap(),
    val settings: Settings = Settings(),
    val profiles: List<Profile> = listOf(Profile("Me")),
    val pi: Int = 0,
    val account: Account? = null,
)

class Store(ctx: Context) {
    private val sp = ctx.getSharedPreferences("onimeku", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun load(): AppState = try {
        sp.getString("state", null)?.let { json.decodeFromString<AppState>(it) } ?: AppState()
    } catch (e: Exception) { AppState() }

    fun save(s: AppState) {
        sp.edit().putString("state", json.encodeToString(s)).apply()
    }
}
