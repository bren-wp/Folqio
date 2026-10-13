package com.brendigo.folqio

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun PdfThumbnail(source: String) {
    val context = LocalContext.current
    var bitmap by remember(source) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(source) {
        val result = try {
            withContext(Dispatchers.IO) {
                context.contentResolver.openFileDescriptor(Uri.parse(source), "r")?.use { descriptor ->
                    PdfRenderer(descriptor).use { pdf ->
                        if (pdf.pageCount == 0) return@use null
                        pdf.openPage(0).use { page ->
                            val ratio = page.height.toFloat() / page.width.coerceAtLeast(1)
                            val width = 100
                            val height = (width * ratio).toInt().coerceIn(1, 160)
                            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { thumbnail ->
                                try {
                                    thumbnail.eraseColor(Color.WHITE)
                                    page.render(thumbnail, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                } catch (failure: Exception) {
                                    thumbnail.recycle()
                                    throw failure
                                }
                            }
                        }
                    }
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
        bitmap = result
    }

    Box(
        modifier = Modifier.size(40.dp, 53.dp)
            .background(ComposeColor.White, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(40.dp, 53.dp)
            )
        } else {
            Icon(Icons.Default.Description, contentDescription = null,
                tint = ComposeColor(0xFF22D3A7))
        }
    }
}
