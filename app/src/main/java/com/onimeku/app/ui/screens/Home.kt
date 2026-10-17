package com.onimeku.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onimeku.app.data.*
import com.onimeku.app.ui.*
import com.onimeku.app.ui.components.*
import com.onimeku.app.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(vm: AppViewModel, onOpen: (Int) -> Unit, onSearch: () -> Unit, onNotif: () -> Unit, onPlay: (Int) -> Unit) {
    val state by vm.home.collectAsState()
    when (val s = state) {
        UiState.Loading -> Column(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.fillMaxWidth().height(340.dp).clip(RoundedCornerShape(22.dp)).shimmer())
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { repeat(3) { Box(Modifier.weight(1f).aspectRatio(2f / 3f).clip(RoundedCornerShape(16.dp)).shimmer()) } } }
        is UiState.Error -> ErrorState(s.message) { vm.load() }
        is UiState.Success -> HomeContent(s.data, vm, onOpen, onSearch, onNotif, onPlay)
    }
}

@Composable
private fun HomeContent(d: HomeData, vm: AppViewModel, onOpen: (Int) -> Unit, onSearch: () -> Unit, onNotif: () -> Unit, onPlay: (Int) -> Unit) {
    val status by vm.status.collectAsState()
    val genre by vm.genre.collectAsState()
    val history by vm.history.collectAsState()
    val notif by vm.notif.collectAsState()
    fun f(l: List<Anime>) = if (genre == "Semua") l else l.filter { genre in it.genres }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = NavPad)) {
        item {
            val pager = rememberPagerState { d.featured.size }
            LaunchedEffect(pager) { while (true) { delay(5000); pager.animateScrollToPage((pager.currentPage + 1) % pager.pageCount) } }
            Box(Modifier.fillMaxWidth().height(480.dp)) {
                HorizontalPager(pager, Modifier.fillMaxSize()) { i ->
                    val a = d.featured[i]
                    Box(Modifier.fillMaxSize()) {
                        Poster(a, Modifier.fillMaxSize(), banner = true)
                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(.65f), Color.Transparent, Bg))))
                        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                            Badge("#${i + 1} Trending", Wine, Color(0xFFFFD3D8))
                            Text(a.title, Modifier.padding(top = 8.dp).clickable { onOpen(a.id) }, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 32.sp)
                            Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) { MetaChip("%.1f".format(a.rating), true); MetaChip("${a.year}"); MetaChip(a.genres.first()); MetaChip("HD") }
                            Row(Modifier.padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                repeat(d.featured.size) { Box(Modifier.height(5.dp).width(if (it == pager.currentPage) 22.dp else 5.dp).clip(CircleShape).background(if (it == pager.currentPage) Color.White else Color.White.copy(.35f))) } }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                PrimaryButton("Putar", icon = Icons.Rounded.PlayArrow) { onPlay(a.id) }
                                GlassButton(if (a.id in status) "Tersimpan" else "My List", icon = if (a.id in status) Icons.Rounded.Check else Icons.Rounded.Add) { vm.quickToggle(a.id) }
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Logo(34.dp)
                    Text("Onimeku", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                    IconCircle(Icons.Rounded.Search, "Cari", onClick = onSearch)
                    Box { IconCircle(Icons.Rounded.NotificationsNone, "Notifikasi", onClick = onNotif)
                        if (notif.any { !it.read }) Box(Modifier.align(Alignment.TopEnd).padding(8.dp).size(9.dp).background(Crimson, CircleShape).border(2.dp, Bg, CircleShape)) }
                }
            }
        }
        item { ChipRow(listOf("Semua") + AnimeRepository.genres, genre) { vm.genre.value = it } }
        if (history.isNotEmpty()) {
            item { SectionHeader("Lanjut Menonton") }
            item { LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(history.toList(), key = { it.first }) { (id, p) -> WideCard(AnimeRepository.byId(id), p.ep, p.pct) { onPlay(id) } } } }
        }
        item { SectionHeader("Top Hits Anime") }
        item { val l = f(d.top).sortedByDescending { it.rating }
            if (l.isEmpty()) EmptyState(Icons.Rounded.Search, "Tidak ada anime")
            else LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) { itemsIndexed(l, key = { _, a -> a.id }) { i, a -> PosterCard(a, rank = i + 1) { onOpen(a.id) } } } }
        item { SectionHeader("Episode Baru") }
        item { LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) { items(f(d.fresh)) { a -> PosterCard(a) { onOpen(a.id) } } } }
    }
}
