package com.onimeku.app

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import androidx.media3.common.util.UnstableApi
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.onimeku.app.data.Repo
import com.onimeku.app.data.Settings
import com.onimeku.app.data.SettingsStore
import com.onimeku.app.ui.*

@UnstableApi
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val s by remember { SettingsStore.of(this@MainActivity) }.flow.collectAsState(Settings())
            OnimekuTheme(s.theme) { Surface(color = MaterialTheme.colorScheme.background) { App() } }
        }
    }
    // Home saat video tampil -> otomatis Picture-in-Picture
    override fun onUserLeaveHint() {
        if (PlayerState.active && Build.VERSION.SDK_INT >= 26)
            enterPictureInPictureMode(PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).build())
    }
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig); PlayerState.inPip = isInPictureInPictureMode
    }
}

@OptIn(ExperimentalSharedTransitionApi::class) @UnstableApi @Composable
fun App() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var overlay by rememberSaveable { mutableStateOf<String?>(null) }   // null | settings | detail | player | signin | signup
    var from by rememberSaveable { mutableStateOf<String?>(null) }      // layar asal pemutar
    var anime by rememberSaveable { mutableStateOf("a1") }
    var ep by rememberSaveable { mutableIntStateOf(1) }
    var posterKey by rememberSaveable { mutableStateOf<String?>(null) }   // kunci poster untuk transisi shared element
    val homeList = rememberLazyListState()
    var email by remember { mutableStateOf<String?>(null) }
    DisposableEffect(Unit) {
        if (FirebaseApp.getApps(ctx).isEmpty()) onDispose { } else {
            val auth = FirebaseAuth.getInstance(); val l = FirebaseAuth.AuthStateListener { email = it.currentUser?.email }
            auth.addAuthStateListener(l); onDispose { auth.removeAuthStateListener(l) } }
    }
    val open = { id: String, key: String -> anime = id; posterKey = key; overlay = "detail" }
    val play = { id: String, e: Int -> anime = id; ep = e; from = overlay; overlay = "player" }
    BackHandler(overlay != null) { overlay = if (overlay == "player") from else null }
    BackHandler(overlay == null && tab != 0) { tab = 0 }

    SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedScope provides this) {
            AnimatedContent(overlay, transitionSpec = { fadeIn(tween(240)) togetherWith fadeOut(tween(180)) }, label = "nav") { ov ->
                CompositionLocalProvider(LocalAnimScope provides this) {
                    when (ov) {
                        "settings" -> SettingsScreen { overlay = null }
                        "signin", "signup" -> AuthScreen(ov == "signup", { overlay = null }) { overlay = null }
                        "detail" -> DetailScreen(anime, posterKey, { overlay = null }) { play(anime, it) }
                        "player" -> { val a = remember(anime) { Repo.of(ctx).one(anime) }.collectAsState(null).value
                            if (a != null) PlayerScreen(a.id, a.title, ep, a.eps, { ep = it }) { overlay = from } }
                        else -> Scaffold(containerColor = MaterialTheme.colorScheme.background, bottomBar = {
                            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                                listOf("Home" to Icons.Filled.Home, "Release Cal." to Icons.Filled.DateRange, "My List" to Icons.Filled.Bookmark,
                                    "Download" to Icons.Filled.Download, "Profile" to Icons.Filled.Person).forEachIndexed { i, (label, icon) ->
                                    NavigationBarItem(tab == i, { tab = i }, { Icon(icon, label) }, label = { Text(label, maxLines = 1) }) } } }) { pad ->
                            androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.padding(bottom = pad.calculateBottomPadding())) {
                                when (tab) {
                                    0 -> HomeTab(homeList, open, play); 1 -> CalendarTab(); 2 -> MyListTab(play); 3 -> DownloadTab(play)
                                    else -> ProfileScreen(email, { overlay = "settings" }, { overlay = "signin" }, { overlay = "signup" },
                                        { if (FirebaseApp.getApps(ctx).isNotEmpty()) FirebaseAuth.getInstance().signOut() })
                                } } }
                    }
                }
            }
        }
    }
}
