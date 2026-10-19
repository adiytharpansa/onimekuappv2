package com.onimeku.app.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Warna dominan poster, dipakai menyepuh gradasi/tombol hero. Dihitung sekali per URL dari
 * versi 48x48 piksel (lewat Coil, memakai cache gambar yang sama), lalu disimpan di memori.
 * Piksel gelap/pucat diabaikan dan hasilnya dijaga cukup gelap supaya teks putih tetap terbaca.
 */
private object PosterColors {
    val cache = mutableStateMapOf<String, Color>()
    val tried = HashSet<String>()
}

@Composable
fun rememberPosterColor(url: String?): Color? {
    if (url.isNullOrBlank()) return null
    val ctx = LocalContext.current
    LaunchedEffect(url) {
        if (PosterColors.cache.containsKey(url) || !PosterColors.tried.add(url)) return@LaunchedEffect
        val c = withContext(Dispatchers.IO) { extractColor(ctx, url) }
        if (c != null) PosterColors.cache[url] = c
    }
    return PosterColors.cache[url]
}

private suspend fun extractColor(ctx: Context, url: String): Color? = try {
    val req = ImageRequest.Builder(ctx).data(url).size(48).allowHardware(false).build()
    val res = ctx.imageLoader.execute(req) as? SuccessResult
    val bmp = res?.drawable?.toBitmap(48, 48)
    if (bmp == null) null else {
        val px = IntArray(48 * 48)
        bmp.getPixels(px, 0, 48, 0, 0, 48, 48)
        val hsv = FloatArray(3)
        var r = 0.0; var g = 0.0; var b = 0.0; var wsum = 0.0
        for (c in px) {
            android.graphics.Color.colorToHSV(c, hsv)
            val s = hsv[1]; val v = hsv[2]
            if (v < 0.2f || (s < 0.15f && v > 0.85f)) continue
            val w = ((s * s) + 0.05f) * v
            r += android.graphics.Color.red(c) * w
            g += android.graphics.Color.green(c) * w
            b += android.graphics.Color.blue(c) * w
            wsum += w
        }
        if (wsum <= 0.0) null else {
            val avg = android.graphics.Color.rgb((r / wsum).toInt(), (g / wsum).toInt(), (b / wsum).toInt())
            android.graphics.Color.colorToHSV(avg, hsv)
            if (hsv[1] < 0.12f) null // poster nyaris abu-abu: biarkan memakai merah tema
            else {
                hsv[1] = hsv[1].coerceIn(0.5f, 0.9f)
                hsv[2] = hsv[2].coerceIn(0.42f, 0.68f)
                Color(android.graphics.Color.HSVToColor(hsv))
            }
        }
    }
} catch (e: Exception) { null }
