package com.onimeku.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onimeku.app.data.*
import com.onimeku.app.ui.AppViewModel
import com.onimeku.app.ui.components.*
import com.onimeku.app.ui.theme.*
import java.util.Calendar

@Composable
private fun ListRow(a: Anime, sub: String, onClick: () -> Unit, trailing: @Composable RowScope.() -> Unit = {}, extra: @Composable ColumnScope.() -> Unit = {}) =
    Row(Modifier.padding(horizontal = 16.dp, vertical = 5.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Surface1).clickable(onClick = onClick).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Poster(a, Modifier.size(58.dp, 82.dp).clip(RoundedCornerShape(12.dp)))
        Column(Modifier.padding(horizontal = 12.dp).weight(1f)) { Text(a.title, fontWeight = FontWeight.SemiBold, maxLines = 1); Text(sub, color = TextSecondary, fontSize = 12.sp); extra() }
        trailing()
    }

@Composable
fun ReleaseScreen(vm: AppViewModel, onOpen: (Int) -> Unit) {
    val remind by vm.remind.collectAsState()
    val now = remember { Calendar.getInstance() }
    var day by remember { mutableIntStateOf(now.get(Calendar.DAY_OF_WEEK) - 1) }
    val names = listOf("Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab")
    val todays = AnimeRepository.schedule.filter { it.day == day }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = NavPad)) {
        item { ScreenTitle("Kalender Rilis") }
        item { LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(7) { i ->
                val c = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, i - (now.get(Calendar.DAY_OF_WEEK) - 1)) }
                val sel = i == day
                Column(Modifier.clip(RoundedCornerShape(18.dp)).background(if (sel) Crimson else Surface1).clickable { day = i }.padding(horizontal = 16.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(names[i], fontSize = 12.sp, color = if (sel) Color.White else TextSecondary); Text("${c.get(Calendar.DAY_OF_MONTH)}", fontWeight = FontWeight.Bold, fontSize = 18.sp) } } } }
        item { Spacer(Modifier.height(12.dp)) }
        if (todays.isEmpty()) item { EmptyState(Icons.Rounded.EventBusy, "Belum ada rilis di hari ini") }
        items(todays) { r ->
            ListRow(r.anime, "Episode ${r.episode} · ${r.time}", { onOpen(r.anime.id) }, trailing = {
                IconCircle(Icons.Rounded.Notifications, "Ingatkan", bg = if (r.anime.id in remind) Crimson else Surface3) { vm.toggleRemind(r.anime.id) } })
        }
    }
}

@Composable
fun MyListScreen(vm: AppViewModel, onOpen: (Int) -> Unit) {
    val status by vm.status.collectAsState()
    val fav by vm.fav.collectAsState()
    var filter by remember { mutableStateOf("Semua") }
    var grid by remember { mutableStateOf(true) }
    val list = AnimeRepository.all.filter { a -> when (filter) {
        "Semua" -> a.id in status || a.id in fav; "Favorit" -> a.id in fav
        else -> status[a.id]?.label?.let { filter == when (it) { ListStatus.Watching.label -> "Menonton"; ListStatus.Plan.label -> "Rencana"; else -> "Selesai" } } == true } }
    LazyVerticalGrid(GridCells.Fixed(3), Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = NavPad), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item(span = { GridItemSpan(3) }) { ScreenTitle("My List") { IconCircle(if (grid) Icons.AutoMirrored.Rounded.ViewList else Icons.Rounded.GridView, "Tampilan", bg = Surface2) { grid = !grid } } }
        item(span = { GridItemSpan(3) }) { ChipRow(listOf("Semua", "Menonton", "Rencana", "Selesai", "Favorit"), filter) { filter = it } }
        if (list.isEmpty()) item(span = { GridItemSpan(3) }) { EmptyState(Icons.Rounded.BookmarkBorder, "Belum ada anime di kategori ini") }
        else if (grid) items(list, key = { it.id }) { Box(Modifier.padding(horizontal = 6.dp)) { PosterCard(it, width = 120) { onOpen(it.id) } } }
        else items(list, key = { it.id }, span = { GridItemSpan(3) }) { a -> ListRow(a, a.genres.joinToString(" · "), { onOpen(a.id) }, extra = {
            Box(Modifier.padding(top = 6.dp)) { Badge(status[a.id]?.label ?: "Favorit", Wine, Color(0xFFFFD3D8)) } }) }
    }
}

@Composable
fun DownloadScreen(vm: AppViewModel, onOpen: (Int) -> Unit) {
    val dl by vm.downloads.collectAsState()
    val used = dl.size * 0.4f
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = NavPad)) {
        item { ScreenTitle("Unduhan") { if (dl.isNotEmpty()) GlassButton("Hapus semua", Modifier.height(36.dp)) { vm.clearDownloads() } } }
        item { Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Surface1).padding(16.dp)) {
            Row { Text("Penyimpanan", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); Text("${"%.1f".format(used)} GB dari 64 GB", color = TextSecondary, fontSize = 13.sp) }
            LinearProgressIndicator({ (used / 64f).coerceAtLeast(.02f) }, Modifier.fillMaxWidth().padding(top = 10.dp).height(6.dp).clip(CircleShape), color = Crimson, trackColor = Surface3) } }
        item { Spacer(Modifier.height(10.dp)) }
        if (dl.isEmpty()) item { EmptyState(Icons.Rounded.CloudDownload, "Belum ada unduhan.\nUnduh dari halaman detail.") }
        items(dl.keys.toList()) { id ->
            val a = AnimeRepository.byId(id); val d = dl[id] ?: Dl()
            ListRow(a, if (d.p >= 100) "Selesai · 400 MB" else if (d.paused) "Dijeda · ${d.p}%" else "Mengunduh · ${d.p}%", { onOpen(id) },
                trailing = { if (d.p < 100) IconCircle(if (d.paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause, "Jeda", bg = Surface3, size = 38.dp) { vm.togglePause(id) }
                    Spacer(Modifier.width(6.dp)); IconCircle(Icons.Rounded.DeleteOutline, "Hapus", bg = Surface3, size = 38.dp) { vm.removeDownload(id) } },
                extra = { if (d.p < 100) LinearProgressIndicator({ d.p / 100f }, Modifier.fillMaxWidth().padding(top = 8.dp), color = Crimson, trackColor = Surface3) })
        }
    }
}

@Composable
fun NotifScreen(vm: AppViewModel, onBack: () -> Unit) {
    val list by vm.notif.collectAsState()
    DisposableEffect(Unit) { onDispose { vm.markNotifRead() } }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 30.dp)) {
        item { Row(Modifier.statusBarsPadding().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconCircle(Icons.AutoMirrored.Rounded.ArrowBack, "Kembali", bg = Surface2, onClick = onBack); Text("Notifikasi", Modifier.padding(start = 12.dp), style = MaterialTheme.typography.titleLarge) } }
        items(list) { n -> Row(Modifier.padding(horizontal = 16.dp, vertical = 5.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Surface1).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).background(if (n.read) Surface3 else Crimson, CircleShape), Alignment.Center) { Icon(Icons.Rounded.Notifications, null, Modifier.size(20.dp)) }
            Column(Modifier.padding(start = 12.dp)) { Text(n.text, fontWeight = FontWeight.Medium); Text("Baru saja", color = TextSecondary, fontSize = 12.sp) } } }
    }
}

@Composable
fun ProfileScreen(vm: AppViewModel, onLogout: () -> Unit) {
    val st by vm.settings.collectAsState()
    val status by vm.status.collectAsState()
    val fav by vm.fav.collectAsState()
    val hist by vm.history.collectAsState()
    var sheet by remember { mutableStateOf("") }
    if (sheet == "q") OptionSheet("Kualitas video", listOf("480p", "720p", "1080p"), st.quality, { q -> vm.setSettings { it.copy(quality = q) }; sheet = "" }, { sheet = "" })
    if (sheet == "s") OptionSheet("Bahasa subtitle", listOf("Indonesia", "English", "Mati"), st.subtitle, { s -> vm.setSettings { it.copy(subtitle = s) }; sheet = "" }, { sheet = "" })
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = NavPad), horizontalAlignment = Alignment.CenterHorizontally) {
        item { Spacer(Modifier.statusBarsPadding().height(24.dp))
            Box(Modifier.size(92.dp).background(Brush.linearGradient(listOf(Crimson, DeepRed)), CircleShape), Alignment.Center) { Icon(Icons.Rounded.Person, null, Modifier.size(46.dp)) } }
        item { Text("Pengguna Onimeku", Modifier.padding(top = 12.dp), style = MaterialTheme.typography.titleLarge); Text("Member Gratis", color = TextSecondary, modifier = Modifier.padding(bottom = 20.dp)) }
        item { Row(Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Surface1).padding(vertical = 14.dp)) {
            listOf("Ditonton" to hist.size + 14, "My List" to status.size, "Favorit" to fav.size).forEach { (k, v) ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) { Text("$v", fontSize = 20.sp, fontWeight = FontWeight.Bold); Text(k, color = TextSecondary, fontSize = 12.sp) } } } }
        item { Spacer(Modifier.height(16.dp))
            Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Surface1)) {
                SwitchRow("Putar otomatis episode berikut", st.autoplay) { v -> vm.setSettings { it.copy(autoplay = v) } }
                SwitchRow("Unduh hanya via Wi-Fi", st.wifi) { v -> vm.setSettings { it.copy(wifi = v) } }
                SwitchRow("Notifikasi episode baru", st.push) { v -> vm.setSettings { it.copy(push = v) } }
                ValueRow("Kualitas video", st.quality) { sheet = "q" }
                ValueRow("Bahasa subtitle", st.subtitle) { sheet = "s" } } }
        item { Spacer(Modifier.height(20.dp)); GlassButton("Keluar", Modifier.padding(horizontal = 16.dp).fillMaxWidth(), onClick = onLogout)
            Text("Onimeku v1.0.0", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 14.dp)) }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) =
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f)); Switch(checked, onChange, colors = SwitchDefaults.colors(checkedTrackColor = Crimson, checkedThumbColor = Color.White)) }

@Composable
private fun ValueRow(label: String, value: String, onClick: () -> Unit) =
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f)); Text("$value ›", color = TextSecondary) }
