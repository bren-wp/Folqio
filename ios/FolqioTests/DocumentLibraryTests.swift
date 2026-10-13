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
