import SwiftData
import SwiftUI

@main
struct FireSurveyApp: App {
    @State private var router = Router()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environment(router)
                .tint(.fireRed)
        }
        .modelContainer(for: [Extinguisher.self, Inspection.self])
    }
}

enum Route: Hashable {
    case detail(code: String)
    case survey(code: String)
    case newExtinguisher(prefilledCode: String)
    case editExtinguisher(code: String)
}

/// Navigation state shared by all screens so any screen can push or pop to root.
@Observable
final class Router {
    var path: [Route] = []

    func popToRoot() { path.removeAll() }
}

extension Color {
    static let fireRed = Color(red: 0.78, green: 0.16, blue: 0.16)
    static let okGreen = Color(red: 0.18, green: 0.49, blue: 0.20)
    static let dueAmber = Color(red: 0.94, green: 0.42, blue: 0.0)
}
