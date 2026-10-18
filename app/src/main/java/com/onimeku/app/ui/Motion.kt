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
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import coil.request.ImageRequest

// ---------------------------------------------------------------------------
// Motion & loading primitives dipakai bersama oleh semua layar.
// ---------------------------------------------------------------------------

private val ShimmerBase = Color(0xFF1E2130)
private val ShimmerHi = Color(0xFF2C3044)

/** Kilau bergerak (skeleton loading). Hanya fase gambar yang dihitung ulang tiap frame. */
@Composable
fun Modifier.shimmer(): Modifier {
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

@Composable
fun SkeletonBox(modifier: Modifier = Modifier, radius: Dp = 12.dp) {
    Box(modifier.clip(RoundedCornerShape(radius)).shimmer())
}

/** Klik dengan efek tekan (skala pegas) — terasa "hidup" tanpa ripple. */
@Composable
fun Modifier.pressable(onClick: () -> Unit): Modifier {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val reduceP = LocalReduceMotion.current
    val sc by animateFloatAsState(
        targetValue = if (pressed && !reduceP) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "press",
    )
    return this
        .graphicsLayer { scaleX = sc; scaleY = sc }
        .clickable(interactionSource = src, indication = null, onClick = onClick)
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
        else ImageRequest.Builder(ctx).data(url).crossfade(350).build()
    }
}

/** Kerangka Home saat katalog pertama kali dimuat. */
@Composable
fun HomeSkeleton(heroHeight: Dp, posterWidth: Dp, bottomPad: Dp) {
    Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = bottomPad)) {
        Box(Modifier.fillMaxWidth().height(heroHeight).shimmer())
        repeat(3) {
            Spacer(Modifier.height(26.dp))
            SkeletonBox(Modifier.padding(horizontal = 16.dp).width(170.dp).height(22.dp), 8.dp)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(3) {
                    Column(Modifier.width(posterWidth)) {
                        SkeletonBox(Modifier.fillMaxWidth().aspectRatio(232f / 311f), 18.dp)
                        Spacer(Modifier.height(8.dp))
                        SkeletonBox(Modifier.fillMaxWidth(0.8f).height(12.dp), 6.dp)
                    }
                }
            }
        }
    }
}

/** Baris kerangka (thumbnail + dua teks) untuk daftar yang masih memuat. */
@Composable
fun SkeletonRows(count: Int = 4) {
    repeat(count) {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SkeletonBox(Modifier.width(96.dp).aspectRatio(16f / 10f), 12.dp)
            Column(Modifier.weight(1f).padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
object SharedArt { var key: String? = null }

/** Elemen bersama: poster "terbang" ke hero Detail dan kembali. Tanpa efek bila kunci/scope tidak ada. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedArt(key: String?): Modifier {
    val ss = LocalSharedScope.current
    val av = LocalAnimScope.current
    if (key == null || ss == null || av == null) return this
    return with(ss) { this@sharedArt.sharedElement(rememberSharedContentState(key = key), animatedVisibilityScope = av) }
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
            if (reduce) EnterTransition.None togetherWith ExitTransition.None
            else (scaleIn(spring(0.45f, 450f), initialScale = 0.3f) + fadeIn(tween(120))) togetherWith
                (scaleOut(tween(100), targetScale = 0.6f) + fadeOut(tween(80)))
        },
        label = "morph",
    ) { st -> Icon(if (st) onIcon else offIcon, desc, Modifier.size(size), tint = tint) }
}
