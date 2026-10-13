import AppKit
import CoreGraphics
import Foundation
import ImageIO
import UniformTypeIdentifiers

// Deterministic, offline rendering of branding/folqio-symbol.svg as an opaque
// 1024x1024 App Store icon. No CDN or generated placeholder asset.
func color(_ hex: UInt32) -> CGColor {
    let r = CGFloat((hex >> 16) & 0xFF) / 255
    let g = CGFloat((hex >> 8) & 0xFF) / 255
    let b = CGFloat(hex & 0xFF) / 255
    return CGColor(red: r, green: g, blue: b, alpha: 1)
}

let iconSize = 1024
let outputDirectory = URL(fileURLWithPath: #filePath)
    .deletingLastPathComponent()
    .appendingPathComponent("Folqio/Assets.xcassets/AppIcon.appiconset", isDirectory: true)
let output = outputDirectory.appendingPathComponent("AppIcon-1024.png")
try FileManager.default.createDirectory(at: outputDirectory, withIntermediateDirectories: true)

guard let context = CGContext(
    data: nil,
    width: iconSize,
    height: iconSize,
    bitsPerComponent: 8,
    bytesPerRow: 0,
    space: CGColorSpaceCreateDeviceRGB(),
    bitmapInfo: CGImageAlphaInfo.noneSkipLast.rawValue
) else {
    fatalError("Unable to allocate app-icon bitmap")
}

// Match the SVG's 512x512 top-left-origin coordinate system.
context.translateBy(x: 0, y: CGFloat(iconSize))
context.scaleBy(x: 2, y: -2)
context.setFillColor(color(0x0B1321))
context.fill(CGRect(x: 0, y: 0, width: 512, height: 512))

let sheet = CGMutablePath()
sheet.move(to: CGPoint(x: 160, y: 90))
sheet.addLine(to: CGPoint(x: 324, y: 90))
sheet.addLine(to: CGPoint(x: 412, y: 178))
sheet.addLine(to: CGPoint(x: 412, y: 405))
sheet.addQuadCurve(to: CGPoint(x: 388, y: 429), control: CGPoint(x: 412, y: 429))
sheet.addLine(to: CGPoint(x: 160, y: 429))
sheet.addQuadCurve(to: CGPoint(x: 135, y: 404), control: CGPoint(x: 135, y: 429))
sheet.addLine(to: CGPoint(x: 135, y: 115))
sheet.addQuadCurve(to: CGPoint(x: 160, y: 90), control: CGPoint(x: 135, y: 90))
sheet.closeSubpath()
context.setFillColor(color(0xF7FAFC))
context.addPath(sheet)
context.fillPath()

let fold = CGMutablePath()
fold.move(to: CGPoint(x: 324, y: 90))
fold.addLine(to: CGPoint(x: 324, y: 178))
fold.addLine(to: CGPoint(x: 412, y: 178))
fold.closeSubpath()
context.setFillColor(color(0x22D3A7))
context.addPath(fold)
context.fillPath()

let check = CGMutablePath()
check.move(to: CGPoint(x: 176, y: 334))
check.addLine(to: CGPoint(x: 238, y: 395))
check.addLine(to: CGPoint(x: 356, y: 260))
context.setStrokeColor(color(0x0A9D83))
context.setLineWidth(29)
context.setLineCap(.round)
context.setLineJoin(.round)
context.addPath(check)
context.strokePath()

guard let image = context.makeImage(),
      let destination = CGImageDestinationCreateWithURL(
        output as CFURL, UTType.png.identifier as CFString, 1, nil
      ) else {
    fatalError("Unable to create PNG icon")
}
CGImageDestinationAddImage(destination, image, nil)
guard CGImageDestinationFinalize(destination) else {
    fatalError("Unable to save PNG icon")
}
let brandDirectory = URL(fileURLWithPath: #filePath)
    .deletingLastPathComponent()
    .appendingPathComponent("Folqio/Assets.xcassets/FolqioMark.imageset", isDirectory: true)
try FileManager.default.createDirectory(at: brandDirectory, withIntermediateDirectories: true)
let brandOutput = brandDirectory.appendingPathComponent("folqio-mark.png")
if FileManager.default.fileExists(atPath: brandOutput.path) {
    try FileManager.default.removeItem(at: brandOutput)
}
try FileManager.default.copyItem(at: output, to: brandOutput)
print("Folqio app icon and shared brand mark created.")
