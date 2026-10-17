package com.onimeku.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onimeku.app.data.Anime
import com.onimeku.app.data.AnimeRepository
import com.onimeku.app.data.ListStatus
import com.onimeku.app.ui.AppViewModel
import com.onimeku.app.ui.components.Badge
import com.onimeku.app.ui.components.ChipRow
import com.onimeku.app.ui.components.EmptyState
import com.onimeku.app.ui.components.IconCircle
import com.onimeku.app.ui.components.MetaChip
import com.onimeku.app.ui.components.OptionSheet
import com.onimeku.app.ui.components.Poster
import com.onimeku.app.ui.components.PosterCard
import com.onimeku.app.ui.components.PrimaryButton
import com.onimeku.app.ui.components.QuickAction
import com.onimeku.app.ui.components.SectionCard
import com.onimeku.app.ui.components.SectionHeader
import com.onimeku.app.ui.components.placeholder
import com.onimeku.app.ui.theme.Bg
import com.onimeku.app.ui.theme.Crimson
import com.onimeku.app.ui.theme.Gold
import com.onimeku.app.ui.theme.Radius
import com.onimeku.app.ui.theme.Space
import com.onimeku.app.ui.theme.Surface1
import com.onimeku.app.ui.theme.Surface2
import com.onimeku.app.ui.theme.Surface3
import com.onimeku.app.ui.theme.TextSecondary
import com.onimeku.app.ui.theme.TextTertiary
import com.onimeku.app.ui.theme.Wine
import androidx.compose.material.icons.rounded.StarBorder

@Composable
fun DetailScreen(
    a: Anime,
    vm: AppViewModel,
    onBack: () -> Unit,
    onOpen: (Int) -> Unit,
    onPlay: (Int) -> Unit
) {
    val ctx = LocalContext.current
    val status by vm.status.collectAsState()
    val fav by vm.fav.collectAsState()
    val dls by vm.downloads.collectAsState()
    val history by vm.history.collectAsState()
    val reviews by vm.reviews.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    var season by remember { mutableIntStateOf(1) }
    var sheet by remember { mutableStateOf(false) }
    var stars by remember { mutableIntStateOf(0) }

    val d = dls[a.id]
    val resumeEp = history[a.id]?.ep ?: 1
    val my = reviews[a.id] ?: emptyList()
    val seasonCount = ((a.episodes + 11) / 12).coerceAtLeast(1)
    val statusLabel = status[a.id]?.label

    if (sheet) OptionSheet(
        "Simpan ke My List",
        ListStatus.entries.map { it.label } +
            if (statusLabel != null) listOf("Hapus dari My List") else emptyList(),
        statusLabel,
        { sel ->
            vm.setStatus(a.id, ListStatus.entries.firstOrNull { it.label == sel })
            sheet = false
        },
        { sheet = false }
    )

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = Space.huge)) {
        item {
            Box(Modifier.fillMaxWidth().height(310.dp)) {
                Poster(a, Modifier.fillMaxSize(), banner = true)
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(.6f),
                            .35f to Color.Transparent,
                            .58f to Color.Transparent,
                            1f to Bg
                        )
                    )
                )
                Row(
                    Modifier.fillMaxWidth().statusBarsPadding()
                        .padding(horizontal = Space.lg, vertical = Space.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconCircle(Icons.AutoMirrored.Rounded.ArrowBack, "Kembali", onClick = onBack)
                    Spacer(Modifier.weight(1f))
                    IconCircle(Icons.Rounded.Share, "Bagikan") {
                        ctx.startActivity(
                            Intent.createChooser(
                                Intent(Intent.ACTION_SEND).setType("text/plain")
                                    .putExtra(Intent.EXTRA_TEXT, "Nonton ${a.title} di Onimeku!"),
                                "Bagikan"
                            )
                        )
                    }
                }
                IconCircle(
                    Icons.Rounded.PlayArrow, "Putar",
                    Modifier.align(Alignment.Center),
                    bg = Crimson.copy(.94f),
                    size = 66.dp
                ) { onPlay(resumeEp) }
                if (a.id in status) Row(
                    Modifier.align(Alignment.BottomStart).padding(Space.lg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Badge(statusLabel ?: "Di My List", Crimson, Color.White)
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = Space.lg)) {
                Text(
                    a.title,
                    style = MaterialTheme.typography.headlineMedium,
                    lineHeight = 30.sp
                )
                Row(
                    Modifier.padding(top = Space.sm),
                    horizontalArrangement = Arrangement.spacedBy(Space.xs)
                ) {
                    MetaChip("%.1f".format(a.rating), true)
                    MetaChip("${a.year}")
                    MetaChip("${a.episodes} Eps")
                }
                Row(
                    Modifier.padding(top = Space.sm),
                    horizontalArrangement = Arrangement.spacedBy(Space.xs)
                ) {
                    a.genres.forEach { Badge(it, Wine, Color(0xFFFFD3D8)) }
                    Badge(a.ageRating, Surface3, Color.White)
                    if (a.hd) Badge("HD", Crimson, Color.White)
                }
                Text(
                    a.synopsis,
                    color = TextSecondary,
                    lineHeight = 21.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = Space.md)
                )
                TextButton(
                    { tab = 1 },
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(30.dp)
                ) { Text("Baca selengkapnya", color = Crimson, fontSize = 12.sp) }

                Row(
                    Modifier.fillMaxWidth().padding(top = Space.sm),
                    horizontalArrangement = Arrangement.spacedBy(Space.sm)
                ) {
                    PrimaryButton(
                        if (history[a.id] != null) "Lanjut Eps $resumeEp" else "Putar",
                        Modifier.weight(1f),
                        Icons.Rounded.PlayArrow
                    ) { onPlay(resumeEp) }
                    IconCircle(
                        if (a.id in status) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        "My List", bg = Surface2, tint = if (a.id in status) Crimson else Color.White,
                        size = 48.dp
                    ) { sheet = true }
                    IconCircle(
                        if (a.id in fav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        "Favorit", bg = Surface2, tint = if (a.id in fav) Crimson else Color.White,
                        size = 48.dp
                    ) { vm.toggleFav(a.id) }
                }

                Row(
                    Modifier.fillMaxWidth().padding(top = Space.md),
                    horizontalArrangement = Arrangement.spacedBy(Space.sm)
                ) {
                    QuickAction(
                        if (a.id in status) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        "My List",
                        active = a.id in status,
                        modifier = Modifier.weight(1f)
                    ) { sheet = true }
                    QuickAction(
                        if (a.id in fav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        "Favorit", active = a.id in fav, modifier = Modifier.weight(1f)
                    ) { vm.toggleFav(a.id) }
                    QuickAction(
                        when {
                            d == null -> Icons.Rounded.Download
                            d.p >= 100 -> Icons.Rounded.DownloadDone
                            else -> Icons.Rounded.Download
                        },
                        when {
                            d == null -> "Unduh"
                            d.p >= 100 -> "Tersimpan"
                            d.paused -> "Dijeda"
                            else -> "${d.p}%"
                        },
                        active = d != null,
                        modifier = Modifier.weight(1f)
                    ) { if (d == null) vm.download(a.id) else vm.togglePause(a.id) }
                }
                if (d != null && d.p < 100) {
                    LinearProgressIndicator(
                        { d.p / 100f },
                        Modifier.fillMaxWidth().padding(top = Space.md).height(5.dp)
                            .clip(CircleShape),
                        color = Crimson,
                        trackColor = Surface3
                    )
                }
                Spacer(Modifier.height(Space.md))
            }
        }

        item {
            TabRow(
                tab,
                containerColor = Color.Transparent,
                contentColor = Color.White,
                divider = {},
                indicator = {
                    TabRowDefaults.Indicator(
                        Modifier.tabIndicatorOffset(it[tab])
                            .padding(horizontal = 26.dp)
                            .height(3.dp)
                            .clip(CircleShape),
                        color = Crimson
                    )
                }
            ) {
                listOf("Episode", "Info", "Ulasan").forEachIndexed { i, t ->
                    Tab(
                        tab == i,
                        { tab = i },
                        text = {
                            Text(
                                t,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = if (tab == i) Color.White else TextSecondary
                            )
                        }
                    )
                }
            }
        }

        when (tab) {
            0 -> {
                item {
                    ChipRow((1..seasonCount).map { "Season $it" }, "Season $season") {
                        season = it.filter { c -> c.isDigit() }.toIntOrNull() ?: 1
                    }
                }
                items((1..12).toList()) { n ->
                    val ep = n + (season - 1) * 12
                    val watched = (history[a.id]?.ep ?: 1) > ep
                    EpisodeRow(a, ep, watched) { onPlay(ep) }
                }
            }

            1 -> {
                item {
                    Column(Modifier.padding(horizontal = Space.lg, vertical = Space.md)) {
                        Text("Sinopsis", style = MaterialTheme.typography.titleMedium)
                        Text(
                            a.synopsis,
                            color = TextSecondary,
                            lineHeight = 22.sp,
                            modifier = Modifier.padding(top = Space.sm)
                        )
                    }
                }
                item {
                    SectionCard("Informasi") {
                        InfoRow("Studio", a.studio)
                        HorizontalDivider(color = Color.White.copy(.05f))
                        InfoRow("Tahun rilis", "${a.year}")
                        HorizontalDivider(color = Color.White.copy(.05f))
                        InfoRow("Rating usia", a.ageRating)
                        HorizontalDivider(color = Color.White.copy(.05f))
                        InfoRow("Total episode", "${a.episodes}")
                        HorizontalDivider(color = Color.White.copy(.05f))
                        InfoRow("Genre", a.genres.joinToString(", "))
                    }
                }
                item { SectionHeader("Pengisi Suara") }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = Space.lg),
                        horizontalArrangement = Arrangement.spacedBy(Space.lg)
                    ) {
                        items(listOf("Natsuki", "Satoshi", "Akari", "Yoshitsugu", "Miyu")) { n ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    Modifier.size(58.dp).clip(CircleShape)
                                        .background(placeholder(n.length * 7)),
                                    Alignment.Center
                                ) {
                                    Text(n.take(1), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                }
                                Text(
                                    n, fontSize = 11.sp, color = TextSecondary,
                                    modifier = Modifier.padding(top = Space.xs)
                                )
                            }
                        }
                    }
                }
            }

            else -> {
                item {
                    Column(
                        Modifier.padding(Space.lg).fillMaxWidth()
                            .clip(RoundedCornerShape(Radius.xl)).background(Surface1)
                            .border(1.dp, Color.White.copy(.06f), RoundedCornerShape(Radius.xl))
                            .padding(Space.lg),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val avg = if (my.isEmpty()) a.rating else my.map { it.stars }.average()
                        Text("%.1f".format(avg), fontSize = 40.sp, fontWeight = FontWeight.Black)
                        Row {
                            repeat(5) { i ->
                                Icon(
                                    Icons.Rounded.Star, null,
                                    Modifier.size(18.dp).padding(horizontal = 1.dp),
                                    tint = if (i < avg.toInt()) Gold else Surface3
                                )
                            }
                        }
                        Text(
                            "${my.size} ulasan dari pengguna",
                            color = TextSecondary, fontSize = 12.sp,
                            modifier = Modifier.padding(top = Space.xs)
                        )
                        Box(
                            Modifier.fillMaxWidth().padding(top = Space.md).height(1.dp)
                                .background(Color.White.copy(.06f))
                        )
                        Text(
                            "Beri nilai untuk judul ini",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = Space.md)
                        )
                        Row(
                            Modifier.padding(top = Space.xs),
                            horizontalArrangement = Arrangement.spacedBy(Space.xxs)
                        ) {
                            (1..5).forEach { n ->
                                Icon(
                                    if (n <= stars) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                    "$n bintang",
                                    Modifier.size(32.dp).clickable { stars = n },
                                    tint = if (n <= stars) Gold else TextTertiary
                                )
                            }
                        }
                        PrimaryButton(
                            "Kirim ulasan",
                            Modifier.fillMaxWidth().padding(top = Space.md),
                            enabled = stars > 0
                        ) {
                            vm.addReview(a.id, stars)
                            stars = 0
                        }
                    }
                }
                if (my.isEmpty()) item {
                    EmptyState(Icons.Rounded.Star, "Belum ada ulasan", "Jadilah yang pertama memberi nilai.")
                }
                items(my) { r ->
                    Row(
                        Modifier.padding(horizontal = Space.lg, vertical = Space.xxs)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Radius.lg)).background(Surface1)
                            .padding(Space.md)
                    ) {
                        Box(
                            Modifier.size(36.dp).clip(CircleShape).background(placeholder(r.user.length * 11)),
                            Alignment.Center
                        ) { Text(r.user.take(1), fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                        Column(Modifier.padding(start = Space.md).weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(r.user, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Spacer(Modifier.weight(1f))
                                Text("★".repeat(r.stars), color = Gold, fontSize = 12.sp)
                                Text(
                                    "☆".repeat((5 - r.stars).coerceAtLeast(0)),
                                    color = Surface3, fontSize = 12.sp
                                )
                            }
                            Text(
                                r.text, color = TextSecondary, fontSize = 13.sp,
                                lineHeight = 19.sp, modifier = Modifier.padding(top = 3.dp)
                            )
                        }
                    }
                }
            }
        }

        if (tab != 2) {
            item { SectionHeader("Mirip dengan ini") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Space.lg),
                    horizontalArrangement = Arrangement.spacedBy(Space.md)
                ) {
                    items(AnimeRepository.all.filter { it.id != a.id }.take(8), key = { it.id }) { r ->
                        PosterCard(r, Modifier.width(112.dp)) { onOpen(r.id) }
                    }
                }
                Spacer(Modifier.height(Space.lg))
            }
        }
    }
}

@Composable
private fun EpisodeRow(a: Anime, ep: Int, watched: Boolean, onClick: () -> Unit) = Row(
    Modifier.padding(horizontal = Space.lg, vertical = Space.xxs)
        .fillMaxWidth().clip(RoundedCornerShape(Radius.lg)).background(Surface1)
        .clickable(onClick = onClick).padding(Space.sm),
    verticalAlignment = Alignment.CenterVertically
) {
    Box(
        Modifier.width(106.dp).aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(Radius.sm)).background(placeholder(a.id + ep)),
        Alignment.Center
    ) {
        Box(
            Modifier.size(30.dp).clip(CircleShape).background(Color.Black.copy(.4f)),
            Alignment.Center
        ) { Icon(Icons.Rounded.PlayArrow, null, Modifier.size(17.dp)) }
        if (watched) LinearProgressIndicator(
            { 1f },
            Modifier.fillMaxWidth().height(3.dp).align(Alignment.BottomCenter),
            color = Crimson,
            trackColor = Color.Transparent
        )
    }
    Column(Modifier.padding(start = Space.md).weight(1f)) {
        Text("Episode $ep", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Text("24 menit · Sub Indonesia", color = TextSecondary, fontSize = 11.sp)
        Row(
            Modifier.padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(Space.xs)
        ) {
            if (watched) Badge("Ditonton", Crimson.copy(.18f), Crimson)
            Badge("HD", Surface3, TextSecondary)
        }
    }
    Icon(
        Icons.Rounded.PlayArrow, "Putar", tint = TextTertiary,
        modifier = Modifier.padding(horizontal = Space.sm).size(20.dp)
    )
}

@Composable
private fun InfoRow(label: String, value: String) = Row(
    Modifier.fillMaxWidth().padding(horizontal = Space.lg, vertical = 13.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    Text(label, color = TextSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
    Text(
        value, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
        textAlign = TextAlign.End, modifier = Modifier.weight(1.4f)
    )
}
