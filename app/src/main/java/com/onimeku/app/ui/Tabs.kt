package com.onimeku.app.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import com.onimeku.app.data.*
import com.onimeku.app.media.MediaKit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable private fun Page(title: String, content: @Composable ColumnScope.() -> Unit) =
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp)) {
        Text(title, Modifier.padding(vertical = 14.dp), style = MaterialTheme.typography.headlineSmall); content() }

@Composable private fun Card(content: @Composable ColumnScope.() -> Unit) =
    Surface(Modifier.fillMaxWidth().padding(bottom = 10.dp), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) { Column(Modifier.padding(16.dp), content = content) }

@Composable private fun Empty(t: String) = Box(Modifier.fillMaxSize(), Alignment.Center) { Text(t, color = MaterialTheme.colorScheme.onSurfaceVariant) }

// ---------- Home ----------
@Composable private fun Hero(a: Anime, key: String, onOpen: () -> Unit, onPlay: () -> Unit) {
    Box(Modifier.fillMaxWidth().aspectRatio(0.9f).clip(RoundedCornerShape(24.dp)).pressClick(onOpen)) {
        PosterImage(a.poster, a.title, Modifier.fillMaxSize(), radius = 0.dp, alignment = Alignment.TopCenter, sharedKey = key)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color(0xEE000000))))
        Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primary) {
                Text(if (a.airDay != null) "SEDANG TAYANG" else "REKOMENDASI", Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    color = MaterialTheme.colorScheme.onPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
            Text(a.title, Modifier.padding(top = 8.dp), color = Color.White, style = MaterialTheme.typography.headlineMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("${a.eps} episode" + a.genreList.take(2).joinToString("") { " · $it" }, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
            Button(onPlay, Modifier.padding(top = 12.dp)) { Icon(Icons.Default.PlayArrow, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Putar") }
        }
    }
}

/** Carousel banner: geser manual atau otomatis tiap 5 detik, dengan indikator titik. */
@Composable private fun HeroPager(list: List<Anime>, onOpen: (String, String) -> Unit, onPlay: (String, Int) -> Unit) {
    val pager = rememberPagerState(pageCount = { list.size })
    LaunchedEffect(pager.settledPage, list.size) {
        if (list.size > 1) { delay(5000); if (!pager.isScrollInProgress) pager.animateScrollToPage((pager.settledPage + 1) % list.size) } }
    Column {
        HorizontalPager(pager, Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp), pageSpacing = 12.dp) { i ->
            val a = list[i]; Hero(a, "hero/${a.id}", { onOpen(a.id, "hero/${a.id}") }, { onPlay(a.id, 1) }) }
        if (list.size > 1) Row(Modifier.fillMaxWidth().padding(top = 10.dp), Arrangement.Center) {
            repeat(list.size) { i ->
                val w by animateDpAsState(if (pager.currentPage == i) 22.dp else 6.dp, label = "dot")
                Box(Modifier.padding(horizontal = 3.dp).height(6.dp).width(w).clip(CircleShape)
                    .background(if (pager.currentPage == i) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) } }
    }
}

@Composable private fun PosterCard(a: Anime, inList: Boolean, key: String, modifier: Modifier, onOpen: () -> Unit, onToggle: () -> Unit) {
    Column(modifier.pressClick(onOpen)) {
        Box {
            PosterImage(a.poster, a.title, Modifier.fillMaxWidth().aspectRatio(2f / 3f), 14.dp, sharedKey = key)
            Surface(Modifier.align(Alignment.TopStart).padding(6.dp), shape = RoundedCornerShape(8.dp), color = Color.Black.copy(alpha = 0.6f)) {
                Text("${a.eps} EP", Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
            Surface(onClick = onToggle, modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(32.dp), shape = CircleShape,
                color = if (inList) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.6f)) {
                Box(contentAlignment = Alignment.Center) {
                    Crossfade(inList, label = "tog") { on -> Icon(if (on) Icons.Default.Check else Icons.Default.Add,
                        if (on) "Hapus dari My List" else "Tambah ke My List", Modifier.size(18.dp), tint = Color.White) } } }
        }
        Text(a.title, Modifier.padding(top = 8.dp, start = 2.dp, end = 2.dp), style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        a.genreList.firstOrNull()?.let { Text(it, Modifier.padding(horizontal = 2.dp), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1) }
    }
}

@Composable private fun ContinueCard(a: Anime, ep: Int, frac: Float, onPlay: () -> Unit) {
    Column(Modifier.width(200.dp).pressClick(onPlay)) {
        Box {
            PosterImage(a.poster, a.title, Modifier.fillMaxWidth().aspectRatio(16f / 10f), 14.dp, alignment = Alignment.TopCenter)
            Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.25f)))
            Icon(Icons.Default.PlayArrow, null, Modifier.align(Alignment.Center).size(36.dp), tint = Color.White)
            LinearProgressIndicator(progress = { frac }, Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(3.dp),
                color = MaterialTheme.colorScheme.primary, trackColor = Color.Black.copy(alpha = 0.4f))
        }
        Text(a.title, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("Episode $ep", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun Rail(title: String, list: List<Anime>, section: String, mine: List<String>,
                             onOpen: (String, String) -> Unit, onToggle: (Anime) -> Unit) {
    Column {
        Text(title, Modifier.padding(start = 16.dp, top = 22.dp, bottom = 10.dp), style = MaterialTheme.typography.titleLarge)
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(list, key = { it.id }) { a -> val k = "$section/${a.id}"
                PosterCard(a, a.id in mine, k, Modifier.width(120.dp), { onOpen(a.id, k) }) { onToggle(a) } } }
    }
}

@OptIn(ExperimentalMaterial3Api::class) @UnstableApi @Composable
fun HomeTab(listState: LazyListState, onOpen: (String, String) -> Unit, onPlay: (String, Int) -> Unit) {
    val ctx = LocalContext.current; val repo = remember { Repo.of(ctx) }; val dao = remember { AppDb.get(ctx).lists() }; val scope = rememberCoroutineScope()
    val all by repo.all.collectAsState(emptyList()); val mine by dao.myList().collectAsState(emptyList())
    val recent by remember { AppDb.get(ctx).progress().recent() }.collectAsState(emptyList())
    val haptic = LocalHapticFeedback.current
    var loading by remember { mutableStateOf(true) }; var failed by remember { mutableStateOf(false) }; var pulling by remember { mutableStateOf(false) }
    var q by rememberSaveable { mutableStateOf("") }; var genre by rememberSaveable { mutableStateOf<String?>(null) }
    fun reload() { scope.launch { loading = true; failed = repo.refresh().isFailure; loading = false } }
    fun toggle(a: Anime) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); scope.launch { if (a.id in mine) dao.del(a.id) else dao.add(MyItem(a.id)) } }
    LaunchedEffect(Unit) { reload() }
    val genres = remember(all) { all.flatMap { it.genreList }.distinct().sorted() }
    val shown = all.filter { (q.isBlank() || it.title.contains(q, true)) && (genre == null || genre in it.genreList) }
    val browsing = q.isBlank() && genre == null
    val featured = remember(all) { all.filter { !it.poster.isNullOrBlank() }.sortedByDescending { it.airDay != null }.take(5) }
    val resume = remember(recent, all) {
        recent.filter { it.durationMs > 0 && it.positionMs < it.durationMs * 0.9 }.distinctBy { it.id.substringBefore(":") }
            .mapNotNull { p -> all.firstOrNull { it.id == p.id.substringBefore(":") }?.let { a ->
                Triple(a, p.id.substringAfter(":").toIntOrNull() ?: 1, (p.positionMs.toFloat() / p.durationMs).coerceIn(0f, 1f)) } }.take(10) }
    val airing = remember(all) { all.filter { it.airDay != null } }
    val finished = remember(all) { all.filter { it.airDay == null } }

    PullToRefreshBox(isRefreshing = pulling, onRefresh = { pulling = true; scope.launch { failed = repo.refresh().isFailure; pulling = false } },
        modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item(key = "title") { Text("Onimeku", Modifier.padding(horizontal = 16.dp, vertical = 14.dp), style = MaterialTheme.typography.headlineMedium) }
            item(key = "search") { OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth().padding(horizontal = 16.dp), placeholder = { Text("Cari anime") },
                leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true, shape = RoundedCornerShape(24.dp)) }
            if (genres.isNotEmpty()) item(key = "chips") { LazyRow(Modifier.padding(vertical = 8.dp), contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(genres) { g -> FilterChip(genre == g, { genre = if (genre == g) null else g }, { Text(g) }) } } }
            when {
                all.isEmpty() && loading -> item(key = "skeleton") { Column(Modifier.padding(horizontal = 16.dp)) {
                    Shimmer(Modifier.fillMaxWidth().aspectRatio(0.9f).clip(RoundedCornerShape(24.dp)))
                    Row(Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        repeat(3) { Shimmer(Modifier.weight(1f).aspectRatio(2f / 3f).clip(RoundedCornerShape(14.dp))) } } } }
                all.isEmpty() -> item(key = "error") { Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (failed) "Tidak bisa memuat daftar anime. Periksa koneksi." else "Belum ada anime.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(::reload, Modifier.padding(top = 12.dp)) { Text("Coba lagi") } } }
                shown.isEmpty() -> item(key = "none") { Text("Tidak ada hasil.", Modifier.fillMaxWidth().padding(48.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                browsing -> {
                    if (failed) item(key = "offline") { Text("Menampilkan data tersimpan (offline).", Modifier.padding(horizontal = 16.dp, vertical = 6.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    if (featured.isNotEmpty()) item(key = "hero") { Box(Modifier.padding(top = 8.dp)) { HeroPager(featured, onOpen, onPlay) } }
                    if (resume.isNotEmpty()) item(key = "resume") { Column {
                        Text("Lanjutkan Menonton", Modifier.padding(start = 16.dp, top = 22.dp, bottom = 10.dp), style = MaterialTheme.typography.titleLarge)
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(resume, key = { it.first.id }) { (a, ep, f) -> ContinueCard(a, ep, f) { onPlay(a.id, ep) } } } } }
                    if (airing.isNotEmpty()) item(key = "airing") { Rail("Sedang Tayang", airing, "airing", mine, onOpen, ::toggle) }
                    genres.take(4).forEach { g -> val l = all.filter { g in it.genreList }
                        if (l.size >= 3) item(key = "genre-$g") { Rail(g, l, "genre-$g", mine, onOpen, ::toggle) } }
                    if (finished.isNotEmpty()) item(key = "finished") { Rail("Sudah Tamat", finished, "done", mine, onOpen, ::toggle) }
                }
                else -> items(shown.chunked(3), key = { it.first().id }) { row ->
                    Row(Modifier.animateItem().padding(horizontal = 16.dp).padding(bottom = 18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { a -> val k = "search/${a.id}"; PosterCard(a, a.id in mine, k, Modifier.weight(1f), { onOpen(a.id, k) }) { toggle(a) } }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) } } }
            }
        }
    }
}

@UnstableApi @Composable
fun CalendarTab() {
    val ctx = LocalContext.current; val dao = remember { AppDb.get(ctx).lists() }; val scope = rememberCoroutineScope()
    val all by remember { Repo.of(ctx).all }.collectAsState(emptyList()); val on by dao.reminders().collectAsState(emptyList())
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val sched = all.filter { it.airDay != null && it.airTime != null }
    Page("Release Calendar") { if (sched.isEmpty()) Empty("Belum ada jadwal tayang.") else LazyColumn {
        (0..6).forEach { d -> val list = sched.filter { it.airDay == d }.sortedBy { it.airTime }
            if (list.isNotEmpty()) { item { Text(DAYS[d].uppercase(), Modifier.padding(vertical = 8.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(list, key = { it.id }) { a -> Card { Row(verticalAlignment = Alignment.CenterVertically) {
                    PosterImage(a.poster, a.title, Modifier.width(48.dp).aspectRatio(2f / 3f), 10.dp)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) { Text(a.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis); Text(a.airTime!!) }
                    Switch(a.id in on, { v -> scope.launch {
                        if (v) { if (Build.VERSION.SDK_INT >= 33) ask.launch(Manifest.permission.POST_NOTIFICATIONS); dao.addRemind(Remind(a.id)); ReminderWorker.schedule(ctx, a) }
                        else { dao.delRemind(a.id); ReminderWorker.cancel(ctx, a.id) } } })
                } } } } } } }
}

@UnstableApi @Composable
fun MyListTab(onPlay: (String, Int) -> Unit) {
    val ctx = LocalContext.current; val dao = remember { AppDb.get(ctx).lists() }; val scope = rememberCoroutineScope()
    val ids by dao.myList().collectAsState(emptyList()); val all by remember { Repo.of(ctx).all }.collectAsState(emptyList())
    val list = all.filter { it.id in ids }
    Page("My List") { if (list.isEmpty()) Empty("Daftar kosong. Tekan + di poster pada Beranda.") else LazyColumn { items(list, key = { it.id }) { a -> Card { Row {
        PosterImage(a.poster, a.title, Modifier.width(64.dp).aspectRatio(2f / 3f), 10.dp)
        Column(Modifier.padding(start = 14.dp)) {
            Text(a.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("${a.eps} episode", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button({ onPlay(a.id, 1) }) { Text("Play") }; OutlinedButton({ scope.launch { dao.del(a.id) } }) { Text("Remove") } } } } } } } }
}

@UnstableApi @Composable
fun DownloadTab(onPlay: (String, Int) -> Unit) {
    val ctx = LocalContext.current; var list by remember { mutableStateOf<List<Download>>(emptyList()) }
    val titles by remember { Repo.of(ctx).all }.collectAsState(emptyList())
    LaunchedEffect(Unit) { while (true) { list = withContext(Dispatchers.IO) { MediaKit.list(ctx) }; delay(1000) } }
    Page("Download") { if (list.isEmpty()) Empty("Belum ada unduhan.") else LazyColumn { items(list, key = { it.request.id }) { d ->
        val (id, ep) = d.request.id.split(":").let { it[0] to it[1].toInt() }; val done = d.state == Download.STATE_COMPLETED
        val anime = titles.firstOrNull { it.id == id }
        Card { Row {
            PosterImage(anime?.poster, anime?.title ?: id, Modifier.width(56.dp).aspectRatio(2f / 3f), 10.dp)
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text("${anime?.title ?: id} · Episode $ep", style = MaterialTheme.typography.titleMedium)
                Text(when (d.state) { Download.STATE_COMPLETED -> "Selesai"; Download.STATE_FAILED -> "Gagal"; Download.STATE_QUEUED -> "Menunggu (cek Wi-Fi only)"
                    else -> "${d.percentDownloaded.toInt().coerceAtLeast(0)}%" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (d.state == Download.STATE_DOWNLOADING) LinearProgressIndicator(progress = { d.percentDownloaded.coerceIn(0f, 100f) / 100f }, Modifier.fillMaxWidth().padding(top = 8.dp))
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (done) Button({ onPlay(id, ep) }) { Text("Play") }
                    OutlinedButton({ MediaKit.remove(ctx, d.request.id) }) { Text("Hapus") } } } } } } } }
}
