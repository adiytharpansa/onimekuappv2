package com.onimeku.app.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import kotlin.math.absoluteValue

private val Gradients = listOf(
    0xFF614385 to 0xFF516395, 0xFF134E5E to 0xFF71B280, 0xFF3A1C71 to 0xFFD76D77,
    0xFFFF512F to 0xFFDD2476, 0xFF1A2980 to 0xFF26D0CE, 0xFF0F2027 to 0xFF2C5364,
)

/** Efek shimmer saat poster sedang dimuat. */
@Composable
fun Shimmer(modifier: Modifier = Modifier) {
    val base = MaterialTheme.colorScheme.surfaceVariant
    val hi = MaterialTheme.colorScheme.outlineVariant
    val t = rememberInfiniteTransition(label = "shimmer")
    val x by t.animateFloat(
        initialValue = -400f, targetValue = 1200f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Restart), label = "x"
    )
    Box(modifier.background(Brush.linearGradient(listOf(base, hi, base), Offset(x, 0f), Offset(x + 400f, 300f))))
}

/** Pengganti poster bila URL kosong / gagal dimuat: gradien warna + inisial judul. */
@Composable
fun PosterFallback(title: String, modifier: Modifier = Modifier) {
    val (c1, c2) = Gradients[title.hashCode().absoluteValue % Gradients.size]
    val initials = title.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
    Box(modifier.background(Brush.linearGradient(listOf(Color(c1), Color(c2)))), contentAlignment = Alignment.Center) {
        Text(initials.ifBlank { "?" }, color = Color.White.copy(alpha = 0.9f), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
    }
}

/**
 * Poster anime siap pakai: shimmer saat memuat, crossfade saat tampil, fallback bila gagal.
 * Atur ukuran lewat [modifier], mis. Modifier.width(110.dp).aspectRatio(2f / 3f).
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PosterImage(
    url: String?, title: String, modifier: Modifier = Modifier, radius: Dp = 12.dp,
    scale: ContentScale = ContentScale.Crop, alignment: Alignment = Alignment.Center,
    sharedKey: String? = null,   // isi agar poster "terbang" mulus antar layar (shared element)
) {
    val ss = LocalSharedScope.current; val av = LocalAnimScope.current
    val shared = if (sharedKey != null && ss != null && av != null)
        with(ss) { Modifier.sharedElement(rememberSharedContentState(sharedKey), av) } else Modifier
    Box(modifier.then(shared).clip(RoundedCornerShape(radius))) {
        if (url.isNullOrBlank()) PosterFallback(title, Modifier.fillMaxSize())
        else SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(url).crossfade(300).build(),
            contentDescription = title, modifier = Modifier.fillMaxSize(), contentScale = scale, alignment = alignment,
            loading = { Shimmer(Modifier.fillMaxSize()) },
            error = { PosterFallback(title, Modifier.fillMaxSize()) },
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedScope = compositionLocalOf<SharedTransitionScope?> { null }
val LocalAnimScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/** Klik dengan efek mengecil halus saat ditekan (tanpa ripple). */
fun Modifier.pressClick(onClick: () -> Unit): Modifier = composed {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.95f else 1f, spring(dampingRatio = 0.6f, stiffness = 400f), label = "press")
    this.graphicsLayer { scaleX = s; scaleY = s }.clickable(interactionSource = src, indication = null, onClick = onClick)
}
