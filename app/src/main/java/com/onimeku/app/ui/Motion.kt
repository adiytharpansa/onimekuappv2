package com.onimeku.app.ui

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
    val sc by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
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
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val a by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(durationMillis = 450, delayMillis = delayMs),
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
