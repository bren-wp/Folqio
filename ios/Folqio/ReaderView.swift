import SwiftUI
import PDFKit

private enum AnnotationEditor: String, Identifiable {
    case note
    case text

    var id: String { rawValue }
    var title: String { self == .note ? "Add a note" : "Add a text overlay" }
}

struct ReaderView: View {
    @EnvironmentObject private var library: DocumentLibrary
    @Environment(\.dismiss) private var dismiss

    let item: DocumentItem

    @AppStorage("folqio_reopen_last_page") private var reopenLastPage = true
    @State private var document: PDFDocument?
    @State private var currentPageIndex = 0
    @State private var query = ""
    @State private var selectedSearchResult: PDFSelection?
    @State private var showingPages = false
    @State private var showingPassword = false
    @State private var unlockGeneration = 0
    @State private var annotationEditor: AnnotationEditor?
    @State private var annotationText = ""
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
            .sheet(item: $annotationEditor) { editor in
                NavigationStack {
                    Form {
                        Section {
                            TextField("Enter text", text: $annotationText, axis: .vertical)
                                .lineLimit(3...8)
                        } footer: {
                            Text("An editable PDF annotation is added to a new document copy. Existing PDF text is not replaced, and signatures in edited copies may no longer validate.")
                        }
                    }
                    .navigationTitle(editor.title)
                    .navigationBarTitleDisplayMode(.inline)
                    .toolbar {
                        ToolbarItem(placement: .cancellationAction) {
                            Button("Cancel") {
                                annotationText = ""
                                annotationEditor = nil
                            }
                        }
                        ToolbarItem(placement: .confirmationAction) {
                            Button("Save copy") {
                                if editor == .note { saveNote() } else { saveTextOverlay() }
                                annotationEditor = nil
                            }
                            .disabled(annotationText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                        }
                    }
                }
                .tint(FolqioColors.mint)
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
            Button { annotationText = ""; annotationEditor = .text } label: {
                Image(systemName: "text.badge.plus")
            }
            .disabled(document == nil || document?.isLocked == true)
            .accessibilityLabel("Add text to a PDF copy")
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
                      initialPage: reopenLastPage ? item.lastReadPage : 0,
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

    // Icon-only, equal-width controls avoid clipping on compact iPhones and Dynamic Type.
    private var pageControls: some View {
        HStack(spacing: 3) {
            ReaderAction(symbol: "chevron.left", label: "Previous page",
                         disabled: currentPageIndex == 0, action: previousPage)
            ReaderAction(symbol: isFavorite ? "star.fill" : "star",
                         label: "Toggle favorite") { library.toggleFavorite(item) }
            ReaderAction(symbol: "note.text.badge.plus", label: "Add note to copy",
                         disabled: document == nil || document?.isLocked == true) {
                annotationText = ""
                annotationEditor = .note
            }
            ReaderAction(symbol: "highlighter", label: "Highlight found text",
                         disabled: selectedSearchResult == nil, action: highlightSearchResult)
            ReaderAction(symbol: "square.grid.2x2", label: "Manage PDF pages",
                         disabled: document == nil) { showingPages = true }
            ReaderAction(symbol: "chevron.right", label: "Next page",
                         disabled: currentPageIndex >= (document?.pageCount ?? 1) - 1,
                         action: nextPage)
        }
        .foregroundStyle(FolqioColors.mint)
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(FolqioColors.slate)
    }

    private var isFavorite: Bool {
        library.documents.first(where: { $0.id == item.id })?.isFavorite == true
    }

    private func loadDocument() {
        let loaded = PDFDocument(url: library.localURL(for: item))
        document = loaded
        if loaded?.isLocked == true { showingPassword = true }
        currentPageIndex = min(reopenLastPage ? item.lastReadPage : 0, max(0, (loaded?.pageCount ?? 1) - 1))
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
        let pageIndex = document.index(for: selectedPage)
        let bounds = selection.bounds(for: selectedPage)
        guard pageIndex != NSNotFound, !bounds.isEmpty else {
            message = "The selected text cannot be highlighted."
            return
        }
        let annotation = PDFAnnotation(bounds: bounds, forType: .highlight, withProperties: nil)
        annotation.color = UIColor.systemYellow.withAlphaComponent(0.45)
        saveAnnotatedCopy(annotation, pageIndex: pageIndex, prefix: "Highlighted")
    }

    private func saveNote() {
        let text = String(annotationText.trimmingCharacters(in: .whitespacesAndNewlines).prefix(2000))
        annotationText = ""
        guard !text.isEmpty,
              let document,
              let page = document.page(at: currentPageIndex) else { return }

        let bounds = page.bounds(for: .mediaBox)
        let note = PDFAnnotation(
            bounds: CGRect(x: bounds.midX - 15, y: bounds.midY - 15, width: 30, height: 30),
            forType: .text, withProperties: nil
        )
        note.contents = text
        note.color = .systemMint
        saveAnnotatedCopy(note, pageIndex: currentPageIndex, prefix: "Note")
    }

    private func saveTextOverlay() {
        let text = annotationText.trimmingCharacters(in: .whitespacesAndNewlines)
        annotationText = ""
        guard !text.isEmpty, let page = document?.page(at: currentPageIndex) else { return }
        let visible = page.bounds(for: .mediaBox).insetBy(dx: 18, dy: 18)
        guard visible.width > 0, visible.height > 0 else {
            message = "The selected page cannot accept text."
            return
        }
        let width = min(300, visible.width)
        let height = min(90, visible.height)
        let placement = CGRect(x: visible.midX - width / 2,
                               y: visible.midY - height / 2,
                               width: width, height: height)
        let annotation = PDFAnnotation(bounds: placement, forType: .freeText,
                                       withProperties: nil)
        annotation.contents = text
        annotation.font = .systemFont(ofSize: 16)
        annotation.fontColor = .black
        annotation.color = UIColor.white.withAlphaComponent(0.9)
        saveAnnotatedCopy(annotation, pageIndex: currentPageIndex, prefix: "Text")
    }

    /// Edits are saved through the protected library writer, never PDFKit.write(to:)
    /// over the source. PDF text and vectors stay intact; existing signatures
    /// may become invalid in the newly annotated copy.
    private func saveAnnotatedCopy(_ annotation: PDFAnnotation, pageIndex: Int,
                                   prefix: String) {
        guard let document else { return }
        do {
            let copy = try DocumentTools.annotatedCopy(document, index: pageIndex,
                                                       annotation: annotation)
            _ = try library.createPDF(copy, named: "\(prefix) - \(item.filename)")
            selectedSearchResult = nil
            message = "New PDF copy saved to your library. Your original is unchanged. Digital signatures in an edited copy may no longer be valid."
        } catch {
            message = "Could not save the PDF copy: \(error.localizedDescription)"
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

private struct ReaderAction: View {
    let symbol: String
    let label: String
    var disabled = false
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: symbol)
                .font(.system(size: 19, weight: .medium))
                .frame(maxWidth: .infinity, minHeight: 48)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(disabled)
        .accessibilityLabel(label)
    }
}
