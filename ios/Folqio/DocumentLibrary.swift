import Foundation
import PDFKit

struct DocumentItem: Codable, Identifiable, Hashable {
    let id: UUID
    var filename: String
    let relativePath: String
    var isFavorite: Bool
    var lastReadPage: Int
}

enum DocumentError: LocalizedError {
    case notPDF
    case inaccessible

    var errorDescription: String? {
        switch self {
        case .notPDF: return "The selected file is not a readable PDF."
        case .inaccessible: return "The document is unavailable."
        }
    }
}

@MainActor
final class DocumentLibrary: ObservableObject {
    @Published private(set) var documents: [DocumentItem] = []
    let rootURL: URL

    init(rootURL: URL? = nil) {
        let support = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
        var directory = rootURL ?? support.appendingPathComponent("Folqio", isDirectory: true)
        try? FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        var resourceValues = URLResourceValues()
        resourceValues.isExcludedFromBackup = true
        try? directory.setResourceValues(resourceValues)
        self.rootURL = directory
        load()
    }

    static func isPDFName(_ name: String) -> Bool {
        name.lowercased().hasSuffix(".pdf")
    }

    func localURL(for item: DocumentItem) -> URL {
        rootURL.appendingPathComponent(item.relativePath, isDirectory: false)
    }

    @discardableResult
    func importPDF(from input: URL) throws -> DocumentItem {
        guard Self.isPDFName(input.lastPathComponent) else { throw DocumentError.notPDF }
        let granted = input.startAccessingSecurityScopedResource()
        defer { if granted { input.stopAccessingSecurityScopedResource() } }

        guard FileManager.default.isReadableFile(atPath: input.path),
              PDFDocument(url: input) != nil
        else { throw DocumentError.notPDF }

        let id = UUID()
        let filename = String(input.lastPathComponent.prefix(180))
        let relativePath = id.uuidString + ".pdf"
        let destination = rootURL.appendingPathComponent(relativePath, isDirectory: false)
        try FileManager.default.copyItem(at: input, to: destination)
        try? FileManager.default.setAttributes(
            [.protectionKey: FileProtectionType.complete],
            ofItemAtPath: destination.path
        )
        let item = DocumentItem(id: id, filename: filename, relativePath: relativePath,
                                isFavorite: false, lastReadPage: 0)
        documents.insert(item, at: 0)
        save()
        return item
    }


    /// Persist a complete new PDF as an app-local document. Failures do not add it to the library.
    @discardableResult
    func createPDF(_ pdf: PDFDocument, named name: String) throws -> DocumentItem {
        guard pdf.pageCount > 0 else { throw DocumentError.notPDF }
        let id = UUID()
        let path = id.uuidString + ".pdf"
        let destination = rootURL.appendingPathComponent(path)
        let temporary = rootURL.appendingPathComponent(UUID().uuidString + ".part")
        defer { try? FileManager.default.removeItem(at: temporary) }

        guard pdf.write(to: temporary) else { throw DocumentError.inaccessible }
        try FileManager.default.setAttributes(
            [.protectionKey: FileProtectionType.complete],
            ofItemAtPath: temporary.path
        )
        try FileManager.default.moveItem(at: temporary, to: destination)
        let safeName = String(name.prefix(175))
        let filename = Self.isPDFName(safeName) ? safeName : safeName + ".pdf"
        let item = DocumentItem(id: id, filename: filename, relativePath: path,
                                isFavorite: false, lastReadPage: 0)
        documents.insert(item, at: 0)
        save()
        return item
    }

    /// Rename only Folqio's display label. Files stay under stable UUID filenames.
    @discardableResult
    func renameLabel(_ item: DocumentItem, requestedName: String) -> Bool {
        let value = String(requestedName.trimmingCharacters(in: .whitespacesAndNewlines).prefix(160))
        guard !value.isEmpty, !value.contains("/"), !value.contains("\\"),
              !value.contains("\n"), !value.contains("\r"),
              let index = documents.firstIndex(where: { $0.id == item.id }) else { return false }
        documents[index].filename = Self.isPDFName(value) ? value : value + ".pdf"
        save()
        return true
    }

    func toggleFavorite(_ item: DocumentItem) {
        guard let index = documents.firstIndex(where: { $0.id == item.id }) else { return }
        documents[index].isFavorite.toggle()
        save()
    }

    func recordPage(_ page: Int, in item: DocumentItem) {
        guard let index = documents.firstIndex(where: { $0.id == item.id }) else { return }
        guard documents[index].lastReadPage != page else { return }
        documents[index].lastReadPage = page
        save()
    }

    @discardableResult
    func remove(_ item: DocumentItem) -> Bool {
        guard documents.contains(where: { $0.id == item.id }) else { return false }
        let url = localURL(for: item)
        // Never remove arbitrary paths from persisted metadata.
        guard item.relativePath == item.id.uuidString + ".pdf" else { return false }
        do {
            try FileManager.default.removeItem(at: url)
        } catch {
            // Keep the document in the library if deletion could not be completed.
            return false
        }
        documents.removeAll { $0.id == item.id }
        save()
        return true
    }

    private func load() {
        let url = rootURL.appendingPathComponent("library.json")
        guard let data = try? Data(contentsOf: url),
              let saved = try? JSONDecoder().decode([DocumentItem].self, from: data) else { return }
        documents = saved.filter { $0.relativePath == $0.id.uuidString + ".pdf" &&
            FileManager.default.fileExists(atPath: localURL(for: $0).path) }
    }

    private func save() {
        let url = rootURL.appendingPathComponent("library.json")
        guard let data = try? JSONEncoder().encode(documents) else { return }
        try? data.write(to: url, options: [.atomic, .completeFileProtection])
    }
}
