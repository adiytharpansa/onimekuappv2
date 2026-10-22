package com.onimeku.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.request.ImageRequest
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------------------
// Motion & loading primitives dipakai bersama oleh semua layar.
// ---------------------------------------------------------------------------

private val ShimmerBase = Color(0xFF1E2130)
private val ShimmerHi = Color(0xFF2C3044)

/** Kilau bergerak (skeleton loading). Hanya fase gambar yang dihitung ulang tiap frame. */
@Composable
fun Modifier.shimmer(): Modifier {
    // Di dalam ProvideShimmer: satu jam bersama, sapuan menyatu lintas kotak. Di luar: jam sendiri (perilaku lama).
    val shared = LocalShimmerPhase.current
    if (shared != null) return this.syncedShimmer(shared)
    val t = rememberInfiniteTransition(label = "shimmer")
    val x by t.animateFloat(
        initialValue = -1f, targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing)),
        label = "shimmerX",
    )
    return this.drawBehind {
        val w = size.width
        drawRect(
            Brush.linearGradient(
                colors = listOf(ShimmerBase, ShimmerHi, ShimmerBase),
                start = Offset(x * w, 0f),
                end = Offset(x * w + w * 0.7f, size.height),
            ),
        )
    }
}

/** Kilau menurut koordinat JENDELA: kotak di mana pun melihat sapuan yang sama pada waktu yang sama. */
@Composable
private fun Modifier.syncedShimmer(phase: androidx.compose.runtime.State<Float>): Modifier {
    val screenW = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp * androidx.compose.ui.platform.LocalDensity.current.density
    val pos = remember { floatArrayOf(0f, 0f) }
    return this
        .onGloballyPositioned { c -> val o = c.positionInWindow(); pos[0] = o.x; pos[1] = o.y }
        .drawBehind {
            val band = screenW * 0.9f
            val lx = -band + phase.value * (screenW + band * 2f) - pos[0]
            drawRect(
                Brush.linearGradient(
                    colors = listOf(ShimmerBase, ShimmerHi, ShimmerBase),
                    start = Offset(lx, 0f), end = Offset(lx + band, band * 0.35f),
                ),
            )
        }
}

@Composable
fun SkeletonBox(modifier: Modifier = Modifier, radius: Dp = 12.dp) {
    Box(modifier.clip(RoundedCornerShape(radius)).shimmer())
}

/** Klik dengan efek tekan (skala pegas) — terasa "hidup" tanpa ripple. */
@Composable
fun Modifier.pressable(onClick: () -> Unit): Modifier {
    // Efek skala tekan sudah dimatikan (target selalu 1f). Dulu tiap item tetap menjalankan
    // collectIsPressedAsState + animateFloatAsState + graphicsLayer; kini hanya klik.
    val src = remember { MutableInteractionSource() }
    return this.clickable(interactionSource = src, indication = null, onClick = onClick)
}

/** Muncul pertama kali: memudar + naik halus, bisa diberi jeda untuk efek berurutan. */
@Composable
fun Modifier.fadeSlideIn(delayMs: Int = 0): Modifier {
    val reduce = LocalReduceMotion.current
    var shown by remember { mutableStateOf(reduce) }
    LaunchedEffect(Unit) { shown = true }
    val a by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(durationMillis = if (reduce) 0 else 450, delayMillis = if (reduce) 0 else delayMs),
        label = "enter",
    )
    return this.graphicsLayer { alpha = a; translationY = (1f - a) * 36f }
}

/** Permintaan gambar Coil dengan crossfade, supaya poster tidak "pop" saat selesai dimuat. */
@Composable
fun rememberImg(url: String?): ImageRequest? {
    val ctx = LocalContext.current
    return remember(url) {
        if (url.isNullOrBlank()) null
        else ImageRequest.Builder(ctx).data(url).crossfade(350).precision(coil.size.Precision.INEXACT).build()
    }
}

/** Kerangka Home saat katalog pertama kali dimuat. */
@Composable
fun HomeSkeleton(heroHeight: Dp, posterWidth: Dp, bottomPad: Dp) = ProvideShimmer { HomeSkeletonBody(heroHeight, posterWidth, bottomPad) }

@Composable
private fun HomeSkeletonBody(heroHeight: Dp, posterWidth: Dp, bottomPad: Dp) {
    Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = bottomPad)) {
        Box(Modifier.fillMaxWidth().height(heroHeight)) {
            Box(Modifier.fillMaxSize().shimmer())
            // Bayangan isi hero: lencana, judul, sinopsis, tombol Play, titik indikator.
            Column(Modifier.align(androidx.compose.ui.Alignment.BottomStart).padding(start = Sp.S20, end = Sp.S20, bottom = 44.dp),
                verticalArrangement = Arrangement.spacedBy(Sp.S12)) {
                val ink = Color(0x40000000)
                Box(Modifier.width(90.dp).height(22.dp).clip(RoundedCornerShape(11.dp)).background(ink))
                Box(Modifier.fillMaxWidth(0.75f).height(30.dp).clip(RoundedCornerShape(8.dp)).background(ink))
                Box(Modifier.fillMaxWidth(0.9f).height(13.dp).clip(RoundedCornerShape(6.dp)).background(ink))
                Row(Modifier.padding(top = Sp.S8), horizontalArrangement = Arrangement.spacedBy(Sp.S12)) {
                    Box(Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(18.dp)).background(ink))
                    Box(Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(ink))
                }
            }
        }
        repeat(3) {
            Spacer(Modifier.height(Sp.S24))
            SkeletonBox(Modifier.padding(horizontal = Sp.S16).width(170.dp).height(22.dp), 8.dp)
            Spacer(Modifier.height(Sp.S16))
            Row(Modifier.padding(horizontal = Sp.S16), horizontalArrangement = Arrangement.spacedBy(Sp.S12)) {
                repeat(3) {
                    Column(Modifier.width(posterWidth)) {
                        SkeletonBox(Modifier.fillMaxWidth().aspectRatio(232f / 311f), 18.dp)
                        Spacer(Modifier.height(Sp.S8))
                        SkeletonBox(Modifier.fillMaxWidth(0.8f).height(12.dp), 6.dp)
                    }
                }
            }
        }
    }
}

/** Baris kerangka (thumbnail + dua teks) untuk daftar yang masih memuat. */
@Composable
fun SkeletonRows(count: Int = 4) = ProvideShimmer { SkeletonRowsBody(count) }

@Composable
private fun SkeletonRowsBody(count: Int) {
    repeat(count) {
        Row(Modifier.fillMaxWidth().padding(vertical = Sp.S12), horizontalArrangement = Arrangement.spacedBy(Sp.S12)) {
            SkeletonBox(Modifier.width(96.dp).aspectRatio(16f / 10f), 12.dp)
            Column(Modifier.weight(1f).padding(top = Sp.S4), verticalArrangement = Arrangement.spacedBy(Sp.S8)) {
                SkeletonBox(Modifier.fillMaxWidth(0.85f).height(14.dp), 6.dp)
                SkeletonBox(Modifier.fillMaxWidth(0.5f).height(11.dp), 6.dp)
            }
        }
    }
}


// ====================================================================== GERAK LANJUTAN
/** True bila pengguna mematikan animasi di Pengaturan Android (Opsi pengembang / Aksesibilitas). */
val LocalReduceMotion = androidx.compose.runtime.staticCompositionLocalOf { false }

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedScope = androidx.compose.runtime.staticCompositionLocalOf<SharedTransitionScope?> { null }
val LocalAnimScope = androidx.compose.runtime.staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

/** Kunci elemen bersama terakhir yang diketuk (poster -> hero Detail). */
object SharedArt {
    private var k: String? = null
    private var at = 0L
    var key: String?
        get() = k
        set(v) { k = v; at = android.os.SystemClock.uptimeMillis() }

    /**
     * Buang kunci yang sudah usang. Klik dari sumber tanpa poster-bertanda (daftar, notifikasi, pencarian, menu cepat)
     * tidak boleh memakai kunci klik lama: elemen bersama bisa "terbang" dari poster yang salah.
     */
    fun expireStale(maxAgeMs: Long = 800L) {
        if (k != null && android.os.SystemClock.uptimeMillis() - at > maxAgeMs) k = null
    }
}

/** Elemen bersama: poster "terbang" ke hero Detail dan kembali. Tanpa efek bila kunci/scope tidak ada. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedArt(key: String?): Modifier {
    // Dimatikan: poster tidak lagi "terbang" ke Detail saat diklik.
    @Suppress("UNUSED_VARIABLE") val off = true
    if (off) return this
    val ss = LocalSharedScope.current
    val av = LocalAnimScope.current
    if (key == null || ss == null || av == null) return this
    return with(ss) { this@sharedArt.sharedElement(rememberSharedContentState(key = key), animatedVisibilityScope = av) }
}

/**
 * Titik asal transisi pemutar: posisi sentuhan terakhir (pecahan lebar/tinggi layar) sebelum pemutar dibuka.
 * Diisi oleh pointerInput di akar App (tanpa menelan sentuhan). Pemutar tumbuh dari titik ini dan kembali ke sana.
 */
object PlayerOrigin {
    var origin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.4f)
    fun set(x: Float, y: Float) { origin = androidx.compose.ui.graphics.TransformOrigin(x.coerceIn(0f, 1f), y.coerceIn(0f, 1f)) }
}

/** Animasi item daftar (LazyRow/LazyColumn): memudar saat masuk/keluar, item lain menutup celah dengan pegas. Mati bila reduce-motion. */
@Composable
fun androidx.compose.foundation.lazy.LazyItemScope.motionItem(): Modifier {
    if (LocalReduceMotion.current) return Modifier.animateItem(fadeInSpec = null, placementSpec = null, fadeOutSpec = null)
    return Modifier.animateItem(
        fadeInSpec = tween(260), fadeOutSpec = tween(180),
        placementSpec = spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow, visibilityThreshold = IntOffset.VisibilityThreshold),
    )
}

/** Sama seperti [motionItem], untuk LazyVerticalGrid. */
@Composable
fun androidx.compose.foundation.lazy.grid.LazyGridItemScope.motionGridItem(): Modifier {
    if (LocalReduceMotion.current) return Modifier.animateItem(fadeInSpec = null, placementSpec = null, fadeOutSpec = null)
    return Modifier.animateItem(
        fadeInSpec = tween(260), fadeOutSpec = tween(180),
        placementSpec = spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow, visibilityThreshold = IntOffset.VisibilityThreshold),
    )
}

object Entered { val keys = HashSet<String>() }

/** Muncul berurutan (stagger) hanya sekali per item selama aplikasi hidup; tidak mengulang saat digulir kembali. */
@Composable
fun Modifier.staggerIn(key: String, index: Int): Modifier {
    val reduce = LocalReduceMotion.current
    val already = remember(key) { reduce || key in Entered.keys }
    var shown by remember(key) { mutableStateOf(already) }
    LaunchedEffect(key) { Entered.keys.add(key); shown = true }
    val a by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(durationMillis = if (already) 0 else 380, delayMillis = if (already) 0 else minOf(index, 8) * 45),
        label = "stagger",
    )
    return this.graphicsLayer { alpha = a; translationY = (1f - a) * 28f; val s = 0.94f + 0.06f * a; scaleX = s; scaleY = s }
}

/** Ikon yang berubah bentuk dengan pantulan pegas (mis. + menjadi centang). */
@Composable
fun MorphIcon(on: Boolean, onIcon: ImageVector, offIcon: ImageVector, desc: String, size: Dp, tint: Color) {
    val reduce = LocalReduceMotion.current
    AnimatedContent(
        targetState = on,
        transitionSpec = {
            EnterTransition.None togetherWith ExitTransition.None
        },
        label = "morph",
    ) { st -> Icon(if (st) onIcon else offIcon, desc, Modifier.size(size), tint = tint) }
}


// ====================================================================== GERAK BERANDA (v2)
/** Zoom sangat pelan bolak-balik pada gambar hero (efek sinematik "Ken Burns"). */
@Composable
fun Modifier.kenBurns(): Modifier {
    if (LocalReduceMotion.current) return this
    val t = rememberInfiniteTransition(label = "kenBurns")
    val s by t.animateFloat(
        initialValue = 1f, targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(9000, easing = androidx.compose.animation.core.FastOutSlowInEasing), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "kenBurnsScale",
    )
    return this.graphicsLayer { scaleX = s; scaleY = s }
}

/** Kilau cahaya miring yang menyapu tombol utama tiap beberapa detik. */
@Composable
fun Modifier.sweepShine(): Modifier {
    if (LocalReduceMotion.current) return this
    val t = rememberInfiniteTransition(label = "shine")
    val x by t.animateFloat(
        initialValue = -0.6f, targetValue = 1.8f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing)),
        label = "shineX",
    )
    return this.drawWithContent {
        drawContent()
        val w = size.width
        val band = w * 0.28f
        val x0 = x * w
        drawRect(
            Brush.linearGradient(
                colors = listOf(Color.Transparent, Color(0x40FFFFFF), Color.Transparent),
                start = Offset(x0, 0f), end = Offset(x0 + band, size.height * 0.6f),
            ),
        )
    }
}

/** Panah kecil yang bergoyang ke kanan, mengajak mengetuk. */
@Composable
fun Modifier.nudgeX(): Modifier {
    if (LocalReduceMotion.current) return this
    val t = rememberInfiniteTransition(label = "nudge")
    val v by t.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(750, easing = androidx.compose.animation.core.FastOutSlowInEasing), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "nudgeV",
    )
    return this.graphicsLayer { translationX = v * 5.dp.toPx() }
}

/** Titik status "Airing": inti hijau + gelombang yang memudar keluar. */
@Composable
fun PulseDot(color: Color) {
    val reduce = LocalReduceMotion.current
    val t = rememberInfiniteTransition(label = "pulse")
    val p by t.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing)),
        label = "pulseP",
    )
    Box(Modifier.size(6.dp)) {
        if (!reduce) Box(
            Modifier.fillMaxSize().graphicsLayer { val s = 1f + 1.8f * p; scaleX = s; scaleY = s; alpha = 0.55f * (1f - p) }
                .clip(androidx.compose.foundation.shape.CircleShape).background(color),
        )
        Box(Modifier.fillMaxSize().clip(androidx.compose.foundation.shape.CircleShape).background(color))
    }
}

/** Titik indikator hero: yang aktif memanjang dan terisi pelan sesuai waktu sebelum slide berganti (5,5 dtk). */
@Composable
fun HeroDot(selected: Boolean, page: Int) {
    val reduce = LocalReduceMotion.current
    val w by animateDpAsState(if (selected) 28.dp else 6.dp, spring(dampingRatio = 0.7f, stiffness = 500f), label = "dotW")
    val prog = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(selected, page) {
        if (selected && !reduce) { prog.snapTo(0f); prog.animateTo(1f, tween(5500, easing = LinearEasing)) }
        else prog.snapTo(if (selected) 1f else 0f)
    }
    Box(
        Modifier.height(6.dp).width(w).clip(RoundedCornerShape(50)).background(Color(0x59FFFFFF))
            .drawBehind { drawRect(RedText, size = androidx.compose.ui.geometry.Size(size.width * prog.value, size.height)) },
    )
}

/** Sekali per tampilan: bilah merah kecil di samping judul bagian tumbuh dari nol. */
@Composable
fun Modifier.growIn(delayMs: Int = 0): Modifier {
    val reduce = LocalReduceMotion.current
    var shown by remember { mutableStateOf(reduce) }
    LaunchedEffect(Unit) { shown = true }
    val a by animateFloatAsState(if (shown) 1f else 0f, tween(if (reduce) 0 else 520, delayMillis = if (reduce) 0 else delayMs, easing = androidx.compose.animation.core.FastOutSlowInEasing), label = "grow")
    return this.graphicsLayer { scaleX = a; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f) }
}

/**
 * Pita genre bergulir otomatis (marquee). Dua baris berlawanan arah; ketuk pita untuk membuka genre.
 * Berhenti otomatis bila pengguna mematikan animasi sistem.
 */
@Composable
fun GenreMarquee(genres: List<String>, onClick: (String) -> Unit) {
    if (genres.isEmpty()) return
    val half = genres.size / 2
    val rows = listOf(genres.take(maxOf(half, 1)) to 1, genres.drop(maxOf(half, 1)).ifEmpty { genres.reversed() } to -1)
    Column(verticalArrangement = Arrangement.spacedBy(Sp.S12)) {
        rows.forEachIndexed { r, (list, dir) -> MarqueeRow(list, dir, 26f + r * 8f, onClick) }
    }
}

@Composable
private fun MarqueeRow(list: List<String>, dir: Int, dpPerSec: Float, onClick: (String) -> Unit) {
    val reduce = LocalReduceMotion.current
    val n = list.size
    val start = Int.MAX_VALUE / 2 - (Int.MAX_VALUE / 2) % n
    val state = androidx.compose.foundation.lazy.rememberLazyListState(initialFirstVisibleItemIndex = start)
    val px = with(androidx.compose.ui.platform.LocalDensity.current) { dpPerSec.dp.toPx() }
    LaunchedEffect(n, reduce) {
        if (reduce) return@LaunchedEffect
        var last = 0L
        while (true) {
            androidx.compose.runtime.withFrameNanos { now ->
                if (last != 0L) state.dispatchRawDelta(dir * px * (now - last) / 1_000_000_000f)
                last = now
            }
        }
    }
    androidx.compose.foundation.lazy.LazyRow(
        state = state, userScrollEnabled = false,
        horizontalArrangement = Arrangement.spacedBy(Sp.S12),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(Int.MAX_VALUE) { i ->
            val g = list[i % n]
            Row(
                Modifier.clip(RoundedCornerShape(50)).background(Bg2).border(1.dp, Line, RoundedCornerShape(50))
                    .pressable { onClick(g) }.padding(horizontal = Sp.S16, vertical = Sp.S12),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Sp.S8),
            ) {
                Box(Modifier.size(6.dp).clip(androidx.compose.foundation.shape.CircleShape).background(RedText))
                androidx.compose.material3.Text(g, fontSize = Ty.Body, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium, color = Color.White, maxLines = 1)
            }
        }
    }
}


// ====================================================================== GERAK LANJUTAN (v3)
/** Muncul dengan pantulan pegas (skala 0,3 -> 1) setelah jeda; dipakai angka ranking Top Hits. */
@Composable
fun Modifier.popIn(delayMs: Int = 0): Modifier {
    val reduce = LocalReduceMotion.current
    var shown by remember { mutableStateOf(reduce) }
    LaunchedEffect(Unit) { shown = true }
    val sc by animateFloatAsState(
        targetValue = if (shown) 1f else 0.3f,
        animationSpec = if (reduce) tween(0) else spring(dampingRatio = 0.45f, stiffness = 260f),
        label = "popScale",
    )
    val al by animateFloatAsState(if (shown) 1f else 0f, tween(if (reduce) 0 else 220, delayMillis = if (reduce) 0 else delayMs), label = "popAlpha")
    return this.graphicsLayer {
        scaleX = sc; scaleY = sc; alpha = al
        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 1f)
    }
}

/** Klik dengan efek "terangkat": mengecil + sedikit miring ke belakang saat ditekan, lalu memantul kembali. */
@Composable
fun Modifier.liftPress(onClick: () -> Unit): Modifier {
    // Umpan balik tekan RINGAN: mengecil 3,5% saat disentuh -> klik terasa langsung merespons.
    // Nilai animasi dibaca di blok graphicsLayer (fase draw), jadi hanya berekomposisi saat tekan/lepas, bukan tiap frame.
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s = animateFloatAsState(if (pressed) 0.965f else 1f, tween(if (pressed) 70 else 140), label = "pressScale")
    return this
        .graphicsLayer { scaleX = s.value; scaleY = s.value }
        .clickable(interactionSource = src, indication = null, onClick = onClick)
}

/** Percikan partikel kecil sekali saat [trigger] berubah dari false ke true (mis. judul masuk My List). */
@Composable
fun Modifier.sparkBurst(trigger: Boolean): Modifier {
    val reduce = LocalReduceMotion.current
    val anim = remember { androidx.compose.animation.core.Animatable(1f) }
    var prev by remember { mutableStateOf(trigger) }
    LaunchedEffect(trigger) {
        if (false) { anim.snapTo(0f); anim.animateTo(1f, tween(650, easing = androidx.compose.animation.core.FastOutSlowInEasing)) }
        prev = trigger
    }
    return this.drawWithContent {
        drawContent()
        val p = anim.value
        if (p < 1f) {
            val c = Offset(size.width / 2f, size.height * 0.38f)
            val maxD = 34.dp.toPx()
            for (i in 0 until 10) {
                val ang = Math.toRadians(i * 36.0 + 12.0)
                val d = 10.dp.toPx() + (maxD - 10.dp.toPx()) * p * (if (i % 2 == 0) 1f else 0.7f)
                drawCircle(
                    color = (if (i % 3 == 0) Gold else RedText).copy(alpha = (1f - p).coerceIn(0f, 1f)),
                    radius = (if (i % 2 == 0) 3.2.dp else 2.dp).toPx() * (1f - 0.6f * p),
                    center = Offset(c.x + (kotlin.math.cos(ang) * d).toFloat(), c.y + (kotlin.math.sin(ang) * d).toFloat()),
                )
            }
        }
    }
}

/** Bilah progres yang terisi dengan animasi dari 0 ke [value] saat pertama tampil. */
@Composable
fun AnimatedProgressBar(value: Float, modifier: Modifier = Modifier, color: Color = Red, track: Color = Color(0x40FFFFFF)) {
    val reduce = LocalReduceMotion.current
    val a = remember { androidx.compose.animation.core.Animatable(if (reduce) value else 0f) }
    LaunchedEffect(value) { a.animateTo(value.coerceIn(0f, 1f), tween(if (reduce) 0 else 900, delayMillis = if (reduce) 0 else 250, easing = androidx.compose.animation.core.FastOutSlowInEasing)) }
    Box(modifier.background(track).drawBehind { drawRect(color, size = androidx.compose.ui.geometry.Size(size.width * a.value, size.height)) })
}


/** Seperti [liftPress] tetapi dengan tekan-lama ([onLong] null = perilaku biasa). */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun Modifier.liftPressLong(onLong: (() -> Unit)?, onClick: () -> Unit): Modifier {
    if (onLong == null) return liftPress(onClick)
    val src = remember { MutableInteractionSource() }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val pressed by src.collectIsPressedAsState()
    val s = animateFloatAsState(if (pressed) 0.965f else 1f, tween(if (pressed) 70 else 140), label = "pressScaleLong")
    return this
        .graphicsLayer { scaleX = s.value; scaleY = s.value }
        .combinedClickable(
            interactionSource = src, indication = null,
            onLongClick = { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress); onLong() },
            onClick = onClick,
        )
}

/** Seperti [pressable] tetapi dengan tekan-lama ([onLong] null = perilaku biasa). */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun Modifier.pressableLong(onLong: (() -> Unit)?, onClick: () -> Unit): Modifier {
    if (onLong == null) return pressable(onClick)
    val src = remember { MutableInteractionSource() }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    return this
        .combinedClickable(
            interactionSource = src, indication = null,
            onLongClick = { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress); onLong() },
            onClick = onClick,
        )
}


/** Tanpa efek ripple/sorotan saat ditekan (dipakai global lewat LocalIndication). */
object NoIndication : androidx.compose.foundation.IndicationNodeFactory {
    override fun create(interactionSource: androidx.compose.foundation.interaction.InteractionSource): androidx.compose.ui.node.DelegatableNode =
        object : androidx.compose.ui.Modifier.Node() {}
    override fun hashCode(): Int = -1
    override fun equals(other: Any?): Boolean = other === this
}
