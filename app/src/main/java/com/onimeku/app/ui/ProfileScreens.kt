package com.onimeku.app.ui

import kotlinx.coroutines.launch
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onimeku.app.AppVM
import com.onimeku.app.I18n
import com.onimeku.app.Notifier
import com.onimeku.app.Route
import com.onimeku.app.data.Catalog

/** Avatar bulat: foto akun bila ada (Google/Gmail), selain itu huruf awal di atas warna. Huruf tetap jadi cadangan selagi foto dimuat/gagal. */
@Composable
private fun AccountAvatar(url: String, initial: String, size: androidx.compose.ui.unit.Dp, bg: Color, border: Color, borderW: androidx.compose.ui.unit.Dp, fontSize: androidx.compose.ui.unit.TextUnit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val req = remember(url) {
        if (url.isBlank()) null
        else coil.request.ImageRequest.Builder(ctx).data(url.replace(Regex("=s\\d+-c$"), "=s256-c")).crossfade(false).build()
    }
    Box(Modifier.size(size).clip(CircleShape).background(bg).border(borderW, border, CircleShape), contentAlignment = Alignment.Center) {
        Text(initial, fontSize = fontSize, fontWeight = FontWeight.Bold)
        if (req != null) coil.compose.AsyncImage(req, null, Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
    }
}

private val avatarColors = listOf(Color(0xFFA6122D), Color(0xFF3B82F6), Color(0xFF10B981), Color(0xFFF59E0B))

@Composable
fun ProfileScreen(vm: AppVM) {
    var name by rememberSaveable { mutableStateOf("") }
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
    var renameIdx by rememberSaveable { mutableStateOf(-1) }
    var deleteIdx by rememberSaveable { mutableStateOf(-1) }
    var confirmOut by rememberSaveable { mutableStateOf(false) }
    var confirmDel by rememberSaveable { mutableStateOf(false) }
    val addNow = {
        if (vm.addProfile(name)) { name = ""; focus.clearFocus() }
    }
    Page(extraTop = 24.dp) {
        val acc = vm.s.account
        LaunchedEffect(acc?.userId) { vm.syncAccountProfile() }
        val hShape = RoundedCornerShape(24.dp)
        Row(
            Modifier.fillMaxWidth().padding(bottom = Sp.S24).clip(hShape).background(Brush.linearGradient(listOf(Color(0xFF3A1220), Bg2)))
                .border(1.dp, Line, hShape).padding(Sp.S20),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Sp.S16),
        ) {
            if (acc != null) AccountAvatar(acc.avatarUrl, acc.name.ifBlank { acc.email }.take(1).uppercase(), 68.dp, Red, RedText, 2.dp, Ty.Display)
            else Box(Modifier.size(68.dp).clip(CircleShape).background(Color(0x26FFFFFF)).border(2.dp, Line, CircleShape), contentAlignment = Alignment.Center) {
                Icon(AppIcons.User, null, Modifier.size(34.dp), tint = InkSoft)
            }
            Column(Modifier.weight(1f)) {
                Text(if (acc != null) acc.name.ifBlank { I18n.t("prof_signed_in", "Signed in") } else I18n.t("prof_guest", "Guest"), fontSize = Ty.Head2, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(acc?.email ?: I18n.t("prof_not_signed", "Not signed in"), Modifier.padding(top = Sp.S2), fontSize = Ty.Body, color = InkSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(I18n.t("prof_active", "Active profile: %s").format(vm.p.name), Modifier.padding(top = Sp.S4), fontSize = Ty.Small, color = NavOff, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        val st = remember(vm.p.days, vm.p.seen, vm.p.myList, Catalog.all) { com.onimeku.app.data.Stats.compute(vm.p) }
        Text(I18n.t("stats_title", "Your stats"), fontSize = Ty.Title, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = Sp.S12))
        CardBox(Modifier.padding(bottom = Sp.S24)) {
            Column(Modifier.padding(Sp.S16), verticalArrangement = Arrangement.spacedBy(Sp.S12)) {
                val mu = I18n.t("unit_min", "min"); val hu = I18n.t("unit_h", "h")
                fun days(n: Int) = "$n ${if (n == 1) I18n.t("unit_day", "day") else I18n.t("unit_days", "days")}"
                Row(horizontalArrangement = Arrangement.spacedBy(Sp.S12)) {
                    StatCell(com.onimeku.app.data.Stats.fmtDuration(st.monthSec, mu, hu), I18n.t("stat_month", "Watched this month"), Modifier.weight(1f))
                    StatCell("${st.episodes}", I18n.t("stat_eps", "Episodes finished"), Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Sp.S12)) {
                    StatCell(st.topGenre ?: "–", I18n.t("stat_genre", "Top genre"), Modifier.weight(1f))
                    StatCell(days(st.streak), I18n.t("stat_streak", "Daily streak"), Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Sp.S12)) {
                    StatCell(com.onimeku.app.data.Stats.fmtDuration(st.totalSec, mu, hu), I18n.t("stat_total", "Total watch time"), Modifier.weight(1f))
                    StatCell(days(maxOf(st.bestStreak, st.streak)), I18n.t("stat_best", "Best streak"), Modifier.weight(1f))
                }
                WeekChart(st.week)
            }
        }
        Text(I18n.t("prof_title", "Profiles"), fontSize = Ty.Title, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = Sp.S12))
        vm.s.profiles.forEachIndexed { i, pr ->
            val sel = i == vm.s.pi
            val bd = if (sel) Red else Line
            Row(Modifier.padding(bottom = Sp.S12), horizontalArrangement = Arrangement.spacedBy(Sp.S8), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.weight(1f).heightIn(min = 56.dp).clip(RoundedCornerShape(18.dp)).background(Bg2)
                        .border(1.dp, bd, RoundedCornerShape(18.dp))
                        .clickable { vm.switchProfile(i) }.padding(horizontal = Sp.S16, vertical = Sp.S12),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Sp.S12),
                ) {
                    AccountAvatar(if (i == 0) acc?.avatarUrl.orEmpty() else "", pr.name.take(1).uppercase(), 36.dp, avatarColors[i % 4], Color.Transparent, 0.dp, androidx.compose.ui.unit.TextUnit.Unspecified)
                    Column(Modifier.weight(1f)) {
                        Text(pr.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(I18n.t("prof_in_list", "%d in My List").format(pr.myList.size), fontSize = Ty.Caption, color = NavOff)
                    }
                    if (sel) Icon(AppIcons.Check, I18n.t("prof_active_short", "Active"), Modifier.size(20.dp), tint = RedText)
                }
                IconChip(AppIcons.Edit, I18n.t("prof_rename", "Rename")) { renameIdx = i }
                if (vm.s.profiles.size > 1 && !sel) IconChip(AppIcons.Trash, I18n.t("prof_delete", "Delete profile")) { deleteIdx = i }
            }
        }
        if (vm.s.profiles.size < com.onimeku.app.data.ProfileRules.MAX_PROFILES) {
            Row(Modifier.padding(bottom = Sp.S24), horizontalArrangement = Arrangement.spacedBy(Sp.S12), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).height(44.dp).clip(CircleShape).background(Bg2).border(1.dp, Line, CircleShape).padding(horizontal = Sp.S16),
                    contentAlignment = Alignment.CenterStart) {
                    BasicTextField(name, { if (it.length <= com.onimeku.app.data.ProfileRules.MAX_LEN) name = it }, singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = Ty.Subtitle), cursorBrush = SolidColor(Color.White),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { addNow() }),
                        decorationBox = { inner -> if (name.isEmpty()) Text(I18n.t("prof_new", "New profile"), color = NavOff); inner() })
                }
                Chip(I18n.t("prof_add", "Add")) { addNow() }
            }
        }

        // ---- dialog: ganti nama / hapus profil ----
        if (renameIdx in vm.s.profiles.indices) {
            val idx = renameIdx
            var txt by remember(idx) { mutableStateOf(vm.s.profiles[idx].name) }
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { renameIdx = -1 },
                title = { Text(I18n.t("prof_rename_title", "Rename profile")) },
                text = {
                    Box(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(14.dp)).background(Color(0x14FFFFFF)).padding(horizontal = Sp.S16), contentAlignment = Alignment.CenterStart) {
                        BasicTextField(txt, { if (it.length <= com.onimeku.app.data.ProfileRules.MAX_LEN) txt = it }, singleLine = true,
                            textStyle = TextStyle(color = Color.White, fontSize = Ty.Subtitle), cursorBrush = SolidColor(Color.White),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { if (vm.renameProfile(idx, txt)) renameIdx = -1 }))
                    }
                },
                confirmButton = { androidx.compose.material3.TextButton(onClick = { if (vm.renameProfile(idx, txt)) renameIdx = -1 }) { Text(I18n.t("prof_save", "Save"), color = RedText) } },
                dismissButton = { androidx.compose.material3.TextButton(onClick = { renameIdx = -1 }) { Text(I18n.t("prof_cancel", "Cancel")) } },
            )
        }
        if (deleteIdx in vm.s.profiles.indices) {
            val idx = deleteIdx
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { deleteIdx = -1 },
                title = { Text(I18n.t("prof_delete_title", "Delete profile \"%s\"?").format(vm.s.profiles[idx].name)) },
                text = { Text(I18n.t("prof_delete_body", "This profile's My List, progress and stats will be removed from this device.")) },
                confirmButton = { androidx.compose.material3.TextButton(onClick = { deleteIdx = -1; vm.delProfile(idx) }) { Text(I18n.t("prof_delete_confirm", "Delete"), color = RedText) } },
                dismissButton = { androidx.compose.material3.TextButton(onClick = { deleteIdx = -1 }) { Text(I18n.t("prof_cancel", "Cancel")) } },
            )
        }

        if (acc == null) CardBox(Modifier.padding(top = Sp.S20)) {
            Column(Modifier.padding(Sp.S20)) {
                Text(I18n.t("prof_signin_t", "Sign in to Onimeku"), fontSize = Ty.Title2, fontWeight = FontWeight.SemiBold)
                Text(I18n.t("prof_signin_b", "Save your watchlist and pick up where you left off on any device."), Modifier.padding(top = Sp.S8), color = InkSoft, fontSize = Ty.Body)
                Row(Modifier.padding(top = Sp.S16), horizontalArrangement = Arrangement.spacedBy(Sp.S12)) {
                    Btn(I18n.t("prof_signin", "Sign in"), BtnStyle.Solid, modifier = Modifier.weight(1f)) { vm.go(Route.Auth(false)) }
                    Btn(I18n.t("prof_signup", "Sign up"), BtnStyle.Play, modifier = Modifier.weight(1f)) { vm.go(Route.Auth(true)) }
                }
            }
        } else Box(Modifier.padding(top = Sp.S20)) {
            Column(verticalArrangement = Arrangement.spacedBy(Sp.S12)) {
                Btn(I18n.t("prof_signout", "Sign out"), BtnStyle.Outline, modifier = Modifier.fillMaxWidth()) { confirmOut = true }
                Btn(I18n.t("prof_delacc", "Delete account"), BtnStyle.Outline, modifier = Modifier.fillMaxWidth(), loading = vm.authBusy) { confirmDel = true }
            }
            if (confirmOut) androidx.compose.material3.AlertDialog(
                onDismissRequest = { confirmOut = false },
                title = { Text(I18n.t("prof_signout_title", "Sign out?")) },
                text = { Text(I18n.t("prof_signout_body", "Data on this device stays. Syncing stops until you sign in again.")) },
                confirmButton = { androidx.compose.material3.TextButton(onClick = { confirmOut = false; vm.signOut() }) { Text(I18n.t("prof_signout", "Sign out"), color = RedText) } },
                dismissButton = { androidx.compose.material3.TextButton(onClick = { confirmOut = false }) { Text(I18n.t("prof_cancel", "Cancel")) } },
            )
            if (confirmDel) androidx.compose.material3.AlertDialog(
                onDismissRequest = { confirmDel = false },
                title = { Text(I18n.t("prof_delacc_title", "Delete account?")) },
                text = { Text(I18n.t("prof_delacc_body", "This permanently deletes your account and the data synced to it. Data stored only on this device stays here.")) },
                confirmButton = { androidx.compose.material3.TextButton(onClick = { confirmDel = false; vm.deleteAccount() }) { Text(I18n.t("prof_delete_confirm", "Delete"), color = RedText) } },
                dismissButton = { androidx.compose.material3.TextButton(onClick = { confirmDel = false }) { Text(I18n.t("prof_cancel", "Cancel")) } },
            )
        }

        CardBox(Modifier.padding(top = Sp.S20)) {
            Row(Modifier.fillMaxWidth().clickable { vm.go(Route.Downloads) }.padding(horizontal = Sp.S16, vertical = Sp.S16),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Sp.S12)) {
                Box(Modifier.size(34.dp).clip(CircleShape).background(Red.copy(alpha = 0.28f)), contentAlignment = Alignment.Center) {
                    Icon(AppIcons.Download, null, Modifier.size(18.dp), tint = RedText)
                }
                Column(Modifier.weight(1f).padding(start = Sp.S2)) {
                    Text(I18n.t("tab_download", "Download"), fontSize = Ty.Subtitle, fontWeight = FontWeight.Medium)
                    Text(
                        if (vm.s.downloads.isEmpty()) I18n.t("dl_profile_empty", "Watch offline") else "${vm.s.downloads.size} ${I18n.t("dl_saved", "episodes saved")}",
                        fontSize = Ty.Small, color = NavOff,
                    )
                }
                Icon(AppIcons.ChevronRight, null, Modifier.size(20.dp), tint = NavOff)
            }
        }

        CardBox(Modifier.padding(top = Sp.S16)) {
            listOf("settings" to I18n.t("settings", "Settings"), "language" to I18n.t("language", "Language"), "help" to I18n.t("help", "Help Center"), "about" to I18n.t("about", "About Onimeku")).forEachIndexed { i, (k, label) ->
                if (i > 0) HLine()
                Row(Modifier.fillMaxWidth().clickable { vm.go(Route.Sub(k)) }.padding(horizontal = Sp.S16, vertical = Sp.S16),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Sp.S12)) {
                    Box(Modifier.size(34.dp).clip(CircleShape).background(Color(0x14FFFFFF)), contentAlignment = Alignment.Center) {
                        Icon(when (k) { "settings" -> AppIcons.Settings; "language" -> AppIcons.Globe; "help" -> AppIcons.Help; else -> AppIcons.Info }, null, Modifier.size(18.dp), tint = InkSoft)
                    }
                    Text(label, Modifier.weight(1f).padding(start = Sp.S2), fontSize = Ty.Subtitle, fontWeight = FontWeight.Medium)
                    if (k == "language") Text(if (vm.s.settings.lang == "id") "Bahasa Indonesia" else "English", color = NavOff, fontSize = Ty.Body)
                    Icon(AppIcons.ChevronRight, null, Modifier.size(20.dp), tint = NavOff)
                }
            }
        }
    }
}

@Composable
private fun Grp(text: String) =
    Text(text.uppercase(), Modifier.padding(start = Sp.S2, top = Sp.S24, bottom = Sp.S8), fontSize = Ty.Caption, fontWeight = FontWeight.SemiBold, color = NavOff, letterSpacing = 1.sp)

/** Baris pengaturan pilihan tunggal: judul + deskripsi + deretan chip yang bisa digeser bila sempit. */
@Composable
private fun ChoiceRow(title: String, desc: String, options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = Sp.S16, vertical = Sp.S12)) {
        Text(title, fontSize = Ty.Subtitle, fontWeight = FontWeight.Medium)
        Text(desc, fontSize = Ty.Small, color = NavOff, modifier = Modifier.padding(top = Sp.S2))
        Row(Modifier.padding(top = Sp.S12).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Sp.S8)) {
            options.forEach { (k, l) -> Chip(l, on = selected == k, height = 40.dp) { onSelect(k) } }
        }
    }
}

@Composable
private fun SwitchRow(title: String, desc: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    // Seluruh baris bisa diketuk (target besar) dan dibaca TalkBack sebagai satu sakelar.
    Row(Modifier.fillMaxWidth().toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = onChange)
        .padding(horizontal = Sp.S16, vertical = Sp.S12).graphicsAlpha(if (enabled) 1f else 0.45f),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Sp.S16)) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = Ty.Subtitle, fontWeight = FontWeight.Medium)
            Text(desc, fontSize = Ty.Small, color = NavOff, modifier = Modifier.padding(top = Sp.S2))
        }
        Switch(checked, null, enabled = enabled, colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White, checkedTrackColor = Red, checkedBorderColor = Red,
            uncheckedThumbColor = Color.White, uncheckedTrackColor = Color(0xFF3A3E50), uncheckedBorderColor = Color(0xFF3A3E50),
            disabledCheckedThumbColor = Color.White, disabledCheckedTrackColor = Red, disabledUncheckedThumbColor = Color.White, disabledUncheckedTrackColor = Color(0xFF3A3E50)))
    }
}

private fun Modifier.graphicsAlpha(a: Float): Modifier = if (a >= 1f) this else this.then(Modifier.graphicsLayer { alpha = a })

/** Contoh teks subtitle dengan gaya terpilih (warna, ukuran, latar). */
@Composable
private fun SubPreview(set: com.onimeku.app.data.Settings) {
    Box(Modifier.fillMaxWidth().padding(horizontal = Sp.S16, vertical = Sp.S12).height(84.dp).clip(RoundedCornerShape(14.dp))
        .background(Brush.verticalGradient(listOf(Color(0xFF2B3350), Color(0xFF12141C)))), contentAlignment = Alignment.BottomCenter) {
        if (set.subSystem) Text(I18n.t("sub_sys_d", "Follows Android caption settings (size, color, edge)."), Modifier.padding(Sp.S12), fontSize = Ty.Small, color = InkSoft, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        else Text(I18n.t("sub_preview", "Sample subtitle text"),
            Modifier.padding(bottom = Sp.S12).let { if (set.subBg) it.background(Color(0xB3000000)).padding(horizontal = Sp.S8, vertical = Sp.S2) else it },
            fontSize = (15f * subScaleOf(set.subSize)).sp, color = Color(subColorOf(set.subColor)), fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SubScreen(vm: AppVM, key: String) {
    val ensure = LocalEnsureNotif.current
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val set = vm.s.settings
    val titles = mapOf("settings" to I18n.t("settings", "Settings"), "language" to I18n.t("language", "Language"), "help" to I18n.t("help", "Help Center"), "about" to I18n.t("about", "About Onimeku"),
        "terms" to I18n.t("terms", "Terms of Service"), "privacy" to I18n.t("privacy", "Privacy Policy"), "licenses" to I18n.t("licenses", "Licenses"))
    Page {
        SubHead(titles[key] ?: "", vm)
        when (key) {
            "settings" -> {
                Grp(I18n.t("grp_playback", "Playback"))
                CardBox {
                    SwitchRow(I18n.t("autoplay_t", "Autoplay next episode"), I18n.t("autoplay_d", "Start the next episode when one ends."), set.autoplay) { v -> vm.setSettings { it.copy(autoplay = v) } }
                    HLine()
                    SwitchRow(I18n.t("skip_intro_t", "Skip intro"), I18n.t("skip_intro_d", "Show a button to skip opening songs."), set.skipIntro) { v -> vm.setSettings { it.copy(skipIntro = v) } }
                    HLine()
                    // Lompat otomatis hanya bekerja bila tombol lewati intro aktif (lihat SkipLogic.autoSkipTarget).
                    SwitchRow(I18n.t("auto_skip_t", "Auto-skip intro"), I18n.t("auto_skip_d", "Jump past the opening automatically when the episode has intro markers."), set.autoSkipIntro, enabled = set.skipIntro) { v -> vm.setSettings { it.copy(autoSkipIntro = v) } }
                    HLine()
                    SwitchRow(I18n.t("bg_play_t", "Background playback"), I18n.t("bg_play_d", "Keep audio playing with lock-screen controls when you leave the app. It stops if you swipe the app away from recents."), set.bgPlay) { v -> vm.setSettings { it.copy(bgPlay = v) } }
                    HLine()
                    SwitchRow(I18n.t("pip_t", "Picture-in-picture"), I18n.t("pip_d", "Keep watching in a small window when you press Home while a video is playing."), set.autoPip) { v -> vm.setSettings { it.copy(autoPip = v) } }
                    HLine()
                    ChoiceRow(
                        I18n.t("quality_def_t", "Default quality"), I18n.t("quality_def_d", "Auto adapts to your connection. A fixed quality is used when the episode has it."),
                        listOf("Auto", "1080p", "720p", "480p").let { base -> if (set.quality !in base) base + set.quality else base }.map { it to it },
                        set.quality,
                    ) { k -> vm.setSettings { it.copy(quality = k) } }
                    HLine()
                    ChoiceRow(
                        I18n.t("dub_def_t", "Default dubbing"), I18n.t("dub_def_d", "Used automatically when an episode offers that audio track."),
                        listOf("auto" to "Auto", "ja" to I18n.t("dub_ja", "Japanese"), "id" to I18n.t("dub_id", "Indonesian"), "en" to I18n.t("dub_en", "English")).let { base ->
                            // Dubbing lain yang pernah dipilih di pemutar (mis. Korea) tetap tampil sebagai opsi aktif.
                            if (set.dubLang != "auto" && base.none { it.first == set.dubLang }) base + (set.dubLang to (dubLangName(set.dubLang) ?: set.dubLang)) else base
                        },
                        set.dubLang,
                    ) { k -> vm.setSettings { it.copy(dubLang = k) } }
                }
                Grp(I18n.t("subtitles", "Subtitles"))
                CardBox {
                    ChoiceRow(
                        I18n.t("sub_def_t", "Default subtitle language"), I18n.t("sub_def_d", "Auto follows the app language. Used when the episode has that track."),
                        listOf("auto" to "Auto", "id" to I18n.t("dub_id", "Indonesian"), "en" to I18n.t("dub_en", "English"), "off" to I18n.t("ctl_off", "Off")).let { base ->
                            if (set.subLang !in base.map { it.first }) base + (set.subLang to (dubLangName(set.subLang) ?: set.subLang)) else base
                        },
                        set.subLang,
                    ) { k -> vm.setSettings { it.copy(subLang = k) } }
                    HLine()
                    ChoiceRow(
                        I18n.t("sub_size_t", "Subtitle size"), I18n.t("sub_size_d", "Applies to the player's subtitles."),
                        listOf("small" to I18n.t("sub_sz_small", "Small"), "normal" to I18n.t("normal", "Normal"), "large" to I18n.t("sub_sz_large", "Large"), "xl" to I18n.t("sub_sz_xl", "Extra large")),
                        set.subSize,
                    ) { k -> vm.setSettings { it.copy(subSize = k) } }
                    HLine()
                    ChoiceRow(
                        I18n.t("sub_color_t", "Subtitle color"), I18n.t("sub_color_d", "Text color for subtitles."),
                        listOf("white" to I18n.t("sub_c_white", "White"), "yellow" to I18n.t("sub_c_yellow", "Yellow")), set.subColor,
                    ) { k -> vm.setSettings { it.copy(subColor = k) } }
                    HLine()
                    SwitchRow(I18n.t("sub_bg", "Black background"), I18n.t("sub_bg_d", "Put a dark box behind the text for easier reading."), set.subBg) { v -> vm.setSettings { it.copy(subBg = v) } }
                    HLine()
                    SwitchRow(I18n.t("sub_sys_t", "Use system caption style"), I18n.t("sub_sys_d", "Follows Android caption settings (size, color, edge)."), set.subSystem) { v -> vm.setSettings { it.copy(subSystem = v) } }
                    SubPreview(set)
                }
                Grp(I18n.t("grp_data", "Data and alerts"))
                CardBox {
                    SwitchRow(I18n.t("wifi_only_t", "Stream over Wi-Fi only"), I18n.t("wifi_only_d", "Avoid using mobile data."), set.wifi) { v -> vm.setSettings { it.copy(wifi = v) } }
                    HLine()
                    SwitchRow(I18n.t("auto_dl_t", "Auto-download next episode"), I18n.t("auto_dl_d", "While you watch on Wi-Fi, save the next episode for offline."), set.autoDlNext) { v -> vm.setSettings { it.copy(autoDlNext = v) } }
                    HLine()
                    SwitchRow(I18n.t("alerts_t", "New episode alerts"), I18n.t("alerts_d", "Get notified when your list updates."), set.alerts) { v ->
                        vm.setSettings { it.copy(alerts = v) }
                        if (v) ensure { g -> if (g) Notifier.show(ctx, I18n.t("alerts_on_t", "Alerts on"), I18n.t("alerts_on_d", "You will get new episode and reminder alerts")) }
                    }
                }
                // Penyimpanan: ringkasan unduhan + hapus semua (dengan konfirmasi).
                Grp(I18n.t("grp_storage", "Storage"))
                var confirmDl by rememberSaveable { mutableStateOf(false) }
                var confirmReset by rememberSaveable { mutableStateOf(false) }
                CardBox {
                    Row(Modifier.fillMaxWidth().padding(horizontal = Sp.S16, vertical = Sp.S12), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Sp.S12)) {
                        Column(Modifier.weight(1f)) {
                            Text(I18n.t("tab_download", "Download"), fontSize = Ty.Subtitle, fontWeight = FontWeight.Medium)
                            Text(if (vm.s.downloads.isEmpty()) I18n.t("dl_profile_empty", "Watch offline") else "${vm.s.downloads.size} ${I18n.t("dl_saved", "episodes saved")}", fontSize = Ty.Small, color = NavOff, modifier = Modifier.padding(top = Sp.S2))
                        }
                        if (vm.s.downloads.isNotEmpty()) Chip(I18n.t("dl_delete_all", "Delete all")) { confirmDl = true }
                    }
                }
                Box(Modifier.padding(top = Sp.S24)) { Btn(I18n.t("reset_settings", "Reset settings"), BtnStyle.Outline, modifier = Modifier.fillMaxWidth()) { confirmReset = true } }
                if (confirmDl) androidx.compose.material3.AlertDialog(
                    onDismissRequest = { confirmDl = false },
                    title = { Text(I18n.t("dl_delete_all_t", "Delete all downloads?")) },
                    text = { Text(I18n.t("dl_delete_all_b", "Saved episodes will be removed from this device. You can download them again later.")) },
                    confirmButton = { androidx.compose.material3.TextButton(onClick = { confirmDl = false; vm.removeAllDl() }) { Text(I18n.t("prof_delete_confirm", "Delete"), color = RedText) } },
                    dismissButton = { androidx.compose.material3.TextButton(onClick = { confirmDl = false }) { Text(I18n.t("prof_cancel", "Cancel")) } },
                )
                if (confirmReset) androidx.compose.material3.AlertDialog(
                    onDismissRequest = { confirmReset = false },
                    title = { Text(I18n.t("reset_settings_t", "Reset settings?")) },
                    text = { Text(I18n.t("reset_settings_b", "Playback, subtitle and alert options go back to their defaults. Your language, lists and downloads are kept.")) },
                    confirmButton = { androidx.compose.material3.TextButton(onClick = { confirmReset = false; vm.resetSettings() }) { Text(I18n.t("reset", "Reset"), color = RedText) } },
                    dismissButton = { androidx.compose.material3.TextButton(onClick = { confirmReset = false }) { Text(I18n.t("prof_cancel", "Cancel")) } },
                )
            }
            "language" -> {
                Text(I18n.t("choose_language", "Choose the language used in the app."), Modifier.padding(start = Sp.S4, bottom = Sp.S16), color = InkSoft)
                CardBox {
                    // Bahasa Indonesia kini BENAR-BENAR berfungsi;
                    // pilihan tersimpan di Settings.lang dan seluruh
                    // antarmuka utama mengikuti lewat I18n.
                    listOf("en" to "English", "id" to "Bahasa Indonesia").forEachIndexed { i, (code, label) ->
                        if (i > 0) HLine()
                        Row(Modifier.fillMaxWidth().clickable { vm.setSettings { st -> st.copy(lang = code) } }.padding(horizontal = Sp.S16, vertical = Sp.S16), verticalAlignment = Alignment.CenterVertically) {
                            Text(label, Modifier.weight(1f), fontSize = Ty.Subtitle)
                            if (vm.s.settings.lang == code) Icon(AppIcons.Check, null, Modifier.size(20.dp), tint = RedText)
                        }
                    }
                    HLine()
                    Row(Modifier.fillMaxWidth().padding(horizontal = Sp.S16, vertical = Sp.S16), verticalAlignment = Alignment.CenterVertically) {
                        Text("日本語", Modifier.weight(1f), fontSize = Ty.Subtitle, color = Color(0xA6FFFFFF))
                        Text(I18n.t("coming_soon", "Coming soon"), fontSize = Ty.Small, color = NavOff)
                    }
                }
            }
            "help" -> {
                Text(I18n.t("help_intro", "Quick answers to common questions."), Modifier.padding(start = Sp.S4, bottom = Sp.S16), color = InkSoft)
                CardBox {
                    val faq = listOf(
                        I18n.t("faq_q1", "How do I watch an episode offline?") to I18n.t("faq_a1", "Tap Download on the episode page while you are online. Find saved episodes in Profile > Download; they stay there until you remove them."),
                        I18n.t("faq_q2", "How do I add an anime to My List?") to I18n.t("faq_a2", "Tap the My List button on the anime you like. It will be saved in the My List tab, where you can filter and sort it."),
                        I18n.t("faq_q3", "An episode will not play. What can I do?") to I18n.t("faq_a3", "Check your connection, then lower the video quality with the gear icon in the player. If it still fails, use Report a problem, or close the app and open it again."),
                        I18n.t("faq_q4", "Why is some quality missing?") to I18n.t("faq_a4", "Not every episode is available in every quality. Auto picks the best one for your connection."),
                        I18n.t("faq_q5", "How do I get new episode alerts?") to I18n.t("faq_a5", "Turn on New episode alerts in Settings, or tap the bell on an episode in the Calendar for a reminder before it airs."),
                        I18n.t("faq_q6", "Can several people share one device?") to I18n.t("faq_a6", "Yes. Add up to 4 profiles in Profile. Each has its own My List, progress and stats."),
                        I18n.t("faq_q7", "How do I change the subtitles?") to I18n.t("faq_a7", "Tap CC in the player to pick a track. Set the default language, size and color in Settings > Subtitles."),
                    )
                    faq.forEachIndexed { i, (qq, aa) ->
                        if (i > 0) HLine()
                        var open by rememberSaveable { mutableStateOf(false) }
                        Column(Modifier.fillMaxWidth().clickable { open = !open }.padding(horizontal = Sp.S16, vertical = Sp.S16)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(qq, Modifier.weight(1f), fontWeight = FontWeight.Medium)
                                Text(if (open) "−" else "+", fontSize = Ty.Head, color = NavOff)
                            }
                            if (open) Text(aa, Modifier.padding(top = Sp.S12), color = InkSoft, fontSize = Ty.Body)
                        }
                    }
                }
            }
            "about" -> {
                Row(Modifier.padding(bottom = Sp.S16), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Sp.S16)) {
                    Logo(56.dp)
                    Column {
                        Text("Onimeku", fontSize = Ty.Head, fontWeight = FontWeight.SemiBold)
                        Text("${I18n.t("version", "Version")} ${com.onimeku.app.BuildConfig.VERSION_NAME}", color = NavOff)
                    }
                }
                Text(I18n.t("about_body", "Onimeku is a place to watch anime online. Browse top hits, follow new episodes, and keep a list of what you want to watch next."),
                    Modifier.padding(start = Sp.S4, bottom = Sp.S16), color = InkSoft)
                CardBox {
                    val ctxA = androidx.compose.ui.platform.LocalContext.current
                    Row(Modifier.fillMaxWidth().clickable {
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch { com.onimeku.app.data.ApiClient.uploadDiag("manual") }
                        val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_SUBJECT, "Onimeku diagnostics")
                            putExtra(android.content.Intent.EXTRA_TEXT, com.onimeku.app.data.CrashLog.read())
                        }
                        ctxA.startActivity(android.content.Intent.createChooser(send, "Send diagnostics").addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                    }.padding(horizontal = Sp.S16, vertical = Sp.S16), verticalAlignment = Alignment.CenterVertically) {
                        Text(I18n.t("send_diag", "Send diagnostics"), Modifier.weight(1f), fontSize = Ty.Subtitle)
                        Icon(AppIcons.ChevronRight, null, Modifier.size(20.dp), tint = NavOff)
                    }
                    HLine()
                    listOf("terms" to I18n.t("terms", "Terms of Service"), "privacy" to I18n.t("privacy", "Privacy Policy"), "licenses" to I18n.t("licenses", "Licenses")).forEachIndexed { i, (k2, l) ->
                        if (i > 0) HLine()
                        Row(Modifier.fillMaxWidth().clickable { vm.go(Route.Sub(k2)) }.padding(horizontal = Sp.S16, vertical = Sp.S16),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(l, Modifier.weight(1f), fontSize = Ty.Subtitle)
                            Icon(AppIcons.ChevronRight, null, Modifier.size(20.dp), tint = NavOff)
                        }
                    }
                }
            }
            "terms", "privacy", "licenses" -> LegalContent(key)
        }
    }
}

@Composable
private fun StatCell(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(Color(0x0FFFFFFF)).padding(horizontal = Sp.S16, vertical = Sp.S12),
    ) {
        Text(value, fontSize = Ty.Title2, fontWeight = FontWeight.Bold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        Text(label, Modifier.padding(top = Sp.S2), fontSize = Ty.Caption, color = NavOff, maxLines = 2)
    }
}

/** Grafik batang 7 hari terakhir (lama -> hari ini). Hari ini berwarna merah; tinggi batang relatif terhadap hari terpanjang. */
@Composable
private fun WeekChart(week: List<Int>) {
    val max = (week.maxOrNull() ?: 0).coerceAtLeast(1)
    val today = java.time.LocalDate.now()
    val loc = java.util.Locale(I18n.lang)
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0x0FFFFFFF)).padding(horizontal = Sp.S16, vertical = Sp.S12)) {
        val weekSec = week.sum()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(I18n.t("stat_week", "Last 7 days"), Modifier.weight(1f), fontSize = Ty.Caption, color = NavOff)
            if (weekSec > 0) Text(com.onimeku.app.data.Stats.fmtDuration(weekSec, I18n.t("unit_min", "min"), I18n.t("unit_h", "h")), fontSize = Ty.Caption, fontWeight = FontWeight.SemiBold, color = InkSoft)
        }
        Row(Modifier.fillMaxWidth().padding(top = Sp.S12), horizontalArrangement = Arrangement.spacedBy(Sp.S8), verticalAlignment = Alignment.Bottom) {
            week.forEachIndexed { i, sec ->
                val d = today.minusDays((6 - i).toLong())
                val h = if (sec <= 0) 4.dp else (8 + 44f * sec / max).dp
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Sp.S4)) {
                    Box(Modifier.width(18.dp).height(h).clip(RoundedCornerShape(5.dp)).background(if (i == 6 && sec > 0) Red else if (sec > 0) Color(0x66FFFFFF) else Color(0x1FFFFFFF)))
                    Text(d.dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, loc), fontSize = Ty.Micro, color = if (i == 6) InkSoft else NavOff, maxLines = 1)
                }
            }
        }
    }
}
