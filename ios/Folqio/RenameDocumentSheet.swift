import SwiftUI

struct RenameDocumentSheet: View {
    @EnvironmentObject private var library: DocumentLibrary
    @Environment(\.dismiss) private var dismiss

    let item: DocumentItem
    @State private var proposedName = ""
    @State private var invalid = false

    var body: some View {
        NavigationStack {
            Form {
                Section("Display name") {
                    TextField("Document.pdf", text: $proposedName)
                        .autocorrectionDisabled()
                        .textInputAutocapitalization(.never)
                }
                Section {
                    Text("This changes Folqio's library label, not the original imported file.")
                        .font(.footnote)
                        .foregroundStyle(FolqioColors.muted)
                }
            }
            .scrollContentBackground(.hidden)
            .background(FolqioColors.midnight)
            .navigationTitle("Rename document")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Save") {
                        if library.renameLabel(item, requestedName: proposedName) {
                            dismiss()
                        } else {
                            invalid = true
                        }
                    }
                    .fontWeight(.bold)
                }
            }
            .onAppear { proposedName = item.filename }
            .alert("Invalid name", isPresented: $invalid) {
                Button("OK", role: .cancel) {}
            } message: {
                Text("Enter a valid filename without slash characters.")
            }
        }
        .tint(FolqioColors.mint)
    }
}
