package com.onimeku.app.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onimeku.app.AppVM
import com.onimeku.app.I18n
import com.onimeku.app.Route
import com.onimeku.app.data.CommentRow

/**
 * Komentar di halaman pemutar. Dua bagian:
 *  - [CommentsPreview]: kartu ringkas di bawah sinopsis (judul, jumlah, komentar terbaru).
 *    Tidak mendorong daftar episode jauh ke bawah, berapa pun jumlah komentarnya.
 *  - [CommentsSheet]: bottom sheet penuh (daftar + kolom tulis menempel di bawah, naik saat keyboard muncul).
 */

private const val MAX_LEN = 1000

@Composable
private fun Avatar(name: String, mine: Boolean, size: androidx.compose.ui.unit.Dp = 36.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(if (mine) Red else Solid), contentAlignment = Alignment.Center) {
        Text(name.ifBlank { "U" }.take(1).uppercase(), fontSize = (size.value * 0.4f).sp, fontWeight = FontWeight.Bold)
    }
}

/** Kartu pratinjau komentar: ketuk di mana saja untuk membuka sheet. */
@Composable
fun CommentsPreview(vm: AppVM, modifier: Modifier = Modifier, onOpen: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val latest = vm.comments.firstOrNull()
    Column(
        modifier.fillMaxWidth().clip(shape).background(Bg2).border(BorderStroke(1.dp, Line), shape)
            .clickable(onClick = onOpen).padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(I18n.t("comments", "Comments"), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            if (vm.comments.isNotEmpty()) Text(
                "${vm.comments.size}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = InkSoft,
                modifier = Modifier.clip(CircleShape).background(Solid).padding(horizontal = 8.dp, vertical = 2.dp),
            )
            Spacer(Modifier.weight(1f))
            Text(I18n.t("comments_open", "Open"), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = RedText)
        }
        Spacer(Modifier.height(10.dp))
        when {
            vm.commentsBusy && latest == null -> CommentSkeleton(1)
            latest != null -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Avatar(latest.author, latest.user_id == vm.s.account?.userId, 28.dp)
                Column(Modifier.weight(1f)) {
                    Text(latest.author.ifBlank { "User" }, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = InkSoft, maxLines = 1)
                    Text(latest.body, Modifier.padding(top = 1.dp), fontSize = 13.sp, color = InkSoft, lineHeight = 18.sp,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            else -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Outlined.ChatBubbleOutline, null, Modifier.size(18.dp), tint = NavOff)
                Text(I18n.t("comment_empty", "No comments yet. Be the first."), fontSize = 13.sp, color = NavOff)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsSheet(vm: AppVM, animeId: String, onDismiss: () -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()
    val loggedIn = vm.s.account != null
    var text by rememberSaveable(animeId) { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<CommentRow?>(null) }
    val sheetH = (LocalConfiguration.current.screenHeightDp * 0.82f).dp

    // Komentar baru muncul di atas: gulir ke atas agar langsung terlihat.
    val topId = vm.comments.firstOrNull()?.id
    LaunchedEffect(topId) { if (topId != null) listState.animateScrollToItem(0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss, sheetState = state,
        containerColor = Bg2, contentColor = Color.White,
    ) {
        Column(Modifier.fillMaxWidth().height(sheetH).imePadding()) {
            // ---- Header ----
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(I18n.t("comments", "Comments"), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                if (vm.comments.isNotEmpty()) Text("  ${vm.comments.size}", fontSize = 14.sp, color = NavOff)
                Spacer(Modifier.weight(1f))
                Box(Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onDismiss), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Close, "Close", Modifier.size(22.dp), tint = Color.White)
                }
            }
            HLine()

            // ---- Daftar ----
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    vm.commentsBusy && vm.comments.isEmpty() ->
                        CommentSkeleton(4, Modifier.padding(horizontal = 20.dp, vertical = 18.dp))
                    vm.comments.isEmpty() -> Column(
                        Modifier.fillMaxSize().padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(Icons.Outlined.ChatBubbleOutline, null, Modifier.size(36.dp), tint = NavOff)
                        Text(I18n.t("comment_empty", "No comments yet. Be the first."), Modifier.padding(top = 12.dp),
                            fontSize = 14.sp, color = NavOff)
                    }
                    else -> LazyColumn(state = listState, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 8.dp)) {
                        items(vm.comments, key = { "comment:${it.id}" }) { c ->
                            val mine = c.user_id == vm.s.account?.userId
                            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Avatar(c.author, mine)
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(c.author.ifBlank { "User" }, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                            color = if (mine) RedText else Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false))
                                        Text(commentTime(c.created_at), fontSize = 11.sp, color = NavOff, maxLines = 1)
                                    }
                                    Text(c.body, Modifier.padding(top = 3.dp), fontSize = 14.sp, color = InkSoft, lineHeight = 20.sp)
                                }
                                if (mine) Box(
                                    Modifier.size(32.dp).clip(CircleShape).clickable { pendingDelete = c },
                                    contentAlignment = Alignment.Center,
                                ) { Icon(Icons.Outlined.DeleteOutline, I18n.t("comment_delete", "Delete"), Modifier.size(18.dp), tint = NavOff) }
                            }
                        }
                    }
                }
            }

            // ---- Kolom tulis (menempel di bawah) ----
            HLine()
            Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
                if (loggedIn) {
                    val canSend = text.isNotBlank()
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            Modifier.weight(1f).heightIn(min = 44.dp).clip(RoundedCornerShape(22.dp)).background(Bg)
                                .border(1.dp, Line, RoundedCornerShape(22.dp)).padding(horizontal = 16.dp, vertical = 12.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            BasicTextField(
                                text, { if (it.length <= MAX_LEN) text = it },
                                modifier = Modifier.fillMaxWidth(), maxLines = 4,
                                textStyle = TextStyle(color = Color.White, fontSize = 14.sp, lineHeight = 20.sp),
                                cursorBrush = SolidColor(Red),
                                decorationBox = { inner ->
                                    if (text.isEmpty()) Text(I18n.t("comment_hint", "Write a comment…"), color = NavOff, fontSize = 14.sp)
                                    inner()
                                },
                            )
                        }
                        Box(
                            Modifier.size(44.dp).clip(CircleShape).background(if (canSend) Red else Solid)
                                .clickable(enabled = canSend) { vm.postComment(animeId, text); text = "" },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, I18n.t("comment_post", "Post"), Modifier.size(20.dp),
                                tint = if (canSend) Color.White else NavOff)
                        }
                    }
                } else {
                    Btn(I18n.t("sign_in_to_comment", "Sign in to comment"), BtnStyle.Solid, modifier = Modifier.fillMaxWidth(), height = 46.dp) {
                        onDismiss(); vm.go(Route.Auth(false))
                    }
                }
            }
        }
    }

    pendingDelete?.let { c ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(I18n.t("comment_delete_q", "Delete this comment?")) },
            confirmButton = {
                TextButton(onClick = { vm.deleteComment(animeId, c.id); pendingDelete = null }) {
                    Text(I18n.t("comment_delete", "Delete"), color = RedText)
                }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text(I18n.t("cancel", "Cancel")) } },
        )
    }
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
