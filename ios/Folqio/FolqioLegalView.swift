import SwiftUI

enum FolqioLegalKind: String, Identifiable {
    case privacy
    case terms
    var id: String { rawValue }
}

struct FolqioLegalView: View {
    let kind: FolqioLegalKind
    let language: String
    @Environment(\.dismiss) private var dismiss

    private var title: String {
        FolqioLanguages.text(kind == .privacy ? "privacy" : "terms", language: language)
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                Text(copy)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .font(.body)
                    .foregroundStyle(FolqioColors.pearl)
                    .lineSpacing(6)
                    .padding(22)
            }
            .background(FolqioColors.midnight)
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
        }
        .tint(FolqioColors.mint)
    }

    private var copy: String {
        let croatian = language == "hr"
        if kind == .privacy {
            return croatian ? """
            POLITIKA PRIVATNOSTI — FOLQIO
            Autor: Brendigo
            Ažurirano: 13. listopada 2026.

            Folqio omogućuje rad s PDF dokumentima na uređaju. Aplikacija ne zahtijeva korisnički račun, ne sadrži oglasni sustav i ne šalje dokumente na poslužitelj Folqija.

            Datoteke koje odaberete ili izradite obrađuju se funkcijama operacijskog sustava. Za izvoz ili dijeljenje koriste se funkcije koje sami pokrećete. Ako odaberete vanjsku uslugu za dijeljenje, primjenjuju se pravila te usluge.

            Postavke jezika i čitanja pohranjuju se na uređaju. Dokumente i podatke koje je aplikacija izradila možete ukloniti iz njezine biblioteke.

            Za informacije o projektu: https://brendigo.com

            Ovaj tekst opisuje ponašanje aplikacije, a nije zamjena za dodatne informacije koje mogu biti potrebne prilikom distribucije putem trgovina aplikacija.
            """ : """
            PRIVACY POLICY — FOLQIO
            Created by: Brendigo
            Updated: October 13, 2026.

            Folqio helps you work with PDF documents on your device. It does not require an account, use advertising systems, or send documents to a Folqio server.

            Documents you select or create are processed with operating-system features. Exports and sharing are initiated by you. If you choose an external sharing service, that service's privacy practices apply.

            Your language and reading preferences stay on your device. You can remove documents created by Folqio from the app's library.

            Project information: https://brendigo.com

            This describes the app's current behavior and does not replace additional disclosures required by app stores.
            """
        }
        return croatian ? """
        UVJETI KORIŠTENJA — FOLQIO
        Autor: Brendigo
        Ažurirano: 13. listopada 2026.

        Folqio je alat za pregled, organiziranje i obradu PDF dokumenata. Koristite ga samo za dokumente kojima imate pravo pristupiti i kojima smijete raspolagati.

        Prije spremanja, dijeljenja ili uređivanja važnih dokumenata provjerite rezultat i zadržite kopiju izvornika. Neke funkcije na Androidu izvoze stranice kao rasterske kopije, zbog čega se mogu izgubiti uređivi tekst, obrasci i digitalni potpisi.

        Folqio nije alat za provjereno nepovratno uklanjanje osjetljivih podataka iz dokumenata. Aplikacija se pruža bez jamstva da je prikladna za svaku vrstu ili format PDF-a, u granicama primjenjivog prava.

        Autor i projekt: Brendigo — https://brendigo.com
        """ : """
        TERMS OF USE — FOLQIO
        Created by: Brendigo
        Updated: October 13, 2026.

        Folqio helps you view, organize and process PDF documents. Use it only with documents you have permission to access and handle.

        Review exported or edited files and keep originals when working with important documents. Some Android page operations create rasterized copies that may not preserve text selection, forms or digital signatures.

        Folqio is not a certified irreversible redaction tool. The software is provided without a guarantee of suitability for every document or PDF format, to the extent permitted by applicable law.

        Created by Brendigo — https://brendigo.com
        """
    }
}
