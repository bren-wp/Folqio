package com.brendigo.folqio

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

/**
 * Copy a user-selected PDF into private app storage so documents remain available
 * after picker permissions expire. The original provider document is never modified.
 */
internal object PdfImportTools {
    fun import(context: Context, uri: Uri): LocalPdf {
        require(uri.scheme == "content") { "Choose a PDF using the document picker." }
        val resolver = context.contentResolver
        val providerName = runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        }.getOrNull()

        val folder = File(context.filesDir, "created")
        if (!folder.isDirectory && !folder.mkdirs()) {
            throw IOException("Cannot prepare document storage.")
        }
        val identifier = UUID.randomUUID().toString()
        val temporary = File(folder, "$identifier.partial")
        val stored = File(folder, "$identifier.pdf")
        var committed = false
        try {
            var count = 0L
            val input = resolver.openInputStream(uri) ?: throw IOException("Cannot open the selected document.")
            input.use { source ->
                FileOutputStream(temporary).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val length = source.read(buffer)
                        if (length < 0) break
                        count += length
                        if (count > PdfImportPolicy.MAX_BYTES) {
                            throw IOException("Choose a PDF smaller than 150 MB.")
                        }
                        output.write(buffer, 0, length)
                    }
                    output.fd.sync()
                }
            }
            if (count == 0L) throw IOException("The selected PDF is empty.")
            val header = ByteArray(minOf(count, 1024L).toInt())
            FileInputStream(temporary).use { source ->
                if (source.read(header) < 5 || !PdfImportPolicy.hasPdfSignature(header)) {
                    throw IOException("The selected file is not a valid PDF.")
                }
            }
            try {
                ParcelFileDescriptor.open(temporary, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                    PdfRenderer(fd).use { reader ->
                        if (reader.pageCount <= 0) throw IOException("The PDF contains no readable pages.")
                    }
                }
            } catch (e: SecurityException) {
                throw IOException("This PDF is protected or cannot be opened.", e)
            } catch (e: IllegalArgumentException) {
                throw IOException("This PDF could not be read.", e)
            }

            if (!temporary.renameTo(stored)) throw IOException("Cannot save the PDF.")
            val ownedUri = FileProvider.getUriForFile(context, "${context.packageName}.files", stored)
            committed = true
            return LocalPdf(ownedUri.toString(), PdfImportPolicy.documentName(providerName))
        } finally {
            temporary.delete()
            if (!committed) stored.delete()
        }
    }
}
