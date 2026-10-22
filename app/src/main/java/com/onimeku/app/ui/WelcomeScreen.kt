package com.onimeku.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import com.onimeku.app.data.Anime
import com.onimeku.app.data.Catalog
import com.onimeku.app.data.PosterHealer
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onimeku.app.AppVM
import com.onimeku.app.I18n
import kotlinx.coroutines.delay

private val slides = listOf(
    Triple("Welcome To\n", "Onimeku.", "Watch Anime Online, Streaming Online"),
    Triple("Thousands\nof ", "Episodes.", "Pick from a big library in HD, with new episodes every day."),
    Triple("Watch\n", "Anywhere.", "Download your favorite episodes and keep watching offline."),
)

@Composable
private fun SlideText(i: Int, modifier: Modifier = Modifier) {
    val (pl, e, sb) = slides[i]
    Column(modifier) {
        Text(buildAnnotatedString {
            append(pl); withStyle(SpanStyle(color = Red)) { append(e) }
        }, fontSize = 40.sp, fontWeight = FontWeight.Bold, lineHeight = 44.sp, letterSpacing = (-1.2).sp)
        Text(sb, Modifier.padding(top = Sp.S12), fontSize = Ty.Body, lineHeight = 22.sp, color = InkSoft)
    }
}

/**
 * Ubin bento ringan (tanpa animasi masuk/zoom tak berujung): hanya crossfade singkat saat poster
 * berganti. Poster ASLI dari katalog; kosong = shimmer.
 */
@Composable
private fun BentoTile(a: Anime?, order: Int, zoomMs: Int, score: Boolean, modifier: Modifier) {
    val shape = RoundedCornerShape(22.dp)
    Box(modifier
        .clip(shape).background(Bg2).border(1.dp, Color(0x26FFFFFF), shape)) {
        Crossfade(a, animationSpec = tween(300), label = "tile") { x ->
            val req = rememberImg(x?.let { PosterHealer.posterFor(it) })
            Box(Modifier.fillMaxSize()) {
                if (req == null) Box(Modifier.fillMaxSize().shimmer())
                else coil.compose.AsyncImage(req, x?.title, Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop, alignment = Alignment.TopCenter)
                val sc = x?.score?.takeIf { score && it > 0 }
                if (sc != null) {
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.6f to Color.Transparent, 1f to Color(0xB3080910))))
                    Row(Modifier.align(Alignment.BottomStart).padding(Sp.S12), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Sp.S4)) {
                        Icon(Icons.Filled.Star, null, Modifier.size(14.dp), tint = Gold)
                        Text(String.format(java.util.Locale.US, "%.1f", sc), fontSize = Ty.Small, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun WelcomeScreen(vm: AppVM) {
    var idx by remember { mutableIntStateOf(0) }
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(tick) { while (true) { delay(4500); idx = (idx + 1) % slides.size } }

    // Kolam poster ASLI teratas, satu per franchise (Season 1/2/Part N tidak ikut ganda) dan satu per gambar.
    // Dipotong ke kelipatan 6: ubin ke-k hanya memakai indeks k, k+6, k+12, ... sehingga 6 ubin yang
    // tampil bersamaan SELALU anime berbeda.
    val pool = remember(Catalog.all) {
        val seenF = HashSet<String>(); val seenP = HashSet<String>()
        val uniq = Catalog.popular.asSequence().mapNotNull { Catalog.byId[it] }
            .filter { a -> !PosterHealer.posterFor(a).isNullOrBlank() }
            .filter { a -> seenP.add(Catalog.posterFileKey(PosterHealer.posterFor(a)!!)) && seenF.add(Catalog.franchiseKey(a.title).ifEmpty { a.id }) }
            .take(24).toList()
        uniq.take((uniq.size / 6) * 6)
    }
    val swaps = remember { mutableStateListOf(0, 0, 0, 0, 0, 0) }
    LaunchedEffect(pool.size) {
        if (pool.size <= 6) return@LaunchedEffect
        var t = 0
        while (true) { delay(3500); swaps[t % 6] = swaps[t % 6] + 1; t++ }
    }
    fun at(k: Int): Anime? {
        if (pool.isEmpty()) return null
        if (pool.size < 6) return pool[k % pool.size]
        return pool[(k + 6 * swaps[k]) % pool.size]
    }

    Box(Modifier.fillMaxSize().background(Bg)) {
        // Cahaya merah lembut di belakang kisi.
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(RedDeep.copy(alpha = 0.55f), Color.Transparent), center = androidx.compose.ui.geometry.Offset(300f, 500f), radius = 1000f)))

        Column(Modifier.align(Alignment.TopCenter).widthIn(max = 560.dp).fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = Sp.S24, vertical = Sp.S16), verticalAlignment = Alignment.CenterVertically) {
                Logo(32.dp)
                Text("Onimeku", Modifier.padding(start = Sp.S12), fontSize = Ty.Subtitle, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp)
                Spacer(Modifier.weight(1f))
                Text(I18n.t("skip_for_now", "Skip for now"), Modifier.clip(CircleShape).background(Color(0x40FFFFFF).copy(alpha = 0.08f)).border(1.dp, Color(0x33FFFFFF), CircleShape)
                    .clickable { vm.closeWelcome() }.padding(horizontal = Sp.S12, vertical = Sp.S8),
                    fontSize = Ty.Caption, fontWeight = FontWeight.Medium, color = InkSoft)
            }

            // Kisi bento 3 kolom, tinggi mengisi sisa layar; ujung bawah memudar ke latar.
            Box(Modifier.weight(1f).fillMaxWidth()) {
                Row(Modifier.fillMaxSize().padding(horizontal = Sp.S24), horizontalArrangement = Arrangement.spacedBy(Sp.S8)) {
                    Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(Sp.S8)) {
                        BentoTile(at(0), 0, 7000, true, Modifier.weight(1.7f).fillMaxWidth())
                        BentoTile(at(1), 3, 9000, false, Modifier.weight(1f).fillMaxWidth())
                    }
                    Column(Modifier.weight(1f).fillMaxHeight().padding(top = Sp.S32), verticalArrangement = Arrangement.spacedBy(Sp.S8)) {
                        BentoTile(at(2), 1, 8000, false, Modifier.weight(1f).fillMaxWidth())
                        BentoTile(at(3), 4, 6500, true, Modifier.weight(1.7f).fillMaxWidth())
                    }
                    Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(Sp.S8)) {
                        BentoTile(at(4), 2, 7500, false, Modifier.weight(1.3f).fillMaxWidth())
                        BentoTile(at(5), 5, 8500, false, Modifier.weight(1.3f).fillMaxWidth())
                    }
                }
                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(96.dp).background(Brush.verticalGradient(listOf(Color.Transparent, Bg))))
            }

            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = Sp.S24).padding(bottom = Sp.S24)) {
                // Tinggi area teks = slide tertinggi (salinan tak terlihat) supaya tombol tidak bergeser.
                Box {
                    slides.indices.forEach { SlideText(it, Modifier.alpha(0f).clearAndSetSemantics { }) }
                    Crossfade(idx, animationSpec = tween(200), label = "welcomeText") { i -> SlideText(i) }
                }
                Row(Modifier.padding(top = Sp.S20), verticalAlignment = Alignment.CenterVertically) {
                    slides.indices.forEach { i ->
                        val dotW = if (i == idx) 28.dp else 8.dp
                        val dotC = if (i == idx) Red else Color(0xFF3A3E4E)
                        Box(Modifier.padding(end = Sp.S8).height(8.dp).width(dotW).clip(CircleShape)
                            .background(dotC).clickable { idx = i; tick++ })
                    }
                }
                Row(Modifier.padding(top = Sp.S24), horizontalArrangement = Arrangement.spacedBy(Sp.S12)) {
                    Btn("Get started", BtnStyle.Play, icon = Icons.AutoMirrored.Filled.ArrowForward, modifier = Modifier.weight(1f), height = 56.dp) { vm.closeWelcome(); vm.go(com.onimeku.app.Route.Auth(true)) }
                    Btn("Sign in", BtnStyle.Solid, height = 56.dp) { vm.closeWelcome(); vm.go(com.onimeku.app.Route.Auth(false)) }
                }
            }
        }
    }
}
