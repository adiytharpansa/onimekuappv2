package com.onimeku.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onimeku.app.data.AnimeRepository
import com.onimeku.app.data.Sort
import com.onimeku.app.ui.AppViewModel
import com.onimeku.app.ui.components.Chip
import com.onimeku.app.ui.components.ChipRow
import com.onimeku.app.ui.components.EmptyState
import com.onimeku.app.ui.components.IconCircle
import com.onimeku.app.ui.components.PosterCard
import com.onimeku.app.ui.components.SectionHeader
import com.onimeku.app.ui.theme.Crimson
import com.onimeku.app.ui.theme.Radius
import com.onimeku.app.ui.theme.Space
import com.onimeku.app.ui.theme.Surface1
import com.onimeku.app.ui.theme.Surface2
import com.onimeku.app.ui.theme.TextSecondary

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SearchScreen(vm: AppViewModel, onBack: () -> Unit, onOpen: (Int) -> Unit) {
    val q by vm.query.collectAsState()
    val g by vm.searchGenre.collectAsState()
    val sort by vm.sort.collectAsState()
    val recent by vm.recent.collectAsState()
    val results by vm.results.collectAsState()
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) { focus.requestFocus() }

    LazyVerticalGrid(
        GridCells.Fixed(3),
        Modifier.fillMaxSize().navigationBarsPadding(),
        contentPadding = PaddingValues(start = Space.lg, end = Space.lg, bottom = Space.xxl),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(span = { GridItemSpan(3) }) {
            Row(
                Modifier.statusBarsPadding().padding(vertical = Space.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconCircle(
                    Icons.AutoMirrored.Rounded.ArrowBack, "Kembali",
                    bg = Surface2, onClick = onBack
                )
                Spacer(Modifier.width(Space.sm))
                TextField(
                    q,
                    { vm.query.value = it },
                    Modifier.weight(1f).focusRequester(focus),
                    singleLine = true,
                    placeholder = { Text("Cari anime, genre, studio…", maxLines = 1) },
                    leadingIcon = { Icon(Icons.Rounded.Search, null, tint = TextSecondary) },
                    trailingIcon = {
                        if (q.isNotEmpty()) IconButton({ vm.query.value = "" }) {
                            Icon(Icons.Rounded.Close, "Hapus", tint = TextSecondary)
                        }
                    },
                    shape = RoundedCornerShape(Radius.full),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        vm.addRecent(q)
                        keyboard?.hide()
                    }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Surface1,
                        unfocusedContainerColor = Surface1,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Crimson
                    )
                )
            }
        }

        item(span = { GridItemSpan(3) }) {
            ChipRow(listOf("Semua") + AnimeRepository.genres, g) { vm.searchGenre.value = it }
        }

        if (recent.isNotEmpty() && q.isBlank()) {
            item(span = { GridItemSpan(3) }) {
                Column {
                    SectionHeader("Pencarian terakhir", "Hapus") { vm.clearRecent() }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(Space.sm),
                        contentPadding = PaddingValues(horizontal = Space.lg)
                    ) {
                        items(recent) { r ->
                            Chip(r, false, leadingIcon = Icons.Rounded.History) {
                                vm.query.value = r
                                vm.addRecent(r)
                            }
                        }
                    }
                }
            }
        }

        item(span = { GridItemSpan(3) }) {
            Row(
                Modifier.fillMaxWidth().padding(top = Space.sm, bottom = Space.xxs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    when {
                        q.isNotBlank() -> "Hasil untuk “$q”"
                        g != "Semua" -> "Genre $g"
                        else -> "Pencarian populer"
                    },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${results.size} judul",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(Modifier.width(Space.md))
                Row(
                    Modifier.clip(RoundedCornerShape(Radius.full)).background(Surface1)
                        .clickable { vm.sort.value = if (sort == Sort.Rating) Sort.Newest else Sort.Rating }
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Sort, null, tint = Crimson, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (sort == Sort.Rating) "Rating" else "Terbaru",
                        color = Crimson, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        if (results.isEmpty()) {
            item(span = { GridItemSpan(3) }) {
                EmptyState(
                    Icons.Rounded.SearchOff,
                    if (q.isBlank()) "Belum ada hasil" else "Tidak ada hasil untuk “$q”",
                    "Coba kata kunci lain atau ubah filter genre."
                )
            }
        }

        items(results, key = { it.id }) { a ->
            PosterCard(a, Modifier.fillMaxWidth()) {
                vm.addRecent(q)
                onOpen(a.id)
            }
        }
    }
}
