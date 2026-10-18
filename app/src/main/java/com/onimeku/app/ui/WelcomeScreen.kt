package com.onimeku.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onimeku.app.AppVM
import kotlinx.coroutines.delay

private val slides = listOf(
    Triple("Welcome To\n", "Onimeku.", "Watch Anime Online, Streaming Online"),
    Triple("Thousands\nof ", "Episodes.", "Pick from a big library in HD, with new episodes every day."),
    Triple("Watch\n", "Anywhere.", "Download your favorite episodes and keep watching offline."),
)

@Composable
fun WelcomeScreen(vm: AppVM) {
    var idx by remember { mutableIntStateOf(0) }
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(tick) { while (true) { delay(4500); idx = (idx + 1) % slides.size } }

    Box(Modifier.fillMaxSize().background(Color(0xFF161821))) {
        Canvas(Modifier.fillMaxWidth().fillMaxHeight(0.64f)) {
            val w = size.width; val h = size.height
            fun strip(top: Float, hh: Float, brush: Brush) =
                rotate(-12f, Offset(w / 2, top + hh / 2)) { drawRect(brush, Offset(-0.15f * w, top), Size(1.3f * w, hh)) }
            strip(h * 0.06f, h * 0.36f, Brush.linearGradient(listOf(Color(0xFF6F8A4A), Color(0xFFD6B46A), Color(0xFF2D6A9F)), Offset.Zero, Offset(w, h * 0.3f)))
            strip(h * 0.34f, h * 0.30f, Brush.linearGradient(listOf(Color(0xFF8A1218), Color(0xFFC0151D), Color(0xFF3A0B0F)), Offset.Zero, Offset(w, h * 0.3f)))
            strip(h * 0.58f, h * 0.46f, Brush.linearGradient(listOf(Color(0xFF4A2C26), Color(0xFFB7653C), Color(0xFF7B3B2A)), Offset.Zero, Offset(w, h * 0.3f)))
            drawCircle(Color(0xFFF4C28A), w * 0.07f, Offset(w * 0.38f, h * 0.20f))
            drawCircle(Color(0xFFE9A58B), w * 0.08f, Offset(w * 0.62f, h * 0.72f))
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            0.30f to Color.Transparent, 0.56f to Color(0xEB161821), 0.68f to Color(0xFF161821))))

        Text("Skip for now", Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(16.dp).clip(CircleShape)
            .background(Color(0x8C14161F)).clickable { vm.closeWelcome() }.padding(horizontal = 14.dp, vertical = 8.dp),
            fontSize = 13.sp, fontWeight = FontWeight.Medium)

        Column(Modifier.align(Alignment.BottomCenter).widthIn(max = 560.dp).navigationBarsPadding().padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            // Teks slide berganti dengan fade + geser halus.
            AnimatedContent(
                targetState = idx,
                transitionSpec = { (fadeIn(tween(450)) + slideInVertically(tween(450)) { it / 6 }) togetherWith fadeOut(tween(200)) },
                label = "welcomeText",
            ) { i ->
                val (pl, e, sb) = slides[i]
                Column {
                    Text(buildAnnotatedString {
                        append(pl); withStyle(SpanStyle(color = Red)) { append(e) }
                    }, fontSize = 46.sp, fontWeight = FontWeight.Bold, lineHeight = 50.sp, letterSpacing = (-1.5).sp)
                    Text(sb, Modifier.padding(top = 16.dp), fontSize = 20.sp, lineHeight = 29.sp, color = Color(0xFFF2F2F6))
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 22.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                slides.indices.forEach { i ->
                    val dotW by animateDpAsState(if (i == idx) 42.dp else 10.dp, tween(300), label = "dotW")
                    val dotC by animateColorAsState(if (i == idx) Red else Color(0xFF5A5F6E), tween(300), label = "dotC")
                    Box(Modifier.padding(start = 9.dp).height(10.dp).width(dotW).clip(CircleShape)
                        .background(dotC).clickable { idx = i; tick++ })
                }
            }
            Row(Modifier.padding(top = 22.dp).fadeSlideIn(150), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Btn("Sign in", BtnStyle.Solid, modifier = Modifier.weight(1f), height = 58.dp) { vm.closeWelcome() }
                Btn("Sign up", BtnStyle.Play, modifier = Modifier.weight(1f), height = 58.dp) { vm.closeWelcome() }
            }
        }
    }
}
