import XCTest
@testable import Folqio

@MainActor
final class DocumentLibraryTests: XCTestCase {
    func testPDFExtensionValidation() {
        XCTAssertTrue(DocumentLibrary.isPDFName("Invoice.pdf"))
        XCTAssertTrue(DocumentLibrary.isPDFName("Invoice.PDF"))
        XCTAssertFalse(DocumentLibrary.isPDFName("Invoice.pdf.exe"))
    }

    func testNewLibraryIsEmpty() {
        let root = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        defer { try? FileManager.default.removeItem(at: root) }
        let library = DocumentLibrary(rootURL: root)
        XCTAssertTrue(library.documents.isEmpty)
    }
}
