package com.onimeku.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.request.ImageRequest
import com.onimeku.app.I18n
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

// ---------------------------------------------------------------------------
// Efek loading tingkat produksi: kilau tersinkron, skeleton per layar, gambar
// dengan reveal + fallback, bilah loading global, dan layar muat pemutar.
// ---------------------------------------------------------------------------

/**
 * Jam kilau BERSAMA. Dipasang pada layar skeleton (bukan seluruh aplikasi, supaya tidak
 * ada animasi tak berujung saat tak ada yang memuat). Semua `shimmer()` di dalamnya
 * membaca fase yang sama dan menggambar menurut koordinat jendela, jadi satu sapuan cahaya
 * melintasi SEMUA kotak sekaligus — bukan tiap kotak berkedip sendiri.
 */
val LocalShimmerPhase = compositionLocalOf<State<Float>?> { null }

@Composable
fun ProvideShimmer(content: @Composable () -> Unit) {
    val t = rememberInfiniteTransition(label = "shimmerClock")
    val p = t.animateFloat(0f, 1f, infiniteRepeatable(tween(1500, easing = LinearEasing)), label = "shimmerPhase")
    CompositionLocalProvider(LocalShimmerPhase provides p, content = content)
}

/** Jumlah gambar yang sedang menampilkan kilau. Jam kilau bersama hanya berdetak selama > 0. */
object ImageLoading {
    val count = androidx.compose.runtime.mutableIntStateOf(0)
}

/**
 * Satu jam kilau untuk SEMUA poster yang sedang memuat (dipasang sekali di akar aplikasi).
 * Dulu tiap poster tanpa skeleton menjalankan transisi tak berujung sendiri-sendiri (belasan sekaligus,
 * sapuan tidak selaras). Kini satu jam, tersinkron lintas poster, dan BERHENTI total saat tak ada gambar
 * yang memuat (tidak ada frame terjadwal percuma). Tidak ada state yang dibaca di komposisi: hanya fase gambar.
 */
@Composable
fun ProvideImageShimmer(content: @Composable () -> Unit) {
    val phase = remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        androidx.compose.runtime.snapshotFlow { ImageLoading.count.intValue > 0 }.collectLatest { on ->
            if (on) {
                while (true) {
                    androidx.compose.runtime.withFrameNanos { n -> phase.floatValue = ((n / 1_000_000L) % 1500L) / 1500f }
                }
            }
        }
    }
    CompositionLocalProvider(LocalShimmerPhase provides phase, content = content)
}

// ------------------------------------------------------------------ Skeleton per layar

/** Beberapa baris teks palsu (baris terakhir lebih pendek) untuk sinopsis/komentar yang masih memuat. */
@Composable
fun TextSkeleton(lines: Int = 3, modifier: Modifier = Modifier) {
    ProvideShimmer {
        Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            repeat(lines) { i ->
                SkeletonBox(Modifier.fillMaxWidth(if (i == lines - 1) 0.6f else 1f).height(13.dp), 6.dp)
            }
        }
    }
}

/** Kerangka komentar: avatar bulat + nama + dua baris isi. */
@Composable
fun CommentSkeleton(count: Int = 3, modifier: Modifier = Modifier) {
    ProvideShimmer {
        Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            repeat(count) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SkeletonBox(Modifier.size(36.dp), 18.dp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SkeletonBox(Modifier.width(110.dp).height(12.dp), 6.dp)
                        SkeletonBox(Modifier.fillMaxWidth().height(12.dp), 6.dp)
                        SkeletonBox(Modifier.fillMaxWidth(0.7f).height(12.dp), 6.dp)
                    }
                }
            }
        }
    }
}

/** Kerangka grid poster 3 kolom (Lihat semua / hasil cari saat katalog belum tiba). */
@Composable
fun PosterGridSkeleton(rows: Int = 4, modifier: Modifier = Modifier, hPad: androidx.compose.ui.unit.Dp = 16.dp) {
    ProvideShimmer {
        Column(modifier.fillMaxWidth().padding(horizontal = hPad), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(rows) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(3) {
                        Column(Modifier.weight(1f)) {
                            SkeletonBox(Modifier.fillMaxWidth().aspectRatio(232f / 311f), 12.dp)
                            Spacer(Modifier.height(8.dp))
                            SkeletonBox(Modifier.fillMaxWidth(0.9f).height(11.dp), 6.dp)
                            Spacer(Modifier.height(6.dp))
                            SkeletonBox(Modifier.fillMaxWidth(0.55f).height(10.dp), 6.dp)
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Gambar: shimmer -> reveal -> fallback

/**
 * Gambar poster/cover dengan siklus loading lengkap:
 *  1. kilau selama memuat,
 *  2. reveal (memudar + zoom-out halus 1,06x -> 1x) saat tiba dari jaringan; dari cache memori langsung tampil tanpa animasi,
 *  3. gagal -> coba ulang otomatis sekali setelah 1,6 dtk,
 *  4. tetap gagal / tak ada gambar -> kartu cadangan bergradasi dengan inisial judul (bukan kotak kosong).
 * [pending] = true bila URL gambar mungkin masih datang (mis. sedang dipulihkan PosterHealer) -> tampilkan kilau, bukan fallback.
 */
@Composable
fun LoadedImage(
    url: String?, title: String, modifier: Modifier = Modifier, pending: Boolean = false,
    alignment: Alignment = Alignment.Center,
) {
    val ctx = LocalContext.current
    var state by remember(url) { mutableIntStateOf(0) }   // 0 memuat, 1 siap, 2 gagal, 3 menunggu coba ulang
    var retry by remember(url) { mutableIntStateOf(0) }
    var instant by remember(url) { mutableStateOf(false) }
    val reveal by animateFloatAsState(if (state == 1) 1f else 0f, tween(if (instant) 0 else 480, easing = FastOutSlowInEasing), label = "imgReveal")
    LaunchedEffect(state) { if (state == 3) { delay(1600); retry++; state = 0 } }
    val req = remember(url, retry) { if (url.isNullOrBlank()) null else ImageRequest.Builder(ctx).data(url).build() }
    // Daftarkan ke jam kilau bersama selama kilau tampil; didaftarkan ulang bila status berubah, dilepas saat keluar layar.
    val shimmering = if (req == null) pending else (state != 1 && state != 2)
    androidx.compose.runtime.DisposableEffect(shimmering) {
        if (shimmering) ImageLoading.count.intValue++
        onDispose { if (shimmering) ImageLoading.count.intValue-- }
    }
    Box(modifier) {
        if (req == null) {
            if (pending) Box(Modifier.fillMaxSize().shimmer()) else ImageFallback(title)
        } else {
            if (state == 2) ImageFallback(title) else if (state != 1) Box(Modifier.fillMaxSize().shimmer())
            androidx.compose.runtime.key(retry) {
                coil.compose.AsyncImage(
                    model = req, contentDescription = title,
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        alpha = reveal
                        val s = 1.06f - 0.06f * reveal
                        scaleX = s; scaleY = s
                    },
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    alignment = alignment,
                    onSuccess = { s ->
                        instant = s.result.dataSource == coil.decode.DataSource.MEMORY_CACHE
                        state = 1
                    },
                    onError = { state = if (retry < 1) 3 else 2 },
                )
            }
        }
    }
}

/** Kartu cadangan saat gambar tak ada/gagal: gradasi gelap + inisial judul. */
@Composable
fun ImageFallback(title: String) {
    val ch = title.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "?"
    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF242838), Color(0xFF14161F)))),
        contentAlignment = Alignment.Center,
    ) {
        Text(ch, fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color(0x33FFFFFF))
    }
}

// ------------------------------------------------------------------ Bilah loading global

/** Garis tipis di puncak layar (di bawah status bar) selama sinkronisasi jaringan: cahaya merah menyapu terus-menerus. */
@Composable
fun TopLoadingBar(active: Boolean, modifier: Modifier = Modifier) {
    val a by animateFloatAsState(if (active) 1f else 0f, tween(260), label = "topBarA")
    if (a < 0.01f) return
    val t = rememberInfiniteTransition(label = "topBar")
    val x by t.animateFloat(-0.45f, 1f, infiniteRepeatable(tween(1150, easing = FastOutSlowInEasing)), label = "topBarX")
    Box(
        modifier.fillMaxWidth().height(3.dp).graphicsLayer { alpha = a }.drawBehind {
            val w = size.width
            drawRect(Color(0x1FE8566F))
            val bw = w * 0.45f
            val sx = x * w
            drawRoundRect(
                Brush.horizontalGradient(listOf(Color.Transparent, Red, RedText, Color.Transparent), startX = sx, endX = sx + bw),
                Offset(sx, 0f), Size(bw, size.height), CornerRadius(size.height, size.height),
            )
        },
    )
}

// ------------------------------------------------------------------ Layar muat pemutar

/** Muat stream: logo berdenyut di dalam cincin berputar + pesan tahapan yang bergantian. */
@Composable
fun StreamLoading() {
    val t = rememberInfiniteTransition(label = "streamLoad")
    val rot by t.animateFloat(0f, 360f, infiniteRepeatable(tween(1100, easing = LinearEasing)), label = "streamRot")
    val pulse by t.animateFloat(0.9f, 1.08f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "streamPulse")
    var stage by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(2400); stage = (stage + 1) % 3 } }
    val msgs = listOf(
        I18n.t("loading_stream", "Loading stream…"),
        I18n.t("loading_stream2", "Finding the best source…"),
        I18n.t("loading_stream3", "Almost ready…"),
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(68.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize().graphicsLayer { rotationZ = rot }) {
                drawArc(
                    Brush.sweepGradient(listOf(Color.Transparent, RedText)),
                    startAngle = 0f, sweepAngle = 280f, useCenter = false,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                )
            }
            Box(Modifier.graphicsLayer { scaleX = pulse; scaleY = pulse }) { Logo(30.dp) }
        }
        AnimatedContent(targetState = stage, transitionSpec = { fadeIn(tween(320)) togetherWith fadeOut(tween(200)) }, label = "streamStage") { s ->
            Text(msgs[s], Modifier.padding(top = 14.dp), fontSize = 13.sp, color = InkSoft)
        }
    }
}
