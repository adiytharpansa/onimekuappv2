package com.onimeku.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MailOutline
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onimeku.app.ui.AppViewModel
import com.onimeku.app.ui.components.GlassButton
import com.onimeku.app.ui.components.IconCircle
import com.onimeku.app.ui.components.Logo
import com.onimeku.app.ui.components.PrimaryButton
import com.onimeku.app.ui.theme.Bg
import com.onimeku.app.ui.theme.Crimson
import com.onimeku.app.ui.theme.Gold
import com.onimeku.app.ui.theme.Radius
import com.onimeku.app.ui.theme.Space
import com.onimeku.app.ui.theme.Surface1
import com.onimeku.app.ui.theme.Surface2
import com.onimeku.app.ui.theme.TextSecondary
import com.onimeku.app.ui.theme.Wine
import kotlinx.coroutines.launch

private data class OnboardPage(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val hue: Float
)

private val pages = listOf(
    OnboardPage(
        "Nonton anime kapan saja",
        "Ribuan judul HD dengan subtitle Indonesia, tanpa ribet.",
        Icons.Rounded.PlayCircle, 350f
    ),
    OnboardPage(
        "Episode baru setiap hari",
        "Kalender rilis dan pengingat agar tak ketinggalan episode favorit.",
        Icons.Rounded.NotificationsActive, 215f
    ),
    OnboardPage(
        "Simpan & unduh offline",
        "My List pribadi dan unduhan agar tetap bisa nonton tanpa internet.",
        Icons.Rounded.Download, 270f
    )
)

@Composable
fun OnboardingScreen(onStart: () -> Unit) {
    val pager = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val tint by animateColorAsState(
        Color.hsv(pages[pager.currentPage].hue, .55f, .28f),
        label = "bgTint"
    )
    val isLast = pager.currentPage == pages.lastIndex

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(tint.copy(.75f), Wine.copy(.35f), Bg))
        )
    ) {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = Space.xxl)) {
            Row(
                Modifier.padding(top = Space.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Logo(32.dp)
                Text(
                    "Onimeku",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    modifier = Modifier.padding(start = Space.sm).weight(1f)
                )
                TextButton({ onStart() }) {
                    Text("Lewati", color = TextSecondary, fontWeight = FontWeight.Medium)
                }
            }

            HorizontalPager(
                state = pager,
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) { i ->
                Column(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OnboardArt(pages[i].icon, pages[i].hue)
                    Spacer(Modifier.height(Space.huge))
                    Text(
                        pages[i].title,
                        fontSize = 32.sp,
                        lineHeight = 38.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        pages[i].subtitle,
                        color = Color.White.copy(.72f),
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = Space.md, start = Space.lg, end = Space.lg)
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(vertical = Space.xl),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(pages.size) {
                    val w by animateDpAsState(
                        if (pager.currentPage == it) 26.dp else 7.dp,
                        label = "dot"
                    )
                    Box(
                        Modifier.padding(horizontal = 3.dp).height(6.dp).width(w)
                            .clip(CircleShape)
                            .background(
                                if (pager.currentPage == it) Color.White
                                else Color.White.copy(.3f)
                            )
                    )
                }
            }

            PrimaryButton(
                if (isLast) "Mulai sekarang" else "Lanjut",
                Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (isLast) onStart()
                else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
            }
            TextButton(
                { onStart() },
                Modifier.fillMaxWidth().padding(top = Space.xs, bottom = Space.xs)
            ) {
                Text("Sudah punya akun? ", color = TextSecondary, fontSize = 13.sp)
                Text("Masuk", color = Crimson, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun OnboardArt(icon: ImageVector, hue: Float) = Box(
    Modifier.fillMaxWidth().height(278.dp),
    contentAlignment = Alignment.Center
) {
    Box(
        Modifier.offset(x = (-58).dp, y = 14.dp).size(136.dp, 196.dp).rotate(-11f)
            .clip(RoundedCornerShape(Radius.xl))
            .background(
                Brush.linearGradient(
                    listOf(Color.hsv((hue + 40) % 360, .55f, .38f), Color.hsv((hue + 20) % 360, .6f, .16f))
                )
            )
    )
    Box(
        Modifier.offset(x = 62.dp, y = 22.dp).size(136.dp, 196.dp).rotate(10f)
            .clip(RoundedCornerShape(Radius.xl))
            .background(
                Brush.linearGradient(
                    listOf(Color.hsv((hue + 20) % 360, .6f, .35f), Color.hsv(hue, .7f, .14f))
                )
            )
    )
    Box(
        Modifier.size(182.dp, 262.dp).rotate(-3f).clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color.hsv(hue, .78f, .52f), Color.hsv((hue + 18) % 360, .82f, .18f))
                )
            )
            .border(1.dp, Color.White.copy(.16f), RoundedCornerShape(28.dp)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.size(86.dp).clip(CircleShape).background(Color.Black.copy(.28f)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, Modifier.size(44.dp), tint = Color.White.copy(.92f)) }
        Row(
            Modifier.align(Alignment.TopEnd).padding(Space.md)
                .clip(RoundedCornerShape(Radius.full)).background(Color.Black.copy(.35f))
                .padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Star, null, Modifier.size(12.dp), tint = Gold)
            Text(" 4.9", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.align(Alignment.BottomStart).padding(Space.lg)) {
            Text("ONIMEKU", color = Color.White.copy(.5f), fontSize = 9.sp, letterSpacing = 2.sp)
            Text("Streaming Anime", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }
    }
}

@Composable
fun AuthScreen(vm: AppViewModel, onBack: () -> Unit, onDone: () -> Unit) {
    var register by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }

    val emailOk = Regex("\\S+@\\S+\\.\\S+").matches(email)
    val passOk = pass.length >= 6
    val nameOk = !register || name.isNotBlank()
    val canSubmit = emailOk && passOk && nameOk

    val colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Crimson,
        unfocusedBorderColor = Color.White.copy(.08f),
        focusedContainerColor = Surface2,
        unfocusedContainerColor = Surface1,
        cursorColor = Crimson,
        errorBorderColor = Crimson,
        errorContainerColor = Surface2,
        focusedLabelColor = Crimson,
        unfocusedLabelColor = TextSecondary
    )

    Column(
        Modifier.fillMaxSize().systemBarsPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = Space.xxl)
    ) {
        Row(Modifier.padding(top = Space.md), verticalAlignment = Alignment.CenterVertically) {
            IconCircle(
                Icons.AutoMirrored.Rounded.ArrowBack, "Kembali",
                bg = Surface2, size = 38.dp, onClick = onBack
            )
            Spacer(Modifier.weight(1f))
            Logo(30.dp)
            Text(
                "Onimeku",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                modifier = Modifier.padding(start = Space.sm)
            )
        }

        Spacer(Modifier.height(Space.huge))
        Row {
            Text(
                if (register) "Buat akun baru" else "Selamat datang",
                fontSize = 30.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Black
            )
            Text(".", fontSize = 30.sp, color = Crimson, fontWeight = FontWeight.Black)
        }
        Text(
            if (register) "Gabung dan mulai nonton gratis."
            else "Masuk untuk melanjutkan tontonanmu.",
            color = TextSecondary,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = Space.xs, bottom = Space.xl)
        )

        SegmentedAuth(
            register = register,
            onChange = {
                register = it
                submitted = false
            }
        )
        Spacer(Modifier.height(Space.xl))

        if (register) {
            OutlinedTextField(
                name, { name = it },
                Modifier.fillMaxWidth(),
                label = { Text("Nama pengguna") },
                leadingIcon = { Icon(Icons.Rounded.Person, null, tint = TextSecondary) },
                singleLine = true,
                isError = submitted && !nameOk,
                supportingText = if (submitted && !nameOk) ({
                    Text("Nama tidak boleh kosong", color = Crimson, fontSize = 11.sp)
                }) else null,
                shape = RoundedCornerShape(Radius.lg),
                colors = colors
            )
            Spacer(Modifier.height(Space.md))
        }

        OutlinedTextField(
            email, { email = it },
            Modifier.fillMaxWidth(),
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Rounded.MailOutline, null, tint = TextSecondary) },
            singleLine = true,
            isError = submitted && !emailOk,
            supportingText = if (submitted && !emailOk) ({
                Text("Format email belum benar", color = Crimson, fontSize = 11.sp)
            }) else null,
            shape = RoundedCornerShape(Radius.lg),
            colors = colors,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        Spacer(Modifier.height(Space.md))
        OutlinedTextField(
            pass, { pass = it },
            Modifier.fillMaxWidth(),
            label = { Text("Kata sandi") },
            leadingIcon = { Icon(Icons.Rounded.Lock, null, tint = TextSecondary) },
            singleLine = true,
            isError = submitted && !passOk,
            supportingText = if (submitted && !passOk) ({
                Text("Sandi minimal 6 karakter", color = Crimson, fontSize = 11.sp)
            }) else null,
            shape = RoundedCornerShape(Radius.lg),
            colors = colors,
            visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton({ show = !show }) {
                    Icon(
                        if (show) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        "Tampilkan sandi", tint = TextSecondary
                    )
                }
            }
        )

        Spacer(Modifier.height(Space.xl))
        PrimaryButton(
            if (register) "Daftar" else "Masuk",
            Modifier.fillMaxWidth().height(52.dp)
        ) {
            submitted = true
            when {
                !nameOk -> vm.toast("Nama tidak boleh kosong")
                !emailOk -> vm.toast("Email tidak valid")
                !passOk -> vm.toast("Sandi minimal 6 karakter")
                else -> onDone()
            }
        }
        Spacer(Modifier.height(Space.md))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).height(1.dp).background(Color.White.copy(.08f)))
            Text("atau", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(horizontal = Space.md))
            Box(Modifier.weight(1f).height(1.dp).background(Color.White.copy(.08f)))
        }
        Spacer(Modifier.height(Space.md))
        GlassButton("Lanjut sebagai tamu", Modifier.fillMaxWidth().height(52.dp), onClick = onDone)
        Text(
            "Dengan melanjutkan, kamu menyetujui Syarat Layanan dan Kebijakan Privasi Onimeku.",
            color = TextSecondary,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = Space.xl)
        )
    }
}

@Composable
private fun SegmentedAuth(register: Boolean, onChange: (Boolean) -> Unit) = Row(
    Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.full)).background(Surface1)
        .border(1.dp, Color.White.copy(.06f), RoundedCornerShape(Radius.full))
        .padding(4.dp)
) {
    listOf(false to "Masuk", true to "Daftar").forEach { (isRegister, label) ->
        val selected = register == isRegister
        Box(
            Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(Radius.full))
                .background(if (selected) Crimson else Color.Transparent)
                .clickable { onChange(isRegister) },
            Alignment.Center
        ) {
            Text(
                label,
                color = if (selected) Color.White else TextSecondary,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 13.sp
            )
        }
    }
}
