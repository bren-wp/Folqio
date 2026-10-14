package com.brendigo.folqio

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import kotlin.math.roundToInt


class MainActivity : ComponentActivity() {
    private val incomingDocument = mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        incomingDocument.value = intent?.data?.takeIf { intent?.action == Intent.ACTION_VIEW }
        setContent { FolqioApp(incomingDocument.value) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingDocument.value = intent.data?.takeIf { intent.action == Intent.ACTION_VIEW }
    }
}

@Composable
private fun FolqioApp(initialUri: Uri?) {
    val context = LocalContext.current
    val library = remember { PdfLibrary(context.applicationContext) }
    val preferences = remember { FolqioPreferences(context.applicationContext) }
    var language by remember { mutableStateOf(preferences.language) }
    var reopenLastPage by remember { mutableStateOf(preferences.reopenLastDocument) }
    var confirmRemoval by remember { mutableStateOf(preferences.confirmRemoval) }
    var zipSelection by remember { mutableStateOf<List<LocalPdf>>(emptyList()) }
    var exportingZip by remember { mutableStateOf(false) }
    var zipResult by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var recent by remember { mutableStateOf(library.recent()) }
    var current by remember { mutableStateOf<LocalPdf?>(null) }
    var importError by remember { mutableStateOf<String?>(null) }
    var creating by remember { mutableStateOf(false) }
    var importing by remember { mutableStateOf(false) }
    var merging by remember { mutableStateOf(false) }
    var favoriteRevision by remember { mutableIntStateOf(0) }
    var pendingCapture by rememberSaveable { mutableStateOf<String?>(null) }

    fun open(uri: Uri) {
        if (uri.scheme != "content") {
            importError = "Choose a PDF using the document picker."
            return
        }
        scope.launch {
            importing = true
            importError = null
            try {
                val file = withContext(Dispatchers.IO) {
                    PdfImportTools.import(context.applicationContext, uri)
                }
                library.add(file)
                recent = library.recent()
                current = file
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                importError = "Couldn't open this PDF: ${failure.localizedMessage ?: "select another document"}"
            } finally {
                importing = false
            }
        }
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
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                importError = "Cannot create PDF: ${failure.localizedMessage ?: "invalid image"}"
            } finally {
                creating = false
            }
        }
    }

    LaunchedEffect(initialUri) { initialUri?.let { open(it) } }

    val zipPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { destination ->
        if (destination != null && zipSelection.isNotEmpty()) {
            val selected = zipSelection
            zipSelection = emptyList()
            scope.launch {
                exportingZip = true
                zipResult = null
                try {
                    val exported = withContext(Dispatchers.IO) {
                        PdfZipExport.export(context.applicationContext, selected, destination)
                    }
                    zipResult = "$exported PDF documents added to your ZIP archive."
                    importError = null
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    importError = "ZIP export failed: ${failure.localizedMessage ?: "try a different folder"}"
                } finally {
                    exportingZip = false
                }
            }
        } else {
            zipSelection = emptyList()
        }
    }

    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            open(uri)
        }
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) createFromImages(uris)
    }
    val mergePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { sources ->
        if (sources.isNotEmpty()) {
            if (!MergePolicy.validFiles(sources.size)) {
                importError = "Select two to five PDF files."
            } else {
                scope.launch {
                    merging = true
                    importError = null
                    try {
                        val merged = withContext(Dispatchers.IO) {
                            AndroidMergeTools.merge(context.applicationContext, sources)
                        }
                        library.add(merged)
                        recent = library.recent()
                        current = merged
                    } catch (cancelled: kotlinx.coroutines.CancellationException) {
                        throw cancelled
                    } catch (failure: Exception) {
                        importError = "Merge failed: ${failure.localizedMessage ?: "unsupported document"}"
                    } finally {
                        merging = false
                    }
                }
            }
        }
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
        primary = FolqioPalette.mint, onPrimary = FolqioPalette.midnight, background = FolqioPalette.midnight, onBackground = FolqioPalette.pearl,
        surface = FolqioPalette.slate, onSurface = FolqioPalette.pearl, surfaceVariant = FolqioPalette.slate
    )) {
        Surface(modifier = Modifier.fillMaxSize(), color = FolqioPalette.midnight) {
            val file = current
            if (file == null) {
                FolqioHome(
                    files = recent,
                    language = language,
                    onLanguageChange = { code ->
                        preferences.language = code
                        language = code
                    },
                    reopenLastPage = reopenLastPage,
                    onReopenChange = { value ->
                        preferences.reopenLastDocument = value
                        reopenLastPage = value
                    },
                    confirmRemoval = confirmRemoval,
                    onConfirmChange = { value ->
                        preferences.confirmRemoval = value
                        confirmRemoval = value
                    },
                    onZip = { selected ->
                        zipSelection = selected
                        zipPicker.launch("Folqio-documents.zip")
                    },
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
                            pendingCapture?.let { stored ->
                                PhotoPdfTools.discardCapture(context, Uri.parse(stored))
                            }
                            pendingCapture = null
                            importError = failure.localizedMessage ?: "Cannot open camera."
                        }
                    },
                    onPhoto = { imagePicker.launch("image/*") },
                    onMerge = { mergePicker.launch(arrayOf("application/pdf")) },
                    onSelect = { current = it },
                    onRemove = { removed ->
                        if (library.remove(removed)) {
                            recent = library.recent()
                        } else {
                            importError = "Could not delete this local file. Try again."
                        }
                    },
                    onRename = { item, name ->
                        val changed = library.renameLabel(item, name)
                        if (changed) recent = library.recent()
                        changed
                    },
                    busy = creating || merging || importing || exportingZip,
                    error = importError,
                    notice = zipResult
                )
            } else {
                PdfReader(
                    file = file, library = library, reopenLastPage = reopenLastPage, confirmRemoval = confirmRemoval, onBack = { current = null },
                    onRemove = {
                        if (library.remove(file)) {
                            recent = library.recent()
                            current = null
                        }
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
private fun PdfReader(file: LocalPdf, library: PdfLibrary, reopenLastPage: Boolean, confirmRemoval: Boolean, onBack: () -> Unit, onRemove: () -> Unit, onCreated: (LocalPdf) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var savingCopy by remember(file.uri) { mutableStateOf(false) }
    var saveInfo by remember(file.uri) { mutableStateOf<String?>(null) }
    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { destination ->
        if (destination != null) {
            scope.launch {
                savingCopy = true
                try {
                    withContext(Dispatchers.IO) {
                        val resolver = context.contentResolver
                        val source = resolver.openInputStream(Uri.parse(file.uri))
                            ?: error("Source PDF cannot be opened")
                        source.use { input ->
                            val output = resolver.openOutputStream(destination, "w")
                                ?: error("Destination cannot be opened")
                            output.use { input.copyTo(it) }
                        }
                    }
                    saveInfo = "PDF copy saved to your selected location."
                } catch (failure: Exception) {
                    saveInfo = "Save failed: ${failure.localizedMessage ?: "unknown error"}"
                } finally {
                    savingCopy = false
                }
            }
        }
    }
    var confirmRemove by remember(file.uri) { mutableStateOf(false) }
    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text("Remove document?") },
            text = { Text("A PDF created by Folqio will be deleted. Imported source files remain untouched.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmRemove = false
                    onRemove()
                }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemove = false }) { Text("Cancel") }
            }
        )
    }
    var exporting by remember(file.uri) { mutableStateOf(false) }
    var exportError by remember(file.uri) { mutableStateOf<String?>(null) }

    var page by remember(file.uri) { mutableIntStateOf(if (reopenLastPage) library.lastPage(file.uri) else 0) }

    fun exportPage(rotate: Boolean) {
        scope.launch {
            exporting = true
            try {
                val created = withContext(Dispatchers.IO) {
                    AndroidPageTools.copyPage(context.applicationContext, Uri.parse(file.uri), page, rotate)
                }
                onCreated(created)
            } catch (cancelled: CancellationException) {
                throw cancelled
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

    DisposableEffect(image) {
        val displayed = image
        onDispose { displayed?.recycle() }
    }

    LaunchedEffect(file.uri, page) {
        library.recordPage(file.uri, page)
        image = null
        error = null
        zoom = 1f
        try {
            val rendered = PdfPages.render(context.applicationContext, Uri.parse(file.uri), page)
            count = rendered.count
            val safePage = PdfPageBounds.clamp(page, rendered.count)
            if (safePage != page) {
                rendered.bitmap.recycle()
                page = safePage
            } else {
                image = rendered.bitmap
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            error = "Cannot read this PDF: ${failure.localizedMessage ?: "unsupported file"}"
        }
    }

    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = FolqioPalette.pearl) }
            Column(Modifier.weight(1f)) {
                Text(file.name, color = FolqioPalette.pearl, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                Text(if (count > 0) "${page + 1} / $count" else "Opening…", color = FolqioPalette.muted)
            }
            IconButton(onClick = { bookmarked = library.toggleBookmark(file.uri, page) }) {
                Icon(if (bookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, "Bookmark page", tint = FolqioPalette.mint)
            }
            IconButton(
                onClick = { saveLauncher.launch(file.name) },
                enabled = !savingCopy
            ) {
                Icon(Icons.Default.SaveAlt, "Save PDF copy", tint = FolqioPalette.mint)
            }
            IconButton(onClick = {
                val share = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, Uri.parse(file.uri))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                try {
                    context.startActivity(Intent.createChooser(share, "Share PDF"))
                } catch (failure: Exception) {
                    saveInfo = "Sharing is unavailable on this device."
                }
            }) { Icon(Icons.Default.IosShare, "Share PDF", tint = FolqioPalette.pearl) }
        }
        Box(Modifier.fillMaxWidth().weight(1f).background(Color(0xFF101C2B)), contentAlignment = Alignment.Center) {
            when {
                error != null -> Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Text(error!!, color = FolqioPalette.pearl)
                    Spacer(Modifier.height(15.dp))
                    OutlinedButton(onClick = { if (confirmRemoval) confirmRemove = true else onRemove() }) { Text("Remove document") }
                }
                image == null -> CircularProgressIndicator(color = FolqioPalette.mint)
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
        if (savingCopy || exporting) LinearProgressIndicator(Modifier.fillMaxWidth(), color = FolqioPalette.mint)
        if (saveInfo != null) {
            Text(saveInfo!!, color = FolqioPalette.muted, modifier = Modifier.padding(horizontal = 14.dp))
        }
        if (exportError != null) Text(exportError!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            OutlinedButton(onClick = { exportPage(false) }, enabled = image != null && !exporting,
                modifier = Modifier.weight(1f)) { Text("Extract copy") }
            OutlinedButton(onClick = { exportPage(true) }, enabled = image != null && !exporting,
                modifier = Modifier.weight(1f)) { Text("Rotate copy") }
        }
        Text("Copies are flattened images, not editable PDF text.",
            style = MaterialTheme.typography.labelSmall, color = FolqioPalette.muted,
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
                Icon(Icons.Default.ChevronLeft, "Previous page", tint = if (page > 0) FolqioPalette.mint else FolqioPalette.muted)
            }
            Text(if (count > 0) "Page ${page + 1} of $count" else "Folqio", color = FolqioPalette.pearl)
            IconButton(onClick = { page += 1 }, enabled = count > 0 && page < count - 1) {
                Icon(Icons.Default.ChevronRight, "Next page", tint = if (count > 0 && page < count - 1) FolqioPalette.mint else FolqioPalette.muted)
            }
        }
    }
}
