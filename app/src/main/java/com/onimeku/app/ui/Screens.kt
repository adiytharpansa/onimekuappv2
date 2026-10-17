package com.onimeku.app.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.util.UnstableApi
import com.onimeku.app.data.*
import com.onimeku.app.media.MediaKit
import kotlinx.coroutines.launch

@Composable private fun Group(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(title.uppercase(), Modifier.padding(start = 2.dp, top = 22.dp, bottom = 8.dp),
        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) { Column(content = content) }
}

@Composable private fun SwitchRow(title: String, sub: String, on: Boolean, last: Boolean = false, set: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.bodyLarge); Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Switch(on, set)
    }
    if (!last) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable private fun TopBar(title: String, onBack: (() -> Unit)?) = Row(Modifier.statusBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
    if (onBack != null) IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
    Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(start = if (onBack == null) 4.dp else 0.dp))
}

@OptIn(ExperimentalMaterial3Api::class) @UnstableApi @Composable
fun SettingsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current; val store = remember { SettingsStore.of(ctx) }; val scope = rememberCoroutineScope()
    val s by store.flow.collectAsState(Settings())
    val askNotif = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> scope.launch { store.set(K.alerts, ok) } }
    LaunchedEffect(s.wifiOnly) { MediaKit.setWifiOnly(ctx, s.wifiOnly) }   // berlaku juga untuk unduhan
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        TopBar("Settings", onBack)
        Group("Playback") {
            SwitchRow("Autoplay next episode", "Start the next episode when one ends.", s.autoplay) { v -> scope.launch { store.set(K.autoplay, v) } }
            SwitchRow("Skip intro", "Show a button to skip opening songs.", s.skipIntro, true) { v -> scope.launch { store.set(K.skipIntro, v) } }
        }
        Text("VIDEO QUALITY", Modifier.padding(start = 2.dp, top = 22.dp, bottom = 8.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val qs = listOf("Auto", "1080p", "720p", "480p")
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            qs.forEachIndexed { i, q -> SegmentedButton(s.quality == q, { scope.launch { store.set(K.quality, q) } }, SegmentedButtonDefaults.itemShape(i, qs.size)) { Text(q) } } }
        Text("THEME", Modifier.padding(start = 2.dp, top = 22.dp, bottom = 8.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val ths = buildList { add("Dark"); add("Light"); add("System"); if (Build.VERSION.SDK_INT >= 31) add("Dynamic") }
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            ths.forEachIndexed { i, t -> SegmentedButton(s.theme == t, { scope.launch { store.set(K.theme, t) } }, SegmentedButtonDefaults.itemShape(i, ths.size)) { Text(t) } } }
        Group("Data and alerts") {
            SwitchRow("Stream over Wi-Fi only", "Avoid using mobile data.", s.wifiOnly) { v -> scope.launch { store.set(K.wifiOnly, v) } }
            SwitchRow("New episode alerts", "Get notified when your list updates.", s.alerts, true) { v ->
                if (v && Build.VERSION.SDK_INT >= 33) askNotif.launch(Manifest.permission.POST_NOTIFICATIONS) else scope.launch { store.set(K.alerts, v) } }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun ProfileScreen(email: String?, onSettings: () -> Unit, onSignIn: () -> Unit, onSignUp: () -> Unit, onSignOut: () -> Unit) {
    val ctx = LocalContext.current; val store = remember { SettingsStore.of(ctx) }; val scope = rememberCoroutineScope()
    val s by store.flow.collectAsState(Settings()); var name by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        TopBar("Profiles", null)
        Spacer(Modifier.height(12.dp))
        s.profiles.forEachIndexed { i, n ->
            Surface(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, if (i == s.current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                onClick = { scope.launch { store.set(K.current, i) } }) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(Modifier.size(36.dp), shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primary) { Box(contentAlignment = Alignment.Center) { Text(n.take(1).uppercase()) } }
                    Text(n, Modifier.padding(start = 12.dp).weight(1f), style = MaterialTheme.typography.titleMedium)
                    if (s.profiles.size > 1 && i != s.current) TextButton({ scope.launch { store.removeProfile(i, s.profiles) } }) { Text("Delete") }
                } } }
        if (s.profiles.size < 4) Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(name, { name = it.take(12) }, Modifier.weight(1f), placeholder = { Text("New profile") }, singleLine = true, shape = RoundedCornerShape(24.dp))
            Button({ scope.launch { store.addProfile(name, s.profiles); name = "" } }, Modifier.padding(start = 10.dp)) { Text("Add") }
        }
        if (email != null) SignedIn(email, onSignOut) else Surface(Modifier.fillMaxWidth().padding(top = 24.dp), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
            Column(Modifier.padding(20.dp)) {
                Text("Sign in to Onimeku", style = MaterialTheme.typography.titleLarge)
                Text("Save your watchlist and pick up where you left off on any device.", Modifier.padding(top = 6.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledTonalButton(onSignIn, Modifier.weight(1f)) { Text("Sign in") }
                    Button(onSignUp, Modifier.weight(1f)) { Text("Sign up") } } } }
        Group("") { listOf("Settings" to onSettings).forEach { (t, f) -> TextButton(f, Modifier.fillMaxWidth().padding(4.dp)) { Text(t, Modifier.weight(1f)); Text("›") } } }
    }
}

@Composable private fun SignedIn(email: String, onSignOut: () -> Unit) =
    Surface(Modifier.fillMaxWidth().padding(top = 24.dp), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(20.dp)) {
            Text("Signed in", style = MaterialTheme.typography.titleLarge)
            Text(email, Modifier.padding(top = 6.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onSignOut, Modifier.padding(top = 16.dp)) { Text("Sign out") } } }
