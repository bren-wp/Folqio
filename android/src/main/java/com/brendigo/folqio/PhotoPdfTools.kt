package com.brendigo.folqio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.min

/**
 * Converts user-selected photos to a real multipage PDF entirely on-device.
 * No external document service, downloaded library, or extra storage permission.
 */
object PhotoPdfTools {
    private const val authoritySuffix = ".files"

    fun newCameraDestination(context: Context): Uri {
        val folder = File(context.filesDir, "captures").apply { mkdirs() }
        val image = File(folder, UUID.randomUUID().toString() + ".jpg")
        return FileProvider.getUriForFile(context, context.packageName + authoritySuffix, image)
    }

    fun discardCapture(context: Context, uri: Uri) {
        if (uri.authority != context.packageName + authoritySuffix) return
        if (uri.pathSegments.size != 2 || uri.pathSegments[0] != "camera_captures") return
        val filename = uri.pathSegments[1]
        if (!Regex("[a-fA-F0-9-]{36}[.]jpg").matches(filename)) return
        File(File(context.filesDir, "captures"), filename).delete()
    }

    fun createPdf(context: Context, imageUri: Uri): LocalPdf =
        createPdf(context, listOf(imageUri))

    fun createPdf(context: Context, imageUris: List<Uri>): LocalPdf {
        try {
            require(PhotoSelectionPolicy.isValidCount(imageUris.size)) {
                "Select between 1 and ${PhotoSelectionPolicy.MAX_PHOTOS} photos"
            }
            require(imageUris.all { it.scheme == "content" }) { "Only user-selected images are supported" }
            val document = PdfDocument()
            val outputFolder = File(context.filesDir, "created").apply { mkdirs() }
            val output = File(outputFolder, UUID.randomUUID().toString() + ".pdf")
            try {
                imageUris.forEachIndexed { index, uri ->
                    val raw = decodeBounded(context, uri)
                    try {
                        val corrected = orientBitmap(context, uri, raw)
                        try {
                            val portrait = corrected.height >= corrected.width
                            val width = if (portrait) 595 else 842
                            val height = if (portrait) 842 else 595
                            val info = PdfDocument.PageInfo.Builder(width, height, index + 1).create()
                            val page = document.startPage(info)
                            page.canvas.drawColor(Color.WHITE)
                            val margin = 24f
                            val scale = min(
                                (width - margin * 2) / corrected.width,
                                (height - margin * 2) / corrected.height
                            )
                            val w = corrected.width * scale
                            val h = corrected.height * scale
                            val target = RectF((width - w) / 2, (height - h) / 2,
                                (width + w) / 2, (height + h) / 2)
                            page.canvas.drawBitmap(corrected, null, target, Paint(Paint.FILTER_BITMAP_FLAG))
                            document.finishPage(page)
                        } finally {
                            if (corrected !== raw) corrected.recycle()
                        }
                    } finally {
                        raw.recycle()
                    }
                }
                FileOutputStream(output).use { document.writeTo(it) }
            } catch (failure: Exception) {
                output.delete()
                throw failure
            } finally {
                document.close()
            }
            val uri = FileProvider.getUriForFile(context, context.packageName + authoritySuffix, output)
            val name = if (imageUris.size == 1) "Photo document.pdf" else "Photo collection.pdf"
            return LocalPdf(uri.toString(), name)
        } finally {
            imageUris.forEach { discardCapture(context, it) }
        }
    }

    private fun decodeBounded(context: Context, uri: Uri): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        } ?: error("Cannot access photo")
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Unreadable photo" }
        var sample = 1
        while (bounds.outWidth / sample > 1600 || bounds.outHeight / sample > 2200) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: error("Cannot decode photo")
    }

    private fun orientBitmap(context: Context, uri: Uri, image: Bitmap): Bitmap {
        val rotation = runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                when (ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
                )) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        }.getOrDefault(0f)
        if (rotation == 0f) return image
        val matrix = Matrix().apply { postRotate(rotation) }
        return Bitmap.createBitmap(image, 0, 0, image.width, image.height, matrix, true)
    }
}
