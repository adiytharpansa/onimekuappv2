package com.onimeku.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.onimeku.app.data.*
import com.onimeku.app.ui.AppViewModel
import com.onimeku.app.ui.components.*
import com.onimeku.app.ui.theme.*

@Composable
fun SearchScreen(vm: AppViewModel, onBack: () -> Unit, onOpen: (Int) -> Unit) {
    val q by vm.query.collectAsState()
    val g by vm.searchGenre.collectAsState()
    val sort by vm.sort.collectAsState()
    val recent by vm.recent.collectAsState()
    val results by vm.results.collectAsState()
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    LazyVerticalGrid(GridCells.Fixed(3), Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 40.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item(span = { GridItemSpan(3) }) {
            Row(Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                IconCircle(Icons.AutoMirrored.Rounded.ArrowBack, "Kembali", bg = Surface2, onClick = onBack)
                Spacer(Modifier.width(8.dp))
                TextField(q, { vm.query.value = it }, Modifier.weight(1f).focusRequester(focus), singleLine = true, placeholder = { Text("Cari anime atau genre") },
                    shape = RoundedCornerShape(26.dp), leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    trailingIcon = { if (q.isNotEmpty()) IconButton({ vm.query.value = "" }) { Icon(Icons.Rounded.Close, "Hapus") } },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { vm.addRecent(q) }),
                    colors = TextFieldDefaults.colors(focusedContainerColor = Surface2, unfocusedContainerColor = Surface2, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, cursorColor = Crimson))
            }
        }
        item(span = { GridItemSpan(3) }) { ChipRow(listOf("Semua") + AnimeRepository.genres, g) { vm.searchGenre.value = it } }
        if (q.isBlank() && recent.isNotEmpty()) {
            item(span = { GridItemSpan(3) }) { Column {
                SectionHeader("Pencarian terakhir", "Hapus") { vm.clearRecent() }
                ChipRow(recent.map { "↺ $it" }, "") { vm.query.value = it.removePrefix("↺ ") } } }
        }
        item(span = { GridItemSpan(3) }) { SectionHeader(if (q.isBlank()) "Pencarian Populer" else "Hasil (${results.size})", "Urut: ${sort.name.let { if (it == "Rating") "Rating" else "Terbaru" }}") {
            vm.sort.value = if (sort == Sort.Rating) Sort.Newest else Sort.Rating } }
        if (results.isEmpty()) item(span = { GridItemSpan(3) }) { EmptyState(Icons.Rounded.SearchOff, "Tidak ada hasil untuk “$q”") }
        items(results, key = { it.id }) { PosterCard(it, width = 120) { vm.addRecent(q); onOpen(it.id) } }
    }
}
