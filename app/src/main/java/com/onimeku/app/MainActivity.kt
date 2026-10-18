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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
    }
}

@Composable
fun App(vm: AppVM) {
    BackHandler(enabled = vm.stack.size > 1 && !vm.welcomeOpen) { vm.back() }

    Surface(Modifier.fillMaxSize(), color = Bg, contentColor = Color.White) {
        Box(Modifier.fillMaxSize()) {
            if (vm.welcomeOpen) WelcomeScreen(vm) else {
                when (val r = vm.route) {
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
                }
                if (vm.route == Route.Home) TopBar(vm)
                if (vm.route !is Route.Player) TabBar(vm, Modifier.align(Alignment.BottomCenter))
            }
            vm.toastMsg?.let { m ->
                val bottom = if (vm.route is Route.Player || vm.welcomeOpen) 24.dp else 86.dp
                Text(m, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = bottom, start = 16.dp, end = 16.dp)
                    .clip(CircleShape).background(Color.White).padding(horizontal = 18.dp, vertical = 10.dp),
                    color = Bg, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun TopBar(vm: AppVM) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 16.dp, end = 10.dp, top = 6.dp),
        verticalAlignment = Alignment.CenterVertically) {
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
        T(Route.Home, "Home", Icons.Outlined.Home, Icons.Filled.Home),
        T(Route.Calendar, "Calendar", Icons.Outlined.CalendarMonth, Icons.Outlined.CalendarMonth),
        T(Route.MyList, "My List", Icons.Outlined.BookmarkBorder, Icons.Outlined.BookmarkBorder),
        T(Route.Downloads, "Download", Icons.Outlined.Download, Icons.Outlined.Download),
        T(Route.ProfileTab, "Profile", Icons.Outlined.Person, Icons.Filled.Person),
    )
    Row(
        modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0xE6141621), Color(0xFF141621))))
            .navigationBarsPadding().padding(top = 10.dp, bottom = 10.dp, start = 8.dp, end = 8.dp),
    ) {
        tabs.forEach { t ->
            val sel = vm.tab == t.r
            Column(Modifier.weight(1f).clickable { vm.setTab(t.r) }, horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(if (sel) t.sel else t.icon, t.label, Modifier.size(26.dp), tint = if (sel) RedText else NavOff)
                Text(t.label, fontSize = 12.sp, color = if (sel) RedText else NavOff, maxLines = 1)
            }
        }
    }
}
