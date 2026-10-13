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
    var recent by remember { mutableStateOf(library.recent()) }
    var current by remember { mutableStateOf<LocalPdf?>(null) }
    var query by remember { mutableStateOf("") }
    var importError by remember { mutableStateOf<String?>(null) }

    fun open(uri: Uri) {
        if (uri.scheme != "content") {
            importError = "Only documents shared through your device are supported."
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

    LaunchedEffect(initialUri) { initialUri?.let { open(it) } }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) {
                // Some document providers only grant temporary access.
            }
            open(uri)
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Mint, onPrimary = Midnight, background = Midnight, onBackground = White,
        surface = Slate, onSurface = White, surfaceVariant = Slate
    )) {
        Surface(modifier = Modifier.fillMaxSize(), color = Midnight) {
            if (current == null) {
                Column(Modifier.fillMaxSize().padding(horizontal = 22.dp)) {
                    Spacer(Modifier.height(24.dp))
                    Text("FOLQIO", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, color = White)
                    Text("Your documents. Your device.", color = Muted)
                    Spacer(Modifier.height(28.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = Slate), shape = RoundedCornerShape(22.dp)) {
                        Column(Modifier.fillMaxWidth().padding(20.dp)) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Mint, modifier = Modifier.size(36.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("Your documents, in your hands.", style = MaterialTheme.typography.titleLarge, color = White, fontWeight = FontWeight.Bold)
                            Text("Open and read PDFs locally. No account or cloud required.", color = Muted)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { picker.launch(arrayOf("application/pdf")) }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null)
                                Spacer(Modifier.width(9.dp))
                                Text("Open PDF")
                            }
                        }
                    }
                    Spacer(Modifier.height(25.dp))
                    Text("Recent files", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = White)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = query, onValueChange = { query = it },
                        label = { Text("Search file names") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Search, null) }
                    )
                    Spacer(Modifier.height(10.dp))
                    val filtered = recent.filter { it.name.contains(query, ignoreCase = true) }
                    if (filtered.isEmpty()) {
                        Text(if (recent.isEmpty()) "Open a PDF to see it here." else "No matching documents.", color = Muted)
                    } else {
                        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(filtered, key = { it.uri }) { file ->
                                Card(onClick = { current = file }, colors = CardDefaults.cardColors(containerColor = Slate), shape = RoundedCornerShape(14.dp)) {
                                    Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Description, null, tint = Mint)
                                        Spacer(Modifier.width(12.dp))
                                        Text(file.name, color = White, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                        IconButton(onClick = { library.remove(file); recent = library.recent() }) {
                                            Icon(Icons.Default.Close, contentDescription = "Remove from recents", tint = Muted)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (importError != null) Text(importError!!, color = MaterialTheme.colorScheme.error)
                }
            } else {
                PdfReader(current!!, library, onBack = { current = null }, onRemove = {
                    library.remove(current!!)
                    recent = library.recent()
                    current = null
                })
            }
        }
    }
}

@Composable
private fun PdfReader(file: LocalPdf, library: PdfLibrary, onBack: () -> Unit, onRemove: () -> Unit) {
    val context = LocalContext.current
    var page by remember(file.uri) { mutableIntStateOf(0) }
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
