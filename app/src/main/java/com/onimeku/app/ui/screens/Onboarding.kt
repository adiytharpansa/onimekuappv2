package com.onimeku.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onimeku.app.ui.AppViewModel
import com.onimeku.app.ui.components.*
import com.onimeku.app.ui.theme.*
import kotlinx.coroutines.launch

private val pages = listOf(
    Triple("Nonton Anime Kapan Saja", "Ribuan judul HD dengan subtitle lengkap, tanpa ribet.", Color.hsv(350f, .7f, .4f)),
    Triple("Episode Baru Setiap Hari", "Kalender rilis dan pengingat agar tak ketinggalan.", Color.hsv(215f, .7f, .4f)),
    Triple("Simpan & Unduh", "My List pribadi dan unduh untuk nonton offline.", Color.hsv(270f, .7f, .4f))
)

@Composable
fun OnboardingScreen(onStart: () -> Unit) {
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val top by animateColorAsState(pages[pager.currentPage].third, label = "bg")
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(top, Wine, Bg)))) {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(24.dp)) {
            GlassButton("Lewati", Modifier.align(Alignment.End).height(36.dp), onClick = onStart)
            HorizontalPager(pager, Modifier.weight(1f)) { i ->
                Column(Modifier.fillMaxSize(), Arrangement.Bottom) {
                    Text(pages[i].first, fontSize = 38.sp, fontWeight = FontWeight.Black, lineHeight = 44.sp)
                    Text(pages[i].second, color = Color.White.copy(.8f), fontSize = 16.sp, modifier = Modifier.padding(top = 12.dp))
                }
            }
            Row(Modifier.padding(vertical = 22.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(pages.size) {
                    val w by animateDpAsState(if (pager.currentPage == it) 26.dp else 8.dp, label = "dot")
                    Box(Modifier.height(6.dp).width(w).clip(CircleShape).background(if (pager.currentPage == it) Color.White else Color.White.copy(.35f)))
                }
            }
            PrimaryButton(if (pager.currentPage < pages.lastIndex) "Lanjut" else "Mulai", Modifier.fillMaxWidth()) {
                if (pager.currentPage < pages.lastIndex) scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } else onStart()
            }
        }
    }
}

@Composable
fun AuthScreen(vm: AppViewModel, onDone: () -> Unit) {
    var register by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    val emailOk = Regex("\\S+@\\S+\\.\\S+").matches(email)
    Column(Modifier.fillMaxSize().systemBarsPadding().imePadding().padding(24.dp), Arrangement.Center) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Logo(34.dp)
            Text("  Onimeku", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.height(20.dp))
        Row { Text(if (register) "Buat akun baru" else "Selamat datang", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold); Text(".", fontSize = 32.sp, color = Crimson, fontWeight = FontWeight.ExtraBold) }
        Text(if (register) "Gabung dan mulai nonton gratis." else "Masuk untuk melanjutkan tontonanmu.", color = TextSecondary, modifier = Modifier.padding(top = 6.dp, bottom = 20.dp))
        val colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Crimson, unfocusedBorderColor = Color.Transparent,
            focusedContainerColor = Surface2, unfocusedContainerColor = Surface2, cursorColor = Crimson, errorBorderColor = Crimson, errorContainerColor = Surface2)
        val shape = RoundedCornerShape(24.dp)
        if (register) { OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), placeholder = { Text("Nama pengguna") }, singleLine = true, shape = shape, colors = colors); Spacer(Modifier.height(12.dp)) }
        OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), placeholder = { Text("Email") }, singleLine = true, shape = shape, colors = colors,
            isError = email.isNotEmpty() && !emailOk, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(pass, { pass = it }, Modifier.fillMaxWidth(), placeholder = { Text("Kata sandi (min. 6)") }, singleLine = true, shape = shape, colors = colors,
            isError = pass.isNotEmpty() && pass.length < 6, visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = { IconButton({ show = !show }) { Icon(if (show) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, "Tampilkan") } })
        Spacer(Modifier.height(20.dp))
        PrimaryButton(if (register) "Daftar" else "Masuk", Modifier.fillMaxWidth().height(52.dp)) {
            when { !emailOk -> vm.toast("Email tidak valid"); pass.length < 6 -> vm.toast("Sandi minimal 6 karakter"); else -> onDone() } }
        Spacer(Modifier.height(10.dp))
        GlassButton("Lanjut sebagai tamu", Modifier.fillMaxWidth().height(52.dp), onClick = onDone)
        TextButton({ register = !register }, Modifier.align(Alignment.CenterHorizontally)) {
            Text(if (register) "Sudah punya akun? " else "Belum punya akun? ", color = TextSecondary); Text(if (register) "Masuk" else "Daftar", color = Crimson, fontWeight = FontWeight.Bold) }
    }
}
