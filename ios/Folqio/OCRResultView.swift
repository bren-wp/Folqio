import SwiftUI
import UIKit

struct OCRResult: Identifiable {
    let id = UUID()
    let pageNumber: Int
    let content: String
}

struct OCRResultView: View {
    @Environment(\.dismiss) private var dismiss
    let result: OCRResult
    @State private var copied = false

    var body: some View {
        NavigationStack {
            ZStack {
                FolqioColors.midnight.ignoresSafeArea()
                VStack(alignment: .leading, spacing: 12) {
                    Label("Recognized on this device", systemImage: "lock.shield")
                        .font(.subheadline)
                        .foregroundStyle(FolqioColors.mint)
                    Text(result.pageNumber == 0 ? "Document text" : "Page \(result.pageNumber)")
                        .font(.title2.bold())
                        .foregroundStyle(FolqioColors.pearl)
                    ScrollView {
                        Text(result.content)
                            .textSelection(.enabled)
                            .foregroundStyle(FolqioColors.pearl)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(18)
                    }
                    .background(FolqioColors.slate, in: RoundedRectangle(cornerRadius: 18))
                    Button {
                        // Clipboard export is an explicit user action.
                        UIPasteboard.general.string = result.content
                        copied = true
                    } label: {
                        Label(copied ? "Copied to clipboard" : "Copy recognized text",
                              systemImage: copied ? "checkmark" : "doc.on.doc")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                }
                .padding(18)
            }
            .navigationTitle("Offline OCR")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
        }
        .tint(FolqioColors.mint)
    }
}
