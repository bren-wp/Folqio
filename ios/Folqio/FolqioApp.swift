import SwiftUI

@main
struct FolqioApp: App {
    @StateObject private var library = DocumentLibrary()

    var body: some Scene {
        WindowGroup {
            HomeView()
                .environmentObject(library)
                .preferredColorScheme(.dark)
        }
    }
}
