import SwiftUI
import PDFKit

struct MergeDocumentsView: View {
    @EnvironmentObject private var library: DocumentLibrary
    @Environment(\.dismiss) private var dismiss
    let onCreated: (DocumentItem) -> Void

    @State private var selected = Set<UUID>()
    @State private var error: String?

    var body: some View {
        NavigationStack {
            ZStack {
                FolqioColors.midnight.ignoresSafeArea()
                VStack(alignment: .leading, spacing: 12) {
                    Text("Select two or more documents. Their pages will be combined in the order shown, without altering the originals.")
                        .foregroundStyle(FolqioColors.muted)
                        .padding(.horizontal)
                    List(library.documents) { item in
                        Button {
                            if !selected.insert(item.id).inserted { selected.remove(item.id) }
                        } label: {
                            HStack {
                                Image(systemName: "doc.text").foregroundStyle(FolqioColors.mint)
                                Text(item.filename).foregroundStyle(FolqioColors.pearl)
                                Spacer()
                                Image(systemName: selected.contains(item.id) ? "checkmark.circle.fill" : "circle")
                                    .foregroundStyle(FolqioColors.mint)
                            }
                        }
                        .listRowBackground(FolqioColors.slate)
                    }
                    .scrollContentBackground(.hidden)
                    Button("Merge \(selected.count) documents", action: merge)
                        .frame(maxWidth: .infinity)
                        .buttonStyle(.borderedProminent)
                        .disabled(selected.count < 2)
                        .padding()
                }
            }
            .navigationTitle("Merge PDFs")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .topBarTrailing) { Button("Done") { dismiss() } } }
            .alert("Unable to merge", isPresented: Binding(get: { error != nil },
                                                           set: { if !$0 { error = nil } })) {
                Button("OK") { error = nil }
            } message: { Text(error ?? "") }
        }
        .tint(FolqioColors.mint)
    }

    private func merge() {
        do {
            let sources = library.documents.filter { selected.contains($0.id) }
            guard sources.count >= 2 else { throw PDFToolError.invalidDocument }
            let pdfs = try sources.map { item -> PDFDocument in
                guard let pdf = PDFDocument(url: library.localURL(for: item)) else {
                    throw PDFToolError.invalidDocument
                }
                return pdf
            }
            let output = try DocumentTools.merge(pdfs)
            let created = try library.createPDF(output, named: "Merged document.pdf")
            onCreated(created)
            dismiss()
        } catch {
            self.error = error.localizedDescription
        }
    }
}
