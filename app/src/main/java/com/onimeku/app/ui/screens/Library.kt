package com.onimeku.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.onimeku.app.data.ListStatus
import com.onimeku.app.ui.AppViewModel
import com.onimeku.app.ui.Dl
import com.onimeku.app.ui.components.Badge
import com.onimeku.app.ui.components.ChipRow
import com.onimeku.app.ui.components.DividerLine
import com.onimeku.app.ui.components.EmptyState
import com.onimeku.app.ui.components.GlassButton
import com.onimeku.app.ui.components.IconCircle
import com.onimeku.app.ui.components.NavPad
import com.onimeku.app.ui.components.Poster
import com.onimeku.app.ui.components.PosterCard
import com.onimeku.app.ui.components.PrimaryButton
import com.onimeku.app.ui.components.ScreenTitle
import com.onimeku.app.ui.components.SectionCard
import com.onimeku.app.ui.components.SectionHeader
import com.onimeku.app.ui.components.StatBlock
import com.onimeku.app.ui.theme.Crimson
import com.onimeku.app.ui.theme.DeepRed
import com.onimeku.app.ui.theme.Gold
import com.onimeku.app.ui.theme.Radius
import com.onimeku.app.ui.theme.Space
import com.onimeku.app.ui.theme.Surface1
import com.onimeku.app.ui.theme.Surface2
import com.onimeku.app.ui.theme.Surface3
import com.onimeku.app.ui.theme.TextSecondary
import com.onimeku.app.ui.theme.TextTertiary
import com.onimeku.app.ui.theme.Wine
import java.util.Calendar
import androidx.compose.material.icons.rounded.Star
import com.onimeku.app.ui.components.OptionSheet

// ── Baris list bersama (Rilis, My List, Unduhan) ─────────────────────────────
@Composable
private fun ListRow(
    a: Anime,
    sub: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    extra: @Composable ColumnScope.() -> Unit = {},
    trailing: @Composable RowScope.() -> Unit = {}
) = Row(
    modifier.padding(vertical = Space.xxs).fillMaxWidth()
        .clip(RoundedCornerShape(Radius.lg)).background(Surface1)
        .border(1.dp, Color.White.copy(.04f), RoundedCornerShape(Radius.lg))
        .clickable(onClick = onClick).padding(Space.sm),
    verticalAlignment = Alignment.CenterVertically
) {
    Poster(a, Modifier.size(58.dp, 82.dp).clip(RoundedCornerShape(Radius.sm)))
    Column(Modifier.padding(horizontal = Space.md).weight(1f)) {
        Text(a.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(sub, color = TextSecondary, fontSize = 12.sp, maxLines = 1)
        extra()
    }
    trailing()
}

// ── Kalender rilis ───────────────────────────────────────────────────────────
@Composable
fun ReleaseScreen(vm: AppViewModel, onOpen: (Int) -> Unit) {
    val remind by vm.remind.collectAsState()
    val now = remember { Calendar.getInstance() }
    val todayIndex = remember { now.get(Calendar.DAY_OF_WEEK) - 1 }
    var day by remember { mutableIntStateOf(todayIndex) }
    val names = listOf("Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab")
    val months = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")

    fun dateFor(i: Int): Calendar = (now.clone() as Calendar).apply {
        add(Calendar.DAY_OF_YEAR, i - todayIndex)
    }

    val selected = dateFor(day)
    val todays = AnimeRepository.schedule.filter { it.day == day }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = NavPad)) {
        item {
            ScreenTitle(
                "Kalender Rilis",
                "Jadwal tayang anime minggu ini"
            ) { Badge("${todays.size} judul", Crimson.copy(.16f), Crimson) }
        }
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Space.lg),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (i in 0..6) {
                    val d = dateFor(i)
                    val sel = i == day
                    Column(
                        Modifier.weight(1f)
                            .clip(RoundedCornerShape(Radius.md))
                            .background(if (sel) Crimson else Surface1)
                            .border(
                                1.dp,
                                if (sel) Crimson else Color.White.copy(.07f),
                                RoundedCornerShape(Radius.md)
                            )
                            .clickable { day = i }
                            .padding(vertical = Space.sm),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            names[i], fontSize = 10.sp,
                            color = if (sel) Color.White else TextSecondary
                        )
                        Text(
                            "${d.get(Calendar.DAY_OF_MONTH)}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            Modifier.padding(top = 3.dp).size(4.dp).clip(CircleShape)
                                .background(
                                    when {
                                        sel -> Color.White
                                        i == todayIndex -> Crimson
                                        else -> Color.Transparent
                                    }
                                )
                        )
                    }
                }
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().padding(
                    start = Space.lg, end = Space.lg, top = Space.lg, bottom = Space.xs
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${names[day]}, ${selected.get(Calendar.DAY_OF_MONTH)} ${months[selected.get(Calendar.MONTH)]}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                if (day == todayIndex) Badge("Hari ini", Crimson, Color.White)
            }
        }
        if (todays.isEmpty()) item {
            EmptyState(
                Icons.Rounded.EventBusy,
                "Belum ada rilis",
                "Tidak ada anime yang tayang di hari ini. Coba hari lain."
            )
        }
        items(todays, key = { it.anime.id }) { r ->
            ListRow(
                r.anime,
                "Episode ${r.episode} · ${r.time} WIB",
                { onOpen(r.anime.id) },
                Modifier.padding(horizontal = Space.lg),
                trailing = {
                IconCircle(
                    if (r.anime.id in remind) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsNone,
                    "Ingatkan",
                    bg = if (r.anime.id in remind) Crimson else Surface3,
                    size = 38.dp
                ) { vm.toggleRemind(r.anime.id) }
            })
        }
    }
}

// ── My List ──────────────────────────────────────────────────────────────────
@Composable
fun MyListScreen(vm: AppViewModel, onOpen: (Int) -> Unit, onBrowse: () -> Unit) {
    val status by vm.status.collectAsState()
    val fav by vm.fav.collectAsState()
    var filter by remember { mutableStateOf("Semua") }
    var asGrid by remember { mutableStateOf(true) }

    val list = AnimeRepository.all.filter { a ->
        when (filter) {
            "Favorit" -> a.id in fav
            "Menonton" -> status[a.id] == ListStatus.Watching
            "Rencana" -> status[a.id] == ListStatus.Plan
            "Selesai" -> status[a.id] == ListStatus.Done
            else -> a.id in status || a.id in fav
        }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenTitle("My List", "${list.size} judul") {
            IconCircle(
                if (asGrid) Icons.AutoMirrored.Rounded.ViewList else Icons.Rounded.GridView,
                "Ubah tampilan", bg = Surface2, onClick = { asGrid = !asGrid }
            )
        }
        ChipRow(
            listOf("Semua", "Menonton", "Rencana", "Selesai", "Favorit"),
            filter
        ) { filter = it }
        Spacer(Modifier.height(Space.xs))
        LazyVerticalGrid(
            GridCells.Fixed(3),
            Modifier.weight(1f),
            contentPadding = PaddingValues(start = Space.lg, end = Space.lg, bottom = NavPad),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (list.isEmpty()) {
                item(span = { GridItemSpan(3) }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        EmptyState(
                            Icons.Rounded.BookmarkBorder,
                            "Masih kosong",
                            "Tambahkan anime ke My List atau favorit untuk melihatnya di sini."
                        )
                        PrimaryButton("Jelajahi anime", onClick = onBrowse)
                    }
                }
            } else if (asGrid) {
                items(list, key = { it.id }) { a ->
                    PosterCard(a, Modifier.fillMaxWidth()) { onOpen(a.id) }
                }
            } else {
                items(list, key = { it.id }, span = { GridItemSpan(3) }) { a ->
                    ListRow(
                        a,
                        a.genres.joinToString(" · "),
                        { onOpen(a.id) },
                        Modifier.padding(horizontal = 0.dp),
                        extra = {
                            Column(Modifier.padding(top = Space.xs)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(Space.xs),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Badge(status[a.id]?.label ?: "Favorit", Wine, Color(0xFFFFD3D8))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Star, null, Modifier.size(12.dp), tint = Gold)
                                        Text(
                                            " ${"%.1f".format(a.rating)}",
                                            fontSize = 11.sp, color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

// ── Unduhan ──────────────────────────────────────────────────────────────────
@Composable
fun DownloadScreen(vm: AppViewModel, onOpen: (Int) -> Unit, onBrowse: () -> Unit) {
    val dl by vm.downloads.collectAsState()
    var confirmClear by remember { mutableStateOf(false) }
    val used = dl.size * .4f
    val pct = (used / 64f).coerceAtLeast(.01f)

    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false },
        title = { Text("Hapus semua unduhan?") },
        text = { Text("File yang tersimpan di perangkat akan dihapus dan tidak bisa dikembalikan.") },
        confirmButton = {
            TextButton({
                vm.clearDownloads()
                confirmClear = false
            }) { Text("Ya, hapus", color = Crimson, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton({ confirmClear = false }) { Text("Batal", color = TextSecondary) }
        },
        containerColor = Surface2
    )

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = NavPad)) {
        item {
            ScreenTitle("Unduhan", "${dl.size} judul tersimpan") {
                if (dl.isNotEmpty()) TextButton({ confirmClear = true }) {
                    Text("Hapus semua", color = Crimson, fontSize = 13.sp)
                }
            }
        }
        item {
            Column(
                Modifier.padding(horizontal = Space.lg).fillMaxWidth()
                    .clip(RoundedCornerShape(Radius.xl)).background(Surface1)
                    .border(1.dp, Color.White.copy(.05f), RoundedCornerShape(Radius.xl))
                    .padding(Space.lg)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CloudDownload, null, tint = Crimson, modifier = Modifier.size(20.dp))
                    Text(
                        "Penyimpanan perangkat",
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = Space.sm).weight(1f)
                    )
                    Text(
                        "%.1f GB / 64 GB".format(used),
                        color = TextSecondary, fontSize = 12.sp
                    )
                }
                LinearProgressIndicator(
                    { pct },
                    Modifier.fillMaxWidth().padding(top = Space.md).height(6.dp).clip(CircleShape),
                    color = Crimson,
                    trackColor = Surface3
                )
                Text(
                    "Sisa ${"%.1f".format(64f - used)} GB · unduhan dihapus otomatis bila penyimpanan penuh",
                    color = TextTertiary, fontSize = 11.sp,
                    modifier = Modifier.padding(top = Space.sm)
                )
            }
        }
        item { Spacer(Modifier.height(Space.md)) }

        if (dl.isEmpty()) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    EmptyState(
                        Icons.Rounded.CloudDownload,
                        "Belum ada unduhan",
                        "Unduh episode dari halaman detail untuk menonton tanpa internet."
                    )
                    PrimaryButton("Jelajahi anime", onClick = onBrowse)
                }
            }
        }

        items(dl.keys.toList(), key = { it }) { id ->
            val a = AnimeRepository.byId(id)
            val d = dl[id] ?: Dl()
            ListRow(
                a,
                when {
                    d.p >= 100 -> "Selesai · 400 MB"
                    d.paused -> "Dijeda · ${d.p}% dari 400 MB"
                    else -> "Mengunduh · ${d.p}% dari 400 MB"
                },
                { onOpen(id) },
                Modifier.padding(horizontal = Space.lg),
                extra = {
                    if (d.p < 100) LinearProgressIndicator(
                        { d.p / 100f },
                        Modifier.fillMaxWidth().padding(top = Space.xs).height(4.dp).clip(CircleShape),
                        color = Crimson,
                        trackColor = Surface3
                    )
                },
                trailing = {
                Row(
                    Modifier.padding(top = Space.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (d.p < 100) {
                        IconCircle(
                            if (d.paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                            if (d.paused) "Lanjutkan" else "Jeda",
                            bg = Surface3, size = 36.dp
                        ) { vm.togglePause(id) }
                        Spacer(Modifier.width(Space.sm))
                    }
                    IconCircle(
                        Icons.Rounded.DeleteOutline, "Hapus",
                        bg = Surface3, size = 36.dp
                    ) { vm.removeDownload(id) }
                }
            })
        }
    }
}

// ── Notifikasi ───────────────────────────────────────────────────────────────
@Composable
fun NotifScreen(vm: AppViewModel, onBack: () -> Unit) {
    val list by vm.notif.collectAsState()
    val unread = list.filter { !it.read }
    val read = list.filter { it.read }

    // Tandai terbaca saat pengguna meninggalkan layar (badge di Beranda ikut hilang).
    DisposableEffect(Unit) { onDispose { vm.markNotifRead() } }

    LazyColumn(
        Modifier.fillMaxSize().navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = Space.xxl)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding()
                    .padding(horizontal = Space.lg, vertical = Space.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconCircle(
                    Icons.AutoMirrored.Rounded.ArrowBack, "Kembali",
                    bg = Surface2, onClick = onBack
                )
                Column(Modifier.weight(1f).padding(horizontal = Space.md)) {
                    Text("Notifikasi", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${unread.size} belum dibaca",
                        color = TextSecondary, fontSize = 12.sp
                    )
                }
                if (unread.isNotEmpty()) TextButton({ vm.markNotifRead() }) {
                    Text("Tandai dibaca", color = Crimson, fontSize = 12.sp)
                }
            }
        }
        if (list.isEmpty()) item {
            EmptyState(
                Icons.Rounded.NotificationsNone,
                "Tidak ada notifikasi",
                "Pengingat episode baru akan muncul di sini."
            )
        }
        if (unread.isNotEmpty()) {
            item { SectionHeader("Baru") }
            items(unread, key = { it.text }) { n ->
                NotifRow(n.text, false)
            }
        }
        if (read.isNotEmpty()) {
            item { SectionHeader("Sebelumnya") }
            items(read, key = { it.text }) { n ->
                NotifRow(n.text, true)
            }
        }
    }
}

@Composable
private fun NotifRow(text: String, read: Boolean) = Row(
    Modifier.padding(horizontal = Space.lg, vertical = Space.xxs)
        .fillMaxWidth()
        .clip(RoundedCornerShape(Radius.lg))
        .background(if (read) Surface1 else Crimson.copy(.08f))
        .border(
            1.dp,
            if (read) Color.White.copy(.04f) else Crimson.copy(.25f),
            RoundedCornerShape(Radius.lg)
        )
        .padding(Space.md),
    verticalAlignment = Alignment.CenterVertically
) {
    Box(
        Modifier.size(40.dp).clip(CircleShape)
            .background(if (read) Surface3 else Crimson.copy(.9f)),
        Alignment.Center
    ) { Icon(Icons.Rounded.Notifications, null, Modifier.size(19.dp)) }
    Column(Modifier.padding(start = Space.md).weight(1f)) {
        Text(text, fontWeight = if (read) FontWeight.Normal else FontWeight.SemiBold, fontSize = 13.sp)
        Text(
            if (read) "Sebelumnya" else "Baru saja",
            color = if (read) TextTertiary else Crimson,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
    if (!read) Box(Modifier.size(7.dp).clip(CircleShape).background(Crimson))
}

// ── Profil ───────────────────────────────────────────────────────────────────
@Composable
fun ProfileScreen(vm: AppViewModel, onLogout: () -> Unit) {
    val st by vm.settings.collectAsState()
    val status by vm.status.collectAsState()
    val fav by vm.fav.collectAsState()
    val hist by vm.history.collectAsState()
    var sheet by remember { mutableStateOf("") }
    var confirmLogout by remember { mutableStateOf(false) }

    if (confirmLogout) AlertDialog(
        onDismissRequest = { confirmLogout = false },
        title = { Text("Keluar dari akun?") },
        text = { Text("Kamu bisa masuk lagi kapan saja. Data My List dan riwayat tetap tersimpan.") },
        confirmButton = {
            TextButton({
                confirmLogout = false
                onLogout()
            }) { Text("Keluar", color = Crimson, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton({ confirmLogout = false }) { Text("Batal", color = TextSecondary) }
        },
        containerColor = Surface2
    )

    if (sheet == "q") OptionSheet(
        "Kualitas video", listOf("480p", "720p", "1080p"), st.quality,
        { q ->
            vm.setSettings { it.copy(quality = q) }
            sheet = ""
        },
        { sheet = "" }
    )
    if (sheet == "s") OptionSheet(
        "Bahasa subtitle", listOf("Indonesia", "English", "Mati"), st.subtitle,
        { s ->
            vm.setSettings { it.copy(subtitle = s) }
            sheet = ""
        },
        { sheet = "" }
    )

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = NavPad),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(Modifier.statusBarsPadding().height(Space.xxl))
            Box(
                Modifier.size(96.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Crimson, DeepRed))),
                Alignment.Center
            ) {
                Icon(Icons.Rounded.Person, null, Modifier.size(48.dp), tint = Color.White.copy(.92f))
            }
            Text(
                "Pengguna Onimeku",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = Space.md)
            )
            Text(
                "Member Gratis",
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = Space.lg)
            )
        }
        item {
            Row(
                Modifier.padding(horizontal = Space.lg).fillMaxWidth()
                    .clip(RoundedCornerShape(Radius.xl)).background(Surface1)
                    .border(1.dp, Color.White.copy(.05f), RoundedCornerShape(Radius.xl))
                    .padding(vertical = Space.lg)
            ) {
                StatBlock("${hist.size}", "Ditonton", Modifier.weight(1f))
                StatDivider()
                StatBlock("${status.size}", "My List", Modifier.weight(1f))
                StatDivider()
                StatBlock("${fav.size}", "Favorit", Modifier.weight(1f))
            }
        }
        item {
            SectionCard("Pemutaran") {
                SwitchRow("Putar otomatis episode berikutnya", st.autoplay) { v ->
                    vm.setSettings { it.copy(autoplay = v) }
                }
                DividerLine(Modifier.padding(horizontal = Space.lg))
                ValueRow("Kualitas video", st.quality) { sheet = "q" }
                DividerLine(Modifier.padding(horizontal = Space.lg))
                ValueRow("Bahasa subtitle", st.subtitle) { sheet = "s" }
            }
        }
        item {
            SectionCard("Unduhan") {
                SwitchRow("Unduh hanya via Wi-Fi", st.wifi) { v ->
                    vm.setSettings { it.copy(wifi = v) }
                }
                DividerLine(Modifier.padding(horizontal = Space.lg))
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = Space.lg, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Wifi, null, tint = TextTertiary, modifier = Modifier.size(18.dp))
                    Text(
                        "Kualitas unduhan",
                        modifier = Modifier.padding(start = Space.sm).weight(1f),
                        fontSize = 14.sp
                    )
                    Text(st.quality, color = TextSecondary, fontSize = 13.sp)
                }
            }
        }
        item {
            SectionCard("Notifikasi") {
                SwitchRow("Notifikasi episode baru", st.push) { v ->
                    vm.setSettings { it.copy(push = v) }
                }
            }
        }
        item {
            SectionCard("Tentang") {
                ValueRow("Versi aplikasi", "1.0.0 (v2)", showChevron = false)
                DividerLine(Modifier.padding(horizontal = Space.lg))
                ValueRow("Tentang Onimeku", "v2", showChevron = false)
            }
        }
        item {
            Spacer(Modifier.height(Space.xxl))
            GlassButton(
                "Keluar",
                Modifier.padding(horizontal = Space.lg).fillMaxWidth(),
                Icons.AutoMirrored.Rounded.Logout
            ) { confirmLogout = true }
            Text(
                "Onimeku v1.0.0 · dibuat dengan ❤ untuk pecinta anime",
                color = TextTertiary, fontSize = 11.sp,
                modifier = Modifier.padding(top = Space.lg, bottom = Space.md)
            )
        }
    }
}

@Composable
private fun StatDivider() = Box(
    Modifier.height(34.dp).width(1.dp).background(Color.White.copy(.07f))
)

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) = Row(
    Modifier.fillMaxWidth().padding(start = Space.lg, end = Space.md, top = Space.xs, bottom = Space.xs),
    verticalAlignment = Alignment.CenterVertically
) {
    Text(label, Modifier.weight(1f), fontSize = 14.sp)
    Switch(
        checked, onChange,
        colors = SwitchDefaults.colors(
            checkedTrackColor = Crimson,
            checkedThumbColor = Color.White,
            uncheckedTrackColor = Surface3,
            uncheckedThumbColor = TextSecondary,
            uncheckedBorderColor = Color.White.copy(.1f)
        )
    )
}

@Composable
private fun ValueRow(
    label: String,
    value: String,
    showChevron: Boolean = true,
    onClick: () -> Unit = {}
) = Row(
    Modifier.fillMaxWidth().clickable(onClick = onClick)
        .padding(horizontal = Space.lg, vertical = 15.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    Text(label, Modifier.weight(1f), fontSize = 14.sp)
    Text(value, color = TextSecondary, fontSize = 13.sp)
    if (showChevron) Icon(
        Icons.Rounded.ChevronRight, null,
        Modifier.padding(start = Space.xxs).size(16.dp), tint = TextTertiary
    )
}
