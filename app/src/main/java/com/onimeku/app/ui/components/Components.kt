package com.onimeku.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.onimeku.app.R
import com.onimeku.app.data.Anime
import com.onimeku.app.ui.theme.Crimson
import com.onimeku.app.ui.theme.Glass
import com.onimeku.app.ui.theme.Gold
import com.onimeku.app.ui.theme.LogoRed
import com.onimeku.app.ui.theme.Radius
import com.onimeku.app.ui.theme.Space
import com.onimeku.app.ui.theme.Surface1
import com.onimeku.app.ui.theme.Surface2
import com.onimeku.app.ui.theme.Surface3
import com.onimeku.app.ui.theme.TextPrimary
import com.onimeku.app.ui.theme.TextSecondary
import com.onimeku.app.ui.theme.TextTertiary
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.animation.core.animateFloat

val NavPad = 112.dp   // ruang agar konten tidak tertutup menu melayang

/**
 * Placeholder monokrom merah tua → hitam (sesuai brand) sampai poster dari API terisi.
 * Tidak memakai hue acak agar tampilan tetap profesional.
 */
fun placeholder(id: Int): Brush {
    val shift = (id * 29 % 40)
    return Brush.linearGradient(
        listOf(
            Color(0xFF3A0D16),
            Color(0xFF22080D).copy(alpha = .96f),
            Color(0xFF131316)
        ),
        start = Offset(shift.toFloat(), 0f),
        end = Offset(600f + shift * 4f, 900f)
    )
}

/** Key-art placeholder: gradasi berlapis + cahaya radial + inisial besar, sampai poster API terisi. */
@Composable
fun Poster(anime: Anime, modifier: Modifier = Modifier, banner: Boolean = false) {
    val h = (anime.id * 47 + 340) % 360f
    Box(modifier.background(placeholder(anime.id)).drawBehind {
        drawRect(
            Brush.radialGradient(
                listOf(Color.hsv(h, .9f, .9f).copy(alpha = .5f), Color.Transparent),
                Offset(size.width * .78f, size.height * .22f),
                size.maxDimension * .55f
            )
        )
        drawRect(
            Brush.radialGradient(
                listOf(Color.hsv((h + 50) % 360, .85f, .8f).copy(alpha = .35f), Color.Transparent),
                Offset(size.width * .15f, size.height * .85f),
                size.maxDimension * .6f
            )
        )
    }) {
        AsyncImage(
            if (banner) anime.banner.ifBlank { anime.poster } else anime.poster,
            anime.title,
            Modifier.matchParentSize(),
            contentScale = ContentScale.Crop
        )
        if (anime.poster.isBlank()) {
            Text(
                anime.title.take(1),
                color = Color.White.copy(.06f),
                fontSize = if (banner) 260.sp else 90.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 6.dp)
            )
        }
    }
}

@Composable
fun Logo(size: Dp = 34.dp) =
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size * .29f)).background(LogoRed),
        Alignment.Center
    ) {
        Image(painterResource(R.drawable.ic_logo_ring), "Onimeku", Modifier.fillMaxSize(.62f))
    }

@Composable
fun MetaChip(text: String, star: Boolean = false) =
    Row(
        Modifier.clip(RoundedCornerShape(Radius.sm))
            .background(Color.White.copy(.12f))
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (star) {
            Icon(Icons.Rounded.Star, null, Modifier.size(12.dp), tint = Gold)
            Spacer(Modifier.width(3.dp))
        }
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }

@Composable
fun Modifier.pressScale(src: MutableInteractionSource, onClick: () -> Unit): Modifier {
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(
        if (pressed) .96f else 1f,
        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "press"
    )
    return scale(s).clickable(src, null, onClick = onClick)
}

/**
 * Kartu poster standar. Ukuran ditentukan pemanggil lewat [modifier]
 * (mis. `Modifier.width(132.dp)` di LazyRow atau `Modifier.fillMaxWidth()` di grid).
 */
@Composable
fun PosterCard(anime: Anime, modifier: Modifier = Modifier, rank: Int? = null, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    Column(modifier.pressScale(src, onClick)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(RoundedCornerShape(Radius.lg))
                .background(Surface2)
        ) {
            Poster(anime, Modifier.matchParentSize())
            Box(
                Modifier.matchParentSize().background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        .55f to Color.Transparent,
                        1f to Color.Black.copy(.85f)
                    )
                )
            )
            Row(Modifier.fillMaxWidth().padding(Space.sm)) {
                Badge(anime.ageRating, Color.Black.copy(.55f), Color.White)
                Spacer(Modifier.weight(1f))
                if (anime.hd) Badge("HD", Crimson, Color.White)
            }
            Row(
                Modifier.align(Alignment.BottomEnd).padding(Space.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Star, null, Modifier.size(12.dp), tint = Gold)
                Text(" ${"%.1f".format(anime.rating)}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            if (rank != null) Text(
                "$rank",
                fontSize = 44.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 9.dp)
            )
        }
        Text(
            anime.title,
            Modifier.padding(top = Space.xs, start = 2.dp, end = 2.dp),
            maxLines = 2,
            minLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 13.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/** Kartu lebar untuk "Lanjut Menonton" dengan bar progres. */
@Composable
fun WideCard(anime: Anime, ep: Int, pct: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    Column(modifier.width(228.dp).pressScale(src, onClick)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(Radius.md))
                .background(Surface2)
        ) {
            Poster(anime, Modifier.matchParentSize(), banner = true)
            Box(
                Modifier.matchParentSize().background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(.55f)))
                )
            )
            Box(
                Modifier.align(Alignment.Center).size(46.dp).clip(CircleShape)
                    .background(Color.Black.copy(.45f)),
                Alignment.Center
            ) { Icon(Icons.Rounded.PlayArrow, "Putar", Modifier.size(26.dp), tint = Color.White) }
            LinearProgressIndicator(
                { pct / 100f },
                Modifier.fillMaxWidth().height(4.dp).align(Alignment.BottomCenter),
                color = Crimson,
                trackColor = Color.White.copy(.22f)
            )
        }
        Text(
            anime.title, Modifier.padding(top = Space.xs),
            maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp, fontWeight = FontWeight.Medium
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Lanjut Eps $ep", color = TextSecondary, fontSize = 11.sp)
            Text("  ·  $pct%", color = TextTertiary, fontSize = 11.sp)
        }
    }
}

@Composable
fun Badge(text: String, bg: Color, fg: Color) = Text(
    text,
    color = fg,
    fontSize = 10.sp,
    lineHeight = 12.sp,
    fontWeight = FontWeight.Bold,
    modifier = Modifier.clip(RoundedCornerShape(Radius.sm)).background(bg)
        .padding(horizontal = 6.dp, vertical = 3.dp)
)

@Composable
fun SectionHeader(
    title: String,
    action: String? = null,
    onAction: () -> Unit = {}
) = Row(
    Modifier.fillMaxWidth()
        .padding(start = Space.lg, end = Space.sm, top = Space.xl, bottom = Space.sm),
    verticalAlignment = Alignment.CenterVertically
) {
    Box(Modifier.size(4.dp, 16.dp).background(Crimson, RoundedCornerShape(2.dp)))
    Spacer(Modifier.width(Space.sm))
    Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
    if (action != null) Row(
        Modifier.clip(RoundedCornerShape(Radius.full))
            .clickable(onClick = onAction)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(action, color = Crimson, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Icon(
            Icons.AutoMirrored.Rounded.KeyboardArrowRight, null,
            Modifier.size(16.dp), tint = Crimson
        )
    }
}

@Composable
fun Chip(
    text: String,
    selected: Boolean,
    leadingIcon: ImageVector? = null,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(if (selected) Crimson else Surface1, label = "chipBg")
    val fg by animateColorAsState(if (selected) Color.White else TextSecondary, label = "chipFg")
    Row(
        Modifier.clip(RoundedCornerShape(Radius.full)).background(bg)
            .border(
                1.dp,
                if (selected) Crimson else Color.White.copy(.1f),
                RoundedCornerShape(Radius.full)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, null, Modifier.size(14.dp), tint = fg)
            Spacer(Modifier.width(5.dp))
        }
        Text(
            text,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = fg
        )
    }
}

@Composable
fun ChipRow(
    items: List<String>,
    selected: String,
    contentPadding: PaddingValues = PaddingValues(horizontal = Space.lg, vertical = Space.xxs),
    onSelect: (String) -> Unit
) =
    LazyRow(
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(Space.sm)
    ) {
        items(items) { Chip(it, it == selected) { onSelect(it) } }
    }

@Composable
fun IconCircle(
    icon: ImageVector,
    desc: String,
    modifier: Modifier = Modifier,
    bg: Color = Color.Black.copy(.4f),
    tint: Color = Color.White,
    size: Dp = 40.dp,
    onClick: () -> Unit
) = Box(
    modifier.size(size).clip(CircleShape).background(bg)
        .border(1.dp, Color.White.copy(.08f), CircleShape)
        .clickable(onClick = onClick),
    Alignment.Center
) { Icon(icon, desc, tint = tint, modifier = Modifier.size(size * .48f)) }

@Composable
fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) = Button(
    onClick,
    modifier.height(48.dp),
    enabled = enabled,
    shape = RoundedCornerShape(Radius.full),
    colors = ButtonDefaults.buttonColors(
        containerColor = Crimson,
        contentColor = Color.White,
        disabledContainerColor = Crimson.copy(.35f),
        disabledContentColor = Color.White.copy(.6f)
    ),
    contentPadding = PaddingValues(horizontal = Space.xl, vertical = Space.sm)
) {
    if (icon != null) {
        Icon(icon, null, Modifier.size(19.dp))
        Spacer(Modifier.width(7.dp))
    }
    Text(text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
}

@Composable
fun GlassButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    onClick: () -> Unit
) = OutlinedButton(
    onClick,
    modifier.height(48.dp),
    shape = RoundedCornerShape(Radius.full),
    border = BorderStroke(1.dp, Color.White.copy(.3f)),
    colors = ButtonDefaults.outlinedButtonColors(
        containerColor = Color.White.copy(.07f),
        contentColor = Color.White
    ),
    contentPadding = PaddingValues(horizontal = Space.xl, vertical = Space.sm)
) {
    if (icon != null) {
        Icon(icon, null, Modifier.size(19.dp))
        Spacer(Modifier.width(7.dp))
    }
    Text(text, fontSize = 14.sp)
}

@Composable
fun Modifier.shimmer(): Modifier {
    val x by rememberInfiniteTransition(label = "sh").animateFloat(
        -300f, 900f, infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "x"
    )
    return background(
        Brush.linearGradient(
            listOf(Surface1, Surface3, Surface1),
            Offset(x, 0f),
            Offset(x + 300f, 100f)
        )
    )
}

@Composable
fun EmptyState(icon: ImageVector, title: String, subtitle: String? = null) = Column(
    Modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = Space.xxl),
    horizontalAlignment = Alignment.CenterHorizontally
) {
    Box(
        Modifier.size(74.dp).clip(CircleShape).background(Surface1)
            .border(1.dp, Color.White.copy(.07f), CircleShape),
        Alignment.Center
    ) { Icon(icon, null, Modifier.size(32.dp), tint = Crimson) }
    Spacer(Modifier.height(Space.lg))
    Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, textAlign = TextAlign.Center)
    if (subtitle != null) {
        Spacer(Modifier.height(Space.xxs))
        Text(subtitle, color = TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 19.sp)
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) = Column(
    Modifier.fillMaxSize().padding(Space.huge),
    Arrangement.Center,
    Alignment.CenterHorizontally
) {
    Logo(56.dp)
    Spacer(Modifier.height(Space.lg))
    Text("Gagal memuat", style = MaterialTheme.typography.titleLarge)
    Text(
        message, color = TextSecondary, textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = Space.xs, bottom = Space.lg)
    )
    PrimaryButton("Coba lagi", onClick = onRetry)
}

@Composable
fun ScreenTitle(
    title: String,
    subtitle: String? = null,
    trailing: @Composable RowScope.() -> Unit = {}
) = Row(
    Modifier.fillMaxWidth().statusBarsPadding()
        .padding(start = Space.lg, end = Space.lg, top = Space.lg, bottom = Space.sm),
    verticalAlignment = Alignment.CenterVertically
) {
    Column(Modifier.weight(1f)) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        if (subtitle != null) {
            Text(subtitle, color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 3.dp))
        }
    }
    trailing()
}

/** Header untuk halaman turunan (Cari, Notifikasi, Detail) dengan tombol kembali. */
@Composable
fun ScreenHeader(
    title: String,
    onBack: () -> Unit,
    subtitle: String? = null,
    trailing: @Composable RowScope.() -> Unit = {}
) = Row(
    Modifier.fillMaxWidth().statusBarsPadding()
        .padding(horizontal = Space.lg, vertical = Space.md),
    verticalAlignment = Alignment.CenterVertically
) {
    IconCircle(Icons.AutoMirrored.Rounded.ArrowBack, "Kembali", bg = Surface2, onClick = onBack)
    Column(Modifier.weight(1f).padding(horizontal = Space.md)) {
        Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (subtitle != null) {
            Text(subtitle, color = TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    trailing()
}

/** Grup pengaturan/konten dengan judul kecil di atas kartu. */
@Composable
fun SectionCard(title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        if (title != null) Text(
            title.uppercase(),
            color = TextTertiary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = Space.xl, bottom = Space.sm, top = Space.xl)
        )
        Column(
            Modifier.padding(horizontal = Space.lg).fillMaxWidth()
                .clip(RoundedCornerShape(Radius.xl)).background(Surface1)
                .border(1.dp, Color.White.copy(.05f), RoundedCornerShape(Radius.xl)),
            content = content
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionSheet(
    title: String,
    options: List<String>,
    selected: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) = ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = Surface1,
    shape = RoundedCornerShape(topStart = Radius.sheet, topEnd = Radius.sheet),
    dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(.2f)) }
) {
    Row(
        Modifier.fillMaxWidth().padding(start = Space.xl, end = Space.sm, bottom = Space.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        IconButton(onDismiss) { Icon(Icons.Rounded.Close, "Tutup", tint = TextSecondary) }
    }
    options.forEach { o ->
        val isSelected = o == selected
        Row(
            Modifier.fillMaxWidth().clickable { onSelect(o) }
                .padding(horizontal = Space.xl, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                o,
                Modifier.weight(1f),
                color = if (isSelected) Crimson else TextPrimary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            if (isSelected) Icon(Icons.Rounded.Check, "Dipilih", tint = Crimson)
        }
    }
    Spacer(Modifier.height(28.dp))
}

@Composable
fun DividerLine(modifier: Modifier = Modifier) =
    Box(modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(.05f)))

/** Tombol aksi cepat berlabel (My List / Favorit / Unduh). */
@Composable
fun QuickAction(
    icon: ImageVector,
    label: String,
    active: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val tint by animateColorAsState(if (active) Crimson else TextPrimary, label = "qaTint")
    val container by animateColorAsState(
        if (active) Crimson.copy(.12f) else Surface1,
        label = "qaBg"
    )
    Column(
        modifier.clip(RoundedCornerShape(Radius.lg)).background(container)
            .border(
                1.dp,
                if (active) Crimson.copy(.5f) else Color.White.copy(.07f),
                RoundedCornerShape(Radius.lg)
            )
            .clickable(onClick = onClick)
            .padding(vertical = Space.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, label, tint = tint, modifier = Modifier.size(21.dp))
        Spacer(Modifier.height(Space.xxs))
        Text(label, fontSize = 11.sp, color = tint, fontWeight = FontWeight.Medium)
    }
}

/** Kartu info kecil untuk statistik profil atau ringkasan. */
@Composable
fun StatBlock(value: String, label: String, modifier: Modifier = Modifier) = Column(
    modifier,
    horizontalAlignment = Alignment.CenterHorizontally
) {
    Text(value, fontSize = 21.sp, fontWeight = FontWeight.Bold)
    Text(label, color = TextSecondary, fontSize = 12.sp)
}

/** Kartu kaca gelap untuk elemen yang menumpang di atas banner. */
@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) =
    Column(
        modifier.clip(RoundedCornerShape(Radius.lg)).background(Glass)
            .border(1.dp, Color.White.copy(.08f), RoundedCornerShape(Radius.lg)),
        content = content
    )
