package com.onimeku.app.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// ---------- Room: progres tonton ----------
@Entity data class Progress(@PrimaryKey val id: String, val positionMs: Long, val durationMs: Long, val updatedAt: Long)
@Dao interface ProgressDao {
    @Query("SELECT * FROM Progress WHERE id = :id") suspend fun get(id: String): Progress?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(p: Progress)
    @Query("SELECT * FROM Progress ORDER BY updatedAt DESC") fun recent(): Flow<List<Progress>>
    @Query("SELECT * FROM Progress WHERE id LIKE :animeId || ':%'") fun forAnime(animeId: String): Flow<List<Progress>>
}
@Entity data class Anime(@PrimaryKey val id: String, val title: String, val eps: Int, val synopsis: String, val poster: String?,
                         val genres: String, val airDay: Int?, val airTime: String?) {   // airDay 0=Minggu..6=Sabtu, airTime "HH:mm"
    val genreList get() = genres.split(",").filter { it.isNotBlank() }
}
@Dao abstract class AnimeDao {
    @Query("SELECT * FROM Anime ORDER BY title") abstract fun all(): Flow<List<Anime>>
    @Query("SELECT * FROM Anime WHERE id = :id") abstract fun one(id: String): Flow<Anime?>
    @Query("SELECT * FROM Anime WHERE id = :id") abstract suspend fun get(id: String): Anime?
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun upsertAll(l: List<Anime>)
    @Query("DELETE FROM Anime") abstract suspend fun clear()
    @Transaction open suspend fun replaceAll(l: List<Anime>) { clear(); upsertAll(l) }
}
@Entity data class MyItem(@PrimaryKey val animeId: String)
@Entity data class Remind(@PrimaryKey val animeId: String)
@Dao interface ListDao {
    @Query("SELECT animeId FROM MyItem") fun myList(): Flow<List<String>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun add(i: MyItem)
    @Query("DELETE FROM MyItem WHERE animeId = :id") suspend fun del(id: String)
    @Query("SELECT animeId FROM Remind") fun reminders(): Flow<List<String>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun addRemind(i: Remind)
    @Query("DELETE FROM Remind WHERE animeId = :id") suspend fun delRemind(id: String)
}
@Database(entities = [Progress::class, MyItem::class, Remind::class, Anime::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun progress(): ProgressDao
    abstract fun lists(): ListDao
    abstract fun anime(): AnimeDao
    companion object {
        @Volatile private var i: AppDb? = null
        fun get(c: Context): AppDb = i ?: synchronized(this) {
            i ?: Room.databaseBuilder(c.applicationContext, AppDb::class.java, "onimeku.db").build().also { i = it } }
    }
}

// ---------- DataStore: pengaturan & profil ----------
private val Context.ds by preferencesDataStore("onimeku")
data class Settings(
    val autoplay: Boolean = true, val skipIntro: Boolean = true, val quality: String = "Auto",
    val wifiOnly: Boolean = false, val alerts: Boolean = true,
    val profiles: List<String> = listOf("Me"), val current: Int = 0, val blocked: Set<String> = emptySet(),
    val theme: String = "Dark")   // Dark | Light | System | Dynamic

object K {
    val autoplay = booleanPreferencesKey("autoplay"); val skipIntro = booleanPreferencesKey("skipIntro")
    val quality = stringPreferencesKey("quality"); val wifiOnly = booleanPreferencesKey("wifiOnly")
    val alerts = booleanPreferencesKey("alerts"); val profiles = stringPreferencesKey("profiles")
    val current = intPreferencesKey("current"); val blocked = stringSetPreferencesKey("blocked")
    val theme = stringPreferencesKey("theme")
}

class SettingsStore private constructor(private val c: Context) {
    val flow: Flow<Settings> = c.ds.data.map { p -> Settings(
        p[K.autoplay] ?: true, p[K.skipIntro] ?: true, p[K.quality] ?: "Auto", p[K.wifiOnly] ?: false, p[K.alerts] ?: true,
        (p[K.profiles] ?: "Me").split("|").filter { it.isNotBlank() }.ifEmpty { listOf("Me") }, p[K.current] ?: 0, p[K.blocked] ?: emptySet(), p[K.theme] ?: "Dark") }
    suspend fun <T> set(k: Preferences.Key<T>, v: T) { c.ds.edit { it[k] = v } }
    suspend fun addProfile(name: String, cur: List<String>) {
        if (cur.size < 4 && name.isNotBlank()) c.ds.edit { it[K.profiles] = (cur + name.trim().take(12).replace("|", "")).joinToString("|") } }
    suspend fun removeProfile(i: Int, cur: List<String>) {
        if (cur.size > 1) c.ds.edit { it[K.profiles] = cur.filterIndexed { n, _ -> n != i }.joinToString("|"); it[K.current] = 0 } }
    suspend fun block(userId: String) { c.ds.edit { it[K.blocked] = (it[K.blocked] ?: emptySet()) + userId } }
    companion object { fun of(c: Context) = SettingsStore(c.applicationContext) }
}
