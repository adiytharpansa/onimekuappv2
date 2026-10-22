package com.onimeku.app.ui

import androidx.compose.ui.layout.layout
import kotlin.math.roundToInt
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
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
    // remember: tanpa ini daftar dipetakan ulang (lookup per id) pada SETIAP recomposition baris.
    val items = remember(ids, Catalog.all) { ids.mapNotNull { Catalog.byId[it] } }
    if (items.isEmpty()) return
    Column(if (delayMs >= 0) Modifier.fadeSlideIn(delayMs) else Modifier) {
        Spacer(Modifier.height(SectionGap))
        // Satu judul saja: tanpa "See all" dan tanpa sisi kanan kosong; tampil sebagai kartu lanskap selebar layar.
        val single = items.size == 1 && !ranked
        SectionHead(title, subtitle = subtitle, onAll = if (single) null else seeAll?.let { key -> { vm.go(Route.SeeAll(key)) } })
        if (single) {
            val a = items[0]
            Poster(
                a, null, modifier = Modifier.padding(horizontal = Sp.S16), ep = true, badges = true,
                meta = cleanTitle(a.title) to cardMeta(a).ifBlank { " " }, sharedKey = "row:$title:${a.id}",
            ) { vm.go(Route.Detail(a.id)) }
            return@Column
        }
        Rail {
            itemsIndexed(items, key = { _, a -> a.id }) { i, a ->
                PosterCard(a, railW, rank = if (ranked) i + 1 else null, sharedKey = "row:$title:${a.id}", index = i) { vm.go(Route.Detail(a.id)) }
            }
        }
    }
}

/**
 * Kartu "Coming Soon": lanskap 16:10 dengan sampul lebar, lencana merah di kiri atas, tahun rilis (kaca gelap)
 * di kanan atas, judul + genre di bawah di atas gradasi. Ketuk = buka Detail.
 */
@Composable
private fun ComingSoonCard(a: Anime, w: androidx.compose.ui.unit.Dp, remind: Boolean, onRemind: () -> Unit, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    val key = "soon:${a.id}"
    Box(
        Modifier.width(w).aspectRatio(16f / 10f)
            .sharedArt(key).liftPress { SharedArt.key = key; onClick() }.clip(shape).artBackground(a).border(1.dp, Line, shape),
    ) {
        val req = rememberImg(com.onimeku.app.data.PosterHealer.coverFor(a) ?: com.onimeku.app.data.PosterHealer.posterFor(a))
        if (req == null) Box(Modifier.fillMaxSize().shimmer())
        else coil.compose.AsyncImage(
            model = req, contentDescription = a.title, modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop, alignment = Alignment.TopCenter,
        )
        Box(Modifier.fillMaxSize().drawBehind {
            drawRect(Brush.verticalGradient(0f to Color(0x59080910), 0.3f to Color.Transparent, 0.55f to Color(0x2214161F), 1f to Color(0xF214161F)))
        })
        Row(
            Modifier.align(Alignment.TopStart).padding(Sp.S12).clip(RoundedCornerShape(8.dp)).background(Red).padding(horizontal = Sp.S8, vertical = Sp.S4),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Sp.S4),
        ) {
            Icon(AppIcons.Clock, null, Modifier.size(12.dp), tint = Color.White)
            Text(I18n.t("coming_badge", "COMING SOON"), fontSize = Ty.Micro, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, color = Color.White, maxLines = 1, softWrap = false)
        }
        if (a.year > 0) Text(
            "${a.year}", Modifier.align(Alignment.TopEnd).padding(Sp.S12).clip(RoundedCornerShape(8.dp)).background(GlassFill)
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp)).padding(horizontal = Sp.S8, vertical = Sp.S4),
            fontSize = Ty.Caption, fontWeight = FontWeight.SemiBold, color = Color.White,
        )
        // Lonceng pengingat: satu-satunya tempat lonceng di Beranda, hanya untuk judul yang belum tayang.
        Box(
            Modifier.align(Alignment.BottomEnd).padding(end = Sp.S12, bottom = Sp.S12).size(48.dp).clip(CircleShape)
                .background(if (remind) Red else GlassFill).border(1.dp, if (remind) Red else Color(0x40FFFFFF), CircleShape)
                .clickable(onClick = onRemind),
            contentAlignment = Alignment.Center,
        ) {
            Icon(if (remind) AppIcons.BellFill else AppIcons.Bell,
                if (remind) I18n.t("reminder_on", "Reminder on") else I18n.t("remind_me", "Remind me"), Modifier.size(20.dp), tint = Color.White)
        }
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = Sp.S16, end = 64.dp, bottom = Sp.S12)) {
            Text(a.title, fontSize = Ty.Title, fontWeight = FontWeight.Bold, lineHeight = 21.sp, letterSpacing = (-0.2).sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val meta = a.genres.take(3).joinToString("  ·  ")
            if (meta.isNotBlank()) Text(meta, Modifier.padding(top = Sp.S4), fontSize = Ty.Caption, color = HeroMeta, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun HomeScreen(vm: AppVM) {
    // Skeleton memudar halus menjadi konten saat katalog tiba (bukan muncul tiba-tiba).
    val loading = Catalog.all.isEmpty() && Catalog.loading
    // true hanya bila instance Home ini lahir saat skeleton tampil (mulai dingin). Buka Home saat data sudah ada: tanpa fade.
    val cameFromSkeleton = remember { loading }
    if (loading) {
        HomeSkeleton(heroHeight(), railW, bottomContentPadding())
    } else {
        // Crossfade lama menumpuk skeleton + konten selama 380 ms (dua "loading" sekaligus di hero).
        // Kini skeleton langsung diganti; hanya konten yang memudar masuk.
        val a = remember { androidx.compose.animation.core.Animatable(if (cameFromSkeleton) 0f else 1f) }
        LaunchedEffect(Unit) { if (a.value < 1f) a.animateTo(1f, tween(320)) }
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = a.value }) { HomeContent(vm) }
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
        // Skeleton ditangani HomeScreen (Crossfade); cabang kedua di sini membuat skeleton ganda saat transisi.
        if (!Catalog.loading) {
            Page(extraTop = 64.dp) {
                RootTitle(I18n.t("home", "Home"))
                EmptyBox(I18n.t("catalog_unavail_t", "Catalog unavailable"), I18n.t("catalog_unavail_b", "The catalog could not be loaded. Check your connection, then try again.")) {
                    Btn("Try again", BtnStyle.Play) { vm.reloadCatalog() }
                }
            }
        }
        return
    }
    // Tarik-untuk-segar BUATAN SENDIRI: riwayatnya,
    // PullToRefreshBox material3 1.3 membuat Home crash di emulator
    // (build .17) sehingga fitur ini dimatikan. Implementasi sekarang
    // tidak memakai komponen perpustakaan: koneksi gulir bersarang
    // mengakumulasi delta tarikan saat daftar di paling atas; lewat
    // ambang 90dp -> vm.refresh(). Risiko crash jalur lama tidak ada.
    // LazyColumn: hanya baris yang terlihat (+ prefetch) yang dikomposisi. Dulu seluruh baris
    // (belasan rail genre) digambar sekaligus saat Home dibuka.
    val base = p.base?.let { Catalog.byId[it] }
    val similar = remember(base?.id, Catalog.all) {
        base?.let { b ->
            val ids = Catalog.similar(b.id).map { it.id }
            // Hindari baris yang sama dengan Top Hits; bila tersisa terlalu sedikit, pakai daftar penuh.
            val fresh = ids.filter { it !in Catalog.top }
            if (fresh.size >= 3) fresh else ids
        }.orEmpty()
    }
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
    val soon = Catalog.comingSoon.take(12)
    // Rail yang isinya (>= 80%) sudah tampil di rail di atasnya disembunyikan, supaya Beranda tidak mengulang
    // judul yang sama berkali-kali saat katalog masih kecil. Urutan = urutan tampil di layar.
    val railLists = remember(Catalog.all) {
        val shown = mutableListOf<Set<String>>(Catalog.top.toSet())
        fun pick(ids: List<String>): List<String> {
            val s = ids.toSet()
            if (s.isEmpty()) return emptyList()
            val dup = shown.any { prev -> s.count { it in prev }.toFloat() / s.size >= 0.8f }
            if (dup) return emptyList()
            shown += s
            return ids
        }
        // "new" (episode terbaru) punya informasi sendiri (nomor episode), jadi selalu tampil tetapi ikut dicatat.
        shown += Catalog.new.map { it.first }.toSet()
        // Airing Now hanya berisi judul berstatus tayang (RELEASING). Selalu tampil bila ada isinya,
        // walau juga ada di Top Hits; satu judul tampil sebagai kartu lanskap (lihat PosterRow).
        val airingRail = Catalog.airing.take(12)
        shown += airingRail.toSet()
        listOf(
            airingRail, pick(Catalog.popular.take(12)),
            pick(Catalog.latest.take(12)), pick(Catalog.donghua.take(12)),
        )
    }
    // Beranda yang dibiarkan terbuka tetap segar: muat ulang katalog tiap 30 menit (judul yang baru rilis
    // keluar dari Coming Soon dan naik ke New Episode Releases). Saat aplikasi dibuka kembali, ON_RESUME sudah menyegarkan (>5 mnt).
    LaunchedEffect(Unit) {
        while (true) { delay(30 * 60_000L); vm.refreshIfStale(30 * 60_000L) }
    }
    // Home hanya menampilkan 24 judul pertama (8 baris); sisanya lewat "Browse all" (daftar penuh + filter genre).
    val gridCols = (LocalConfiguration.current.screenWidthDp / 150).coerceIn(3, 10)
    // Genre terpilih menyaring grid di tempat (null = Semua).
    val homeGenre = vm.homeGenre
    val allRows = remember(Catalog.all, gridCols, homeGenre) {
        (homeGenre?.let { Catalog.byGenre(it) } ?: Catalog.alphabetical)
            .mapNotNull { Catalog.byId[it] }.take(maxOf(24, gridCols * 8)).chunked(gridCols)
    }
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
    // Selama menyegarkan konten tetap tertahan turun (holdPx) bersama indikator, lalu naik halus saat selesai.
    val holdPx = with(density) { 72.dp.toPx() }
    val stretch by androidx.compose.animation.core.animateFloatAsState(
        if (vm.refreshing) maxOf(pullPx, holdPx) else pullPx,
        androidx.compose.animation.core.tween(if (pullPx == 0f) 340 else 0, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "ptrStretch",
    )
    Box(Modifier.fillMaxSize()) {
    LazyColumn(
        Modifier.fillMaxSize().nestedScroll(ptrConn),
        state = vm.homeScroll, contentPadding = PaddingValues(bottom = bottomContentPadding()),
    ) {
        item(key = "hero", contentType = "hero") { Hero(vm, heroH) { stretch } }
        item(key = "surprise", contentType = "surprise") {
            Row(Modifier.padding(horizontal = Sp.S16).padding(top = Sp.S16), horizontalArrangement = Arrangement.spacedBy(Sp.S8)) {
                Chip(I18n.t("surprise", "Surprise me"), icon = AppIcons.Dice) { vm.surprise() }
            }
        }

        if (contItems.isNotEmpty()) {
            item(key = "continue", contentType = "rail") {
                Column(Modifier.fadeSlideIn(0)) {
                    Spacer(Modifier.height(SectionGap))
                    SectionHead(I18n.t("sec_continue", "Continue Watching"))
                    val contW = (LocalConfiguration.current.screenWidthDp * 0.58f).coerceIn(210f, 300f).dp
                    Rail {
                        items(contItems, key = { it.a.id }) { c ->
                            Poster(
                                c.a, contW, modifier = motionItem(), ep = true,
                                meta = c.a.title to ("Episode ${c.ep}" + when {
                                    c.pos > 0 && c.dur > c.pos -> " · ${fmt(c.dur - c.pos)} ${I18n.t("time_left", "left")}"
                                    c.pos > 0 -> " · ${fmt(c.pos)}"
                                    else -> ""
                                }),
                                playIcon = true,
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
        // Coming Soon: tepat di atas "New Episode Releases". Disembunyikan bila API tidak punya judul NOT_YET_RELEASED.
        if (soon.isNotEmpty()) {
            item(key = "soon", contentType = "rail") {
                Column(Modifier.fadeSlideIn(40)) {
                    Spacer(Modifier.height(SectionGap))
                    SectionHead(I18n.t("sec_coming", "Coming Soon")) { vm.go(Route.SeeAll("soon")) }
                    val cw = (LocalConfiguration.current.screenWidthDp * 0.74f).coerceIn(250f, 340f).dp
                    val ensure = LocalEnsureNotif.current
                    Rail {
                        items(soon, key = { it }) { id ->
                            Catalog.byId[id]?.let { a ->
                                ComingSoonCard(a, cw, remind = id in vm.p.remind, onRemind = { vm.toggleRemind(id, ensure) }) { vm.go(Route.Detail(id)) }
                            }
                        }
                    }
                }
            }
        }
        if (fresh.isNotEmpty()) {
            item(key = "new", contentType = "rail") {
                Column(Modifier.fadeSlideIn(70)) {
                    Spacer(Modifier.height(SectionGap))
                    SectionHead(I18n.t("sec_new_eps", "New Episode Releases")) { vm.go(Route.SeeAll("new")) }
                    Rail {
                        items(fresh, key = { it.first }) { (id, ep, whenTxt) ->
                            Catalog.byId[id]?.let { a ->
                                // Judul anime di bawah kartu (dulu kartu hanya bertuliskan "Episode N" tanpa nama anime).
                                Column(motionItem().width(railW)) {
                                    // debug.33: ukuran poster potret SAMA dengan rail lain (dulu kartu lanskap 232:150).
                                    Poster(a, railW, badges = true, meta = "Episode $ep" to whenTxt, newBadge = whenTxt == "Today" || whenTxt == "Yesterday") { vm.go(Route.Detail(id)) }
                                    Text(a.title, Modifier.padding(top = Sp.S8), fontSize = Ty.Small, fontWeight = FontWeight.Medium, lineHeight = 17.sp,
                                        minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }
        // delayMs = -1: tanpa animasi masuk (baris di bawah layar tidak mengulang animasi saat digulir kembali).
        item(key = "airing", contentType = "rail") { PosterRow(I18n.t("sec_airing", "Airing Now"), railLists[0], vm, seeAll = "airing", delayMs = 140) }
        item(key = "popular", contentType = "rail") { PosterRow(I18n.t("sec_popular", "Popular"), railLists[1], vm, seeAll = "popular", delayMs = -1) }
        item(key = "latest", contentType = "rail") { PosterRow(I18n.t("sec_latest", "Latest Releases"), railLists[2], vm, seeAll = "latest", delayMs = -1) }
        item(key = "donghua", contentType = "rail") { PosterRow(I18n.t("sec_donghua", "Donghua"), railLists[3], vm, seeAll = "donghua", delayMs = -1) }
        // All Anime: seluruh katalog langsung tampil sebagai grid 3 kolom (dipecah per baris agar tetap lazy).
        item(key = "all-head", contentType = "all-head") {
            Column {
                Spacer(Modifier.height(SectionGap))
                SectionHead(I18n.t("sec_all", "All Anime")) { vm.go(Route.SeeAll(homeGenre?.let { "genre:$it" } ?: "all")) }
                // Filter genre di tempat: "Semua" aktif secara bawaan; ketuk genre lagi untuk melepas. Tepi kanan memudar = bisa digeser.
                Box(Modifier.padding(bottom = Sp.S16)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(Sp.S8), contentPadding = PaddingValues(horizontal = Sp.S16)) {
                        item(key = "__all") { Chip(I18n.t("all_genres", "All"), on = homeGenre == null, height = 32.dp) { vm.homeGenre = null } }
                        items(Catalog.chips, key = { it }) { g ->
                            Chip(g, on = homeGenre == g, height = 32.dp) { vm.homeGenre = if (homeGenre == g) null else g }
                        }
                    }
                    Box(Modifier.align(Alignment.TopEnd).width(36.dp).height(32.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, Bg))))
                }
            }
        }
        items(allRows.size, key = { "all-row:${homeGenre ?: "-"}:$it" }, contentType = { "all-row" }) { r ->
            val row = allRows[r]
            // Jarak antarkolom = jarak antarbaris (12dp) agar grid rapi.
            Row(Modifier.fillMaxWidth().padding(horizontal = Sp.S16).padding(bottom = Sp.S12), horizontalArrangement = Arrangement.spacedBy(Sp.S12)) {
                row.forEachIndexed { c, a -> Box(Modifier.weight(1f)) { Tile(a, vm, (r * gridCols + c) % 9) } }
                repeat(gridCols - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        item(key = "all-more", contentType = "all-more") {
            Box(Modifier.fillMaxWidth().padding(horizontal = Sp.S16).padding(top = Sp.S4), contentAlignment = Alignment.Center) {
                Btn(I18n.t("browse_all", "Browse all"), BtnStyle.Outline, modifier = Modifier.fillMaxWidth()) { vm.go(Route.SeeAll(homeGenre?.let { "genre:$it" } ?: "all")) }
            }
        }
    }
    // Indikator tarik-untuk-segar: kapsul gelap kaca dengan putaran merah, melayang di atas poster.
    if (vm.refreshing || stretch > 4f) {
        val frac = (stretch / ptrThresholdPx).coerceIn(0f, 1f)
        val k = if (vm.refreshing) 1f else frac
        Box(
            Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = Sp.S12)
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
private fun heroHeight(): androidx.compose.ui.unit.Dp {
    val c = LocalConfiguration.current
    val h = c.screenHeightDp.toFloat()
    // Landscape (lebar > tinggi): hero tidak boleh lebih tinggi dari layar.
    return if (c.screenWidthDp > c.screenHeightDp) (h * 0.85f).coerceIn(240f, 560f).dp else (h * 0.64f).coerceIn(420f, 640f).dp
}

/** Carousel hero: 5 judul teratas, geser manual atau otomatis tiap 5,5 dtk, indikator animasi, parallax saat digulir. */
@Composable
private fun Hero(vm: AppVM, heroH: androidx.compose.ui.unit.Dp, pull: () -> Float = { 0f }) {
    // Hero selalu anime TERBARU secara realtime (perintah user 19 Okt 2026):
    // urut updatedAt API terbaru, bukan skor tertinggi — judul lama tamat
    // tidak lagi mendominasi Beranda. Kualitas hero (perintah user 19 Okt
    // 2026 "poster paling atas harus HD"): dari 30 terbaru, yang punya
    // BANNER (karya lebar, resolusi tertinggi) didahulukan menurut
    // kebaruan, lalu yang hanya punya poster. Syarat minimum: punya poster.
    // remember: dulu dipetakan ulang dari SELURUH daftar freshest (ratusan id) di tiap recomposition Hero.
    val items = remember(Catalog.all) {
        val pool = Catalog.freshest.asSequence().mapNotNull { Catalog.byId[it] }.filter { it.poster != null }.take(30).toList()
        (pool.filter { !it.banner.isNullOrBlank() } + pool.filter { it.banner.isNullOrBlank() })
            .take(5).ifEmpty { Catalog.all.take(1) }
    }
    if (items.isEmpty()) return
    // Pager "tak terbatas": halaman virtual besar, isi diambil dengan modulo. Dengan begitu slide otomatis
    // SELALU bergerak maju satu halaman (dulu dari slide ke-5 ke ke-1 menggulung mundur melewati semua halaman
    // sehingga tampak loncat/berantakan).
    val n = items.size
    val pager = rememberPagerState(initialPage = HeroStart - HeroStart % n) { if (n > 1) HeroPages else 1 }
    val reduceMotion = LocalReduceMotion.current
    // SATU jam zoom Ken Burns untuk semua halaman (dulu tiap HeroPage menjalankan transisi tak berujung sendiri;
    // dengan beyondViewportPageCount = 1 itu 3 animasi paralel).
    val ken = rememberHeroKen()
    // Carousel berhenti berputar selama jari menyentuh hero (tahan untuk membaca), lanjut setelah dilepas.
    var held by remember { mutableStateOf(false) }
    // Dikunci pada settledPage (BUKAN currentPage): currentPage berganti di tengah animasi, sehingga efek lama
    // dibatalkan di tengah jalan dan hero berhenti di posisi setengah-geser (penyebab slide tidak presisi).
    // settledPage hanya berganti setelah slide benar-benar berhenti, jadi animasi selalu tuntas dan menempel rapi.
    LaunchedEffect(pager.settledPage, n) {
        if (n > 1) {
            delay(5500)
            // Jeda juga saat Home tidak di depan (membuka Detail/Player): jangan animasi di balik transisi.
            while (pager.isScrollInProgress || held || vm.overlayOpen || vm.route != Route.Home || vm.homeScroll.firstVisibleItemIndex > 0) delay(100)
            pager.animateScrollToPage(
                pager.settledPage + 1,
                animationSpec = tween(if (reduceMotion) 0 else 850, easing = androidx.compose.animation.core.CubicBezierEasing(0.33f, 0f, 0.15f, 1f)),
            )
        }
    }
    // clipToBounds: gambar parallax digeser ke bawah saat digulir; tanpa klip ia bocor ke bagian di bawah hero.
    Box(
        // Tinggi hero = tinggi dasar + jarak tarik-segar. Dihitung di fase layout (tanpa recompose tiap frame):
        // seluruh konten di bawah hero (Surprise me, rail, dst.) ikut turun persis dari bawah tombol Play.
        Modifier.fillMaxWidth().layout { m, c ->
            val h = heroH.roundToPx() + pull().roundToInt().coerceAtLeast(0)
            val pl = m.measure(c.copy(minHeight = h, maxHeight = h))
            layout(pl.width, h) { pl.place(0, 0) }
        }.clipToBounds().background(Bg).pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                held = true
                do {
                    val e = awaitPointerEvent(PointerEventPass.Initial)
                } while (e.changes.any { it.pressed })
                held = false
            }
        },
    ) {
        // Stretch saat ditarik HANYA pada gambar (di HeroPage); teks & tombol tidak ikut membesar/terpotong.
        // beyondViewportPageCount = 1: halaman tetangga sudah dimuat (gambar & warna) sebelum digeser masuk.
        HorizontalPager(pager, Modifier.fillMaxSize(), beyondViewportPageCount = 1) { i ->
            HeroPage(vm, items[i % n], pull, ken) { (pager.currentPage - i) + pager.currentPageOffsetFraction }
        }
        // Scrim tipis di atas supaya status bar & lonceng tetap terbaca di poster terang.
        Box(Modifier.fillMaxWidth().height(120.dp).drawBehind { drawRect(Brush.verticalGradient(listOf(Color(0x8C000000), Color(0x33000000), Color.Transparent))) })
        if (items.size > 1) {
            // Titik indikator: tepat di atas tombol "+", rata tengah dengannya (kotak selebar tombol; baris titik boleh lebih lebar, dipusatkan).
            // Ikut turun bersama hero (tidak ditahan) agar tetap tepat di atas tombol "+".
            Box(Modifier.align(Alignment.BottomEnd).padding(end = HeroPad, bottom = HeroBottomPad + HeroActionW + HeroDotsGap).width(HeroActionW)) {
                Row(Modifier.wrapContentWidth(Alignment.CenterHorizontally, unbounded = true), horizontalArrangement = Arrangement.spacedBy(Sp.S8)) {
                    items.indices.forEach { i ->
                        HeroDot(selected = pager.currentPage % n == i, page = pager.settledPage)
                    }
                }
            }
        }
    }
}

private const val HeroPages = 100_000
private const val HeroStart = HeroPages / 2

/** Zoom lambat (Ken Burns) sebagai State, dibaca di blok graphicsLayer sehingga tidak merekomposisi tiap frame. */
@Composable
private fun rememberHeroKen(): androidx.compose.runtime.State<Float> {
    if (LocalReduceMotion.current) return remember { mutableFloatStateOf(1f) }
    val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "heroKen")
    return t.animateFloat(
        1f, 1.08f,
        androidx.compose.animation.core.infiniteRepeatable(
            tween(9000, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            androidx.compose.animation.core.RepeatMode.Reverse,
        ),
        label = "heroKenScale",
    )
}

@Composable
private fun HeroPage(vm: AppVM, a: Anime, pull: () -> Float, ken: androidx.compose.runtime.State<Float>, pageOffset: () -> Float) {
    val on = a.id in vm.p.myList
    val firstEp = a.available.firstOrNull() ?: 1
    val wide = LocalConfiguration.current.screenWidthDp >= 700
    // Warna dominan poster menyepuh gradasi, tombol Play, dan label HD (merah tema bila belum terhitung / poster abu-abu).
    val tint = rememberPosterColor(a.poster)
    val accent by androidx.compose.animation.animateColorAsState(tint ?: Red, tween(500), label = "heroAccent")
    val pageW = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    // clipToBounds PER HALAMAN: gambar diperbesar (overscan + Ken Burns) dan digeser (parallax); tanpa klip ia bocor
    // melewati tepi halaman dan menimpa halaman tetangga saat slide, tampak sebagai strip/sambungan yang tidak rapi.
    Box(Modifier.fillMaxSize().clipToBounds().artBackground(a).clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) { SharedArt.key = "hero:${a.id}"; vm.go(Route.Detail(a.id)) }) {
        // Layar lebar: pakai banner (lanskap) bila ada, poster potret akan terpotong parah.
        val heroUrl = if (wide) (com.onimeku.app.data.PosterHealer.coverFor(a) ?: com.onimeku.app.data.PosterHealer.posterFor(a)) else com.onimeku.app.data.PosterHealer.posterFor(a)
        val req = rememberImg(heroUrl)
        if (req == null) Box(Modifier.fillMaxSize().shimmer())
        else coil.compose.AsyncImage(
            model = req, contentDescription = a.title,
            // Parallax: gambar bergeser 35% dari jarak scroll.
            modifier = Modifier.fillMaxSize().sharedArt("hero:${a.id}").graphicsLayer {
                // Gambar sedikit diperbesar (HeroOverscan) sebagai ruang parallax, jadi tepinya tidak
                // pernah membuka celah gelap saat digeser. Saat tarik-segar hero memanjang (layout),
                // gambar otomatis mengisi tinggi baru lewat ContentScale.Crop.
                // Ken Burns digabung ke satu layer (dulu dua graphicsLayer bersarang dengan titik pusat berbeda -> bergetar).
                val z = HeroOverscan * ken.value
                scaleX = z; scaleY = z
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f)
                translationY = vm.homeScrollPx().coerceAtMost(4000) * 0.35f
                translationX = -pageOffset() * size.width * HeroParallax
            },
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            alignment = Alignment.TopCenter,
            onSuccess = { s -> if (heroUrl != null) ImageKeys.put(heroUrl, s.result.memoryCacheKey) },
        )
        // Sepuhan warna poster di bagian bawah gambar (di bawah peleburan ke latar).
        Box(Modifier.fillMaxSize().drawBehind {
            drawRect(Brush.verticalGradient(0f to Color.Transparent, 0.3f to Color.Transparent, 0.65f to accent.copy(alpha = 0.42f), 1f to accent.copy(alpha = 0.2f)))
        })
        Box(Modifier.fillMaxSize().drawBehind {
            drawRect(Brush.verticalGradient(0f to Color.Transparent, 0.26f to Color.Transparent, 0.45f to Color(0x4D14161F), 0.72f to Color(0xD914161F), 0.88f to Color(0xF214161F), 1f to Bg))
        })
        // Layar lebar: info di kiri, gambar terlihat di kanan.
        if (wide) Box(Modifier.fillMaxSize().drawBehind { drawRect(Brush.horizontalGradient(0f to Color(0xCC14161F), 0.6f to Color.Transparent)) })
        val dens = LocalDensity.current
        // Batasi font scale sistem di hero agar kolom teks tidak melebihi tinggi hero.
        androidx.compose.runtime.CompositionLocalProvider(
            LocalDensity provides androidx.compose.ui.unit.Density(dens.density, fontScale = dens.fontScale.coerceAtMost(1.15f)),
        ) {
        Column(Modifier.align(Alignment.BottomStart).graphicsLayer {
            // Saat slide: teks tertinggal sedikit di belakang gambar (kedalaman) dan memudar lembut, lalu muncul
            // kembali di halaman berikutnya. Tidak ada translationY: saat hero memanjang (tarik-segar) rating,
            // judul, deskripsi, Play dan "+" ikut turun bersama tombol "Surprise me" di bawahnya.
            val o = pageOffset()
            alpha = (1f - kotlin.math.abs(o) * 1.6f).coerceIn(0f, 1f)
            translationX = -o * pageW * 0.3f
        }.widthIn(max = 600.dp).padding(start = HeroPad, end = HeroPad, bottom = HeroBottomPad)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Sp.S8), verticalAlignment = Alignment.CenterVertically) {
                a.rating?.let { ScorePill(it) }
                if (a.hd) Tag("HD", accent, Color.White)
                StatusPill(a.status)
            }
            Text(a.title, Modifier.padding(top = Sp.S12), fontSize = 32.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp, letterSpacing = (-0.6).sp,
                minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val meta = listOfNotNull(
                a.year.takeIf { it > 0 }?.toString(),
                // Judul yang masih tayang: tulis episode terakhir ("Ep 11"), bukan "11 episodes" seolah sudah tamat.
                if (a.status == "Airing" && a.ep > 0) "Ep ${a.ep}"
                else a.eps.takeIf { it > 0 }?.let { "$it ${if (it == 1) "episode" else "episodes"}" },
                a.genres.take(2).joinToString(", ").ifBlank { null },
            ).joinToString("  ·  ")
            Text(meta, Modifier.padding(top = Sp.S8), fontSize = Ty.Small, color = HeroMeta, maxLines = 1, overflow = TextOverflow.Ellipsis)
            // Selalu memesan 2 baris (juga saat sinopsis kosong) agar posisi tombol sama di semua slide.
            Text(a.synopsis, Modifier.padding(top = Sp.S8), fontSize = Ty.Small, lineHeight = 19.sp,
                color = HeroSynopsis, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(Modifier.padding(top = HeroActionsTop), horizontalArrangement = Arrangement.spacedBy(HeroActionGap), verticalAlignment = Alignment.CenterVertically) {
                PlayButton(I18n.t("play", "Play"), null, Modifier.weight(1f), accent = accent) { vm.go(Route.Player(a.id, firstEp)) }
                Box(
                    Modifier.size(HeroActionW).pressable { vm.toggleML(a.id) }.clip(RoundedCornerShape(18.dp))
                        .background(if (on) Color.White else GlassFill)
                        .border(1.dp, if (on) Color.White else GlassStroke, RoundedCornerShape(18.dp)).sparkBurst(on),
                    contentAlignment = Alignment.Center,
                ) { MorphIcon(on, AppIcons.Check, AppIcons.Plus, if (on) "Added" else "My List", 26.dp, if (on) Bg else Color.White) }
            }
        }
        }
    }
}

@Composable
fun ItemRow(
    a: Anime, thumbLabel: String, title: String, sub: String, subOk: Boolean = false,
    onThumb: (() -> Unit)? = null, onOpen: () -> Unit, nextUp: Boolean = false, trailing: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().padding(bottom = Sp.S12).clip(shape).background(Bg2).border(1.dp, Line, shape).padding(Sp.S8),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Sp.S12),
    ) {
        Box(Modifier.width(108.dp).aspectRatio(16f / 10f).clip(RoundedCornerShape(11.dp)).artBackground(a)
            .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null, onClick = onThumb ?: onOpen)) {
            RealImage(a, Modifier.fillMaxSize())
            if (thumbLabel.isNotBlank()) Text(thumbLabel, Modifier.align(Alignment.BottomStart).padding(Sp.S8).clip(RoundedCornerShape(6.dp)).background(Color(0xB3000000))
                .padding(horizontal = Sp.S8, vertical = Sp.S2), fontSize = Ty.Micro, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f).clickable(onClick = onOpen)) {
            Text(title, fontSize = Ty.Subtitle, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 19.sp)
            Text(sub, fontSize = Ty.Caption, color = if (subOk) RedText else NavOff, modifier = Modifier.padding(top = Sp.S4), maxLines = 1, overflow = TextOverflow.Ellipsis)
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
        if (l.isEmpty()) {
            // Ajakan di tengah (tanpa kartu berbingkai) + rekomendasi supaya layar tidak kosong.
            Column(Modifier.fillMaxWidth().padding(top = 36.dp, bottom = Sp.S28), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(56.dp).clip(CircleShape).background(Color(0x26A6122D)), contentAlignment = Alignment.Center) {
                    Icon(AppIcons.Bookmark, null, Modifier.size(28.dp), tint = RedText)
                }
                Text(I18n.t("mylist_empty_t", "Your list is empty"), Modifier.padding(top = Sp.S12), fontSize = Ty.Title, fontWeight = FontWeight.SemiBold)
                Text(I18n.t("mylist_empty_b", "Tap + on any anime to save it here."), Modifier.padding(top = Sp.S8, start = Sp.S16, end = Sp.S16), fontSize = Ty.Small, color = InkSoft, textAlign = TextAlign.Center)
                Row(Modifier.padding(top = Sp.S16), horizontalArrangement = Arrangement.spacedBy(Sp.S8), verticalAlignment = Alignment.CenterVertically) {
                    Btn(I18n.t("browse_anime", "Browse anime"), BtnStyle.Play, height = 42.dp) { vm.setTab(Route.Home) }
                    Chip(I18n.t("surprise", "Surprise me"), icon = AppIcons.Dice) { vm.surprise() }
                }
            }
            val recs = Catalog.top.take(10).mapNotNull { Catalog.byId[it] }
            if (recs.isNotEmpty()) {
                Row(Modifier.padding(bottom = Sp.S12), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.padding(end = Sp.S8).width(3.dp).height(16.dp).clip(CircleShape).background(Red))
                    Text(I18n.t("start_here", "Start here"), fontSize = Ty.Subtitle, fontWeight = FontWeight.SemiBold)
                }
                val bleed = with(LocalDensity.current) { 16.dp.roundToPx() }
                Row(
                    Modifier.fillMaxWidth().layout { m, c ->
                        val p = m.measure(c.copy(maxWidth = c.maxWidth + bleed))
                        layout(c.maxWidth, p.height) { p.place(0, 0) }
                    }.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(Sp.S12),
                ) {
                    recs.forEach { t ->
                        val on = t.id in vm.p.myList
                        Column(Modifier.width(104.dp)) {
                            Box(Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(RoundedCornerShape(12.dp)).artBackground(t).clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) { vm.go(Route.Detail(t.id)) }) {
                                RealImage(t, Modifier.fillMaxSize())
                                t.rating?.replace("★", "")?.trim()?.takeIf { it.isNotBlank() }?.let {
                                    Row(Modifier.padding(Sp.S8).clip(RoundedCornerShape(6.dp)).background(Color(0x99000000)).padding(horizontal = Sp.S8, vertical = Sp.S2),
                                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Sp.S4)) {
                                        Icon(Icons.Filled.Star, null, Modifier.size(11.dp), tint = Gold)
                                        Text(it, fontSize = Ty.Micro, fontWeight = FontWeight.Bold, color = Gold)
                                    }
                                }
                                Box(
                                    Modifier.align(Alignment.BottomEnd).padding(Sp.S8).size(30.dp).clip(CircleShape)
                                        .background(if (on) Red else Color(0xCC14161F)).border(1.dp, if (on) Red else Color(0x66FFFFFF), CircleShape)
                                        .clickable { vm.toggleML(t.id) },
                                    contentAlignment = Alignment.Center,
                                ) { Icon(if (on) AppIcons.Check else AppIcons.Plus, if (on) "Added" else "My List", Modifier.size(16.dp), tint = Color.White) }
                            }
                            Text(t.title, Modifier.padding(top = Sp.S8), fontSize = Ty.Caption, lineHeight = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        else Text("${l.size} ${I18n.t("n_saved", "saved")}", color = InkSoft, modifier = Modifier.padding(top = Sp.S4, bottom = Sp.S4))
    }
}

@Composable
fun DownloadsScreen(vm: AppVM) = Page {
    SubHead(I18n.t("tab_download", "Download"), vm)
    val d = vm.s.downloads
    if (d.isEmpty()) EmptyBox(I18n.t("dl_none_t", "No downloads yet"), I18n.t("dl_none_b", "Open an anime and tap the download icon on an episode to watch it offline.")) { Btn(I18n.t("browse_anime", "Browse anime"), BtnStyle.Play) { vm.setTab(Route.Home) } }
    else {
        Text("${d.size} ${I18n.t("n_episodes_unit", "episode")}${if (d.size > 1 && I18n.lang != "id") "s" else ""}", color = InkSoft, modifier = Modifier.padding(top = Sp.S4, bottom = Sp.S12))
        Box(Modifier.padding(bottom = Sp.S12)) { Chip(I18n.t("delete_all", "Delete all"), icon = AppIcons.Trash) { vm.removeAllDl() } }
        d.forEachIndexed { idx, x ->
            val a = Catalog.byId[x.id] ?: return@forEachIndexed
            val pct = vm.downloading["${x.id}:${x.ep}"]
            Column(Modifier.fadeSlideIn(minOf(idx, 5) * 50)) {
                ItemRow(a, "Ep ${x.ep}", a.title,
                    if (pct != null) "${I18n.t("downloading", "Downloading…")} $pct%" else "Episode ${x.ep} · ${I18n.t("saved_offline", "Saved offline")}",
                    onOpen = { vm.go(Route.Player(x.id, x.ep)) }) {
                    IconChip(AppIcons.Trash, I18n.t("remove_dl", "Remove download")) { vm.removeDl(x.id, x.ep) }
                }
                if (pct != null) {
                    val animPct by animateFloatAsState(pct / 100f, tween(300), label = "dlBar")
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { animPct },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = Sp.S12).padding(bottom = Sp.S12),
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
        Icon(AppIcons.Close, "Cancel download", Modifier.size(10.dp), tint = Color.White)
    }
}

// ---- Komponen khusus halaman Detail (redesain debug.33) ----
/** Margin samping konten: 20 dp di ponsel; di layar lebar konten dipusatkan dengan lebar ±720 dp. */
val Gutter: androidx.compose.ui.unit.Dp
    @Composable get() {
        val w = LocalConfiguration.current.screenWidthDp
        return if (w > 760) ((w - 720) / 2).dp else 20.dp
    }
private val GoodGreen = Color(0xFF3DDC97)

/** Tombol bulat kaca (blur-look) yang melayang di atas gambar hero. */
@Composable
fun GlassButton(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(CircleShape).background(Color(0x4D000000))
            .border(1.dp, Color(0x40FFFFFF), CircleShape).pressable(onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, Modifier.size(22.dp), tint = Color.White) }
}

/** Skor ber-bintang emas di atas kaca gelap. */
@Composable
private fun ScorePill(score: String) {
    Row(
        Modifier.clip(RoundedCornerShape(8.dp)).background(ChipGlass).background(Color(0x26FFC857)).padding(horizontal = Sp.S8, vertical = Sp.S4),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Sp.S4),
    ) {
        Icon(Icons.Filled.Star, null, Modifier.size(13.dp), tint = Gold)
        Text(score.replace("★", "").trim(), fontSize = Ty.Caption, fontWeight = FontWeight.Bold, color = Gold)
    }
}

@Composable
private fun StatusPill(status: String) {
    val live = status.equals("Airing", ignoreCase = true)
    Row(
        Modifier.clip(RoundedCornerShape(8.dp)).background(ChipGlass).padding(horizontal = Sp.S8, vertical = Sp.S4),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Sp.S8),
    ) {
        if (live) PulseDot(GoodGreen) else Box(Modifier.size(6.dp).clip(CircleShape).background(NavOff))
        Text(statusText(status), fontSize = Ty.Caption, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 1, softWrap = false)
    }
}

/** Tombol utama: gradasi crimson, tinggi 56dp, bayangan lembut. */
@Composable
private fun PlayButton(label: String, sub: String?, modifier: Modifier = Modifier, accent: Color? = null, onClick: () -> Unit) {
    Row(
        modifier.fillMaxWidth().height(56.dp).pressable(onClick).clip(RoundedCornerShape(18.dp))
            .background(Brush.horizontalGradient(if (accent != null) listOf(androidx.compose.ui.graphics.lerp(accent, Color.White, 0.12f), accent) else listOf(Color(0xFFC8142F), Red)))
            .sweepShine()
            .padding(horizontal = Sp.S20),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Sp.S12),
    ) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(Color(0x33FFFFFF)), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.PlayArrow, null, Modifier.size(22.dp), tint = Color.White)
        }
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = Ty.Subtitle, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
            if (sub != null) Text(sub, fontSize = Ty.Caption, color = Color(0xCCFFFFFF), maxLines = 1)
        }
        Icon(AppIcons.ChevronRight, null, Modifier.nudgeX(), tint = Color(0xB3FFFFFF))
    }
}

/** Aksi sekunder: ikon di atas, label di bawah, tiga sama lebar. */
@Composable
internal fun ActionTile(
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
        Text(label, Modifier.padding(top = Sp.S4), fontSize = Ty.Caption, fontWeight = FontWeight.Medium,
            color = if (on) RedText else InkSoft, maxLines = 1)
    }
}

@Composable
fun SectionTitle(text: String, trailing: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = Gutter), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(4.dp).height(18.dp).clip(CircleShape).background(Red))
        Text(text, Modifier.weight(1f).padding(start = Sp.S12), fontSize = Ty.Title2, fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp)
        trailing?.invoke()
    }
}

/** Satu baris episode: thumbnail + progres tonton + status + aksi unduh. */
@Composable
fun EpisodeCard(
    a: Anime, n: Int, seen: Boolean, progSec: Int?, current: Boolean,
    onOpen: () -> Unit, nextUp: Boolean = false, durSec: Int = 1440, compact: Boolean = false, onLong: (() -> Unit)? = null, trailing: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    val frac = when { seen -> 1f; progSec != null -> (progSec / durSec.coerceAtLeast(1).toFloat()).coerceIn(0.03f, 0.97f); else -> 0f }
    Row(
        Modifier.padding(horizontal = Gutter).padding(bottom = Sp.S12).fillMaxWidth().pressableLong(onLong, onOpen).clip(shape)
            .background(if (current) Color(0x1FA6122D) else Bg2)
            .border(1.dp, if (current) Red.copy(alpha = 0.6f) else Line, shape).padding(Sp.S8),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Sp.S12),
    ) {
        // compact = daftar di halaman pemutar: hulu belum punya thumbnail per episode, jadi poster yang sama berulang
        // terlihat seperti placeholder; tampilkan nomor episode saja.
        if (compact) {
            Box(Modifier.size(58.dp).clip(RoundedCornerShape(11.dp)).background(if (current) Red.copy(alpha = 0.9f) else Color(0x1FFFFFFF)), contentAlignment = Alignment.Center) {
                Text("$n", fontSize = Ty.Title2, fontWeight = FontWeight.Bold, color = Color.White)
                if (frac > 0f) AnimatedProgressBar(frac, Modifier.align(Alignment.BottomStart).fillMaxWidth().height(3.dp), track = Color(0x59FFFFFF))
            }
        } else Box(Modifier.width(118.dp).aspectRatio(16f / 10f).clip(RoundedCornerShape(11.dp)).artBackground(a)) {
            RealImage(a, Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Color(0x40000000)))
            Box(Modifier.align(Alignment.Center).size(30.dp).clip(CircleShape).background(Color(0x99000000)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.PlayArrow, null, Modifier.size(20.dp), tint = Color.White)
            }
            Text("EP $n", Modifier.align(Alignment.TopStart).padding(Sp.S8).clip(RoundedCornerShape(6.dp)).background(Color(0xB3000000))
                .padding(horizontal = Sp.S8, vertical = Sp.S2), fontSize = Ty.Micro, fontWeight = FontWeight.Bold, color = Color.White)
            if (frac > 0f) AnimatedProgressBar(frac, Modifier.align(Alignment.BottomStart).fillMaxWidth().height(3.dp), track = Color(0x59FFFFFF))
        }
        Column(Modifier.weight(1f)) {
            Text("Episode $n", fontSize = Ty.Subtitle, fontWeight = FontWeight.SemiBold, maxLines = 1,
                color = if (seen) Color(0xB3FFFFFF) else Color.White)
            val (txt, col) = when {
                current -> I18n.t("now_watching", "Now watching") to RedText
                seen -> I18n.t("watched", "Watched") to GoodGreen
                progSec != null -> "${I18n.t("resume_at", "Resume at")} ${fmt(progSec)}" to RedText
                nextUp -> I18n.t("up_next", "Up next") to RedText
                else -> I18n.t("not_watched", "Not watched yet") to NavOff
            }
            Row(Modifier.padding(top = Sp.S4), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Sp.S4)) {
                if (seen && !current) Icon(Icons.Filled.CheckCircle, null, Modifier.size(13.dp), tint = col)
                Text(txt, fontSize = Ty.Caption, color = col, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (nextUp && !seen && progSec == null) Text(a.title, Modifier.padding(top = Sp.S2), fontSize = Ty.Caption, color = NavOff, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
    var markEp by remember(id) { mutableStateOf<Int?>(null) }
    val baseEps = remember(a.available, a.eps) { if (a.available.isNotEmpty()) a.available else (1..a.eps).toList() }
    val epNums = remember(baseEps, newestFirst) { if (newestFirst) baseEps.reversed() else baseEps }
    // Dulu Catalog.similar() jalan di main thread pada SETIAP rekomposisi (termasuk saat animasi masuk). Kini di thread latar.
    val sim by produceState(initialValue = emptyList<Anime>(), id, Catalog.all) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { Catalog.similar(id) }
    }
    val resume = vm.p.last?.takeIf { it.id == id }
    // Katalog daftar berbentuk lite: perkaya judul ini dari endpoint
    // detail (sinopsis penuh + nomor episode asli) sekali per sesi.
    LaunchedEffect(id) { vm.loadDetail(id) }
    // Pemanasan data putar: ambil info stream episode sasaran
    // tombol Play (dan episode lanjutan terakhir) SEKARANG, selagi user
    // membaca Detail. Di HP user, resolusi dingin endpoint putar ~5 dtk+
    // di 4G sempat menghabiskan batas tunggu pemutar -> "Stream
    // unavailable" padahal stream-nya hidup. Dengan prefetch, ketukan
    // Play mengenai cache dan video mulai nyaris seketika.
    val trCtx = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(id) {
        // Tunggu animasi masuk selesai, baru pemanasan jaringan (di thread IO) — tidak berebut CPU/jaringan dengan transisi.
        delay(450)
        val firstEp = a.available.firstOrNull() ?: 1
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            com.onimeku.app.data.ApiClient.prefetchPlayback(id, firstEp)
            resume?.let { if (it.ep != firstEp) com.onimeku.app.data.ApiClient.prefetchPlayback(id, it.ep) }
        }
    }
    // Muat-dulu segmen gudang (debug.60): sesudah data putar episode
    // sasaran tombol Putar terambil di atas, hangatkan playlist + segmen
    // pertama tangga awalnya dari pintu terakhir-berhasil (GudangStore
    // .warmStream memeriksa sendiri sumbernya gudang atau bukan, dan
    // berhenti aman bila prefetch belum tiba). Pindah layar membatalkan
    // efek ini; pekerjaan yang sudah terlanjur jalan di IO bersifat
    // usaha-terbaik dan berhenti di permintaan berikutnya yang gagal.
    LaunchedEffect(id) {
        delay(1200)
        val target = resume?.ep ?: (a.available.firstOrNull() ?: 1)
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            com.onimeku.app.data.GudangStore.warmStream(trCtx, id, target, vm.s.settings.quality)
        }
    }
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val cfgD = LocalConfiguration.current
    val heroH = if (cfgD.screenWidthDp > cfgD.screenHeightDp) (cfgD.screenHeightDp * 0.7f).coerceIn(220f, 420f).dp
        else (cfgD.screenHeightDp * 0.5f).coerceIn(320f, 470f).dp
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

    // background(Bg) OPAK: Detail memudar masuk di atas Home tanpa Home tembus di baliknya.
    Box(Modifier.fillMaxSize().background(Bg)) {
        // LazyColumn: judul panjang (ratusan episode) tidak digambar sekaligus.
        LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = bottomContentPadding())) {
            item(key = "hero") {
                // ===== HERO: gambar penuh lebar, parallax, judul menempel di bawahnya =====
                Box(Modifier.fillMaxWidth().height(heroH).clipToBounds().artBackground(a)) {
                    // Dihitung SEKALI per Detail: kunci stabil selama transisi masuk & kembali, tidak berubah oleh klik lain.
                    val heroKey = remember(a.id) { SharedArt.key?.takeIf { it.endsWith(":${a.id}") } }
                    RealImage(a, Modifier.fillMaxSize().sharedArt(heroKey).graphicsLayer {
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
                        Row(horizontalArrangement = Arrangement.spacedBy(Sp.S8), verticalAlignment = Alignment.CenterVertically) {
                            a.rating?.let { ScorePill(it) }
                            if (a.hd) Tag("HD", Red, Color.White)
                            StatusPill(a.status)
                        }
                        Text(a.title, Modifier.padding(top = Sp.S12), fontSize = Ty.Display, fontWeight = FontWeight.Bold,
                            lineHeight = 33.sp, letterSpacing = (-0.5).sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        Text(listOfNotNull(a.year.takeIf { it > 0 }?.toString(), a.eps.takeIf { it > 0 }?.let { "$it eps" }, a.genres.take(2).joinToString(", ").ifBlank { null }).joinToString("  ·  "),
                            Modifier.padding(top = Sp.S8), fontSize = Ty.Small, color = InkSoft, maxLines = 1)
                    }
                    }
                }
            }
            item(key = "actions") {
                Column(Modifier.padding(horizontal = Gutter).fadeSlideIn(60)) {
                    Spacer(Modifier.height(Sp.S16))
                    PlayButton(
                        label = if (resume != null) "${I18n.t("resume", "Resume Ep")} ${resume.ep}" else I18n.t("play", "Play"),
                        sub = resume?.let { r -> vm.p.prog["$id:${r.ep}"]?.let { "${I18n.t("continue_from", "Continue from")} ${fmt(it)}" } }
                            ?: if (resume == null) "${I18n.t("start_with", "Start with Episode")} ${a.available.firstOrNull() ?: 1}" else null,
                    ) { vm.go(Route.Player(id, resume?.ep ?: a.available.firstOrNull() ?: 1)) }
                    Row(Modifier.fillMaxWidth().padding(top = Sp.S12), horizontalArrangement = Arrangement.spacedBy(Sp.S12)) {
                        ActionTile(AppIcons.Plus, if (on) I18n.t("added", "Added") else I18n.t("my_list", "My List"),
                            Modifier.weight(1f), on = on, iconOn = AppIcons.Check) { vm.toggleML(id) }
                        ActionTile(AppIcons.Download, I18n.t("download_all", "Download all"), Modifier.weight(1f)) { vm.downloadAll(id) }
                        ActionTile(AppIcons.Share, I18n.t("share", "Share"), Modifier.weight(1f)) { share() }
                    }
                }
            }
            if (a.genres.isNotEmpty()) item(key = "genres") {
                Row(Modifier.padding(top = Sp.S16).horizontalScroll(rememberScrollState()).padding(horizontal = Gutter).fadeSlideIn(110),
                    horizontalArrangement = Arrangement.spacedBy(Sp.S8)) {
                    a.genres.forEach { g ->
                        Text(g, fontSize = Ty.Caption, fontWeight = FontWeight.Medium, color = InkSoft, maxLines = 1,
                            modifier = Modifier.clip(CircleShape).background(Color(0x14FFFFFF)).padding(horizontal = Sp.S12, vertical = Sp.S8))
                    }
                }
            }
            item(key = "synopsis") {
                Column(Modifier.padding(horizontal = Gutter).padding(top = Sp.S20).fadeSlideIn(160)) {
                    Text(I18n.t("synopsis", "Synopsis"), fontSize = Ty.Subtitle, fontWeight = FontWeight.Bold)
                    Box(Modifier.padding(top = Sp.S8).animateContentSize()) {
                        // Detail lengkap masih diambil dan sinopsis belum ada -> kerangka teks, bukan ruang kosong.
                        if (a.synopsis.isBlank() && vm.detailBusy == id) TextSkeleton(4)
                        else Text(a.synopsis, color = InkSoft, fontSize = Ty.Body, lineHeight = 22.sp,
                            maxLines = if (expanded) Int.MAX_VALUE else 4, overflow = TextOverflow.Ellipsis)
                    }
                    if (a.synopsis.length > 180) {
                        Row(Modifier.padding(top = Sp.S8).clip(CircleShape).clickable { expanded = !expanded }.padding(vertical = Sp.S4),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(if (expanded) I18n.t("read_less", "Show less") else I18n.t("read_more", "Read more"),
                                color = RedText, fontSize = Ty.Body, fontWeight = FontWeight.SemiBold)
                            Icon(if (expanded) AppIcons.ChevronUp else AppIcons.ChevronDown, null, Modifier.size(18.dp), tint = RedText)
                        }
                    }
                }
            }
            // ===== Episode lebih dulu: ini alasan orang membuka halaman ini =====
            item(key = "ep_head") {
                Spacer(Modifier.height(Sp.S24))
                SectionTitle(I18n.t("episodes", "Episodes")) {
                    Text("${epNums.size}", fontSize = Ty.Small, color = NavOff, modifier = Modifier.padding(end = Sp.S12))
                    if (epNums.size > 1) Row(
                        Modifier.clip(CircleShape).background(Bg2).border(1.dp, Line, CircleShape)
                            .clickable { newestFirst = !newestFirst }.padding(horizontal = Sp.S12, vertical = Sp.S8),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Sp.S4),
                    ) {
                        Icon(AppIcons.Swap, null, Modifier.size(15.dp), tint = InkSoft)
                        Text(if (newestFirst) I18n.t("ep_newest", "Newest") else I18n.t("ep_oldest", "Oldest"), fontSize = Ty.Caption, fontWeight = FontWeight.Medium, color = InkSoft)
                    }
                }
                Spacer(Modifier.height(Sp.S12))
                if (epNums.isEmpty()) {
                    if (vm.detailBusy == id) Box(Modifier.padding(horizontal = Gutter)) { SkeletonRows(3) }
                    else Text(I18n.t("no_eps", "No episodes available yet."), Modifier.padding(horizontal = Gutter), color = NavOff, fontSize = Ty.Body)
                }
            }
            itemsIndexed(epNums, key = { _, n -> "ep$n" }) { idx, n ->
                val k = "$id:$n"
                val pct = vm.downloading[k]
                val dl = vm.hasDl(id, n)
                Box(Modifier.staggerIn("ep:$id:$n", idx)) {
                EpisodeCard(
                    a, n, seen = k in vm.p.seen, progSec = vm.p.prog[k], current = resume?.ep == n,
                    onOpen = { vm.go(Route.Player(id, n)) },
                    onLong = { markEp = n },
                ) {
                    when {
                        pct != null -> DownloadRing(pct) { vm.download(id, n) }
                        dl -> IconChip(AppIcons.Check, I18n.t("saved_offline", "Saved offline"), on = true) { vm.download(id, n) }
                        else -> IconChip(AppIcons.Download, "${I18n.t("download_ep", "Download episode")} $n") { vm.download(id, n) }
                    }
                }
                }
            }
            if (epNums.size > 1) item(key = "ep_hint") {
                Text(I18n.t("ep_hint", "Tip: press and hold an episode to mark it as watched."), Modifier.padding(horizontal = Gutter).padding(bottom = Sp.S4), fontSize = Ty.Caption, color = NavOff)
            }
            if (sim.isNotEmpty()) {
                item(key = "similar") {
                    Spacer(Modifier.height(Sp.S24))
                    SectionTitle(I18n.t("more_like", "More like this"))
                    Spacer(Modifier.height(Sp.S12))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(Sp.S12), contentPadding = PaddingValues(horizontal = Gutter)) {
                        items(sim, key = { it.id }) { x -> PosterCard(x, railW) { vm.go(Route.Detail(x.id)) } }
                    }
                }
            }
        }
        markEp?.let { n ->
            val seenNow = "$id:$n" in vm.p.seen
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { markEp = null },
                title = { Text("Episode $n") },
                text = { Text(I18n.t("mark_body", "Mark episodes as watched up to this one? Useful if you watched them in another app.")) },
                confirmButton = {
                    Row {
                        if (seenNow) androidx.compose.material3.TextButton(onClick = { vm.unmarkSeen(id, n); markEp = null }) { Text(I18n.t("mark_unwatched", "Mark unwatched")) }
                        androidx.compose.material3.TextButton(onClick = { vm.markSeenUpTo(id, n); markEp = null }) { Text("${I18n.t("mark_upto", "Mark up to Ep")} $n", color = RedText) }
                    }
                },
                dismissButton = { androidx.compose.material3.TextButton(onClick = { markEp = null }) { Text(I18n.t("cancel", "Cancel")) } },
            )
        }
        // ===== Bilah atas melayang: kaca di atas hero, jadi solid + judul saat digulir =====
        Box(Modifier.fillMaxWidth().height(statusTop + 58.dp).background(Bg.copy(alpha = barA * 0.97f)).padding(top = statusTop)) {
            Row(Modifier.fillMaxSize().padding(horizontal = Sp.S16), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Sp.S12)) {
                GlassButton(AppIcons.ArrowLeft, "Back") { vm.back() }
                Text(a.title, Modifier.weight(1f).graphicsLayer { alpha = barA }, fontSize = Ty.Subtitle, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                GlassButton(if (on) AppIcons.BookmarkFill else AppIcons.Bookmark, if (on) "Remove from My List" else "Add to My List") { vm.toggleML(id) }
            }
        }
    }
}

/** Waktu komentar dari server (UTC ISO) ditampilkan dalam zona waktu perangkat, mis. "19 Okt 14:05". */
internal fun uiLocale(): Locale = if (I18n.lang == "id") Locale.forLanguageTag("id") else Locale.ENGLISH

@Composable
fun SeeAllScreen(vm: AppVM, key: String) {
    val (title, ids) = when {
        key == "top" -> I18n.t("sec_top", "Top Hits Anime") to Catalog.popular // semua judul urut skor, bukan hanya 10 teratas
        key == "all" -> I18n.t("sec_all", "All Anime") to Catalog.alphabetical
        key == "airing" -> I18n.t("sec_airing", "Airing Now") to Catalog.airing
        key == "popular" -> I18n.t("sec_popular", "Popular") to Catalog.popular
        key == "latest" -> "Latest Releases" to Catalog.latest
        key == "donghua" -> I18n.t("sec_donghua", "Donghua") to Catalog.donghua
        key == "soon" -> I18n.t("sec_coming", "Coming Soon") to Catalog.comingSoon
        key.startsWith("genre:") -> key.removePrefix("genre:") to Catalog.byGenre(key.removePrefix("genre:"))
        else -> I18n.t("sec_new_eps", "New Episode Releases") to Catalog.new.map { it.first }
    }
    val items = remember(key, ids, Catalog.all) { ids.mapNotNull { Catalog.byId[it] } }
    // Katalog belum tiba: kerangka grid, bukan daftar kosong "0 anime".
    if (Catalog.all.isEmpty() && Catalog.loading) {
        // Selaras dengan AnimeGrid (lebar maks. GridMaxW, margin responsif) supaya tidak "melompat" saat katalog tiba.
        Box(Modifier.fillMaxSize().safeSides(), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = GridMaxW).fillMaxWidth().padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + Sp.S16)) {
                Box(Modifier.padding(horizontal = pageSidePad())) { SubHead(title, vm) }
                PosterGridSkeleton(4, Modifier.padding(top = Sp.S8), hPad = pageSidePad())
            }
        }
        return
    }
    AnimeGrid(items, vm) {
        SubHead(title, vm)
        Text("${items.size} anime", color = NavOff, fontSize = Ty.Small, modifier = Modifier.padding(start = Sp.S4, bottom = Sp.S4))
    }
}

// ------------------------------------------------------------------ NOTIFICATIONS
@Composable
fun NotifScreen(vm: AppVM) = Page {
    SubHead(I18n.t("notifications", "Notifications"), vm)
    if (!vm.s.settings.alerts) {
        Row(Modifier.fillMaxWidth().padding(bottom = Sp.S8).clip(RoundedCornerShape(14.dp)).background(Bg2)
            .border(1.dp, Line, RoundedCornerShape(14.dp)).padding(Sp.S12)) {
            Text("New episode alerts are off. ", fontSize = Ty.Small, color = InkSoft)
            Text("Turn on in Settings", fontSize = Ty.Small, color = RedText, modifier = Modifier.clickable { vm.openSettings() })
        }
    }
    if (Catalog.all.isEmpty()) SkeletonRows(4)
    Catalog.notifs.forEach { (id, ep, whenTxt) ->
        val a = Catalog.byId[id] ?: return@forEach
        Column {
            Row(Modifier.fillMaxWidth().clickable { vm.go(Route.Detail(id)) }.padding(vertical = Sp.S16),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Sp.S12)) {
                Box(Modifier.size(9.dp).clip(CircleShape).background(if (vm.s.notifRead) Color.Transparent else Red))
                Text(androidx.compose.ui.text.buildAnnotatedString {
                    pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold)); append(a.title); pop()
                    append(" episode $ep is out")
                }, Modifier.weight(1f), fontSize = Ty.Body)
                Text(whenTxt, fontSize = Ty.Caption, color = NavOff)
            }
            HLine()
        }
    }
    if (!vm.s.notifRead) Box(Modifier.padding(top = Sp.S16)) { Btn(I18n.t("mark_all_read", "Mark all as read"), BtnStyle.Solid) { vm.markAllRead() } }
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
    val voiceCtx = androidx.compose.ui.platform.LocalContext.current
    val voiceOk = remember { android.speech.SpeechRecognizer.isRecognitionAvailable(voiceCtx) }
    var showVoice by remember { mutableStateOf(false) }
    if (showVoice) VoiceSearchSheet(
        onApply = { txt -> q = txt; vm.addHist(txt); showVoice = false },
        onType = { showVoice = false; fr.requestFocus() },
        onDismiss = { showVoice = false },
    )
    val any = gs.isNotEmpty() || yr.isNotEmpty() || st.isNotEmpty()

    @Composable fun group(label: String, content: @Composable RowScope.() -> Unit) {
        Row(Modifier.fillMaxWidth().padding(top = Sp.S12), verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.width(56.dp), fontSize = Ty.Caption, color = NavOff)
            // Baris chip melewati padding kanan halaman (sampai tepi layar) dan memudar di ujung, bukan terpotong kasar.
            val bleed = with(LocalDensity.current) { 16.dp.roundToPx() }
            Row(
                Modifier.weight(1f)
                    .layout { m, c ->
                        val p = m.measure(c.copy(maxWidth = c.maxWidth + bleed))
                        layout(c.maxWidth, p.height) { p.place(0, 0) }
                    }
                    .graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        val w = 28.dp.toPx()
                        drawRect(
                            Brush.horizontalGradient(listOf(Color.Black, Color.Transparent), startX = size.width - w, endX = size.width),
                            topLeft = Offset(size.width - w, 0f), size = androidx.compose.ui.geometry.Size(w, size.height),
                            blendMode = androidx.compose.ui.graphics.BlendMode.DstIn,
                        )
                    }
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Sp.S8), content = content,
            )
        }
    }
    @Composable fun heading(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector? = null, top: androidx.compose.ui.unit.Dp = 20.dp) {
        Row(Modifier.padding(top = top, bottom = Sp.S8), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Sp.S8)) {
            if (icon != null) Icon(icon, null, Modifier.size(20.dp), tint = RedText)
            Text(text, fontSize = Ty.Title, fontWeight = FontWeight.SemiBold)
        }
    }

    val query = appliedQ.trim().lowercase()
    // Dulu: tiap perubahan kata/filter memanggil lowercase() + joinToString() untuk SELURUH katalog di main thread
    // (ribuan alokasi string -> ketukan chip genre terasa tersendat). Kini teks huruf-kecil dihitung SEKALI per katalog
    // dan penyaringan berjalan di thread latar; hasil lama tetap tampil sampai hasil baru siap (tidak berkedip kosong).
    val searchIndex by produceState(initialValue = emptyList<Pair<Anime, String>>(), Catalog.all) {
        val all = Catalog.all
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            all.map { it to (it.title.lowercase() + "\n" + it.genres.joinToString(" ").lowercase()) }
        }
    }
    val results by produceState(initialValue = emptyList<Anime>(), query, gs, yr, st, any, searchIndex) {
        value = if (query.isEmpty() && !any) emptyList()
        else kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            searchIndex.filter { (c, text) ->
                (query.isEmpty() || text.contains(query)) &&
                    gs.all { it in c.genres } && (yr.isEmpty() || c.year.toString() == yr) && (st.isEmpty() || c.status == st)
            }.map { it.first }
        }
    }

    // Hasil memakai grid lazy: mengetik di katalog besar tidak lagi menggambar ratusan kartu sekaligus.
    AnimeGrid(results, vm) {
        // Search adalah tab utama: tanpa panah kembali kecuali dibuka dari layar lain (tumpukan > 1).
        if (vm.stack.size > 1) SubHead(I18n.t("search", "Search"), vm)
        else Text(I18n.t("search", "Search"), Modifier.padding(bottom = Sp.S12), fontSize = Ty.Display, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
        Row(Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(18.dp)).background(Bg2).border(1.dp, if (q.isNotEmpty()) Red.copy(alpha = 0.7f) else Line, RoundedCornerShape(18.dp)).padding(start = Sp.S16, end = Sp.S8),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Sp.S12)) {
            Icon(AppIcons.Search, null, Modifier.size(22.dp), tint = if (q.isNotEmpty()) RedText else NavOff)
            BasicTextField(q, { q = it }, Modifier.weight(1f).focusRequester(fr), singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = Ty.Subtitle), cursorBrush = SolidColor(Color.White),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { vm.addHist(q) }),
                decorationBox = { inner -> if (q.isEmpty()) Text(I18n.t("search_hint2", "Search anime or genre"), color = NavOff); inner() })
            if (q.isNotEmpty()) Box(Modifier.size(48.dp).clip(CircleShape).clickable { q = "" }, contentAlignment = Alignment.Center) {
                Icon(AppIcons.Close, "Clear", Modifier.size(18.dp), tint = InkSoft)
            } else if (voiceOk) Box(Modifier.size(48.dp).clip(CircleShape).clickable { showVoice = true }, contentAlignment = Alignment.Center) {
                Icon(AppIcons.Mic, I18n.t("voice_search", "Voice search"), Modifier.size(22.dp), tint = InkSoft)
            }
        }
        group("Genre") { Catalog.chips.forEach { g -> Chip(g, g in gs) { gs = if (g in gs) gs - g else gs + g } } }
        group("Status") { Catalog.statuses.forEach { v -> Chip(v, st == v) { st = if (st == v) "" else v } } }
        group(I18n.t("f_year", "Year")) {
            Catalog.years.forEach { v -> Chip(v, yr == v) { yr = if (yr == v) "" else v } }
        }
        if (any) {
            val nf = gs.size + (if (yr.isNotEmpty()) 1 else 0) + (if (st.isNotEmpty()) 1 else 0)
            Row(Modifier.fillMaxWidth().padding(top = Sp.S12), verticalAlignment = Alignment.CenterVertically) {
                Text("$nf ${I18n.t("filters_active", "filters active")}", fontSize = Ty.Caption, color = NavOff)
                Spacer(Modifier.weight(1f))
                Text(I18n.t("reset", "Reset"), Modifier.clip(RoundedCornerShape(8.dp)).clickable { gs = emptyList(); yr = ""; st = "" }.padding(horizontal = Sp.S8, vertical = Sp.S4),
                    fontSize = Ty.Small, fontWeight = FontWeight.Medium, color = RedText)
            }
        }
        Spacer(Modifier.height(Sp.S8))
        if (q.trim().lowercase() != query) {
            androidx.compose.material3.LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp), color = Red, trackColor = Color.Transparent)
        }

        if (query.isEmpty() && !any) {
            if (vm.s.hist.isEmpty()) {
                // Ajakan singkat (bukan kartu berbingkai bertanda "kosong"): ikon pencarian + dua baris teks.
                Column(Modifier.fillMaxWidth().padding(top = Sp.S28, bottom = Sp.S8), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(AppIcons.Search, null, Modifier.size(30.dp), tint = RedText)
                    Text(I18n.t("find_next_t", "Find your next anime"), Modifier.padding(top = Sp.S8), fontSize = Ty.Subtitle, fontWeight = FontWeight.SemiBold)
                    Text(I18n.t("find_next_b", "Search a title or genre, or pick filters above."), Modifier.padding(top = Sp.S4), fontSize = Ty.Small, color = InkSoft, textAlign = TextAlign.Center)
                }
            } else {
                heading(I18n.t("recent_searches", "Recent searches"), AppIcons.History, top = 14.dp)
                FlowRow(Modifier.padding(top = Sp.S8), horizontalArrangement = Arrangement.spacedBy(Sp.S8), verticalArrangement = Arrangement.spacedBy(Sp.S8)) {
                    vm.s.hist.forEach { h -> Chip(h) { q = h } }
                    Chip(I18n.t("clear_hist", "Clear history")) { vm.clearHist() }
                }
            }
            val trend = Catalog.top.take(8).mapNotNull { Catalog.byId[it] }
            if (trend.isNotEmpty()) {
                heading(I18n.t("trending_now", "Trending now"), AppIcons.Flame)
                trend.forEachIndexed { i, t ->
                    if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
                    Row(
                        Modifier.fillMaxWidth().clickable { vm.go(Route.Detail(t.id)) }.padding(vertical = Sp.S8),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Sp.S12),
                    ) {
                        Text("${i + 1}", Modifier.width(18.dp), fontSize = Ty.Body, fontWeight = FontWeight.Bold, color = RedText, textAlign = TextAlign.Center)
                        Box(Modifier.size(width = 44.dp, height = 62.dp).clip(RoundedCornerShape(8.dp)).artBackground(t)) { RealImage(t, Modifier.fillMaxSize()) }
                        Column(Modifier.weight(1f)) {
                            Text(t.title, fontSize = Ty.Subtitle, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 19.sp)
                            val gl = t.genres.take(2).joinToString(", ")
                            if (gl.isNotBlank()) Text(gl, Modifier.padding(top = Sp.S2), fontSize = Ty.Caption, color = NavOff, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        t.rating?.replace("★", "")?.trim()?.takeIf { it.isNotBlank() }?.let {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Sp.S4)) {
                                Icon(Icons.Filled.Star, null, Modifier.size(13.dp), tint = Gold)
                                Text(it, fontSize = Ty.Caption, fontWeight = FontWeight.Bold, color = Gold)
                            }
                        }
                    }
                }
            }
        } else if (results.isEmpty()) {
            EmptyBox(I18n.t("no_results_t", "No results"), I18n.t("no_results_b", "Nothing matches. Try fewer filters or check the spelling."))
        } else {
            Text("${results.size} ${I18n.t("n_results", "result")}${if (results.size > 1 && I18n.lang != "id") "s" else ""}", Modifier.padding(bottom = Sp.S2), color = NavOff, fontSize = Ty.Small)
        }
    }
}
