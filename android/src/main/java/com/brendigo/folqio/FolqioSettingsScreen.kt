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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val slate = Color(0xFF1A2433)
private val mint = Color(0xFF22D3A7)
private val pearl = Color(0xFFF7FAFC)
private val muted = Color(0xFFA9B9C9)

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
        Card(colors = CardDefaults.cardColors(containerColor = slate), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(19.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Text("Folqio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = pearl)
                Text("Your documents. Your device.", color = muted)
                HorizontalDivider(color = muted.copy(alpha = .2f))
                Text(tr("language"), color = pearl, style = MaterialTheme.typography.titleMedium)
                Box {
                    OutlinedButton(onClick = { languageMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Language, null)
                        Spacer(Modifier.width(10.dp))
                        Text(FolqioLanguages.supported[language] ?: "English")
                    }
                    DropdownMenu(expanded = languageMenu, onDismissRequest = { languageMenu = false }) {
                        Box(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                            Column {
                                FolqioLanguages.supported.forEach { (code, name) ->
                                    DropdownMenuItem(text = { Text(name) }, onClick = {
                                        onLanguageChange(code)
                                        languageMenu = false
                                    })
                                }
                            }
                        }
                    }
                }
                Text("Choose the language that feels right for you.",
                    style = MaterialTheme.typography.bodySmall, color = muted)
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = slate), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(19.dp)) {
                Text("Reading & documents", color = pearl, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("reopen"), color = pearl, modifier = Modifier.weight(1f))
                    Switch(checked = reopenLastPage, onCheckedChange = onReopenChange)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("confirm"), color = pearl, modifier = Modifier.weight(1f))
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

        Card(colors = CardDefaults.cardColors(containerColor = slate), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(19.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Privacy & information", color = pearl,
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Documents stay under your control. No Folqio account or advertising.",
                    color = muted)
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
                HorizontalDivider(color = muted.copy(alpha = .2f))
                Text("${tr("author")} Brendigo", color = pearl)
                Text("Folqio · © 2026 Brendigo", color = muted,
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
            Ažurirano: 13. listopada 2026.

            Folqio obrađuje PDF dokumente na uređaju. Ne zahtijeva korisnički račun, ne sadrži oglase i ne šalje dokumente na Folqio poslužitelje.

            Aplikacija sprema odabrane postavke i nedavne dokumente u privatnu pohranu uređaja. Izvoz u ZIP ili dijeljenje pokrećete sami. Vanjske usluge dijeljenja imaju vlastita pravila privatnosti.

            Više o projektu: https://brendigo.com
        """.trimIndent() else """
            PRIVACY POLICY — FOLQIO
            Created by Brendigo · Updated October 13, 2026.

            Folqio processes PDF documents on your device. No account or advertising system is required, and documents are not sent to a Folqio server.

            Preferences and recently opened document references are stored within the app. ZIP exports and sharing occur only when you choose them. Third-party sharing destinations have their own privacy practices.

            More information: https://brendigo.com
        """.trimIndent()

        return if (hr) """
            UVJETI KORIŠTENJA — FOLQIO
            Autor: Brendigo · Ažurirano: 13. listopada 2026.

            Folqio je namijenjen pregledu i obradi dokumenata koje imate pravo koristiti. Prije dijeljenja važnih datoteka provjerite rezultate i sačuvajte izvornike.

            Neki Android alati izvoze rasterske PDF kopije i ne čuvaju uređivi tekst, obrasce ili digitalne potpise. Folqio nije certificirani alat za nepovratno uklanjanje osjetljivih podataka.

            Projekt: https://brendigo.com
        """.trimIndent() else """
            TERMS OF USE — FOLQIO
            Created by Brendigo · Updated October 13, 2026.

            Use Folqio only with documents you have the right to access and process. Review results and retain originals when working with important files.

            Some Android PDF tools create rasterized copies which do not preserve selectable text, forms or signatures. Folqio is not a certified secure-redaction tool.

            Project: https://brendigo.com
        """.trimIndent()
    }
}
