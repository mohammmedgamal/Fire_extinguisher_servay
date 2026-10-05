import SwiftUI
import UIKit

/// Coloured pill showing the result of the last check (or "Never checked").
struct StatusBadge: View {
    let status: InspectionStatus?
    var overdue = false

    var body: some View {
        let (color, icon, text): (Color, String, String) = switch (status, overdue) {
        case (nil, _): (.gray, "questionmark.circle.fill", "Never checked")
        case (.notOK?, _): (.fireRed, "exclamationmark.octagon.fill", "Not OK")
        case (.ok?, true): (.dueAmber, "clock.fill", "OK · check due")
        case (.ok?, false): (.okGreen, "checkmark.circle.fill", "OK")
        }
        Label(text, systemImage: icon)
            .font(.caption.weight(.semibold))
            .padding(.horizontal, 10)
            .padding(.vertical, 4)
            .foregroundStyle(color)
            .background(color.opacity(0.12), in: Capsule())
            .fixedSize()
    }
}

struct LabeledValue: View {
    let label: String
    let value: String
    var color: Color = .primary

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(label).font(.caption).foregroundStyle(.secondary)
            Text(value.isEmpty ? "—" : value).foregroundStyle(color)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

/// UIKit share sheet, used for files generated on demand (CSV export).
struct ActivityView: UIViewControllerRepresentable {
    let items: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: items, applicationActivities: nil)
    }

    func updateUIViewController(_ controller: UIActivityViewController, context: Context) {}
}

/// Identifiable wrapper so a URL can drive `.sheet(item:)`.
struct ShareFile: Identifiable {
    let url: URL
    var id: URL { url }
}
