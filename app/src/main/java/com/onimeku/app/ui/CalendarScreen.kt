package com.onimeku.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onimeku.app.AppVM
import com.onimeku.app.I18n
import com.onimeku.app.Reminders
import com.onimeku.app.Route
import com.onimeku.app.data.Anime
import com.onimeku.app.data.ApiScheduleItem
import com.onimeku.app.data.Catalog
import com.onimeku.app.data.PosterHealer
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as DateTextStyle

/** Satu episode terjadwal: judul katalog + data jadwal + waktu tayang di zona perangkat. */
private data class Slot(val anime: Anime, val item: ApiScheduleItem, val at: ZonedDateTime)

private val Tnum = TextStyle(fontFeatureSettings = "tnum")
private val NextBg = Color(0xFF211620)
private val NextBorder = Color(0xFF5A1426)
private val DotIdle = Color(0xFF3A3E56)
private val TimeInk = Color(0xFFCFD2E6)
private val RAIL = 24.dp
private val TIME_W = 40.dp
private val DOT_Y = 16.dp

// ------------------------------------------------------------------ CALENDAR
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalendarScreen(vm: AppVM) {
    val ensure = LocalEnsureNotif.current
    val zone = remember { ZoneId.systemDefault() }
    val days = remember { (0..6).map { LocalDate.now().plusDays(it.toLong()) } }
    var onlyList by rememberSaveable { mutableStateOf(false) }
    var jumped by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { vm.loadSchedule() }

    // "Sekarang" diperbarui tiap 30 dtk: episode yang jamnya lewat hilang sendiri.
    val nowSec by produceState(System.currentTimeMillis() / 1000) {
        while (true) { delay(30_000); value = System.currentTimeMillis() / 1000 }
    }
    val sched = vm.schedule
    val byAnilist = remember(Catalog.all) {
        Catalog.unique.filter { it.anilistId != null }.groupBy { it.anilistId!! }
            .mapValues { (_, v) -> v.maxByOrNull { it.ep } ?: v.first() }
    }
    val myList = vm.p.myList
    // Hanya episode yang BELUM tayang dan ada di katalog aplikasi.
    val week = remember(sched, byAnilist, nowSec) {
        sched.orEmpty().filter { it.airingAt > nowSec }
            .mapNotNull { s -> byAnilist[s.anilistId]?.let { Slot(it, s, Instant.ofEpochSecond(s.airingAt).atZone(zone)) } }
            .sortedBy { it.item.airingAt }
    }
    val perDay = remember(week, onlyList, myList, days) {
        val base = if (onlyList) week.filter { it.anime.id in myList } else week
        days.map { d -> base.filter { it.at.toLocalDate() == d } }
    }

    // Saat jadwal pertama termuat dan hari ini kosong, loncat ke hari terdekat yang berisi.
    val ready = sched != null && Catalog.all.isNotEmpty()
    LaunchedEffect(ready) {
        if (ready && !jumped) {
            jumped = true
            if (perDay.getOrNull(vm.calDay)?.isEmpty() == true) {
                perDay.indexOfFirst { it.isNotEmpty() }.takeIf { it >= 0 }?.let { vm.calDay = it }
            }
        }
    }

    Page {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f).padding(end = 10.dp)) {
                Text(I18n.t("calendar_t", "Release Calendar"), fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
                val mins = ((nowSec * 1000 - vm.scheduleUpdatedAt) / 60_000).coerceAtLeast(0)
                Text(
                    when {
                        sched == null -> I18n.t("calendar_sub", "Episodes airing this week.")
                        mins < 1 -> I18n.t("cal_updated_now", "Updated just now")
                        else -> I18n.t("cal_updated", "Updated %d min ago").format(mins)
                    },
                    Modifier.padding(top = 4.dp), fontSize = 13.sp, color = NavOff,
                )
            }
            ListFilter(onlyList) { onlyList = !onlyList }
        }

        // Chip tanggal: hari, tanggal, dan maksimal tiga titik penanda jumlah episode.
        Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 20.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            days.forEachIndexed { k, d ->
                val sel = k == vm.calDay
                val n = perDay[k].size
                Column(
                    Modifier.weight(1f).height(78.dp).clip(RoundedCornerShape(18.dp))
                        .background(if (sel) Red else Bg2).border(1.dp, if (sel) Red else Line, RoundedCornerShape(18.dp))
                        .pressable { vm.calDay = k },
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        d.dayOfWeek.getDisplayName(DateTextStyle.SHORT, uiLocale()), fontSize = 11.5.sp, fontWeight = FontWeight.Medium,
                        color = if (sel) Color(0xFFFFD3DB) else if (k == 0) RedText else NavOff,
                    )
                    Text("${d.dayOfMonth}", Modifier.padding(top = 2.dp), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Row(Modifier.padding(top = 5.dp).height(5.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        repeat(minOf(n, 3)) { Box(Modifier.size(4.dp).clip(CircleShape).background(if (sel) Color.White else DotIdle)) }
                    }
                }
            }
        }

        when {
            sched == null && vm.scheduleFailed -> EmptyBox(
                I18n.t("schedule_fail_t", "Couldn't load schedule"),
                I18n.t("schedule_fail_b", "Check your connection and try again."),
            ) { TextButton(onClick = { vm.loadSchedule(force = true) }) { Text(I18n.t("retry", "Retry")) } }
            !ready -> SkeletonRows(5)
            else -> key(vm.calDay) {
                Column(Modifier.fadeSlideIn()) {
                    val list = perDay[vm.calDay]
                    if (list.isEmpty()) {
                        EmptyBox(
                            I18n.t("cal_empty_t", "No releases today"),
                            if (onlyList) I18n.t("cal_empty_ml", "None of your My List titles air on this day. Turn off the filter to see everything.")
                            else I18n.t("nothing_airing_b", "No new episodes are scheduled for this day."),
                        )
                    } else {
                        val today = vm.calDay == 0
                        list.groupBy { periodOf(it, nowSec, today) }.forEach { (period, slots) ->
                            Text(
                                periodLabel(period), Modifier.padding(start = TIME_W + RAIL, top = 8.dp, bottom = 10.dp),
                                fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NavOff,
                            )
                            slots.forEachIndexed { i, s ->
                                TimelineRow(s, i == 0, i == slots.lastIndex, next = today && s === list.first(), nowSec, vm, ensure)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun periodOf(s: Slot, nowSec: Long, today: Boolean): String {
    if (today && s.item.airingAt - nowSec < 3600) return "soon"
    return when (s.at.hour) { in 0..4 -> "dawn"; in 5..10 -> "morning"; in 11..14 -> "noon"; in 15..17 -> "afternoon"; else -> "night" }
}

private fun periodLabel(p: String) = when (p) {
    "soon" -> I18n.t("cal_soon", "Soon")
    "dawn" -> I18n.t("cal_dawn", "Early morning")
    "morning" -> I18n.t("cal_morning", "Morning")
    "noon" -> I18n.t("cal_noon", "Midday")
    "afternoon" -> I18n.t("cal_afternoon", "Afternoon")
    else -> I18n.t("cal_night", "Night")
}

private fun etaText(sec: Long): String {
    val m = (sec / 60).coerceAtLeast(1)
    val body = if (m >= 60) "${m / 60} ${I18n.t("unit_h", "h")} ${m % 60} ${I18n.t("unit_min", "min")}" else "$m ${I18n.t("unit_min", "min")}"
    return I18n.t("cal_in", "in %s").format(body)
}

@Composable
private fun TimelineRow(
    s: Slot, first: Boolean, last: Boolean, next: Boolean, nowSec: Long, vm: AppVM,
    ensure: ((Boolean) -> Unit) -> Unit,
) {
    val a = s.anime
    val reminded = vm.p.remindEp.any { it.startsWith("${a.id}|${s.item.ep}|") }
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Text(
            s.at.format(DateTimeFormatter.ofPattern("HH:mm")), Modifier.width(TIME_W).padding(top = 7.dp),
            fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TimeInk, textAlign = TextAlign.End, style = Tnum,
        )
        // Rel timeline: garis vertikal + titik; titik episode berikutnya menyala merah.
        Box(Modifier.width(RAIL).fillMaxHeight().drawBehind {
            val x = size.width / 2; val y = DOT_Y.toPx()
            drawLine(Line, Offset(x, if (first) y else 0f), Offset(x, if (last) y else size.height), 1.dp.toPx())
            if (next) drawCircle(Red.copy(alpha = 0.3f), 8.dp.toPx(), Offset(x, y))
            drawCircle(if (next) RedText else DotIdle, 4.5.dp.toPx(), Offset(x, y))
        })
        Box(Modifier.weight(1f).padding(bottom = 10.dp)) {
            EpisodeCard(s, next, reminded, nowSec, onOpen = { vm.go(Route.Detail(a.id)) }) {
                vm.toggleRemindEpisode(a.id, s.item.ep, s.item.airingAt, ensure)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EpisodeCard(s: Slot, next: Boolean, reminded: Boolean, nowSec: Long, onOpen: () -> Unit, onBell: () -> Unit) {
    val a = s.anime
    val shape = RoundedCornerShape(20.dp)
    val today = s.at.toLocalDate() == LocalDate.now()
    Row(
        Modifier.fillMaxWidth().clip(shape).background(if (next) NextBg else Bg2)
            .border(1.dp, if (next) NextBorder else Line, shape).clickable(onClick = onOpen).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(62.dp, 86.dp).clip(RoundedCornerShape(13.dp)).artBackground(a)) {
            LoadedImage(PosterHealer.posterFor(a) ?: PosterHealer.coverFor(a), a.title, Modifier.fillMaxSize(), pending = !a.poster.isNullOrBlank())
        }
        Column(Modifier.weight(1f)) {
            Text(a.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, lineHeight = 19.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(Modifier.padding(top = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Episode ${s.item.ep}", fontSize = 12.5.sp, color = NavOff)
                a.genres.firstOrNull()?.let { g ->
                    Box(Modifier.padding(horizontal = 8.dp).width(1.dp).height(11.dp).background(Line))
                    Text(g, fontSize = 12.5.sp, color = NavOff, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            FlowRow(Modifier.padding(top = 9.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (next) CountdownPill(s.item.airingAt)
                else if (today) Pill(etaText(s.item.airingAt - nowSec), Color(0xFF1D2033), Color(0xFFA3A7C0))
                if (s.item.ep == 1) Pill(I18n.t("cal_premiere", "Premiere"), Color(0x298B5CF6), Color(0xFFC4B5FD))
                if (a.eps > 1 && s.item.ep == a.eps) Pill(I18n.t("cal_finale", "Finale"), Color(0x2434D399), Color(0xFF6EE7B7))
            }
        }
        IconChip(
            if (reminded) Icons.Filled.NotificationsActive else Icons.Outlined.NotificationsNone,
            if (reminded) I18n.t("reminder_on", "Reminder on") else I18n.t("remind_me", "Remind me"), reminded, onBell,
        )
    }
}

/** Hitung mundur hh:mm:ss; hanya komposabel ini yang rekomposisi tiap detik, bukan seluruh layar. */
@Composable
private fun CountdownPill(airingAt: Long) {
    val left by produceState(airingAt - System.currentTimeMillis() / 1000, airingAt) {
        while (true) { value = airingAt - System.currentTimeMillis() / 1000; delay(1000) }
    }
    val l = left.coerceAtLeast(0)
    Pill("%02d:%02d:%02d".format(l / 3600, l % 3600 / 60, l % 60), Color(0x29FF2E58), Color(0xFFFF9FB2))
}

@Composable
private fun Pill(text: String, bg: Color, fg: Color) {
    Text(
        text, Modifier.clip(CircleShape).background(bg).padding(horizontal = 9.dp, vertical = 3.dp),
        fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = fg, style = Tnum,
    )
}

@Composable
private fun ListFilter(on: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.clip(CircleShape).background(if (on) Red else Bg2).border(1.dp, if (on) Red else Line, CircleShape)
            .pressable(onClick).padding(horizontal = 13.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(if (on) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder, null, Modifier.size(16.dp), tint = if (on) Color.White else NavOff)
        Text("My List", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = if (on) Color.White else TimeInk)
    }
}
