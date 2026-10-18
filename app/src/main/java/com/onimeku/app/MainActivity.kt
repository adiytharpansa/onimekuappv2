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
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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

@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun App(vm: AppVM) {
    // Bahasa antarmuka aktif mengikuti Settings (debug.31).
    androidx.compose.runtime.SideEffect { I18n.lang = vm.s.settings.lang }
    // Kesegaran katalog (debug.33): segarkan diam-diam saat aplikasi kembali
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
    // Hormati "Hapus animasi" / skala animasi 0 di Android.
    val ctxMotion = androidx.compose.ui.platform.LocalContext.current
    val reduceMotion = remember {
        try { android.provider.Settings.Global.getFloat(ctxMotion.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f } catch (e: Exception) { false }
    }

    val enterA by animateFloatAsState(if (vm.welcomeOpen) 0f else 1f, tween(520), label = "homeEnter")
    Surface(Modifier.fillMaxSize(), color = Bg, contentColor = Color.White) {
        Box(Modifier.fillMaxSize()) {
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
                            dir == 0 -> fadeIn(tween(220)) togetherWith fadeOut(tween(140))
                            // MASUK: Player naik dari bawah; Detail dibawa elemen bersama (poster terbang); lainnya geser dari kanan.
                            dir > 0 && t is Route.Player -> (slideInVertically(tween(340, easing = ease)) { it / 6 } + fadeIn(tween(280))) togetherWith fadeOut(tween(200))
                            dir > 0 && t is Route.Detail -> fadeIn(tween(300)) togetherWith fadeOut(tween(200))
                            dir > 0 -> (slideInHorizontally(tween(320, easing = ease)) { it / 4 } + fadeIn(tween(280))) togetherWith
                                (slideOutHorizontally(tween(320, easing = ease)) { -it / 6 } + fadeOut(tween(220)))
                            // KEMBALI: kebalikannya.
                            i is Route.Player -> fadeIn(tween(240)) togetherWith (slideOutVertically(tween(300, easing = ease)) { it / 6 } + fadeOut(tween(240)))
                            i is Route.Detail -> fadeIn(tween(240)) togetherWith fadeOut(tween(220))
                            else -> (slideInHorizontally(tween(320, easing = ease)) { -it / 6 } + fadeIn(tween(280))) togetherWith
                                (slideOutHorizontally(tween(320, easing = ease)) { it / 4 } + fadeOut(tween(220)))
                        }
                        ct.also { it.targetContentZIndex = if (dir < 0) -1f else 1f }
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
private fun TabBar(vm: AppVM, modifier: Modifier) {
    data class T(val r: Route, val label: String, val icon: ImageVector, val sel: ImageVector)
    val tabs = listOf(
        T(Route.Home, I18n.t("tab_home", "Home"), Icons.Outlined.Home, Icons.Filled.Home),
        T(Route.Calendar, I18n.t("tab_calendar", "Calendar"), Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth),
        T(Route.MyList, I18n.t("tab_mylist", "My List"), Icons.Outlined.BookmarkBorder, Icons.Filled.Bookmark),
        T(Route.Search, I18n.t("tab_search", "Search"), Icons.Outlined.Search, Icons.Filled.Search),
        T(Route.ProfileTab, I18n.t("tab_profile", "Profile"), Icons.Outlined.Person, Icons.Filled.Person),
    )
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val selIdx = tabs.indexOfFirst { it.r == vm.tab }.coerceAtLeast(0)
    val pos by animateFloatAsState(selIdx.toFloat(), spring(dampingRatio = 0.72f, stiffness = 380f), label = "tabBar")
    Box(
        // Solid + garis tipis di atas, supaya konten yang digulir tidak tembus di balik tab bar.
        modifier.fillMaxWidth().background(Bg)
            .drawBehind { drawLine(Line, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }
            .navigationBarsPadding().height(TabBarHeight),
    ) {
    // Penanda merah yang meluncur mengikuti tab aktif.
    Box(Modifier.fillMaxWidth().height(3.dp).padding(horizontal = 8.dp).drawBehind {
        val seg = size.width / tabs.size
        drawRoundRect(RedText, Offset(pos * seg + seg * 0.3f, 0f), androidx.compose.ui.geometry.Size(seg * 0.4f, size.height),
            androidx.compose.ui.geometry.CornerRadius(size.height, size.height))
    })
    Row(Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
        tabs.forEach { t ->
            val sel = vm.tab == t.r
            Column(Modifier.weight(1f).fillMaxHeight().clickable { if (!sel) haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove); vm.setTab(t.r) }, horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)) {
                val tint by animateColorAsState(if (sel) RedText else NavOff, tween(200), label = "tabTint")
                val pill by animateColorAsState(if (sel) Red.copy(alpha = 0.30f) else Color.Transparent, tween(220), label = "tabPill")
                // debug.33: pil melebar dengan pegas + ikon sedikit membesar saat aktif.
                val pw by animateDpAsState(if (sel) 60.dp else 34.dp, spring(dampingRatio = 0.7f, stiffness = 500f), label = "tabPillW")
                val isc by animateFloatAsState(if (sel) 1.12f else 1f, spring(dampingRatio = 0.5f, stiffness = 600f), label = "tabIconScale")
                val lblA by animateFloatAsState(if (sel) 1f else 0.72f, tween(200), label = "tabLabelA")
                Box(Modifier.size(width = pw, height = 30.dp).clip(CircleShape).background(pill), contentAlignment = Alignment.Center) {
                    Icon(if (sel) t.sel else t.icon, t.label, Modifier.size(24.dp).graphicsLayer { scaleX = isc; scaleY = isc }, tint = tint)
                }
                Text(t.label, fontSize = 11.5.sp, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Medium, color = tint, maxLines = 1,
                    modifier = Modifier.graphicsLayer { alpha = lblA })
            }
        }
    }
    }
}
