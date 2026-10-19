package com.onimeku.app.ui

import androidx.compose.foundation.lazy.grid.itemsIndexed
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onimeku.app.AppVM
import com.onimeku.app.I18n
import com.onimeku.app.Route
import com.onimeku.app.data.Anime

val LocalEnsureNotif = staticCompositionLocalOf<((Boolean) -> Unit) -> Unit> { { cb -> cb(false) } }

fun Context.findActivity(): Activity? {
    var c = this
    while (c is ContextWrapper) { if (c is Activity) return c; c = c.baseContext }
    return null
}

fun fmt(t: Int): String {
    val s = t.coerceAtLeast(0)
    val h = s / 3600; val m = s % 3600 / 60; val sec = s % 60
    val ss = sec.toString().padStart(2, '0')
    return if (h > 0) "$h:${m.toString().padStart(2, '0')}:$ss" else "$m:$ss"
}

@Composable
fun Logo(size: Dp) {
    androidx.compose.foundation.Canvas(Modifier.size(size)) {
        val k = this.size.width / 60f
        rotate(45f) {
            drawRoundRect(LogoRed, Offset(6 * k, 6 * k), Size(48 * k, 48 * k), CornerRadius(16 * k))
        }
        drawCircle(Color.White, radius = 10 * k, style = Stroke(6 * k))
    }
}

@Composable
fun Tag(text: String, bg: Color, fg: Color, modifier: Modifier = Modifier, small: Boolean = false) {
    Text(text, color = fg, fontSize = if (small) 11.sp else 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false,
        modifier = modifier.clip(RoundedCornerShape(if (small) 6.dp else 7.dp)).background(bg)
            .padding(horizontal = if (small) 7.dp else 9.dp, vertical = if (small) 2.dp else 3.dp))
}

@Composable
fun Chip(text: String, on: Boolean = false, minW: Dp = 0.dp, icon: ImageVector? = null, onClick: () -> Unit) {
    val fg = if (on) RedText else Color.White
    Row(
        Modifier.height(34.dp).widthIn(min = minW).clip(CircleShape).background(Bg2)
            .border(1.dp, if (on) Red else Line, CircleShape).clickable(onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        if (icon != null) Icon(icon, null, Modifier.size(16.dp), tint = fg)
        Text(text, color = fg, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
fun IconChip(icon: ImageVector, desc: String, on: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier.size(width = 40.dp, height = 36.dp).clip(CircleShape).background(Bg2)
            .border(1.dp, if (on) Red else Line, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, Modifier.size(18.dp), tint = if (on) RedText else Color.White) }
}

enum class BtnStyle { Play, Solid, Outline, OutlineOn }

@Composable
fun Btn(text: String, style: BtnStyle, icon: ImageVector? = null, modifier: Modifier = Modifier, height: Dp = 48.dp, onClick: () -> Unit) {
    val (bg, fg, bd) = when (style) {
        BtnStyle.Play -> Triple(Red, Color.White, Red)
        BtnStyle.Solid -> Triple(Solid, Color.White, Solid)
        // Outline: kaca gelap tipis + garis halus (dulu cincin putih tebal 3dp yang terasa kasar).
        BtnStyle.Outline -> Triple(Color(0x40000000), Color.White, Color(0xB3FFFFFF))
        BtnStyle.OutlineOn -> Triple(Color.White, Bg, Color.White)
    }
    Row(
        modifier.pressable(onClick).height(height).clip(CircleShape).background(bg).border(1.5.dp, bd, CircleShape)
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        if (icon != null) Icon(icon, null, Modifier.size(20.dp), tint = fg)
        Text(text, color = fg, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/**
 * Latar polos gelap untuk area gambar. Dulu ini gradasi buatan ala poster
 * contoh — dibuang: aplikasi hanya menampilkan gambar ASLI anime. Selama
 * poster asli belum termuat, area ini kosong polos, bukan gambar palsu.
 */
fun Modifier.artBackground(a: Anime): Modifier = this.background(Bg2)

/**
 * Gambar ASLI anime untuk thumbnail/header lebar (cover, jatuh ke poster).
 * Tidak ada gambar buatan: sebelum gambar termuat, area tetap polos gelap.
 */
@Composable
fun RealImage(a: Anime, modifier: Modifier = Modifier) {
    val img = com.onimeku.app.data.PosterHealer.coverFor(a) ?: com.onimeku.app.data.PosterHealer.posterFor(a)
    val req = rememberImg(img)
    val done = remember(a.id, img) { mutableStateOf(false) }
    Box(modifier) {
        // Kilau selama gambar dimuat; crossfade saat tiba.
        if (!done.value && (req != null || !a.poster.isNullOrBlank() || !a.banner.isNullOrBlank())) Box(Modifier.fillMaxSize().shimmer())
        if (req != null) {
            coil.compose.AsyncImage(
                model = req, contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                onSuccess = { done.value = true },
                onError = { done.value = true },
            )
        }
    }
}

/** Teks status yang mengikuti bahasa aktif (nilai mentah "Airing"/"Completed" tetap dipakai untuk filter). */
fun statusText(status: String): String = I18n.t("st_" + status.lowercase(), status)

val railW: Dp @Composable get() = (LocalConfiguration.current.screenWidthDp * 0.4f).coerceIn(142f, 230f).dp

@Composable
fun Poster(
    a: Anime, w: Dp?, modifier: Modifier = Modifier, ep: Boolean = false, badges: Boolean = false,
    label: Boolean = false, rank: Int? = null, meta: Pair<String, String>? = null,
    progress: Float? = null, compact: Boolean = false, sharedKey: String? = null, playIcon: Boolean = false, newBadge: Boolean = false, onClick: () -> Unit,
) {
    val sized = if (w != null) modifier.width(w) else modifier.fillMaxWidth()
    val quick = LocalQuickMenu.current
    val longCb: (() -> Unit)? = if (!ep && quick != null) ({ quick(a) }) else null
    Box(
        sized.liftPressLong(longCb) { if (sharedKey != null) SharedArt.key = sharedKey; onClick() }.aspectRatio(if (ep) 232f / 150f else 232f / 311f)
            .sharedArt(sharedKey)
            .clip(RoundedCornerShape(if (compact) 12.dp else 18.dp)).artBackground(a),
    ) {
        val img = if (ep) {
            com.onimeku.app.data.PosterHealer.coverFor(a) ?: com.onimeku.app.data.PosterHealer.posterFor(a)
        } else {
            com.onimeku.app.data.PosterHealer.posterFor(a)
        }
        // Kilau selama poster dimuat; hilang saat gambar tiba atau gagal. Crossfade 350 ms saat tampil.
        val imgDone = remember(a.id, img) { mutableStateOf(false) }
        val req = rememberImg(img)
        if (!imgDone.value && (req != null || !a.poster.isNullOrBlank())) {
            Box(Modifier.fillMaxSize().shimmer())
        }
        if (req != null) {
            coil.compose.AsyncImage(
                model = req, contentDescription = a.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                onSuccess = { imgDone.value = true },
                onError = { imgDone.value = true },
            )
        }
        if (badges) {
            a.rating?.let { Tag(it, Color.White, Bg, Modifier.align(Alignment.TopStart).padding(if (compact) 6.dp else 12.dp), small = compact) }
            if (a.hd || newBadge) Column(
                Modifier.align(Alignment.TopEnd).padding(if (compact) 6.dp else 10.dp),
                horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (newBadge) Tag(I18n.t("badge_new", "NEW"), Gold, Bg, small = true)
                if (a.hd) Tag("HD", Red, Color.White, small = compact)
            }
        }
        if (rank != null) {
            val big = 60.sp
            Box(Modifier.align(Alignment.BottomStart).padding(start = 10.dp, bottom = 2.dp).popIn(120 + rank * 70)) {
                Text("$rank", fontSize = big, fontWeight = FontWeight.Bold, color = Bg,
                    style = TextStyle(drawStyle = Stroke(width = 14f)), lineHeight = big)
                Text("$rank", fontSize = big, fontWeight = FontWeight.Bold, color = Color.White, lineHeight = big)
            }
        }
        if (meta != null) {
            Column(
                Modifier.align(Alignment.BottomStart).fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xD90A0B10))))
                    .padding(start = 12.dp, end = 12.dp, top = 26.dp, bottom = 10.dp),
            ) {
                Text(meta.first, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(meta.second, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (playIcon) {
            // Penanda "lanjutkan": lingkaran kaca gelap + ikon play di tengah kartu Continue Watching.
            Box(
                Modifier.align(Alignment.Center).size(40.dp).clip(CircleShape).background(Color(0x8C080910))
                    .border(1.dp, Color(0x59FFFFFF), CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.PlayArrow, null, Modifier.size(24.dp), tint = Color.White) }
        }
        if (progress != null) {
            AnimatedProgressBar(progress, Modifier.align(Alignment.BottomStart).fillMaxWidth().height(5.dp))
        }
    }
}

/** Poster + judul di bawahnya (dua baris tetap, agar baris rail sejajar rapi). */
@Composable
fun PosterCard(a: Anime, w: Dp, rank: Int? = null, sharedKey: String? = null, index: Int = 0, onClick: () -> Unit) {
    Column(Modifier.width(w).let { m -> if (sharedKey != null) m.staggerIn(sharedKey, index) else m }) {
        Poster(a, w, badges = true, rank = rank, sharedKey = sharedKey, onClick = onClick)
        Text(a.title, Modifier.padding(top = 8.dp), fontSize = 13.sp, fontWeight = FontWeight.Medium, lineHeight = 17.sp,
            minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** Kartu untuk grid (3 kolom di ponsel): poster ringkas + judul + genre. */
@Composable
fun Tile(a: Anime, vm: AppVM, index: Int = 0) {
    Column(Modifier.staggerIn("grid:${a.id}", index)) {
        Poster(a, null, badges = true, compact = true, sharedKey = "grid:${a.id}") { vm.go(Route.Detail(a.id)) }
        Text(a.title, Modifier.padding(top = 6.dp), fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 15.sp,
            minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
        // Baris genre selalu memesan tinggi (spasi bila genre kosong) supaya kartu di satu baris grid sama tinggi.
        Text(a.genres.take(2).joinToString(", ").ifBlank { " " }, fontSize = 11.sp, color = NavOff, modifier = Modifier.padding(top = 2.dp),
            minLines = 1, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Jarak konten ke bawah supaya tidak tertutup tab bar + navigation bar sistem. */
val TabBarHeight: Dp = 68.dp

@Composable
fun bottomContentPadding(): Dp =
    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + TabBarHeight + 16.dp

/**
 * Grid anime berbasis LazyVerticalGrid: hanya sel yang terlihat yang digambar,
 * jadi "See all", Search, dan My List tetap ringan walau katalog ribuan judul.
 * [header] tampil satu baris penuh di atas grid.
 */
@Composable
fun AnimeGrid(items: List<Anime>, vm: AppVM, header: @Composable () -> Unit) {
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    LazyVerticalGrid(
        columns = GridCells.Adaptive(100.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = top + 16.dp, bottom = bottomContentPadding()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) { Column { header() } }
        itemsIndexed(items, key = { _, it -> it.id }) { i, a -> Box(Modifier.animateItem()) { Tile(a, vm, i % 9) } }
    }
}

@Composable
fun Rail(content: LazyListScope.() -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 16.dp), content = content)
}

@Composable
fun SectionHead(title: String, subtitle: String? = null, onAll: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.padding(end = 10.dp).width(4.dp).height(if (subtitle != null) 38.dp else 20.dp).growIn().clip(CircleShape).background(Red))
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.2).sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, Modifier.padding(top = 2.dp), fontSize = 14.sp, color = InkSoft,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (onAll != null) Row(
            Modifier.clip(CircleShape).clickable(onClick = onAll).padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(I18n.t("see_all", "See all"), color = RedText, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Icon(Icons.Filled.ChevronRight, null, Modifier.size(18.dp), tint = RedText)
        }
    }
}

@Composable
fun Page(extraTop: Dp = 16.dp, content: @Composable ColumnScope.() -> Unit) {
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = top + extraTop, bottom = bottomContentPadding()),
        content = content,
    )
}

@Composable
fun SubHead(title: String, vm: AppVM) {
    Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(CircleShape).clickable { vm.back() }, contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
        }
        Spacer(Modifier.width(6.dp))
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun RootTitle(title: String, lead: String? = null) {
    Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
    if (lead != null) Text(lead, color = InkSoft, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
}

@Composable
fun EmptyBox(title: String, text: String, action: (@Composable () -> Unit)? = null) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        Modifier.padding(top = 28.dp).fillMaxWidth().clip(shape).background(Bg2).border(1.dp, Line, shape).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.padding(bottom = 14.dp).size(56.dp).clip(CircleShape).background(Color(0x26A6122D)), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Inbox, null, Modifier.size(28.dp), tint = RedText)
        }
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Text(text, color = InkSoft, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp, bottom = 16.dp))
        action?.invoke()
    }
}

@Composable
fun CardBox(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(modifier.fillMaxWidth().clip(shape).background(Bg2).border(1.dp, Line, shape), content = content)
}

@Composable
fun HLine() = Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
