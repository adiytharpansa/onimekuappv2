package com.onimeku.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
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
import com.onimeku.app.I18n
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
    Column(if (delayMs >= 0) Modifier.fadeSlideIn(delayMs) else Modifier) {
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
                RootTitle(I18n.t("home", "Home"))
                EmptyBox(I18n.t("catalog_unavail_t", "Catalog unavailable"), I18n.t("catalog_unavail_b", "The catalog could not be loaded. Check your connection, then try again.")) {
                    Btn("Try again", BtnStyle.Play) { vm.reloadCatalog() }
                }
            }
        }
        return
    }
    // Tarik-untuk-segar BUATAN SENDIRI (debug.31): riwayatnya,
    // PullToRefreshBox material3 1.3 membuat Home crash di emulator
    // (build .17) sehingga fitur ini dimatikan. Implementasi sekarang
    // tidak memakai komponen perpustakaan: koneksi gulir bersarang
    // mengakumulasi delta tarikan saat daftar di paling atas; lewat
    // ambang 90dp -> vm.refresh(). Risiko crash jalur lama tidak ada.
    // LazyColumn: hanya baris yang terlihat (+ prefetch) yang dikomposisi. Dulu seluruh baris
    // (belasan rail genre) digambar sekaligus saat Home dibuka.
    val base = p.base?.let { Catalog.byId[it] }
    val similar = remember(base?.id, Catalog.all) { base?.let { b -> Catalog.similar(b.id).map { it.id } }.orEmpty() }
    val last = p.last
    val lastT = last?.let { p.prog["${it.id}:${it.ep}"] }
    val lastAnime = last?.let { Catalog.byId[it.id] }
    val fresh = Catalog.new
    var pullPx by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val ptrThresholdPx = with(density) { 90.dp.toPx() }
    val ptrConn = remember {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): Offset {
                val atTop = vm.homeScroll.firstVisibleItemIndex == 0 && vm.homeScroll.firstVisibleItemScrollOffset == 0
                if (source == androidx.compose.ui.input.nestedscroll.NestedScrollSource.UserInput && atTop && available.y > 0f && !vm.refreshing) {
                    pullPx = (pullPx + available.y * 0.5f).coerceAtMost(340f)
                    return Offset(0f, available.y)
                }
                if (pullPx > 0f && available.y < 0f) {
                    val used = minOf(pullPx, -available.y)
                    pullPx -= used
                    return Offset(0f, -used)
                }
                return Offset.Zero
            }
            override suspend fun onPreFling(available: androidx.compose.ui.unit.Velocity): androidx.compose.ui.unit.Velocity {
                if (pullPx > ptrThresholdPx) vm.refresh()
                pullPx = 0f
                return super.onPreFling(available)
            }
        }
    }
    Box(Modifier.fillMaxSize()) {
    LazyColumn(Modifier.fillMaxSize().nestedScroll(ptrConn).graphicsLayer { translationY = pullPx }, state = vm.homeScroll, contentPadding = PaddingValues(bottom = bottomContentPadding())) {
        item(key = "hero", contentType = "hero") { Hero(vm, heroH) }

        if (last != null && lastT != null && lastAnime != null) {
            item(key = "continue", contentType = "rail") {
                Column(Modifier.fadeSlideIn(0)) {
                    Spacer(Modifier.height(SectionGap))
                    SectionHead(I18n.t("sec_continue", "Continue Watching"))
                    Rail {
                        item {
                            Poster(lastAnime, railW, ep = true, meta = lastAnime.title to "Episode ${last.ep} · ${fmt(lastT)}") {
                                vm.go(Route.Player(last.id, last.ep))
                            }
                        }
                    }
                }
            }
        }
        if (base != null && similar.isNotEmpty()) {
            item(key = "because", contentType = "rail") { PosterRow("${I18n.t("sec_because", "Because you watched")} ${base.title}", similar, vm, seeAll = null, delayMs = 0) }
        }
        item(key = "top", contentType = "rail") { PosterRow(I18n.t("sec_top", "Top Hits Anime"), Catalog.top, vm, seeAll = "top", ranked = true, delayMs = 0) }
        if (fresh.isNotEmpty()) {
            item(key = "new", contentType = "rail") {
                Column(Modifier.fadeSlideIn(70)) {
                    Spacer(Modifier.height(SectionGap))
                    SectionHead(I18n.t("sec_new_eps", "New Episode Releases")) { vm.go(Route.SeeAll("new")) }
                    Rail {
                        items(fresh, key = { it.first }) { (id, ep, whenTxt) ->
                            Catalog.byId[id]?.let { a ->
                                // Judul anime di bawah kartu (dulu kartu hanya bertuliskan "Episode N" tanpa nama anime).
                                Column(Modifier.width(railW)) {
                                    Poster(a, railW, ep = true, meta = "Episode $ep" to whenTxt) { vm.go(Route.Detail(id)) }
                                    Text(a.title, Modifier.padding(top = 8.dp), fontSize = 13.sp, fontWeight = FontWeight.Medium, lineHeight = 17.sp,
                                        minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }
        // delayMs = -1: tanpa animasi masuk (baris di bawah layar tidak mengulang animasi saat digulir kembali).
        item(key = "airing", contentType = "rail") { PosterRow(I18n.t("sec_airing", "Airing Now"), Catalog.airing.take(12), vm, seeAll = "airing", delayMs = 140) }
        item(key = "popular", contentType = "rail") { PosterRow(I18n.t("sec_popular", "Popular"), Catalog.popular.take(12), vm, seeAll = "popular", delayMs = -1) }
        item(key = "latest", contentType = "rail") { PosterRow("Latest Releases", Catalog.latest.take(12), vm, seeAll = "latest", delayMs = -1) }
        item(key = "donghua", contentType = "rail") { PosterRow(I18n.t("sec_donghua", "Donghua"), Catalog.donghua.take(12), vm, seeAll = "donghua", delayMs = -1) }
        items(Catalog.topGenres, key = { "genre:$it" }, contentType = { "rail" }) { g ->
            PosterRow(g, Catalog.byGenre(g).take(12), vm, seeAll = "genre:$g", delayMs = -1)
        }
        // Pintu masuk ke SELURUH katalog (grid lazy, tanpa batas 12 judul).
        item(key = "all", contentType = "all") {
            Column {
                Spacer(Modifier.height(SectionGap))
                SectionHead(I18n.t("sec_all", "All Anime")) { vm.go(Route.SeeAll("all")) }
                Box(Modifier.padding(horizontal = 16.dp)) {
                    Btn("${I18n.t("browse_all", "Browse all")} ${Catalog.all.size} anime", BtnStyle.Solid, Icons.Filled.GridView, Modifier.fillMaxWidth()) { vm.go(Route.SeeAll("all")) }
                }
            }
        }
    }
    if (pullPx > 6f || vm.refreshing) {
        Box(Modifier.align(Alignment.TopCenter).padding(top = 76.dp).size(38.dp).clip(CircleShape).background(Bg2).border(1.dp, Line, CircleShape), contentAlignment = Alignment.Center) {
            if (vm.refreshing) androidx.compose.material3.CircularProgressIndicator(Modifier.size(20.dp), color = Red, strokeWidth = 2.5.dp)
            else androidx.compose.material3.CircularProgressIndicator(progress = { (pullPx / ptrThresholdPx).coerceIn(0f, 1f) }, Modifier.size(20.dp), color = Red, strokeWidth = 2.5.dp)
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
    // clipToBounds: gambar parallax digeser ke bawah saat digulir; tanpa klip ia bocor ke bagian di bawah hero.
    Box(Modifier.fillMaxWidth().height(heroH).clipToBounds()) {
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
            modifier = Modifier.fillMaxSize().graphicsLayer { translationY = vm.homeScrollPx().coerceAtMost(4000) * 0.35f },
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            alignment = Alignment.TopCenter,
        )
        Box(Modifier.fillMaxSize().drawBehind {
            drawRect(Brush.verticalGradient(0f to Color(0x8014161F), 0.28f to Color.Transparent, 0.68f to Color(0xC014161F), 1f to Bg))
        })
        Column(Modifier.align(Alignment.BottomStart).padding(start = 16.dp, end = 16.dp, bottom = 40.dp)) {
            Text(a.title, fontSize = 30.sp, fontWeight = FontWeight.Bold, lineHeight = 35.sp, letterSpacing = (-0.4).sp,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            val meta = listOfNotNull(
                a.year.takeIf { it > 0 }?.toString(),
                a.eps.takeIf { it > 0 }?.let { "$it ${if (it == 1) "episode" else "episodes"}" },
                a.status,
            ).joinToString("  ·  ")
            Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                a.rating?.let { Text(it, Modifier.padding(end = 10.dp), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Gold) }
                Text(meta, fontSize = 14.sp, color = Color(0xFFE3E5EE), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(a.genres.take(4).joinToString(", "), Modifier.padding(top = 4.dp), fontSize = 14.sp,
                color = Color(0xFFB9BDCB), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Btn(I18n.t("play", "Play"), BtnStyle.Play, Icons.Filled.PlayCircleFilled) { vm.go(Route.Player(a.id, firstEp)) }
                Btn(if (on) I18n.t("added", "Added") else I18n.t("my_list", "My List"), if (on) BtnStyle.OutlineOn else BtnStyle.Outline,
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
    val list = remember(dow, Catalog.all) { Catalog.all.filter { it.day == dow }.sortedBy { it.time } }
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
        RootTitle(I18n.t("calendar_t", "Release Calendar"), I18n.t("calendar_sub", "Episodes airing this week."))
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            days.forEachIndexed { k, d ->
                val sel = k == vm.calDay
                val chipBg by androidx.compose.animation.animateColorAsState(if (sel) Red else Bg2, tween(220), label = "dayBg")
                val chipBd by androidx.compose.animation.animateColorAsState(if (sel) Red else Line, tween(220), label = "dayBd")
                Column(
                    Modifier.width(56.dp).pressable { vm.calDay = k }.clip(RoundedCornerShape(16.dp)).background(chipBg)
                        .border(1.dp, chipBd, RoundedCornerShape(16.dp)).padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(if (k == 0) "Today" else d.dayOfWeek.getDisplayName(DateTextStyle.SHORT, Locale.ENGLISH),
                        fontSize = 12.sp, color = if (sel) Color.White else NavOff)
                    Text("${d.dayOfMonth}", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        if (Catalog.all.isEmpty()) {
            SkeletonRows(5)
        } else key(vm.calDay) { Column(Modifier.fadeSlideIn()) {
        if (realList != null) {
            if (realList.isEmpty()) EmptyBox(I18n.t("nothing_airing_t", "Nothing airing"), I18n.t("nothing_airing_b", "No new episodes are scheduled for this day."))
            else realList.forEach { (a, s, dt) ->
                val on = a.id in vm.p.remind
                ItemRow(
                    a, thumbLabel = dt.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")), title = a.title,
                    sub = "Episode ${s.ep} · ${a.genres.firstOrNull() ?: "Anime"}",
                    onOpen = { vm.go(Route.Detail(a.id)) },
                ) { Chip(if (on) I18n.t("reminder_on", "Reminder on") else I18n.t("remind_me", "Remind me"), on) { vm.toggleRemind(a.id, ensure) } }
            }
        } else if (list.isEmpty()) EmptyBox(I18n.t("nothing_airing_t", "Nothing airing"), I18n.t("nothing_airing_b", "No new episodes are scheduled for this day."))
        else list.forEach { a ->
            val on = a.id in vm.p.remind
            ItemRow(
                a, thumbLabel = a.time, title = a.title, sub = "Episode ${a.ep} · ${a.genres.firstOrNull() ?: "Anime"}",
                onOpen = { vm.go(Route.Detail(a.id)) },
            ) { Chip(if (on) I18n.t("reminder_on", "Reminder on") else I18n.t("remind_me", "Remind me"), on) { vm.toggleRemind(a.id, ensure) } }
        }
        } }
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
        RootTitle(I18n.t("tab_mylist", "My List"))
        if (l.isEmpty()) EmptyBox(I18n.t("mylist_empty_t", "Your list is empty"), I18n.t("mylist_empty_b", "Tap My List on any anime to save it here.")) { Btn(I18n.t("browse_anime", "Browse anime"), BtnStyle.Play) { vm.setTab(Route.Home) } }
        else Text("${l.size} saved", color = InkSoft, modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))
    }
}

@Composable
fun DownloadsScreen(vm: AppVM) = Page {
    RootTitle(I18n.t("tab_download", "Download"))
    val d = vm.s.downloads
    if (d.isEmpty()) EmptyBox(I18n.t("dl_none_t", "No downloads yet"), I18n.t("dl_none_b", "Open an anime and tap the download icon on an episode to watch it offline.")) { Btn(I18n.t("browse_anime", "Browse anime"), BtnStyle.Play) { vm.setTab(Route.Home) } }
    else {
        Text("${d.size} episode${if (d.size > 1) "s" else ""}", color = InkSoft, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
        Box(Modifier.padding(bottom = 12.dp)) { Chip("Delete all", icon = Icons.Outlined.Delete) { vm.removeAllDl() } }
        d.forEachIndexed { idx, x ->
            val a = Catalog.byId[x.id] ?: return@forEachIndexed
            val pct = vm.downloading["${x.id}:${x.ep}"]
            Column(Modifier.fadeSlideIn(minOf(idx, 5) * 50)) {
                ItemRow(a, "Ep ${x.ep}", a.title,
                    if (pct != null) "Downloading… $pct%" else "Episode ${x.ep} · Saved offline",
                    onOpen = { vm.go(Route.Player(x.id, x.ep)) }) {
                    IconChip(Icons.Outlined.Delete, "Remove download") { vm.removeDl(x.id, x.ep) }
                }
                if (pct != null) {
                    val animPct by animateFloatAsState(pct / 100f, tween(300), label = "dlBar")
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { animPct },
                        modifier = Modifier.fillMaxWidth().padding(start = 108.dp, end = 12.dp, bottom = 8.dp),
                        color = Red, trackColor = Line,
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------ DETAIL
/** Cincin progres unduhan; ketuk untuk membatalkan. */
@Composable
private fun DownloadRing(pct: Int, onClick: () -> Unit) {
    val prog by animateFloatAsState(pct / 100f, tween(300), label = "dlProg")
    Box(Modifier.size(width = 40.dp, height = 36.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        androidx.compose.material3.CircularProgressIndicator(
            progress = { prog }, modifier = Modifier.size(22.dp), color = Red, trackColor = Line, strokeWidth = 2.5.dp,
        )
        Icon(Icons.Filled.Close, "Cancel download", Modifier.size(10.dp), tint = Color.White)
    }
}

// ---- Komponen khusus halaman Detail (redesain debug.33) ----
val Gutter = 20.dp
private val GoodGreen = Color(0xFF3DDC97)

/** Tombol bulat kaca (blur-look) yang melayang di atas gambar hero. */
@Composable
fun GlassButton(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit) {
    Box(
        Modifier.size(42.dp).clip(CircleShape).background(Color(0x66080910))
            .border(1.dp, Color(0x33FFFFFF), CircleShape).pressable(onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, Modifier.size(22.dp), tint = Color.White) }
}

/** Skor ber-bintang emas di atas kaca gelap. */
@Composable
private fun ScorePill(score: String) {
    Row(
        Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0x33FFC857)).padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(Icons.Filled.Star, null, Modifier.size(13.dp), tint = Gold)
        Text(score, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Gold)
    }
}

@Composable
private fun StatusPill(status: String) {
    val live = status.equals("Airing", ignoreCase = true)
    Row(
        Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0x26FFFFFF)).padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(if (live) GoodGreen else NavOff))
        Text(status, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}

/** Tombol utama: gradasi crimson, tinggi 56dp, bayangan lembut. */
@Composable
private fun PlayButton(label: String, sub: String?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).pressable(onClick).clip(RoundedCornerShape(18.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFFC8142F), Red)))
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(Color(0x33FFFFFF)), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.PlayArrow, null, Modifier.size(22.dp), tint = Color.White)
        }
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
            if (sub != null) Text(sub, fontSize = 12.sp, color = Color(0xCCFFFFFF), maxLines = 1)
        }
        Icon(Icons.Filled.ChevronRight, null, tint = Color(0xB3FFFFFF))
    }
}

/** Aksi sekunder: ikon di atas, label di bawah, tiga sama lebar. */
@Composable
private fun ActionTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, modifier: Modifier = Modifier,
    on: Boolean = false, onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier.height(68.dp).pressable(onClick).clip(shape).background(if (on) Color(0x26A6122D) else Bg2)
            .border(1.dp, if (on) Red.copy(alpha = 0.7f) else Line, shape),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, Modifier.size(22.dp), tint = if (on) RedText else Color.White)
        Text(label, Modifier.padding(top = 5.dp), fontSize = 12.sp, fontWeight = FontWeight.Medium,
            color = if (on) RedText else InkSoft, maxLines = 1)
    }
}

@Composable
fun SectionTitle(text: String, trailing: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = Gutter), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(4.dp).height(18.dp).clip(CircleShape).background(Red))
        Text(text, Modifier.weight(1f).padding(start = 10.dp), fontSize = 19.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp)
        trailing?.invoke()
    }
}

/** Satu baris episode: thumbnail + progres tonton + status + aksi unduh. */
@Composable
fun EpisodeCard(
    a: Anime, n: Int, seen: Boolean, progSec: Int?, current: Boolean,
    onOpen: () -> Unit, trailing: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    val frac = when { seen -> 1f; progSec != null -> (progSec / 1440f).coerceIn(0.03f, 0.97f); else -> 0f }
    Row(
        Modifier.padding(horizontal = Gutter).padding(bottom = 10.dp).fillMaxWidth().pressable(onOpen).clip(shape)
            .background(if (current) Color(0x1FA6122D) else Bg2)
            .border(1.dp, if (current) Red.copy(alpha = 0.6f) else Line, shape).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.width(118.dp).aspectRatio(16f / 10f).clip(RoundedCornerShape(11.dp)).artBackground(a)) {
            RealImage(a, Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Color(0x40000000)))
            Box(Modifier.align(Alignment.Center).size(30.dp).clip(CircleShape).background(Color(0x99000000)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.PlayArrow, null, Modifier.size(20.dp), tint = Color.White)
            }
            Text("EP $n", Modifier.align(Alignment.TopStart).padding(6.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xB3000000))
                .padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
            if (frac > 0f) Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(3.dp).background(Color(0x59FFFFFF))) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(frac).background(Red))
            }
        }
        Column(Modifier.weight(1f)) {
            Text("Episode $n", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
                color = if (seen) Color(0xB3FFFFFF) else Color.White)
            val (txt, col) = when {
                current -> "Now watching" to RedText
                seen -> "Watched" to GoodGreen
                progSec != null -> "Resume at ${fmt(progSec)}" to RedText
                else -> "Tap to play" to NavOff
            }
            Row(Modifier.padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (seen && !current) Icon(Icons.Filled.CheckCircle, null, Modifier.size(13.dp), tint = col)
                Text(txt, fontSize = 12.sp, color = col, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        trailing()
    }
}

@Composable
fun DetailScreen(vm: AppVM, id: String) {
    val a = Catalog.byId[id] ?: return
    val on = id in vm.p.myList
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    var expanded by rememberSaveable(id) { mutableStateOf(false) }
    var newestFirst by rememberSaveable(id) { mutableStateOf(false) }
    val baseEps = if (a.available.isNotEmpty()) a.available else (1..a.eps).toList()
    val epNums = if (newestFirst) baseEps.reversed() else baseEps
    val sim = Catalog.similar(id)
    val resume = vm.p.last?.takeIf { it.id == id }
    var commentText by remember(id) { mutableStateOf("") }
    LaunchedEffect(id) { if (com.onimeku.app.data.Comments.enabled) vm.loadComments(id) }
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val heroH = (LocalConfiguration.current.screenHeightDp * 0.5f).coerceIn(320f, 470f).dp
    val heroPx = with(density) { heroH.toPx() }
    // 0 = hero terlihat penuh, 1 = hero sudah tergulung -> bilah atas jadi solid + judul muncul.
    val barA by remember(heroPx) {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else ((listState.firstVisibleItemScrollOffset - heroPx * 0.45f) / (heroPx * 0.3f)).coerceIn(0f, 1f)
        }
    }
    val ctx = androidx.compose.ui.platform.LocalContext.current
    fun share() {
        val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, "Watch ${a.title} on Onimeku: onimeku://anime/$id")
        }
        ctx.startActivity(android.content.Intent.createChooser(send, I18n.t("share", "Share")).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    Box(Modifier.fillMaxSize()) {
        // LazyColumn: judul panjang (ratusan episode) tidak digambar sekaligus.
        LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = bottomContentPadding())) {
            item(key = "hero") {
                // ===== HERO: gambar penuh lebar, parallax, judul menempel di bawahnya =====
                Box(Modifier.fillMaxWidth().height(heroH).artBackground(a)) {
                    RealImage(a, Modifier.fillMaxSize().graphicsLayer {
                        val off = if (listState.firstVisibleItemIndex == 0) listState.firstVisibleItemScrollOffset else Int.MAX_VALUE
                        translationY = minOf(off * 0.45f, size.height * 0.12f)
                        scaleX = 1.15f; scaleY = 1.15f
                    })
                    // scrim atas (ikon terbaca) + peleburan ke latar di bawah
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                        0f to Color(0x99080910), 0.28f to Color.Transparent, 0.5f to Color(0x2214161F), 0.82f to Color(0xE614161F), 1f to Bg)))
                    Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = Gutter)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            a.rating?.let { ScorePill(it) }
                            if (a.hd) Tag("HD", Red, Color.White)
                            StatusPill(a.status)
                        }
                        Text(a.title, Modifier.padding(top = 10.dp), fontSize = 28.sp, fontWeight = FontWeight.Bold,
                            lineHeight = 33.sp, letterSpacing = (-0.5).sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        Text(listOfNotNull(a.year.takeIf { it > 0 }?.toString(), "${a.eps} eps", a.genres.firstOrNull()).joinToString("  ·  "),
                            Modifier.padding(top = 6.dp), fontSize = 13.sp, color = InkSoft, maxLines = 1)
                    }
                }
            }
            item(key = "actions") {
                Column(Modifier.padding(horizontal = Gutter).fadeSlideIn(60)) {
                    Spacer(Modifier.height(16.dp))
                    PlayButton(
                        label = if (resume != null) "${I18n.t("resume", "Resume Ep")} ${resume.ep}" else I18n.t("play", "Play"),
                        sub = resume?.let { r -> vm.p.prog["$id:${r.ep}"]?.let { "Continue from ${fmt(it)}" } }
                            ?: if (resume == null) "Start with Episode ${a.available.firstOrNull() ?: 1}" else null,
                    ) { vm.go(Route.Player(id, resume?.ep ?: a.available.firstOrNull() ?: 1)) }
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ActionTile(if (on) Icons.Filled.Check else Icons.Filled.Add, if (on) I18n.t("added", "Added") else I18n.t("my_list", "My List"),
                            Modifier.weight(1f), on = on) { vm.toggleML(id) }
                        ActionTile(Icons.Outlined.Download, I18n.t("download_all", "Download all"), Modifier.weight(1f)) { vm.downloadAll(id) }
                        ActionTile(Icons.Outlined.Share, I18n.t("share", "Share"), Modifier.weight(1f)) { share() }
                    }
                }
            }
            if (a.genres.isNotEmpty()) item(key = "genres") {
                Row(Modifier.padding(top = 16.dp).horizontalScroll(rememberScrollState()).padding(horizontal = Gutter).fadeSlideIn(110),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    a.genres.forEach { g ->
                        Text(g, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = InkSoft, maxLines = 1,
                            modifier = Modifier.clip(CircleShape).background(Color(0x14FFFFFF)).padding(horizontal = 13.dp, vertical = 6.dp))
                    }
                }
            }
            item(key = "synopsis") {
                Column(Modifier.padding(horizontal = Gutter).padding(top = 20.dp).fadeSlideIn(160)) {
                    Text("Synopsis", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Box(Modifier.padding(top = 8.dp).animateContentSize()) {
                        Text(a.synopsis, color = InkSoft, fontSize = 14.5.sp, lineHeight = 22.sp,
                            maxLines = if (expanded) Int.MAX_VALUE else 4, overflow = TextOverflow.Ellipsis)
                    }
                    if (a.synopsis.length > 180) {
                        Row(Modifier.padding(top = 6.dp).clip(CircleShape).clickable { expanded = !expanded }.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(if (expanded) I18n.t("read_less", "Show less") else I18n.t("read_more", "Read more"),
                                color = RedText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, Modifier.size(18.dp), tint = RedText)
                        }
                    }
                }
            }
            // ===== Episode lebih dulu: ini alasan orang membuka halaman ini =====
            item(key = "ep_head") {
                Spacer(Modifier.height(26.dp))
                SectionTitle(I18n.t("episodes", "Episodes")) {
                    Text("${epNums.size}", fontSize = 13.sp, color = NavOff, modifier = Modifier.padding(end = 10.dp))
                    if (epNums.size > 1) Row(
                        Modifier.clip(CircleShape).background(Bg2).border(1.dp, Line, CircleShape)
                            .clickable { newestFirst = !newestFirst }.padding(horizontal = 11.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(Icons.Filled.SwapVert, null, Modifier.size(15.dp), tint = InkSoft)
                        Text(if (newestFirst) "Newest" else "Oldest", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = InkSoft)
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (epNums.isEmpty()) Text("No episodes available yet.", Modifier.padding(horizontal = Gutter), color = NavOff, fontSize = 14.sp)
            }
            items(epNums, key = { "ep$it" }) { n ->
                val k = "$id:$n"
                val pct = vm.downloading[k]
                val dl = vm.hasDl(id, n)
                EpisodeCard(
                    a, n, seen = k in vm.p.seen, progSec = vm.p.prog[k], current = resume?.ep == n,
                    onOpen = { vm.go(Route.Player(id, n)) },
                ) {
                    when {
                        pct != null -> DownloadRing(pct) { vm.download(id, n) }
                        dl -> IconChip(Icons.Filled.Check, "Saved offline", on = true) { vm.download(id, n) }
                        else -> IconChip(Icons.Outlined.Download, "Download episode $n") { vm.download(id, n) }
                    }
                }
            }
            if (sim.isNotEmpty()) {
                item(key = "similar") {
                    Spacer(Modifier.height(22.dp))
                    SectionTitle("More like this")
                    Spacer(Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = Gutter)) {
                        items(sim, key = { it.id }) { x -> PosterCard(x, railW) { vm.go(Route.Detail(x.id)) } }
                    }
                }
            }
            // ==== Komentar (debug.31): backend Supabase (tabel comments).
            // Tamu bisa membaca; menulis wajib masuk. Hanya dirender bila backend akun aktif. ====
            if (com.onimeku.app.data.Comments.enabled) {
                item(key = "comments_head") {
                    Column(Modifier.padding(top = 28.dp)) {
                        SectionTitle("${I18n.t("comments", "Comments")} (${vm.comments.size})")
                        if (vm.s.account != null) {
                            Row(Modifier.padding(horizontal = Gutter).padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(Bg2).border(1.dp, Line, RoundedCornerShape(16.dp))
                                    .padding(horizontal = 14.dp, vertical = 13.dp)) {
                                    androidx.compose.foundation.text.BasicTextField(
                                        commentText, { if (it.length <= 1000) commentText = it },
                                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp),
                                        cursorBrush = androidx.compose.ui.graphics.SolidColor(Red),
                                        decorationBox = { inner -> if (commentText.isEmpty()) Text(I18n.t("comment_hint", "Write a comment…"), color = NavOff, fontSize = 14.sp); inner() },
                                    )
                                }
                                Btn(I18n.t("comment_post", "Post"), BtnStyle.Play, height = 46.dp) {
                                    vm.postComment(id, commentText); commentText = ""
                                }
                            }
                        } else {
                            Box(Modifier.padding(horizontal = Gutter).padding(top = 14.dp)) {
                                Chip(I18n.t("sign_in_to_comment", "Sign in to comment")) { vm.go(Route.Auth(false)) }
                            }
                        }
                    }
                }
                if (vm.commentsBusy && vm.comments.isEmpty()) {
                    item(key = "comments_busy") { Text("…", color = NavOff, modifier = Modifier.padding(horizontal = Gutter, vertical = 16.dp)) }
                } else if (vm.comments.isEmpty()) {
                    item(key = "comments_empty") {
                        Text(I18n.t("comment_empty", "No comments yet. Be the first."), color = NavOff, fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = Gutter, vertical = 16.dp))
                    }
                }
                items(vm.comments, key = { "comment:${it.id}" }) { c ->
                    val mine = c.user_id == vm.s.account?.userId
                    Row(Modifier.padding(horizontal = Gutter).padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(36.dp).clip(CircleShape).background(if (mine) Red else Solid), contentAlignment = Alignment.Center) {
                            Text(c.author.ifBlank { "U" }.take(1).uppercase(), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(c.author.ifBlank { "User" }, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (mine) RedText else Color.White)
                                Spacer(Modifier.weight(1f))
                                Text(if (c.created_at.length >= 16) c.created_at.substring(0, 10) + " " + c.created_at.substring(11, 16) else "", fontSize = 11.sp, color = NavOff)
                            }
                            Text(c.body, fontSize = 14.sp, color = InkSoft, lineHeight = 20.sp, modifier = Modifier.padding(top = 3.dp))
                            if (mine) Text(I18n.t("comment_delete", "Delete"), fontSize = 12.sp, color = NavOff,
                                modifier = Modifier.padding(top = 6.dp).clickable { vm.deleteComment(id, c.id) })
                        }
                    }
                }
            }
        }
        // ===== Bilah atas melayang: kaca di atas hero, jadi solid + judul saat digulir =====
        Box(Modifier.fillMaxWidth().height(statusTop + 58.dp).background(Bg.copy(alpha = barA * 0.97f)).padding(top = statusTop)) {
            Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassButton(Icons.AutoMirrored.Filled.ArrowBack, "Back") { vm.back() }
                Text(a.title, Modifier.weight(1f).graphicsLayer { alpha = barA }, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                GlassButton(if (on) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder, if (on) "Remove from My List" else "Add to My List") { vm.toggleML(id) }
            }
        }
    }
}

@Composable
fun SeeAllScreen(vm: AppVM, key: String) {
    val (title, ids) = when {
        key == "top" -> I18n.t("sec_top", "Top Hits Anime") to Catalog.popular // semua judul urut skor, bukan hanya 10 teratas
        key == "all" -> I18n.t("sec_all", "All Anime") to Catalog.alphabetical
        key == "airing" -> I18n.t("sec_airing", "Airing Now") to Catalog.airing
        key == "popular" -> I18n.t("sec_popular", "Popular") to Catalog.popular
        key == "latest" -> "Latest Releases" to Catalog.latest
        key == "donghua" -> I18n.t("sec_donghua", "Donghua") to Catalog.donghua
        key.startsWith("genre:") -> key.removePrefix("genre:") to Catalog.byGenre(key.removePrefix("genre:"))
        else -> I18n.t("sec_new_eps", "New Episode Releases") to Catalog.new.map { it.first }
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
    if (Catalog.all.isEmpty()) SkeletonRows(4)
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
        SubHead(I18n.t("search", "Search"), vm)
        Row(Modifier.fillMaxWidth().height(48.dp).clip(CircleShape).background(Bg2).border(1.dp, Line, CircleShape).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Outlined.Search, null, Modifier.size(20.dp), tint = NavOff)
            BasicTextField(q, { q = it }, Modifier.weight(1f).focusRequester(fr), singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 16.sp), cursorBrush = SolidColor(Color.White),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { vm.addHist(q) }),
                decorationBox = { inner -> if (q.isEmpty()) Text(I18n.t("search_hint2", "Search anime or genre"), color = NavOff); inner() })
        }
        group("Genre") { Catalog.chips.forEach { g -> Chip(g, g in gs) { gs = if (g in gs) gs - g else gs + g } } }
        group("Status") { Catalog.statuses.forEach { v -> Chip(v, st == v) { st = if (st == v) "" else v } } }
        group("Year") {
            Catalog.years.forEach { v -> Chip(v, yr == v) { yr = if (yr == v) "" else v } }
            if (any) Chip("Clear") { gs = emptyList(); yr = ""; st = "" }
        }
        Spacer(Modifier.height(8.dp))
        if (q.trim().lowercase() != query) {
            androidx.compose.material3.LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp), color = Red, trackColor = Color.Transparent)
        }

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
            EmptyBox(I18n.t("no_results_t", "No results"), I18n.t("no_results_b", "Nothing matches. Try fewer filters or check the spelling."))
        } else {
            Text("${results.size} result${if (results.size > 1) "s" else ""}", Modifier.padding(bottom = 2.dp), color = NavOff, fontSize = 13.sp)
        }
    }
}
