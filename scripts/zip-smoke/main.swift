import Foundation

guard CommandLine.arguments.count == 2 else { fatalError("Provide working folder") }
let directory = URL(fileURLWithPath: CommandLine.arguments[1], isDirectory: true)
try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
let payloads: [(String, String)] = [
    ("Folder/Invoice.PDF", "%PDF-1.4\nfirst invoice\n"),
    ("invoice.pdf", "%PDF-1.4\nsecond invoice\n"),
    ("docs/Život.pdf", "%PDF-1.4\nthird invoice\n")
]
let inputs = try payloads.enumerated().map { index, entry -> (url: URL, name: String) in
    let url = directory.appendingPathComponent("input-\(index).pdf")
    try Data(entry.1.utf8).write(to: url)
    return (url: url, name: entry.0)
}
let archive = try FolqioZipExporter.export(inputs)
try archive.path.write(to: directory.appendingPathComponent("archive-path.txt"), atomically: true,
                       encoding: .utf8)
print("Wrote ZIP archive with \(inputs.count) real PDF payloads")
