package com.brendigo.folqio

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.min

/**
 * Creates a real local PDF from a user-selected photo or the external system camera.
 * No network call, broad storage permission, or third-party scanner SDK is involved.
 */
object PhotoPdfTools {
    private const val authoritySuffix = ".files"

    fun newCameraDestination(context: Context): Uri {
        val folder = File(context.filesDir, "captures").apply { mkdirs() }
        val image = File(folder, UUID.randomUUID().toString() + ".jpg")
        return FileProvider.getUriForFile(context, context.packageName + authoritySuffix, image)
    }

    fun createPdf(context: Context, imageUri: Uri): LocalPdf {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(imageUri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        } ?: error("Cannot access this image")
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Unsupported image format" }
        var sampleSize = 1
        while (bounds.outWidth / sampleSize > 2200 || bounds.outHeight / sampleSize > 2200) {
            sampleSize *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val bitmap = resolver.openInputStream(imageUri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: error("Cannot decode this image")

        val document = PdfDocument()
        val folder = File(context.filesDir, "created").apply { mkdirs() }
        val output = File(folder, UUID.randomUUID().toString() + ".pdf")
        try {
            val portrait = bitmap.height >= bitmap.width
            val pageWidth = if (portrait) 595 else 842
            val pageHeight = if (portrait) 842 else 595
            val page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create())
            page.canvas.drawColor(Color.WHITE)
            val margin = 24f
            val scale = min((pageWidth - 2 * margin) / bitmap.width, (pageHeight - 2 * margin) / bitmap.height)
            val width = bitmap.width * scale
            val height = bitmap.height * scale
            val rect = RectF((pageWidth - width) / 2, (pageHeight - height) / 2,
                (pageWidth + width) / 2, (pageHeight + height) / 2)
            page.canvas.drawBitmap(bitmap, null, rect, Paint(Paint.FILTER_BITMAP_FLAG))
            document.finishPage(page)
            FileOutputStream(output).use { document.writeTo(it) }
        } catch (failure: Exception) {
            output.delete()
            throw failure
        } finally {
            bitmap.recycle()
            document.close()
        }
        val uri = FileProvider.getUriForFile(context, context.packageName + authoritySuffix, output)
        return LocalPdf(uri.toString(), "Photo document.pdf")
    }
}
