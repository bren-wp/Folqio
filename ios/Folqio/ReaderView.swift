import SwiftUI
import PDFKit

struct ReaderView: View {
    @EnvironmentObject private var library: DocumentLibrary
    @Environment(\.dismiss) private var dismiss

    let item: DocumentItem

    @State private var document: PDFDocument?
    @State private var currentPageIndex = 0
    @State private var query = ""
    @State private var selectedSearchResult: PDFSelection?
    @State private var showingPages = false
    @State private var showingPassword = false
    @State private var unlockGeneration = 0
    @State private var showingNote = false
    @State private var noteText = ""
    @State private var message: String?
    @State private var recognizing = false
    @State private var ocrProgress: String?
    @State private var ocrTask: Task<Void, Never>?
    @State private var ocrResult: OCRResult?

    var body: some View {
        readerLayout
            .tint(FolqioColors.mint)
            .onAppear(perform: loadDocument)
            .onDisappear { ocrTask?.cancel() }
            .onChange(of: currentPageIndex) { page in
                library.recordPage(page, in: item)
            }
            .alert("Add a PDF note", isPresented: $showingNote) {
                TextField("Note text", text: $noteText)
                Button("Cancel", role: .cancel) { noteText = "" }
                Button("Save", action: saveNote)
            } message: {
                Text("Adds a note annotation to the current page.")
            }
            .sheet(isPresented: $showingPassword) {
                if let document {
                    UnlockDocumentSheet(
                        document: document,
                        onUnlocked: {
                            showingPassword = false
                            unlockGeneration += 1
                        },
                        onCancel: {
                            showingPassword = false
                            dismiss()
                        }
                    )
                }
            }
            .sheet(item: $ocrResult) { result in
                OCRResultView(result: result)
            }
            .sheet(isPresented: $showingPages) {
                PageToolsView(item: item) { _ in
                    message = "Saved an edited copy to your document library."
                }
                .environmentObject(library)
            }
            .alert("PDF reader", isPresented: messageIsPresented) {
                Button("OK", role: .cancel) { message = nil }
            } message: {
                Text(message ?? "")
            }
    }

    private var messageIsPresented: Binding<Bool> {
        Binding(get: { message != nil }, set: { if !$0 { message = nil } })
    }

    private var readerLayout: some View {
        ZStack {
            FolqioColors.midnight.ignoresSafeArea()
            VStack(spacing: 0) {
                navigationBar
                searchBar
                if let ocrProgress {
                    HStack {
                        ProgressView().tint(FolqioColors.mint)
                        Text(ocrProgress).font(.caption).foregroundStyle(FolqioColors.muted)
                        Spacer()
                        Button("Cancel") { ocrTask?.cancel() }
                            .font(.caption.weight(.semibold))
                    }
                    .padding(.horizontal, 18)
                    .padding(.bottom, 8)
                }
                documentArea
                pageControls
            }
        }
    }

    private var navigationBar: some View {
        HStack(spacing: 14) {
            Button(action: { dismiss() }) {
                Image(systemName: "chevron.left")
                    .font(.title3.bold())
            }
            .accessibilityLabel("Back")
            VStack(alignment: .leading, spacing: 4) {
                Text(item.filename)
                    .font(.headline)
                    .foregroundStyle(FolqioColors.pearl)
                    .lineLimit(1)
                Text(pageDescription)
                    .font(.caption)
                    .foregroundStyle(FolqioColors.muted)
            }
            Spacer(minLength: 0)
            Menu {
                Button(action: recognizeCurrentPage) {
                    Label("Recognize current page", systemImage: "text.viewfinder")
                }
                Button(action: recognizeWholeDocument) {
                    Label("Recognize entire PDF (max 25 pages)", systemImage: "doc.text.magnifyingglass")
                }
                .disabled(!OfflineOCR.canScanWholeDocument(document?.pageCount ?? 0))
            } label: {
                if recognizing {
                    ProgressView()
                } else {
                    Image(systemName: "text.viewfinder")
                }
            }
            .disabled(document == nil || document?.isLocked == true || recognizing)
            .accessibilityLabel("Offline OCR options")
            ShareLink(item: library.localURL(for: item)) {
                Image(systemName: "square.and.arrow.up")
            }
            .accessibilityLabel("Share PDF")
        }
        .padding(17)
    }

    private var pageDescription: String {
        guard let document else { return "Opening…" }
        return "\(currentPageIndex + 1) / \(document.pageCount)"
    }

    private var searchBar: some View {
        HStack(spacing: 12) {
            Image(systemName: "magnifyingglass")
            TextField("Find text in this PDF", text: $query)
                .submitLabel(.search)
                .onSubmit(search)
                .autocorrectionDisabled()
            if !query.isEmpty {
                Button("Find", action: search).fontWeight(.semibold)
            }
        }
        .foregroundStyle(FolqioColors.pearl)
        .padding(14)
        .background(FolqioColors.slate, in: RoundedRectangle(cornerRadius: 13))
        .padding(.horizontal, 16)
        .padding(.bottom, 10)
    }

    @ViewBuilder
    private var documentArea: some View {
        if let document {
            PDFViewer(document: document,
                      initialPage: item.lastReadPage,
                      currentPageIndex: $currentPageIndex,
                      selection: selectedSearchResult)
                .id(unlockGeneration)
                .background(Color.white)
        } else {
            Spacer()
            Text("Document cannot be opened.")
                .foregroundStyle(FolqioColors.pearl)
            Spacer()
        }
    }

    private var pageControls: some View {
        HStack(spacing: 20) {
            Button(action: previousPage) {
                Image(systemName: "chevron.left")
            }
            .disabled(currentPageIndex == 0)
            .accessibilityLabel("Previous page")
            Spacer()
            Button(action: { library.toggleFavorite(item) }) {
                Image(systemName: isFavorite ? "star.fill" : "star")
            }
            .accessibilityLabel("Toggle favorite")
            Spacer()
            Button(action: { showingNote = true }) {
                Label("Add note", systemImage: "note.text.badge.plus")
            }
            .disabled(document == nil)
            Spacer()
            Button(action: highlightSearchResult) {
                Image(systemName: "highlighter")
            }
            .disabled(selectedSearchResult == nil)
            .accessibilityLabel("Highlight found text")
            Spacer()
            Button(action: { showingPages = true }) {
                Image(systemName: "square.grid.2x2")
            }
            .disabled(document == nil)
            .accessibilityLabel("Manage PDF pages")
            Spacer()
            Button(action: nextPage) {
                Image(systemName: "chevron.right")
            }
            .disabled(currentPageIndex >= (document?.pageCount ?? 1) - 1)
            .accessibilityLabel("Next page")
        }
        .foregroundStyle(FolqioColors.mint)
        .padding(18)
    }

    private var isFavorite: Bool {
        library.documents.first(where: { $0.id == item.id })?.isFavorite == true
    }

    private func loadDocument() {
        let loaded = PDFDocument(url: library.localURL(for: item))
        document = loaded
        if loaded?.isLocked == true { showingPassword = true }
        currentPageIndex = min(item.lastReadPage, max(0, (loaded?.pageCount ?? 1) - 1))
    }

    private func previousPage() {
        guard currentPageIndex > 0 else { return }
        selectedSearchResult = nil
        currentPageIndex -= 1
    }

    private func nextPage() {
        guard let document, currentPageIndex + 1 < document.pageCount else { return }
        selectedSearchResult = nil
        currentPageIndex += 1
    }

    private func recognizeCurrentPage() {
        guard !recognizing, let pdf = document, !pdf.isLocked,
              let page = pdf.page(at: currentPageIndex) else { return }
        let pageNumber = currentPageIndex + 1
        recognizing = true
        ocrProgress = "Reading page \(pageNumber)…"
        ocrTask = Task {
            defer {
                recognizing = false
                ocrProgress = nil
                ocrTask = nil
            }
            do {
                let text = try await OfflineOCR.recognize(page)
                try Task.checkCancellation()
                ocrResult = OCRResult(pageNumber: pageNumber, content: text)
            } catch is CancellationError {
                // User cancelled processing.
            } catch {
                message = error.localizedDescription
            }
        }
    }

    private func recognizeWholeDocument() {
        guard !recognizing, let pdf = document, !pdf.isLocked else { return }
        guard OfflineOCR.canScanWholeDocument(pdf.pageCount) else {
            message = "Whole-document OCR supports PDFs of up to 25 pages. You can still recognize an individual page."
            return
        }
        recognizing = true
        ocrTask = Task {
            defer {
                recognizing = false
                ocrProgress = nil
                ocrTask = nil
            }
            var sections: [String] = []
            do {
                for index in 0..<pdf.pageCount {
                    try Task.checkCancellation()
                    ocrProgress = "Recognizing page \(index + 1) of \(pdf.pageCount)…"
                    guard let page = pdf.page(at: index) else { continue }
                    do {
                        let text = try await OfflineOCR.recognize(page)
                        try Task.checkCancellation()
                        sections.append("Page \(index + 1)\n\(text.prefix(8000))")
                    } catch OCRFailure.emptyText {
                        // Blank pages are normal and should not abort the document.
                    }
                }
                try Task.checkCancellation()
                guard !sections.isEmpty else { throw OCRFailure.emptyText }
                ocrResult = OCRResult(pageNumber: 0, content: sections.joined(separator: "\n\n"))
            } catch is CancellationError {
                // Do not show incomplete OCR results.
            } catch {
                message = error.localizedDescription
            }
        }
    }

    private func search() {
        guard let document, !query.isEmpty else { return }
        selectedSearchResult = document.findString(query, withOptions: .caseInsensitive).first
        if selectedSearchResult == nil { message = "No matching text found." }
    }

    private func highlightSearchResult() {
        guard let document, let selection = selectedSearchResult,
              let selectedPage = selection.pages.first,
              selection.pages.count == 1 else {
            message = "Find text on one page before highlighting."
            return
        }
        let bounds = selection.bounds(for: selectedPage)
        guard !bounds.isEmpty else {
            message = "The selected text cannot be highlighted."
            return
        }
        let annotation = PDFAnnotation(bounds: bounds, forType: .highlight, withProperties: nil)
        annotation.color = UIColor.systemYellow.withAlphaComponent(0.45)
        selectedPage.addAnnotation(annotation)
        if document.write(to: library.localURL(for: item)) {
            selectedSearchResult = nil
            message = "Highlight saved to your local document."
        } else {
            selectedPage.removeAnnotation(annotation)
            message = "The highlight could not be saved."
        }
    }

    private func saveNote() {
        let text = noteText.trimmingCharacters(in: .whitespacesAndNewlines)
        noteText = ""
        guard !text.isEmpty,
              let document,
              let page = document.page(at: currentPageIndex) else { return }

        let pageBounds = page.bounds(for: .mediaBox)
        let noteBounds = CGRect(x: pageBounds.midX - 15, y: pageBounds.midY - 15,
                                width: 30, height: 30)
        let note = PDFAnnotation(bounds: noteBounds, forType: .text, withProperties: nil)
        note.contents = text
        note.color = .systemMint
        page.addAnnotation(note)
        if !document.write(to: library.localURL(for: item)) {
            page.removeAnnotation(note)
            message = "The note could not be saved."
        }
    }
}

private struct PDFViewer: UIViewRepresentable {
    let document: PDFDocument
    let initialPage: Int
    @Binding var currentPageIndex: Int
    let selection: PDFSelection?

    func makeCoordinator() -> Coordinator { Coordinator(self) }

    func makeUIView(context: Context) -> PDFView {
        let view = PDFView()
        view.autoScales = true
        view.displayMode = .singlePageContinuous
        view.backgroundColor = .white
        view.document = document
        if let page = document.page(at: initialPage) { view.go(to: page) }
        context.coordinator.observe(view)
        return view
    }

    func updateUIView(_ view: PDFView, context: Context) {
        context.coordinator.parent = self
        if view.document !== document { view.document = document }
        if let selection {
            if context.coordinator.appliedSelection !== selection {
                view.highlightedSelections = [selection]
                view.go(to: selection)
                context.coordinator.appliedSelection = selection
            }
            return
        }

        context.coordinator.appliedSelection = nil
        view.highlightedSelections = nil
        guard let currentPage = view.currentPage else { return }
        if document.index(for: currentPage) != currentPageIndex,
           let target = document.page(at: currentPageIndex) {
            view.go(to: target)
        }
    }

    final class Coordinator {
        var parent: PDFViewer
        var appliedSelection: PDFSelection?
        private var observer: NSObjectProtocol?

        init(_ parent: PDFViewer) { self.parent = parent }

        func observe(_ view: PDFView) {
            observer = NotificationCenter.default.addObserver(
                forName: .PDFViewPageChanged,
                object: view,
                queue: .main
            ) { [weak self, weak view] _ in
                guard let view, let page = view.currentPage,
                      let index = view.document?.index(for: page),
                      index != NSNotFound else { return }
                self?.parent.currentPageIndex = index
            }
        }

        deinit {
            if let observer { NotificationCenter.default.removeObserver(observer) }
        }
    }
}
