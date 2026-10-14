import SwiftUI
import UniformTypeIdentifiers
import PhotosUI
import VisionKit
import PDFKit

private enum MainSection { case home, files, favorites }

struct HomeView: View {
    @EnvironmentObject private var library: DocumentLibrary
    @State private var isImporterPresented = false
    @State private var selectedDocument: DocumentItem?
    @State private var query = ""
    @State private var importError: String?
    @State private var showSettings = false
    @State private var pendingRemoval: DocumentItem?
    @AppStorage("folqio_language") private var language = "en"
    @AppStorage("folqio_confirm_deletion") private var confirmDeletion = true
    @State private var renameItem: DocumentItem?
    @State private var showingScanner = false
    @State private var showingMerge = false
    @State private var onlyFavorites = false
    @State private var section: MainSection = .home
    @State private var selectedPhotos: [PhotosPickerItem] = []

    private var filtered: [DocumentItem] {
        library.documents.filter {
            (!(onlyFavorites || section == .favorites) || $0.isFavorite) &&
            (query.isEmpty || $0.filename.localizedCaseInsensitiveContains(query))
        }
    }

    private func tr(_ key: String) -> String {
        FolqioLanguages.text(key, language: language)
    }

    var body: some View {
        NavigationStack {
            ZStack {
                FolqioColors.midnight.ignoresSafeArea()
                ScrollView {
                    VStack(alignment: .leading, spacing: 22) {
                        HStack(spacing: 13) {
                            FolqioMark()
                            VStack(alignment: .leading, spacing: 3) {
                                Text("FOLQIO")
                                    .font(.system(size: 29, weight: .black, design: .rounded))
                                    .foregroundStyle(FolqioColors.pearl)
                                Text("Your documents. Your device.")
                                    .font(.subheadline)
                                    .foregroundStyle(FolqioColors.muted)
                            }
                            Spacer()
                            Button { showSettings = true } label: {
                                Image(systemName: "shield.lefthalf.filled")
                                    .font(.title2)
                                    .foregroundStyle(FolqioColors.mint)
                            }
                            .accessibilityLabel(tr("settings"))
                        }
                        .padding(.top, 22)

                        if section == .home {
                        FolqioCard {
                            VStack(alignment: .leading, spacing: 14) {
                                Image(systemName: "folder.badge.plus")
                                    .font(.system(size: 30))
                                    .foregroundStyle(FolqioColors.mint)
                                Text("Your documents, in your hands.")
                                    .font(.title2.bold())
                                    .foregroundStyle(FolqioColors.pearl)
                                Text("Read and organize PDFs on your device. No account or cloud required.")
                                    .foregroundStyle(FolqioColors.muted)
                                Button {
                                    isImporterPresented = true
                                } label: {
                                    Label("Open PDF", systemImage: "folder")
                                        .fontWeight(.semibold)
                                        .frame(maxWidth: .infinity)
                                        .padding(5)
                                }
                                .buttonStyle(.borderedProminent)
                                .tint(FolqioColors.mint)
                                .foregroundStyle(FolqioColors.midnight)
                            }
                        }

                        VStack(alignment: .leading, spacing: 12) {
                            Text("Quick actions")
                                .font(.title3.bold())
                                .foregroundStyle(FolqioColors.pearl)
                            LazyVGrid(columns: [GridItem(.adaptive(minimum: 74), spacing: 10)], spacing: 10) {
                                Button { isImporterPresented = true } label: {
                                    QuickAction(title: tr("open"), symbol: "folder")
                                }
                                Button {
                                    if VNDocumentCameraViewController.isSupported {
                                        showingScanner = true
                                    } else {
                                        importError = "Document scanning requires a camera-enabled iPhone or iPad."
                                    }
                                } label: {
                                    QuickAction(title: tr("scan"), symbol: "camera.viewfinder")
                                }
                                PhotosPicker(selection: $selectedPhotos,
                                             maxSelectionCount: PhotoPDFBuilder.maximumPhotos,
                                             matching: .images) {
                                    QuickAction(title: tr("photos"), symbol: "photo")
                                }
                                Button { showingMerge = true } label: {
                                    QuickAction(title: tr("merge"), symbol: "square.on.square")
                                }
                                .disabled(library.documents.count < 2)
                            }
                            Text("All document processing stays on your device.")
                                .font(.caption)
                                .foregroundStyle(FolqioColors.muted)
                        }

                        }

                        HStack(spacing: 12) {
                            Button("All files") { onlyFavorites = false }
                                .foregroundStyle(onlyFavorites ? FolqioColors.muted : FolqioColors.mint)
                            Button("Favorites") { onlyFavorites = true }
                                .foregroundStyle(onlyFavorites ? FolqioColors.mint : FolqioColors.muted)
                        }
                        .font(.subheadline.weight(.medium))

                        HStack {
                            Text(section == .favorites ? tr("favorites") : section == .files ? tr("files") : tr("recent"))
                                .font(.title2.bold())
                                .foregroundStyle(FolqioColors.pearl)
                            Spacer()
                            Text("\(library.documents.count) documents")
                                .foregroundStyle(FolqioColors.muted)
                                .font(.caption)
                        }
                        HStack {
                            Image(systemName: "magnifyingglass")
                                .foregroundStyle(FolqioColors.muted)
                            TextField("Search file names", text: $query)
                                .tint(FolqioColors.mint)
                                .foregroundStyle(FolqioColors.pearl)
                        }
                        .padding(15)
                        .background(FolqioColors.slate, in: RoundedRectangle(cornerRadius: 14))

                        if filtered.isEmpty {
                            Text(library.documents.isEmpty ? "Open a PDF to see it here." : "No matching documents.")
                                .foregroundStyle(FolqioColors.muted)
                                .padding(.vertical, 24)
                        }
                        ForEach(filtered) { document in
                            Button {
                                selectedDocument = document
                            } label: {
                                HStack(spacing: 15) {
                                    DocumentThumbnail(url: library.localURL(for: document))
                                    VStack(alignment: .leading, spacing: 4) {
                                        Text(document.filename)
                                            .fontWeight(.semibold)
                                            .foregroundStyle(FolqioColors.pearl)
                                            .lineLimit(2)
                                        Text("Local PDF")
                                            .font(.caption)
                                            .foregroundStyle(FolqioColors.muted)
                                    }
                                    Spacer()
                                    if document.isFavorite {
                                        Image(systemName: "star.fill")
                                            .foregroundStyle(FolqioColors.mint)
                                    }
                                    Image(systemName: "chevron.right")
                                        .foregroundStyle(FolqioColors.muted)
                                }
                                .padding(16)
                                .background(FolqioColors.slate, in: RoundedRectangle(cornerRadius: 15))
                            }
                            .contextMenu {
                                Button("Rename label", systemImage: "pencil") { renameItem = document }
                                Button(document.isFavorite ? "Remove favorite" : "Add favorite",
                                       systemImage: document.isFavorite ? "star.slash" : "star") {
                                    library.toggleFavorite(document)
                                }
                                ShareLink(item: library.localURL(for: document)) {
                                    Label("Share copy", systemImage: "square.and.arrow.up")
                                }
                                Button("Remove document", systemImage: "trash", role: .destructive) {
                                    if confirmDeletion {
                                        pendingRemoval = document
                                    } else if !library.remove(document) {
                                        importError = "The document could not be removed."
                                    }
                                }
                            }
                        }
                        Text("PRIVATE  ·  OFFLINE-FIRST  ·  SECURE")
                            .font(.caption2.weight(.medium))
                            .tracking(1)
                            .foregroundStyle(FolqioColors.muted)
                            .padding(.top, 15)
                    }
                    .padding(.horizontal, 22)
                    .padding(.bottom, 30)
                }
            }
            .safeAreaInset(edge: .bottom, spacing: 0) {
                bottomNavigation
            }
            .toolbar(.hidden, for: .navigationBar)
            .fileImporter(isPresented: $isImporterPresented, allowedContentTypes: [.pdf]) { result in
                switch result {
                case .success(let url):
                    do { selectedDocument = try library.importPDF(from: url) }
                    catch { importError = error.localizedDescription }
                case .failure(let error):
                    importError = error.localizedDescription
                }
            }
            .alert("Could not open PDF", isPresented: Binding(
                get: { importError != nil },
                set: { if !$0 { importError = nil } }
            )) {
                Button("OK", role: .cancel) { importError = nil }
            } message: {
                Text(importError ?? "")
            }
            .fullScreenCover(item: $selectedDocument) { item in
                ReaderView(item: item)
                    .environmentObject(library)
            }
            .fullScreenCover(isPresented: $showingScanner) {
                DocumentScannerView(onScan: { pdf in
                    showingScanner = false
                    do { _ = try library.createPDF(pdf, named: "Scanned document.pdf") }
                    catch { importError = error.localizedDescription }
                }, onCancel: {
                    showingScanner = false
                }, onError: { error in
                    showingScanner = false
                    importError = error.localizedDescription
                })
            }
            .sheet(isPresented: $showingMerge) {
                MergeDocumentsView(onCreated: { _ in })
                    .environmentObject(library)
            }
            .onChange(of: selectedPhotos) { photos in
                guard !photos.isEmpty else { return }
                Task {
                    let selected = photos
                    selectedPhotos = []
                    do {
                        guard PhotoPDFBuilder.isValidPhotoCount(selected.count) else {
                            throw PhotoPDFError.invalidSelection
                        }
                        let pdf = PDFDocument()
                        for photo in selected {
                            guard let data = try await photo.loadTransferable(type: Data.self),
                                  data.count <= PhotoPDFBuilder.maximumBytesPerPhoto,
                                  let image = UIImage(data: data)
                            else { throw PhotoPDFError.unreadableImage }
                            try PhotoPDFBuilder.appendImage(image, to: pdf)
                        }
                        let created = try library.createPDF(
                            pdf, named: selected.count == 1 ? "Photo document.pdf" : "Photo collection.pdf"
                        )
                        selectedDocument = created
                    } catch {
                        importError = error.localizedDescription
                    }
                }
            }
            .sheet(isPresented: $showSettings) {
                FolqioSettingsView().environmentObject(library)
            }
            .confirmationDialog("Remove document?", isPresented: Binding(
                get: { pendingRemoval != nil },
                set: { if !$0 { pendingRemoval = nil } }
            )) {
                Button("Remove", role: .destructive) {
                    if let item = pendingRemoval, !library.remove(item) {
                        importError = "The document could not be removed."
                    }
                    pendingRemoval = nil
                }
                Button("Cancel", role: .cancel) { pendingRemoval = nil }
            } message: {
                Text("Documents saved in Folqio are deleted. Imported originals remain untouched.")
            }
        }
        .tint(FolqioColors.mint)
    }

    private var bottomNavigation: some View {
        HStack(spacing: 2) {
            BottomNavigationItem(title: tr("home"), symbol: "house.fill", active: section == .home) {
                section = .home
                onlyFavorites = false
            }
            BottomNavigationItem(title: tr("files"), symbol: "folder", active: section == .files) {
                section = .files
                onlyFavorites = false
            }
            BottomNavigationItem(title: tr("scan"), symbol: "camera", active: false) {
                if VNDocumentCameraViewController.isSupported {
                    showingScanner = true
                } else {
                    importError = "Document scanning requires a camera-enabled iPhone or iPad."
                }
            }
            BottomNavigationItem(title: tr("favorites"), symbol: "star", active: section == .favorites) {
                section = .favorites
                onlyFavorites = true
            }
            BottomNavigationItem(title: tr("settings"), symbol: "gearshape", active: false) {
                showSettings = true
            }
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 8)
        .background(FolqioColors.slate)
    }
}

private struct QuickAction: View {
    let title: String
    let symbol: String

    var body: some View {
        VStack(spacing: 9) {
            Image(systemName: symbol)
                .font(.title2.weight(.medium))
                .frame(height: 28)
            Text(title)
                .font(.caption.weight(.semibold))
                .lineLimit(1)
                .minimumScaleFactor(0.75)
        }
        .foregroundStyle(FolqioColors.mint)
        .frame(maxWidth: .infinity)
        .padding(.vertical, 16)
        .background(FolqioColors.slate, in: RoundedRectangle(cornerRadius: 16))
    }
}

private struct BottomNavigationItem: View {
    let title: String
    let symbol: String
    let active: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: 5) {
                Image(systemName: symbol)
                    .font(.system(size: 19))
                Text(title)
                    .font(.system(size: 10, weight: .semibold))
            }
            .foregroundStyle(active ? FolqioColors.mint : FolqioColors.muted)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 5)
        }
        .buttonStyle(.plain)
    }
}
