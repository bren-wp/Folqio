import SwiftUI

enum FolqioColors {
    static let midnight = Color(red: 11.0 / 255, green: 19.0 / 255, blue: 33.0 / 255)
    static let slate = Color(red: 26.0 / 255, green: 36.0 / 255, blue: 51.0 / 255)
    static let mint = Color(red: 34.0 / 255, green: 211.0 / 255, blue: 167.0 / 255)
    static let pearl = Color(red: 247.0 / 255, green: 250.0 / 255, blue: 252.0 / 255)
    static let muted = Color(red: 169.0 / 255, green: 185.0 / 255, blue: 201.0 / 255)
}

struct FolqioMark: View {
    var size: CGFloat = 58

    var body: some View {
        Image("FolqioMark")
            .resizable()
            .interpolation(.high)
            .scaledToFit()
            .frame(width: size, height: size)
            .clipShape(RoundedRectangle(cornerRadius: size * 0.23))
            .accessibilityLabel("Folqio")
    }
}

struct FolqioCard<Content: View>: View {
    @ViewBuilder var content: () -> Content

    var body: some View {
        content()
            .padding(18)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(FolqioColors.slate, in: RoundedRectangle(cornerRadius: 20))
    }
}
