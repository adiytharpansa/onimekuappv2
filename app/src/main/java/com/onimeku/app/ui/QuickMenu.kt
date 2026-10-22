package com.onimeku.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onimeku.app.AppVM
import com.onimeku.app.I18n
import com.onimeku.app.Route
import com.onimeku.app.data.Anime
import com.onimeku.app.data.Catalog

/** Dipasang di MainActivity; Poster memanggilnya saat ditekan lama. null = tanpa menu. */
val LocalQuickMenu = staticCompositionLocalOf<((Anime) -> Unit)?> { null }

/** Menu aksi cepat: Daftarku, pengingat, unduh episode pertama, bagikan — tanpa membuka Detail. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickMenuSheet(vm: AppVM, id: String) {
    val a = Catalog.byId[id] ?: return
    val ctx = LocalContext.current
    val ensure = LocalEnsureNotif.current
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val inList = id in vm.p.myList
    val reminded = id in vm.p.remind
    val firstEp = a.available.firstOrNull() ?: 1
    val canDownload = a.apiStatus != "NOT_YET_RELEASED" && (a.ep > 0 || a.available.isNotEmpty())
    val canRemind = a.status != "Completed"

    @Composable fun row(icon: ImageVector, label: String, on: Boolean = false, action: () -> Unit) {
        Row(
            Modifier.fillMaxWidth().clickable { vm.quick = null; action() }.padding(horizontal = 4.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(icon, null, Modifier.size(24.dp), tint = if (on) RedText else Color.White)
            Text(label, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = if (on) RedText else Color.White)
        }
    }

    ModalBottomSheet(onDismissRequest = { vm.quick = null }, sheetState = sheet, containerColor = Bg2, contentColor = Color.White) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 20.dp).navigationBarsPadding()) {
            Text(a.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val meta = a.genres.take(3).joinToString("  ·  ")
            if (meta.isNotBlank()) Text(meta, Modifier.padding(top = 3.dp), fontSize = 13.sp, color = NavOff, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(10.dp))
            HLine()
            row(if (inList) AppIcons.Check else AppIcons.Plus,
                if (inList) I18n.t("qm_in_list", "Remove from My List") else I18n.t("qm_add_list", "Add to My List"), on = inList) { vm.toggleML(id) }
            if (canRemind) row(if (reminded) AppIcons.BellFill else AppIcons.Bell,
                if (reminded) I18n.t("qm_unremind", "Remove reminder") else I18n.t("qm_remind", "Remind me"), on = reminded) { vm.toggleRemind(id, ensure) }
            if (canDownload) row(AppIcons.Download, "${I18n.t("qm_download", "Download episode")} $firstEp") { vm.download(id, firstEp) }
            row(AppIcons.Share, I18n.t("share", "Share")) {
                val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_TEXT, "Watch ${a.title} on Onimeku: onimeku://anime/$id")
                }
                ctx.startActivity(android.content.Intent.createChooser(send, I18n.t("share", "Share")).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            row(AppIcons.Info, I18n.t("qm_details", "Open details")) { vm.go(Route.Detail(id)) }
        }
    }
}
