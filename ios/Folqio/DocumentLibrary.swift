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
    case tooLarge

    var errorDescription: String? {
        switch self {
        case .notPDF: return "The selected file is not a readable PDF."
        case .inaccessible: return "The document is unavailable."
        case .tooLarge: return "Choose a PDF smaller than 150 MB."
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

        // Reject excessive input before PDFKit parses or copies it.
        let attributes = try FileManager.default.attributesOfItem(atPath: input.path)
        guard (attributes[.size] as? NSNumber)?.int64Value ?? 0 <= 150 * 1024 * 1024 else {
            throw DocumentError.tooLarge
        }
        guard FileManager.default.isReadableFile(atPath: input.path),
              PDFDocument(url: input) != nil
        else { throw DocumentError.notPDF }

        let id = UUID()
        let filename = String(input.lastPathComponent.prefix(180))
        let relativePath = id.uuidString + ".pdf"
        let destination = rootURL.appendingPathComponent(relativePath, isDirectory: false)
        try FileManager.default.copyItem(at: input, to: destination)
        do {
            try FileManager.default.setAttributes(
                [.protectionKey: FileProtectionType.complete],
                ofItemAtPath: destination.path
            )
        } catch {
            try? FileManager.default.removeItem(at: destination)
            throw DocumentError.inaccessible
        }
        let item = DocumentItem(id: id, filename: filename, relativePath: relativePath,
                                isFavorite: false, lastReadPage: 0)
        documents.insert(item, at: 0)
        guard save() else {
            documents.removeAll { $0.id == id }
            try? FileManager.default.removeItem(at: destination)
            throw DocumentError.inaccessible
        }
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
        guard save() else {
            documents.removeAll { $0.id == id }
            try? FileManager.default.removeItem(at: destination)
            throw DocumentError.inaccessible
        }
        return item
    }

    /// Rename only Folqio's display label. Files stay under stable UUID filenames.
    @discardableResult
    func renameLabel(_ item: DocumentItem, requestedName: String) -> Bool {
        let value = String(requestedName.trimmingCharacters(in: .whitespacesAndNewlines).prefix(160))
        guard !value.isEmpty, !value.contains("/"), !value.contains("\\"),
              !value.contains("\n"), !value.contains("\r"),
              let index = documents.firstIndex(where: { $0.id == item.id }) else { return false }
        let previous = documents[index].filename
        documents[index].filename = Self.isPDFName(value) ? value : value + ".pdf"
        guard save() else {
            documents[index].filename = previous
            return false
        }
        return true
    }

    func toggleFavorite(_ item: DocumentItem) {
        guard let index = documents.firstIndex(where: { $0.id == item.id }) else { return }
        documents[index].isFavorite.toggle()
        if !save() { documents[index].isFavorite.toggle() }
    }

    func recordPage(_ page: Int, in item: DocumentItem) {
        guard let index = documents.firstIndex(where: { $0.id == item.id }) else { return }
        guard documents[index].lastReadPage != page else { return }
        let previous = documents[index].lastReadPage
        documents[index].lastReadPage = page
        if !save() { documents[index].lastReadPage = previous }
    }

    @discardableResult
    func remove(_ item: DocumentItem) -> Bool {
        guard documents.contains(where: { $0.id == item.id }),
              item.relativePath == item.id.uuidString + ".pdf" else { return false }

        // Stage deletion with a same-directory rename. A failed metadata write can
        // restore the file and the document record without losing its original bytes.
        let original = localURL(for: item)
        let pending = rootURL.appendingPathComponent(item.id.uuidString + ".pending-delete")
        do {
            try FileManager.default.moveItem(at: original, to: pending)
        } catch {
            return false
        }
        let previous = documents
        documents.removeAll { $0.id == item.id }
        guard save() else {
            documents = previous
            try? FileManager.default.moveItem(at: pending, to: original)
            return false
        }
        // After metadata is durable, cleanup is best effort; the next launch can
        // collect orphaned pending-delete files if the process was interrupted.
        try? FileManager.default.removeItem(at: pending)
        return true
    }

    private func load() {
        let url = rootURL.appendingPathComponent("library.json")
        guard let data = try? Data(contentsOf: url),
              let saved = try? JSONDecoder().decode([DocumentItem].self, from: data) else { return }
        let valid = saved.filter { $0.relativePath == $0.id.uuidString + ".pdf" }
        for item in valid {
            let documentURL = localURL(for: item)
            let pendingURL = rootURL.appendingPathComponent(item.id.uuidString + ".pending-delete")
            if !FileManager.default.fileExists(atPath: documentURL.path),
               FileManager.default.fileExists(atPath: pendingURL.path) {
                try? FileManager.default.moveItem(at: pendingURL, to: documentURL)
            }
        }
        documents = valid.filter {
            FileManager.default.fileExists(atPath: localURL(for: $0).path)
        }

        // Remove only our validated UUID-named tombstones that have no library entry.
        let referenced = Set(valid.map { $0.id.uuidString })
        if let names = try? FileManager.default.contentsOfDirectory(atPath: rootURL.path) {
            for name in names where name.hasSuffix(".pending-delete") {
                let prefix = String(name.dropLast(".pending-delete".count))
                guard UUID(uuidString: prefix) != nil,
                      !referenced.contains(prefix) else { continue }
                try? FileManager.default.removeItem(
                    at: rootURL.appendingPathComponent(name)
                )
            }
        }
    }

    @discardableResult
    private func save() -> Bool {
        let url = rootURL.appendingPathComponent("library.json")
        do {
            let data = try JSONEncoder().encode(documents)
            try data.write(to: url, options: [.atomic, .completeFileProtection])
            return true
        } catch {
            return false
        }
    }
}
