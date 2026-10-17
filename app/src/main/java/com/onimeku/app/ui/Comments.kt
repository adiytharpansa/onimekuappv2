package com.onimeku.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.onimeku.app.data.CommentDto

private fun mmss(ms: Long) = "%d:%02d".format(ms / 60000, ms / 1000 % 60)

/** Panel komentar di sisi kanan pemutar. Tap komentar = loncat ke menit itu. */
@Composable
fun CommentsPanel(modifier: Modifier, comments: List<CommentDto>, canPost: Boolean, message: String?, posMs: Long,
                  onClose: () -> Unit, onSeek: (Long) -> Unit, onPost: (String) -> Unit,
                  onDelete: (CommentDto) -> Unit, onReport: (CommentDto) -> Unit, onBlock: (CommentDto) -> Unit) {
    var text by remember { mutableStateOf("") }
    Surface(modifier.width(330.dp).fillMaxHeight(), color = MaterialTheme.colorScheme.surface.copy(alpha = .96f)) {
        Column(Modifier.statusBarsPadding().imePadding().padding(12.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Komentar (${comments.size})", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                TextButton(onClose) { Text("Tutup") } }
            message?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall) }
            LazyColumn(Modifier.weight(1f)) {
                if (comments.isEmpty()) item { Text("Belum ada komentar. Jadilah yang pertama!", Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(comments, key = { it.id }) { c ->
                    Column(Modifier.fillMaxWidth().clickable { onSeek(c.timeMs) }.padding(vertical = 8.dp)) {
                        Row { Text(c.user, fontWeight = FontWeight.Bold); Text("  ${mmss(c.timeMs)}", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        Text(c.text)
                        Row {
                            if (c.mine) TextButton({ onDelete(c) }, contentPadding = PaddingValues(0.dp)) { Text("Hapus") }
                            else { TextButton({ onReport(c) }, contentPadding = PaddingValues(end = 12.dp)) { Text("Laporkan") }
                                TextButton({ onBlock(c) }, contentPadding = PaddingValues(0.dp)) { Text("Blokir") } } } }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) } }
            if (canPost) Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                OutlinedTextField(text, { text = it.take(200) }, Modifier.weight(1f), placeholder = { Text("Komentar di ${mmss(posMs)}") }, maxLines = 2)
                Button({ if (text.isNotBlank()) { onPost(text); text = "" } }, Modifier.padding(start = 8.dp)) { Text("Kirim") } }
            else Text("Sign in (tab Profile) untuk berkomentar.", Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Dilarang spam, SARA, dan spoiler. Komentar yang dilaporkan ditinjau moderator.", Modifier.padding(top = 6.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
