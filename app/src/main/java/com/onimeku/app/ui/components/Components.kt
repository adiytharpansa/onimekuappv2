package com.onimeku.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.res.painterResource
import com.onimeku.app.R
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.onimeku.app.data.Anime
import com.onimeku.app.ui.theme.*

val NavPad = 110.dp   // ruang agar konten tidak tertutup menu melayang

/** Gradasi unik per anime sebagai placeholder sampai poster dari API terisi. */
fun placeholder(id: Int, angle: Float = 0f): Brush {
    val h = (id * 47 + 340) % 360f
    return Brush.linearGradient(listOf(Color.hsv(h, .72f, .45f), Color.hsv((h + 30) % 360, .6f, .17f)))
}

/** Key-art placeholder: gradasi berlapis + cahaya radial + inisial besar, sampai poster API terisi. */
@Composable
fun Poster(anime: Anime, modifier: Modifier = Modifier, banner: Boolean = false) {
    val h = (anime.id * 47 + 340) % 360f
    Box(modifier.background(placeholder(anime.id)).drawBehind {
        drawRect(Brush.radialGradient(listOf(Color.hsv(h, .9f, .9f).copy(alpha = .5f), Color.Transparent), Offset(size.width * .78f, size.height * .22f), size.maxDimension * .55f))
        drawRect(Brush.radialGradient(listOf(Color.hsv((h + 50) % 360, .85f, .8f).copy(alpha = .35f), Color.Transparent), Offset(size.width * .15f, size.height * .85f), size.maxDimension * .6f))
    }) {
        AsyncImage(if (banner) anime.banner.ifBlank { anime.poster } else anime.poster, anime.title, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
        if (anime.poster.isBlank()) Text(anime.title.take(1), color = Color.White.copy(.1f), fontSize = if (banner) 280.sp else 90.sp,
            fontWeight = FontWeight.ExtraBold, modifier = Modifier.align(Alignment.TopEnd).padding(end = 6.dp))
    }
}

@Composable
fun Logo(size: Dp = 34.dp) = Box(Modifier.size(size).clip(RoundedCornerShape(size * .29f)).background(LogoRed), Alignment.Center) {
    androidx.compose.foundation.Image(painterResource(R.drawable.ic_logo_ring), "Onimeku", Modifier.fillMaxSize(.62f)) }

@Composable
fun MetaChip(text: String, star: Boolean = false) = Row(Modifier.clip(RoundedCornerShape(8.dp)).background(Color.White.copy(.14f)).padding(horizontal = 9.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
    if (star) { Icon(Icons.Rounded.Star, null, Modifier.size(12.dp), tint = Gold); Spacer(Modifier.width(3.dp)) }
    Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }

@Composable
fun Modifier.pressScale(src: MutableInteractionSource, onClick: () -> Unit): Modifier {
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) .94f else 1f, spring(Spring.DampingRatioMediumBouncy), label = "press")
    return scale(s).clickable(src, null, onClick = onClick)
}

@Composable
fun PosterCard(anime: Anime, rank: Int? = null, width: Int = 132, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    Column(Modifier.width(width.dp).pressScale(src, onClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(RoundedCornerShape(16.dp))) {
            Poster(anime, Modifier.matchParentSize())
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(.8f)), startY = 180f)))
            Row(Modifier.padding(8.dp)) { Badge(anime.ageRating, Color.White, Color.Black); Spacer(Modifier.weight(1f)); if (anime.hd) Badge("HD", Crimson, Color.White) }
            if (rank != null) Text("$rank", fontSize = 46.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.BottomStart).padding(start = 10.dp))
            else Row(Modifier.align(Alignment.BottomEnd).padding(8.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.Star, null, Modifier.size(12.dp), tint = Gold); Text(" ${"%.1f".format(anime.rating)}", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        }
        Text(anime.title, Modifier.padding(top = 7.dp), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

/** Kartu lebar untuk "Lanjut Menonton" dengan bar progres. */
@Composable
fun WideCard(anime: Anime, ep: Int, pct: Int, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    Column(Modifier.width(220.dp).pressScale(src, onClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(16.dp))) {
            Poster(anime, Modifier.matchParentSize(), banner = true)
            Icon(Icons.Rounded.PlayCircle, null, Modifier.align(Alignment.Center).size(40.dp), tint = Color.White.copy(.9f))
            LinearProgressIndicator({ pct / 100f }, Modifier.fillMaxWidth().height(4.dp).align(Alignment.BottomCenter), color = Crimson, trackColor = Color.White.copy(.25f))
        }
        Text(anime.title, Modifier.padding(top = 7.dp), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Text("Lanjut Eps $ep", color = TextSecondary, fontSize = 11.sp)
    }
}

@Composable
fun Badge(text: String, bg: Color, fg: Color) = Text(text, color = fg, fontSize = 10.sp, fontWeight = FontWeight.Bold,
    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(bg).padding(horizontal = 6.dp, vertical = 2.dp))

@Composable
fun SectionHeader(title: String, action: String? = null, onAction: () -> Unit = {}) =
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(4.dp, 16.dp).background(Crimson, RoundedCornerShape(2.dp))); Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (action != null) Text(action, color = Crimson, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable(onClick = onAction))
    }

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) Crimson else Surface1, label = "chip")
    Text(text, Modifier.clip(RoundedCornerShape(20.dp)).background(bg).border(1.dp, if (selected) Crimson else Color.White.copy(.13f), RoundedCornerShape(20.dp))
        .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp), fontSize = 13.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
}

@Composable
fun ChipRow(items: List<String>, selected: String, onSelect: (String) -> Unit) =
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(items) { Chip(it, it == selected) { onSelect(it) } } }

@Composable
fun IconCircle(icon: ImageVector, desc: String, modifier: Modifier = Modifier, bg: Color = Color.Black.copy(.45f), tint: Color = Color.White, size: Dp = 40.dp, onClick: () -> Unit) =
    Box(modifier.size(size).clip(CircleShape).background(bg).clickable(onClick = onClick), Alignment.Center) { Icon(icon, desc, tint = tint, modifier = Modifier.size(size * .5f)) }

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null, onClick: () -> Unit) =
    Button(onClick, modifier.height(46.dp), shape = RoundedCornerShape(26.dp), colors = ButtonDefaults.buttonColors(containerColor = Crimson)) {
        if (icon != null) { Icon(icon, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)) }; Text(text, fontWeight = FontWeight.SemiBold) }

@Composable
fun GlassButton(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null, onClick: () -> Unit) =
    OutlinedButton(onClick, modifier.height(46.dp), shape = RoundedCornerShape(26.dp), border = BorderStroke(1.dp, Color.White.copy(.35f)),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White.copy(.08f), contentColor = Color.White)) {
        if (icon != null) { Icon(icon, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)) }; Text(text) }

@Composable
fun Modifier.shimmer(): Modifier {
    val x by rememberInfiniteTransition(label = "sh").animateFloat(-300f, 900f, infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "x")
    return background(Brush.linearGradient(listOf(Surface1, Surface3, Surface1), Offset(x, 0f), Offset(x + 300f, 100f)))
}

@Composable
fun EmptyState(icon: ImageVector, msg: String) = Column(Modifier.fillMaxWidth().padding(vertical = 70.dp, horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
    Icon(icon, null, Modifier.size(52.dp), tint = Crimson); Spacer(Modifier.height(12.dp)); Text(msg, color = TextSecondary, textAlign = TextAlign.Center) }

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) = Column(Modifier.fillMaxSize().padding(32.dp), Arrangement.Center, Alignment.CenterHorizontally) {
    Text("Oops!", style = MaterialTheme.typography.headlineMedium); Text(message, color = TextSecondary, modifier = Modifier.padding(vertical = 8.dp)); PrimaryButton("Coba lagi", onClick = onRetry) }

@Composable
fun ScreenTitle(title: String, trailing: @Composable RowScope.() -> Unit = {}) =
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f)); trailing() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionSheet(title: String, options: List<String>, selected: String?, onSelect: (String) -> Unit, onDismiss: () -> Unit) =
    ModalBottomSheet(onDismiss, containerColor = Surface1) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
        options.forEach { o ->
            Row(Modifier.fillMaxWidth().clickable { onSelect(o) }.padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(o, Modifier.weight(1f), color = if (o == selected) Crimson else Color.White, fontWeight = if (o == selected) FontWeight.Bold else FontWeight.Normal)
                if (o == selected) Icon(Icons.Rounded.Check, null, tint = Crimson)
            }
        }
        Spacer(Modifier.height(28.dp))
    }
