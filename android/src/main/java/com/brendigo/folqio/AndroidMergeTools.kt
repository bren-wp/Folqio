package com.brendigo.folqio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
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
 * Local, raster-only merging for Android's system PdfRenderer.
 * Does not preserve selectable text, form fields, vector paths or signatures.
 * The source files are never modified. Users must not rely on this for redaction.
 */
object AndroidMergeTools {
    fun merge(context: Context, sources: List<Uri>): LocalPdf {
        require(MergePolicy.validFiles(sources.size)) { "Choose two to five PDF files." }
        require(sources.all { it.scheme == "content" } &&
                sources.map(Uri::toString).distinct().size == sources.size
        ) { "Choose distinct PDF files from the system picker." }

        val resolver = context.contentResolver
        val folder = File(context.filesDir, "created").apply { mkdirs() }
        val destination = File(folder, UUID.randomUUID().toString() + ".pdf")
        val output = PdfDocument()
        var totalPages = 0

        try {
            for (uri in sources) {
                resolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                    require(MergePolicy.validReportedSize(descriptor.statSize)) {
                        "Choose a PDF smaller than 150 MB."
                    }
                    PdfRenderer(descriptor).use { renderer ->
                        require(renderer.pageCount > 0) { "An input PDF has no pages." }
                        require(totalPages + renderer.pageCount <= MergePolicy.MAX_PAGES) {
                            "Merged documents cannot exceed ${MergePolicy.MAX_PAGES} pages."
                        }
                        for (index in 0 until renderer.pageCount) {
                            renderer.openPage(index).use { sourcePage ->
                                val nativeWidth = sourcePage.width.coerceAtLeast(1)
                                val nativeHeight = sourcePage.height.coerceAtLeast(1)
                                val factor = min(1f, min(1200f / nativeWidth, 1700f / nativeHeight))
                                val width = (nativeWidth * factor).toInt().coerceAtLeast(1)
                                val height = (nativeHeight * factor).toInt().coerceAtLeast(1)
                                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                                try {
                                    bitmap.eraseColor(Color.WHITE)
                                    sourcePage.render(
                                        bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT
                                    )
                                    val page = output.startPage(
                                        PdfDocument.PageInfo.Builder(width, height, totalPages + 1).create()
                                    )
                                    page.canvas.drawColor(Color.WHITE)
                                    page.canvas.drawBitmap(
                                        bitmap, null, RectF(0f, 0f, width.toFloat(), height.toFloat()),
                                        Paint(Paint.FILTER_BITMAP_FLAG)
                                    )
                                    output.finishPage(page)
                                } finally {
                                    bitmap.recycle()
                                }
                            }
                            totalPages++
                        }
                    }
                } ?: error("An input PDF cannot be opened.")
            }
            require(MergePolicy.validPages(totalPages)) { "No valid pages were selected." }
            FileOutputStream(destination).use { stream -> output.writeTo(stream) }
        } catch (failure: Exception) {
            destination.delete()
            throw failure
        } finally {
            output.close()
        }

        val uri = FileProvider.getUriForFile(context, context.packageName + ".files", destination)
        return LocalPdf(uri.toString(), "Merged PDF ($totalPages pages, flattened).pdf")
    }
}
