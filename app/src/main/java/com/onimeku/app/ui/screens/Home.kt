package com.onimeku.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onimeku.app.data.Anime
import com.onimeku.app.data.AnimeRepository
import com.onimeku.app.data.HomeData
import com.onimeku.app.data.ReleaseItem
import com.onimeku.app.ui.AppViewModel
import com.onimeku.app.ui.UiState
import com.onimeku.app.ui.components.Badge
import com.onimeku.app.ui.components.ChipRow
import com.onimeku.app.ui.components.EmptyState
import com.onimeku.app.ui.components.ErrorState
import com.onimeku.app.ui.components.GlassButton
import com.onimeku.app.ui.components.IconCircle
import com.onimeku.app.ui.components.Logo
import com.onimeku.app.ui.components.MetaChip
import com.onimeku.app.ui.components.NavPad
import com.onimeku.app.ui.components.Poster
import com.onimeku.app.ui.components.PosterCard
import com.onimeku.app.ui.components.PrimaryButton
import com.onimeku.app.ui.components.SectionHeader
import com.onimeku.app.ui.components.WideCard
import com.onimeku.app.ui.components.shimmer
import com.onimeku.app.ui.theme.Bg
import com.onimeku.app.ui.theme.Crimson
import com.onimeku.app.ui.theme.Radius
import com.onimeku.app.ui.theme.Space
import com.onimeku.app.ui.theme.Surface1
import com.onimeku.app.ui.theme.TextSecondary
import com.onimeku.app.ui.theme.Wine
import kotlinx.coroutines.delay
import java.util.Calendar

@Composable
fun HomeScreen(
    vm: AppViewModel,
    onOpen: (Int) -> Unit,
    onSearch: () -> Unit,
    onNotif: () -> Unit,
    onPlay: (Int) -> Unit,
    onRelease: () -> Unit
) {
    val state by vm.home.collectAsState()
    when (val s = state) {
        UiState.Loading -> HomeSkeleton()
        is UiState.Error -> ErrorState(s.message) { vm.load() }
        is UiState.Success -> HomeContent(s.data, vm, onOpen, onSearch, onNotif, onPlay, onRelease)
    }
}

@Composable
private fun HomeSkeleton() = Column(
    Modifier.fillMaxSize().statusBarsPadding().padding(Space.lg),
    verticalArrangement = Arrangement.spacedBy(Space.lg)
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).shimmer())
        Spacer(Modifier.width(Space.md))
        Box(Modifier.width(110.dp).height(18.dp).clip(RoundedCornerShape(6.dp)).shimmer())
        Spacer(Modifier.weight(1f))
        Box(Modifier.size(40.dp).clip(CircleShape).shimmer())
    }
    Box(Modifier.fillMaxWidth().height(400.dp).clip(RoundedCornerShape(Radius.xl)).shimmer())
    Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
        repeat(4) { Box(Modifier.width(74.dp).height(32.dp).clip(RoundedCornerShape(Radius.full)).shimmer()) }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
        repeat(3) {
            Box(
                Modifier.weight(1f).aspectRatio(2f / 3f).clip(RoundedCornerShape(Radius.lg)).shimmer()
            )
        }
    }
}

@Composable
private fun HomeContent(
    d: HomeData,
    vm: AppViewModel,
    onOpen: (Int) -> Unit,
    onSearch: () -> Unit,
    onNotif: () -> Unit,
    onPlay: (Int) -> Unit,
    onRelease: () -> Unit
) {
    val status by vm.status.collectAsState()
    val genre by vm.genre.collectAsState()
    val history by vm.history.collectAsState()
    val notif by vm.notif.collectAsState()
    fun f(l: List<Anime>) = if (genre == "Semua") l else l.filter { genre in it.genres }

    val today = remember { Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1 }
    val todayReleases = remember(today) {
        AnimeRepository.schedule.filter { it.day == today }.take(4)
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = NavPad)) {
        item {
            HeroPager(d.featured, status.keys, onOpen, onPlay, notif.any { !it.read }, onSearch, onNotif) {
                vm.quickToggle(it)
            }
        }
        item {
            ChipRow(listOf("Semua") + AnimeRepository.genres, genre) { vm.genre.value = it }
        }
        if (history.isNotEmpty()) {
            item { SectionHeader("Lanjut Menonton") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Space.lg),
                    horizontalArrangement = Arrangement.spacedBy(Space.md)
                ) {
                    items(history.toList(), key = { it.first }) { (id, p) ->
                        WideCard(AnimeRepository.byId(id), p.ep, p.pct) { onPlay(id) }
                    }
                }
            }
        }
        item {
            SectionHeader(
                "Top Hits Anime",
                if (genre == "Semua") null else genre,
                onAction = { vm.genre.value = "Semua" }
            )
        }
        item {
            val l = f(d.top).sortedByDescending { it.rating }
            if (l.isEmpty()) EmptyState(Icons.Rounded.Search, "Tidak ada anime di genre ini")
            else LazyRow(
                contentPadding = PaddingValues(horizontal = Space.lg),
                horizontalArrangement = Arrangement.spacedBy(Space.md)
            ) {
                itemsIndexed(l, key = { _, a -> a.id }) { i, a ->
                    PosterCard(a, Modifier.width(132.dp), rank = i + 1) { onOpen(a.id) }
                }
            }
        }
        item { SectionHeader("Episode Baru") }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = Space.lg),
                horizontalArrangement = Arrangement.spacedBy(Space.md)
            ) {
                items(f(d.fresh), key = { it.id }) { a ->
                    PosterCard(a, Modifier.width(124.dp)) { onOpen(a.id) }
                }
            }
        }
        if (todayReleases.isNotEmpty()) {
            item { SectionHeader("Rilis Hari Ini", "Kalender", onRelease) }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Space.lg),
                    horizontalArrangement = Arrangement.spacedBy(Space.sm)
                ) {
                    items(todayReleases, key = { it.anime.id }) { r ->
                        ReleaseTeaser(r) { onOpen(r.anime.id) }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(Space.xl)) }
    }
}

@Composable
private fun HeroPager(
    featured: List<Anime>,
    saved: Set<Int>,
    onOpen: (Int) -> Unit,
    onPlay: (Int) -> Unit,
    hasUnread: Boolean,
    onSearch: () -> Unit,
    onNotif: () -> Unit,
    onToggleSave: (Int) -> Unit
) {
    val pager = rememberPagerState(pageCount = { featured.size })
    LaunchedEffect(pager, featured.size) {
        if (featured.size <= 1) return@LaunchedEffect
        while (true) {
            delay(6000)
            if (!pager.isScrollInProgress) {
                pager.animateScrollToPage((pager.currentPage + 1) % featured.size)
            }
        }
    }
    Box(Modifier.fillMaxWidth().height(500.dp)) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { i ->
            val a = featured[i]
            Box(Modifier.fillMaxSize()) {
                Poster(a, Modifier.fillMaxSize(), banner = true)
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(.62f),
                            .28f to Color.Transparent,
                            .55f to Color.Transparent,
                            1f to Bg
                        )
                    )
                )
                Column(Modifier.align(Alignment.BottomStart).padding(Space.lg)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Badge("#${i + 1} Trending", Wine, Color(0xFFFFD3D8))
                        Spacer(Modifier.width(Space.sm))
                        Text("·", color = TextSecondary)
                        Spacer(Modifier.width(Space.sm))
                        Text(
                            "${a.episodes} episode · ${a.ageRating}",
                            color = TextSecondary, fontSize = 11.sp
                        )
                    }
                    Text(
                        a.title,
                        Modifier.padding(top = Space.sm).clickable { onOpen(a.id) },
                        fontSize = 30.sp,
                        lineHeight = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        Modifier.padding(vertical = Space.sm),
                        horizontalArrangement = Arrangement.spacedBy(Space.xs)
                    ) {
                        MetaChip("%.1f".format(a.rating), true)
                        MetaChip("${a.year}")
                        MetaChip(a.genres.first())
                        if (a.hd) MetaChip("HD")
                    }
                    Row(
                        Modifier.padding(bottom = Space.md),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        repeat(featured.size) {
                            Box(
                                Modifier.height(5.dp)
                                    .width(if (it == pager.currentPage) 22.dp else 5.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (it == pager.currentPage) Color.White
                                        else Color.White.copy(.35f)
                                    )
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                        PrimaryButton("Putar", icon = Icons.Rounded.PlayArrow) { onPlay(a.id) }
                        GlassButton(
                            if (a.id in saved) "Tersimpan" else "My List",
                            icon = if (a.id in saved) Icons.Rounded.Check else Icons.Rounded.Add
                        ) { onToggleSave(a.id) }
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().statusBarsPadding()
                .padding(horizontal = Space.lg, vertical = Space.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Logo(34.dp)
            Column(Modifier.weight(1f).padding(start = Space.sm)) {
                Text("Onimeku", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                Text("Streaming anime gratis", color = TextSecondary, fontSize = 10.sp)
            }
            IconCircle(Icons.Rounded.Search, "Cari", onClick = onSearch)
            Spacer(Modifier.width(Space.sm))
            Box {
                IconCircle(Icons.Rounded.NotificationsNone, "Notifikasi", onClick = onNotif)
                if (hasUnread) Box(
                    Modifier.align(Alignment.TopEnd).padding(7.dp).size(10.dp)
                        .background(Crimson, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun ReleaseTeaser(r: ReleaseItem, onClick: () -> Unit) = Row(
    Modifier.width(244.dp).clip(RoundedCornerShape(Radius.lg)).background(Surface1)
        .clickable(onClick = onClick).padding(Space.sm),
    verticalAlignment = Alignment.CenterVertically
) {
    Poster(
        r.anime,
        Modifier.size(46.dp, 64.dp).clip(RoundedCornerShape(Radius.sm))
    )
    Column(Modifier.padding(start = Space.md).weight(1f)) {
        Text(
            r.anime.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        Text("Eps ${r.episode} · ${r.time}", color = TextSecondary, fontSize = 11.sp)
        Row(
            Modifier.padding(top = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.NotificationsActive, null,
                Modifier.size(12.dp), tint = Crimson
            )
            Text(" Hari ini", color = Crimson, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
