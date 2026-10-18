package com.onimeku.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.TextStyle
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
@Composable
fun HomeScreen(vm: AppVM) {
    val p = vm.p
    if (Catalog.all.isEmpty()) {
        Page {
            RootTitle("Home")
            if (Catalog.loading) {
                Text("Loading catalog…", color = InkSoft, modifier = Modifier.padding(top = 12.dp))
            } else {
                EmptyBox("Catalog unavailable", "The catalog could not be loaded. Check your connection, then try again.") {
                    Btn("Try again", BtnStyle.Play) { vm.reloadCatalog() }
                }
            }
        }
        return
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 110.dp)) {
        Hero(vm)

        val last = p.last
        val t = last?.let { p.prog["${it.id}:${it.ep}"] }
        if (last != null && t != null && Catalog.byId[last.id] != null) {
            val a = Catalog.byId[last.id]!!
            Spacer(Modifier.height(26.dp))
            SectionHead("Continue Watching")
            Rail {
                item {
                    Poster(a, railW, ep = true, meta = a.title to "Episode ${last.ep} · ${fmt(t)} / 24:00", progress = t / 1440f) {
                        vm.go(Route.Player(last.id, last.ep))
                    }
                }
            }
        }

        val rec = p.base?.let { Catalog.byId[it] }?.let { Catalog.similar(it.id) }.orEmpty()
        if (rec.isNotEmpty()) {
            Spacer(Modifier.height(26.dp))
            SectionHead("Because you watched ${p.base?.let { Catalog.byId[it]?.title }.orEmpty()}")
            Rail { items(rec) { a -> Poster(a, railW, badges = true) { vm.go(Route.Detail(a.id)) } } }
        }

        Spacer(Modifier.height(26.dp))
        SectionHead("Top Hits Anime") { vm.go(Route.SeeAll("top")) }
        Rail {
            items(Catalog.top.size) { i ->
                val a = Catalog.byId[Catalog.top[i]]!!
                Poster(a, railW, badges = true, label = true, rank = i + 1) { vm.go(Route.Detail(a.id)) }
            }
        }

        Spacer(Modifier.height(26.dp))
        SectionHead("New Episode Releases") { vm.go(Route.SeeAll("new")) }
        Rail {
            items(Catalog.new) { (id, ep, whenTxt) ->
                val a = Catalog.byId[id]!!
                Poster(a, railW, ep = true, meta = "Episode $ep" to whenTxt) { vm.go(Route.Detail(id)) }
            }
        }
    }
}

@Composable
private fun Hero(vm: AppVM) {
    val a = Catalog.top.firstOrNull()?.let { Catalog.byId[it] } ?: Catalog.all.firstOrNull() ?: return
    val on = a.id in vm.p.myList
    val firstEp = a.available.firstOrNull() ?: 1
    Box(Modifier.fillMaxWidth().height(560.dp).artBackground(a)) {
        val img = com.onimeku.app.data.PosterHealer.coverFor(a) ?: com.onimeku.app.data.PosterHealer.posterFor(a)
        if (!img.isNullOrBlank()) {
            coil.compose.AsyncImage(
                model = img, contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            )
        }
        Box(Modifier.fillMaxSize().drawBehind {
            drawRect(Brush.verticalGradient(0f to Color(0x5914161F), 0.3f to Color.Transparent, 0.72f to Color(0xB814161F), 1f to Bg))
        })
        Column(Modifier.align(Alignment.BottomStart).padding(start = 16.dp, end = 16.dp, bottom = 28.dp)) {
            Text(a.title, fontSize = 30.sp, fontWeight = FontWeight.SemiBold, lineHeight = 36.sp)
            Text(a.genres.joinToString(", "), Modifier.padding(top = 10.dp), fontSize = 14.sp, color = Color(0xFFF1E8EE))
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
        if (list.isEmpty()) EmptyBox("Nothing airing", "No new episodes are scheduled for this day.")
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
                Text(thumbLabel, Modifier.align(Alignment.BottomStart).fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xD90A0B10))))
                    .padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            Column(Modifier.weight(1f).clickable(onClick = onOpen)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(sub, fontSize = 12.sp, color = if (subOk) RedText else NavOff, modifier = Modifier.padding(top = 2.dp))
            }
            trailing()
        }
        HLine()
    }
}

// ------------------------------------------------------------------ MY LIST / DOWNLOADS
@Composable
fun MyListScreen(vm: AppVM) = Page {
    RootTitle("My List")
    val l = vm.p.myList.mapNotNull { Catalog.byId[it] }
    if (l.isEmpty()) EmptyBox("Your list is empty", "Tap My List on any anime to save it here.") { Btn("Browse anime", BtnStyle.Play) { vm.setTab(Route.Home) } }
    else {
        Text("${l.size} saved", color = InkSoft, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
        TileGrid(l, vm)
    }
}

private fun sizeOf(id: String, ep: Int) = 110 + ((id[1].code * 7 + ep * 13) % 90)

@Composable
fun DownloadsScreen(vm: AppVM) = Page {
    RootTitle("Download")
    val d = vm.s.downloads
    if (d.isEmpty()) EmptyBox("No downloads yet", "Open an anime and tap the download icon on an episode to watch it offline.") { Btn("Browse anime", BtnStyle.Play) { vm.setTab(Route.Home) } }
    else {
        val tot = d.sumOf { sizeOf(it.id, it.ep) }
        Text("${d.size} episode${if (d.size > 1) "s" else ""} · $tot MB", color = InkSoft, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
        Box(Modifier.padding(bottom = 12.dp)) { Chip("Delete all", icon = Icons.Outlined.Delete) { vm.removeAllDl() } }
        d.forEach { x ->
            val a = Catalog.byId[x.id] ?: return@forEach
            ItemRow(a, "Ep ${x.ep}", a.title, "Episode ${x.ep} · ${sizeOf(x.id, x.ep)} MB",
                onOpen = { vm.go(Route.Player(x.id, x.ep)) }) {
                IconChip(Icons.Outlined.Delete, "Remove download") { vm.removeDl(x.id, x.ep) }
            }
        }
    }
}

// ------------------------------------------------------------------ DETAIL
@Composable
fun DetailScreen(vm: AppVM, id: String) {
    val a = Catalog.byId[id] ?: return
    val on = id in vm.p.myList
    Page {
        SubHead("Details", vm)
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(18.dp)).artBackground(a)
            .drawBehind { drawRect(Brush.verticalGradient(0.5f to Color.Transparent, 1f to Color(0xB314161F))) })
        Spacer(Modifier.height(16.dp))
        Text(a.title, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, lineHeight = 31.sp)
        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            a.rating?.let { Tag(it, Color.White, Bg) }
            if (a.hd) Tag("HD", Red, Color.White)
            Text("${a.eps} episodes · ${a.genres.joinToString(", ")}", fontSize = 13.sp, color = InkSoft, modifier = Modifier.weight(1f))
        }
        Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Btn("Play", BtnStyle.Play, Icons.Filled.PlayArrow) { vm.go(Route.Player(id, a.available.firstOrNull() ?: 1)) }
            Btn(if (on) "Added" else "My List", if (on) BtnStyle.OutlineOn else BtnStyle.Outline,
                if (on) Icons.Filled.Check else Icons.Filled.Add) { vm.toggleML(id) }
        }
        Box(Modifier.padding(top = 14.dp)) { Chip("Download all", icon = Icons.Outlined.Download) { vm.downloadAll(id) } }
        Text(a.synopsis, Modifier.padding(top = 18.dp), color = InkSoft, fontSize = 15.sp)

        val sim = Catalog.similar(id)
        if (sim.isNotEmpty()) {
            Text("More like this", Modifier.padding(top = 26.dp, bottom = 12.dp), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                sim.forEach { x -> Poster(x, railW, badges = true) { vm.go(Route.Detail(x.id)) } }
            }
        }
        Text("Episodes", Modifier.padding(top = 26.dp, bottom = 4.dp), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        val epNums = if (a.available.isNotEmpty()) a.available else (1..a.eps).toList()
        for (n in epNums) {
            val k = "$id:$n"
            val seen = k in vm.p.seen
            val prog = vm.p.prog[k]
            val pct = vm.downloading[k]
            val dl = vm.hasDl(id, n)
            ItemRow(
                a, "$n", "Episode $n",
                if (seen) "24 min · Watched" else "24 min" + (prog?.let { " · Resume at ${fmt(it)}" } ?: ""), subOk = seen,
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
fun SeeAllScreen(vm: AppVM, key: String) = Page {
    SubHead(if (key == "top") "Top Hits Anime" else "New Episode Releases", vm)
    val ids = if (key == "top") Catalog.top else Catalog.new.map { it.first }
    TileGrid(ids.mapNotNull { Catalog.byId[it] }, vm)
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
        val a = Catalog.byId[id]!!
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

    Page {
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

        val query = q.trim().lowercase()
        if (query.isEmpty() && !any) {
            if (vm.s.hist.isEmpty()) Text("Search a title, or pick filters above", Modifier.padding(top = 10.dp), color = NavOff, fontSize = 13.sp)
            else {
                Text("Recent searches", Modifier.padding(top = 10.dp), color = NavOff, fontSize = 13.sp)
                FlowRow(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    vm.s.hist.forEach { h -> Chip(h) { q = h } }
                    Chip("Clear history") { vm.clearHist() }
                }
            }
        } else {
            val r = Catalog.all.filter { c ->
                (query.isEmpty() || c.title.lowercase().contains(query) || c.genres.joinToString(" ").lowercase().contains(query)) &&
                    gs.all { it in c.genres } && (yr.isEmpty() || c.year.toString() == yr) && (st.isEmpty() || c.status == st)
            }
            if (r.isEmpty()) EmptyBox("No results", "Nothing matches. Try fewer filters or check the spelling.")
            else {
                Text("${r.size} result${if (r.size > 1) "s" else ""}", Modifier.padding(bottom = 10.dp), color = NavOff, fontSize = 13.sp)
                TileGrid(r, vm)
            }
        }
    }
}
