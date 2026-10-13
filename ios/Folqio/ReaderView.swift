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
    @State private var showingNote = false
    @State private var noteText = ""
    @State private var message: String?

    var body: some View {
        ZStack {
            FolqioColors.midnight.ignoresSafeArea()
            VStack(spacing: 0) {
                HStack {
                    Button { dismiss() } label: {
                        Image(systemName: "chevron.left").font(.title3.bold())
                    }
                    .accessibilityLabel("Back")
                    VStack(alignment: .leading) {
                        Text(item.filename)
                            .font(.headline)
                            .foregroundStyle(FolqioColors.pearl)
                            .lineLimit(1)
                        Text(document.map { "\(currentPageIndex + 1) / \($0.pageCount)" } ?? "Opening…")
                            .font(.caption)
                            .foregroundStyle(FolqioColors.muted)
                    }
                    .padding(.leading, 10)
                    Spacer()
                    if let url = Optional(library.localURL(for: item)) {
                        ShareLink(item: url) {
                            Image(systemName: "square.and.arrow.up")
                        }
                        .accessibilityLabel("Share PDF")
                    }
                }
                .padding(17)

                HStack(spacing: 12) {
                    Image(systemName: "magnifyingglass")
                    TextField("Find text in this PDF", text: $query)
                        .submitLabel(.search)
                        .onSubmit(search)
                        .autocorrectionDisabled()
                    if !query.isEmpty {
                        Button("Find", action: search)
                            .fontWeight(.semibold)
                    }
                }
                .foregroundStyle(FolqioColors.pearl)
                .padding(14)
                .background(FolqioColors.slate, in: RoundedRectangle(cornerRadius: 13))
                .padding(.horizontal, 16)
                .padding(.bottom, 10)

                if let document {
                    PDFViewer(document: document,
                              initialPage: item.lastReadPage,
                              currentPageIndex: $currentPageIndex,
                              selection: selectedSearchResult)
                        .background(Color.white)
                } else {
                    Spacer()
                    Text("Document cannot be opened.")
                        .foregroundStyle(FolqioColors.pearl)
                    Spacer()
                }

                HStack(spacing: 20) {
                    Button {
                        if currentPageIndex > 0 {
                            currentPageIndex -= 1
                            selectedSearchResult = document?.page(at: currentPageIndex)?.selectionForEntirePage()
                        }
                    } label: {
                        Image(systemName: "chevron.left")
                    }
                    .disabled(currentPageIndex == 0)
                    Spacer()
                    Button { library.toggleFavorite(item) } label: {
                        Image(systemName: library.documents.first(where: { $0.id == item.id })?.isFavorite == true
                              ? "star.fill" : "star")
                    }
                    .accessibilityLabel("Toggle favorite")
                    Spacer()
                    Button { showingNote = true } label: {
                        Label("Add note", systemImage: "note.text.badge.plus")
                    }
                    .disabled(document == nil)
                    Spacer()
                    Button {
                        if let document, currentPageIndex < document.pageCount - 1 {
                            currentPageIndex += 1
                            selectedSearchResult = document.page(at: currentPageIndex)?.selectionForEntirePage()
                        }
                    } label: {
                        Image(systemName: "chevron.right")
                    }
                    .disabled(currentPageIndex >= (document?.pageCount ?? 1) - 1)
                }
                .foregroundStyle(FolqioColors.mint)
                .padding(18)
            }
        }
        .tint(FolqioColors.mint)
        .onAppear {
            let loaded = PDFDocument(url: library.localURL(for: item))
            document = loaded
            currentPageIndex = min(item.lastReadPage, max(0, (loaded?.pageCount ?? 1) - 1))
        }
        .onChange(of: currentPageIndex) { page in
            library.recordPage(page, in: item)
        }
        .alert("Add a PDF note", isPresented: $showingNote) {
            TextField("Note text", text: $noteText)
            Button("Cancel", role: .cancel) { noteText = "" }
            Button("Save") { saveNote() }
        } message: {
            Text("Adds a note annotation to the current page.")
        }
        .alert("PDF editor", isPresented: Binding(
            get: { message != nil },
            set: { if !$0 { message = nil } }
        )) {
            Button("OK", role: .cancel) { message = nil }
        } message: {
            Text(message ?? "")
        }
    }

    private func search() {
        guard let document, !query.isEmpty else { return }
        selectedSearchResult = document.findString(query, withOptions: .caseInsensitive).first
        if selectedSearchResult == nil { message = "No matching text found." }
    }

    private func saveNote() {
        let text = noteText.trimmingCharacters(in: .whitespacesAndNewlines)
        noteText = ""
        guard !text.isEmpty, let document, let page = document.page(at: currentPageIndex) else { return }
        let bounds = page.bounds(for: .mediaBox)
        let annotation = PDFAnnotation(bounds: CGRect(x: bounds.midX - 15, y: bounds.midY - 15,
                                                      width: 30, height: 30),
                                       forType: .text, withProperties: nil)
        annotation.contents = text
        annotation.color = .systemMint
        page.addAnnotation(annotation)
        if !document.write(to: library.localURL(for: item)) {
            page.removeAnnotation(annotation)
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
        view.backgroundColor = UIColor.white
        view.document = document
        if let page = document.page(at: initialPage) { view.go(to: page) }
        context.coordinator.observe(view)
        return view
    }

    func updateUIView(_ view: PDFView, context: Context) {
        if view.document !== document { view.document = document }
        if let selection {
            view.highlightedSelections = [selection]
            view.go(to: selection)
        }
        if let current = view.currentPage,
           let index = document.index(for: current),
           index != currentPageIndex,
           let target = document.page(at: currentPageIndex),
           selection == nil {
            view.go(to: target)
        }
    }

    final class Coordinator {
        var parent: PDFViewer
        private var observer: NSObjectProtocol?

        init(_ parent: PDFViewer) { self.parent = parent }

        func observe(_ view: PDFView) {
            observer = NotificationCenter.default.addObserver(
                forName: .PDFViewPageChanged, object: view, queue: .main
            ) { [weak self, weak view] _ in
                guard let view, let page = view.currentPage,
                      let index = view.document?.index(for: page) else { return }
                self?.parent.currentPageIndex = index
            }
        }

        deinit {
            if let observer { NotificationCenter.default.removeObserver(observer) }
        }
    }
}
