package com.brendigo.folqio

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Article
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp


@Composable
internal fun FolqioSettingsScreen(
    language: String,
    onLanguageChange: (String) -> Unit,
    reopenLastPage: Boolean,
    onReopenChange: (Boolean) -> Unit,
    confirmRemoval: Boolean,
    onConfirmChange: (Boolean) -> Unit,
    files: List<LocalPdf>,
    onExport: (List<LocalPdf>) -> Unit
) {
    var languageMenu by remember { mutableStateOf(false) }
    var selecting by remember { mutableStateOf(false) }
    var selection by remember { mutableStateOf<Set<String>>(emptySet()) }
    var legalPage by remember { mutableStateOf<String?>(null) }
    fun tr(key: String) = FolqioLanguages.text(language, key)
    val context = LocalContext.current
    val version = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "—"
    }

    if (selecting) {
        AlertDialog(
            onDismissRequest = { selecting = false },
            title = { Text(tr("choose")) },
            text = {
                Column(Modifier.heightIn(max = 330.dp).verticalScroll(rememberScrollState())) {
                    files.take(PdfZipExport.MAX_FILES).forEach { file ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = file.uri in selection,
                                onCheckedChange = { chosen ->
                                    selection = if (chosen) selection + file.uri else selection - file.uri
                                }
                            )
                            Text(file.name, modifier = Modifier.weight(1f))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = selection.isNotEmpty(),
                    onClick = {
                        selecting = false
                        onExport(files.filter { it.uri in selection })
                    }
                ) { Text(tr("export")) }
            },
            dismissButton = {
                TextButton(onClick = { selecting = false }) { Text(tr("cancel")) }
            }
        )
    }
    legalPage?.let { kind ->
        AlertDialog(
            onDismissRequest = { legalPage = null },
            title = { Text(tr(kind)) },
            text = {
                Box(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState())) {
                    Text(FolqioLegalCopy.text(kind, language))
                }
            },
            confirmButton = { TextButton(onClick = { legalPage = null }) { Text("OK") } }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = FolqioPalette.slate), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(19.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Text("Folqio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = FolqioPalette.pearl)
                Text("Your documents. Your device.", color = FolqioPalette.muted)
                HorizontalDivider(color = FolqioPalette.muted.copy(alpha = .2f))
                Text(tr("language"), color = FolqioPalette.pearl, style = MaterialTheme.typography.titleMedium)
                Box {
                    OutlinedButton(onClick = { languageMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Language, null)
                        Spacer(Modifier.width(10.dp))
                        Text(FolqioLanguages.supported[language] ?: "English")
                    }
                    // DropdownMenu provides its own bounded scrolling. Nested
                    // vertical scroll containers can steal touch events on phones.
                    DropdownMenu(expanded = languageMenu, onDismissRequest = { languageMenu = false }) {
                        FolqioLanguages.supported.forEach { (code, name) ->
                            DropdownMenuItem(text = { Text(name) }, onClick = {
                                onLanguageChange(code)
                                languageMenu = false
                            })
                        }
                    }
                }
                Text("Choose the language that feels right for you.",
                    style = MaterialTheme.typography.bodySmall, color = FolqioPalette.muted)
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = FolqioPalette.slate), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(19.dp)) {
                Text("Reading & documents", color = FolqioPalette.pearl, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("reopen"), color = FolqioPalette.pearl, modifier = Modifier.weight(1f))
                    Switch(checked = reopenLastPage, onCheckedChange = onReopenChange)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("confirm"), color = FolqioPalette.pearl, modifier = Modifier.weight(1f))
                    Switch(checked = confirmRemoval, onCheckedChange = onConfirmChange)
                }
                Button(onClick = {
                    selection = files.take(PdfZipExport.MAX_FILES).map { it.uri }.toSet()
                    selecting = true
                }, enabled = files.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Archive, null)
                    Spacer(Modifier.width(8.dp))
                    Text(tr("exportZip"))
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = FolqioPalette.slate), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(19.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Privacy & information", color = FolqioPalette.pearl,
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Documents stay under your control. No Folqio account or advertising.",
                    color = FolqioPalette.muted)
                OutlinedButton(onClick = { legalPage = "privacy" }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.PrivacyTip, null)
                    Spacer(Modifier.width(10.dp))
                    Text(tr("privacy"))
                }
                OutlinedButton(onClick = { legalPage = "terms" }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Article, null)
                    Spacer(Modifier.width(10.dp))
                    Text(tr("terms"))
                }
                HorizontalDivider(color = FolqioPalette.muted.copy(alpha = .2f))
                Text("${tr("author")} Brendigo", color = FolqioPalette.pearl)
                Text("Folqio · v$version · © 2026 Brendigo", color = FolqioPalette.muted,
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

object FolqioLegalCopy {
    fun text(kind: String, language: String): String {
        val hr = language == "hr"
        if (kind == "privacy") return if (hr) """
            POLITIKA PRIVATNOSTI — FOLQIO
            Autor: Brendigo
            Ažurirano: 22. listopada 2026.

            Folqio obrađuje PDF dokumente na uređaju. Ne zahtijeva korisnički račun, ne sadrži oglase i ne šalje dokumente na Folqio poslužitelje.

            Aplikacija sprema odabrane postavke i nedavne dokumente u privatnu pohranu uređaja. Izvoz u ZIP ili dijeljenje pokrećete sami. Vanjske usluge dijeljenja imaju vlastita pravila privatnosti.

            Više o projektu: https://brendigo.com
        """.trimIndent() else """
            PRIVACY POLICY — FOLQIO
            Created by Brendigo · Updated October 22, 2026.

            Folqio processes PDF documents on your device. No account or advertising system is required, and documents are not sent to a Folqio server.

            Preferences and recently opened document references are stored within the app. ZIP exports and sharing occur only when you choose them. Third-party sharing destinations have their own privacy practices.

            More information: https://brendigo.com
        """.trimIndent()

        return if (hr) """
            UVJETI KORIŠTENJA — FOLQIO
            Autor: Brendigo · Ažurirano: 22. listopada 2026.

            Folqio je namijenjen pregledu i obradi dokumenata koje imate pravo koristiti. Prije dijeljenja važnih datoteka provjerite rezultate i sačuvajte izvornike.

            Neki Android alati izvoze rasterske PDF kopije i ne čuvaju uređivi tekst, obrasce ili digitalne potpise. Folqio nije certificirani alat za nepovratno uklanjanje osjetljivih podataka.

            Projekt: https://brendigo.com
        """.trimIndent() else """
            TERMS OF USE — FOLQIO
            Created by Brendigo · Updated October 22, 2026.

            Use Folqio only with documents you have the right to access and process. Review results and retain originals when working with important files.

            Some Android PDF tools create rasterized copies which do not preserve selectable text, forms or signatures. Folqio is not a certified secure-redaction tool.

            Project: https://brendigo.com
        """.trimIndent()
    }
}
