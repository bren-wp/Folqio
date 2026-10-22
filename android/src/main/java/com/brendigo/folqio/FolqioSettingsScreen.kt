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
    busy: Boolean,
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
                    enabled = selection.isNotEmpty() && !busy,
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
                }, enabled = files.isNotEmpty() && !busy, modifier = Modifier.fillMaxWidth()) {
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

            Folqio obrađuje PDF dokumente i odabrane fotografije lokalno na Android uređaju. Ne zahtijeva račun, ne prikazuje oglase, ne sadrži sustav praćenja i ne šalje dokumente na Folqio poslužitelje.

            U privatnu pohranu aplikacije spremaju se uvezeni ili izrađeni PDF-ovi, nazivi dokumenata, omiljene stavke, oznake stranica, posljednja otvorena stranica i postavke. Datoteke možete ukloniti iz biblioteke. U slučaju prekinutog brisanja privremena se kopija može sačuvati radi zaštite od gubitka podataka.

            Sustavnu kameru i odabir datoteka koristite samo na vlastiti zahtjev. Izvoz i dijeljenje također pokrećete sami; odabrana vanjska usluga može obrađivati podijeljene datoteke prema svojim pravilima. Brisanje aplikacije u pravilu uklanja njezinu privatnu pohranu, ali ne i kopije koje ste izvezli drugdje.

            Razvojni tim i kontakt: Brendigo · https://brendigo.com
            Cjelovita politika: github.com/bren-wp/Folqio/blob/main/docs/PRIVACY_POLICY.md
        """.trimIndent() else """
            PRIVACY POLICY — FOLQIO
            Created by Brendigo · Updated October 22, 2026.

            Folqio processes PDF documents and selected photos locally on your Android device. No account, advertising or tracking system is required, and documents are not uploaded to a Folqio server.

            Imported and generated PDFs, document names, favorites, bookmarks, last page and preferences are saved in private app storage. You can remove documents from the library. After an interrupted deletion, a temporary copy may be retained to prevent data loss.

            System camera and document selection operate when you request them. You choose when to export or share files; recipient services may process shared content under their own policies. Uninstalling normally removes Folqio's private data, but not files exported elsewhere.

            Developer and contact: Brendigo · https://brendigo.com
            Full privacy policy: github.com/bren-wp/Folqio/blob/main/docs/PRIVACY_POLICY.md
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
