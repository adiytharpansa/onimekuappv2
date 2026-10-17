package com.onimeku.app.ui

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import com.onimeku.app.data.*
import com.onimeku.app.media.MediaKit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@UnstableApi @Composable
fun DetailScreen(id: String, posterKey: String?, onBack: () -> Unit, onPlay: (Int) -> Unit) {
    val ctx = LocalContext.current; val repo = remember { Repo.of(ctx) }; val scope = rememberCoroutineScope()
    val a = repo.one(id).collectAsState(null).value
    val lists = remember { AppDb.get(ctx).lists() }
    val mine by lists.myList().collectAsState(emptyList())
    val progress by remember { AppDb.get(ctx).progress().forAnime(id) }.collectAsState(emptyList())
    var dls by remember { mutableStateOf<Map<String, Download>>(emptyMap()) }
    LaunchedEffect(Unit) { while (true) { dls = withContext(Dispatchers.IO) { MediaKit.list(ctx).associateBy { it.request.id } }; delay(1000) } }
    if (a == null) { Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }; return }
    fun mmss(ms: Long) = "%d:%02d".format(ms / 60000, ms / 1000 % 60)

    val list = rememberLazyListState(); val haptic = LocalHapticFeedback.current
    val collapse by remember { derivedStateOf { if (list.firstVisibleItemIndex > 0) 1f else (list.firstVisibleItemScrollOffset / 520f).coerceIn(0f, 1f) } }
    var expanded by rememberSaveable { mutableStateOf(false) }
    val inList = id in mine
    val resume = progress.filter { it.positionMs < it.durationMs * 0.9 }.maxByOrNull { it.updatedAt }?.id?.substringAfter(":")?.toIntOrNull()
    val bg = MaterialTheme.colorScheme.background

    Box(Modifier.fillMaxSize()) {
        LazyColumn(state = list, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 112.dp)) {
            // ---- Header: backdrop blur + parallax, poster di depan ----
            item {
                Box(Modifier.fillMaxWidth().height(380.dp)) {
                    PosterImage(a.poster, a.title, Modifier.fillMaxSize().graphicsLayer { translationY = list.firstVisibleItemScrollOffset * 0.4f }.blur(30.dp),
                        radius = 0.dp, alignment = Alignment.TopCenter)
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color(0x66000000), 0.5f to Color(0x22000000), 1f to bg)))
                    Row(Modifier.align(Alignment.BottomStart).padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalAlignment = Alignment.Bottom) {
                        PosterImage(a.poster, a.title, Modifier.width(130.dp).aspectRatio(2f / 3f).shadow(14.dp, RoundedCornerShape(14.dp)), 14.dp,
                            sharedKey = posterKey)
                        Column(Modifier.padding(start = 14.dp)) {
                            Text(a.title, style = MaterialTheme.typography.headlineSmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            Text("${a.eps} episode · " + (if (a.airDay != null) "tayang ${DAYS[a.airDay]} ${a.airTime}" else "selesai tayang"),
                                Modifier.padding(top = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            // ---- Genre, sinopsis, unduh ----
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    if (a.genreList.isNotEmpty()) LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(a.genreList) { g -> Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.secondaryContainer) {
                            Text(g, Modifier.padding(horizontal = 12.dp, vertical = 5.dp), style = MaterialTheme.typography.labelMedium) } } }
                    Column(Modifier.padding(vertical = 14.dp).animateContentSize()) {
                        Text(a.synopsis, maxLines = if (expanded) Int.MAX_VALUE else 4, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (a.synopsis.length > 180) Text(if (expanded) "Sembunyikan" else "Selengkapnya",
                            Modifier.clickable { expanded = !expanded }.padding(top = 6.dp), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton({ scope.launch { (1..a.eps).filter { "$id:$it" !in dls }.forEach { repo.download(id, it) } } }) { Text("Unduh semua") }
                    Text("Episodes", Modifier.padding(top = 20.dp, bottom = 10.dp), style = MaterialTheme.typography.titleLarge)
                }
            }
            // ---- Daftar episode dengan thumbnail ----
            items((1..a.eps).toList()) { n ->
                val p = progress.firstOrNull { it.id == "$id:$n" }; val d = dls["$id:$n"]
                val watched = p != null && p.positionMs >= p.durationMs * 0.9
                val sub = when { p == null -> "24 menit"; watched -> "Ditonton"; else -> "Lanjut di ${mmss(p.positionMs)}" }
                Surface(onClick = { onPlay(n) }, modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 10.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface) {
                    Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box {
                            PosterImage(a.poster, a.title, Modifier.width(120.dp).aspectRatio(16f / 9f), 10.dp, alignment = Alignment.TopCenter)
                            Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.3f)))
                            Icon(Icons.Default.PlayArrow, null, Modifier.align(Alignment.Center).size(28.dp), tint = Color.White)
                            if (p != null && !watched && p.durationMs > 0) LinearProgressIndicator(progress = { (p.positionMs.toFloat() / p.durationMs).coerceIn(0f, 1f) },
                                Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(3.dp), color = MaterialTheme.colorScheme.primary, trackColor = Color.Black.copy(alpha = 0.4f))
                        }
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text("Episode $n", style = MaterialTheme.typography.titleMedium)
                            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        when {
                            d == null -> TextButton({ scope.launch { if (!repo.download(id, n)) Toast.makeText(ctx, "Gagal memulai unduhan", Toast.LENGTH_SHORT).show() } }) { Text("Unduh") }
                            d.state == Download.STATE_COMPLETED -> TextButton({ MediaKit.remove(ctx, d.request.id) }) { Text("Hapus") }
                            else -> TextButton({ MediaKit.remove(ctx, d.request.id) }) { Text("${d.percentDownloaded.toInt().coerceAtLeast(0)}% · Batal") }
                        }
                    }
                }
            }
        }

        // ---- Top bar yang muncul perlahan saat digulir ----
        Surface(modifier = Modifier.fillMaxWidth().align(Alignment.TopStart), color = MaterialTheme.colorScheme.surface.copy(alpha = collapse)) {
            Row(Modifier.statusBarsPadding().height(56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onBack, colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.35f * (1f - collapse)), contentColor = if (collapse > 0.5f) MaterialTheme.colorScheme.onSurface else Color.White)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                Text(a.title, Modifier.padding(start = 8.dp).graphicsLayer { alpha = collapse }, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        // ---- Tombol putar menempel di bawah ----
        Surface(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter), color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
            Row(Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Button({ onPlay(resume ?: 1) }, Modifier.weight(1f).height(52.dp)) {
                    Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text(if (resume != null) "Lanjut Ep $resume" else "Putar Ep 1") }
                Spacer(Modifier.width(10.dp))
                FilledTonalIconButton({ haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); scope.launch { if (inList) lists.del(id) else lists.add(MyItem(id)) } }, Modifier.size(52.dp)) {
                    Icon(if (inList) Icons.Default.Check else Icons.Default.Add, if (inList) "Hapus dari My List" else "Tambah ke My List") }
            }
        }
    }
}
