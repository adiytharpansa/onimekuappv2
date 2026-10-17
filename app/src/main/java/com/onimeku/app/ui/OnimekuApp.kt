package com.onimeku.app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.onimeku.app.data.AnimeRepository
import com.onimeku.app.ui.screens.*
import com.onimeku.app.ui.theme.*

enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    Home("home", "Beranda", Icons.Rounded.Home), Release("release", "Rilis", Icons.Rounded.CalendarMonth),
    MyList("mylist", "My List", Icons.Rounded.Bookmark), Download("download", "Unduhan", Icons.Rounded.Download),
    Profile("profile", "Profil", Icons.Rounded.Person)
}

@Composable
fun OnimekuApp() {
    val nav = rememberNavController()
    val vm: AppViewModel = viewModel()
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    val home by vm.home.collectAsState()
    val showBar = Tab.entries.any { it.route == route } && !(route == "home" && home !is UiState.Success)
    val snack = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { vm.msg.collect { snack.currentSnackbarData?.dismiss(); snack.showSnackbar(it, duration = SnackbarDuration.Short) } }
    fun open(id: Int) = nav.navigate("detail/$id")

    Box(Modifier.fillMaxSize().background(Bg)) {
        NavHost(nav, "onboarding", Modifier.fillMaxSize(),
            enterTransition = { fadeIn(tween(300)) + slideInHorizontally(tween(300)) { it / 12 } },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(300)) },
            popExitTransition = { fadeOut(tween(200)) + slideOutHorizontally(tween(300)) { it / 12 } }) {
            composable("onboarding") { OnboardingScreen { nav.navigate("auth") } }
            composable("auth") { AuthScreen(vm) { nav.navigate("home") { popUpTo("onboarding") { inclusive = true } } } }
            composable("home") { HomeScreen(vm, ::open, { nav.navigate("search") }, { nav.navigate("notif") }, { id -> nav.navigate("player/$id/${vm.history.value[id]?.ep ?: 1}") }) }
            composable("release") { ReleaseScreen(vm, ::open) }
            composable("mylist") { MyListScreen(vm, ::open) }
            composable("download") { DownloadScreen(vm, ::open) }
            composable("profile") { ProfileScreen(vm) { nav.navigate("onboarding") { popUpTo(0) } } }
            composable("notif") { NotifScreen(vm) { nav.popBackStack() } }
            composable("search") { SearchScreen(vm, { nav.popBackStack() }, ::open) }
            composable("detail/{id}", listOf(navArgument("id") { type = NavType.IntType })) {
                val id = it.arguments!!.getInt("id")
                DetailScreen(AnimeRepository.byId(id), vm, { nav.popBackStack() }, ::open) { ep -> nav.navigate("player/$id/$ep") }
            }
            composable("player/{id}/{ep}", listOf(navArgument("id") { type = NavType.IntType }, navArgument("ep") { type = NavType.IntType }),
                enterTransition = { fadeIn(tween(400)) }) {
                PlayerScreen(vm, AnimeRepository.byId(it.arguments!!.getInt("id")), it.arguments!!.getInt("ep")) { nav.popBackStack() }
            }
        }
        SnackbarHost(snack, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = if (showBar) 96.dp else 24.dp)) {
            Snackbar(it, containerColor = Color.White, contentColor = Color.Black, shape = RoundedCornerShape(22.dp)) }
        AnimatedVisibility(showBar, Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(350)) { it * 2 } + fadeIn(), exit = slideOutVertically(tween(300)) { it * 2 } + fadeOut()) {
            FloatingNav(route) { t -> nav.navigate(t.route) { popUpTo("home") { saveState = true }; launchSingleTop = true; restoreState = true } }
        }
    }
}

@Composable
private fun FloatingNav(current: String?, onSelect: (Tab) -> Unit) =
    Surface(Modifier.navigationBarsPadding().padding(start = 14.dp, end = 14.dp, bottom = 12.dp).fillMaxWidth().height(68.dp),
        shape = RoundedCornerShape(26.dp), color = Glass, border = BorderStroke(1.dp, Color.White.copy(.08f)), shadowElevation = 16.dp) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val w = maxWidth / Tab.entries.size
            val idx = Tab.entries.indexOfFirst { it.route == current }.coerceAtLeast(0)
            val x by animateDpAsState(w * idx, spring(.7f, Spring.StiffnessMedium), label = "pill")
            Box(Modifier.offset(x = x).width(w).fillMaxHeight().padding(7.dp).clip(RoundedCornerShape(20.dp))
                .background(Crimson.copy(.14f)).border(1.dp, Crimson.copy(.35f), RoundedCornerShape(20.dp)))
            Row(Modifier.fillMaxSize()) {
                Tab.entries.forEach { t ->
                    val sel = current == t.route
                    val tint by animateColorAsState(if (sel) Crimson else TextSecondary, label = "tint")
                    Column(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(26.dp)).clickable { onSelect(t) }, Arrangement.Center, Alignment.CenterHorizontally) {
                        Icon(t.icon, t.label, tint = tint, modifier = Modifier.size(22.dp))
                        Text(t.label, fontSize = 10.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium, color = if (sel) Color.White else TextSecondary)
                    }
                }
            }
        }
    }
