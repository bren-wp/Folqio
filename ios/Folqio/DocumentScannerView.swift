import SwiftUI
import VisionKit
import PDFKit

/// Uses Apple's device-local document camera UI for edge detection and cropping.
struct DocumentScannerView: UIViewControllerRepresentable {
    let onScan: (PDFDocument) -> Void
    let onCancel: () -> Void
    let onError: (Error) -> Void

    func makeCoordinator() -> Coordinator { Coordinator(self) }

    func makeUIViewController(context: Context) -> VNDocumentCameraViewController {
        let controller = VNDocumentCameraViewController()
        controller.delegate = context.coordinator
        return controller
    }

    func updateUIViewController(_ controller: VNDocumentCameraViewController, context: Context) {}

    final class Coordinator: NSObject, VNDocumentCameraViewControllerDelegate {
        let parent: DocumentScannerView

        init(_ parent: DocumentScannerView) { self.parent = parent }

        func documentCameraViewControllerDidCancel(_ controller: VNDocumentCameraViewController) {
            parent.onCancel()
        }

        func documentCameraViewController(_ controller: VNDocumentCameraViewController,
                                          didFailWithError error: Error) {
            parent.onError(error)
        }

        func documentCameraViewController(_ controller: VNDocumentCameraViewController,
                                          didFinishWith scan: VNDocumentCameraScan) {
            let document = PDFDocument()
            for index in 0..<scan.pageCount {
                guard let page = PDFPage(image: scan.imageOfPage(at: index)) else {
                    parent.onError(PDFToolError.invalidDocument)
                    return
                }
                document.insert(page, at: document.pageCount)
            }
            guard document.pageCount > 0 else {
                parent.onError(PDFToolError.invalidDocument)
                return
            }
            parent.onScan(document)
        }
    }
}
