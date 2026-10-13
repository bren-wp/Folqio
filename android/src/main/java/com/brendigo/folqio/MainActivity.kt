package com.brendigo.folqio

import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.saveable.rememberSaveable
import kotlin.math.roundToInt

private val Midnight = Color(0xFF0B1321)
private val Slate = Color(0xFF1A2433)
private val Mint = Color(0xFF22D3A7)
private val White = Color(0xFFF7FAFC)
private val Muted = Color(0xFFA9B9C9)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val incomingUri = intent?.data?.takeIf { intent?.action == Intent.ACTION_VIEW }
        setContent { FolqioApp(incomingUri) }
    }
}

@Composable
private fun FolqioApp(initialUri: Uri?) {
    val context = LocalContext.current
    val library = remember { PdfLibrary(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var recent by remember { mutableStateOf(library.recent()) }
    var current by remember { mutableStateOf<LocalPdf?>(null) }
    var importError by remember { mutableStateOf<String?>(null) }
    var creating by remember { mutableStateOf(false) }
    var favoriteRevision by remember { mutableIntStateOf(0) }
    var pendingCapture by rememberSaveable { mutableStateOf<String?>(null) }

    fun open(uri: Uri) {
        if (uri.scheme != "content") {
            importError = "Use the system file picker to open this PDF."
            return
        }
        val displayName = runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor: Cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()?.take(160) ?: "Document.pdf"
        val file = LocalPdf(uri.toString(), displayName)
        library.add(file)
        recent = library.recent()
        current = file
        importError = null
    }

    fun createFromImages(uris: List<Uri>) {
        scope.launch {
            creating = true
            try {
                val pdf = withContext(Dispatchers.IO) {
                    PhotoPdfTools.createPdf(context.applicationContext, uris)
                }
                library.add(pdf)
                recent = library.recent()
                current = pdf
                importError = null
            } catch (failure: Exception) {
                importError = "Cannot create PDF: ${failure.localizedMessage ?: "invalid image"}"
            } finally {
                creating = false
            }
        }
    }

    LaunchedEffect(initialUri) { initialUri?.let { open(it) } }

    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) {
                // Some providers supply only temporary read access.
            }
            open(uri)
        }
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) createFromImages(uris)
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val uri = pendingCapture?.let(Uri::parse)
        pendingCapture = null
        if (uri != null) {
            if (saved) createFromImages(listOf(uri))
            else PhotoPdfTools.discardCapture(context, uri)
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Mint, onPrimary = Midnight, background = Midnight, onBackground = White,
        surface = Slate, onSurface = White, surfaceVariant = Slate
    )) {
        Surface(modifier = Modifier.fillMaxSize(), color = Midnight) {
            val file = current
            if (file == null) {
                FolqioHome(
                    files = recent,
                    isFavorite = { key -> favoriteRevision.let { library.isFavorite(key) } },
                    onFavorite = { key ->
                        library.toggleFavorite(key)
                        favoriteRevision++
                    },
                    onOpen = { pdfPicker.launch(arrayOf("application/pdf")) },
                    onCamera = {
                        try {
                            val uri = PhotoPdfTools.newCameraDestination(context)
                            pendingCapture = uri.toString()
                            camera.launch(uri)
                        } catch (failure: Exception) {
                            importError = failure.localizedMessage ?: "Cannot open camera."
                        }
                    },
                    onPhoto = { imagePicker.launch("image/*") },
                    onSelect = { current = it },
                    onRemove = { removed ->
                        library.remove(removed)
                        recent = library.recent()
                    },
                    onRename = { item, name ->
                        val changed = library.renameLabel(item, name)
                        if (changed) recent = library.recent()
                        changed
                    },
                    busy = creating,
                    error = importError
                )
            } else {
                PdfReader(
                    file = file, library = library, onBack = { current = null },
                    onRemove = {
                        library.remove(file)
                        recent = library.recent()
                        current = null
                    },
                    onCreated = { newFile ->
                        library.add(newFile)
                        recent = library.recent()
                        current = newFile
                    }
                )
            }
        }
    }
}

@Composable
private fun PdfReader(file: LocalPdf, library: PdfLibrary, onBack: () -> Unit, onRemove: () -> Unit, onCreated: (LocalPdf) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var exporting by remember(file.uri) { mutableStateOf(false) }
    var exportError by remember(file.uri) { mutableStateOf<String?>(null) }

    var page by remember(file.uri) { mutableIntStateOf(0) }

    fun exportPage(rotate: Boolean) {
        scope.launch {
            exporting = true
            try {
                val created = withContext(Dispatchers.IO) {
                    AndroidPageTools.copyPage(context.applicationContext, Uri.parse(file.uri), page, rotate)
                }
                onCreated(created)
            } catch (failure: Exception) {
                exportError = failure.localizedMessage ?: "Cannot export this page."
            } finally {
                exporting = false
            }
        }
    }

    var count by remember(file.uri) { mutableIntStateOf(0) }
    var image by remember(file.uri) { mutableStateOf<android.graphics.Bitmap?>(null) }
    var error by remember(file.uri) { mutableStateOf<String?>(null) }
    var zoom by remember(file.uri) { mutableFloatStateOf(1f) }
    var bookmarked by remember(file.uri, page) { mutableStateOf(library.isBookmarked(file.uri, page)) }

    LaunchedEffect(file.uri, page) {
        image = null
        error = null
        zoom = 1f
        try {
            val rendered = PdfPages.render(context.applicationContext, Uri.parse(file.uri), page)
            image = rendered.bitmap
            count = rendered.count
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            error = "Cannot read this PDF: ${failure.localizedMessage ?: "unsupported file"}"
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = White) }
            Column(Modifier.weight(1f)) {
                Text(file.name, color = White, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                Text(if (count > 0) "${page + 1} / $count" else "Opening…", color = Muted)
            }
            IconButton(onClick = { bookmarked = library.toggleBookmark(file.uri, page) }) {
                Icon(if (bookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, "Bookmark page", tint = Mint)
            }
            IconButton(onClick = {
                val share = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, Uri.parse(file.uri))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(share, "Share PDF"))
            }) { Icon(Icons.Default.IosShare, "Share PDF", tint = White) }
        }
        Box(Modifier.fillMaxWidth().weight(1f).background(Color(0xFF101C2B)), contentAlignment = Alignment.Center) {
            when {
                error != null -> Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Text(error!!, color = White)
                    Spacer(Modifier.height(15.dp))
                    OutlinedButton(onClick = onRemove) { Text("Remove from recent files") }
                }
                image == null -> CircularProgressIndicator(color = Mint)
                else -> Image(
                    bitmap = image!!.asImageBitmap(), contentDescription = "PDF page ${page + 1}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(8.dp)
                        .graphicsLayer(scaleX = zoom, scaleY = zoom)
                        .pointerInput(file.uri, page) {
                            detectTransformGestures { _, _, scale, _ -> zoom = (zoom * scale).coerceIn(1f, 4f) }
                        }
                )
            }
        }
        if (exporting) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Mint)
        if (exportError != null) Text(exportError!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            OutlinedButton(onClick = { exportPage(false) }, enabled = image != null && !exporting,
                modifier = Modifier.weight(1f)) { Text("Extract copy") }
            OutlinedButton(onClick = { exportPage(true) }, enabled = image != null && !exporting,
                modifier = Modifier.weight(1f)) { Text("Rotate copy") }
        }
        Text("Copies are flattened images, not editable PDF text.",
            style = MaterialTheme.typography.labelSmall, color = Muted,
            modifier = Modifier.padding(start = 14.dp, bottom = 3.dp))
        if (count > 1) {
            Slider(
                value = page.toFloat(), onValueChange = { page = it.roundToInt() },
                valueRange = 0f..(count - 1).toFloat(),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { page -= 1 }, enabled = page > 0) {
                Icon(Icons.Default.ChevronLeft, "Previous page", tint = if (page > 0) Mint else Muted)
            }
            Text(if (count > 0) "Page ${page + 1} of $count" else "Folqio", color = White)
            IconButton(onClick = { page += 1 }, enabled = count > 0 && page < count - 1) {
                Icon(Icons.Default.ChevronRight, "Next page", tint = if (count > 0 && page < count - 1) Mint else Muted)
            }
        }
    }
}
