package com.brendigo.folqio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.min

/**
 * PdfRenderer is read-only on Android 26+. These page-copy operations intentionally
 * FLATTEN one rendered PDF page; text and interactive annotations cannot be recovered.
 * They must never be presented as lossless editing or secure redaction.
 */
object AndroidPageTools {
    fun copyPage(context: Context, source: Uri, index: Int, rotateClockwise: Boolean): LocalPdf {
        val folder = File(context.filesDir, "created").apply { mkdirs() }
        val output = File(folder, UUID.randomUUID().toString() + ".pdf")
        try {
            context.contentResolver.openFileDescriptor(source, "r")?.use { descriptor ->
                PdfRenderer(descriptor).use { renderer ->
                    require(index in 0 until renderer.pageCount) { "Invalid page number" }
                    renderer.openPage(index).use { page ->
                        val baseWidth = page.width.coerceAtLeast(1)
                        val baseHeight = page.height.coerceAtLeast(1)
                        val sampling = min(1f, min(1600f / baseWidth, 2200f / baseHeight))
                        val width = (baseWidth * sampling).toInt().coerceAtLeast(1)
                        val height = (baseHeight * sampling).toInt().coerceAtLeast(1)
                        val image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        try {
                            image.eraseColor(Color.WHITE)
                            page.render(image, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                            val paper = PdfDocument()
                            try {
                                val pageWidth = if (rotateClockwise) baseHeight else baseWidth
                                val pageHeight = if (rotateClockwise) baseWidth else baseHeight
                                val destination = paper.startPage(
                                    PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
                                )
                                destination.canvas.drawColor(Color.WHITE)
                                if (rotateClockwise) {
                                    destination.canvas.translate(pageWidth.toFloat(), 0f)
                                    destination.canvas.rotate(90f)
                                }
                                destination.canvas.drawBitmap(
                                    image, null, RectF(0f, 0f, baseWidth.toFloat(), baseHeight.toFloat()),
                                    Paint(Paint.FILTER_BITMAP_FLAG)
                                )
                                paper.finishPage(destination)
                                FileOutputStream(output).use { paper.writeTo(it) }
                            } finally { paper.close() }
                        } finally { image.recycle() }
                    }
                }
            } ?: error("This PDF is unavailable")
        } catch (failure: Exception) {
            output.delete()
            throw failure
        }
        val uri = FileProvider.getUriForFile(context, context.packageName + ".files", output)
        val label = if (rotateClockwise) "Rotated page" else "Extracted page"
        return LocalPdf(uri.toString(), "$label ${index + 1} (flattened).pdf")
    }
}
