package com.onimeku.app.ui

import androidx.compose.foundation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onimeku.app.AppVM
import com.onimeku.app.Route
import com.onimeku.app.data.Anime
import com.onimeku.app.data.Catalog
import java.time.LocalDate
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale

// ------------------------------------------------------------------ HOME
private val SectionGap = 26.dp

/** Satu baris poster bergulir horizontal dengan judul bagian + "See all". */
@Composable
private fun PosterRow(title: String, ids: List<String>, vm: AppVM, seeAll: String?, ranked: Boolean = false, delayMs: Int = 0) {
    val items = ids.mapNotNull { Catalog.byId[it] }
    if (items.isEmpty()) return
    Column(Modifier.fadeSlideIn(delayMs)) {
        Spacer(Modifier.height(SectionGap))
        SectionHead(title, seeAll?.let { key -> { vm.go(Route.SeeAll(key)) } })
        Rail {
            itemsIndexed(items, key = { _, a -> a.id }) { i, a ->
                PosterCard(a, railW, rank = if (ranked) i + 1 else null) { vm.go(Route.Detail(a.id)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: AppVM) {
    val p = vm.p
    val heroH = heroHeight()
    if (Catalog.all.isEmpty()) {
        if (Catalog.loading) {
            HomeSkeleton(heroH, railW, bottomContentPadding())
        } else {
            Page(extraTop = 64.dp) {
                RootTitle("Home")
                EmptyBox("Catalog unavailable", "The catalog could not be loaded. Check your connection, then try again.") {
                    Btn("Try again", BtnStyle.Play) { vm.reloadCatalog() }
                }
            }
        }
        return
    }
    PullToRefreshBox(isRefreshing = vm.refreshing, onRefresh = { vm.refresh() }, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(vm.homeScroll).padding(bottom = bottomContentPadding())) {
            Hero(vm, heroH)

            val last = p.last
            val t = last?.let { p.prog["${it.id}:${it.ep}"] }
            val lastAnime = last?.let { Catalog.byId[it.id] }
            if (last != null && t != null && lastAnime != null) {
                Column(Modifier.fadeSlideIn(0)) {
                    Spacer(Modifier.height(SectionGap))
                    SectionHead("Continue Watching")
                    Rail {
                        item {
                            Poster(lastAnime, railW, ep = true, meta = lastAnime.title to "Episode ${last.ep} · ${fmt(t)}") {
                                vm.go(Route.Player(last.id, last.ep))
                            }
                        }
                    }
                }
            }

            // Jeda bertahap (maks. 4 langkah) supaya baris muncul berurutan, bukan serentak.
            var n = 0
            fun d() = minOf(n++, 4) * 70

            val base = p.base?.let { Catalog.byId[it] }
            if (base != null) {
                PosterRow("Because you watched ${base.title}", Catalog.similar(base.id).map { it.id }, vm, seeAll = null, delayMs = d())
            }

            PosterRow("Top Hits Anime", Catalog.top, vm, seeAll = "top", ranked = true, delayMs = d())

            val fresh = Catalog.new
            if (fresh.isNotEmpty()) {
                Column(Modifier.fadeSlideIn(d())) {
                    Spacer(Modifier.height(SectionGap))
                    SectionHead("New Episode Releases") { vm.go(Route.SeeAll("new")) }
                    Rail {
                        items(fresh, key = { it.first }) { (id, ep, whenTxt) ->
                            Catalog.byId[id]?.let { a ->
                                Poster(a, railW, ep = true, meta = "Episode $ep" to whenTxt) { vm.go(Route.Detail(id)) }
                            }
                        }
                    }
                }
            }

            PosterRow("Airing Now", Catalog.airing.take(12), vm, seeAll = "airing", delayMs = d())
            PosterRow("Popular", Catalog.popular.take(12), vm, seeAll = "popular", delayMs = d())
            PosterRow("Latest Releases", Catalog.latest.take(12), vm, seeAll = "latest", delayMs = d())
            PosterRow("Donghua", Catalog.donghua.take(12), vm, seeAll = "donghua", delayMs = d())
            Catalog.topGenres.forEach { g -> PosterRow(g, Catalog.byGenre(g).take(12), vm, seeAll = "genre:$g", delayMs = d()) }

            // Pintu masuk ke SELURUH katalog (grid lazy, tanpa batas 12 judul).
            Spacer(Modifier.height(SectionGap))
            SectionHead("All Anime") { vm.go(Route.SeeAll("all")) }
            Box(Modifier.padding(horizontal = 16.dp)) {
                Btn("Browse all ${Catalog.all.size} anime", BtnStyle.Solid, Icons.Filled.GridView,
                    Modifier.fillMaxWidth()) { vm.go(Route.SeeAll("all")) }
            }
        }
    }
}

/** Tinggi hero mengikuti layar (±64%), bukan angka tetap. */
@Composable
private fun heroHeight() = (LocalConfiguration.current.screenHeightDp * 0.64f).coerceIn(420f, 640f).dp

/** Carousel hero: 5 judul teratas, geser manual atau otomatis tiap 5,5 dtk, indikator animasi, parallax saat digulir. */
@Composable
private fun Hero(vm: AppVM, heroH: androidx.compose.ui.unit.Dp) {
    val items = Catalog.top.take(5).mapNotNull { Catalog.byId[it] }.ifEmpty { Catalog.all.take(1) }
    if (items.isEmpty()) return
    val pager = rememberPagerState { items.size }
    LaunchedEffect(pager, items.size) {
        if (items.size > 1) while (true) {
            delay(5500)
            if (!pager.isScrollInProgress) pager.animateScrollToPage((pager.currentPage + 1) % items.size)
        }
    }
    Box(Modifier.fillMaxWidth().height(heroH)) {
        HorizontalPager(pager, Modifier.fillMaxSize()) { i -> HeroPage(vm, items[i.coerceIn(0, items.lastIndex)]) }
        if (items.size > 1) {
            Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items.indices.forEach { i ->
                    val sel = pager.currentPage == i
                    val w by animateDpAsState(if (sel) 22.dp else 6.dp, label = "dotW")
                    Box(Modifier.height(6.dp).width(w).clip(CircleShape).background(if (sel) Red else Color(0x66FFFFFF)))
                }
            }
        }
    }
}

@Composable
private fun HeroPage(vm: AppVM, a: Anime) {
    val on = a.id in vm.p.myList
    val firstEp = a.available.firstOrNull() ?: 1
    Box(Modifier.fillMaxSize().artBackground(a).clickable { vm.go(Route.Detail(a.id)) }) {
        val req = rememberImg(com.onimeku.app.data.PosterHealer.posterFor(a))
        if (req == null) Box(Modifier.fillMaxSize().shimmer())
        else coil.compose.AsyncImage(
            model = req, contentDescription = a.title,
            // Parallax: gambar bergeser 35% dari jarak scroll.
            modifier = Modifier.fillMaxSize().graphicsLayer { translationY = vm.homeScroll.value * 0.35f },
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            alignment = Alignment.TopCenter,
        )
        Box(Modifier.fillMaxSize().drawBehind {
            drawRect(Brush.verticalGradient(0f to Color(0x8014161F), 0.28f to Color.Transparent, 0.68f to Color(0xC014161F), 1f to Bg))
        })
        Column(Modifier.align(Alignment.BottomStart).padding(start = 16.dp, end = 16.dp, bottom = 40.dp)) {
            Text(a.title, fontSize = 30.sp, fontWeight = FontWeight.SemiBold, lineHeight = 36.sp,
                maxLines = 3, overflow = TextOverflow.Ellipsis)
            Text(a.genres.take(4).joinToString(", "), Modifier.padding(top = 10.dp), fontSize = 14.sp,
                color = Color(0xFFF1E8EE), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Btn("Play", BtnStyle.Play, Icons.Filled.PlayCircleFilled) { vm.go(Route.Player(a.id, firstEp)) }
                Btn(if (on) "Added" else "My List", if (on) BtnStyle.OutlineOn else BtnStyle.Outline,
                    if (on) Icons.Filled.Check else Icons.Filled.Add) { vm.toggleML(a.id) }
            }
        }
    }
}

// ------------------------------------------------------------------ CALENDAR
@Composable
fun CalendarScreen(vm: AppVM) {
    val ensure = LocalEnsureNotif.current
    val days = remember { (0..6).map { LocalDate.now().plusDays(it.toLong()) } }
    val dow = days[vm.calDay].dayOfWeek.value % 7
    val list = Catalog.all.filter { it.day == dow }.sortedBy { it.time }
    // Jadwal ASLI dari API bila sudah termuat: hanya judul dengan episode
    // berikutnya yang benar-benar terjadwal pada tanggal terpilih.
    val sched = vm.schedule
    val realList = if (sched.isNotEmpty()) {
        val sel = days[vm.calDay]
        Catalog.all.mapNotNull { a ->
            val s = sched[a.id] ?: return@mapNotNull null
            val dt = java.time.Instant.ofEpochSecond(s.airingAt).atZone(java.time.ZoneId.systemDefault())
            if (dt.toLocalDate() == sel) Triple(a, s, dt) else null
        }.sortedBy { it.second.airingAt }
    } else null
    Page {
        RootTitle("Release Calendar", "Episodes airing this week.")
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            days.forEachIndexed { k, d ->
                val sel = k == vm.calDay
                Column(
                    Modifier.width(56.dp).clip(RoundedCornerShape(16.dp)).background(if (sel) Red else Bg2)
                        .border(1.dp, if (sel) Red else Line, RoundedCornerShape(16.dp))
                        .clickable { vm.calDay = k }.padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(if (k == 0) "Today" else d.dayOfWeek.getDisplayName(DateTextStyle.SHORT, Locale.ENGLISH),
                        fontSize = 12.sp, color = if (sel) Color.White else NavOff)
                    Text("${d.dayOfMonth}", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        if (realList != null) {
            if (realList.isEmpty()) EmptyBox("Nothing airing", "No new episodes are scheduled for this day.")
            else realList.forEach { (a, s, dt) ->
                val on = a.id in vm.p.remind
                ItemRow(
                    a, thumbLabel = dt.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")), title = a.title,
                    sub = "Episode ${s.ep} · ${a.genres.firstOrNull() ?: "Anime"}",
                    onOpen = { vm.go(Route.Detail(a.id)) },
                ) { Chip(if (on) "Reminder on" else "Remind me", on) { vm.toggleRemind(a.id, ensure) } }
            }
        } else if (list.isEmpty()) EmptyBox("Nothing airing", "No new episodes are scheduled for this day.")
        else list.forEach { a ->
            val on = a.id in vm.p.remind
            ItemRow(
                a, thumbLabel = a.time, title = a.title, sub = "Episode ${a.ep} · ${a.genres.firstOrNull() ?: "Anime"}",
                onOpen = { vm.go(Route.Detail(a.id)) },
            ) { Chip(if (on) "Reminder on" else "Remind me", on) { vm.toggleRemind(a.id, ensure) } }
        }
    }
}

@Composable
fun ItemRow(
    a: Anime, thumbLabel: String, title: String, sub: String, subOk: Boolean = false,
    onThumb: (() -> Unit)? = null, onOpen: () -> Unit, trailing: @Composable () -> Unit,
) {
    Column {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.width(96.dp).aspectRatio(16f / 10f).clip(RoundedCornerShape(12.dp)).artBackground(a)
                .clickable(onClick = onThumb ?: onOpen)) {
                RealImage(a, Modifier.fillMaxSize())
                Text(thumbLabel, Modifier.align(Alignment.BottomStart).fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xD90A0B10))))
                    .padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            Column(Modifier.weight(1f).clickable(onClick = onOpen)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(sub, fontSize = 12.sp, color = if (subOk) RedText else NavOff, modifier = Modifier.padding(top = 2.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            trailing()
        }
        HLine()
    }
}

// ------------------------------------------------------------------ MY LIST / DOWNLOADS
@Composable
fun MyListScreen(vm: AppVM) {
    val l = vm.p.myList.mapNotNull { Catalog.byId[it] }
    AnimeGrid(l, vm) {
        RootTitle("My List")
        if (l.isEmpty()) EmptyBox("Your list is empty", "Tap My List on any anime to save it here.") { Btn("Browse anime", BtnStyle.Play) { vm.setTab(Route.Home) } }
        else Text("${l.size} saved", color = InkSoft, modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))
    }
}

@Composable
fun DownloadsScreen(vm: AppVM) = Page {
    RootTitle("Download")
    val d = vm.s.downloads
    if (d.isEmpty()) EmptyBox("No downloads yet", "Open an anime and tap the download icon on an episode to watch it offline.") { Btn("Browse anime", BtnStyle.Play) { vm.setTab(Route.Home) } }
    else {
        Text("${d.size} episode${if (d.size > 1) "s" else ""}", color = InkSoft, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
        Box(Modifier.padding(bottom = 12.dp)) { Chip("Delete all", icon = Icons.Outlined.Delete) { vm.removeAllDl() } }
        d.forEach { x ->
            val a = Catalog.byId[x.id] ?: return@forEach
            val pct = vm.downloading["${x.id}:${x.ep}"]
            Column {
                ItemRow(a, "Ep ${x.ep}", a.title,
                    if (pct != null) "Downloading… $pct%" else "Episode ${x.ep} · Saved offline",
                    onOpen = { vm.go(Route.Player(x.id, x.ep)) }) {
                    IconChip(Icons.Outlined.Delete, "Remove download") { vm.removeDl(x.id, x.ep) }
                }
                if (pct != null) {
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { pct / 100f },
                        modifier = Modifier.fillMaxWidth().padding(start = 108.dp, end = 12.dp, bottom = 8.dp),
                        color = Red, trackColor = Line,
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------ DETAIL
@Composable
fun DetailScreen(vm: AppVM, id: String) {
    val a = Catalog.byId[id] ?: return
    val on = id in vm.p.myList
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    var expanded by rememberSaveable(id) { mutableStateOf(false) }
    val epNums = if (a.available.isNotEmpty()) a.available else (1..a.eps).toList()
    val sim = Catalog.similar(id)
    // LazyColumn: judul panjang (ratusan episode) tidak lagi digambar sekaligus.
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = top + 16.dp, bottom = bottomContentPadding()),
    ) {
        item {
            SubHead("Details", vm)
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(18.dp)).artBackground(a)) {
                RealImage(a, Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().drawBehind { drawRect(Brush.verticalGradient(0.5f to Color.Transparent, 1f to Color(0xB314161F))) })
            }
            Spacer(Modifier.height(16.dp))
            Text(a.title, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, lineHeight = 31.sp)
            Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                a.rating?.let { Tag(it, Color.White, Bg) }
                if (a.hd) Tag("HD", Red, Color.White)
                Text(listOfNotNull(a.year.takeIf { it > 0 }?.toString(), "${a.eps} eps", a.status).joinToString(" · "),
                    fontSize = 13.sp, color = InkSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (a.genres.isNotEmpty()) {
                Text(a.genres.joinToString(", "), Modifier.padding(top = 6.dp), fontSize = 13.sp, color = NavOff)
            }
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Btn("Play", BtnStyle.Play, Icons.Filled.PlayArrow, Modifier.weight(1f)) { vm.go(Route.Player(id, a.available.firstOrNull() ?: 1)) }
                Btn(if (on) "Added" else "My List", if (on) BtnStyle.OutlineOn else BtnStyle.Outline,
                    if (on) Icons.Filled.Check else Icons.Filled.Add, Modifier.weight(1f)) { vm.toggleML(id) }
            }
            Box(Modifier.padding(top = 14.dp)) { Chip("Download all", icon = Icons.Outlined.Download) { vm.downloadAll(id) } }
            Text(a.synopsis, Modifier.padding(top = 18.dp), color = InkSoft, fontSize = 15.sp, lineHeight = 22.sp,
                maxLines = if (expanded) Int.MAX_VALUE else 4, overflow = TextOverflow.Ellipsis)
            if (a.synopsis.length > 180) {
                Text(if (expanded) "Show less" else "Read more", Modifier.padding(top = 6.dp).clickable { expanded = !expanded },
                    color = RedText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
            if (sim.isNotEmpty()) {
                Text("More like this", Modifier.padding(top = 26.dp, bottom = 12.dp), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(sim, key = { it.id }) { x -> PosterCard(x, railW) { vm.go(Route.Detail(x.id)) } }
                }
            }
            Text("Episodes", Modifier.padding(top = 26.dp, bottom = 4.dp), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            if (epNums.isEmpty()) Text("No episodes available yet.", color = NavOff, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
        }
        items(epNums, key = { it }) { n ->
            val k = "$id:$n"
            val seen = k in vm.p.seen
            val prog = vm.p.prog[k]
            val pct = vm.downloading[k]
            val dl = vm.hasDl(id, n)
            ItemRow(
                a, "$n", "Episode $n",
                if (seen) "Watched" else (prog?.let { "Resume at ${fmt(it)}" } ?: ""), subOk = seen,
                onOpen = { vm.go(Route.Player(id, n)) },
            ) {
                when {
                    pct != null -> Chip("$pct%", minW = 52.dp) { vm.download(id, n) }
                    dl -> IconChip(Icons.Filled.Check, "Saved offline", on = true) { vm.download(id, n) }
                    else -> IconChip(Icons.Outlined.Download, "Download episode $n") { vm.download(id, n) }
                }
            }
        }
    }
}

@Composable
fun SeeAllScreen(vm: AppVM, key: String) {
    val (title, ids) = when {
        key == "top" -> "Top Hits Anime" to Catalog.popular // semua judul urut skor, bukan hanya 10 teratas
        key == "all" -> "All Anime" to Catalog.alphabetical
        key == "airing" -> "Airing Now" to Catalog.airing
        key == "popular" -> "Popular" to Catalog.popular
        key == "latest" -> "Latest Releases" to Catalog.latest
        key == "donghua" -> "Donghua" to Catalog.donghua
        key.startsWith("genre:") -> key.removePrefix("genre:") to Catalog.byGenre(key.removePrefix("genre:"))
        else -> "New Episode Releases" to Catalog.new.map { it.first }
    }
    val items = ids.mapNotNull { Catalog.byId[it] }
    AnimeGrid(items, vm) {
        SubHead(title, vm)
        Text("${items.size} anime", color = NavOff, fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
    }
}

// ------------------------------------------------------------------ NOTIFICATIONS
@Composable
fun NotifScreen(vm: AppVM) = Page {
    SubHead("Notifications", vm)
    if (!vm.s.settings.alerts) {
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(14.dp)).background(Bg2)
            .border(1.dp, Line, RoundedCornerShape(14.dp)).padding(12.dp)) {
            Text("New episode alerts are off. ", fontSize = 13.sp, color = InkSoft)
            Text("Turn on in Settings", fontSize = 13.sp, color = RedText, modifier = Modifier.clickable { vm.openSettings() })
        }
    }
    Catalog.notifs.forEach { (id, ep, whenTxt) ->
        val a = Catalog.byId[id] ?: return@forEach
        Column {
            Row(Modifier.fillMaxWidth().clickable { vm.go(Route.Detail(id)) }.padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(9.dp).clip(CircleShape).background(if (vm.s.notifRead) Color.Transparent else Red))
                Text(androidx.compose.ui.text.buildAnnotatedString {
                    pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold)); append(a.title); pop()
                    append(" episode $ep is out")
                }, Modifier.weight(1f), fontSize = 14.sp)
                Text(whenTxt, fontSize = 12.sp, color = NavOff)
            }
            HLine()
        }
    }
    if (!vm.s.notifRead) Box(Modifier.padding(top = 16.dp)) { Btn("Mark all as read", BtnStyle.Solid) { vm.markAllRead() } }
}

// ------------------------------------------------------------------ SEARCH
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(vm: AppVM) {
    var q by rememberSaveable { mutableStateOf("") }
    // Debounce 500 ms: hasil pencarian mengikuti teks yang sudah berhenti
    // berubah, bukan setiap ketukan.
    var appliedQ by remember { mutableStateOf("") }
    LaunchedEffect(q) { kotlinx.coroutines.delay(500); appliedQ = q }
    var gs by remember { mutableStateOf(listOf<String>()) }
    var yr by remember { mutableStateOf("") }
    var st by remember { mutableStateOf("") }
    val fr = remember { FocusRequester() }
    LaunchedEffect(Unit) { fr.requestFocus() }
    val any = gs.isNotEmpty() || yr.isNotEmpty() || st.isNotEmpty()

    @Composable fun group(label: String, content: @Composable RowScope.() -> Unit) {
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, Modifier.width(52.dp), fontSize = 12.sp, color = NavOff)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), content = content)
        }
    }

    val query = appliedQ.trim().lowercase()
    val results = remember(query, gs, yr, st, Catalog.all) {
        if (query.isEmpty() && !any) emptyList()
        else Catalog.all.filter { c ->
            (query.isEmpty() || c.title.lowercase().contains(query) || c.genres.joinToString(" ").lowercase().contains(query)) &&
                gs.all { it in c.genres } && (yr.isEmpty() || c.year.toString() == yr) && (st.isEmpty() || c.status == st)
        }
    }

    // Hasil memakai grid lazy: mengetik di katalog besar tidak lagi menggambar ratusan kartu sekaligus.
    AnimeGrid(results, vm) {
        SubHead("Search", vm)
        Row(Modifier.fillMaxWidth().height(48.dp).clip(CircleShape).background(Bg2).border(1.dp, Line, CircleShape).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Outlined.Search, null, Modifier.size(20.dp), tint = NavOff)
            BasicTextField(q, { q = it }, Modifier.weight(1f).focusRequester(fr), singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 16.sp), cursorBrush = SolidColor(Color.White),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { vm.addHist(q) }),
                decorationBox = { inner -> if (q.isEmpty()) Text("Search anime or genre", color = NavOff); inner() })
        }
        group("Genre") { Catalog.chips.forEach { g -> Chip(g, g in gs) { gs = if (g in gs) gs - g else gs + g } } }
        group("Status") { Catalog.statuses.forEach { v -> Chip(v, st == v) { st = if (st == v) "" else v } } }
        group("Year") {
            Catalog.years.forEach { v -> Chip(v, yr == v) { yr = if (yr == v) "" else v } }
            if (any) Chip("Clear") { gs = emptyList(); yr = ""; st = "" }
        }
        Spacer(Modifier.height(8.dp))

        if (query.isEmpty() && !any) {
            if (vm.s.hist.isEmpty()) Text("Search a title, or pick filters above", Modifier.padding(top = 10.dp), color = NavOff, fontSize = 13.sp)
            else {
                Text("Recent searches", Modifier.padding(top = 10.dp), color = NavOff, fontSize = 13.sp)
                FlowRow(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    vm.s.hist.forEach { h -> Chip(h) { q = h } }
                    Chip("Clear history") { vm.clearHist() }
                }
            }
        } else if (results.isEmpty()) {
            EmptyBox("No results", "Nothing matches. Try fewer filters or check the spelling.")
        } else {
            Text("${results.size} result${if (results.size > 1) "s" else ""}", Modifier.padding(bottom = 2.dp), color = NavOff, fontSize = 13.sp)
        }
    }
}
