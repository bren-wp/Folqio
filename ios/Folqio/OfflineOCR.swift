import Foundation
import PDFKit
import UIKit
import Vision

enum OCRFailure: LocalizedError {
    case unreadablePage
    case emptyText

    var errorDescription: String? {
        switch self {
        case .unreadablePage: return "This PDF page could not be converted into an image."
        case .emptyText: return "No recognizable text was found on this page."
        }
    }
}

/// A single-page, on-device Vision text recognition pass.
/// No document content or recognized text is transmitted or persisted.
enum OfflineOCR {
    static let maximumDocumentPages = 25

    static func canScanWholeDocument(_ pageCount: Int) -> Bool {
        pageCount in 1...maximumDocumentPages
    }

    static func recognize(_ page: PDFPage) async throws -> String {
        let preview = page.thumbnail(
            of: CGSize(width: 1700, height: 2200), for: .mediaBox
        )
        guard let image = preview.cgImage else { throw OCRFailure.unreadablePage }

        return try await withCheckedThrowingContinuation { continuation in
            DispatchQueue.global(qos: .userInitiated).async {
                do {
                    let request = VNRecognizeTextRequest()
                    request.recognitionLevel = .accurate
                    request.usesLanguageCorrection = true
                    request.automaticallyDetectsLanguage = true
                    let handler = VNImageRequestHandler(cgImage: image, options: [:])
                    try handler.perform([request])
                    let text = (request.results ?? [])
                        .compactMap { $0.topCandidates(1).first?.string }
                        .joined(separator: "\n")
                        .trimmingCharacters(in: .whitespacesAndNewlines)
                    guard !text.isEmpty else { throw OCRFailure.emptyText }
                    continuation.resume(returning: text)
                } catch {
                    continuation.resume(throwing: error)
                }
            }
        }
    }
}
