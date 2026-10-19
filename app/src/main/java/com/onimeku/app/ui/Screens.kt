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
private fun PosterRow(title: String, ids: List<String>, vm: AppVM, seeAll: String?, ranked: Boolean = false, delayMs: Int = 0, subtitle: String? = null) {
    val items = ids.mapNotNull { Catalog.byId[it] }
    if (items.isEmpty()) return
    Column(if (delayMs >= 0) Modifier.fadeSlideIn(delayMs) else Modifier) {
        Spacer(Modifier.height(SectionGap))
        SectionHead(title, seeAll?.let { key -> { vm.go(Route.SeeAll(key)) } }, subtitle = subtitle)
        Rail {
            itemsIndexed(items, key = { _, a -> a.id }) { i, a ->
                PosterCard(a, railW, rank = if (ranked) i + 1 else null, sharedKey = "row:$title:${a.id}", index = i) { vm.go(Route.Detail(a.id)) }
            }
        }
    }
}

@Composable
fun HomeScreen(vm: AppVM) {
    // Skeleton memudar halus menjadi konten saat katalog tiba (bukan muncul tiba-tiba).
    val loading = Catalog.all.isEmpty() && Catalog.loading
    androidx.compose.animation.Crossfade(targetState = loading, animationSpec = androidx.compose.animation.core.tween(380), label = "homeLoad") { l ->
        if (l) HomeSkeleton(heroHeight(), railW, bottomContentPadding()) else HomeContent(vm)
    }
}

/** Satu kartu Continue Watching: anime, episode untuk dilanjutkan, posisi (detik), durasi episode (detik, 0 = tak diketahui). */
private data class ContItem(val a: Anime, val ep: Int, val pos: Int, val dur: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(vm: AppVM) {
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
    // Continue Watching = anime yang BENAR-BENAR sudah ditonton (perintah
    // user 19 Okt 2026), terbaru dulu, realtime dari catatan tontonan +
    // progres tersimpan. Yang tampil hanya judul dengan progres aktif
    // (sudah diputar >=5 dtk, belum tuntas) atau episode lanjutan tepat
    // sesudah episode yang dituntaskan. Progres warisan (belum ada di
    // catatan tontonan) ikut di belakang agar tak ada yang hilang.
    val contItems = remember(p.watch, p.prog, p.seen, Catalog.all) {
        val out = mutableListOf<ContItem>()
        val covered = mutableSetOf<String>()
        fun consider(id: String, ep: Int, durHint: Int) {
            if (id in covered) return
            val a = Catalog.byId[id] ?: return
            val pos = p.prog["$id:$ep"]
            when {
                pos != null && pos >= 5 -> { out += ContItem(a, ep, pos, durHint); covered += id }
                "$id:$ep" in p.seen && ep + 1 <= a.ep -> { out += ContItem(a, ep + 1, 0, 0); covered += id }
            }
        }
        p.watch.forEach { w -> consider(w.id, w.ep, w.dur) }
        p.prog.keys.forEach { k ->
            consider(k.substringBeforeLast(':'), k.substringAfterLast(':').toIntOrNull() ?: return@forEach, 0)
        }
        out.take(12)
    }
    val fresh = Catalog.new
    val allRows = remember(Catalog.all) { Catalog.alphabetical.mapNotNull { Catalog.byId[it] }.chunked(3) }
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
    // Tarik-untuk-segar (perintah user 19 Okt 2026: "saat refresh poster atas
    // jangan terpotong / ada hitam di atas, poster harus full"). Daftar TIDAK
    // lagi digeser turun (itu yang membuka celah hitam di atas hero). Poster
    // hero tetap menempel penuh ke tepi atas layar; saat ditarik ia hanya
    // membesar halus (efek stretch) dan indikator putar merah melayang di atas
    // poster, di bawah status bar. Saat menyegarkan, indikator tetap tampil
    // sampai muat selesai.
    val stretch by androidx.compose.animation.core.animateFloatAsState(
        pullPx, androidx.compose.animation.core.tween(if (pullPx == 0f) 280 else 0), label = "ptrStretch",
    )
    Box(Modifier.fillMaxSize()) {
    LazyColumn(
        Modifier.fillMaxSize().nestedScroll(ptrConn),
        state = vm.homeScroll, contentPadding = PaddingValues(bottom = bottomContentPadding()),
    ) {
        item(key = "hero", contentType = "hero") { Hero(vm, heroH) { stretch } }

        if (contItems.isNotEmpty()) {
            item(key = "continue", contentType = "rail") {
                Column(Modifier.fadeSlideIn(0)) {
                    Spacer(Modifier.height(SectionGap))
                    SectionHead(I18n.t("sec_continue", "Continue Watching"))
                    val contW = (LocalConfiguration.current.screenWidthDp * 0.58f).coerceIn(210f, 300f).dp
                    Rail {
                        items(contItems, key = { it.a.id }) { c ->
                            Poster(
                                c.a, contW, ep = true,
                                meta = c.a.title to ("Episode ${c.ep}" + if (c.pos > 0) " · ${fmt(c.pos)}" else ""),
                                progress = when {
                                    c.pos > 0 && c.dur > 0 -> (c.pos.toFloat() / c.dur).coerceIn(0.03f, 0.97f)
                                    c.pos > 0 -> (c.pos / 1440f).coerceIn(0.03f, 0.97f)
                                    else -> null
                                },
                            ) { vm.go(Route.Player(c.a.id, c.ep)) }
                        }
                    }
                }
            }
        }
        if (base != null && similar.isNotEmpty()) {
            item(key = "because", contentType = "rail") { PosterRow(I18n.t("sec_because", "Because you watched"), similar, vm, seeAll = null, delayMs = 0, subtitle = base.title) }
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
                                    // debug.33: ukuran poster potret SAMA dengan rail lain (dulu kartu lanskap 232:150).
                                    Poster(a, railW, badges = true, meta = "Episode $ep" to whenTxt) { vm.go(Route.Detail(id)) }
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
        item(key = "latest", contentType = "rail") { PosterRow(I18n.t("sec_latest", "Latest Releases"), Catalog.latest.take(12), vm, seeAll = "latest", delayMs = -1) }
        item(key = "donghua", contentType = "rail") { PosterRow(I18n.t("sec_donghua", "Donghua"), Catalog.donghua.take(12), vm, seeAll = "donghua", delayMs = -1) }
        // All Anime: seluruh katalog langsung tampil sebagai grid 3 kolom (dipecah per baris agar tetap lazy).
        item(key = "all-head", contentType = "all-head") {
            Column {
                Spacer(Modifier.height(SectionGap))
                SectionHead(I18n.t("sec_all", "All Anime"))
            }
        }
        items(allRows.size, key = { "all-row:$it" }, contentType = { "all-row" }) { r ->
            val row = allRows[r]
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEachIndexed { c, a -> Box(Modifier.weight(1f)) { Tile(a, vm, (r * 3 + c) % 9) } }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
    // Indikator tarik-untuk-segar: kapsul gelap kaca dengan putaran merah, melayang di atas poster.
    if (vm.refreshing || stretch > 4f) {
        val frac = (stretch / ptrThresholdPx).coerceIn(0f, 1f)
        val k = if (vm.refreshing) 1f else frac
        Box(
            Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 12.dp)
                .graphicsLayer {
                    alpha = k
                    scaleX = 0.6f + 0.4f * k; scaleY = 0.6f + 0.4f * k
                    translationY = (k - 1f) * 28.dp.toPx()
                }
                .size(42.dp).clip(CircleShape).background(Color(0xE614161F)).border(1.dp, Line, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (vm.refreshing) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(22.dp), color = RedText, trackColor = Color.Transparent, strokeWidth = 2.5.dp,
                )
            } else {
                androidx.compose.material3.CircularProgressIndicator(
                    progress = { frac }, modifier = Modifier.size(22.dp), color = RedText, trackColor = Color.Transparent, strokeWidth = 2.5.dp,
                )
            }
        }
    }
    }
}

/** Tinggi hero mengikuti layar (±64%), bukan angka tetap. */
@Composable
private fun heroHeight() = (LocalConfiguration.current.screenHeightDp * 0.64f).coerceIn(420f, 640f).dp

/** Carousel hero: 5 judul teratas, geser manual atau otomatis tiap 5,5 dtk, indikator animasi, parallax saat digulir. */
@Composable
private fun Hero(vm: AppVM, heroH: androidx.compose.ui.unit.Dp, pull: () -> Float = { 0f }) {
    // Hero selalu anime TERBARU secara realtime (perintah user 19 Okt 2026):
    // urut updatedAt API terbaru, bukan skor tertinggi — judul lama tamat
    // tidak lagi mendominasi Beranda. Kualitas hero (perintah user 19 Okt
    // 2026 "poster paling atas harus HD"): dari 30 terbaru, yang punya
    // BANNER (karya lebar, resolusi tertinggi) didahulukan menurut
    // kebaruan, lalu yang hanya punya poster. Syarat minimum: punya poster.
    val pool = Catalog.freshest.mapNotNull { Catalog.byId[it] }.filter { it.poster != null }.take(30)
    val items = (pool.filter { !it.banner.isNullOrBlank() } + pool.filter { it.banner.isNullOrBlank() })
        .take(5).ifEmpty { Catalog.all.take(1) }
    if (items.isEmpty()) return
    val pager = rememberPagerState { items.size }
    // Dikunci pada currentPage: geser manual otomatis mengulang hitung mundur 5,5 dtk,
    // persis sama dengan animasi isi titik (HeroDot) yang juga mulai ulang tiap halaman berganti.
    LaunchedEffect(pager.currentPage, items.size) {
        if (items.size > 1) {
            delay(5500)
            while (pager.isScrollInProgress) delay(100)
            pager.animateScrollToPage((pager.currentPage + 1) % items.size)
        }
    }
    // clipToBounds: gambar parallax digeser ke bawah saat digulir; tanpa klip ia bocor ke bagian di bawah hero.
    Box(Modifier.fillMaxWidth().height(heroH).clipToBounds().background(Bg)) {
        // Stretch saat ditarik HANYA pada gambar (di HeroPage); teks & tombol tidak ikut membesar/terpotong.
        HorizontalPager(pager, Modifier.fillMaxSize()) { i ->
            HeroPage(vm, items[i.coerceIn(0, items.lastIndex)], pull) { (pager.currentPage - i) + pager.currentPageOffsetFraction }
        }
        // Scrim tipis di atas supaya status bar & lonceng tetap terbaca di poster terang.
        Box(Modifier.fillMaxWidth().height(90.dp).drawBehind { drawRect(Brush.verticalGradient(listOf(Color(0x40000000), Color.Transparent))) })
        if (items.size > 1) {
            // Titik indikator: tepat di atas tombol "+", rata tengah dengannya (kotak selebar tombol; baris titik boleh lebih lebar, dipusatkan).
            Box(Modifier.align(Alignment.BottomEnd).padding(end = HeroPad, bottom = HeroBottomPad + HeroActionW + HeroDotsGap).width(HeroActionW)) {
                Row(Modifier.wrapContentWidth(Alignment.CenterHorizontally, unbounded = true), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items.indices.forEach { i ->
                        HeroDot(selected = pager.currentPage == i, page = pager.currentPage)
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroPage(vm: AppVM, a: Anime, pull: () -> Float, pageOffset: () -> Float) {
    val on = a.id in vm.p.myList
    val firstEp = a.available.firstOrNull() ?: 1
    Box(Modifier.fillMaxSize().artBackground(a).clickable { SharedArt.key = "hero:${a.id}"; vm.go(Route.Detail(a.id)) }) {
        val req = rememberImg(com.onimeku.app.data.PosterHealer.posterFor(a))
        if (req == null) Box(Modifier.fillMaxSize().shimmer())
        else coil.compose.AsyncImage(
            model = req, contentDescription = a.title,
            // Parallax: gambar bergeser 35% dari jarak scroll.
            modifier = Modifier.fillMaxSize().sharedArt("hero:${a.id}").graphicsLayer {
                // Gambar sedikit diperbesar (HeroOverscan) sebagai ruang parallax, jadi tepinya tidak
                // pernah membuka celah gelap saat digeser. Stretch tarik-segar juga hanya di sini.
                val stretch = 1f + (pull() / size.height).coerceIn(0f, 0.3f) * 0.6f
                val z = HeroOverscan * stretch
                scaleX = z; scaleY = z
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f)
                translationY = vm.homeScrollPx().coerceAtMost(4000) * 0.35f
                translationX = -pageOffset() * size.width * HeroParallax
            }.kenBurns(),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            alignment = Alignment.TopCenter,
        )
        Box(Modifier.fillMaxSize().drawBehind {
            drawRect(Brush.verticalGradient(0f to Color.Transparent, 0.26f to Color.Transparent, 0.52f to Color(0x3314161F), 0.8f to Color(0xE614161F), 1f to Bg))
        })
        val dens = LocalDensity.current
        // Batasi font scale sistem di hero agar kolom teks tidak melebihi tinggi hero.
        androidx.compose.runtime.CompositionLocalProvider(
            LocalDensity provides androidx.compose.ui.unit.Density(dens.density, fontScale = dens.fontScale.coerceAtMost(1.15f)),
        ) {
        Column(Modifier.align(Alignment.BottomStart).graphicsLayer {
            // Halaman sudah digeser pager; di sini cukup memudar (tanpa translationX tambahan yang membuat teks bertumpuk).
            alpha = (1f - kotlin.math.abs(pageOffset()) * 1.6f).coerceIn(0f, 1f)
        }.padding(start = HeroPad, end = HeroPad, bottom = HeroBottomPad)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                a.rating?.let { ScorePill(it) }
                if (a.hd) Tag("HD", Red, Color.White)
                StatusPill(a.status)
            }
            Text(a.title, Modifier.padding(top = 12.dp), fontSize = 32.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp, letterSpacing = (-0.6).sp,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            val meta = listOfNotNull(
                a.year.takeIf { it > 0 }?.toString(),
                a.eps.takeIf { it > 0 }?.let { "$it ${if (it == 1) "episode" else "episodes"}" },
                a.genres.take(2).joinToString(", ").ifBlank { null },
            ).joinToString("  ·  ")
            Text(meta, Modifier.padding(top = 6.dp), fontSize = 13.sp, color = HeroMeta, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (a.synopsis.isNotBlank()) Text(a.synopsis, Modifier.padding(top = 8.dp), fontSize = 13.sp, lineHeight = 19.sp,
                color = HeroSynopsis, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(Modifier.padding(top = HeroActionsTop), horizontalArrangement = Arrangement.spacedBy(HeroActionGap), verticalAlignment = Alignment.CenterVertically) {
                PlayButton(I18n.t("play", "Play"), null, Modifier.weight(1f)) { vm.go(Route.Player(a.id, firstEp)) }
                Box(
                    Modifier.size(HeroActionW).pressable { vm.toggleML(a.id) }.clip(RoundedCornerShape(18.dp))
                        .background(if (on) Color.White else GlassFill)
                        .border(1.dp, if (on) Color.White else GlassStroke, RoundedCornerShape(18.dp)).sparkBurst(on),
                    contentAlignment = Alignment.Center,
                ) { MorphIcon(on, Icons.Filled.Check, Icons.Filled.Add, if (on) "Added" else "My List", 26.dp, if (on) Bg else Color.White) }
            }
        }
        }
    }
}

// ------------------------------------------------------------------ CALENDAR
@Composable
fun CalendarScreen(vm: AppVM) {
    val ensure = LocalEnsureNotif.current
    val days = remember { (0..6).map { LocalDate.now().plusDays(it.toLong()) } }
    // Kalender SELALU berisi (perintah user 19 Okt 2026): API tidak
    // menyediakan jadwal tayang per hari, jadi isi halaman diturunkan
    // dari katalog realtime — judul yang diperbarui pada tanggal
    // terpilih, lalu daftar Sedang Tayang terbaru yang selalu tampil
    // di bawahnya agar halaman tidak pernah kosong.
    val selDate = days[vm.calDay]
    val dayList = remember(selDate, Catalog.all) {
        Catalog.freshest.mapNotNull { Catalog.byId[it] }.filter { a ->
            runCatching {
                java.time.OffsetDateTime.parse(a.updatedAt)
                    .atZoneSameInstant(java.time.ZoneId.systemDefault()).toLocalDate() == selDate
            }.getOrDefault(false)
        }
    }
    val airingList = remember(Catalog.all) { Catalog.airingFresh.mapNotNull { Catalog.byId[it] }.take(40) }
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
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            days.forEachIndexed { k, d ->
                val sel = k == vm.calDay
                val chipBg by androidx.compose.animation.animateColorAsState(if (sel) Red else Bg2, tween(220), label = "dayBg")
                val chipBd by androidx.compose.animation.animateColorAsState(if (sel) Red else Line, tween(220), label = "dayBd")
                Column(
                    Modifier.weight(1f).pressable { vm.calDay = k }.clip(RoundedCornerShape(16.dp)).background(chipBg)
                        .border(1.dp, chipBd, RoundedCornerShape(16.dp)).padding(vertical = 11.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(d.dayOfWeek.getDisplayName(DateTextStyle.SHORT, uiLocale()).uppercase(),
                        fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp, color = if (sel) Color(0xCCFFFFFF) else NavOff)
                    Text("${d.dayOfMonth}", Modifier.padding(top = 3.dp), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Box(Modifier.padding(top = 5.dp).size(5.dp).clip(CircleShape).background(if (k == 0) (if (sel) Color.White else RedText) else Color.Transparent))
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
                ) { IconChip(if (on) Icons.Filled.NotificationsActive else Icons.Outlined.NotificationsNone, if (on) I18n.t("reminder_on", "Reminder on") else I18n.t("remind_me", "Remind me"), on) { vm.toggleRemind(a.id, ensure) } }
            }
        } else {
            if (dayList.isNotEmpty()) {
                SectionHead(I18n.t("sec_new_eps", "New Episode Releases"))
                dayList.forEach { a ->
                    val on = a.id in vm.p.remind
                    ItemRow(
                        a, thumbLabel = a.time, title = a.title, sub = "Episode ${a.ep} · ${a.genres.firstOrNull() ?: "Anime"}",
                        onOpen = { vm.go(Route.Detail(a.id)) },
                    ) { IconChip(if (on) Icons.Filled.NotificationsActive else Icons.Outlined.NotificationsNone, if (on) I18n.t("reminder_on", "Reminder on") else I18n.t("remind_me", "Remind me"), on) { vm.toggleRemind(a.id, ensure) } }
                }
                Spacer(Modifier.height(12.dp))
            }
            SectionHead(I18n.t("sec_airing", "Airing") + " · " + I18n.t("sec_latest", "Latest"))
            if (airingList.isEmpty()) EmptyBox(I18n.t("nothing_airing_t", "Nothing airing"), I18n.t("nothing_airing_b", "No new episodes are scheduled for this day."))
            else airingList.forEach { a ->
                val on = a.id in vm.p.remind
                ItemRow(
                    a, thumbLabel = "", title = a.title, sub = "Episode ${a.ep} · ${a.genres.firstOrNull() ?: "Anime"}",
                    onOpen = { vm.go(Route.Detail(a.id)) },
                ) { IconChip(if (on) Icons.Filled.NotificationsActive else Icons.Outlined.NotificationsNone, if (on) I18n.t("reminder_on", "Reminder on") else I18n.t("remind_me", "Remind me"), on) { vm.toggleRemind(a.id, ensure) } }
            }
        }
        } }
    }
}

@Composable
fun ItemRow(
    a: Anime, thumbLabel: String, title: String, sub: String, subOk: Boolean = false,
    onThumb: (() -> Unit)? = null, onOpen: () -> Unit, nextUp: Boolean = false, trailing: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(shape).background(Bg2).border(1.dp, Line, shape).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.width(108.dp).aspectRatio(16f / 10f).clip(RoundedCornerShape(11.dp)).artBackground(a)
            .clickable(onClick = onThumb ?: onOpen)) {
            RealImage(a, Modifier.fillMaxSize())
            if (thumbLabel.isNotBlank()) Text(thumbLabel, Modifier.align(Alignment.BottomStart).padding(6.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xB3000000))
                .padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f).clickable(onClick = onOpen)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 19.sp)
            Text(sub, fontSize = 12.sp, color = if (subOk) RedText else NavOff, modifier = Modifier.padding(top = 3.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        trailing()
    }
}

// ------------------------------------------------------------------ MY LIST / DOWNLOADS
@Composable
fun MyListScreen(vm: AppVM) {
    val l = vm.p.myList.mapNotNull { Catalog.byId[it] }
    AnimeGrid(l, vm) {
        RootTitle(I18n.t("tab_mylist", "My List"))
        if (l.isEmpty()) EmptyBox(I18n.t("mylist_empty_t", "Your list is empty"), I18n.t("mylist_empty_b", "Tap My List on any anime to save it here.")) { Btn(I18n.t("browse_anime", "Browse anime"), BtnStyle.Play) { vm.setTab(Route.Home) } }
        else Text("${l.size} ${I18n.t("n_saved", "saved")}", color = InkSoft, modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))
    }
}

@Composable
fun DownloadsScreen(vm: AppVM) = Page {
    SubHead(I18n.t("tab_download", "Download"), vm)
    val d = vm.s.downloads
    if (d.isEmpty()) EmptyBox(I18n.t("dl_none_t", "No downloads yet"), I18n.t("dl_none_b", "Open an anime and tap the download icon on an episode to watch it offline.")) { Btn(I18n.t("browse_anime", "Browse anime"), BtnStyle.Play) { vm.setTab(Route.Home) } }
    else {
        Text("${d.size} ${I18n.t("n_episodes_unit", "episode")}${if (d.size > 1 && I18n.lang != "id") "s" else ""}", color = InkSoft, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
        Box(Modifier.padding(bottom = 12.dp)) { Chip(I18n.t("delete_all", "Delete all"), icon = Icons.Outlined.Delete) { vm.removeAllDl() } }
        d.forEachIndexed { idx, x ->
            val a = Catalog.byId[x.id] ?: return@forEachIndexed
            val pct = vm.downloading["${x.id}:${x.ep}"]
            Column(Modifier.fadeSlideIn(minOf(idx, 5) * 50)) {
                ItemRow(a, "Ep ${x.ep}", a.title,
                    if (pct != null) "${I18n.t("downloading", "Downloading…")} $pct%" else "Episode ${x.ep} · ${I18n.t("saved_offline", "Saved offline")}",
                    onOpen = { vm.go(Route.Player(x.id, x.ep)) }) {
                    IconChip(Icons.Outlined.Delete, I18n.t("remove_dl", "Remove download")) { vm.removeDl(x.id, x.ep) }
                }
                if (pct != null) {
                    val animPct by animateFloatAsState(pct / 100f, tween(300), label = "dlBar")
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { animPct },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 12.dp),
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
        Modifier.size(44.dp).clip(CircleShape).background(Color(0x4D000000))
            .border(1.dp, Color(0x40FFFFFF), CircleShape).pressable(onClick),
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
        Text(score.replace("★", "").trim(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Gold)
    }
}

@Composable
private fun StatusPill(status: String) {
    val live = status.equals("Airing", ignoreCase = true)
    Row(
        Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0x26FFFFFF)).padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (live) PulseDot(GoodGreen) else Box(Modifier.size(6.dp).clip(CircleShape).background(NavOff))
        Text(statusText(status), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 1, softWrap = false)
    }
}

/** Tombol utama: gradasi crimson, tinggi 56dp, bayangan lembut. */
@Composable
private fun PlayButton(label: String, sub: String?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier.fillMaxWidth().height(56.dp).pressable(onClick).clip(RoundedCornerShape(18.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFFC8142F), Red)))
            .sweepShine()
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
        Icon(Icons.Filled.ChevronRight, null, Modifier.nudgeX(), tint = Color(0xB3FFFFFF))
    }
}

/** Aksi sekunder: ikon di atas, label di bawah, tiga sama lebar. */
@Composable
private fun ActionTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, modifier: Modifier = Modifier,
    on: Boolean = false, iconOn: androidx.compose.ui.graphics.vector.ImageVector? = null, onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier.height(68.dp).pressable(onClick).clip(shape).background(if (on) Color(0x26A6122D) else Bg2)
            .border(1.dp, if (on) Red.copy(alpha = 0.7f) else Line, shape).sparkBurst(on),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        if (iconOn != null) MorphIcon(on, iconOn, icon, label, 22.dp, if (on) RedText else Color.White)
        else Icon(icon, null, Modifier.size(22.dp), tint = if (on) RedText else Color.White)
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
    onOpen: () -> Unit, nextUp: Boolean = false, trailing: @Composable () -> Unit,
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
            if (frac > 0f) AnimatedProgressBar(frac, Modifier.align(Alignment.BottomStart).fillMaxWidth().height(3.dp), track = Color(0x59FFFFFF))
        }
        Column(Modifier.weight(1f)) {
            Text("Episode $n", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
                color = if (seen) Color(0xB3FFFFFF) else Color.White)
            val (txt, col) = when {
                current -> I18n.t("now_watching", "Now watching") to RedText
                seen -> I18n.t("watched", "Watched") to GoodGreen
                progSec != null -> "${I18n.t("resume_at", "Resume at")} ${fmt(progSec)}" to RedText
                nextUp -> I18n.t("up_next", "Up next") to RedText
                else -> I18n.t("not_watched", "Not watched yet") to NavOff
            }
            Row(Modifier.padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (seen && !current) Icon(Icons.Filled.CheckCircle, null, Modifier.size(13.dp), tint = col)
                Text(txt, fontSize = 12.sp, color = col, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (nextUp && !seen && progSec == null) Text(a.title, Modifier.padding(top = 2.dp), fontSize = 12.sp, color = NavOff, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
    // Katalog daftar berbentuk lite: perkaya judul ini dari endpoint
    // detail (sinopsis penuh + nomor episode asli) sekali per sesi.
    LaunchedEffect(id) { vm.loadDetail(id) }
    // Pemanasan data putar (debug.37): ambil info stream episode sasaran
    // tombol Play (dan episode lanjutan terakhir) SEKARANG, selagi user
    // membaca Detail. Di HP user, resolusi dingin endpoint putar ~5 dtk+
    // di 4G sempat menghabiskan batas tunggu pemutar -> "Stream
    // unavailable" padahal stream-nya hidup. Dengan prefetch, ketukan
    // Play mengenai cache dan video mulai nyaris seketika.
    LaunchedEffect(id) {
        com.onimeku.app.data.ApiClient.prefetchPlayback(id, a.available.firstOrNull() ?: 1)
        resume?.let { if (it.ep != (a.available.firstOrNull() ?: 1)) com.onimeku.app.data.ApiClient.prefetchPlayback(id, it.ep) }
    }
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
                Box(Modifier.fillMaxWidth().height(heroH).clipToBounds().artBackground(a)) {
                    RealImage(a, Modifier.fillMaxSize().sharedArt(SharedArt.key?.takeIf { it.endsWith(":${a.id}") }).graphicsLayer {
                        val off = if (listState.firstVisibleItemIndex == 0) listState.firstVisibleItemScrollOffset else Int.MAX_VALUE
                        // Geser maksimum 7% < overhang skala 1,15 (7,5%): tepi atas gambar tidak pernah membuka celah saat digulir.
                        translationY = minOf(off * 0.45f, size.height * 0.07f)
                        scaleX = 1.15f; scaleY = 1.15f
                    })
                    // scrim atas (ikon terbaca) + peleburan ke latar di bawah
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                        0f to Color(0x99080910), 0.28f to Color.Transparent, 0.5f to Color(0x2214161F), 0.82f to Color(0xE614161F), 1f to Bg)))
                    // Batasi font scale sistem di hero Detail (tinggi hero tetap) agar judul 3 baris tidak keluar dari layar.
                    val dens0 = LocalDensity.current
                    androidx.compose.runtime.CompositionLocalProvider(
                        LocalDensity provides androidx.compose.ui.unit.Density(dens0.density, fontScale = dens0.fontScale.coerceAtMost(1.15f)),
                    ) {
                    Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().graphicsLayer {
                        val off = if (listState.firstVisibleItemIndex == 0) listState.firstVisibleItemScrollOffset.toFloat() else heroPx
                        val f = (off / (heroPx * 0.6f)).coerceIn(0f, 1f)
                        alpha = 1f - f; translationY = off * 0.3f
                        val sc = 1f - 0.06f * f; scaleX = sc; scaleY = sc
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 1f)
                    }.padding(horizontal = Gutter)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            a.rating?.let { ScorePill(it) }
                            if (a.hd) Tag("HD", Red, Color.White)
                            StatusPill(a.status)
                        }
                        Text(a.title, Modifier.padding(top = 10.dp), fontSize = 28.sp, fontWeight = FontWeight.Bold,
                            lineHeight = 33.sp, letterSpacing = (-0.5).sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        Text(listOfNotNull(a.year.takeIf { it > 0 }?.toString(), a.eps.takeIf { it > 0 }?.let { "$it eps" }, a.genres.take(2).joinToString(", ").ifBlank { null }).joinToString("  ·  "),
                            Modifier.padding(top = 6.dp), fontSize = 13.sp, color = InkSoft, maxLines = 1)
                    }
                    }
                }
            }
            item(key = "actions") {
                Column(Modifier.padding(horizontal = Gutter).fadeSlideIn(60)) {
                    Spacer(Modifier.height(16.dp))
                    PlayButton(
                        label = if (resume != null) "${I18n.t("resume", "Resume Ep")} ${resume.ep}" else I18n.t("play", "Play"),
                        sub = resume?.let { r -> vm.p.prog["$id:${r.ep}"]?.let { "${I18n.t("continue_from", "Continue from")} ${fmt(it)}" } }
                            ?: if (resume == null) "${I18n.t("start_with", "Start with Episode")} ${a.available.firstOrNull() ?: 1}" else null,
                    ) { vm.go(Route.Player(id, resume?.ep ?: a.available.firstOrNull() ?: 1)) }
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ActionTile(Icons.Filled.Add, if (on) I18n.t("added", "Added") else I18n.t("my_list", "My List"),
                            Modifier.weight(1f), on = on, iconOn = Icons.Filled.Check) { vm.toggleML(id) }
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
                    Text(I18n.t("synopsis", "Synopsis"), fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
                        Text(if (newestFirst) I18n.t("ep_newest", "Newest") else I18n.t("ep_oldest", "Oldest"), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = InkSoft)
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (epNums.isEmpty()) Text(I18n.t("no_eps", "No episodes available yet."), Modifier.padding(horizontal = Gutter), color = NavOff, fontSize = 14.sp)
            }
            itemsIndexed(epNums, key = { _, n -> "ep$n" }) { idx, n ->
                val k = "$id:$n"
                val pct = vm.downloading[k]
                val dl = vm.hasDl(id, n)
                Box(Modifier.staggerIn("ep:$id:$n", idx)) {
                EpisodeCard(
                    a, n, seen = k in vm.p.seen, progSec = vm.p.prog[k], current = resume?.ep == n,
                    onOpen = { vm.go(Route.Player(id, n)) },
                ) {
                    when {
                        pct != null -> DownloadRing(pct) { vm.download(id, n) }
                        dl -> IconChip(Icons.Filled.Check, I18n.t("saved_offline", "Saved offline"), on = true) { vm.download(id, n) }
                        else -> IconChip(Icons.Outlined.Download, "${I18n.t("download_ep", "Download episode")} $n") { vm.download(id, n) }
                    }
                }
                }
            }
            if (sim.isNotEmpty()) {
                item(key = "similar") {
                    Spacer(Modifier.height(22.dp))
                    SectionTitle(I18n.t("more_like", "More like this"))
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
                                Text(commentTime(c.created_at), fontSize = 11.sp, color = NavOff)
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

/** Waktu komentar dari server (UTC ISO) ditampilkan dalam zona waktu perangkat, mis. "19 Okt 14:05". */
private fun uiLocale(): Locale = if (I18n.lang == "id") Locale.forLanguageTag("id") else Locale.ENGLISH

private fun commentTime(iso: String): String = runCatching {
    java.time.OffsetDateTime.parse(iso).atZoneSameInstant(java.time.ZoneId.systemDefault())
        .format(java.time.format.DateTimeFormatter.ofPattern("d MMM HH:mm", uiLocale()))
}.getOrElse { if (iso.length >= 16) iso.substring(0, 10) + " " + iso.substring(11, 16) else "" }

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
    SubHead(I18n.t("notifications", "Notifications"), vm)
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
    if (!vm.s.notifRead) Box(Modifier.padding(top = 16.dp)) { Btn(I18n.t("mark_all_read", "Mark all as read"), BtnStyle.Solid) { vm.markAllRead() } }
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
        Row(Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(18.dp)).background(Bg2).border(1.dp, if (q.isNotEmpty()) Red.copy(alpha = 0.7f) else Line, RoundedCornerShape(18.dp)).padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Outlined.Search, null, Modifier.size(22.dp), tint = if (q.isNotEmpty()) RedText else NavOff)
            BasicTextField(q, { q = it }, Modifier.weight(1f).focusRequester(fr), singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 16.sp), cursorBrush = SolidColor(Color.White),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { vm.addHist(q) }),
                decorationBox = { inner -> if (q.isEmpty()) Text(I18n.t("search_hint2", "Search anime or genre"), color = NavOff); inner() })
            if (q.isNotEmpty()) Box(Modifier.size(36.dp).clip(CircleShape).clickable { q = "" }, contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Close, "Clear", Modifier.size(18.dp), tint = InkSoft)
            }
        }
        group("Genre") { Catalog.chips.forEach { g -> Chip(g, g in gs) { gs = if (g in gs) gs - g else gs + g } } }
        group("Status") { Catalog.statuses.forEach { v -> Chip(v, st == v) { st = if (st == v) "" else v } } }
        group(I18n.t("f_year", "Year")) {
            Catalog.years.forEach { v -> Chip(v, yr == v) { yr = if (yr == v) "" else v } }
            if (any) Chip(I18n.t("clear", "Clear")) { gs = emptyList(); yr = ""; st = "" }
        }
        Spacer(Modifier.height(8.dp))
        if (q.trim().lowercase() != query) {
            androidx.compose.material3.LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp), color = Red, trackColor = Color.Transparent)
        }

        if (query.isEmpty() && !any) {
            if (vm.s.hist.isEmpty()) EmptyBox(I18n.t("find_next_t", "Find your next anime"), I18n.t("find_next_b", "Search a title or genre, or pick filters above."))
            else {
                Text(I18n.t("recent_searches", "Recent searches"), Modifier.padding(top = 10.dp), color = NavOff, fontSize = 13.sp)
                FlowRow(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    vm.s.hist.forEach { h -> Chip(h) { q = h } }
                    Chip(I18n.t("clear_hist", "Clear history")) { vm.clearHist() }
                }
            }
        } else if (results.isEmpty()) {
            EmptyBox(I18n.t("no_results_t", "No results"), I18n.t("no_results_b", "Nothing matches. Try fewer filters or check the spelling."))
        } else {
            Text("${results.size} ${I18n.t("n_results", "result")}${if (results.size > 1 && I18n.lang != "id") "s" else ""}", Modifier.padding(bottom = 2.dp), color = NavOff, fontSize = 13.sp)
        }
    }
}
