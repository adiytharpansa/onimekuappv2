package com.onimeku.app.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.onimeku.app.data.AnimeRepository
import com.onimeku.app.ui.screens.AuthScreen
import com.onimeku.app.ui.screens.DetailScreen
import com.onimeku.app.ui.screens.DownloadScreen
import com.onimeku.app.ui.screens.HomeScreen
import com.onimeku.app.ui.screens.MyListScreen
import com.onimeku.app.ui.screens.NotifScreen
import com.onimeku.app.ui.screens.OnboardingScreen
import com.onimeku.app.ui.screens.PlayerScreen
import com.onimeku.app.ui.screens.ProfileScreen
import com.onimeku.app.ui.screens.ReleaseScreen
import com.onimeku.app.ui.screens.SearchScreen
import com.onimeku.app.ui.theme.Bg
import com.onimeku.app.ui.theme.Crimson
import com.onimeku.app.ui.theme.Glass
import com.onimeku.app.ui.theme.Radius
import com.onimeku.app.ui.theme.Space
import com.onimeku.app.ui.theme.Surface2
import com.onimeku.app.ui.theme.TextSecondary
import androidx.compose.foundation.layout.width

enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    Home("home", "Beranda", Icons.Rounded.Home),
    Release("release", "Rilis", Icons.Rounded.CalendarMonth),
    MyList("mylist", "My List", Icons.Rounded.Bookmark),
    Download("download", "Unduhan", Icons.Rounded.Download),
    Profile("profile", "Profil", Icons.Rounded.Person)
}

@Composable
fun OnimekuApp() {
    val nav = rememberNavController()
    val vm: AppViewModel = viewModel()
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    val showBar = Tab.entries.any { it.route == route }
    val snack = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        vm.msg.collect {
            snack.currentSnackbarData?.dismiss()
            snack.showSnackbar(it, duration = SnackbarDuration.Short)
        }
    }

    fun open(id: Int) = nav.navigate("detail/$id")

    // Bersihkan seluruh back stack lalu jadikan [route] sebagai akar baru.
    fun resetTo(route: String) = nav.navigate(route) {
        popUpTo(nav.graph.id) { inclusive = true }
        launchSingleTop = true
    }
    fun goHome() = nav.navigate("home") {
        popUpTo("home") { inclusive = false }
        launchSingleTop = true
        restoreState = true
    }
    fun selectTab(t: Tab) = nav.navigate(t.route) {
        popUpTo("home") { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

    Box(Modifier.fillMaxSize().background(Bg)) {
        NavHost(
            nav, startDestination = if (vm.profile.value.name == "Tamu") "onboarding" else "home", modifier = Modifier.fillMaxSize(),
            enterTransition = { fadeIn(tween(260)) + slideInHorizontally(tween(320)) { it / 10 } },
            exitTransition = { fadeOut(tween(180)) },
            popEnterTransition = { fadeIn(tween(240)) },
            popExitTransition = { fadeOut(tween(180)) + slideOutHorizontally(tween(320)) { it / 10 } }
        ) {
            composable("onboarding") { OnboardingScreen { nav.navigate("auth") } }
            composable("auth") {
                AuthScreen(
                    vm,
                    onBack = { nav.popBackStack() },
                    onDone = { resetTo("home") }
                )
            }

            composable("home", enterTransition = { fadeIn(tween(220)) }, exitTransition = { fadeOut(tween(160)) }) {
                HomeScreen(
                    vm = vm,
                    onOpen = ::open,
                    onSearch = { nav.navigate("search") },
                    onNotif = { nav.navigate("notif") },
                    onPlay = { id -> nav.navigate("player/$id/${vm.history.value[id]?.ep ?: 1}") },
                    onRelease = { selectTab(Tab.Release) }
                )
            }
            composable("release", enterTransition = { fadeIn(tween(220)) }, exitTransition = { fadeOut(tween(160)) }) {
                ReleaseScreen(vm, ::open)
            }
            composable("mylist", enterTransition = { fadeIn(tween(220)) }, exitTransition = { fadeOut(tween(160)) }) {
                MyListScreen(vm, ::open, ::goHome)
            }
            composable("download", enterTransition = { fadeIn(tween(220)) }, exitTransition = { fadeOut(tween(160)) }) {
                DownloadScreen(vm, ::open, ::goHome)
            }
            composable("profile", enterTransition = { fadeIn(tween(220)) }, exitTransition = { fadeOut(tween(160)) }) {
                ProfileScreen(vm, onAuth = { nav.navigate("auth") }) { resetTo("onboarding") }
            }
            composable("notif") { NotifScreen(vm) { nav.popBackStack() } }
            composable("search") { SearchScreen(vm, { nav.popBackStack() }, ::open) }
            composable(
                "detail/{id}",
                listOf(navArgument("id") { type = NavType.IntType })
            ) {
                val id = it.arguments!!.getInt("id")
                DetailScreen(
                    AnimeRepository.byId(id), vm,
                    onBack = { nav.popBackStack() },
                    onOpen = ::open,
                    onPlay = { ep -> nav.navigate("player/$id/$ep") }
                )
            }
            composable(
                "player/{id}/{ep}",
                listOf(
                    navArgument("id") { type = NavType.IntType },
                    navArgument("ep") { type = NavType.IntType }
                ),
                enterTransition = { fadeIn(tween(350)) }
            ) {
                val args = it.arguments!!
                PlayerScreen(
                    vm, AnimeRepository.byId(args.getInt("id")), args.getInt("ep")
                ) { nav.popBackStack() }
            }
        }

        // Scrim di belakang menu melayang supaya konten yang lewat tidak "bertabrakan".
        AnimatedVisibility(
            showBar,
            Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(tween(250)),
            exit = fadeOut(tween(200))
        ) {
            Box(
                Modifier.fillMaxWidth().height(170.dp).background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Bg.copy(alpha = .82f), Bg)
                    )
                )
            )
        }

        SnackbarHost(
            snack,
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = if (showBar) 98.dp else 24.dp)
        ) {
            Snackbar(
                it,
                containerColor = Surface2,
                contentColor = Color.White,
                actionColor = Crimson,
                shape = RoundedCornerShape(Radius.full)
            )
        }

        AnimatedVisibility(
            showBar,
            Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(350)) { it * 2 } + fadeIn(),
            exit = slideOutVertically(tween(300)) { it * 2 } + fadeOut()
        ) {
            FloatingNav(route, ::selectTab)
        }
    }
}

@Composable
private fun FloatingNav(current: String?, onSelect: (Tab) -> Unit) =
    Surface(
        Modifier.navigationBarsPadding()
            .padding(start = 14.dp, end = 14.dp, bottom = 12.dp)
            .fillMaxWidth().height(66.dp),
        shape = RoundedCornerShape(26.dp),
        color = Glass,
        border = BorderStroke(1.dp, Color.White.copy(.09f)),
        shadowElevation = 18.dp
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val w = maxWidth / Tab.entries.size
            val idx = Tab.entries.indexOfFirst { it.route == current }.coerceAtLeast(0)
            val x by animateDpAsState(
                w * idx,
                spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow),
                label = "pill"
            )
            Box(
                Modifier.offset(x = x).width(w).fillMaxHeight().padding(7.dp)
                    .clip(RoundedCornerShape(19.dp))
                    .background(Crimson.copy(.13f))
                    .border(1.dp, Crimson.copy(.38f), RoundedCornerShape(19.dp))
            )
            Row(Modifier.fillMaxSize()) {
                Tab.entries.forEach { t ->
                    val sel = current == t.route
                    val tint by animateColorAsState(
                        if (sel) Crimson else TextSecondary,
                        label = "tint"
                    )
                    Column(
                        Modifier.weight(1f).fillMaxHeight()
                            .clip(RoundedCornerShape(24.dp))
                            .clickable { onSelect(t) },
                        Arrangement.Center,
                        Alignment.CenterHorizontally
                    ) {
                        Icon(t.icon, t.label, tint = tint, modifier = Modifier.size(22.dp))
                        Text(
                            t.label,
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                            color = if (sel) Color.White else TextSecondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
