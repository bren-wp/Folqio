import SwiftUI

struct FolqioSettingsView: View {
    @EnvironmentObject private var library: DocumentLibrary
    @Environment(\.dismiss) private var dismiss
    @AppStorage("folqio_language") private var language = "en"
    @AppStorage("folqio_reopen_last_page") private var reopenLastPage = true
    @AppStorage("folqio_confirm_deletion") private var confirmDeletion = true
    @State private var showExport = false
    @State private var legalPage: FolqioLegalKind?

    private func tr(_ key: String) -> String {
        FolqioLanguages.text(key, language: language)
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    HStack(spacing: 14) {
                        FolqioMark(size: 55)
                        VStack(alignment: .leading, spacing: 3) {
                            Text("Folqio").font(.title3.bold())
                            Text("Your documents. Your device.")
                                .font(.caption).foregroundStyle(FolqioColors.muted)
                        }
                    }.padding(.vertical, 5)
                }

                Section(tr("language")) {
                    Picker(tr("language"), selection: $language) {
                        ForEach(FolqioLanguages.supported.indices, id: \.self) { index in
                            Text(FolqioLanguages.supported[index].name)
                                .tag(FolqioLanguages.supported[index].code)
                        }
                    }
                    Text("Make Folqio feel like yours.")
                        .font(.caption).foregroundStyle(FolqioColors.muted)
                }

                Section("Reading & documents") {
                    Toggle(tr("reopen"), isOn: $reopenLastPage)
                    Toggle(tr("confirm"), isOn: $confirmDeletion)
                    Button { showExport = true } label: {
                        Label(tr("exportZip"), systemImage: "archivebox")
                    }
                    .disabled(library.documents.isEmpty)
                }

                Section("Your privacy") {
                    Label("No account or advertising", systemImage: "hand.raised")
                    Label("Documents stay under your control", systemImage: "lock.doc")
                }

                Section {
                    Button { legalPage = .privacy } label: {
                        Label(tr("privacy"), systemImage: "hand.raised")
                    }
                    Button { legalPage = .terms } label: {
                        Label(tr("terms"), systemImage: "doc.text")
                    }
                    HStack {
                        Text(tr("author"))
                        Spacer()
                        Link("Brendigo", destination: URL(string: "https://brendigo.com")!)
                            .foregroundStyle(FolqioColors.mint)
                    }
                    HStack {
                        Text("Folqio")
                        Spacer()
                        Text("© 2026 Brendigo").foregroundStyle(FolqioColors.muted)
                    }
                }
            }
            .scrollContentBackground(.hidden)
            .background(FolqioColors.midnight)
            .navigationTitle(tr("settings"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
            .sheet(isPresented: $showExport) {
                FolqioZipExportView().environmentObject(library)
            }
            .sheet(item: $legalPage) { kind in
                FolqioLegalView(kind: kind, language: language)
            }
        }
        .tint(FolqioColors.mint)
    }
}

private struct FolqioZipExportView: View {
    @EnvironmentObject private var library: DocumentLibrary
    @Environment(\.dismiss) private var dismiss
    @AppStorage("folqio_language") private var language = "en"
    @State private var selected = Set<UUID>()
    @State private var archiveURL: URL?
    @State private var isExporting = false
    @State private var problem: String?

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                Text("Choose the documents to include. The original PDFs stay unchanged.")
                    .font(.subheadline)
                    .foregroundStyle(FolqioColors.muted)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(16)
                List(library.documents) { item in
                    Button {
                        if !selected.insert(item.id).inserted {
                            selected.remove(item.id)
                        }
                        archiveURL = nil
                    } label: {
                        HStack {
                            Image(systemName: selected.contains(item.id) ? "checkmark.circle.fill" : "circle")
                                .foregroundStyle(FolqioColors.mint)
                            Text(item.filename).lineLimit(2)
                            Spacer(minLength: 4)
                        }
                    }
                    .foregroundStyle(FolqioColors.pearl)
                    .listRowBackground(FolqioColors.slate)
                }
                .scrollContentBackground(.hidden)
                VStack(spacing: 10) {
                    if isExporting { ProgressView("Preparing your archive…") }
                    if let problem {
                        Text(problem).font(.caption).foregroundStyle(.red)
                    }
                    if let archiveURL {
                        ShareLink(item: archiveURL) {
                            Label("Save or share ZIP", systemImage: "square.and.arrow.up")
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.borderedProminent)
                    } else {
                        Button(action: exportSelected) {
                            Label(FolqioLanguages.text("export", language: language),
                                  systemImage: "archivebox")
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.borderedProminent)
                        .disabled(selected.isEmpty || isExporting)
                    }
                }
                .padding(18)
            }
            .background(FolqioColors.midnight)
            .navigationTitle(FolqioLanguages.text("exportZip", language: language))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Done") { dismiss() }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Select all") {
                        selected = Set(library.documents.prefix(FolqioZipExporter.maxFiles).map(\.id))
                        archiveURL = nil
                    }
                }
            }
        }
        .tint(FolqioColors.mint)
        .onAppear { FolqioZipExporter.purgeExpiredExports() }
    }

    private func exportSelected() {
        let files = library.documents.filter { selected.contains($0.id) }
            .map { (url: library.localURL(for: $0), name: $0.filename) }
        guard !files.isEmpty else { return }
        isExporting = true
        problem = nil
        Task {
            do {
                let result = try await Task.detached(priority: .userInitiated) {
                    try FolqioZipExporter.export(files)
                }.value
                archiveURL = result
            } catch {
                problem = error.localizedDescription
            }
            isExporting = false
        }
    }
}
