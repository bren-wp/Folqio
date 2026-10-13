import SwiftUI
import PDFKit

struct DocumentThumbnail: View {
    let url: URL
    @State private var thumbnail: UIImage?

    var body: some View {
        Group {
            if let thumbnail {
                Image(uiImage: thumbnail)
                    .resizable()
                    .scaledToFit()
            } else {
                Image(systemName: "doc.text")
                    .resizable()
                    .scaledToFit()
                    .padding(10)
                    .foregroundStyle(FolqioColors.mint)
            }
        }
        .frame(width: 44, height: 60)
        .background(Color.white, in: RoundedRectangle(cornerRadius: 6))
        .clipShape(RoundedRectangle(cornerRadius: 6))
        .task(id: url) {
            thumbnail = PDFDocument(url: url)?.page(at: 0)?
                .thumbnail(of: CGSize(width: 90, height: 125), for: .mediaBox)
        }
        .accessibilityHidden(true)
    }
}
