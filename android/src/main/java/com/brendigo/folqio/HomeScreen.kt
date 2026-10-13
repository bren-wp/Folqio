package com.brendigo.folqio

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private val midnight = Color(0xFF0B1321)
private val slate = Color(0xFF1A2433)
private val mint = Color(0xFF22D3A7)
private val pearl = Color(0xFFF7FAFC)
private val muted = Color(0xFFA9B9C9)

private enum class FolqioTab { HOME, FILES, FAVORITES, SETTINGS }

@Composable
internal fun FolqioHome(
    files: List<LocalPdf>,
    isFavorite: (String) -> Boolean,
    onFavorite: (String) -> Unit,
    onOpen: () -> Unit,
    onCamera: () -> Unit,
    onPhoto: () -> Unit,
    onMerge: () -> Unit,
    onSelect: (LocalPdf) -> Unit,
    onRemove: (LocalPdf) -> Unit,
    onRename: (LocalPdf, String) -> Boolean,
    busy: Boolean,
    error: String?
) {
    var tab by remember { mutableStateOf(FolqioTab.HOME) }
    var query by remember { mutableStateOf("") }
    var renaming by remember { mutableStateOf<LocalPdf?>(null) }
    var renameText by remember { mutableStateOf("") }
    var invalidRename by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf<LocalPdf?>(null) }
    val filtered = files.filter {
        (tab != FolqioTab.FAVORITES || isFavorite(it.uri)) &&
            it.name.contains(query, ignoreCase = true)
    }
    if (renaming != null) {
        AlertDialog(
            onDismissRequest = { renaming = null; invalidRename = false },
            title = { Text("Rename document label") },
            text = {
                Column {
                    OutlinedTextField(
                        value = renameText, onValueChange = { renameText = it },
                        label = { Text("Display name") }, singleLine = true
                    )
                    if (invalidRename) Text("Use a valid PDF name.", color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val target = renaming
                    if (target != null && onRename(target, renameText)) {
                        renaming = null
                        invalidRename = false
                    } else invalidRename = true
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text("Cancel") } }
        )
    }
    if (removing != null) {
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text("Remove document?") },
            text = {
                Text(
                    "Files created in Folqio are permanently deleted. " +
                    "Imported PDFs remain at their original location."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    removing?.let(onRemove)
                    removing = null
                }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { removing = null }) { Text("Cancel") }
            }
        )
    }
    Scaffold(
        containerColor = midnight,
        bottomBar = {
            NavigationBar(containerColor = slate) {
                listOf(
                    Triple(FolqioTab.HOME, Icons.Default.Home, "Home"),
                    Triple(FolqioTab.FILES, Icons.Default.Folder, "Files"),
                    Triple(FolqioTab.FAVORITES, Icons.Default.Star, "Favorites"),
                    Triple(FolqioTab.SETTINGS, Icons.Default.Settings, "Settings")
                ).forEach { (item, icon, title) ->
                    NavigationBarItem(
                        selected = tab == item, onClick = { tab = item },
                        icon = { Icon(icon, contentDescription = title) },
                        label = { Text(title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = midnight, selectedTextColor = mint,
                            indicatorColor = mint, unselectedIconColor = muted, unselectedTextColor = muted
                        )
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp),
            contentPadding = PaddingValues(top = 23.dp, bottom = 24.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.folqio_mark), contentDescription = null,
                        modifier = Modifier.size(54.dp))
                    Spacer(Modifier.width(11.dp))
                    Column {
                        Text("FOLQIO", style = MaterialTheme.typography.headlineMedium,
                            color = pearl, fontWeight = FontWeight.Black)
                        Text("Your documents. Your device.", color = muted,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (tab == FolqioTab.SETTINGS) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = slate),
                        shape = RoundedCornerShape(20.dp)) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                            Text("Privacy first", color = pearl, style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold)
                            Text("Documents are processed locally. No Folqio account, analytics or cloud upload.",
                                color = muted)
                            Text("PDFs are opened through the system file picker. Camera and image access are initiated only by you.",
                                color = muted)
                            Text("Your documents. Your device.", color = mint)
                        }
                    }
                }
            } else {
                if (tab == FolqioTab.HOME) {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = slate),
                            shape = RoundedCornerShape(20.dp)) {
                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Your documents, in your hands.",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold, color = pearl)
                                Text("Read PDFs and create new documents locally.", color = muted)
                                Button(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Open PDF")
                                }
                            }
                        }
                    }
                    item {
                        Text("Quick actions", color = pearl,
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            QuickAction("Open", Icons.Default.FolderOpen, Modifier.weight(1f), onOpen)
                            QuickAction("Scan", Icons.Default.CameraAlt, Modifier.weight(1f), onCamera)
                            QuickAction("Photos", Icons.Default.Photo, Modifier.weight(1f), onPhoto)
                        }
                        Spacer(Modifier.height(9.dp))
                        Card(
                            onClick = onMerge,
                            colors = CardDefaults.cardColors(containerColor = slate),
                            shape = RoundedCornerShape(15.dp)
                        ) {
                            Row(Modifier.fillMaxWidth().padding(15.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CallMerge, contentDescription = null, tint = mint)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text("Merge PDF files", color = pearl, fontWeight = FontWeight.SemiBold)
                                    Text("2–5 files · flattened output · max 50 pages",
                                        color = muted, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
                item {
                    Text(if (tab == FolqioTab.FAVORITES) "Favorites" else "Recent files",
                        color = pearl, style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = query, onValueChange = { query = it },
                        singleLine = true, label = { Text("Search file names") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = mint) }
                if (error != null) item { Text(error, color = MaterialTheme.colorScheme.error) }
                if (filtered.isEmpty()) {
                    item {
                        Text(
                            if (files.isEmpty()) "Open or scan a document to see it here."
                            else if (tab == FolqioTab.FAVORITES) "No favorites yet."
                            else "No matching documents.",
                            color = muted
                        )
                    }
                }
                items(filtered, key = { it.uri }) { file ->
                    Card(onClick = { onSelect(file) },
                        colors = CardDefaults.cardColors(containerColor = slate),
                        shape = RoundedCornerShape(15.dp)) {
                        Row(Modifier.fillMaxWidth().padding(start = 14.dp, top = 8.dp,
                            end = 5.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            PdfThumbnail(file.uri)
                            Spacer(Modifier.width(12.dp))
                            Text(file.name, color = pearl, maxLines = 2,
                                overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            var menuOpen by remember(file.uri) { mutableStateOf(false) }
                            Box {
                                IconButton(onClick = { menuOpen = true }) {
                                    Icon(Icons.Default.MoreVert, "Document options", tint = muted)
                                }
                                DropdownMenu(
                                    expanded = menuOpen,
                                    onDismissRequest = { menuOpen = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(if (isFavorite(file.uri)) "Remove favorite" else "Add favorite") },
                                        onClick = { menuOpen = false; onFavorite(file.uri) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Rename") },
                                        onClick = {
                                            menuOpen = false
                                            renaming = file
                                            renameText = file.name
                                            invalidRename = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Remove") },
                                        onClick = { menuOpen = false; removing = file }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
                        modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = slate),
        shape = RoundedCornerShape(15.dp)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 15.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Icon(icon, contentDescription = null, tint = mint, modifier = Modifier.size(25.dp))
            Text(label, color = pearl, style = MaterialTheme.typography.labelLarge)
        }
    }
}
