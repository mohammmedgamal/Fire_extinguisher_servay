import Foundation

enum Formats {
    /// Extinguishers are expected to be checked at least this often.
    static let inspectionIntervalDays = 30

    private static let dateTimeFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd HH:mm"
        return f
    }()

    private static let dateFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd"
        return f
    }()

    static func dateTime(_ date: Date) -> String { dateTimeFormatter.string(from: date) }
    static func date(_ date: Date) -> String { dateFormatter.string(from: date) }

    static func daysAgo(_ date: Date, now: Date = .now) -> Int {
        Calendar.current.dateComponents([.day], from: date, to: now).day ?? 0
    }

    static func relative(_ date: Date) -> String {
        switch daysAgo(date) {
        case ..<1: "today"
        case 1: "yesterday"
        case let d: "\(d) days ago"
        }
    }

    static func isOverdue(_ last: Date?) -> Bool {
        guard let last else { return true }
        return daysAgo(last) >= inspectionIntervalDays
    }
}
