package com.onimeku.app

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.onimeku.app.ui.LocalReduceMotion
import com.onimeku.app.ui.RedText
import com.onimeku.app.ui.LocalSharedScope
import com.onimeku.app.ui.LocalAnimScope
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import com.onimeku.app.data.Last
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
        if (d.scheme != "onimeku") return
        when (d.host) {
            "anime" -> {
                val id = d.pathSegments.firstOrNull() ?: return
                vm.closeWelcome()
                vm.go(Route.Detail(id))
            }
            // Pintasan ikon aplikasi: "Lanjut menonton" langsung ke episode terakhir; "Cari" membuka Search.
            "continue" -> {
                vm.closeWelcome()
                val wid = d.getQueryParameter("id")
                val wep = d.getQueryParameter("ep")?.toIntOrNull()
                val l = if (!wid.isNullOrBlank() && wep != null) Last(wid, wep)
                    else vm.p.last ?: vm.p.watch.firstOrNull()?.let { Last(it.id, it.ep) }
                if (l != null) vm.go(Route.Player(l.id, l.ep)) else vm.toast(I18n.t("nothing_to_continue", "Nothing to continue yet"))
            }
            "search" -> { vm.closeWelcome(); vm.setTab(Route.Search) }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Android 12+ memakai auto-enter (diatur di PlayerScreen); di bawahnya masuk PiP manual di sini.
        if (Build.VERSION.SDK_INT < 31 && vm.s.settings.autoPip && vm.route is Route.Player && vm.playing) {
            try { enterPictureInPictureMode(vm.pipParams ?: PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).build()) } catch (_: Exception) {}
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
                CompositionLocalProvider(androidx.compose.foundation.LocalIndication provides com.onimeku.app.ui.NoIndication, LocalEnsureNotif provides { cb -> ensureNotif(cb) }, LocalQuickMenu provides { a -> vm.quick = a.id }) {
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

@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun App(vm: AppVM) {
    // Bahasa antarmuka aktif mengikuti Settings.
    androidx.compose.runtime.SideEffect { I18n.lang = vm.s.settings.lang }
    // Kesegaran katalog: segarkan diam-diam saat aplikasi kembali
    // ke depan, dan periksa berkala tiap 10 menit selama aplikasi terbuka.
    val hostActivity = androidx.compose.ui.platform.LocalContext.current as? androidx.activity.ComponentActivity
    androidx.compose.runtime.DisposableEffect(hostActivity) {
        if (hostActivity == null) return@DisposableEffect onDispose { }
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) vm.refreshIfStale()
        }
        hostActivity.lifecycle.addObserver(obs)
        onDispose { hostActivity.lifecycle.removeObserver(obs) }
    }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(10 * 60_000L)
            vm.refreshIfStale(10 * 60_000L)
        }
    }
    // Predictive back: layar mengecil mengikuti jari saat menggeser dari tepi; lepas = kembali, batal = pulih.
    var backP by remember { mutableStateOf(0f) }
    androidx.activity.compose.PredictiveBackHandler(enabled = vm.stack.size > 1 && !vm.welcomeOpen) { progress ->
        try {
            progress.collect { ev -> backP = ev.progress }
            vm.back(animated = false)
            backP = 0f
        } catch (e: kotlinx.coroutines.CancellationException) {
            backP = 0f
        }
    }
    // Back di akar: tab lain -> kembali ke Beranda dulu; di Beranda -> konfirmasi keluar (seperti YouTube).
    var confirmExit by remember { mutableStateOf(false) }
    BackHandler(enabled = vm.stack.size <= 1 && !vm.welcomeOpen && !vm.inPip) {
        if (vm.route != Route.Home) vm.setTab(Route.Home) else confirmExit = true
    }
    // Hero Beranda dijeda selama dialog terbuka (tidak berganti poster di balik scrim).
    LaunchedEffect(confirmExit) { vm.overlayOpen = confirmExit }
    if (confirmExit) ExitDialog(onCancel = { confirmExit = false }, onExit = { confirmExit = false; hostActivity?.finish() })
    // Hormati "Hapus animasi" / skala animasi 0 di Android.
    val ctxMotion = androidx.compose.ui.platform.LocalContext.current
    val reduceMotion = remember {
        try { android.provider.Settings.Global.getFloat(ctxMotion.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f } catch (e: Exception) { false }
    }

    val enterA by animateFloatAsState(if (vm.welcomeOpen) 0f else 1f, tween(520), label = "homeEnter")
    Surface(Modifier.fillMaxSize(), color = Bg, contentColor = Color.White) {
        Box(Modifier.fillMaxSize().pointerInput(Unit) {
            // Catat titik sentuhan (tanpa menelan) agar pemutar bisa tumbuh dari poster/tombol yang diketuk.
            awaitEachGesture {
                val d = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                if (vm.route !is Route.Player && size.width > 0) PlayerOrigin.set(d.position.x / size.width, d.position.y / size.height)
            }
        }) {
            if (vm.welcomeOpen) WelcomeScreen(vm) else Box(Modifier.fillMaxSize().graphicsLayer { alpha = enterA; val z = 1.05f - 0.05f * enterA; scaleX = z; scaleY = z }) {
                Box(Modifier.fillMaxSize().graphicsLayer {
                    val p = backP
                    val s = 1f - 0.07f * p
                    scaleX = s; scaleY = s
                    alpha = 1f - 0.2f * p
                    shape = androidx.compose.foundation.shape.RoundedCornerShape((28 * p).dp); clip = p > 0f
                }) {
                SharedTransitionLayout {
                AnimatedContent(
                    targetState = vm.route,
                    transitionSpec = {
                        val t = targetState; val i = initialState; val dir = vm.navDir
                        val ease = androidx.compose.animation.core.FastOutSlowInEasing
                        val ct = when {
                            reduceMotion -> EnterTransition.None togetherWith ExitTransition.None
                            // TAB -> TAB: konten masuk menggeser searah perpindahan (kanan = dari kanan, kiri = dari kiri).
                            dir == 0 && vm.tabDir != 0 && vm.tabIndex(i) >= 0 && vm.tabIndex(t) >= 0 -> {
                                val sgn = vm.tabDir
                                (slideInHorizontally(tween(320, easing = ease)) { (it * 0.22f).toInt() * sgn } + fadeIn(tween(260))) togetherWith
                                    (slideOutHorizontally(tween(320, easing = ease)) { -(it * 0.22f).toInt() * sgn } + fadeOut(tween(180)))
                            }
                            dir == 0 -> fadeIn(tween(220)) togetherWith fadeOut(tween(140))
                            // MASUK pemutar: tumbuh dari titik sentuhan terakhir (poster/tombol yang diketuk) + pudar.
                            dir > 0 && t is Route.Player -> {
                                val o = PlayerOrigin.origin
                                (scaleIn(tween(380, easing = ease), initialScale = 0.78f, transformOrigin = o) + fadeIn(tween(300))) togetherWith
                                    (scaleOut(tween(380, easing = ease), targetScale = 1.04f, transformOrigin = o) + fadeOut(tween(220)))
                            }
                            dir > 0 && t is Route.Detail -> EnterTransition.None togetherWith ExitTransition.None
                            dir > 0 -> (slideInHorizontally(tween(320, easing = ease)) { it / 4 } + fadeIn(tween(280))) togetherWith
                                (slideOutHorizontally(tween(320, easing = ease)) { -it / 6 } + fadeOut(tween(220)))
                            // KEMBALI: kebalikannya.
                            // KEMBALI dari pemutar: menyusut ke titik asal.
                            i is Route.Player -> {
                                val o = PlayerOrigin.origin
                                (fadeIn(tween(260)) + scaleIn(tween(320, easing = ease), initialScale = 1.04f, transformOrigin = o)) togetherWith
                                    (scaleOut(tween(320, easing = ease), targetScale = 0.78f, transformOrigin = o) + fadeOut(tween(260)))
                            }
                            i is Route.Detail -> EnterTransition.None togetherWith ExitTransition.None
                            else -> (slideInHorizontally(tween(320, easing = ease)) { -it / 6 } + fadeIn(tween(280))) togetherWith
                                (slideOutHorizontally(tween(320, easing = ease)) { it / 4 } + fadeOut(tween(220)))
                        }
                        (EnterTransition.None togetherWith ExitTransition.None).also { it.targetContentZIndex = if (dir < 0) -1f else 1f }
                    },
                    label = "route",
                ) { r ->
                    val animScope = this
                    CompositionLocalProvider(
                        LocalSharedScope provides this@SharedTransitionLayout,
                        LocalAnimScope provides animScope,
                        LocalReduceMotion provides reduceMotion,
                    ) {
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
                }
                }
                }
                // Tab bar meluncur/memudar keluar saat pemutar/Auth masuk (bukan hilang seketika, yang membuat "lubang" di layar
                // selama pemutar masih tumbuh dari poster) dan kembali halus saat keluar.
                AnimatedVisibility(
                    visible = vm.route !is Route.Player && vm.route !is Route.Auth,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    enter = fadeIn(tween(240)) + slideInVertically(tween(280, easing = FastOutSlowInEasing)) { it },
                    exit = fadeOut(tween(160)) + slideOutVertically(tween(220, easing = FastOutSlowInEasing)) { it },
                ) { TabBar(vm, Modifier) }
            }
            // Bilah loading global: cahaya merah menyapu di puncak layar selama sinkronisasi jaringan.
            if (vm.route !is Route.Player && !vm.welcomeOpen) {
                TopLoadingBar(vm.syncing || vm.refreshing || vm.detailBusy != null, Modifier.align(Alignment.TopCenter).statusBarsPadding())
            }
            vm.quick?.let { QuickMenuSheet(vm, it) }
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
private fun TabBar(vm: AppVM, modifier: Modifier) {
    data class T(val r: Route, val label: String, val glyph: NavGlyph)
    val tabs = listOf(
        T(Route.Home, I18n.t("tab_home", "Home"), NavGlyph.Home),
        T(Route.Calendar, I18n.t("tab_calendar", "Calendar"), NavGlyph.Calendar),
        T(Route.MyList, I18n.t("tab_mylist", "My List"), NavGlyph.Bookmark),
        T(Route.Search, I18n.t("tab_search", "Search"), NavGlyph.Search),
        T(Route.ProfileTab, I18n.t("tab_profile", "Profile"), NavGlyph.Profile),
    )
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Box(
        // Solid + garis tipis di atas, supaya konten yang digulir tidak tembus di balik tab bar.
        modifier.fillMaxWidth().background(Bg)
            .drawBehind { drawLine(Line, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }
            .navigationBarsPadding().height(TabBarHeight),
    ) {
    Row(Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
        tabs.forEach { t ->
            val sel = vm.tab == t.r
            // Tanpa ripple/sorotan putih transparan: indication = null. Umpan balik sentuhan hanya
            // mengecilkan ikon sedikit; satu-satunya penanda aktif adalah kapsul merah tua (garis bawah dibuang karena dobel).
            val src = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            val pressed by src.collectIsPressedAsState()
            Column(
                Modifier.weight(1f).fillMaxHeight().clickable(interactionSource = src, indication = null) {
                    if (!sel) haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    vm.setTab(t.r)
                },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.Top),
            ) {
                Spacer(Modifier.height(5.dp))
                val tint by animateColorAsState(if (sel) Color.White else NavOff, tween(200), label = "tabTint")
                val labelTint by animateColorAsState(if (sel) Color.White else NavOff, tween(200), label = "tabLabel")
                val pill by animateColorAsState(if (sel) RedDeep else Color.Transparent, tween(220), label = "tabPill")
                val pw by animateDpAsState(if (sel) 58.dp else 34.dp, spring(dampingRatio = 0.75f, stiffness = 500f), label = "tabPillW")
                val isc by animateFloatAsState(if (pressed) 0.86f else if (sel) 1.06f else 1f, spring(dampingRatio = 0.55f, stiffness = 650f), label = "tabIconScale")
                val fillA by animateFloatAsState(if (sel) 1f else 0f, tween(240), label = "tabFill")
                val lblA by animateFloatAsState(if (sel) 1f else 0.78f, tween(200), label = "tabLabelA")
                Box(Modifier.size(width = pw, height = 32.dp).clip(CircleShape).background(pill), contentAlignment = Alignment.Center) {
                    NavGlyphIcon(t.glyph, tint, fillA, Modifier.size(24.dp).graphicsLayer { scaleX = isc; scaleY = isc })
                }
                Text(t.label, fontSize = 11.sp, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Medium, color = labelTint, maxLines = 1,
                    letterSpacing = 0.1.sp, modifier = Modifier.graphicsLayer { alpha = lblA })
            }
        }
    }
    }
}
