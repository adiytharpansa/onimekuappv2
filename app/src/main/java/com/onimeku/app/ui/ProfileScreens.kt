package com.onimeku.app.ui

import kotlinx.coroutines.launch
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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

private val avatarColors = listOf(Color(0xFFA6122D), Color(0xFF3B82F6), Color(0xFF10B981), Color(0xFFF59E0B))

@Composable
fun ProfileScreen(vm: AppVM) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var name by rememberSaveable { mutableStateOf("") }
    Page(extraTop = 24.dp) {
        val acc = vm.s.account
        val hShape = RoundedCornerShape(24.dp)
        Row(
            Modifier.fillMaxWidth().padding(bottom = 26.dp).clip(hShape).background(Brush.linearGradient(listOf(Color(0xFF3A1220), Bg2)))
                .border(1.dp, Line, hShape).padding(20.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(Modifier.size(68.dp).clip(CircleShape).background(if (acc != null) Red else Color(0x26FFFFFF)).border(2.dp, if (acc != null) RedText else Line, CircleShape), contentAlignment = Alignment.Center) {
                if (acc != null) Text(acc.email.take(1).uppercase(), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                else Icon(Icons.Outlined.Person, null, Modifier.size(34.dp), tint = InkSoft)
            }
            Column(Modifier.weight(1f)) {
                Text(if (acc != null) "Signed in" else "Guest", fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp)
                Text(acc?.email ?: "Not signed in", Modifier.padding(top = 2.dp), fontSize = 14.sp, color = InkSoft, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
        }
        Text("Profiles", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
        vm.s.profiles.forEachIndexed { i, pr ->
            val sel = i == vm.s.pi
            val bd by androidx.compose.animation.animateColorAsState(if (sel) Red else Line, androidx.compose.animation.core.tween(250), label = "profBd")
            Row(Modifier.padding(bottom = 10.dp).fadeSlideIn(minOf(i, 3) * 60), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.weight(1f).heightIn(min = 56.dp).clip(RoundedCornerShape(18.dp)).background(Bg2)
                        .border(1.dp, bd, RoundedCornerShape(18.dp))
                        .clickable { vm.switchProfile(i) }.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(Modifier.size(36.dp).clip(CircleShape).background(avatarColors[i % 4]), contentAlignment = Alignment.Center) {
                        Text(pr.name.take(1).uppercase(), fontWeight = FontWeight.Bold)
                    }
                    Text(pr.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    if (sel) Icon(Icons.Filled.Check, "Active", Modifier.size(20.dp), tint = RedText)
                }
                if (vm.s.profiles.size > 1 && !sel) IconChip(Icons.Outlined.Delete, "Delete profile") { vm.delProfile(i) }
            }
        }
        if (vm.s.profiles.size < 4) {
            Row(Modifier.padding(bottom = 26.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).height(44.dp).clip(CircleShape).background(Bg2).border(1.dp, Line, CircleShape).padding(horizontal = 18.dp),
                    contentAlignment = Alignment.CenterStart) {
                    BasicTextField(name, { if (it.length <= 12) name = it }, singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 15.sp), cursorBrush = SolidColor(Color.White),
                        decorationBox = { inner -> if (name.isEmpty()) Text("New profile", color = NavOff); inner() })
                }
                Chip("Add") {
                    if (name.isBlank()) vm.toast("Enter a name") else { vm.addProfile(name.trim()); name = "" }
                }
            }
        }


        if (acc == null) CardBox(Modifier.padding(top = 20.dp).fadeSlideIn(120)) {
            Column(Modifier.padding(20.dp)) {
                Text("Sign in to Onimeku", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Text("Save your watchlist and pick up where you left off on any device.", Modifier.padding(top = 6.dp), color = InkSoft, fontSize = 14.sp)
                Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Btn("Sign in", BtnStyle.Solid, modifier = Modifier.weight(1f)) { vm.go(Route.Auth(false)) }
                    Btn("Sign up", BtnStyle.Play, modifier = Modifier.weight(1f)) { vm.go(Route.Auth(true)) }
                }
            }
        } else Box(Modifier.padding(top = 20.dp)) {
            var confirmDel by remember { mutableStateOf(false) }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Btn("Sign out", BtnStyle.Outline, modifier = Modifier.fillMaxWidth()) { vm.signOut() }
                Btn("Delete account", BtnStyle.Outline, modifier = Modifier.fillMaxWidth()) { confirmDel = true }
            }
            if (confirmDel) androidx.compose.material3.AlertDialog(
                onDismissRequest = { confirmDel = false },
                title = { Text("Delete account?") },
                text = { Text("This permanently deletes your account and the data synced to it. Data stored only on this device stays here.") },
                confirmButton = { androidx.compose.material3.TextButton(onClick = { confirmDel = false; vm.deleteAccount() }) { Text("Delete", color = RedText) } },
                dismissButton = { androidx.compose.material3.TextButton(onClick = { confirmDel = false }) { Text("Cancel") } },
            )
        }

        CardBox(Modifier.padding(top = 20.dp).fadeSlideIn(160)) {
            Row(Modifier.fillMaxWidth().clickable { vm.go(Route.Downloads) }.padding(horizontal = 18.dp, vertical = 17.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(34.dp).clip(CircleShape).background(Red.copy(alpha = 0.28f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Download, null, Modifier.size(18.dp), tint = RedText)
                }
                Column(Modifier.weight(1f).padding(start = 2.dp)) {
                    Text(I18n.t("tab_download", "Download"), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Text(
                        if (vm.s.downloads.isEmpty()) I18n.t("dl_profile_empty", "Watch offline") else "${vm.s.downloads.size} ${I18n.t("dl_saved", "episodes saved")}",
                        fontSize = 12.5.sp, color = NavOff,
                    )
                }
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, Modifier.size(20.dp), tint = NavOff)
            }
        }

        CardBox(Modifier.padding(top = 16.dp).fadeSlideIn(200)) {
            listOf("settings" to I18n.t("settings", "Settings"), "language" to I18n.t("language", "Language"), "help" to I18n.t("help", "Help Center"), "about" to I18n.t("about", "About Onimeku")).forEachIndexed { i, (k, label) ->
                if (i > 0) HLine()
                Row(Modifier.fillMaxWidth().clickable { vm.go(Route.Sub(k)) }.padding(horizontal = 18.dp, vertical = 17.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(34.dp).clip(CircleShape).background(Color(0x14FFFFFF)), contentAlignment = Alignment.Center) {
                        Icon(when (k) { "settings" -> Icons.Outlined.Settings; "language" -> Icons.Outlined.Language; "help" -> Icons.Outlined.HelpOutline; else -> Icons.Outlined.Info }, null, Modifier.size(18.dp), tint = InkSoft)
                    }
                    Text(label, Modifier.weight(1f).padding(start = 2.dp), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    if (k == "language") Text(if (vm.s.settings.lang == "id") "Bahasa Indonesia" else "English", color = NavOff, fontSize = 14.sp)
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, Modifier.size(20.dp), tint = NavOff)
                }
            }
        }
    }
}

@Composable
private fun Grp(text: String) =
    Text(text.uppercase(), Modifier.padding(start = 2.dp, top = 22.dp, bottom = 8.dp), fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = NavOff, letterSpacing = 1.sp)

@Composable
private fun SwitchRow(title: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(desc, fontSize = 12.5.sp, color = NavOff, modifier = Modifier.padding(top = 2.dp))
        }
        Switch(checked, onChange, colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White, checkedTrackColor = Red, checkedBorderColor = Red,
            uncheckedThumbColor = Color.White, uncheckedTrackColor = Color(0xFF3A3E50), uncheckedBorderColor = Color(0xFF3A3E50)))
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
                    SwitchRow(I18n.t("bg_play_t", "Background playback"), I18n.t("bg_play_d", "Keep audio playing with lock-screen controls when you leave the app."), set.bgPlay) { v -> vm.setSettings { it.copy(bgPlay = v) } }
                }
                Grp(I18n.t("quality_s", "Video quality"))
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Bg2).border(1.dp, Line, RoundedCornerShape(18.dp)).padding(5.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Auto", "1080p", "720p", "480p").forEach { qv ->
                        val on = set.quality == qv
                        Box(Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(14.dp)).background(if (on) Red else Color.Transparent)
                            .clickable { vm.setSettings { it.copy(quality = qv) } }, contentAlignment = Alignment.Center) {
                            Text(qv, fontSize = 14.sp, color = if (on) Color.White else InkSoft)
                        }
                    }
                }
                Grp(I18n.t("grp_data", "Data and alerts"))
                CardBox {
                    SwitchRow(I18n.t("wifi_only_t", "Stream over Wi-Fi only"), I18n.t("wifi_only_d", "Avoid using mobile data."), set.wifi) { v -> vm.setSettings { it.copy(wifi = v) } }
                    HLine()
                    SwitchRow(I18n.t("alerts_t", "New episode alerts"), I18n.t("alerts_d", "Get notified when your list updates."), set.alerts) { v ->
                        vm.setSettings { it.copy(alerts = v) }
                        if (v) ensure { g -> if (g) Notifier.show(ctx, "Alerts on", "You will get new episode and reminder alerts") }
                    }
                }
            }
            "language" -> {
                Text(I18n.t("choose_language", "Choose the language used in the app."), Modifier.padding(start = 4.dp, bottom = 18.dp), color = InkSoft)
                CardBox {
                    // Bahasa Indonesia kini BENAR-BENAR berfungsi (debug.31);
                    // pilihan tersimpan di Settings.lang dan seluruh
                    // antarmuka utama mengikuti lewat I18n.
                    listOf("en" to "English", "id" to "Bahasa Indonesia").forEachIndexed { i, (code, label) ->
                        if (i > 0) HLine()
                        Row(Modifier.fillMaxWidth().clickable { vm.setSettings { st -> st.copy(lang = code) } }.padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(label, Modifier.weight(1f), fontSize = 16.sp)
                            if (vm.s.settings.lang == code) Icon(Icons.Filled.Check, null, Modifier.size(20.dp), tint = RedText)
                        }
                    }
                    HLine()
                    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("日本語", Modifier.weight(1f), fontSize = 16.sp, color = Color(0xA6FFFFFF))
                        Text(I18n.t("coming_soon", "Coming soon"), fontSize = 13.sp, color = NavOff)
                    }
                }
            }
            "help" -> {
                Text("Quick answers to common questions.", Modifier.padding(start = 4.dp, bottom = 18.dp), color = InkSoft)
                CardBox {
                    val faq = listOf(
                        "How do I watch an episode offline?" to "Open the Download tab and save an episode while you are online. Saved episodes stay there until you remove them.",
                        "How do I add an anime to My List?" to "Tap the My List button on the anime you like. It will be saved in the My List tab.",
                        "An episode will not play. What can I do?" to "Check your connection, then lower the video quality in Settings. If it still fails, close the app and open it again.",
                        "Why is some quality missing?" to "Not every episode is available in every quality. Auto picks the best one for your connection.",
                        "How do I get new episode alerts?" to "Turn on New episode alerts in Settings. You will be told when something in your list updates.",
                    )
                    faq.forEachIndexed { i, (qq, aa) ->
                        if (i > 0) HLine()
                        var open by rememberSaveable { mutableStateOf(false) }
                        Column(Modifier.fillMaxWidth().clickable { open = !open }.padding(horizontal = 18.dp, vertical = 16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(qq, Modifier.weight(1f), fontWeight = FontWeight.Medium)
                                Text(if (open) "−" else "+", fontSize = 22.sp, color = NavOff)
                            }
                            if (open) Text(aa, Modifier.padding(top = 10.dp), color = InkSoft, fontSize = 14.sp)
                        }
                    }
                }
            }
            "about" -> {
                Row(Modifier.padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Logo(56.dp)
                    Column {
                        Text("Onimeku", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                        Text("Version ${com.onimeku.app.BuildConfig.VERSION_NAME}", color = NavOff)
                    }
                }
                Text("Onimeku is a place to watch anime online. Browse top hits, follow new episodes, and keep a list of what you want to watch next.",
                    Modifier.padding(start = 4.dp, bottom = 18.dp), color = InkSoft)
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
                    }.padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Send diagnostics", Modifier.weight(1f), fontSize = 16.sp)
                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, Modifier.size(20.dp), tint = NavOff)
                    }
                    HLine()
                    listOf("terms" to "Terms of Service", "privacy" to "Privacy Policy", "licenses" to "Licenses").forEachIndexed { i, (k2, l) ->
                        if (i > 0) HLine()
                        Row(Modifier.fillMaxWidth().clickable { vm.go(Route.Sub(k2)) }.padding(horizontal = 18.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(l, Modifier.weight(1f), fontSize = 16.sp)
                            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, Modifier.size(20.dp), tint = NavOff)
                        }
                    }
                }
            }
            "terms", "privacy", "licenses" -> LegalContent(key)
        }
    }
}
