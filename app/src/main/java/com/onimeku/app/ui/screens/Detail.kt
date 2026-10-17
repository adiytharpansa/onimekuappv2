package com.onimeku.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onimeku.app.data.*
import com.onimeku.app.ui.AppViewModel
import com.onimeku.app.ui.components.*
import com.onimeku.app.ui.theme.*

@Composable
fun DetailScreen(a: Anime, vm: AppViewModel, onBack: () -> Unit, onOpen: (Int) -> Unit, onPlay: (Int) -> Unit) {
    val ctx = LocalContext.current
    val status by vm.status.collectAsState()
    val fav by vm.fav.collectAsState()
    val dls by vm.downloads.collectAsState()
    val history by vm.history.collectAsState()
    val reviews by vm.reviews.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    var season by remember { mutableIntStateOf(1) }
    var sheet by remember { mutableStateOf(false) }
    val d = dls[a.id]
    val resumeEp = history[a.id]?.ep ?: 1
    val my = reviews[a.id] ?: emptyList()

    if (sheet) OptionSheet("Simpan ke My List", ListStatus.entries.map { it.label } + if (a.id in status) listOf("Hapus dari My List") else emptyList(),
        status[a.id]?.label, { sel -> vm.setStatus(a.id, ListStatus.entries.firstOrNull { it.label == sel }); sheet = false }, { sheet = false })

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 30.dp)) {
        item {
            Box(Modifier.fillMaxWidth().height(280.dp)) {
                Poster(a, Modifier.fillMaxSize(), banner = true)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(.5f), Color.Transparent, Bg))))
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp)) {
                    IconCircle(Icons.AutoMirrored.Rounded.ArrowBack, "Kembali", onClick = onBack); Spacer(Modifier.weight(1f))
                    IconCircle(Icons.Rounded.Share, "Bagikan") {
                        ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "Nonton ${a.title} di Onimeku!"), "Bagikan")) }
                }
                IconCircle(Icons.Rounded.PlayArrow, "Putar", Modifier.align(Alignment.Center), bg = Crimson, size = 66.dp) { onPlay(resumeEp) }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text(a.title, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                Row(Modifier.padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) { MetaChip("%.1f".format(a.rating), true); MetaChip("${a.year}"); MetaChip("${a.episodes} Eps"); MetaChip(a.studio) }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { (a.genres + "HD").forEach { Badge(it, Wine, Color(0xFFFFD3D8)) } }
                Row(Modifier.padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    PrimaryButton(if (history[a.id] != null) "Lanjut Eps $resumeEp" else "Putar", Modifier.weight(1f), Icons.Rounded.PlayArrow) { onPlay(resumeEp) }
                    IconCircle(if (a.id in status) Icons.Rounded.Check else Icons.Rounded.Add, "My List", bg = Surface2, size = 46.dp) { sheet = true }
                    IconCircle(Icons.Rounded.Favorite, "Favorit", bg = Surface2, tint = if (a.id in fav) Crimson else Color.White, size = 46.dp) { vm.toggleFav(a.id) }
                    IconCircle(if (d?.p == 100) Icons.Rounded.DownloadDone else Icons.Rounded.Download, "Unduh", bg = Surface2, size = 46.dp) { vm.download(a.id) }
                }
                if (d != null && d.p < 100) LinearProgressIndicator({ d.p / 100f }, Modifier.fillMaxWidth().padding(bottom = 12.dp), color = Crimson, trackColor = Surface2)
            }
        }
        item {
            TabRow(tab, containerColor = Color.Transparent, contentColor = Color.White, divider = {},
                indicator = { TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(it[tab]), color = Crimson) }) {
                listOf("Episode", "Info", "Ulasan").forEachIndexed { i, t -> Tab(tab == i, { tab = i }, text = { Text(t, fontWeight = FontWeight.SemiBold, color = if (tab == i) Color.White else TextSecondary) }) } }
        }
        when (tab) {
            0 -> {
                item { ChipRow(listOf("Season 1", "Season 2"), "Season $season") { season = it.last().digitToInt() } }
                items((1..12).toList()) { n ->
                    val ep = n + (season - 1) * 12
                    val watched = (history[a.id]?.ep ?: 1) > ep
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 5.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Surface1).clickable { onPlay(ep) }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.width(104.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(10.dp)).background(placeholder(a.id + ep)), Alignment.Center) {
                            Icon(Icons.Rounded.PlayArrow, null, Modifier.size(20.dp))
                            if (watched) LinearProgressIndicator({ 1f }, Modifier.fillMaxWidth().align(Alignment.BottomCenter).height(3.dp), color = Crimson, trackColor = Color.Transparent) }
                        Column(Modifier.padding(start = 12.dp)) { Text("Episode $ep", fontWeight = FontWeight.SemiBold); Text("24 menit${if (watched) " · Ditonton" else ""}", color = TextSecondary, fontSize = 12.sp) }
                    }
                }
            }
            1 -> {
                item { Column(Modifier.padding(16.dp)) {
                    Text(a.synopsis, color = TextSecondary, lineHeight = 22.sp)
                    Column(Modifier.padding(vertical = 16.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Surface1)) {
                        listOf("Studio" to a.studio, "Rilis" to "${a.year}", "Rating usia" to a.ageRating, "Total episode" to "${a.episodes}").forEach { (k, v) ->
                            Row(Modifier.padding(16.dp)) { Text(k, color = TextSecondary, modifier = Modifier.weight(1f)); Text(v, fontWeight = FontWeight.Bold) } } }
                    Text("Pengisi Suara", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) { listOf("Natsuki", "Satoshi", "Akari", "Yoshitsugu").forEach { n ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) { Box(Modifier.size(56.dp).background(placeholder(n.length * 7), CircleShape), Alignment.Center) { Text(n.take(1), fontWeight = FontWeight.Bold) }
                            Text(n, fontSize = 11.sp, color = TextSecondary, modifier = Modifier.padding(top = 4.dp)) } } }
                } }
                item { SectionHeader("Mirip dengan ini") }
                item { LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(AnimeRepository.all.filter { it.id != a.id }.take(8)) { r -> PosterCard(r, width = 112) { onOpen(r.id) } } } }
            }
            else -> {
                item { Column(Modifier.padding(16.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Surface1).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Beri nilai", fontWeight = FontWeight.Bold)
                    Row { (1..5).forEach { n -> IconButton({ vm.addReview(a.id, n) }) { Icon(Icons.Rounded.Star, "$n bintang", tint = Gold, modifier = Modifier.size(30.dp)) } } } } }
                if (my.isEmpty()) item { EmptyState(Icons.Rounded.RateReview, "Belum ada ulasan. Jadilah yang pertama!") }
                items(my) { r -> Column(Modifier.padding(horizontal = 16.dp, vertical = 5.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Surface1).padding(14.dp)) {
                    Row { Text(r.user, fontWeight = FontWeight.Bold); Text("  " + "★".repeat(r.stars), color = Gold) }
                    Text(r.text, color = TextSecondary, modifier = Modifier.padding(top = 4.dp)) } }
            }
        }
    }
}
