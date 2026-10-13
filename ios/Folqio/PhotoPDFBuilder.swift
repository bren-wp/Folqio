import Foundation
import PDFKit
import UIKit

enum PhotoPDFError: LocalizedError {
    case invalidSelection
    case unreadableImage

    var errorDescription: String? {
        switch self {
        case .invalidSelection: return "Select between one and ten photos."
        case .unreadableImage: return "One of the selected photos cannot be read."
        }
    }
}

enum PhotoPDFBuilder {
    static let maximumPhotos = 10
    static let maximumBytesPerPhoto = 25_000_000

    static func isValidPhotoCount(_ count: Int) -> Bool {
        (1...maximumPhotos).contains(count)
    }

    static func appendImage(_ image: UIImage, to document: PDFDocument) throws {
        let size = image.size
        guard size.width > 0, size.height > 0 else { throw PhotoPDFError.unreadableImage }
        let ratio = min(1, min(1600 / size.width, 2200 / size.height))
        let scaledSize = CGSize(width: max(1, floor(size.width * ratio)),
                                height: max(1, floor(size.height * ratio)))
        let renderer = UIGraphicsImageRenderer(size: scaledSize)
        let reduced = renderer.image { _ in
            image.draw(in: CGRect(origin: .zero, size: scaledSize))
        }
        guard let page = PDFPage(image: reduced) else { throw PhotoPDFError.unreadableImage }
        document.insert(page, at: document.pageCount)
    }
}
