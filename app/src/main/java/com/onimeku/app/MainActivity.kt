package com.onimeku.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onimeku.app.ui.*
import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.util.Rational
import androidx.activity.viewModels

class MainActivity : ComponentActivity() {
    private val vm: AppVM by viewModels()
    private var permCb: ((Boolean) -> Unit)? = null
    private val permLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { g ->
        permCb?.invoke(g); permCb = null
    }

    private fun ensureNotif(cb: (Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT < 33 ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) cb(true)
        else { permCb = cb; permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleLink(intent)
    }

    /** onimeku://anime/<id> -> halaman Detail (bila judul ada di katalog). */
    private fun handleLink(i: android.content.Intent?) {
        val d = i?.data ?: return
        if (d.scheme != "onimeku" || d.host != "anime") return
        val id = d.pathSegments.firstOrNull() ?: return
        vm.closeWelcome()
        vm.go(Route.Detail(id))
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Android 12+ memakai auto-enter (diatur di PlayerScreen); di bawahnya masuk PiP manual di sini.
        if (Build.VERSION.SDK_INT < 31 && vm.route is Route.Player && vm.playing) {
            try { enterPictureInPictureMode(PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).build()) } catch (_: Exception) {}
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        vm.inPip = isInPictureInPictureMode
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            OnimekuTheme {
                CompositionLocalProvider(LocalEnsureNotif provides { cb -> ensureNotif(cb) }) {
                    val appVm: AppVM = viewModel()
                    App(appVm)
                }
            }
        }
        // Kait tes otomatis (emulator CI): buka pemutar langsung ke satu
        // episode lewat intent agar putar bisa diverifikasi ujung-ke-ujung
        // tanpa mengetuk UI. Tidak berpengaruh pada pemakaian normal.
        handleLink(intent)
        val testId = intent?.getStringExtra("test_id")
        if (!testId.isNullOrBlank()) {
            val testEp = intent?.getIntExtra("test_ep", 1) ?: 1
            vm.closeWelcome()
            vm.go(Route.Player(testId, testEp))
        } else if (intent?.getBooleanExtra("test_home", false) == true) {
            vm.closeWelcome()
        }
    }
}

@Composable
fun App(vm: AppVM) {
    // Bahasa antarmuka aktif mengikuti Settings (debug.31).
    androidx.compose.runtime.SideEffect { I18n.lang = vm.s.settings.lang }
    BackHandler(enabled = vm.stack.size > 1 && !vm.welcomeOpen) { vm.back() }

    Surface(Modifier.fillMaxSize(), color = Bg, contentColor = Color.White) {
        Box(Modifier.fillMaxSize()) {
            if (vm.welcomeOpen) WelcomeScreen(vm) else {
                AnimatedContent(
                    targetState = vm.route,
                    transitionSpec = { (fadeIn(tween(260)) + scaleIn(tween(260), initialScale = 0.98f)) togetherWith fadeOut(tween(160)) },
                    label = "route",
                ) { r ->
                    when (r) {
                        Route.Home -> HomeScreen(vm)
                        Route.Calendar -> CalendarScreen(vm)
                        Route.MyList -> MyListScreen(vm)
                        Route.Downloads -> DownloadsScreen(vm)
                        Route.ProfileTab -> ProfileScreen(vm)
                        is Route.Sub -> SubScreen(vm, r.key)
                        is Route.Detail -> DetailScreen(vm, r.id)
                        is Route.SeeAll -> SeeAllScreen(vm, r.key)
                        Route.Notif -> NotifScreen(vm)
                        Route.Search -> SearchScreen(vm)
                        is Route.Player -> PlayerScreen(vm, r)
                        is Route.Auth -> AuthScreen(vm, r.signUp)
                    }
                }
                if (vm.route == Route.Home) TopBar(vm)
                if (vm.route !is Route.Player && vm.route !is Route.Auth) TabBar(vm, Modifier.align(Alignment.BottomCenter))
            }
            // Toast masuk/keluar dengan fade + geser; teks terakhir ditahan selama animasi keluar.
            var lastToast by remember { mutableStateOf("") }
            LaunchedEffect(vm.toastMsg) { vm.toastMsg?.let { lastToast = it } }
            val offlineBar = !vm.online && !vm.welcomeOpen && vm.route !is Route.Player
            AnimatedVisibility(
                visible = offlineBar,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
                    .padding(bottom = if (vm.route is Route.Auth) 0.dp else TabBarHeight),
                enter = fadeIn(tween(200)) + slideInVertically(tween(250)) { it },
                exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { it },
            ) {
                Text("You're offline · downloads still work", Modifier.fillMaxWidth().background(RedDeep).padding(vertical = 6.dp),
                    color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Medium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
            val bottom = (if (vm.route is Route.Player || vm.route is Route.Auth || vm.welcomeOpen) 24.dp else TabBarHeight + 12.dp) + (if (offlineBar) 30.dp else 0.dp)
            AnimatedVisibility(
                visible = vm.toastMsg != null,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = bottom, start = 16.dp, end = 16.dp),
                enter = fadeIn(tween(200)) + slideInVertically(tween(250)) { it / 2 },
                exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { it / 2 },
            ) {
                Text(vm.toastMsg ?: lastToast, Modifier.clip(CircleShape).background(Color.White).padding(horizontal = 18.dp, vertical = 10.dp),
                    color = Bg, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun TopBar(vm: AppVM) {
    // Transparan di atas hero; setelah digulir menjadi latar solid supaya ikon
    // tidak bertabrakan dengan poster di bawahnya.
    // Transisi halus: scrim gradasi di atas hero, solid bertahap mulai ±80px gulir (tanpa lonjakan mendadak).
    // derivedStateOf + kuantisasi 5%: hanya recompose saat nilai benar-benar berubah, bukan tiap piksel gulir.
    // Perbaikan debug.32: dulu solid hanya 97% sehingga judul hero tembus samar di balik bilah; kini 100% + garis halus.
    val k by remember { derivedStateOf { (((vm.homeScrollPx() - 60) / 160f).coerceIn(0f, 1f) * 20f).toInt() / 20f } }
    Row(
        Modifier.fillMaxWidth()
            .drawBehind {
                // Di atas hero: scrim lembut tanpa tepi keras, supaya jam/ikon status terbaca di poster terang.
                if (k < 1f) drawRect(Brush.verticalGradient(listOf(Bg.copy(alpha = 0.72f * (1f - k)), Bg.copy(alpha = 0f))))
                // Setelah digulir: latar solid penuh + garis tipis di tepi bawah.
                if (k > 0f) drawRect(Bg.copy(alpha = k))
                if (k >= 1f) drawLine(Line, Offset(0f, size.height - 0.5.dp.toPx()), Offset(size.width, size.height - 0.5.dp.toPx()), 1.dp.toPx())
            }
            .statusBarsPadding().padding(start = 16.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.clickable { vm.setTab(Route.Home) }) { Logo(40.dp) }
        Spacer(Modifier.weight(1f))
        IconBtn(Icons.Outlined.Search, "Search") { vm.go(Route.Search) }
        Box {
            IconBtn(Icons.Outlined.Notifications, "Notifications") { vm.go(Route.Notif) }
            if (!vm.s.notifRead && vm.s.settings.alerts)
                Box(Modifier.align(Alignment.TopEnd).padding(top = 9.dp, end = 10.dp).size(9.dp).clip(CircleShape).background(Red))
        }
    }
}

@Composable
private fun IconBtn(icon: ImageVector, desc: String, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, desc, Modifier.size(26.dp))
    }
}

@Composable
private fun TabBar(vm: AppVM, modifier: Modifier) {
    data class T(val r: Route, val label: String, val icon: ImageVector, val sel: ImageVector)
    val tabs = listOf(
        T(Route.Home, I18n.t("tab_home", "Home"), Icons.Outlined.Home, Icons.Filled.Home),
        T(Route.Calendar, I18n.t("tab_calendar", "Calendar"), Icons.Outlined.CalendarMonth, Icons.Outlined.CalendarMonth),
        T(Route.MyList, I18n.t("tab_mylist", "My List"), Icons.Outlined.BookmarkBorder, Icons.Outlined.BookmarkBorder),
        T(Route.Downloads, I18n.t("tab_download", "Download"), Icons.Outlined.Download, Icons.Outlined.Download),
        T(Route.ProfileTab, I18n.t("tab_profile", "Profile"), Icons.Outlined.Person, Icons.Filled.Person),
    )
    Row(
        // Solid + garis tipis di atas, supaya konten yang digulir tidak tembus di balik tab bar.
        modifier.fillMaxWidth().background(Bg)
            .drawBehind { drawLine(Line, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }
            .navigationBarsPadding().height(TabBarHeight).padding(horizontal = 8.dp),
    ) {
        tabs.forEach { t ->
            val sel = vm.tab == t.r
            Column(Modifier.weight(1f).fillMaxHeight().clickable { vm.setTab(t.r) }, horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)) {
                val tint by animateColorAsState(if (sel) RedText else NavOff, tween(200), label = "tabTint")
                val pill by animateColorAsState(if (sel) Red.copy(alpha = 0.28f) else Color.Transparent, tween(200), label = "tabPill")
                // Indikator "pil" di belakang ikon aktif (gaya Material 3), lebih jelas daripada hanya warna.
                Box(Modifier.size(width = 58.dp, height = 30.dp).clip(CircleShape).background(pill), contentAlignment = Alignment.Center) {
                    Icon(if (sel) t.sel else t.icon, t.label, Modifier.size(24.dp), tint = tint)
                }
                Text(t.label, fontSize = 11.5.sp, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Medium, color = tint, maxLines = 1)
            }
        }
    }
}
