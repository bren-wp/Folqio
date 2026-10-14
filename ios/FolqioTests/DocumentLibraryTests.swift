import XCTest
import PDFKit
import UIKit
@testable import Folqio

@MainActor
final class DocumentLibraryTests: XCTestCase {
    func testPDFExtensionValidation() {
        XCTAssertTrue(DocumentLibrary.isPDFName("Invoice.pdf"))
        XCTAssertTrue(DocumentLibrary.isPDFName("Invoice.PDF"))
        XCTAssertFalse(DocumentLibrary.isPDFName("Invoice.pdf.exe"))
    }

    func testRenameRejectsBadNames() {
        let root = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        defer { try? FileManager.default.removeItem(at: root) }
        let library = DocumentLibrary(rootURL: root)
        let unknown = DocumentItem(id: UUID(), filename: "Unknown.pdf",
                                   relativePath: UUID().uuidString + ".pdf",
                                   isFavorite: false, lastReadPage: 0)
        XCTAssertFalse(library.renameLabel(unknown, requestedName: "New.pdf"))
        XCTAssertFalse(library.renameLabel(unknown, requestedName: "../invalid"))
    }

    func testImportedPDFKeepsOriginalAndCommitsOneProtectedLocalCopy() throws {
        let base = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        try FileManager.default.createDirectory(at: base, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: base) }
        let source = base.appendingPathComponent("source.pdf")
        let pdf = PDFDocument()
        let picture = UIGraphicsImageRenderer(size: CGSize(width: 60, height: 80))
            .image { ctx in UIColor.white.setFill(); ctx.fill(CGRect(x: 0, y: 0, width: 60, height: 80)) }
        pdf.insert(try XCTUnwrap(PDFPage(image: picture)), at: 0)
        XCTAssertTrue(pdf.write(to: source))
        let original = try Data(contentsOf: source)
        let storage = base.appendingPathComponent("app")
        let library = DocumentLibrary(rootURL: storage)
        let imported = try library.importPDF(from: source)
        XCTAssertEqual(imported.filename, "source.pdf")
        XCTAssertNotEqual(library.localURL(for: imported), source)
        XCTAssertEqual(try Data(contentsOf: source), original)
        XCTAssertEqual(try Data(contentsOf: library.localURL(for: imported)), original)
        XCTAssertEqual(library.documents.count, 1)
        let leftover = try FileManager.default.contentsOfDirectory(atPath: storage.path)
        XCTAssertFalse(leftover.contains(where: { $0.hasSuffix(".part") }))
        XCTAssertTrue(library.remove(imported))
        XCTAssertEqual(try Data(contentsOf: source), original)
    }

    func testRejectsOversizedDocumentBeforeParsing() throws {
        let root = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        try FileManager.default.createDirectory(at: root, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: root) }
        let input = root.appendingPathComponent("oversized.pdf")
        try Data("%PDF-1.7".utf8).write(to: input)
        let handle = try FileHandle(forWritingTo: input)
        try handle.truncate(atOffset: 151 * 1024 * 1024)
        try handle.close()
        let library = DocumentLibrary(rootURL: root.appendingPathComponent("app"))
        XCTAssertThrowsError(try library.importPDF(from: input)) { error in
            guard case DocumentError.tooLarge = error else {
                return XCTFail("Expected size rejection before PDFKit parsing")
            }
        }
        XCTAssertTrue(library.documents.isEmpty)
    }

    func testNewLibraryIsEmpty() {
        let root = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        defer { try? FileManager.default.removeItem(at: root) }
        let library = DocumentLibrary(rootURL: root)
        XCTAssertTrue(library.documents.isEmpty)
    }

    func testRemovalPersistsAndDeletesOnlyOwnedFile() throws {
        let root = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        defer { try? FileManager.default.removeItem(at: root) }
        let library = DocumentLibrary(rootURL: root)
        let picture = UIGraphicsImageRenderer(size: CGSize(width: 80, height: 120))
            .image { context in UIColor.white.setFill(); context.fill(CGRect(x: 0, y: 0, width: 80, height: 120)) }
        let pdf = PDFDocument()
        let page = try XCTUnwrap(PDFPage(image: picture))
        pdf.insert(page, at: 0)
        let document = try library.createPDF(pdf, named: "Archive.pdf")
        XCTAssertTrue(FileManager.default.fileExists(atPath: library.localURL(for: document).path))
        XCTAssertTrue(library.remove(document))
        XCTAssertFalse(FileManager.default.fileExists(atPath: library.localURL(for: document).path))
        XCTAssertTrue(DocumentLibrary(rootURL: root).documents.isEmpty)
    }

    func testInterruptedRemovalRecoversDocumentOnNextLaunch() throws {
        let root = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        defer { try? FileManager.default.removeItem(at: root) }
        let library = DocumentLibrary(rootURL: root)
        let picture = UIGraphicsImageRenderer(size: CGSize(width: 80, height: 120))
            .image { context in UIColor.white.setFill(); context.fill(CGRect(x: 0, y: 0, width: 80, height: 120)) }
        let pdf = PDFDocument()
        pdf.insert(try XCTUnwrap(PDFPage(image: picture)), at: 0)
        let item = try library.createPDF(pdf, named: "Recovery.pdf")
        let pending = root.appendingPathComponent(item.id.uuidString + ".pending-delete")
        try FileManager.default.moveItem(at: library.localURL(for: item), to: pending)
        let restored = DocumentLibrary(rootURL: root)
        XCTAssertEqual(restored.documents.count, 1)
        XCTAssertTrue(FileManager.default.fileExists(atPath: restored.localURL(for: item).path))
        XCTAssertFalse(FileManager.default.fileExists(atPath: pending.path))
    }


}

@MainActor
final class DocumentToolsTests: XCTestCase {
    private func makePDF(pages: Int) -> PDFDocument {
        let document = PDFDocument()
        for index in 0..<pages {
            let renderer = UIGraphicsImageRenderer(size: CGSize(width: 100, height: 140))
            let image = renderer.image { context in
                UIColor.white.setFill()
                context.fill(CGRect(x: 0, y: 0, width: 100, height: 140))
                UIColor.black.setFill()
                ("Page " + String(index + 1)).draw(at: CGPoint(x: 8, y: 12),
                    withAttributes: [.font: UIFont.systemFont(ofSize: 16)])
            }
            if let page = PDFPage(image: image) {
                document.insert(page, at: document.pageCount)
            }
        }
        return document
    }

    func testMergePreservesAllPages() throws {
        let result = try DocumentTools.merge([makePDF(pages: 2), makePDF(pages: 3)])
        XCTAssertEqual(result.pageCount, 5)
    }

    func testExtractOnlyChosenPages() throws {
        let result = try DocumentTools.extract(makePDF(pages: 4), indices: [1, 3])
        XCTAssertEqual(result.pageCount, 2)
    }

    func testRejectsInvalidPage() {
        XCTAssertThrowsError(try DocumentTools.extract(makePDF(pages: 2), indices: [2]))
    }

    func testDeleteKeepsOriginalUnchanged() throws {
        let original = makePDF(pages: 3)
        let otherCopy = makePDF(pages: 3)
        let edited = try DocumentTools.deletedCopy(otherCopy, index: 1)
        XCTAssertEqual(edited.pageCount, 2)
        XCTAssertEqual(original.pageCount, 3)
        XCTAssertThrowsError(try DocumentTools.deletedCopy(makePDF(pages: 1), index: 0))
    }

    func testRotateAndReorder() throws {
        let pdf = makePDF(pages: 3)
        let rotated = try DocumentTools.rotatedCopy(pdf, index: 0)
        XCTAssertEqual(rotated.page(at: 0)?.rotation, 90)
        let moved = try DocumentTools.movedCopy(rotated, from: 0, to: 2)
        XCTAssertEqual(moved.pageCount, 3)
    }

    func testPageMutationsReturnCopiesAndKeepInputPagesUnchanged() throws {
        let source = makePDF(pages: 3)
        let sourcePages = (0..<source.pageCount).compactMap { source.page(at: $0) }
        let deleted = try DocumentTools.deletedCopy(source, index: 1)
        XCTAssertEqual(deleted.pageCount, 2)
        XCTAssertEqual(source.pageCount, 3)

        let rotated = try DocumentTools.rotatedCopy(source, index: 0)
        XCTAssertEqual(rotated.page(at: 0)?.rotation, 90)
        XCTAssertEqual(source.page(at: 0)?.rotation, 0)

        let moved = try DocumentTools.movedCopy(source, from: 0, to: 2)
        XCTAssertEqual(moved.pageCount, 3)
        XCTAssertEqual(source.pageCount, 3)
        for (index, originalPage) in sourcePages.enumerated() {
            XCTAssertTrue(source.page(at: index) === originalPage,
                          "Source page order changed at index \\(index)")
        }
    }

    func testMergeAndExtractRetainCallerPDFPages() throws {
        let first = makePDF(pages: 2)
        let second = makePDF(pages: 3)
        let firstPages = (0..<2).compactMap { first.page(at: $0) }
        let secondPages = (0..<3).compactMap { second.page(at: $0) }
        let merged = try DocumentTools.merge([first, second])
        XCTAssertEqual(merged.pageCount, 5)
        XCTAssertEqual(first.pageCount, 2)
        XCTAssertEqual(second.pageCount, 3)

        let extracted = try DocumentTools.extract(first, indices: [1])
        XCTAssertEqual(extracted.pageCount, 1)
        XCTAssertEqual(first.pageCount, 2)
        for (index, page) in firstPages.enumerated() {
            XCTAssertTrue(first.page(at: index) === page)
        }
        for (index, page) in secondPages.enumerated() {
            XCTAssertTrue(second.page(at: index) === page)
        }
    }

    func testAnnotationCreatesIndependentPDFWithoutChangingSource() throws {
        let original = makePDF(pages: 2)
        let annotation = PDFAnnotation(bounds: CGRect(x: 10, y: 20, width: 80, height: 35),
                                       forType: .freeText, withProperties: nil)
        annotation.contents = "Approved"
        let edited = try DocumentTools.annotatedCopy(original, index: 1, annotation: annotation)

        XCTAssertEqual(original.page(at: 1)?.annotations.count, 0)
        XCTAssertEqual(edited.page(at: 1)?.annotations.count, 1)
        XCTAssertEqual(edited.page(at: 1)?.annotations.first?.contents, "Approved")
        XCTAssertEqual(edited.pageCount, 2)
        XCTAssertThrowsError(try DocumentTools.annotatedCopy(original, index: 2,
                                                              annotation: annotation))
    }

    func testAnnotatedCopyPersistedSeparatelyWithOriginalBytesUnchanged() throws {
        let root = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        defer { try? FileManager.default.removeItem(at: root) }
        let library = DocumentLibrary(rootURL: root)
        let original = try library.createPDF(makePDF(pages: 1), named: "Original.pdf")
        let originalURL = library.localURL(for: original)
        let originalBytes = try Data(contentsOf: originalURL)
        let source = try XCTUnwrap(PDFDocument(url: originalURL))
        let annotation = PDFAnnotation(bounds: CGRect(x: 10, y: 10, width: 20, height: 20),
                                       forType: .text, withProperties: nil)
        annotation.contents = "Private note"

        let edited = try DocumentTools.annotatedCopy(source, index: 0, annotation: annotation)
        let saved = try library.createPDF(edited, named: "Note - Original.pdf")
        let savedPDF = try XCTUnwrap(PDFDocument(url: library.localURL(for: saved)))
        XCTAssertNotEqual(saved.id, original.id)
        XCTAssertEqual(try Data(contentsOf: originalURL), originalBytes)
        XCTAssertEqual(PDFDocument(url: originalURL)?.page(at: 0)?.annotations.count, 0)
        // PDFKit can persist a note together with its associated Popup annotation.
        // Verify the meaningful note count instead of rejecting the companion popup.
        let annotations = try XCTUnwrap(savedPDF.page(at: 0)?.annotations)
        let noteType = PDFAnnotationSubtype.text.rawValue
        let popupType = PDFAnnotationSubtype.popup.rawValue
        // PDFKit stores annotation.type as "Text", while subtype raw values may be "/Text".
        func normalized(_ value: String?) -> String? {
            value?.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        }
        let notes = annotations.filter { normalized($0.type) == normalized(noteType) }
        XCTAssertEqual(notes.count, 1, "Unexpected PDF annotations: \(annotations.map { $0.type ?? "nil" })")
        XCTAssertEqual(notes.first?.contents, "Private note")
        XCTAssertTrue(annotations.allSatisfy {
            normalized($0.type) == normalized(noteType) || normalized($0.type) == normalized(popupType)
        },
                      "Unexpected additional annotation types: \(annotations.map { $0.type ?? "nil" })")
    }
}

@MainActor
final class PhotoPDFBuilderTests: XCTestCase {
    func testPhotoCountLimits() {
        XCTAssertFalse(PhotoPDFBuilder.isValidPhotoCount(0))
        XCTAssertTrue(PhotoPDFBuilder.isValidPhotoCount(1))
        XCTAssertTrue(PhotoPDFBuilder.isValidPhotoCount(10))
        XCTAssertFalse(PhotoPDFBuilder.isValidPhotoCount(11))
    }

    func testPhotoBuilderAppendsPagesLocally() throws {
        let renderer = UIGraphicsImageRenderer(size: CGSize(width: 220, height: 320))
        let image = renderer.image { context in
            UIColor.white.setFill()
            context.fill(CGRect(x: 0, y: 0, width: 220, height: 320))
        }
        let pdf = PDFDocument()
        try PhotoPDFBuilder.appendImage(image, to: pdf)
        try PhotoPDFBuilder.appendImage(image, to: pdf)
        XCTAssertEqual(pdf.pageCount, 2)
    }
}

@MainActor
final class OfflineOCRPolicyTests: XCTestCase {
    func testWholeDocumentOCRLimit() {
        XCTAssertFalse(OfflineOCR.canScanWholeDocument(0))
        XCTAssertTrue(OfflineOCR.canScanWholeDocument(1))
        XCTAssertTrue(OfflineOCR.canScanWholeDocument(25))
        XCTAssertFalse(OfflineOCR.canScanWholeDocument(26))
    }
}

@MainActor
final class FolqioZipExporterTests: XCTestCase {
    func testStoredZipContainsOriginalDocuments() throws {
        let dir = FileManager.default.temporaryDirectory
        let first = dir.appendingPathComponent(UUID().uuidString + ".pdf")
        let second = dir.appendingPathComponent(UUID().uuidString + ".pdf")
        try Data("%PDF-first".utf8).write(to: first)
        try Data("%PDF-second".utf8).write(to: second)
        defer {
            try? FileManager.default.removeItem(at: first)
            try? FileManager.default.removeItem(at: second)
        }
        let output = try FolqioZipExporter.export([(url: first, name: "Document.pdf"),
                                                     (url: second, name: "Document.pdf")])
        defer { try? FileManager.default.removeItem(at: output) }
        let bytes = try Data(contentsOf: output)
        XCTAssertEqual(Array(bytes.prefix(4)), [0x50, 0x4b, 0x03, 0x04])
        XCTAssertTrue(bytes.range(of: Data("Document.pdf".utf8)) != nil)
        XCTAssertTrue(bytes.range(of: Data("Document (2).pdf".utf8)) != nil)
        XCTAssertEqual(Array(bytes.suffix(22).prefix(4)), [0x50, 0x4b, 0x05, 0x06])
    }

    func testSafeNamesCannotTraverseDirectories() {
        XCTAssertEqual(FolqioZipExporter.safeName("../report.pdf"), "report.pdf")
        XCTAssertEqual(FolqioZipExporter.safeName("report"), "report.pdf")
        XCTAssertEqual(FolqioZipExporter.safeName("Statement.PDF"), "Statement.PDF")
    }

    func testExportRejectsEmptySelection() {
        XCTAssertThrowsError(try FolqioZipExporter.export([]))
    }
}
