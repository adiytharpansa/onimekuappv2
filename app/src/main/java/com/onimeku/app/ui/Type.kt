package com.onimeku.app.ui

import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Skala ukuran huruf resmi. Semua `fontSize` memakai token ini; jangan menulis
 * angka sp pecahan (12,5 / 13,5 dst.). Angka di atas 28sp (hero/skor) boleh lepas.
 */
object Ty {
    val Micro: TextUnit = 11.sp
    val Caption: TextUnit = 12.sp
    val Small: TextUnit = 13.sp
    val Body: TextUnit = 14.sp
    val Subtitle: TextUnit = 16.sp
    val Title: TextUnit = 18.sp
    val Title2: TextUnit = 20.sp
    val Head: TextUnit = 22.sp
    val Head2: TextUnit = 24.sp
    val Display: TextUnit = 28.sp
}
