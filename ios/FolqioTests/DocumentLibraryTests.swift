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
