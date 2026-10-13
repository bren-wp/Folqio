import SwiftUI
import PDFKit

struct UnlockDocumentSheet: View {
    let document: PDFDocument
    let onUnlocked: () -> Void
    let onCancel: () -> Void

    @State private var password = ""
    @State private var invalid = false

    var body: some View {
        NavigationStack {
            ZStack {
                FolqioColors.midnight.ignoresSafeArea()
                VStack(alignment: .leading, spacing: 18) {
                    Image(systemName: "lock.doc")
                        .font(.system(size: 48))
                        .foregroundStyle(FolqioColors.mint)
                    Text("Protected PDF")
                        .font(.title.bold())
                        .foregroundStyle(FolqioColors.pearl)
                    Text("Enter this PDF's password. It is used on this device and is not saved by Folqio.")
                        .foregroundStyle(FolqioColors.muted)
                    SecureField("PDF password", text: $password)
                        .textContentType(.password)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .padding(13)
                        .background(FolqioColors.slate, in: RoundedRectangle(cornerRadius: 12))
                        .foregroundStyle(FolqioColors.pearl)
                    if invalid {
                        Text("Incorrect password.")
                            .foregroundStyle(.red)
                    }
                    Button("Unlock") {
                        if document.unlock(withPassword: password) {
                            password = ""
                            onUnlocked()
                        } else {
                            invalid = true
                            password = ""
                        }
                    }
                    .buttonStyle(.borderedProminent)
                    .frame(maxWidth: .infinity)
                    .disabled(password.isEmpty)
                    Spacer()
                }
                .padding(24)
            }
            .navigationTitle("Unlock document")
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Cancel") {
                        password = ""
                        onCancel()
                    }
                }
            }
            .navigationBarTitleDisplayMode(.inline)
        }
        .tint(FolqioColors.mint)
        .interactiveDismissDisabled()
    }
}
