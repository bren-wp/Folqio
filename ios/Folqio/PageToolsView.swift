import SwiftUI
import PDFKit

struct PageToolsView: View {
    @EnvironmentObject private var library: DocumentLibrary
    @Environment(\.dismiss) private var dismiss

    let item: DocumentItem
    let onCreated: (DocumentItem) -> Void

    @State private var pageIndex = 0
    @State private var error: String?

    @State private var document: PDFDocument?
    private let columns = [GridItem(.flexible()), GridItem(.flexible())]

    var body: some View {
        NavigationStack {
            ZStack {
                FolqioColors.midnight.ignoresSafeArea()
                ScrollView {
                    VStack(alignment: .leading, spacing: 16) {
                        Text("Select a page, then choose an action. An edited PDF is saved separately; your original stays intact.")
                            .font(.subheadline)
                            .foregroundStyle(FolqioColors.muted)
                        if let document {
                            LazyVGrid(columns: columns, spacing: 14) {
                                ForEach(0..<document.pageCount, id: \.self) { index in
                                    Button {
                                        pageIndex = index
                                    } label: {
                                        VStack {
                                            if let page = document.page(at: index) {
                                                Image(uiImage: page.thumbnail(of: CGSize(width: 180, height: 250), for: .mediaBox))
                                                    .resizable()
                                                    .scaledToFit()
                                                    .frame(height: 164)
                                                    .background(.white)
                                            }
                                            Text("Page \(index + 1)")
                                                .font(.caption.weight(.medium))
                                                .foregroundStyle(pageIndex == index ? FolqioColors.mint : FolqioColors.pearl)
                                        }
                                        .padding(10)
                                        .frame(maxWidth: .infinity)
                                        .background(FolqioColors.slate, in: RoundedRectangle(cornerRadius: 16))
                                        .overlay(RoundedRectangle(cornerRadius: 16)
                                            .stroke(pageIndex == index ? FolqioColors.mint : .clear, lineWidth: 2))
                                    }
                                    .buttonStyle(.plain)
                                }
                            }
                            .padding(.vertical, 4)
                        }
                        actionButton("Rotate 90°", icon: "rotate.right") { document in
                            try DocumentTools.rotatedCopy(document, index: pageIndex)
                        }
                        actionButton("Extract selected page", icon: "square.on.square") { document in
                            try DocumentTools.extract(document, indices: [pageIndex])
                        }
                        actionButton("Move page earlier", icon: "arrow.up") { document in
                            try DocumentTools.movedCopy(document, from: pageIndex, to: max(0, pageIndex - 1))
                        }
                        actionButton("Move page later", icon: "arrow.down") { document in
                            try DocumentTools.movedCopy(document, from: pageIndex, to: min(document.pageCount - 1, pageIndex + 1))
                        }
                    }
                    .padding(20)
                }
            }
            .navigationTitle("Page tools")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
            .alert("Unable to edit", isPresented: Binding(get: { error != nil },
                                                          set: { if !$0 { error = nil } })) {
                Button("OK") { error = nil }
            } message: { Text(error ?? "") }
        }
        .tint(FolqioColors.mint)
        .onAppear { document = PDFDocument(url: library.localURL(for: item)) }
    }

    private func actionButton(_ title: String, icon: String,
                              edit: @escaping (PDFDocument) throws -> PDFDocument) -> some View {
        Button {
            do {
                guard let source = document else { throw PDFToolError.invalidDocument }
                let result = try edit(PDFDocument(url: library.localURL(for: item)) ?? source)
                let created = try library.createPDF(result, named: title + " - " + item.filename)
                onCreated(created)
                dismiss()
            } catch {
                self.error = error.localizedDescription
            }
        } label: {
            Label(title, systemImage: icon)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(14)
                .background(FolqioColors.slate, in: RoundedRectangle(cornerRadius: 14))
        }
        .disabled(document == nil)
    }
}
