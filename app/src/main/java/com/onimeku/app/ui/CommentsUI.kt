package com.onimeku.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onimeku.app.AppVM
import com.onimeku.app.I18n
import com.onimeku.app.Route
import com.onimeku.app.data.CommentRow

/**
 * Komentar inline di panel bawah video (menggantikan daftar Episodes).
 * Susunan: pemisah -> judul + jumlah -> kolom tulis -> daftar -> "Lihat semua".
 * Dipakai lewat [commentsSection] di dalam LazyColumn; state diangkat ke [CommentsUi]
 * karena item LazyColumn tidak boleh menyimpan state yang harus bertahan saat digulir.
 */

private const val MAX_LEN = 1000
private const val PREVIEW_COUNT = 5

@Stable
class CommentsUi(
    val text: MutableState<String>,
    val expanded: MutableState<Boolean>,
    val pendingDelete: MutableState<CommentRow?>,
)

@Composable
fun rememberCommentsUi(animeId: String): CommentsUi {
    val text = rememberSaveable(animeId) { mutableStateOf("") }
    val expanded = rememberSaveable(animeId) { mutableStateOf(false) }
    val pending = remember(animeId) { mutableStateOf<CommentRow?>(null) }
    return remember(text, expanded, pending) { CommentsUi(text, expanded, pending) }
}

@Composable
private fun Avatar(name: String, highlight: Boolean, size: Dp = 36.dp, photoUrl: String = "") {
    // Foto akun yang sedang login (sama dengan di tab Profile); huruf awal jadi cadangan selagi foto dimuat/gagal.
    AccountAvatar(photoUrl, name.ifBlank { "U" }.take(1).uppercase(), size, if (highlight) Red else Solid, Color.Transparent, 0.dp, (size.value * 0.4f).sp)
}

fun LazyListScope.commentsSection(vm: AppVM, animeId: String, ui: CommentsUi) {
    // ---- Judul ----
    item(key = "c_head") {
        Column(Modifier.padding(top = Sp.S24)) {
            Box(Modifier.padding(horizontal = Gutter)) { HLine() }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Gutter).padding(top = Sp.S16),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Sp.S8),
            ) {
                Text(I18n.t("comments", "Comments"), fontSize = Ty.Title, fontWeight = FontWeight.Bold)
                if (vm.comments.isNotEmpty()) Text(
                    "${vm.comments.size}", fontSize = Ty.Caption, fontWeight = FontWeight.SemiBold, color = InkSoft,
                    modifier = Modifier.clip(CircleShape).background(Solid).padding(horizontal = Sp.S8, vertical = Sp.S2),
                )
            }
        }
    }

    // ---- Kolom tulis ----
    item(key = "c_compose") { CommentComposer(vm, animeId, ui) }

    // ---- Daftar ----
    val list = vm.comments
    when {
        vm.commentsBusy && list.isEmpty() -> item(key = "c_busy") {
            CommentSkeleton(3, Modifier.padding(horizontal = Gutter, vertical = Sp.S16))
        }
        list.isEmpty() -> item(key = "c_empty") {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = Sp.S28),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(AppIcons.Message, null, Modifier.size(32.dp), tint = NavOff)
                Text(I18n.t("comment_empty_t", "No comments yet"), Modifier.padding(top = Sp.S12), fontSize = Ty.Body, fontWeight = FontWeight.SemiBold)
                Text(I18n.t("comment_empty_s", "Be the first to share what you think."), Modifier.padding(top = Sp.S4),
                    fontSize = Ty.Small, color = NavOff, textAlign = TextAlign.Center)
            }
        }
        else -> {
            val shown = if (ui.expanded.value) list else list.take(PREVIEW_COUNT)
            items(shown, key = { "comment:${it.id}" }) { c ->
                val mine = c.user_id == vm.s.account?.userId
                CommentItem(c, mine, if (mine) vm.s.account?.avatarUrl.orEmpty() else "") { ui.pendingDelete.value = c }
            }
            if (list.size > PREVIEW_COUNT) item(key = "c_more") {
                val open = ui.expanded.value
                Box(
                    Modifier.fillMaxWidth().padding(horizontal = Gutter).padding(top = Sp.S12).height(42.dp)
                        .clip(CircleShape).border(1.dp, Line, CircleShape).clickable { ui.expanded.value = !open },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (open) I18n.t("comments_less", "Show less") else "${I18n.t("comments_all", "View all comments")} (${list.size})",
                        fontSize = Ty.Small, fontWeight = FontWeight.Medium, color = InkSoft,
                    )
                }
            }
        }
    }
}

@Composable
private fun CommentComposer(vm: AppVM, animeId: String, ui: CommentsUi) {
    val acc = vm.s.account
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Gutter).padding(top = Sp.S16, bottom = Sp.S8),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Sp.S12),
    ) {
        if (acc == null) {
            Avatar("?", false)
            Row(
                Modifier.weight(1f).height(42.dp).clip(CircleShape).background(Bg2).border(1.dp, Line, CircleShape)
                    .clickable { vm.go(Route.Auth(false)) }.padding(horizontal = Sp.S16),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Sp.S8),
            ) {
                Icon(AppIcons.Lock, null, Modifier.size(16.dp), tint = NavOff)
                Text(I18n.t("sign_in_to_comment", "Sign in to comment"), fontSize = Ty.Body, color = NavOff)
            }
            return@Row
        }
        var text by ui.text
        var focused by remember { mutableStateOf(false) }
        val focus = LocalFocusManager.current
        val canSend = text.isNotBlank()
        Avatar(acc.name.ifBlank { acc.email }.ifBlank { vm.p.name }, true, photoUrl = acc.avatarUrl)
        Row(
            Modifier.weight(1f).heightIn(min = 42.dp).clip(RoundedCornerShape(21.dp)).background(Bg2)
                .border(1.dp, if (focused) RedText else Line, RoundedCornerShape(21.dp)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f).padding(start = Sp.S16, top = Sp.S12, bottom = Sp.S12, end = Sp.S8)) {
                BasicTextField(
                    text, { if (it.length <= MAX_LEN) text = it },
                    modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
                    maxLines = 4,
                    textStyle = TextStyle(color = Color.White, fontSize = Ty.Body, lineHeight = 20.sp),
                    cursorBrush = SolidColor(RedText),
                    decorationBox = { inner ->
                        if (text.isEmpty()) Text(I18n.t("comment_hint", "Write a comment…"), color = NavOff, fontSize = Ty.Body)
                        inner()
                    },
                )
            }
            if (canSend) Box(
                Modifier.padding(end = Sp.S4).size(32.dp).clip(CircleShape).background(Red)
                    .clickable { vm.postComment(animeId, text); text = ""; focus.clearFocus() },
                contentAlignment = Alignment.Center,
            ) { Icon(AppIcons.Send, I18n.t("comment_post", "Post"), Modifier.size(16.dp), tint = Color.White) }
        }
    }
}

@Composable
private fun CommentItem(c: CommentRow, mine: Boolean, photoUrl: String, onDelete: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Gutter).padding(top = Sp.S16),
        horizontalArrangement = Arrangement.spacedBy(Sp.S12),
    ) {
        Avatar(c.author, mine, photoUrl = photoUrl)
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Sp.S8)) {
                Text(
                    c.author.ifBlank { "User" }, fontSize = Ty.Small, fontWeight = FontWeight.SemiBold, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, color = if (mine) RedText else Color.White,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (mine) Text(
                    I18n.t("comment_you", "You"), fontSize = Ty.Micro, fontWeight = FontWeight.SemiBold, color = RedText,
                    modifier = Modifier.clip(CircleShape).background(Color(0x26E8566F)).padding(horizontal = Sp.S8, vertical = 1.dp),
                )
                // Parse ISO + buat DateTimeFormatter dulu terjadi pada SETIAP rekomposisi tiap komentar; kini sekali per komentar.
                val time = remember(c.created_at) { commentTime(c.created_at) }
                Text(time, fontSize = Ty.Caption, color = NavOff, maxLines = 1)
                Spacer(Modifier.weight(1f))
                if (mine) Box(
                    Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onDelete),
                    contentAlignment = Alignment.Center,
                ) { Icon(AppIcons.Trash, I18n.t("comment_delete", "Delete"), Modifier.size(17.dp), tint = NavOff) }
            }
            Text(c.body, Modifier.padding(top = Sp.S4), fontSize = Ty.Body, color = InkSoft, lineHeight = 21.sp)
        }
    }
}

/** Dialog konfirmasi hapus; letakkan di luar LazyColumn. */
@Composable
fun CommentDeleteDialog(vm: AppVM, animeId: String, ui: CommentsUi) {
    val c = ui.pendingDelete.value ?: return
    AlertDialog(
        onDismissRequest = { ui.pendingDelete.value = null },
        title = { Text(I18n.t("comment_delete_q", "Delete this comment?")) },
        confirmButton = {
            TextButton(onClick = { vm.deleteComment(animeId, c.id); ui.pendingDelete.value = null }) {
                Text(I18n.t("comment_delete", "Delete"), color = RedText)
            }
        },
        dismissButton = { TextButton(onClick = { ui.pendingDelete.value = null }) { Text(I18n.t("cancel", "Cancel")) } },
    )
}

/** Waktu relatif untuk <24 jam ("5 mnt", "3 jam"), selebihnya tanggal dalam zona waktu perangkat ("19 Okt 14:05"). */
internal fun commentTime(iso: String): String = runCatching {
    val t = java.time.OffsetDateTime.parse(iso)
    val mins = java.time.Duration.between(t, java.time.OffsetDateTime.now()).toMinutes()
    when {
        mins < 1 -> I18n.t("time_now", "Just now")
        mins < 60 -> "$mins ${I18n.t("time_min", "min")}"
        mins < 24 * 60 -> "${mins / 60} ${I18n.t("time_hour", "h")}"
        else -> t.atZoneSameInstant(java.time.ZoneId.systemDefault())
            .format(java.time.format.DateTimeFormatter.ofPattern("d MMM HH:mm", uiLocale()))
    }
}.getOrElse { if (iso.length >= 16) iso.substring(0, 10) + " " + iso.substring(11, 16) else "" }

