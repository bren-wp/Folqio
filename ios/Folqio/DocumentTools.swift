import Foundation
import PDFKit

enum PDFToolError: LocalizedError {
    case invalidPages
    case invalidDocument
    case encryptedAnnotationCopy

    var errorDescription: String? {
        switch self {
        case .invalidPages: return "Choose one or more valid pages."
        case .invalidDocument: return "The PDF cannot be processed."
        case .encryptedAnnotationCopy: return "Password-protected PDFs cannot yet be re-saved as annotated copies without changing their protection."
        }
    }
}

/// These operations preserve PDF pages and annotations instead of flattening them to images.
/// All edits are saved as a new file by DocumentLibrary, leaving originals untouched.
enum DocumentTools {
    /// Create an independent PDF copy for a new annotation. Never mutate the
    /// reader's PDFDocument or write over the original file.
    /// A fresh representation also keeps each edit isolated if persistence fails.
    static func annotatedCopy(_ source: PDFDocument, index: Int,
                              annotation: PDFAnnotation) throws -> PDFDocument {
        guard !source.isEncrypted else { throw PDFToolError.encryptedAnnotationCopy }
        guard !source.isLocked, index >= 0, index < source.pageCount,
              let representation = source.dataRepresentation(),
              let copy = PDFDocument(data: representation),
              let page = copy.page(at: index) else { throw PDFToolError.invalidDocument }
        page.addAnnotation(annotation)
        return copy
    }

    static func merge(_ documents: [PDFDocument]) throws -> PDFDocument {
        guard documents.count >= 2 else { throw PDFToolError.invalidDocument }
        let result = PDFDocument()
        for source in documents {
            guard source.pageCount > 0 else { throw PDFToolError.invalidDocument }
            for index in 0..<source.pageCount {
                guard let page = source.page(at: index) else { throw PDFToolError.invalidPages }
                result.insert(page, at: result.pageCount)
            }
        }
        return result
    }

    static func extract(_ source: PDFDocument, indices: [Int]) throws -> PDFDocument {
        guard !indices.isEmpty, Set(indices).count == indices.count,
              indices.allSatisfy({ $0 >= 0 && $0 < source.pageCount })
        else { throw PDFToolError.invalidPages }

        let result = PDFDocument()
        for index in indices {
            guard let page = source.page(at: index) else { throw PDFToolError.invalidPages }
            result.insert(page, at: result.pageCount)
        }
        return result
    }

    static func deletedCopy(_ source: PDFDocument, index: Int) throws -> PDFDocument {
        guard source.pageCount > 1, index >= 0, index < source.pageCount
        else { throw PDFToolError.invalidPages }
        source.removePage(at: index)
        return source
    }

    static func rotatedCopy(_ source: PDFDocument, index: Int) throws -> PDFDocument {
        guard let page = source.page(at: index) else { throw PDFToolError.invalidPages }
        page.rotation = (page.rotation + 90) % 360
        return source
    }

    static func movedCopy(_ source: PDFDocument, from: Int, to: Int) throws -> PDFDocument {
        guard from >= 0, from < source.pageCount, to >= 0, to < source.pageCount,
              let page = source.page(at: from)
        else { throw PDFToolError.invalidPages }
        source.removePage(at: from)
        source.insert(page, at: to)
        return source
    }
}
