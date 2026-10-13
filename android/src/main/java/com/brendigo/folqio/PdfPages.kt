package com.brendigo.folqio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class RenderedPage(val bitmap: Bitmap, val count: Int)

object PdfPages {
    // Each render owns its own descriptor and PdfRenderer. No shared mutable renderer
    // survives a lifecycle transition or competes across dispatchers.
    suspend fun render(context: Context, uri: Uri, requestedPage: Int): RenderedPage =
        withContext(Dispatchers.IO) {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                PdfRenderer(descriptor).use { renderer ->
                    check(renderer.pageCount > 0) { "PDF has no pages" }
                    val index = requestedPage.coerceIn(0, renderer.pageCount - 1)
                    renderer.openPage(index).use { page ->
                        val ratio = page.width.toFloat() / page.height.coerceAtLeast(1)
                        val width = context.resources.displayMetrics.widthPixels.coerceIn(640, 1400)
                        val height = (width / ratio).toInt().coerceIn(1, 2400)
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        try {
                            bitmap.eraseColor(Color.WHITE)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            RenderedPage(bitmap, renderer.pageCount)
                        } catch (failure: Exception) {
                            bitmap.recycle()
                            throw failure
                        }
                    }
                }
            } ?: error("The selected document cannot be opened")
        }
}
