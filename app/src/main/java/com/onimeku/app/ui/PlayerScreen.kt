package com.onimeku.app.ui

import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import android.app.PictureInPictureParams
import android.content.Context
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Rational
import android.view.WindowManager
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.graphics.graphicsLayer
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.content.pm.ActivityInfo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.onimeku.app.AppVM
import com.onimeku.app.I18n
import com.onimeku.app.PlaybackService
import com.onimeku.app.Route
import com.onimeku.app.data.Catalog
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val PIP_ACTION = "com.onimeku.app.PIP_CONTROL"

private fun pipAction(ctx: Context, op: String, icon: Int, title: String, code: Int): android.app.RemoteAction {
    val pi = android.app.PendingIntent.getBroadcast(
        ctx, code, android.content.Intent(PIP_ACTION).setPackage(ctx.packageName).putExtra("op", op),
        android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT,
    )
    return android.app.RemoteAction(android.graphics.drawable.Icon.createWithResource(ctx, icon), title, title, pi)
}

/** Parameter PiP ala YouTube: rasio mengikuti video, tombol sebelumnya/putar-jeda/berikutnya, transisi halus dari area video. */
private fun pipParams(
    ctx: Context, auto: Boolean, playing: Boolean, ratio: Float = 16f / 9f,
    rect: android.graphics.Rect? = null, hasPrev: Boolean = false, hasNext: Boolean = false,
): PictureInPictureParams {
    val r = ratio.coerceIn(0.5f, 2.3f)
    val b = PictureInPictureParams.Builder().setAspectRatio(Rational((r * 1000).toInt(), 1000))
    val acts = ArrayList<android.app.RemoteAction>()
    if (hasPrev) acts.add(pipAction(ctx, "prev", com.onimeku.app.R.drawable.ic_pip_prev, "Previous", 1))
    acts.add(pipAction(ctx, "toggle", if (playing) com.onimeku.app.R.drawable.ic_pip_pause else com.onimeku.app.R.drawable.ic_pip_play, if (playing) "Pause" else "Play", 2))
    if (hasNext) acts.add(pipAction(ctx, "next", com.onimeku.app.R.drawable.ic_pip_next, "Next", 3))
    b.setActions(acts)
    if (rect != null && rect.width() > 0 && rect.height() > 0) b.setSourceRectHint(rect)
    if (Build.VERSION.SDK_INT >= 31) { b.setAutoEnterEnabled(auto); b.setSeamlessResizeEnabled(true) }
    return b.build()
}

private fun onWifi(ctx: Context): Boolean {
    val cm = ctx.getSystemService(ConnectivityManager::class.java)
    val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
    return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
}

@Composable
private fun VideoSurface(
    player: Player, modifier: Modifier, subScale: Float = 1f, subBg: Boolean = false, resizeMode: Int = 0,
    subColor: Int = android.graphics.Color.WHITE, subSystem: Boolean = false, subLift: Boolean = false, liftDp: Int = 80,
) {
    // Tinggi tampilan (px) dipakai mengubah tinggi angkat (dp) menjadi fraksi yang diminta SubtitleView.
    var hPx by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val density = androidx.compose.ui.platform.LocalDensity.current.density
    AndroidView(
        factory = { c -> PlayerView(c).also { it.player = player; it.useController = false } },
        update = { pv ->
            pv.resizeMode = resizeMode // 0 = FIT, 4 = ZOOM (penuh, memotong tepi); literal agar tak butuh opt-in API tak stabil
            pv.subtitleView?.let { sv ->
                if (subSystem) {
                    // Ikuti pengaturan "Teks tayang"/Captions sistem (ukuran, warna, tepi) untuk aksesibilitas.
                    sv.setApplyEmbeddedStyles(true)
                    sv.setUserDefaultStyle()
                    sv.setUserDefaultTextSize()
                } else {
                    sv.setApplyEmbeddedStyles(false)
                    sv.setFractionalTextSize(androidx.media3.ui.SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * subScale)
                    sv.setStyle(androidx.media3.ui.CaptionStyleCompat(
                        subColor, if (subBg) 0xB3000000.toInt() else android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT,
                        androidx.media3.ui.CaptionStyleCompat.EDGE_TYPE_OUTLINE, android.graphics.Color.BLACK, null,
                    ))
                }
                // Saat kontrol tampil, subtitle naik di atas bilah seek (seperti YouTube) agar tidak tertutup.
                val base = androidx.media3.ui.SubtitleView.DEFAULT_BOTTOM_PADDING_FRACTION
                sv.setBottomPaddingFraction(if (subLift && hPx > 0) maxOf(base, (liftDp * density / hPx).coerceAtMost(0.45f)) else base)
            }
        },
        modifier = modifier.onSizeChanged { hPx = it.height },
        // Pemutar dipakai bersama lintas episode: lepaskan PlayerView lama agar listener-nya tidak menumpuk.
        onRelease = { pv -> runCatching { pv.player = null } },
    )
}

internal fun subScaleOf(size: String): Float = when (size) { "small" -> 0.8f; "large" -> 1.3f; "xl" -> 1.7f; else -> 1f }

/** Warna teks subtitle dari nama pilihan ("white" | "yellow"). */
internal fun subColorOf(name: String): Int = if (name == "yellow") 0xFFFFE04B.toInt() else android.graphics.Color.WHITE

@Composable
fun PlayerScreen(vm: AppVM, r: Route.Player) {
    val ctx = LocalContext.current
    if (vm.s.settings.wifi && !onWifi(ctx)) {
        Column(Modifier.fillMaxSize().background(Bg).statusBarsPadding().padding(Sp.S16)) {
            SubHead(I18n.t("wifi_only_title", "Wi-Fi only is on"), vm)
            Text(I18n.t("wifi_only_body", "Streaming over mobile data is turned off in Settings. Connect to Wi-Fi or turn the option off."), color = InkSoft)
            Box(Modifier.padding(top = Sp.S16)) { Btn(I18n.t("open_settings", "Open Settings"), BtnStyle.Solid) { vm.openSettings() } }
        }
        return
    }
    PlayerContent(vm, r)
}

private val SUB_EXTS = listOf(".vtt", ".srt", ".ass", ".ssa")

/**
 * Tebak format subtitle dari ISI berkas (bukan dari URL). Hasil: ekstensi
 * yang cocok (".vtt" / ".srt" / ".ass"), "" bila isinya jelas BUKAN subtitle
 * (halaman galat HTML / JSON dari server teks), null bila tak dikenali.
 * debug.64: dulu ekstensi hanya ditebak dari URL dan bawaannya ".vtt" —
 * subtitle SRT/ASS dari URL tanpa ekstensi disimpan sebagai .vtt, diparse
 * sebagai WebVTT, dan hasilnya nol cue. Halaman galat 200-OK juga ikut
 * tersimpan dan dipakai ulang tiap putar.
 */
private fun sniffSub(head: ByteArray): String? {
    val s = String(head, Charsets.UTF_8).trimStart('\uFEFF', ' ', '\t', '\r', '\n')
    return when {
        s.startsWith("WEBVTT") -> ".vtt"
        s.startsWith("<!doctype", true) || s.startsWith("<html", true) || s.startsWith("{\"") -> ""
        s.contains("[Script Info]", true) || s.startsWith("[V4+ Styles]", true) -> ".ass"
        Regex("""\d{1,2}:\d{2}:\d{2},\d{3}\s*-->""").containsMatchIn(s) -> ".srt"
        else -> null
    }
}

/**
 * Satu trek: pakai ulang berkas tersimpan bila isinya memang subtitle
 * (ekstensi diperbaiki sesuai isi), selain itu unduh. null = gagal.
 * Fungsi BLOKING — dipanggil lewat runInterruptible agar batas waktu
 * benar-benar memutus panggilan jaringan.
 */
private fun fetchOneSub(
    dir: java.io.File,
    r: Route.Player,
    ready: com.onimeku.app.data.Playback,
    s: com.onimeku.app.data.ApiSubtitle,
    i: Int,
): com.onimeku.app.data.ApiSubtitle? {
    val base = "${r.id}_${r.ep}_${s.lang}_$i".replace(Regex("[^A-Za-z0-9._-]"), "_")
    SUB_EXTS.map { java.io.File(dir, base + it) }.firstOrNull { it.exists() }?.let { old ->
        val head = old.inputStream().use { ins -> val b = ByteArray(2048); b.copyOf(maxOf(ins.read(b), 0)) }
        val kind = sniffSub(head)
        if (old.length() > 0L && kind != "") {
            val want = kind ?: ".${old.extension}"
            val f = if (".${old.extension}" == want) old else java.io.File(dir, base + want).also { old.renameTo(it) }
            if (f.exists()) return s.copy(url = android.net.Uri.fromFile(f).toString())
        }
        old.delete() // berkas rusak / halaman galat tersimpan: unduh ulang
    }
    val req = okhttp3.Request.Builder().url(s.url)
        .header("Referer", s.referer ?: ready.referer ?: "")
        .header("User-Agent", com.onimeku.app.data.Net.UA).build()
    val bytes = com.onimeku.app.data.Net.client.newCall(req).execute().use { resp ->
        if (!resp.isSuccessful) return null
        resp.body?.bytes() ?: return null
    }
    if (bytes.isEmpty()) return null
    val kind = sniffSub(bytes.copyOf(minOf(bytes.size, 2048)))
    if (kind == "") return null // 200 OK tetapi isinya halaman galat
    val ext = kind ?: SUB_EXTS.firstOrNull { s.url.contains(it) } ?: ".vtt"
    val f = java.io.File(dir, base + ext)
    val tmp = java.io.File(dir, "$base$ext.tmp")
    tmp.writeBytes(bytes)
    if (!tmp.renameTo(f)) { f.writeBytes(bytes); tmp.delete() }
    return s.copy(url = android.net.Uri.fromFile(f).toString())
}

/**
 * Unduh trek subtitle [ready] ke berkas lokal (semua trek PARALEL, pakai
 * ulang berkas yang sudah ada).
 * Fungsi bersama jalur utama dan efek pemulihan trek.
 *
 * debug.64 — dua cacat lama yang membuat subtitle hilang saat stream:
 *  1. Batas waktu 4 dtk membungkus SELURUH unduhan dan unduhannya BLOKING
 *     (execute()) sehingga tak pernah benar-benar terputus: scope baru
 *     selesai setelah semua unduhan selesai, lalu hasilnya DIBUANG
 *     (null -> daftar kosong untuk non-gudang). Kini batas per trek,
 *     bisa diinterupsi, dan trek yang sudah selesai tetap dipakai.
 *  2. Trek yang gagal/lambat pada putar non-gudang menghilang tanpa
 *     jejak. Kini dicatat ke CrashLog; efek pemulihan (yang juga sudah
 *     diperbaiki) mencoba lagi di latar dengan [budgetMs] lebih longgar.
 * Putar GUDANG tetap mempertahankan URL pintu asli untuk trek yang gagal.
 */
private suspend fun localizeSubtitles(
    ctx: Context,
    r: Route.Player,
    ready: com.onimeku.app.data.Playback,
    budgetMs: Long? = null,
    offsetMs: Int = 0,
): com.onimeku.app.data.Playback {
    if (ready.subtitles.isEmpty()) return ready
    val isGudangPb = ready.source == "gudang"
    val dir = java.io.File(ctx.cacheDir, "subs").apply { mkdirs() }
    val budget = budgetMs ?: if (isGudangPb) 15_000L else 4_000L
    val loaded = kotlinx.coroutines.coroutineScope {
        ready.subtitles.mapIndexed { i, s ->
            async(kotlinx.coroutines.Dispatchers.IO) {
                val fallback = if (isGudangPb) s else null
                val got = try {
                    kotlinx.coroutines.withTimeoutOrNull(budget) {
                        kotlinx.coroutines.runInterruptible(kotlinx.coroutines.Dispatchers.IO) { fetchOneSub(dir, r, ready, s, i) }
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    com.onimeku.app.data.CrashLog.note("subs", "${r.id} ep${r.ep} unduh trek ${s.lang} gagal (${e.javaClass.simpleName})")
                    null
                }
                if (got == null) com.onimeku.app.data.CrashLog.note("subs", "${r.id} ep${r.ep} trek ${s.lang} tidak terunduh (gagal/lambat/bukan subtitle) -> ${if (fallback != null) "pakai URL pintu" else "dilewati"}")
                got ?: fallback
            }
        }.awaitAll().filterNotNull()
    }
    return ready.copy(subtitles = if (offsetMs != 0) withSubOffset(loaded, offsetMs) else loaded)
}

/**
 * Sinkronisasi subtitle: pasang berkas lokal yang waktunya digeser [offsetMs] (lihat [SubSync]). Berkas asli tidak
 * diubah; hasil geseran di-cache di sebelahnya (".s<ms>") sehingga ketukan berulang pada nilai yang sama murah.
 * Trek yang bukan berkas lokal (URL pintu gudang) dikembalikan apa adanya. 0 = kembali ke berkas asli.
 */
private suspend fun withSubOffset(
    subs: List<com.onimeku.app.data.ApiSubtitle>, offsetMs: Int,
): List<com.onimeku.app.data.ApiSubtitle> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    subs.map { s ->
        try {
            val uri = android.net.Uri.parse(s.url)
            val path = if (uri.scheme == "file") uri.path else null
            if (path == null) return@map s
            val orig = SubSync.originalPath(path)
            val target = SubSync.targetPath(orig, offsetMs)
            val src = java.io.File(orig)
            if (!src.exists()) return@map s
            val tf = java.io.File(target)
            if (offsetMs != 0 && (!tf.exists() || tf.length() == 0L)) {
                val shifted = SubSync.shift(SubSync.decode(src.readBytes()), ".${src.extension}", offsetMs)
                val tmp = java.io.File("$target.tmp")
                tmp.writeText(shifted, Charsets.UTF_8)
                if (!tmp.renameTo(tf)) { tf.writeText(shifted, Charsets.UTF_8); tmp.delete() }
            }
            s.copy(url = android.net.Uri.fromFile(tf).toString())
        } catch (e: Exception) {
            com.onimeku.app.data.CrashLog.note("subs", "geser subtitle ${s.lang} gagal (${e.javaClass.simpleName})")
            s
        }
    }
}

@Composable
private fun PlayerContent(vm: AppVM, r: Route.Player) {
    val a = Catalog.byId[r.id] ?: return
    val ctx = LocalContext.current
    val act = ctx.findActivity()
    val audio = remember { ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val quality = vm.s.settings.quality

    // Data putar diambil dari API per episode (stream + referer + subtitle).
    var attempt by remember(r) { mutableIntStateOf(0) }
    var pb by remember(r) { mutableStateOf<com.onimeku.app.data.Playback?>(null) }
    var pbOffline by remember(r) { mutableStateOf(false) }
    var pbFailed by remember(r) { mutableStateOf(false) }
    var chainDead by remember(r) { mutableStateOf(false) }
    var useDirect by remember(r) { mutableStateOf(false) }
    var mainRetry by remember(r) { mutableIntStateOf(0) }
    // Gudang: indeks pintu yang sedang dicoba (daftar pintu diurut
    // ulang dari pintu terakhir yang berhasil — lihat GudangStore).
    var doorIdx by remember(r) { mutableIntStateOf(0) }
    LaunchedEffect(r.id) { com.onimeku.app.data.GudangStore.flushReports(ctx) }
    LaunchedEffect(r, attempt) {
        pb = null; pbFailed = false; pbOffline = false
        // Data putar yang sudah di-prefetch (dari Detail) tiba nyaris seketika, lalu PlayerReady membangun ExoPlayer
        // di main thread TEPAT saat animasi masuk (380 ms) berjalan -> patah. Tahan penyerahan [pb] sampai animasi selesai.
        val t0 = android.os.SystemClock.uptimeMillis()
        suspend fun settle() { val w = 380L - (android.os.SystemClock.uptimeMillis() - t0); if (w > 0) delay(w) }
        // Cadangan unduhan lokal (debug.63): Playback putar-dari-unduhan
        // bila episode ini sudah terunduh PENUH, null bila tidak ada.
        // Dipakai SEMUA jalan gagal bootstrap — versi lama hanya
        // memeriksanya saat fetchPlayback mengembalikan null; respons
        // yang datang tetapi tanpa alamat aliran (cabang kosong di
        // bawah) langsung menyerah "Stream unavailable" tanpa pernah
        // melihat unduhan (regresi lapangan debug.62, Chainsaw Man
        // ep1: episode terunduh sehat, layar galat total). Subtitle
        // luring gudang (en/id) yang ikut terunduh dipasangkan;
        // daftar kosongnya dipulihkan efek pemulihan trek sesudah
        // putar terpasang (efek itu tidak pernah menahan bootstrap).
        suspend fun loadOffline(sebab: String): com.onimeku.app.data.Playback? {
            val dl = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                com.onimeku.app.data.Downloads.states(ctx)[com.onimeku.app.data.Downloads.key(r.id, r.ep)]
            }
            if (dl == null || dl.state != androidx.media3.exoplayer.offline.Download.STATE_COMPLETED) return null
            val offSubs = com.onimeku.app.data.GudangStore.offlineSubs(ctx, r.id, r.ep)
            com.onimeku.app.data.CrashLog.note("playback", "${r.id} ep${r.ep} $sebab -> putar unduhan, subs luring=${offSubs.size}")
            return com.onimeku.app.data.Playback(
                streamUrl = dl.request.uri.toString(), episode = r.ep,
                subtitles = offSubs,
            )
        }
        // Batas waktu TOTAL memuat data putar (debug.30, dilonggarkan
        // debug.37): endpoint hulu kadang lambat/flaky (terukur 4,6 dtk
        // dan sesekali 503 untuk Bleach ep1) dan timeout baca per
        // percobaan 12 dtk x 2 putaran bisa membuat "Loading stream..."
        // menggantung di jaringan seluler. debug.37: data putar kini
        // dipanaskan sejak halaman Detail dibuka + endpoint di-cache di
        // tepi, jadi batas ini hanya jaring pengaman terakhir (35 dtk)
        // sebelum galat jujur + tombol coba lagi tampil.
        val res = kotlinx.coroutines.withTimeoutOrNull(35_000) {
            try { com.onimeku.app.data.ApiClient.fetchPlayback(r.id, r.ep) } catch (e: Exception) { null }
        }
        if (res == null) {
            // API tak terjangkau (offline): bila episode ini sudah
            // terunduh penuh, putar dari cache unduhan — unduhan harus
            // benar-benar bisa diputar tanpa jaringan.
            val off = loadOffline("API tak terjangkau")
            if (off != null) {
                pbOffline = true
                settle()
                pb = off
            } else {
                pbFailed = true
            }
        } else if (!res.hasStream) {
            // debug.63: respons DATANG tetapi tanpa satu pun alamat
            // yang bisa diputar (degradasi sesaat hulu/fasad — bentuk
            // kegagalan sesi debug.62). Dulu cabang ini langsung
            // pbFailed: cadangan unduhan tidak pernah diperiksa dan
            // episode yang terunduh sehat ikut tak bisa diputar.
            // Kini sama seperti jalur offline: unduhan penuh menang;
            // galat jujur hanya bila unduhan pun tidak ada.
            val off = loadOffline("respons tanpa aliran")
            if (off != null) {
                pbOffline = true
                settle()
                pb = off
            } else {
                pbFailed = true
            }
        } else {
            // Subtitle diunduh ke penyimpanan lokal lebih dulu (dengan Referer
            // per-trek dari API), lalu diputar dari file. Trek subtitle yang
            // ditautkan mentah ke pemutar bisa menggagalkan seluruh putar
            // bila server teksnya menolak permintaan pemutar.
            // Perbaikan agen (berulang dari v6): tipe eksplisit non-null —
            // var tertangkap closure + diubah isinya tidak bisa smart-cast.
            var ready: com.onimeku.app.data.Playback = res
            // Opsi 2: putar GUDANG tidak lagi melokalkan subtitle —
            // subtitle gudang adalah trek teks bawaan manifest HLS;
            // daftar payload (en/id) hanya menjadi model menu, URL-nya
            // tidak diunduh dan tidak ditempel ke pemutar.
            if (ready.subtitles.isNotEmpty() && ready.source != "gudang") {
                ready = localizeSubtitles(ctx, r, ready, offsetMs = vm.s.settings.subOffsetByTitle[r.id] ?: 0)
            }
            com.onimeku.app.data.CrashLog.note("playback", "${r.id} ep${r.ep} stream=${if (ready.streamUrl.isNullOrBlank()) "none" else "ada"} embed=${if (ready.embedUrl.isNullOrBlank()) "no" else "yes"} subs=${ready.subtitles.size}")
            settle()
            pb = ready
            // Uji mandiri jalur stream: GET polos meniru langkah
            // pemutar (master -> varian -> segmen 1) lalu unggah log.
            if (com.onimeku.app.BuildConfig.DEBUG) launch { diagStreamProbe(r, ready) }
        }
    }
    // Pemulihan trek subtitle (debug.62). Bukti lapangan debug.61:
    // menu menulis Subtitles "Not available" (subTracks kosong) untuk
    // episode gudang yang muatan API-nya terbukti memuat en+id —
    // Playback yang sampai ke pemutar bisa kehilangan subtitlenya di
    // hulu sesi (API goyah -> jatuh ke putar unduhan yang berkas
    // subtitle luringnya tak pernah tersimpan), dan di .61 TIDAK ADA
    // jalan memulihkannya: daftar kosong mengunci seluruh sesi.
    // Efek ini mencoba ulang mengambil data putar segar beberapa kali
    // di latar; begitu trek tiba, HANYA subtitlenya yang dipasang ke
    // Playback berjalan (aliran/pintu tidak berubah — LaunchedEffect
    // subCount di PlayerReady memasang ulang konfigurasinya ke item
    // yang sama).
    // debug.64 — dua cacat efek pemulihan debug.62/.63 (itulah sebab trek
    // yang hilang tak pernah kembali):
    //  1. [subRecoverTried] adalah state yang sekaligus KUNCI LaunchedEffect.
    //     Efek menulisnya jadi true di baris pertama -> kunci berubah ->
    //     Compose MEMBATALKAN korutin itu (di delay pertama) dan memulai
    //     ulang efek yang langsung keluar karena flag sudah true. Pemulihan
    //     tidak pernah sempat jalan. Kini flag biasa (bukan state) dan kunci
    //     hanya "butuh trek atau tidak".
    //  2. Trek segar yang berhasil diambil dibuang untuk putar non-gudang
    //     (gerbang `boleh`). Aliran tidak disentuh sama sekali oleh efek ini,
    //     jadi gerbang itu dihapus: semua sumber boleh dipulihkan.
    val subRecoverTried = remember(r) { booleanArrayOf(false) }
    val needSubs = pb?.subtitles?.isEmpty() == true
    LaunchedEffect(r, needSubs, pbFailed) {
        if (pbFailed || !needSubs) return@LaunchedEffect
        val cur = pb ?: return@LaunchedEffect
        // Opsi 2: putar gudang TIDAK dipulihkan lewat efek ini —
        // subtitle gudang kini trek teks bawaan manifest HLS, bukan
        // trek samping, jadi tidak ada daftar samping yang diisi ulang.
        if (cur.source == "gudang") return@LaunchedEffect
        if (subRecoverTried[0]) return@LaunchedEffect
        subRecoverTried[0] = true
        val waits = longArrayOf(4_000L, 8_000L, 15_000L, 25_000L)
        for ((i, w) in waits.withIndex()) {
            delay(w)
            val fresh = try { com.onimeku.app.data.ApiClient.fetchPlayback(r.id, r.ep, fresh = true) } catch (e: Exception) { null }
            if (fresh == null || fresh.subtitles.isEmpty()) {
                com.onimeku.app.data.CrashLog.note("subs", "${r.id} ep${r.ep} pemulihan trek: percobaan ${i + 1} belum berhasil")
                continue
            }
            val prepared = localizeSubtitles(ctx, r, fresh, budgetMs = 20_000L, offsetMs = vm.s.settings.subOffsetByTitle[r.id] ?: 0)
            if (prepared.subtitles.isEmpty()) continue
            pb = (pb ?: cur).copy(subtitles = prepared.subtitles)
            com.onimeku.app.data.CrashLog.note("subs", "${r.id} ep${r.ep} trek dipulihkan: ${prepared.subtitles.size} trek (percobaan ${i + 1})")
            break
        }
    }
    if (pbFailed) {
        PlayerStatus(vm, a, r, loading = false, message = I18n.t("stream_unavailable", "This episode's stream is unavailable right now."), onRetry = { attempt++ })
        return
    }
    val playback = pb
    if (playback == null) {
        // Latar PEKAT sejak bingkai pertama: tanpa ini halaman
        // Detail di bawahnya tembus selama transisi rute — "halaman
        // ganda" pada rekaman user. Semua keadaan layar-penuh pemutar
        // (memuat/galat/rantai habis/Wi-Fi) kini mengecat Bg sendiri.
        PlayerStatus(vm, a, r, loading = true)
        return
    }
    // Sebagian judul hanya menyediakan pemutar embed (streamUrl-nya halaman
    // web, bukan berkas media) — untuk itu tampilkan pemutar embed resmi.
    val mediaUrl = playback.streamUrl?.takeIf { isMediaUrl(it) }
        ?: playback.streamProxyUrl?.takeIf { isMediaUrl(it) }
    val embedUrl = playback.embedUrl ?: playback.pageUrl
        ?: playback.streamUrl?.takeIf { !isMediaUrl(it) }
    // Meniru pemutar web: embed HANYA untuk judul yang memang tanpa
    // stream media (embed-only). Judul ber-stream tidak pernah jatuh ke
    // embed — halaman embed MegaPlay bisa sudah mati (Error 410) dan
    // menampilkannya menyembunyikan kegagalan jalur utama yang sebenarnya.
    if (mediaUrl == null && !embedUrl.isNullOrBlank()) {
        EmbedPlayer(vm, r, a, embedUrl)
        return
    }
    // Pintu gudang (episode tersalin): daftar pintu dari API,
    // diurutkan mulai dari pintu terakhir yang berhasil di HP ini.
    val doors = if (playback.source == "gudang") {
        com.onimeku.app.data.GudangStore.orderedDoors(ctx, playback.doors)
    } else {
        emptyList()
    }
    // Rantai cadangan putar (meniru web): URL utama (untuk MegaPlay =
    // proksi API) -> coba ulang utama sekali -> URL CDN langsung ->
    // gagal jujur. Embed bukan bagian rantai untuk judul ber-stream.
    // Untuk gudang: URL = pintu ke-doorIdx; jalur lama tidak dipakai.
    val streamUrl = if (doors.isNotEmpty()) {
        doors.getOrNull(doorIdx)?.url?.takeIf { isMediaUrl(it) }
    } else if (useDirect) {
        playback.directUrl?.takeIf { isMediaUrl(it) } ?: mediaUrl
    } else {
        mediaUrl
    }
    if (streamUrl == null) {
        PlayerStatus(vm, a, r, loading = false, message = I18n.t("stream_unavailable", "This episode's stream is unavailable right now."), onRetry = { attempt++ })
        return
    }

    if (chainDead) {
        PlayerStatus(vm, a, r, loading = false, message = I18n.t("stream_unavailable", "This episode's stream is unavailable right now."),
            onRetry = { chainDead = false; useDirect = false; mainRetry = 0; doorIdx = 0; attempt++ })
        return
    }

    key(r, useDirect, mainRetry, doorIdx) {
        PlayerReady(vm, r, a, ctx, act, audio, quality, playback, streamUrl, offline = pbOffline,
            onDoorSuccess = if (doors.isNotEmpty()) {
                { doors.getOrNull(doorIdx)?.let { com.onimeku.app.data.GudangStore.saveDoor(ctx, it.name) } }
            } else {
                null
            },
            onFatalError = {
            if (com.onimeku.app.BuildConfig.DEBUG) kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch { com.onimeku.app.data.ApiClient.uploadDiag("fatal") }
            // Rantai cadangan (logika murni di PlaybackChain, diuji unit): gudang -> pintu berikutnya DARI DETIK YANG SAMA
            // (posisi sudah disimpan ke vm.resumeMs di atas), maksimal satu putaran; selain gudang -> ulang jalur utama
            // sekali -> URL langsung -> habis (galat jujur + coba lagi, bukan halaman embed yang mungkin mati).
            when (val next = PlaybackChain.onFatal(doors.size, doorIdx, useDirect, mainRetry, !playback.directUrl.isNullOrBlank())) {
                is ChainStep.NextDoor -> {
                    doorIdx = next.idx
                    com.onimeku.app.data.CrashLog.note("player", "${r.id} ep${r.ep} pintu gudang gagal, pindah ke ${doors[doorIdx].name}")
                }
                ChainStep.RetryMain -> {
                    mainRetry = 1
                    com.onimeku.app.data.CrashLog.note("player", "${r.id} ep${r.ep} jalur utama gagal, mencoba ulang sekali")
                }
                ChainStep.UseDirect -> useDirect = true
                ChainStep.Dead -> {
                    chainDead = true
                    com.onimeku.app.data.CrashLog.note("player", "${r.id} ep${r.ep} " + if (doors.isNotEmpty()) "semua pintu gudang habis" else "rantai putar habis (proksi+gagal langsung)")
                }
            }
        })
    }

}

/**
 * ID stabil untuk trek subtitle samping (side-load) yang dibangun dari
 * payload API. Media3 menyalin ID ini ke Format trek (terverifikasi di
 * sumber media3 1.5.0: SingleSampleMediaSource dan jalur ekstraksi
 * DefaultMediaSourceFactory sama-sama memakai SubtitleConfiguration.id),
 * sehingga trek samping bisa dibedakan dari grup teks bawaan manifest
 * HLS. Grup manifest gudang menunjuk berkas .vtt MENTAH sebagai URI
 * rendition — bukan media playlist — jadi parser HLS Media3 menolaknya
 * ("Input does not start with the #EXTM3U header") dan grup itu tidak
 * akan pernah menghasilkan cue; memilihnya = CC tidak tampil.
 */
private const val LOCAL_SUB_ID_PREFIX = "onimeku-local-sub:"

private fun isLocalSubtitleGroup(g: androidx.media3.common.Tracks.Group): Boolean {
    if (g.type != androidx.media3.common.C.TRACK_TYPE_TEXT) return false
    val id = runCatching { g.mediaTrackGroup.getFormat(0).id }.getOrNull() ?: return false
    return id.startsWith(LOCAL_SUB_ID_PREFIX)
}

private fun subConfigs(playback: com.onimeku.app.data.Playback): List<MediaItem.SubtitleConfiguration> =
    playback.subtitles.filter { it.url.isNotBlank() }.mapIndexed { idx, s ->
        val mime = when {
            s.url.contains(".srt") -> androidx.media3.common.MimeTypes.APPLICATION_SUBRIP
            s.url.contains(".ass") || s.url.contains(".ssa") -> androidx.media3.common.MimeTypes.TEXT_SSA
            else -> androidx.media3.common.MimeTypes.TEXT_VTT
        }
        MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(s.url))
            .setMimeType(mime).setLanguage(s.lang).setLabel(s.lang.uppercase())
            .setId(LOCAL_SUB_ID_PREFIX + idx).build()
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerReady(vm: AppVM, r: Route.Player, a: com.onimeku.app.data.Anime, ctx: Context, act: android.app.Activity?, audio: AudioManager, quality: String, playback: com.onimeku.app.data.Playback, streamUrl: String, offline: Boolean = false, onDoorSuccess: (() -> Unit)? = null, onFatalError: () -> Unit = {}) {
    // Pemilik pemutar: ExoPlayer + MediaSession hidup di PlayerHolder (di luar composition) dan dipakai ulang
    // antar-episode / pintu / retry; PlayerReady hanya "meminjam" lewat token ini.
    val token = remember { Any() }
    // Pabrik sumber media episode ini (OkHttp + Referer/UA + cache/gudang): dipakai juga saat subtitle dipasang ulang di bawah,
    // karena pemutar bersama tidak lagi membawa pabrik sumber sendiri.
    val srcFactory = remember { arrayOfNulls<androidx.media3.exoplayer.source.MediaSource.Factory>(1) }
    val player = remember(r, streamUrl) {
        val exo = PlayerHolder.acquire(ctx, token)
        // Transport OkHttp BERSAMA (Net): pool koneksi + cookie dipakai ulang
        // antar-episode; UA peramban + Referer dari API (CDN menolak klien
        // tanpa pola ini).
        val dsFactory = androidx.media3.datasource.okhttp.OkHttpDataSource.Factory(com.onimeku.app.data.Net.client)
        // Perbaikan debug.25 (audit vs web): identitas permintaan dibedakan
        // per jalur. Ke PROKSI (workers.dev) JANGAN pakai samaran UA
        // peramban dan JANGAN kirim header Referer situs asing — panggilan
        // API aplikasi sendiri ("OnimekuApp/1.0", tanpa Referer) terbukti
        // lolos di jaringan HP, sedangkan pemutar ber-UA Chrome + Referer
        // megaplay gagal; proksi hanya membaca parameter ref. Samaran +
        // Referer akar origin HANYA untuk jalur langsung ke CDN (di sana
        // memang syaratnya: CDN menolak Referer halaman penuh/asing).
        val viaProxy = streamUrl.contains("/api/proxy")
        if (!viaProxy) {
            dsFactory.setUserAgent(com.onimeku.app.data.Net.UA)
            val streamHost = runCatching { java.net.URL(streamUrl).host }.getOrNull().orEmpty()
            val effReferer = if (streamHost == "nexabloom.top" || streamHost.endsWith(".nexabloom.top")) {
                "https://megaplay.buzz/"
            } else {
                playback.referer
            }
            if (!effReferer.isNullOrBlank()) {
                dsFactory.setDefaultRequestProperties(mapOf("Referer" to effReferer))
            }
        }
        // Pemutar MEMBACA cache unduhan (episode terunduh diputar dari disk)
        // tetapi TIDAK menulis ke dalamnya: sebelumnya tiap segmen yang
        // di-stream ikut ditulis ke SimpleCache ber-NoOpCacheEvictor, yaitu
        // beban I/O disk saat memutar dan penyimpanan yang tumbuh tanpa batas.
        val cacheFactory = androidx.media3.datasource.cache.CacheDataSource.Factory()
            .setCache(com.onimeku.app.data.Downloads.cache(ctx))
            .setCacheWriteDataSinkFactory(null)
            .setUpstreamDataSourceFactory(
                androidx.media3.datasource.DefaultDataSource.Factory(ctx, dsFactory)
            )
            .setFlags(androidx.media3.datasource.cache.CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        // Perbaikan debug.25: STREAMING tidak lagi membaca cache bersama.
        // Entri cache era lama (playlist/segmen rusak yang tersimpan di
        // bawah kunci URL/konten yang stabil) dibaca pemutar lebih dulu
        // dan menggagalkan putar walau server+jaringan sehat — instalasi
        // segar tidak pernah mengalaminya. Cache bersama hanya dipakai
        // untuk memutar UNDUHAN (offline); streaming murni dari jaringan.
        // Balapan pintu gudang (debug.60): untuk putar gudang daring,
        // sumber data dibungkus DoorRaceDataSource — muatan awal dan
        // muatan yang lambat/gantung dibalap ke 2-3 pintu bersamaan
        // (pemenang tercepat dipakai, pemenang dicatat sebagai pintu
        // terakhir-berhasil). Putar luring tetap cacheFactory murni;
        // jalur non-gudang tidak berubah sama sekali.
        val raceFactory = if (!offline && playback.source == "gudang" && playback.doors.size >= 2) {
            val ordered = com.onimeku.app.data.GudangStore.orderedDoors(ctx, playback.doors)
            val startName = ordered.firstOrNull { it.url == streamUrl }?.name
                ?: ordered.firstOrNull()?.name
            val session = com.onimeku.app.data.DoorRaceSession(ordered, startName) { name ->
                com.onimeku.app.data.GudangStore.saveDoor(ctx, name)
                com.onimeku.app.data.CrashLog.note("player", "${r.id} ep${r.ep} balapan pintu dimenangkan $name")
            }
            com.onimeku.app.data.DoorRaceDataSource.Factory(
                androidx.media3.datasource.DefaultDataSource.Factory(ctx, dsFactory), session)
        } else {
            null
        }
        val playFactory = if (offline) cacheFactory else raceFactory ?: androidx.media3.datasource.DefaultDataSource.Factory(ctx, dsFactory)
        val sourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(playFactory)
            // 5 percobaan ulang per segmen (bawaan 3) sebelum dianggap galat fatal.
            .setLoadErrorHandlingPolicy(androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy(5))
        // Opsi 2: blok experimentalParseSubtitlesDuringExtraction(false)
        // khusus gudang (perbaikan debug.61 untuk jalur samping) DIHAPUS
        // — subtitle gudang kini trek teks bawaan manifest HLS, tidak
        // ada lagi trek samping yang perlu dipaksa lewat SingleSample;
        // pabrik sumber dibiarkan pada bawaan Media3.
        srcFactory[0] = sourceFactory
        exo.apply {
            com.onimeku.app.data.CrashLog.note("player", "PlayerReady dibuat ep${r.ep} url=${streamUrl.take(60)}")
            // Seek resume dibaca EXACT (diset PlayerHolder.reset); dilonggarkan ke CLOSEST_SYNC saat pertama READY (lihat listener).
            quality.removeSuffix("p").toIntOrNull()?.let { h ->
                trackSelectionParameters = trackSelectionParameters.buildUpon().setMaxVideoSize(Int.MAX_VALUE, h).build()
            }
            val itemBuilder = MediaItem.Builder().setUri(android.net.Uri.parse(streamUrl))
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(a.title).setSubtitle("Episode ${r.ep}")
                        .setArtworkUri(a.poster?.let { android.net.Uri.parse(it) }).build()
                )
            // Opsi 2: putar gudang daring TIDAK diberi konfigurasi
            // subtitle samping — subtitle datang dari grup teks bawaan
            // manifest HLS (master menunjuk playlist WebVTT yang sah).
            // Jalur lain (non-gudang dan luring) tetap memasang
            // konfigurasi samping seperti sebelumnya.
            if (!(playback.source == "gudang" && !offline)) {
                itemBuilder.setSubtitleConfigurations(subConfigs(playback))
            }
            // AKAR KEGAGALAN PUTAR DI HP: URL proksi ber-path "/api/proxy" —
            // ekstensi .m3u8 hanya ada di dalam parameter terenkode —
            // sehingga ExoPlayer gagal mengenali item sebagai HLS dan
            // memperlakukannya sebagai media progresif:
            // ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED untuk SEMUA
            // judul berproksi. Di emulator kaki langsung (CDN mentah)
            // diam-diam menyelamatkan putar; di HP user kaki itu
            // terblokir operator sehingga tak ada yang tersisa.
            // Nyatakan tipe HLS secara eksplisit untuk stream playlist.
            val looksHls = streamUrl.contains(".m3u8") ||
                (playback.directUrl?.contains(".m3u8") ?: false) ||
                (playback.streamUrl?.contains(".m3u8") ?: false)
            if (looksHls) itemBuilder.setMimeType(androidx.media3.common.MimeTypes.APPLICATION_M3U8)
            // Kunci cache unduhan HANYA untuk putar offline (lihat
            // playFactory): item streaming tidak menyentuh cache bersama.
            if (offline) itemBuilder.setCustomCacheKey(com.onimeku.app.data.Downloads.key(r.id, r.ep))
            setMediaSource(sourceFactory.createMediaSource(itemBuilder.build()))
            val resumeFb = vm.resumeMs?.takeIf { it.first == "${r.id}:${r.ep}" && it.second > 0 }?.second
            vm.resumeMs = null
            seekTo(resumeFb ?: ((vm.p.prog["${r.id}:${r.ep}"] ?: 0) * 1000L))
            playWhenReady = true
            prepare()
        }
    }

    var pos by remember(r) { mutableFloatStateOf((vm.p.prog["${r.id}:${r.ep}"] ?: 0).toFloat()) }
    var dur by remember(r) { mutableIntStateOf(0) } // 0 = belum diketahui (tampil --:--)
    var wantPlay by remember(r) { mutableStateOf(true) }
    var buffering by remember(r) { mutableStateOf(true) }
    var failed by remember(r) { mutableStateOf(false) }
    var errMsg by remember(r) { mutableStateOf<String?>(null) }
    var dragging by remember(r) { mutableStateOf(false) }
    var speed by remember(r) { mutableFloatStateOf(vm.playerSpeed) }
    var br by remember { mutableFloatStateOf(1f) }
    var hud by remember { mutableStateOf<String?>(null) }
    var hudTick by remember { mutableIntStateOf(0) }
    val durNow by rememberUpdatedState(dur)
    var audioLabels by remember(r) { mutableStateOf<List<String>>(emptyList()) }
    var audioKeys by remember(r) { mutableStateOf<List<String>>(emptyList()) }
    var audioIdx by remember(r) { mutableIntStateOf(0) }
    // true = dubbing episode ini sudah ditetapkan (otomatis atau pilihan pengguna); onTracksChanged tidak menimpanya lagi.
    val dubDone = remember(r) { BooleanArray(1) }
    var heights by remember(r) { mutableStateOf<List<Int>>(emptyList()) }
    var sleepMin by remember(r) { mutableIntStateOf(0) }
    var locked by remember(r) { mutableStateOf(false) }
    var rendered by remember(r) { mutableStateOf(false) }
    var vidRatio by remember(r) { mutableFloatStateOf(16f / 9f) }
    var pipRect by remember { mutableStateOf<android.graphics.Rect?>(null) }
    val bg by rememberUpdatedState(vm.s.settings.bgPlay)

    // ---- Fullscreen, kontrol otomatis sembunyi, subtitle, kualitas ----
    // Fullscreen diwarisi dari AppVM: pindah episode membongkar
    // PlayerReady, tetapi keadaan penuhnya tidak ikut hilang.
    var full by remember { mutableStateOf(vm.playerFull) }
    var showUi by remember(r) { mutableStateOf(true) }
    var uiTick by remember { mutableIntStateOf(0) }
    val subTracks = remember(playback) { playback.subtitles.filter { it.url.isNotBlank() } }
    var subIdx by remember(r) { mutableIntStateOf(-1) }       // -1 = mati, selain itu indeks di subTracks
    var subReady by remember(r) { mutableStateOf(false) }
    // debug.60: apakah pilihan subIdx saat ini sudah diterapkan ke trek
    // samping lokal (bukan ke grup teks manifest yang tak bisa dirender)?
    var subLocalApplied by remember(r) { mutableStateOf(false) }
    // Sinkronisasi subtitle: daftar trek yang sedang terpasang (null = sama dengan playback.subtitles) dan apakah gudang
    // daring sedang memakai trek samping lokal (hanya saat geseran != 0). nativeNow() = gudang daring yang masih memakai
    // trek teks manifest HLS (dibaca saat dipanggil, jadi tidak basi di dalam listener).
    var liveSubs by remember(r) { mutableStateOf<List<com.onimeku.app.data.ApiSubtitle>?>(null) }
    var gudangSide by remember(r) { mutableStateOf(false) }
    fun nativeNow(): Boolean = playback.source == "gudang" && !offline && !gudangSide
    // Daftar trek subtitle berubah saat video sudah berjalan (mis. efek pemulihan trek): pasang ulang
    // konfigurasinya ke item yang sama, lanjut dari posisi terakhir, lalu pilih ulang sesuai preferensi.
    val subCount = playback.subtitles.count { it.url.isNotBlank() }
    var builtSubs by remember(player) { mutableIntStateOf(subCount) }
    LaunchedEffect(subCount) {
        if (subCount != builtSubs) {
            builtSubs = subCount
            // Opsi 2: putar gudang daring tidak memasang ulang
            // konfigurasi samping — trek teksnya berasal dari
            // manifest; daftar payload di sini hanya model menu.
            if (nativeNow()) return@LaunchedEffect
            val cur = player.currentMediaItem
            if (cur != null) {
                val pos = player.currentPosition
                val play = player.playWhenReady
                val item = cur.buildUpon().setSubtitleConfigurations(subConfigs(playback.copy(subtitles = liveSubs ?: playback.subtitles))).build()
                val sf = srcFactory[0]
                if (sf != null) player.setMediaSource(sf.createMediaSource(item), pos) else player.setMediaItem(item, pos)
                player.prepare()
                player.playWhenReady = play
                subReady = false
            }
        }
    }
    // Geseran waktu subtitle per judul (menu Subtitle -> Sinkronisasi). Berkas lokal ditulis ulang oleh [withSubOffset],
    // lalu trek dipasang ulang ke item yang sama dari posisi yang sama (ketukan beruntun digabung 0,4 dtk).
    // Gudang daring: trek teks manifest HLS tidak bisa ditulis ulang, jadi saat geseran != 0 trek payload (en/id) diunduh
    // ke berkas lokal, digeser, dan dipasang sebagai trek samping (jalur yang sama dengan putar non-gudang); saat kembali
    // ke 0 trek samping dilepas dan trek manifest dipakai lagi.
    val subOffsetNow = vm.s.settings.subOffsetByTitle[r.id] ?: 0
    var appliedOffset by remember(r) { mutableIntStateOf(if (playback.source == "gudang" && !offline) 0 else subOffsetNow) }
    fun reattachSubs(configs: List<MediaItem.SubtitleConfiguration>) {
        val cur = player.currentMediaItem ?: return
        val pos = player.currentPosition
        val play = player.playWhenReady
        val item = cur.buildUpon().setSubtitleConfigurations(configs).build()
        val sf = srcFactory[0]
        if (sf != null) player.setMediaSource(sf.createMediaSource(item), pos) else player.setMediaItem(item, pos)
        player.prepare()
        player.playWhenReady = play
        subReady = false
    }
    LaunchedEffect(subOffsetNow) {
        if (subOffsetNow == appliedOffset) return@LaunchedEffect
        delay(400)
        if (playback.source == "gudang" && !offline) {
            if (subOffsetNow == 0) {
                if (gudangSide) { gudangSide = false; liveSubs = null; reattachSubs(emptyList()) }
            } else {
                val got = localizeSubtitles(ctx, r, playback, budgetMs = 20_000L, offsetMs = subOffsetNow).subtitles
                if (got.none { it.url.startsWith("file:") }) {
                    vm.toast(I18n.t("sub_sync_fail", "Couldn't shift subtitles for this source"))
                } else {
                    liveSubs = got; gudangSide = true
                    reattachSubs(subConfigs(playback.copy(subtitles = got)))
                }
            }
        } else {
            val base = liveSubs ?: playback.subtitles
            val shifted = withSubOffset(base, subOffsetNow)
            if (shifted.map { it.url } != base.map { it.url }) {
                liveSubs = shifted
                reattachSubs(subConfigs(playback.copy(subtitles = shifted)))
            }
        }
        appliedOffset = subOffsetNow
    }
    // "Sinkronkan ke ucapan sekarang": geseran dihitung dari baris subtitle terdekat dengan posisi tayang saat tombol ditekan.
    val subScope = androidx.compose.runtime.rememberCoroutineScope()
    fun alignSubs() {
        val t = (liveSubs ?: playback.subtitles).filter { it.url.isNotBlank() }.getOrNull(subIdx)
        val posMs = player.currentPosition
        val curOff = vm.s.settings.subOffsetByTitle[r.id] ?: 0
        subScope.launch {
            val ms: Int? = kotlinx.coroutines.withContext<Int?>(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val path = t?.url?.let { android.net.Uri.parse(it) }?.takeIf { it.scheme == "file" }?.path
                    if (path == null) return@withContext null
                    val src = java.io.File(SubSync.originalPath(path))
                    if (!src.exists()) return@withContext null
                    val starts = SubSync.cueStartsMs(SubSync.decode(src.readBytes()), ".${src.extension}")
                    SubSync.alignOffset(starts, curOff, posMs)
                } catch (e: Exception) {
                    null
                }
            }
            if (ms == null) {
                vm.toast(I18n.t("sub_sync_none", "No subtitle line near this moment. Try again while someone is speaking"))
            } else {
                vm.setSettings { it.copy(subOffsetByTitle = SubSync.remember(it.subOffsetByTitle, r.id, ms)) }
                vm.toast(I18n.t("sub_sync_done", "Synced: %s").format(SubSync.label(ms)))
            }
        }
    }
    // Menu pengaturan ala YouTube (roda gigi): null = tertutup, "main" | "quality" | "subs" | "speed" | "audio".
    var sheet by remember { mutableStateOf<String?>(null) }
    var buffered by remember(r) { mutableFloatStateOf(0f) }
    var playingH by remember(r) { mutableIntStateOf(0) }
    var boost by remember { mutableStateOf(false) }          // tahan layar = 2x
    var autoCancel by remember(r) { mutableStateOf(false) }  // batalkan autoplay episode berikutnya
    val autoCancelNow by rememberUpdatedState(autoCancel)
    val sleepNow by rememberUpdatedState(sleepMin)           // -1 = jeda di akhir episode
    var fillMode by remember { mutableStateOf(false) }       // false = pas layar, true = penuh (zoom)
    var audioOnly by remember { mutableStateOf(false) }
    val audioOnlyNow by rememberUpdatedState(audioOnly)
    var fullByRot by remember { mutableStateOf(false) }      // fullscreen karena HP diputar (bukan tombol)
    // Tawaran melanjutkan: episode yang pernah ditonton >=30 dtk mulai dari posisi tersimpan, dengan opsi "Mulai dari awal" 7 dtk.
    val resumeStart = remember(r) { vm.p.prog["${r.id}:${r.ep}"] ?: 0 }
    var resumeChip by remember(r) { mutableStateOf(EpisodeMath.offerResume(resumeStart)) }
    LaunchedEffect(r) { if (resumeChip) { delay(7000); resumeChip = false } }
    var epPanel by remember { mutableStateOf(false) }        // daftar episode di mode layar penuh
    var barKind by remember { mutableIntStateOf(0) }         // bilah vertikal: 1 = kecerahan, 2 = volume
    var barLevel by remember { mutableFloatStateOf(0f) }
    val orient = LocalConfiguration.current.orientation
    // Portrait <-> landscape/fullscreen dianimasikan (tinggi area video tumbuh/menyusut) agar tidak "melompat".
    val reduceMotionP = LocalReduceMotion.current
    val fullA by androidx.compose.animation.core.animateFloatAsState(
        if (full) 1f else 0f,
        androidx.compose.animation.core.tween(if (reduceMotionP) 0 else 380, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "playerFull",
    )
    var dtSide by remember { mutableIntStateOf(0) }          // ketuk dua kali: -1 kiri, 1 kanan
    var dtSecs by remember { mutableIntStateOf(0) }
    var dtTick by remember { mutableIntStateOf(0) }
    val speedNow by rememberUpdatedState(speed)
    fun subName(i: Int): String = if (i < 0) I18n.t("ctl_off", "Off") else subTracks.getOrNull(i)?.let { SubLang.display(it, i) } ?: I18n.t("ctl_off", "Off")

    fun applySub(i: Int) {
        // Opsi 2 (subtitle HLS native, putar gudang daring): subtitle
        // adalah trek teks bawaan manifest — pilihan diterapkan dengan
        // mekanisme standar Media3 pada trek teks manifest: bahasa
        // pilihan lewat setPreferredTextLanguage, Off lewat
        // penonaktifan tipe trek teks. Tidak ada trek samping, unduhan
        // lokal, atau penundaan di cabang ini. Putar luring TIDAK
        // masuk cabang ini (perilakunya tidak berubah).
        if (nativeNow()) {
            val nb = player.trackSelectionParameters.buildUpon()
            if (i < 0) {
                nb.setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, true)
            } else {
                val want = subTracks.getOrNull(i)
                val code = want?.let { SubLang.code(it) ?: it.lang }
                nb.setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, false)
                if (!code.isNullOrBlank()) nb.setPreferredTextLanguage(code)
            }
            player.trackSelectionParameters = nb.build()
            subIdx = i
            com.onimeku.app.data.CrashLog.note("subs", "${r.id} ep${r.ep} applySub gudang-native($i) lang=${if (i < 0) "off" else subTracks.getOrNull(i)?.lang ?: "?"}")
            return
        }
        // Perbaikan agen (berulang dari v5/v6): konstanta C dipakai sebagai
        // qualifier nama lengkap, tidak disimpan ke val (ilegal di Kotlin).
        val b = player.trackSelectionParameters.buildUpon()
        val allText = player.currentTracks.groups.filter { it.type == androidx.media3.common.C.TRACK_TYPE_TEXT }
        // debug.60 (CC gudang tidak tampil): bila trek samping lokal dari
        // payload sudah termuat, HANYA grup itu yang boleh dipilih. Grup
        // teks manifest HLS gudang menunjuk berkas .vtt mentah (bukan
        // playlist) sehingga tidak pernah menghasilkan cue di Media3 —
        // memilihnya membuat UI tertulis "Indonesian" tanpa teks apa pun,
        // dan urutan gabungan sumber menaruh grup manifest lebih dulu.
        // Jalur non-gudang tanpa trek ber-ID lokal tidak berubah: kolam
        // cadangannya tetap semua grup teks seperti sebelumnya.
        val localText = allText.filter { isLocalSubtitleGroup(it) }
        val groups = if (localText.isNotEmpty()) localText else allText
        if (i < 0) {
            b.setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, true)
            subLocalApplied = false
        } else if (groups.isEmpty()) {
            // Trek belum termuat sama sekali: ingat saja pilihannya —
            // jangan matikan teks; onTracksChanged menerapkannya begitu
            // trek samping tiba (cabang subLocalApplied).
            subLocalApplied = false
            subIdx = i
            return
        } else {
            // Cocokkan trek menurut bahasa (urutan ke-n untuk bahasa yang sama); cadangan: indeks.
            val want = subTracks.getOrNull(i)
            val nth = if (want == null) 0 else subTracks.take(i).count { it.lang.equals(want.lang, true) }
            val byLang = if (want == null) emptyList() else groups.filter { it.mediaTrackGroup.getFormat(0).language.equals(want.lang, true) }
            val g = byLang.getOrNull(nth) ?: groups.getOrNull(i) ?: groups.first()
            b.setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, false)
                .setOverrideForType(androidx.media3.common.TrackSelectionOverride(g.mediaTrackGroup, 0))
            subLocalApplied = isLocalSubtitleGroup(g)
        }
        player.trackSelectionParameters = b.build()
        subIdx = i
    }
    fun applyQuality(q: String) {
        val b = player.trackSelectionParameters.buildUpon()
        val h = q.removeSuffix("p").toIntOrNull()
        if (h != null) b.setMaxVideoSize(Int.MAX_VALUE, h) else b.setMaxVideoSize(Int.MAX_VALUE, Int.MAX_VALUE)
        player.trackSelectionParameters = b.build()
    }
    fun applyAudio(i: Int, save: Boolean = true) {
        if (save) dubDone[0] = true
        val g = player.currentTracks.groups.filter { it.type == androidx.media3.common.C.TRACK_TYPE_AUDIO }.getOrNull(i) ?: return
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setOverrideForType(androidx.media3.common.TrackSelectionOverride(g.mediaTrackGroup, 0)).build()
        audioIdx = i
        // Pilihan pengguna jadi dubbing bawaan (tersimpan permanen), dipakai lagi di episode berikutnya.
        // Disimpan dua tingkat: bawaan global (judul baru ikut) + khusus judul ini (menang atas global saat judul ini dibuka lagi).
        if (save) audioKey(g.mediaTrackGroup.getFormat(0)).takeIf { it.isNotBlank() }?.let { k ->
            vm.setSettings { it.copy(dubLang = k, dubByTitle = DubLogic.remember(it.dubByTitle, r.id, k)) }
        }
    }
    fun seekBy(ms: Long) {
        val t = (player.currentPosition + ms).coerceIn(0L, if (player.duration > 0) player.duration else Long.MAX_VALUE)
        player.seekTo(t)
        hud = if (ms > 0) "+${ms / 1000}s" else "−${-ms / 1000}s"; hudTick++
    }

    // Ketuk dua kali di sisi kiri/kanan: geser +-10 dtk; ketukan lanjutan menambah (10, 20, 30 ...).
    fun doubleSeek(side: Int) {
        if (locked) return
        if (dtSide != side) dtSecs = 0
        dtSide = side; dtSecs += 10; dtTick++
        player.seekTo((player.currentPosition + side * 10_000L).coerceIn(0L, if (player.duration > 0) player.duration else Long.MAX_VALUE))
    }
    LaunchedEffect(dtTick) { if (dtTick > 0) { delay(800); dtSide = 0; dtSecs = 0 } }

    DisposableEffect(player) {
        val stalls = ArrayDeque<Long>()
        var wasReady = false
        var hinted = false
        var seekRelaxed = false
        var lastSeekAt = 0L
        var netRetry = 0
        var disposed = false
        val l = object : Player.Listener {
            override fun onPositionDiscontinuity(o: Player.PositionInfo, n: Player.PositionInfo, reason: Int) {
                if (reason == Player.DISCONTINUITY_REASON_SEEK) lastSeekAt = System.currentTimeMillis()
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (!PlayerHolder.isOwner(token)) return
                buffering = state == Player.STATE_BUFFERING
                // Saran kualitas: 3x macet dalam 90 dtk (di luar seek) -> tawarkan turunkan kualitas, sekali per sesi.
                val now = System.currentTimeMillis()
                if (state == Player.STATE_READY) wasReady = true
                // Resume sudah presisi (EXACT); seek berikutnya (ketuk dua kali / geser) kembali cepat ke keyframe terdekat.
                if (state == Player.STATE_READY && !seekRelaxed) { seekRelaxed = true; player.setSeekParameters(androidx.media3.exoplayer.SeekParameters.CLOSEST_SYNC) }
                if (state == Player.STATE_BUFFERING && wasReady && now - lastSeekAt > 3_000) {
                    stalls.addLast(now)
                    while (stalls.isNotEmpty() && now - stalls.first() > 90_000) stalls.removeFirst()
                    if (stalls.size >= 3 && !hinted) { hinted = true; vm.toast(I18n.t("slow_conn", "Slow connection. Try a lower quality.")) }
                }
                if (state == Player.STATE_READY) { failed = false; netRetry = 0 }
                if (state == Player.STATE_ENDED) {
                    vm.saveProg(r.id, r.ep, durNow, durNow)
                    val nxt = EpisodeMath.next(a.available, a.eps, r.ep)
                    if (sleepNow == -1) { player.pause(); vm.toast(I18n.t("sleep_done", "Sleep timer: paused")) }
                    else if (vm.s.settings.autoplay && !autoCancelNow && nxt != null) vm.replaceTop(Route.Player(r.id, nxt))
                }
            }
            override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                // Perbaikan agen: alias C0 sama ilegalnya dengan val C.
                val ag = tracks.groups.filter { it.type == androidx.media3.common.C.TRACK_TYPE_AUDIO }
                val af = ag.map { it.mediaTrackGroup.getFormat(0) }
                val keys = af.map { audioKey(it) }
                audioLabels = DubLogic.uniqueLabels(af.mapIndexed { i, f -> dubName(f, i) })
                audioKeys = keys
                audioIdx = ag.indexOfFirst { it.isSelected }.coerceAtLeast(0)
                // Dubbing otomatis SEKALI per episode (judul ini > bawaan global). Dulu dicoba tiap trek berubah, sehingga dua trek
                // berbahasa sama (mis. dua "English") selalu dikembalikan ke yang pertama dan pilihan pengguna tertimpa.
                if (!dubDone[0] && af.isNotEmpty()) {
                    dubDone[0] = true
                    val st = vm.s.settings
                    val title = st.dubByTitle[r.id]
                    val wi = DubLogic.pick(keys, title, st.dubLang)
                    if (wi != null && wi != audioIdx) applyAudio(wi, save = false)
                    val used = wi ?: audioIdx
                    // Dubbing pilihan tidak ada di episode ini (tapi ada pilihan lain): beri tahu, seperti subtitle.
                    DubLogic.fallbackNotice(keys, title, st.dubLang, used)?.let { w ->
                        vm.toast(I18n.t("dub_unavail", "%1\$s dubbing unavailable · using %2\$s").format(dubLangName(w) ?: w, audioLabels.getOrElse(used) { "?" }))
                    }
                }
                heights = tracks.groups.filter { it.type == androidx.media3.common.C.TRACK_TYPE_VIDEO }
                    .flatMap { g -> (0 until g.length).map { g.getTrackFormat(it).height } }
                    .filter { it > 0 }.distinct().sortedDescending()
                if (!subReady && tracks.groups.any { it.type == androidx.media3.common.C.TRACK_TYPE_TEXT }) {
                    subReady = true
                    val pref = vm.s.settings.subLang
                    val pick = SubLang.pick(subTracks, pref, vm.s.settings.lang)
                    // Bahasa pilihan tidak tersedia di episode ini -> beri tahu, lalu pakai bahasa cadangan.
                    if (pref != "auto" && pref != "off" && pick >= 0 && SubLang.key(subTracks[pick]) != pref)
                        vm.toast(I18n.t("sub_unavail", "%1\$s subtitle unavailable · using %2\$s").format(SubLang.prefName(pref), subName(pick)))
                    applySub(pick)
                } else if (subReady && subIdx >= 0 && !subLocalApplied && tracks.groups.any { isLocalSubtitleGroup(it) }) {
                    // debug.60: trek samping lokal tiba SESUDAH grup teks
                    // manifest (pilihan awal jatuh ke grup manifest yang
                    // tak bisa dirender) -> terapkan ulang pilihan yang
                    // sama ke trek lokal yang sehat.
                    applySub(subIdx)
                }
            }
            override fun onRenderedFirstFrame() {
                rendered = true
                // Gudang: pintu ini terbukti memutar — ingat sebagai
                // pintu awal kunjungan berikutnya.
                onDoorSuccess?.invoke()
            }
            override fun onVideoSizeChanged(size: androidx.media3.common.VideoSize) {
                if (size.width > 0 && size.height > 0) vidRatio = size.width * size.pixelWidthHeightRatio / size.height
                if (size.height > 0) playingH = size.height
            }
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                wantPlay = playWhenReady; vm.playing = playWhenReady
                // Dijeda: simpan posisi sekarang (di latar proses bisa dimatikan sistem sebelum polling 10 dtk berikutnya).
                if (!playWhenReady && PlayerHolder.isOwner(token) && player.playbackState != Player.STATE_IDLE && player.currentPosition > 0)
                    vm.saveProg(r.id, r.ep, (player.currentPosition / 1000).toInt(), durNow)
            }
            override fun onPlayerError(error: PlaybackException) {
                if (!PlayerHolder.isOwner(token)) return
                // Galat jaringan sesaat saat sudah memutar: coba lanjutkan di tempat (maks 3x) sebelum menjatuhkan ke jalur cadangan.
                // debug.64: dulu hanya CONNECTION_FAILED/TIMEOUT. Segmen yang gagal dengan 5xx/429, galat IO tak spesifik
                // (koneksi putus di tengah baca), dan TIMEOUT umum juga sesaat -> ikut dicoba ulang di tempat. 4xx lain
                // (403/404: token/segmen mati) tetap langsung ke rantai cadangan.
                val httpCode = (error.cause as? androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException)?.responseCode
                val transient = PlaybackChain.isTransient(error.errorCode, httpCode)
                if (transient && wasReady && netRetry < 3) {
                    netRetry++; buffering = true
                    com.onimeku.app.data.CrashLog.note("player", "${r.id} ep${r.ep} galat sesaat ${error.errorCodeName}${httpCode?.let { " http=$it" } ?: ""}, coba lagi #$netRetry")
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ if (!disposed) player.prepare() }, 1000L * netRetry)
                    return
                }
                vm.resumeMs = "${r.id}:${r.ep}" to player.currentPosition
                failed = true; buffering = false
                errMsg = error.errorCodeName
                com.onimeku.app.data.CrashLog.note("player", "${r.id} ep${r.ep} leg=${if (streamUrl.contains("/api/proxy")) "proxy" else "direct"} ${error.errorCodeName}: ${error.cause ?: error.message}")
                // Serahkan ke rantai cadangan di PlayerContent (URL
                // langsung / embed) — ia yang tahu lapisan berikutnya.
                onFatalError()
            }
        }
        player.addListener(l)
        vm.playing = true
        onDispose {
            disposed = true
            player.removeListener(l)
            // Pemutar dipakai bersama: bila episode/pintu berikutnya sudah mengambil alih, jangan menyimpan posisi
            // episode baru sebagai milik episode lama (progres lama sudah disimpan di goEp / ENDED).
            val mine = PlayerHolder.isOwner(token)
            if (mine) vm.saveProg(r.id, r.ep, (player.currentPosition / 1000).toInt(), durNow)
            PlayerHolder.release(token)
            if (mine) vm.playing = false
        }
    }

    // debug.64 — pengawas macet. Dulu buffering yang menggantung tanpa galat (koneksi setengah mati) hanya
    // berakhir setelah timeout baca OkHttp 25 dtk, dan sesudahnya tak ada yang menurunkan beban. Kini, bila video
    // sudah pernah tampil lalu menunggu data terus-menerus: 12 dtk -> minta ulang segmen di posisi yang sama
    // (seek ke posisi sekarang); 15 dtk berikutnya -> turunkan kualitas satu tingkat (sesi ini saja, pengaturan
    // tidak diubah) + siapkan ulang sumber; sekali lagi bila belum pulih. Maks 3 siklus per kemacetan.
    LaunchedEffect(player, buffering, rendered) {
        if (!buffering || !rendered) return@LaunchedEffect
        var tries = 0
        while (tries < 3) {
            delay(StallPolicy.delayMs(tries))
            if (player.playbackState != Player.STATE_BUFFERING || !player.playWhenReady) return@LaunchedEffect
            tries++
            val lower = heights.filter { it < playingH }.maxOrNull()
            if (StallPolicy.shouldLower(tries, lower)) {
                applyQuality("${lower}p")
                vm.toast(I18n.t("slow_switched", "Slow connection · switched to %1\$dp").format(lower))
            }
            com.onimeku.app.data.CrashLog.note("player", "${r.id} ep${r.ep} macet >${if (tries == 1) 12 else 12 + 15 * (tries - 1)}s, pulihkan #$tries (tinggi=$playingH, turun=${lower ?: 0})")
            player.seekTo(player.currentPosition)
            if (tries >= 2) player.prepare()
        }
    }

    // MediaSession + layanan latar kini dimiliki PlayerHolder (hidup selama pemutar dipakai, lintas episode).

    // Timer tidur.
    LaunchedEffect(sleepMin) {
        if (sleepMin > 0) {
            delay(sleepMin * 60_000L)
            player.pause(); vm.toast(I18n.t("sleep_done", "Sleep timer: paused")); sleepMin = 0
        }
    }

    // Polling posisi 1 dtk dan HANYA menulis state saat detiknya berubah
    // (sebelumnya tiap 500 ms -> seluruh PlayerReady recompose 2x/dtk).
    // Simpan progres tiap 10 dtk (tiap simpan = serialisasi seluruh AppState).
    val nextEp = remember(r, a) { EpisodeMath.next(a.available, a.eps, r.ep) }
    LaunchedEffect(player) {
        var n = 0
        var prefetched = false
        var trPre = false
        var introAutoDone = false
        while (true) {
            // Pemutar sudah diambil alih episode/pintu berikutnya: berhenti (jangan menyimpan posisi episode lain).
            if (!PlayerHolder.isOwner(token)) break
            val sec = (player.currentPosition / 1000).toInt()
            if (!dragging && sec != pos.toInt()) pos = sec.toFloat()
            // Lewati intro otomatis (opsional): sekali per episode, hanya bila API memberi penanda intro.
            if (!introAutoDone) {
                val tgt = SkipLogic.autoSkipTarget(sec, playback.introStart, playback.introEnd, vm.s.settings.skipIntro, vm.s.settings.autoSkipIntro)
                if (tgt != null) {
                    introAutoDone = true
                    player.seekTo(tgt * 1000L)
                    vm.toast(I18n.t("intro_skipped", "Intro skipped"))
                }
            }
            val bufSec = (player.bufferedPosition / 1000L).toFloat()
            if (bufSec != buffered) buffered = bufSec
            val d = player.duration
            if (d > 0) {
                val ds = (d / 1000).toInt()
                if (ds != dur) dur = ds
                // 60 dtk terakhir: ambil data putar episode berikutnya di latar,
                // supaya autoplay langsung mulai tanpa menunggu API.
                // Pemanasan data putar episode berikutnya dimulai lebih awal (2 menit setelah mulai).
                if (!trPre && nextEp != null && sec >= 120) {
                    trPre = true
                    launch { com.onimeku.app.data.ApiClient.prefetchPlayback(r.id, nextEp) }
                }
                if (!prefetched && nextEp != null && vm.s.settings.autoplay && ds - sec in 1..60) {
                    prefetched = true
                    launch { com.onimeku.app.data.ApiClient.prefetchPlayback(r.id, nextEp) }
                }
            }
            if (++n % 10 == 0 && wantPlay) vm.saveProg(r.id, r.ep, sec, durNow)
            delay(1000)
        }
    }

    // Ke background: pause, kecuali PiP atau "Background playback" aktif.
    // Saat berlanjut di latar, trek video dimatikan (hemat data + baterai).
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, player) {
        val o = LifecycleEventObserver { _, e ->
            fun videoOff(off: Boolean) {
                player.trackSelectionParameters = player.trackSelectionParameters.buildUpon().setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_VIDEO, off).build()
            }
            if (e == Lifecycle.Event.ON_STOP) {
                // Simpan posisi sebelum proses berpeluang dimatikan di latar (polling hanya menyimpan tiap 10 dtk).
                if (PlayerHolder.isOwner(token) && player.currentPosition > 0) vm.saveProg(r.id, r.ep, (player.currentPosition / 1000).toInt(), durNow)
                // ON_STOP saat masih di PiP = jendela PiP ditutup (X): berhenti seperti YouTube, jangan lanjut berbunyi di latar.
                // Mengetuk jendela PiP (membuka kembali) tidak memicu ON_STOP, jadi pemutaran lanjut normal.
                if (vm.inPip || act?.isInPictureInPictureMode == true) player.pause()
                else if (bg) videoOff(true) else player.pause()
            }
            if (e == Lifecycle.Event.ON_START) videoOff(audioOnlyNow)
        }
        owner.lifecycle.addObserver(o)
        onDispose { owner.lifecycle.removeObserver(o) }
    }

    // PiP: auto-enter saat video diputar (Android 12+), layar tetap menyala, kecerahan dipulihkan
    DisposableEffect(Unit) {
        act?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            vm.pipParams = null
            act?.setPictureInPictureParams(pipParams(ctx, false, false))
            act?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            act?.window?.let { w -> w.attributes = w.attributes.also { it.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE } }
        }
    }
    LaunchedEffect(player) { if (speed != 1f) player.setPlaybackSpeed(speed) }
    LaunchedEffect(hudTick) { if (hudTick > 0) { delay(800); hud = null; barKind = 0 } }

    // Fullscreen: kunci landscape + sembunyikan bilah sistem; dipulihkan saat keluar/pergi dari pemutar.
    LaunchedEffect(r) {
        if (!vm.s.settings.gestureHintSeen) { delay(25_000); vm.setSettings { it.copy(gestureHintSeen = true) } }
    }
    // Putar HP ke landscape = fullscreen otomatis; kembali ke portrait = keluar (hanya bila fullscreen berasal dari rotasi).
    LaunchedEffect(orient) {
        if (orient == Configuration.ORIENTATION_LANDSCAPE && !full) { fullByRot = true; full = true }
        else if (orient == Configuration.ORIENTATION_PORTRAIT && full && fullByRot) { full = false; fullByRot = false }
    }
    val prevFull = remember { java.util.concurrent.atomic.AtomicBoolean(full) }
    LaunchedEffect(full) {
        vm.playerFull = full
        val leaving = !full && prevFull.getAndSet(full)
        act?.requestedOrientation = when {
            full && !fullByRot -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            leaving && orient == Configuration.ORIENTATION_LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        if (leaving) android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ act?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }, 700)
        act?.window?.let { w ->
            val c = WindowCompat.getInsetsController(w, w.decorView)
            if (full) {
                c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                c.hide(WindowInsetsCompat.Type.systemBars())
            } else c.show(WindowInsetsCompat.Type.systemBars())
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            act?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            act?.window?.let { w -> WindowCompat.getInsetsController(w, w.decorView).show(WindowInsetsCompat.Type.systemBars()) }
        }
    }
    BackHandler(enabled = full) { full = false; fullByRot = false }
    BackHandler(enabled = epPanel) { epPanel = false }
    BackHandler(enabled = sheet != null) { if (sheet == "main") sheet = null else sheet = "main" }
    // Kontrol hilang sendiri 3,5 dtk setelah ketukan terakhir saat video berjalan.
    LaunchedEffect(showUi, uiTick, wantPlay, buffering) { if (showUi && wantPlay && !buffering) { delay(3500); showUi = false } }

    // Pindah episode: simpan posisi episode ini DULU (pemutar bersama langsung dipakai episode baru).
    fun goEp(n: Int) {
        vm.saveProg(r.id, r.ep, (player.currentPosition / 1000).toInt(), durNow)
        vm.replaceTop(Route.Player(r.id, n))
    }
    fun step(d: Int) {
        val n = EpisodeMath.step(a.available, a.eps, r.ep, d)
        if (n != null) goEp(n)
    }
    val hasPrev = (if (a.available.isNotEmpty()) a.available.firstOrNull() else 1)?.let { r.ep > it } ?: (r.ep > 1)
    val hasNext = (if (a.available.isNotEmpty()) a.available.lastOrNull() else a.eps)?.let { r.ep < it } ?: (r.ep < a.eps)

    // PiP ala YouTube: parameter diperbarui tiap rasio/status putar/posisi video berubah; auto-masuk PiP hanya saat video diputar.
    val autoPip = vm.s.settings.autoPip
    LaunchedEffect(wantPlay, vidRatio, pipRect, hasPrev, hasNext, autoPip) {
        // Auto-enter hanya saat video benar-benar diputar dan pengaturan Picture-in-picture aktif.
        val p = pipParams(ctx, wantPlay && autoPip, wantPlay, vidRatio, pipRect, hasPrev, hasNext)
        vm.pipParams = p
        act?.setPictureInPictureParams(p)
    }
    // Tombol di jendela PiP (putar/jeda, sebelumnya, berikutnya).
    val stepNow by rememberUpdatedState<(Int) -> Unit>({ d -> step(d) })
    // Tombol Next/Prev di notifikasi & layar kunci (sesi media dipegang PlayerHolder).
    LaunchedEffect(player, hasPrev, hasNext) {
        PlayerHolder.setNav(token, EpisodeNav(hasPrev, hasNext, { stepNow(-1) }, { stepNow(1) }))
    }
    DisposableEffect(player) {
        val rcv = object : android.content.BroadcastReceiver() {
            override fun onReceive(c: Context, i: android.content.Intent) {
                when (i.getStringExtra("op")) {
                    "toggle" -> if (player.playWhenReady) player.pause() else player.play()
                    "prev" -> stepNow(-1)
                    "next" -> stepNow(1)
                }
            }
        }
        androidx.core.content.ContextCompat.registerReceiver(ctx, rcv, android.content.IntentFilter(PIP_ACTION), androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { try { ctx.unregisterReceiver(rcv) } catch (_: Exception) {} }
    }
    // Masuk PiP: sembunyikan kontrol. PiP ditutup (X) saat aplikasi sudah berhenti: jeda, jangan terus berbunyi di latar.
    var wasPip by remember { mutableStateOf(false) }
    LaunchedEffect(vm.inPip) {
        if (vm.inPip) showUi = false
        if (wasPip && !vm.inPip && !owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) && !bg) player.pause()
        wasPip = vm.inPip
    }

    // Posisi/buffer dibaca lewat lambda DI composable anak: hanya anak itu yang recompose tiap detik, bukan seluruh PlayerReady.
    val posProvider: () -> Float = { pos }
    val bufferedProvider: () -> Float = { buffered }
    val subScale = subScaleOf(vm.s.settings.subSize)
    val marks = remember(playback) {
        buildList<Pair<Float, Float>> {
            val i1 = playback.introEnd; val i0 = playback.introStart ?: 0
            if (i1 != null && i1 > i0) add(i0.toFloat() to i1.toFloat())
            val o0 = playback.outroStart; val o1 = playback.outroEnd ?: 0
            if (o0 != null && o1 > o0) add(o0.toFloat() to o1.toFloat())
        }
    }
    val subBgOn = vm.s.settings.subBg
    val subColorInt = subColorOf(vm.s.settings.subColor)
    val subSys = vm.s.settings.subSystem
    if (vm.inPip) {
        Box(Modifier.fillMaxSize().background(Color.Black)) { VideoSurface(player, Modifier.fillMaxSize(), subScale, subBgOn, 0, subColorInt, subSys) }
        return
    }

    val epList = if (a.available.isNotEmpty()) a.available else (1..a.eps).toList()
    val curIdx = epList.indexOf(r.ep)
    val epListState = rememberLazyListState()
    // Komentar inline di panel bawah video (menggantikan daftar Episodes).
    val commentsOn = com.onimeku.app.data.Comments.enabled
    val commentsUi = rememberCommentsUi(r.id)
    LaunchedEffect(r.id) { if (commentsOn) vm.loadComments(r.id) }
    val thumbSrc = remember { MutableInteractionSource() }

    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.lerp(Bg, Color.Black, fullA))) {
      // Area video: penuh lebar, menempel di bawah status bar (tanpa bingkai), hitam pekat.
      Box(Modifier.fillMaxWidth().background(Color.Black).let { if (fullA < 1f) it.statusBarsPadding() else it }) {
        Box(
            Modifier.fillMaxWidth().layout { m, c ->
                // Tinggi: 16:9 saat portrait -> seluruh sisa layar saat fullscreen, diinterpolasi oleh fullA.
                val w = c.maxWidth
                // Tablet/jendela lebar non-fullscreen: tinggi video dibatasi 60% area agar panel episode & info
                // di bawahnya tetap terlihat (video tetap 16:9 di tengah, sisi kosong hitam).
                val hVideo = (w * 9f / 16f).coerceAtMost(if (c.hasBoundedHeight) c.maxHeight * 0.6f else Float.MAX_VALUE)
                val hFull = if (c.hasBoundedHeight) c.maxHeight.toFloat() else hVideo
                val h = (hVideo + (hFull - hVideo) * fullA).toInt().coerceAtLeast(0)
                val p = m.measure(androidx.compose.ui.unit.Constraints.fixed(w, h))
                layout(w, h) { p.place(0, 0) }
            }.background(Color.Black)
                .onGloballyPositioned { c ->
                    val bnd = c.boundsInWindow()
                    val nr = android.graphics.Rect(bnd.left.toInt(), bnd.top.toInt(), bnd.right.toInt(), bnd.bottom.toInt())
                    if (nr != pipRect) pipRect = nr
                }
                .pointerInput(player) {
                    detectTapGestures(
                        onTap = { o ->
                            if (epPanel) epPanel = false
                            else if (dtSide != 0 && !locked) doubleSeek(if (o.x < size.width / 2f) -1 else 1)
                            else { showUi = !showUi; uiTick++ }
                        },
                        onDoubleTap = { o -> doubleSeek(if (o.x < size.width / 2f) -1 else 1) },
                    )
                }
                .pointerInput(player) {
                    // Tahan layar = kecepatan 2x selama jari menempel (seperti YouTube).
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        var held = true
                        withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) { waitForUpOrCancellation(); held = false }
                        if (held && !locked && player.isPlaying) {
                            boost = true; player.setPlaybackSpeed(2f)
                            waitForUpOrCancellation()
                            boost = false; player.setPlaybackSpeed(speedNow)
                        }
                    }
                }
                .pointerInput(Unit) {
                    var left = true
                    var ok = true
                    val edge = 28.dp.toPx()
                    // Zona aman: abaikan sentuhan di tepi kiri/kanan (gestur kembali sistem) dan 12% atas/bawah (bilah kontrol, seek bar).
                    detectVerticalDragGestures(onDragStart = {
                        left = it.x < size.width / 2
                        ok = it.x > edge && it.x < size.width - edge && it.y > size.height * 0.12f && it.y < size.height * 0.88f
                    }) { change, dy ->
                        if (!ok) return@detectVerticalDragGestures
                        change.consume()
                        if (locked) return@detectVerticalDragGestures
                        val d = -dy / size.height * 1.3f
                        if (left) {
                            br = (br + d).coerceIn(0.2f, 1f)
                            act?.window?.let { w -> w.attributes = w.attributes.also { it.screenBrightness = br } }
                            barKind = 1; barLevel = br
                        } else {
                            val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                            val cur = audio.getStreamVolume(AudioManager.STREAM_MUSIC) / max.toFloat()
                            val v = (cur + d).coerceIn(0f, 1f)
                            audio.setStreamVolume(AudioManager.STREAM_MUSIC, (v * max).roundToInt(), 0)
                            barKind = 2; barLevel = v
                        }
                        hudTick++
                    }
                }
                // Geser horizontal = seek (satu lebar layar = 90 dtk); lepas jari untuk melompat.
                .pointerInput(player) {
                    var startPos = 0L; var acc = 0f; var ok = true; var target = 0L
                    val edge = 28.dp.toPx()
                    detectHorizontalDragGestures(
                        onDragStart = { o ->
                            ok = !locked && o.x > edge && o.x < size.width - edge && o.y > size.height * 0.12f && o.y < size.height * 0.88f
                            startPos = player.currentPosition; acc = 0f; target = startPos
                        },
                        onDragEnd = { if (ok && acc != 0f) player.seekTo(target) },
                        onDragCancel = { },
                    ) { change, dx ->
                        if (!ok) return@detectHorizontalDragGestures
                        change.consume()
                        acc += dx
                        val d = if (player.duration > 0) player.duration else Long.MAX_VALUE
                        target = (startPos + (acc / size.width * 90_000f).toLong()).coerceIn(0L, d)
                        val delta = (target - startPos) / 1000
                        hud = "${fmt((target / 1000).toInt())}  (${if (delta >= 0) "+" else "−"}${kotlin.math.abs(delta)}s)"; hudTick++
                    }
                }
                // Cubit dua jari: buka = Fill (zoom), tutup = Fit. Tidak mengonsumsi event, jadi gestur lain tetap jalan.
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        var startDist = 0f
                        do {
                            val ev = awaitPointerEvent()
                            val ps = ev.changes.filter { it.pressed }
                            if (ps.size >= 2) {
                                val dd = (ps[0].position - ps[1].position).getDistance()
                                if (startDist == 0f) startDist = dd
                                else {
                                    val z = dd / startDist
                                    if (z > 1.15f && !fillMode) { fillMode = true; hud = I18n.t("hud_fill", "Fill"); hudTick++ }
                                    else if (z < 0.87f && fillMode) { fillMode = false; hud = I18n.t("hud_fit", "Fit"); hudTick++ }
                                }
                            }
                        } while (ev.changes.any { it.pressed })
                    }
                },
        ) {
            // Kontrol juga tampil saat dijeda/buffering/galat (bukan hanya showUi); subtitle harus naik di semua keadaan itu.
            val ctlShown = !locked && (showUi || !wantPlay || failed || buffering)
            VideoSurface(player, Modifier.fillMaxSize(), subScale, subBgOn, if (fillMode) 4 else 0, subColorInt, subSys, subLift = ctlShown, liftDp = if (full) 96 else 80)
            // Sampul anime menutupi layar hitam sampai bingkai video pertama tampil.
            if (!rendered && !failed) {
                RealImage(a, Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Color(0xA6080910)))
            }
            SkipChips(
                posProvider = posProvider, dur = dur, playback = playback, enabled = vm.s.settings.skipIntro,
                onSeekSec = { sec -> player.seekTo(sec * 1000L) },
            )
            hud?.let {
                Text(it, Modifier.align(Alignment.Center).offset(y = (-58).dp).clip(RoundedCornerShape(14.dp)).background(Color(0xB3000000))
                    .padding(horizontal = Sp.S16, vertical = Sp.S8), fontSize = Ty.Body, fontWeight = FontWeight.SemiBold)
            }
            if (nextEp != null) UpNextCard(
                posProvider = posProvider, dur = dur, nextEp = nextEp, failed = failed,
                autoplay = vm.s.settings.autoplay, autoCancel = autoCancel,
                onCancel = { autoCancel = true }, onPlayNow = { goEp(nextEp) },
            )
            // Layar penuh landscape: kontrol dijauhkan dari notch/kamera & sudut membulat (edge-to-edge membuatnya tertutup).
            val cutPad = if (full) Modifier.windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)) else Modifier
            if (locked && showUi) {
                Box(Modifier.align(Alignment.CenterStart).then(cutPad).padding(start = Sp.S16).size(48.dp).clip(CircleShape).background(Color(0x99000000))
                    .clickable { locked = false; uiTick++ }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.LockOpen, "Unlock controls")
                }
            }
            if (dtSide != 0) {
                val left = dtSide < 0
                Box(Modifier.align(if (left) Alignment.CenterStart else Alignment.CenterEnd).fillMaxHeight().fillMaxWidth(0.38f)
                    .clip(if (left) RoundedCornerShape(topEndPercent = 50, bottomEndPercent = 50) else RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50))
                    .background(Color(0x4D000000)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(if (left) Icons.Filled.FastRewind else Icons.Filled.FastForward, null, Modifier.size(34.dp), tint = Color.White)
                        Text("$dtSecs ${I18n.t("seconds", "seconds")}", Modifier.padding(top = Sp.S4), fontSize = Ty.Small, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }
            if (boost) {
                Row(Modifier.align(Alignment.TopCenter).padding(top = Sp.S16).clip(CircleShape).background(Color(0x99000000)).padding(horizontal = Sp.S12, vertical = Sp.S8),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Sp.S8)) {
                    Text("2×", fontSize = Ty.Small, fontWeight = FontWeight.Bold, color = Color.White)
                    Icon(Icons.Filled.FastForward, null, Modifier.size(16.dp), tint = Color.White)
                }
            }
            if (barKind != 0) {
                val bl = barLevel.coerceIn(0f, 1f)
                Column(Modifier.align(if (barKind == 1) Alignment.CenterStart else Alignment.CenterEnd).then(cutPad).padding(horizontal = Sp.S24)
                    .clip(RoundedCornerShape(18.dp)).background(Color(0x99000000)).padding(vertical = Sp.S12, horizontal = Sp.S12),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Sp.S8)) {
                    Icon(
                        when { barKind == 1 -> Icons.Filled.BrightnessMedium; bl <= 0f -> Icons.AutoMirrored.Filled.VolumeOff; else -> Icons.AutoMirrored.Filled.VolumeUp },
                        null, Modifier.size(20.dp), tint = Color.White,
                    )
                    Box(Modifier.width(6.dp).height(120.dp).clip(CircleShape).background(Color(0x40FFFFFF)), contentAlignment = Alignment.BottomCenter) {
                        Box(Modifier.fillMaxWidth().fillMaxHeight(bl.coerceAtLeast(0.03f)).clip(CircleShape).background(Color.White))
                    }
                    Text("${(bl * 100).roundToInt()}", fontSize = Ty.Micro, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = resumeChip && !failed && rendered,
                modifier = Modifier.align(Alignment.TopCenter).then(cutPad).padding(top = 62.dp),
                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically { -it / 2 },
                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically { -it / 2 },
            ) {
                Row(Modifier.clip(CircleShape).background(Color(0xCC0A0B10)).border(1.dp, Line, CircleShape).padding(start = Sp.S16, end = Sp.S4),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Sp.S8)) {
                    Text("${I18n.t("resuming_from", "Resuming from")} ${fmt(resumeStart)}", fontSize = Ty.Small, color = Color.White)
                    Text(I18n.t("from_start", "Start over"), Modifier.clip(CircleShape).clickable { player.seekTo(0L); resumeChip = false }
                        .padding(horizontal = Sp.S12, vertical = Sp.S8), fontSize = Ty.Small, fontWeight = FontWeight.SemiBold, color = RedText)
                }
            }
            val uiVisible = ctlShown
            val uiA = if (uiVisible) 1f else 0f
            if (uiA > 0.01f) Box(Modifier.matchParentSize().graphicsLayer { alpha = uiA }) {
                Box(Modifier.matchParentSize().background(Brush.verticalGradient(0f to Color(0xB3080910), 0.32f to Color(0x33080910), 0.6f to Color(0x33080910), 1f to Color(0xD9080910))))

                // ---- Bar atas: kembali · judul · CC · roda gigi ----
                Row(Modifier.align(Alignment.TopStart).graphicsLayer { translationY = -(1f - uiA) * 28.dp.toPx() }.fillMaxWidth().then(cutPad).padding(start = Sp.S8, end = Sp.S8, top = if (full) Sp.S12 else Sp.S8), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).clip(CircleShape).clickable { vm.back() }, contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                    Column(Modifier.weight(1f).padding(start = Sp.S4)) {
                        Text(a.title, fontSize = Ty.Body, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        Text("Episode ${r.ep}", fontSize = Ty.Caption, color = Color(0xFFE6E8F0))
                    }
                    if (full) Box(Modifier.size(48.dp).clip(CircleShape).clickable { epPanel = !epPanel; uiTick++ }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.VideoLibrary, I18n.t("episodes", "Episodes"), Modifier.size(20.dp), tint = Color.White)
                    }
                    if (subTracks.isNotEmpty()) Box(Modifier.size(48.dp).clip(CircleShape).clickable {
                        // Tombol CC: hidupkan/matikan subtitle sekali ketuk (seperti YouTube).
                        if (subIdx >= 0) {
                            applySub(-1); vm.setSettings { it.copy(subLang = "off") }; vm.toast("${I18n.t("subtitles", "Subtitles")}: ${I18n.t("ctl_off", "Off")}")
                        } else {
                            val pf = vm.s.settings.subLang.let { if (it == "off") "auto" else it }
                            val n = SubLang.pick(subTracks, pf, vm.s.settings.lang); applySub(n)
                            vm.setSettings { it.copy(subLang = pf) }; vm.toast("${I18n.t("subtitles", "Subtitles")}: ${subName(n)}")
                        }
                        uiTick++
                    }, contentAlignment = Alignment.Center) {
                        Icon(if (subIdx >= 0) Icons.Filled.ClosedCaption else Icons.Filled.ClosedCaptionOff, "Subtitles", Modifier.size(20.dp), tint = Color.White)
                    }
                    // Ikon aksi bar atas (episode, CC, dubbing, roda gigi) seragam 20dp agar rapi; area ketuk tetap 48dp.
                    // Tombol dubbing langsung di bar kontrol (posisi paling umum, seperti "Audio & Subtitles" Netflix / Audio track YouTube).
                    // Hanya tampil bila ada lebih dari satu trek audio; ketuk membuka lembar "Dubbing".
                    if (audioLabels.size > 1) Box(Modifier.size(48.dp).clip(CircleShape).clickable(
                        role = androidx.compose.ui.semantics.Role.Button,
                        onClickLabel = I18n.t("dub_pick", "Choose dubbing"),
                    ) { sheet = "audio"; uiTick++ }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Audiotrack, "${I18n.t("dubbing", "Dubbing")}: ${audioLabels.getOrElse(audioIdx) { audioLabels[0] }}", Modifier.size(20.dp), tint = Color.White)
                    }
                    Box(Modifier.size(48.dp).clip(CircleShape).clickable { sheet = "main"; uiTick++ }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Settings, "Settings", Modifier.size(20.dp), tint = Color.White)
                    }
                }

                // ---- Tengah: -10 · putar/jeda (ikon putih polos) · +10 ----
                if (failed) {
                    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.ErrorOutline, null, Modifier.size(30.dp), tint = RedText)
                        Text(I18n.t("cant_play", "Can’t play this video"), Modifier.padding(top = Sp.S8), fontWeight = FontWeight.SemiBold)
                        Text(friendlyPlayError(errMsg), Modifier.padding(top = Sp.S4, start = Sp.S28, end = Sp.S28), fontSize = Ty.Caption, color = InkSoft,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 16.sp)
                        Spacer(Modifier.height(Sp.S12))
                        Row(horizontalArrangement = Arrangement.spacedBy(Sp.S8)) {
                            Chip(I18n.t("retry", "Retry"), icon = Icons.Filled.Refresh) { failed = false; player.prepare(); player.playWhenReady = true }
                            val lowerQ = lowerQuality(vm.s.settings.quality)
                            if (lowerQ != null) Chip(I18n.t("lower_quality", "Lower quality")) {
                                vm.setSettings { it.copy(quality = lowerQ) }; applyQuality(lowerQ)
                                failed = false; player.prepare(); player.playWhenReady = true
                                vm.toast("${I18n.t("quality", "Quality")}: $lowerQ")
                            }
                        }
                        Text(I18n.t("report_problem", "Report a problem"),
                            Modifier.padding(top = Sp.S8).clip(RoundedCornerShape(8.dp)).clickable {
                                com.onimeku.app.data.GudangStore.report(ctx, a.anilistId, r.id, r.ep, "Galat putar di pemutar: ${errMsg ?: "-"}")
                                vm.toast(I18n.t("report_sent", "Thanks — your report was sent"))
                            }.padding(horizontal = Sp.S12, vertical = Sp.S8),
                            fontSize = Ty.Caption, fontWeight = FontWeight.Medium, color = RedText)
                    }
                } else {
                    var rotB by remember { mutableStateOf(0f) }
                    var rotF by remember { mutableStateOf(0f) }
                    val rb = 0f
                    val rf = 0f
                    Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Sp.S16)) {
                        if (hasPrev) Box(Modifier.size(44.dp).clip(CircleShape).pressable { step(-1); uiTick++ }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.SkipPrevious, "Previous episode", Modifier.size(30.dp), tint = Color.White)
                        } else Spacer(Modifier.size(44.dp))
                        Box(Modifier.size(52.dp).clip(CircleShape).pressable { rotB -= 360f; seekBy(-10_000); uiTick++ }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Replay10, "Back 10 seconds", Modifier.size(34.dp).graphicsLayer { rotationZ = rb }, tint = Color.White)
                        }
                        Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                            if (buffering) {
                                CircularProgressIndicator(Modifier.size(52.dp), color = Color.White, trackColor = Color(0x33FFFFFF), strokeWidth = 3.5.dp)
                            } else {
                                Box(Modifier.fillMaxSize().clip(CircleShape).pressable { player.playWhenReady = !player.playWhenReady; uiTick++ }, contentAlignment = Alignment.Center) {
                                    MorphIcon(wantPlay, Icons.Filled.Pause, Icons.Filled.PlayArrow, if (wantPlay) "Pause" else "Play", 56.dp, Color.White)
                                }
                            }
                        }
                        Box(Modifier.size(52.dp).clip(CircleShape).pressable { rotF += 360f; seekBy(10_000); uiTick++ }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Forward10, "Forward 10 seconds", Modifier.size(34.dp).graphicsLayer { rotationZ = rf }, tint = Color.White)
                        }
                        if (hasNext) Box(Modifier.size(44.dp).clip(CircleShape).pressable { step(1); uiTick++ }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.SkipNext, "Next episode", Modifier.size(30.dp), tint = Color.White)
                        } else Spacer(Modifier.size(44.dp))
                    }
                }

                // ---- Bawah: waktu + layar penuh (kanan), bilah seek tipis dengan buffer ----
                Column(Modifier.align(Alignment.BottomCenter).graphicsLayer { translationY = (1f - uiA) * 28.dp.toPx() }.fillMaxWidth().then(cutPad).padding(start = if (full) Sp.S28 else Sp.S16, end = if (full) Sp.S28 else Sp.S16, bottom = if (full) Sp.S12 else 0.dp)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = Sp.S2), verticalAlignment = Alignment.CenterVertically) {
                        TimeLabel(posProvider, dur)
                        Spacer(Modifier.weight(1f))
                        Box(Modifier.size(48.dp).clip(CircleShape).clickable { fillMode = !fillMode; hud = if (fillMode) I18n.t("hud_fill", "Fill") else I18n.t("hud_fit", "Fit"); hudTick++; uiTick++ }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.AspectRatio, I18n.t("fit_fill", "Fit / fill"), tint = Color.White)
                        }
                        Box(Modifier.size(48.dp).clip(CircleShape).clickable { full = !full; fullByRot = false; uiTick++ }, contentAlignment = Alignment.Center) {
                            Icon(if (full) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen, if (full) "Exit fullscreen" else "Fullscreen", tint = Color.White)
                        }
                    }
                    SeekBarLive(
                        posProvider = posProvider, dur = dur.toFloat(), bufferedProvider = bufferedProvider, marks = marks, modifier = Modifier.padding(bottom = Sp.S4),
                        onPreview = { dragging = true; pos = it; uiTick++ },
                        onDone = { player.seekTo((it * 1000).toLong()); pos = it; dragging = false; uiTick++ },
                    )
                }
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = epPanel && full,
                modifier = Modifier.align(Alignment.CenterEnd),
                enter = androidx.compose.animation.slideInHorizontally { it } + androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.slideOutHorizontally { it } + androidx.compose.animation.fadeOut(),
            ) {
                val fls = rememberLazyListState(initialFirstVisibleItemIndex = maxOf(curIdx - 2, 0))
                Column(Modifier.fillMaxHeight().width(290.dp).background(Color(0xF20A0B10)).then(cutPad).padding(top = Sp.S12)) {
                    Row(Modifier.padding(horizontal = Sp.S16, vertical = Sp.S8), verticalAlignment = Alignment.CenterVertically) {
                        Text(I18n.t("episodes", "Episodes"), Modifier.weight(1f), fontSize = Ty.Subtitle, fontWeight = FontWeight.Bold)
                        Box(Modifier.size(48.dp).clip(CircleShape).clickable { epPanel = false }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Close, "Close", Modifier.size(22.dp), tint = Color.White)
                        }
                    }
                    LazyColumn(state = fls, contentPadding = PaddingValues(bottom = Sp.S16)) {
                        items(epList, key = { "fp$it" }) { n ->
                            val isCur = n == r.ep
                            val seenEp = "${r.id}:$n" in vm.p.seen
                            Row(Modifier.fillMaxWidth().clickable { epPanel = false; if (!isCur) goEp(n) }
                                .background(if (isCur) Color(0x33E5484D) else Color.Transparent).padding(horizontal = Sp.S16, vertical = Sp.S16),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Sp.S12)) {
                                Text("Episode $n", Modifier.weight(1f), fontSize = Ty.Body, fontWeight = if (isCur) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (isCur) RedText else Color.White)
                                if (isCur) Icon(Icons.Filled.GraphicEq, null, Modifier.size(18.dp), tint = RedText)
                                else if (seenEp) Icon(Icons.Filled.CheckCircle, null, Modifier.size(16.dp), tint = Color(0xFF3DDC97))
                            }
                        }
                    }
                }
            }
        }
      }

      if (!full || fullA < 0.999f) {
        // ===== Panel di bawah video: info, navigasi episode, kontrol, komentar =====
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().imePadding(), state = epListState,
            contentPadding = PaddingValues(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + Sp.S24),
        ) {
            item(key = "info") {
                // Baris badge dibuat setinggi 48dp (area ketuk chip dubbing) SELALU, supaya judul tidak melompat saat chip muncul
                // setelah trek audio termuat; padding atas/judul dikurangi agar jarak visualnya tetap seperti semula.
                Column(Modifier.padding(horizontal = Gutter).padding(top = Sp.S2)) {
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Sp.S8)) {
                        Tag("EPISODE ${r.ep}", Red, Color.White, small = true)
                        if (offline) Tag("OFFLINE", Color(0x33FFFFFF), Color.White, small = true)
                        // Pemilih dubbing: pil bahasa rata kanan sebaris dengan badge episode; satu ketuk langsung ganti dubbing.
                        if (audioLabels.isNotEmpty()) {
                            DubSwitcher(audioLabels, audioKeys, audioIdx, Modifier.weight(1f)) { n ->
                                applyAudio(n); vm.toast("${I18n.t("dubbing", "Dubbing")}: ${audioLabels.getOrElse(n) { "?" }}")
                            }
                        }
                    }
                    Text(a.title, fontSize = Ty.Head, fontWeight = FontWeight.Bold, lineHeight = 26.sp,
                        letterSpacing = (-0.3).sp, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    Text(listOfNotNull(a.year.takeIf { it > 0 }?.toString(), a.score?.takeIf { it > 0 }?.let { "★ " + String.format(java.util.Locale.US, "%.1f", it) },
                        a.genres.take(2).joinToString(", ").ifBlank { null }, statusText(a.status)).joinToString("  ·  "),
                        Modifier.padding(top = Sp.S4), fontSize = Ty.Small, color = InkSoft, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)

                    // Sinopsis tepat di bawah judul (seperti Netflix/Crunchyroll), 3 baris + Show more.
                    if (a.synopsis.isNotBlank()) {
                        var more by remember(r.id) { mutableStateOf(false) }
                        var clipped by remember(r.id) { mutableStateOf(false) }
                        Text(a.synopsis, Modifier.padding(top = Sp.S12).clickable { more = !more }, fontSize = Ty.Small, color = InkSoft, lineHeight = 19.sp,
                            maxLines = if (more) 24 else 3, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            onTextLayout = { if (!more) clipped = it.hasVisualOverflow })
                        if (clipped || more) Text(if (more) I18n.t("syn_less", "Show less") else I18n.t("syn_more", "Show more"),
                            Modifier.heightIn(min = 48.dp).clickable { more = !more }.wrapContentHeight(Alignment.CenterVertically),
                            fontSize = Ty.Small, fontWeight = FontWeight.SemiBold, color = RedText)
                    }

                    Row(Modifier.fillMaxWidth().padding(top = Sp.S16), horizontalArrangement = Arrangement.spacedBy(Sp.S12)) {
                        val onList = r.id in vm.p.myList
                        ActionTile(Icons.Filled.Add, if (onList) I18n.t("added", "Added") else I18n.t("my_list", "My List"),
                            Modifier.weight(1f), on = onList, iconOn = Icons.Filled.Check) { vm.toggleML(r.id) }
                        if (!offline) {
                            val dk = "${r.id}:${r.ep}"
                            val pct = vm.downloading[dk]
                            val saved = pct == null && vm.hasDl(r.id, r.ep)
                            ActionTile(if (saved) AppIcons.Check else AppIcons.Download,
                                when { pct != null -> I18n.t("dl_saving", "Saving %d%%").format(pct); saved -> I18n.t("dl_done", "Saved"); else -> I18n.t("download", "Download") },
                                Modifier.weight(1f), on = saved || pct != null) { vm.download(r.id, r.ep) }
                        }
                        ActionTile(AppIcons.Share, I18n.t("share", "Share"), Modifier.weight(1f)) {
                            val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, I18n.t("share_text", "Watch %s on Onimeku: %s").format(a.title, "onimeku://anime/${r.id}"))
                            }
                            ctx.startActivity(android.content.Intent.createChooser(send, I18n.t("share", "Share")).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                        }
                    }
                }
            }
            if (epList.size > 1) item(key = "eps") {
                val rowState = rememberLazyListState(initialFirstVisibleItemIndex = maxOf(curIdx - 2, 0))
                Column(Modifier.padding(top = Sp.S20)) {
                    Row(Modifier.padding(horizontal = Gutter), verticalAlignment = Alignment.CenterVertically) {
                        Text(I18n.t("episodes", "Episodes"), Modifier.weight(1f), fontSize = Ty.Subtitle, fontWeight = FontWeight.Bold)
                        Text("${curIdx + 1} / ${epList.size}", fontSize = Ty.Small, color = NavOff)
                    }
                    androidx.compose.foundation.lazy.LazyRow(state = rowState, contentPadding = PaddingValues(horizontal = Gutter),
                        horizontalArrangement = Arrangement.spacedBy(Sp.S8), modifier = Modifier.padding(top = Sp.S8)) {
                        items(epList, key = { "ep$it" }) { n ->
                            EpisodeChip(n, isCur = n == r.ep, seen = "${r.id}:$n" in vm.p.seen, saved = vm.hasDl(r.id, n),
                                inProgress = (vm.p.prog["${r.id}:$n"] ?: 0) > 30 && "${r.id}:$n" !in vm.p.seen) { if (n != r.ep) goEp(n) }
                        }
                    }
                    if (hasPrev || hasNext) Row(Modifier.padding(horizontal = Gutter).padding(top = Sp.S12), horizontalArrangement = Arrangement.spacedBy(Sp.S12)) {
                        if (hasPrev) NavEpCard(I18n.t("previous", "Previous"), epList.getOrNull(curIdx - 1) ?: (r.ep - 1), false, Modifier.weight(1f)) { step(-1) }
                        if (hasNext) NavEpCard(I18n.t("up_next", "Up next"), epList.getOrNull(curIdx + 1) ?: (r.ep + 1), true, Modifier.weight(1f)) { step(1) }
                    }
                }
            }
            item(key = "about") {
                Column(Modifier.padding(horizontal = Gutter)) {
                    if (!vm.s.settings.gestureHintSeen) Row(Modifier.padding(top = Sp.S16).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0x0FFFFFFF)).padding(horizontal = Sp.S12, vertical = Sp.S2),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Sp.S12)) {
                        Icon(Icons.Filled.TouchApp, null, Modifier.size(16.dp), tint = NavOff)
                        Text(I18n.t("swipe_hint", "Swipe left: brightness · right: volume · double-tap sides: ±10s · hold: 2× speed · swipe sideways: seek · pinch: fill"), Modifier.weight(1f), fontSize = Ty.Caption, color = InkSoft, lineHeight = 16.sp)
                        Box(Modifier.size(48.dp).clip(CircleShape).clickable { vm.setSettings { it.copy(gestureHintSeen = true) } }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Close, "Dismiss", Modifier.size(18.dp), tint = NavOff)
                        }
                    }
                }
            }
            if (commentsOn) commentsSection(vm, r.id, commentsUi)
        }
      }
    }
    if (commentsOn) CommentDeleteDialog(vm, r.id, commentsUi)
    PlayerSettingsSheet(
        page = sheet, full = full,
        quality = quality, autoRes = playingH,
        qualityOpts = listOf("Auto") + (if (heights.isNotEmpty()) heights.map { "${it}p" } else listOf("1080p", "720p", "480p")),
        onQuality = { q -> vm.setSettings { it.copy(quality = q) }; applyQuality(q); sheet = null; vm.toast("${I18n.t("quality", "Quality")}: $q") },
        subTracks = subTracks, subCurrent = subIdx, subPref = vm.s.settings.subLang, subLabel = subName(subIdx),
        onSubTrack = { i ->
            applySub(i); sheet = null
            SubLang.key(subTracks[i])?.let { c -> vm.setSettings { it.copy(subLang = c) } }
            vm.toast("${I18n.t("subtitles", "Subtitles")}: ${subName(i)}")
        },
        onSubOff = { applySub(-1); sheet = null; vm.setSettings { it.copy(subLang = "off") }; vm.toast("${I18n.t("subtitles", "Subtitles")}: ${I18n.t("ctl_off", "Off")}") },
        onSubAuto = {
            sheet = null; vm.setSettings { it.copy(subLang = "auto") }
            val n = SubLang.pick(subTracks, "auto", vm.s.settings.lang); applySub(n); vm.toast("${I18n.t("subtitles", "Subtitles")}: Auto · ${subName(n)}")
        },
        subSize = vm.s.settings.subSize, subBg = vm.s.settings.subBg,
        onSubSize = { v -> vm.setSettings { it.copy(subSize = v) } },
        onSubBg = { v -> vm.setSettings { it.copy(subBg = v) } },
        subColor = vm.s.settings.subColor, subSystem = vm.s.settings.subSystem,
        onSubColor = { v -> vm.setSettings { it.copy(subColor = v) } },
        onSubSystem = { v -> vm.setSettings { it.copy(subSystem = v) } },
        subOffsetMs = subOffsetNow,
        onSubAlign = { alignSubs() },
        onSubOffset = { v -> vm.setSettings { it.copy(subOffsetByTitle = SubSync.remember(it.subOffsetByTitle, r.id, v)) } },
        autoplay = vm.s.settings.autoplay, onAutoplay = { v -> vm.setSettings { it.copy(autoplay = v) }; vm.toast("${I18n.t("autoplay_next", "Autoplay next")}: ${if (v) "On" else "Off"}") },
        speed = speed, onSpeed = { v -> speed = v; vm.playerSpeed = v; player.setPlaybackSpeed(v); sheet = null; vm.toast("${I18n.t("speed", "Speed")} ${spdLabel(v)}×") },
        audioLabels = audioLabels, audioKeys = audioKeys, audioIdx = audioIdx, onAudio = { n -> applyAudio(n); sheet = null; vm.toast("${I18n.t("dubbing", "Dubbing")}: ${audioLabels.getOrElse(n) { "?" }}") },
        sleepMin = sleepMin, onSleep = { m -> sleepMin = m; sheet = null; vm.toast(when (m) { 0 -> "Sleep timer: Off"; -1 -> "Sleep timer: ${I18n.t("sleep_end", "End of episode")}"; else -> "Sleep timer: $m min" }) },
        onLock = { locked = true; showUi = true; sheet = null },
        onPip = {
            sheet = null
            try { act?.enterPictureInPictureMode(vm.pipParams ?: pipParams(ctx, false, wantPlay)) } catch (e: Exception) { vm.toast(I18n.t("pip_na", "Picture-in-Picture unavailable")) }
        },
        audioOnly = audioOnly,
        onAudioOnly = {
            audioOnly = !audioOnly; sheet = null
            player.trackSelectionParameters = player.trackSelectionParameters.buildUpon().setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_VIDEO, audioOnly).build()
            vm.toast(if (audioOnly) "Audio only: On" else "Audio only: Off")
        },
        onPage = { sheet = it },
        onReport = {
            sheet = null
            // debug.62: laporan lama hanya berisi kalimat baku —
            // catatan CrashLog (termasuk catatan "subs"/"playback")
            // tidak pernah ikut, jadi laporan penonton tidak bisa
            // dipakai mendiagnosis dari server. Kini ekor log ikut di
            // catatan (route memotong 500 karakter) dan log penuh
            // diunggah ke bak diagnostik yang sama seperti jalur fatal.
            val tail = try { com.onimeku.app.data.CrashLog.read().takeLast(400) } catch (e: Exception) { "" }
            com.onimeku.app.data.GudangStore.report(ctx, a.anilistId, r.id, r.ep, "Laporan penonton dari pemutar | $tail")
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch { com.onimeku.app.data.ApiClient.uploadDiag("report") }
            vm.toast(I18n.t("report_sent", "Thanks — your report was sent"))
        },
    )
    }
}

private class CtlTile(val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String, val value: String, val on: Boolean = false, val action: () -> Unit)

/** Ubin kontrol: ikon di atas, nama kecil, nilai saat ini tebal. */
@Composable
private fun ControlTile(t: CtlTile, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier.height(78.dp).pressable(t.action).clip(shape).background(Bg2)
            .border(1.dp, if (t.on) Red.copy(alpha = 0.7f) else Line, shape).padding(horizontal = Sp.S4),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Icon(t.icon, null, Modifier.size(22.dp), tint = if (t.on) RedText else Color.White)
        Text(t.value, Modifier.padding(top = Sp.S8), fontSize = Ty.Small, fontWeight = FontWeight.SemiBold,
            color = if (t.on) RedText else Color.White, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        Text(t.label, fontSize = Ty.Micro, color = NavOff, maxLines = 1)
    }
}

/** Keadaan memuat / galat: area video hitam dengan sampul redup, info + tombol coba lagi di bawah. */
@Composable
private fun PlayerStatus(vm: AppVM, a: com.onimeku.app.data.Anime, r: Route.Player, loading: Boolean, message: String? = null, onRetry: (() -> Unit)? = null) {
    Column(Modifier.fillMaxSize().background(Bg)) {
        Box(Modifier.fillMaxWidth().background(Color.Black).statusBarsPadding()) {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
                RealImage(a, Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Color(0xCC080910)))
                Box(Modifier.align(Alignment.TopStart).padding(Sp.S8)) { GlassButton(Icons.AutoMirrored.Filled.ArrowBack, "Back") { vm.back() } }
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (loading) {
                        StreamLoading()
                    } else {
                        Icon(Icons.Filled.ErrorOutline, null, Modifier.size(34.dp), tint = RedText)
                        Text(I18n.t("stream_unavail", "Stream unavailable"), Modifier.padding(top = Sp.S8), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        Column(Modifier.padding(horizontal = Gutter).padding(top = Sp.S16)) {
            Tag("EPISODE ${r.ep}", Red, Color.White, small = true)
            Text(a.title, Modifier.padding(top = Sp.S8), fontSize = Ty.Head, fontWeight = FontWeight.Bold, lineHeight = 26.sp, maxLines = 2)
            if (message != null) Text(message, Modifier.padding(top = Sp.S12), color = InkSoft, fontSize = Ty.Body, lineHeight = 20.sp)
            if (onRetry != null) Box(Modifier.padding(top = Sp.S16)) { Btn(I18n.t("try_again", "Try again"), BtnStyle.Play, Icons.Filled.Refresh, height = 48.dp, onClick = onRetry) }
        }
    }
}

// ------------------------------------------------------------------ Bilah seek ala YouTube
/** Bilah seek tipis: lintasan, segmen buffer, progres merah, bulatan penunjuk yang membesar saat digeser. Ketuk = lompat. */

/** Uji mandiri jalur stream: GET polos OkHttp (klien bersama
 * yang sama dengan pemutar) meniru langkah ExoPlayer: master -> varian ->
 * 128KB segmen pertama, mencatat status/waktu tiap langkah ke CrashLog.
 * Membedakan "lapisan OkHttp/jaringan perangkat gagal" dari "pemutar
 * yang tak melanjutkan" — server hanya melihat master yang tiba. */
private suspend fun diagStreamProbe(r: Route.Player, pb: com.onimeku.app.data.Playback) {
    val master = pb.streamUrl ?: return
    if (!master.contains("/api/proxy")) return
    suspend fun fetch(u: String, maxBytes: Int): Triple<Int, Int, String> =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val req = okhttp3.Request.Builder().url(u).build()
            com.onimeku.app.data.Net.client.newCall(req).execute().use { resp ->
                val buf = ByteArray(maxBytes)
                var n = 0
                resp.body?.byteStream()?.use { ins ->
                    while (n < maxBytes) {
                        val rd = ins.read(buf, n, maxBytes - n)
                        if (rd < 0) break
                        n += rd
                    }
                }
                Triple(resp.code, n, String(buf, 0, n, Charsets.UTF_8))
            }
        }
    try {
        val done = kotlinx.coroutines.withTimeoutOrNull(60_000) {
            val t1 = System.currentTimeMillis()
            val (c1, n1, masterText) = fetch(master, 8192)
            com.onimeku.app.data.CrashLog.note("diag", "master HTTP $c1 ${n1}B ${System.currentTimeMillis() - t1}ms")
            val variantRel = masterText.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() && !it.startsWith("#") } ?: return@withTimeoutOrNull false
            val variantUrl = java.net.URI(master).resolve(variantRel).toString()
            val t2 = System.currentTimeMillis()
            val (c2, n2, varText) = fetch(variantUrl, 32768)
            com.onimeku.app.data.CrashLog.note("diag", "varian HTTP $c2 ${n2}B ${System.currentTimeMillis() - t2}ms")
            val segRel = varText.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() && !it.startsWith("#") } ?: return@withTimeoutOrNull false
            val segUrl = java.net.URI(variantUrl).resolve(segRel).toString()
            val t3 = System.currentTimeMillis()
            val (c3, n3, _) = fetch(segUrl, 131072)
            com.onimeku.app.data.CrashLog.note("diag", "segmen1 HTTP $c3 ${n3}B ${System.currentTimeMillis() - t3}ms")
            true
        }
        if (done != true) com.onimeku.app.data.CrashLog.note("diag", "TIMEOUT/gagal di salah satu langkah (lihat urutan di atas)")
    } catch (e: Exception) {
        com.onimeku.app.data.CrashLog.note("diag", "GALAT ${e.javaClass.simpleName}: ${e.message}")
    }
    com.onimeku.app.data.ApiClient.uploadDiag("selftest")
}

/** Pesan galat putar yang bisa dipahami pengguna (bukan kode mentah ExoPlayer). */
private fun friendlyPlayError(code: String?): String {
    val c = code.orEmpty()
    return when {
        c.contains("NETWORK") || c.contains("TIMEOUT") -> I18n.t("err_net", "Connection problem. Check your internet and try again.")
        c.contains("DECOD") || c.contains("UNSUPPORTED") || c.contains("PARSING") -> I18n.t("err_dec", "This device couldn’t play this video format. Try a lower quality.")
        c.contains("HTTP") || c.contains("IO_") -> I18n.t("err_srv", "The video server is having trouble. Try again in a moment.")
        else -> I18n.t("err_gen", "Something went wrong while loading the video. Try again or lower the quality.")
    }
}

private fun lowerQuality(q: String): String? = when (q) {
    "Auto", "1080p" -> "720p"
    "720p" -> "480p"
    "480p" -> "360p"
    else -> null
}

private fun isMediaUrl(u: String): Boolean =
    u.contains(".m3u8") || u.contains(".mp4") || u.contains(".mpd")

/** Pemutar embed untuk judul yang sumbernya hanya menyediakan embed resmi. */
@Composable
private fun EmbedPlayer(vm: AppVM, r: Route.Player, a: com.onimeku.app.data.Anime, url: String) {
    val ctx = LocalContext.current
    val act = ctx.findActivity()
    DisposableEffect(Unit) {
        act?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { act?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
    val nums = if (a.available.isNotEmpty()) a.available else (1..a.eps).toList()
    val idx = nums.indexOf(r.ep)
    val commentsOn = com.onimeku.app.data.Comments.enabled
    val commentsUi = rememberCommentsUi(r.id)
    LaunchedEffect(r.id) { if (commentsOn) vm.loadComments(r.id) }
    if (commentsOn) CommentDeleteDialog(vm, r.id, commentsUi)
    Column(Modifier.fillMaxSize().background(Bg)) {
        Box(Modifier.fillMaxWidth().background(Color.Black).statusBarsPadding()) {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black)) {
                AndroidView(
                    factory = { c ->
                        val cm = android.webkit.CookieManager.getInstance()
                        cm.setAcceptCookie(true)
                        android.webkit.WebView(c).apply {
                            cm.setAcceptThirdPartyCookies(this, true)
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.mediaPlaybackRequiresUserGesture = false
                            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            settings.javaScriptCanOpenWindowsAutomatically = true
                            settings.userAgentString = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
                            webViewClient = android.webkit.WebViewClient()
                            webChromeClient = android.webkit.WebChromeClient()
                            loadUrl(url)
                        }
                    },
                    update = { wv -> if (wv.url != null && wv.url != url) wv.loadUrl(url) },
                    modifier = Modifier.fillMaxSize(),
                )
                Box(Modifier.align(Alignment.TopStart).padding(Sp.S8)) { GlassButton(Icons.AutoMirrored.Filled.ArrowBack, "Back") { vm.back() } }
            }
        }
        androidx.compose.foundation.lazy.LazyColumn(
            Modifier.weight(1f).fillMaxWidth().imePadding(),
            contentPadding = PaddingValues(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + Sp.S24),
        ) {
            item(key = "info") {
            Column(Modifier.padding(horizontal = Gutter).padding(top = Sp.S16)) {
                Tag("EPISODE ${r.ep}", Red, Color.White, small = true)
            Text(a.title, Modifier.padding(top = Sp.S8), fontSize = Ty.Head, fontWeight = FontWeight.Bold, lineHeight = 26.sp, maxLines = 2)
            if (idx > 0 || (idx >= 0 && idx < nums.size - 1)) Row(Modifier.fillMaxWidth().padding(top = Sp.S16), horizontalArrangement = Arrangement.spacedBy(Sp.S12)) {
                if (idx > 0) Btn(I18n.t("previous", "Previous"), BtnStyle.Solid, Icons.Filled.SkipPrevious, Modifier.weight(1f), height = 48.dp) { vm.replaceTop(Route.Player(r.id, nums[idx - 1])) }
                if (idx >= 0 && idx < nums.size - 1) Btn(I18n.t("next_episode", "Next episode"), BtnStyle.Play, Icons.Filled.SkipNext, Modifier.weight(1f), height = 48.dp) { vm.replaceTop(Route.Player(r.id, nums[idx + 1])) }
            }
            Text(
                "Judul ini diputar lewat pemutar resmi sumbernya. Ketuk tombol putar di dalam video.",
                Modifier.padding(top = Sp.S16), fontSize = Ty.Caption, color = InkSoft,
            )

                }
            }
            if (commentsOn) commentsSection(vm, r.id, commentsUi)
        }
    }
}


// ===== Komposabel anak yang membaca posisi sendiri (recompose per detik terbatas di sini) =====

/** Waktu "1:05 / 23:55". */
@Composable
private fun TimeLabel(posProvider: () -> Float, dur: Int) {
    Text(fmt(posProvider().toInt()), fontSize = Ty.Small, fontWeight = FontWeight.SemiBold, color = Color.White)
    Text(" / ${if (dur > 0) fmt(dur) else "--:--"}", fontSize = Ty.Small, color = Color(0xB3FFFFFF))
}

/** Bilah seek: posisi & buffer dibaca di sini, bukan di PlayerReady. */
@Composable
private fun SeekBarLive(
    posProvider: () -> Float, dur: Float, bufferedProvider: () -> Float, marks: List<Pair<Float, Float>>, modifier: Modifier,
    onPreview: (Float) -> Unit, onDone: (Float) -> Unit,
) {
    YtSeekBar(pos = posProvider(), dur = dur, buffered = bufferedProvider(), marks = marks, modifier = modifier, onPreview = onPreview, onDone = onDone)
}

/**
 * Tombol lewati intro/outro. Penanda dari API dipakai bila ada; bila tidak ada penanda intro, episode penuh
 * mendapat tombol cadangan "Lewati 85 dtk" pada awal tayangan.
 */
@Composable
private fun BoxScope.SkipChips(
    posProvider: () -> Float, dur: Int, playback: com.onimeku.app.data.Playback, enabled: Boolean, onSeekSec: (Int) -> Unit,
) {
    if (!enabled) return
    val pos = posProvider()
    val sec = pos.toInt()
    val introEnd = playback.introEnd
    val outroStart = playback.outroStart
    val hasIntro = introEnd != null && introEnd > 0
    val chip = Modifier.align(Alignment.BottomEnd).padding(end = Sp.S16, bottom = 68.dp).clip(CircleShape)
        .background(Color(0x99080910)).border(1.dp, Color(0xCCFFFFFF), CircleShape)
    if (introEnd != null && introEnd > 0 && pos >= (playback.introStart ?: 0) && pos < introEnd) {
        Text(I18n.t("skip_intro", "Skip intro"), chip.clickable { onSeekSec(introEnd) }
            .padding(horizontal = Sp.S16, vertical = 6.dp), fontSize = Ty.Small, fontWeight = FontWeight.Medium)
    }
    if (outroStart != null && outroStart > 0 && pos >= outroStart && (playback.outroEnd ?: dur) > pos) {
        Text(I18n.t("skip_outro", "Skip outro"), chip.clickable { onSeekSec(playback.outroEnd ?: dur) }
            .padding(horizontal = Sp.S16, vertical = 6.dp), fontSize = Ty.Small, fontWeight = FontWeight.Medium)
    }
    if (SkipLogic.fallbackVisible(sec, hasIntro, true, dur)) {
        Text(I18n.t("skip_fallback", "Skip %1\$ds").format(SkipLogic.FALLBACK_SEC), chip.clickable { onSeekSec(sec + SkipLogic.FALLBACK_SEC) }
            .padding(horizontal = Sp.S16, vertical = 6.dp), fontSize = Ty.Small, fontWeight = FontWeight.Medium)
    }
}

/** Kartu "Selanjutnya" 20 dtk terakhir: hitung mundur autoplay, Putar sekarang, Batal. */
@Composable
private fun BoxScope.UpNextCard(
    posProvider: () -> Float, dur: Int, nextEp: Int, failed: Boolean, autoplay: Boolean, autoCancel: Boolean,
    onCancel: () -> Unit, onPlayNow: () -> Unit,
) {
    val remain = dur - posProvider().toInt()
    if (!(dur > 60 && remain in 1..20 && !failed)) return
    Row(Modifier.align(Alignment.BottomStart).padding(start = Sp.S16, bottom = 66.dp).clip(RoundedCornerShape(14.dp))
        .background(Color(0xCC0A0B10)).border(1.dp, Line, RoundedCornerShape(14.dp)).padding(start = Sp.S16, end = Sp.S8, top = Sp.S8, bottom = Sp.S8),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Sp.S12)) {
        Column {
            Text(I18n.t("up_next", "Up next"), fontSize = Ty.Micro, color = InkSoft)
            Text(if (autoplay && !autoCancel) "Episode $nextEp · ${remain}${I18n.t("sec_short", "s")}" else "Episode $nextEp",
                fontSize = Ty.Small, fontWeight = FontWeight.SemiBold)
        }
        Chip(I18n.t("play_now", "Play now")) { onPlayNow() }
        if (autoplay && !autoCancel) Chip(I18n.t("cancel", "Cancel")) { onCancel() }
    }
}

/** Kotak nomor episode untuk deretan geser di bawah video: episode aktif merah, sudah ditonton redup + centang, tersimpan offline bertitik. */
@Composable
private fun EpisodeChip(n: Int, isCur: Boolean, seen: Boolean, saved: Boolean, inProgress: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier.size(width = 56.dp, height = 48.dp).clip(shape)
            .background(if (isCur) Red else Bg2).border(1.dp, if (isCur) Red else Line, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text("$n", fontSize = Ty.Body, fontWeight = if (isCur) FontWeight.Bold else FontWeight.Medium,
            color = if (isCur) Color.White else if (seen) NavOff else Color.White, maxLines = 1)
        if (seen && !isCur) Icon(Icons.Filled.CheckCircle, null, Modifier.align(Alignment.TopEnd).padding(Sp.S4).size(12.dp), tint = Color(0xFF3DDC97))
        if (inProgress && !isCur) Box(Modifier.align(Alignment.BottomCenter).padding(horizontal = Sp.S8).fillMaxWidth().height(3.dp).clip(CircleShape).background(Red))
        if (saved) Box(Modifier.align(Alignment.BottomCenter).padding(bottom = Sp.S4).size(5.dp).clip(CircleShape).background(if (isCur) Color.White else RedText))
    }
}

/** Kartu navigasi episode (Previous / Up next) di bawah deretan episode. */
@Composable
private fun NavEpCard(label: String, ep: Int, next: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(modifier.heightIn(min = 56.dp).clip(shape).background(Bg2).border(1.dp, if (next) Red else Line, shape)
        .clickable(onClick = onClick).padding(horizontal = Sp.S12, vertical = Sp.S8),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Sp.S8)) {
        if (!next) Icon(Icons.Filled.SkipPrevious, null, Modifier.size(22.dp), tint = Color.White)
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = Ty.Caption, color = if (next) RedText else InkSoft, maxLines = 1)
            Text("Episode $ep", fontSize = Ty.Body, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
        if (next) Icon(Icons.Filled.SkipNext, null, Modifier.size(22.dp), tint = Color.White)
    }
}
