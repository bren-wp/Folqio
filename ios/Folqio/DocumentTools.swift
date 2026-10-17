import Foundation
import PDFKit

enum PDFToolError: LocalizedError {
    case invalidPages
    case invalidDocument
    case encryptedDocumentCopy

    var errorDescription: String? {
        switch self {
        case .invalidPages: return "Choose one or more valid pages."
        case .invalidDocument: return "The PDF cannot be processed."
        case .encryptedDocumentCopy:
            return "Password-protected PDFs cannot be re-saved as edited copies without changing their protection."
        }
    }
}

/// PDFKit document and page instances are mutable reference types.
/// Every operation works on a fresh in-memory copy so neither the original
/// file nor the caller's reader state can be changed by a failed edit.
enum DocumentTools {
    private static func editableCopy(_ source: PDFDocument) throws -> PDFDocument {
        guard !source.isEncrypted else { throw PDFToolError.encryptedDocumentCopy }
        guard !source.isLocked, source.pageCount > 0,
              let bytes = source.dataRepresentation(),
              let copy = PDFDocument(data: bytes), copy.pageCount == source.pageCount
        else { throw PDFToolError.invalidDocument }
        return copy
    }

    static func annotatedCopy(_ source: PDFDocument, index: Int,
                              annotation: PDFAnnotation) throws -> PDFDocument {
        guard index >= 0, index < source.pageCount else { throw PDFToolError.invalidPages }
        let result = try editableCopy(source)
        guard let page = result.page(at: index) else { throw PDFToolError.invalidPages }
        page.addAnnotation(annotation)
        return result
    }

    static func merge(_ documents: [PDFDocument]) throws -> PDFDocument {
        guard documents.count >= 2 else { throw PDFToolError.invalidDocument }
        let result = PDFDocument()
        for source in documents {
            let copy = try editableCopy(source)
            // Snapshot all page references before inserting any into another PDF.
            // PDFKit is permitted to transfer ownership of inserted PDFPage objects.
            let pages = (0..<copy.pageCount).compactMap { copy.page(at: $0) }
            guard pages.count == copy.pageCount else { throw PDFToolError.invalidPages }
            for page in pages { result.insert(page, at: result.pageCount) }
        }
        return result
    }

    static func extract(_ source: PDFDocument, indices: [Int]) throws -> PDFDocument {
        guard !indices.isEmpty, Set(indices).count == indices.count,
              indices.allSatisfy({ $0 >= 0 && $0 < source.pageCount })
        else { throw PDFToolError.invalidPages }
        let copy = try editableCopy(source)
        let pages = indices.compactMap { copy.page(at: $0) }
        guard pages.count == indices.count else { throw PDFToolError.invalidPages }
        let result = PDFDocument()
        for page in pages { result.insert(page, at: result.pageCount) }
        return result
    }

    static func deletedCopy(_ source: PDFDocument, index: Int) throws -> PDFDocument {
        guard source.pageCount > 1, index >= 0, index < source.pageCount
        else { throw PDFToolError.invalidPages }
        let result = try editableCopy(source)
        result.removePage(at: index)
        return result
    }

    static func rotatedCopy(_ source: PDFDocument, index: Int) throws -> PDFDocument {
        guard index >= 0, index < source.pageCount else { throw PDFToolError.invalidPages }
        let result = try editableCopy(source)
        guard let page = result.page(at: index) else { throw PDFToolError.invalidPages }
        page.rotation = (page.rotation + 90) % 360
        return result
    }

    /// Insert an A4-size blank page into an independent PDFKit copy. Existing
    /// source pages retain their PDF objects (including vector text and graphics).
    static func insertedBlankPageCopy(_ source: PDFDocument, after index: Int) throws -> PDFDocument {
        guard index >= 0, index < source.pageCount else { throw PDFToolError.invalidPages }
        let result = try editableCopy(source)
        let blank = PDFPage()
        blank.setBounds(CGRect(x: 0, y: 0, width: 595, height: 842), for: .mediaBox)
        result.insert(blank, at: index + 1)
        return result
    }

    static func movedCopy(_ source: PDFDocument, from: Int, to: Int) throws -> PDFDocument {
        guard from >= 0, from < source.pageCount, to >= 0, to < source.pageCount
        else { throw PDFToolError.invalidPages }
        let result = try editableCopy(source)
        guard let page = result.page(at: from) else { throw PDFToolError.invalidPages }
        result.removePage(at: from)
        result.insert(page, at: to)
        return result
    }
}
