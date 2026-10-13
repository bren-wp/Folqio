import Foundation

enum ZipExportError: LocalizedError {
    case invalidSelection
    case archiveTooLarge
    case unreadableFile

    var errorDescription: String? {
        switch self {
        case .invalidSelection: return "Choose between 1 and 100 PDF documents."
        case .archiveTooLarge: return "Choose fewer documents. ZIP exports are limited to 300 MB."
        case .unreadableFile: return "One of the documents could not be read."
        }
    }
}

/// ZIP32 (stored, without compression) for genuine, lossless PDF export.
/// Stream file data rather than loading an entire document library into RAM.
enum FolqioZipExporter {
    static let maxFiles = 100
    static let maxBytes: UInt64 = 300 * 1024 * 1024
    private static let exportPrefix = "Folqio-"

    /// Remove only Folqio temporary ZIPs from previous sessions, never user exports.
    /// A grace period protects files handed off through asynchronous share sheets.
    static func purgeExpiredExports(olderThan age: TimeInterval = 24 * 60 * 60,
                                   now: Date = Date()) {
        let folder = FileManager.default.temporaryDirectory
        guard let entries = try? FileManager.default.contentsOfDirectory(
            at: folder, includingPropertiesForKeys: [.contentModificationDateKey, .isRegularFileKey]
        ) else { return }
        for url in entries where url.lastPathComponent.hasPrefix(exportPrefix)
                              && url.pathExtension.lowercased() == "zip" {
            guard let values = try? url.resourceValues(forKeys: [.contentModificationDateKey, .isRegularFileKey]),
                  values.isRegularFile == true,
                  let changed = values.contentModificationDate,
                  now.timeIntervalSince(changed) > age else { continue }
            try? FileManager.default.removeItem(at: url)
        }
    }

    static func safeName(_ input: String) -> String {
        let leaf = input.replacingOccurrences(of: "\\", with: "/")
            .split(separator: "/").last.map(String.init) ?? "Document.pdf"
        let filtered = String(leaf.unicodeScalars.map {
            CharacterSet.letters.contains($0) || CharacterSet.decimalDigits.contains($0) ||
            " ._-()".unicodeScalars.contains($0) ? Character($0) : "_"
        }).trimmingCharacters(in: CharacterSet(charactersIn: " .")).prefix(100)
        let name = filtered.isEmpty ? "Document" : String(filtered)
        return name.lowercased().hasSuffix(".pdf") ? name : name + ".pdf"
    }

    private static func append16(_ value: UInt16, to data: inout Data) {
        data.append(UInt8(truncatingIfNeeded: value))
        data.append(UInt8(truncatingIfNeeded: value >> 8))
    }
    private static func append32(_ value: UInt32, to data: inout Data) {
        append16(UInt16(truncatingIfNeeded: value), to: &data)
        append16(UInt16(truncatingIfNeeded: value >> 16), to: &data)
    }

    private static let crcTable: [UInt32] = (0..<256).map { number in
        (0..<8).reduce(UInt32(number)) { value, _ in
            (value & 1 == 1) ? (value >> 1) ^ 0xEDB88320 : value >> 1
        }
    }
    private static func crc(_ bytes: Data, previous: UInt32) -> UInt32 {
        bytes.reduce(previous) { result, byte in
            crcTable[Int((result ^ UInt32(byte)) & 0xff)] ^ (result >> 8)
        }
    }

    private struct Entry {
        let filename: Data
        let crc: UInt32
        let size: UInt32
        let offset: UInt32
    }

    static func export(_ files: [(url: URL, name: String)]) throws -> URL {
        guard (1...maxFiles).contains(files.count) else { throw ZipExportError.invalidSelection }
        let destination = FileManager.default.temporaryDirectory
            .appendingPathComponent("Folqio-\(UUID().uuidString).zip")
        guard FileManager.default.createFile(atPath: destination.path, contents: nil) else {
            throw ZipExportError.unreadableFile
        }
        #if os(iOS)
        do {
            try FileManager.default.setAttributes(
                [.protectionKey: FileProtectionType.completeUnlessOpen],
                ofItemAtPath: destination.path
            )
        } catch {
            try? FileManager.default.removeItem(at: destination)
            throw error
        }
        #endif
        let archive = try FileHandle(forWritingTo: destination)
        var succeeded = false
        defer {
            try? archive.close()
            if !succeeded { try? FileManager.default.removeItem(at: destination) }
        }
        var entries: [Entry] = []
        var used = Set<String>()
        var total: UInt64 = 0

        for (url, displayName) in files {
            try Task.checkCancellation()
            var filename = safeName(displayName)
            let root = String(filename.dropLast(4))
            var index = 2
            while !used.insert(filename.lowercased()).inserted {
                filename = "\(root.prefix(85)) (\(index)).pdf"
                index += 1
            }
            let filenameData = Data(filename.utf8)
            guard filenameData.count <= Int(UInt16.max) else { throw ZipExportError.invalidSelection }
            let source = try FileHandle(forReadingFrom: url)
            defer { try? source.close() }
            let offset = archive.offsetInFile
            guard offset <= UInt32.max else { throw ZipExportError.archiveTooLarge }
            var local = Data()
            append32(0x04034b50, to: &local)
            append16(20, to: &local)
            append16(0x0808, to: &local) // UTF-8 filenames and trailing data descriptor
            append16(0, to: &local)      // Store PDFs without alteration
            append16(0, to: &local)
            append16(0, to: &local)
            append32(0, to: &local)
            append32(0, to: &local)
            append32(0, to: &local)
            append16(UInt16(filenameData.count), to: &local)
            append16(0, to: &local)
            local.append(filenameData)
            try archive.write(contentsOf: local)

            var check: UInt32 = 0xffffffff
            var length: UInt64 = 0
            while true {
                try Task.checkCancellation()
                let block = source.readData(ofLength: 65536)
                if block.isEmpty { break }
                total += UInt64(block.count)
                length += UInt64(block.count)
                guard total <= maxBytes, length <= UInt32.max else {
                    throw ZipExportError.archiveTooLarge
                }
                check = crc(block, previous: check)
                try archive.write(contentsOf: block)
            }
            let checksum = check ^ 0xffffffff
            var descriptor = Data()
            append32(0x08074b50, to: &descriptor)
            append32(checksum, to: &descriptor)
            append32(UInt32(length), to: &descriptor)
            append32(UInt32(length), to: &descriptor)
            try archive.write(contentsOf: descriptor)
            entries.append(Entry(filename: filenameData, crc: checksum,
                                 size: UInt32(length), offset: UInt32(offset)))
        }

        let directoryOffset = archive.offsetInFile
        for entry in entries {
            var central = Data()
            append32(0x02014b50, to: &central)
            append16(20, to: &central)
            append16(20, to: &central)
            append16(0x0808, to: &central)
            append16(0, to: &central)
            append16(0, to: &central)
            append16(0, to: &central)
            append32(entry.crc, to: &central)
            append32(entry.size, to: &central)
            append32(entry.size, to: &central)
            append16(UInt16(entry.filename.count), to: &central)
            append16(0, to: &central)
            append16(0, to: &central)
            append16(0, to: &central)
            append16(0, to: &central)
            append32(0, to: &central)
            append32(entry.offset, to: &central)
            central.append(entry.filename)
            try archive.write(contentsOf: central)
        }
        let directoryLength = archive.offsetInFile - directoryOffset
        guard directoryOffset <= UInt32.max, directoryLength <= UInt32.max else {
            throw ZipExportError.archiveTooLarge
        }
        var end = Data()
        append32(0x06054b50, to: &end)
        append16(0, to: &end)
        append16(0, to: &end)
        append16(UInt16(entries.count), to: &end)
        append16(UInt16(entries.count), to: &end)
        append32(UInt32(directoryLength), to: &end)
        append32(UInt32(directoryOffset), to: &end)
        append16(0, to: &end)
        try archive.write(contentsOf: end)
        try archive.synchronize()
        succeeded = true
        return destination
    }
}
