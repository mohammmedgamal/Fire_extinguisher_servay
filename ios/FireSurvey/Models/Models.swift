import Foundation
import SwiftData

/// A physical extinguisher. `code` is the exact text encoded in its QR label.
@Model
final class Extinguisher {
    @Attribute(.unique) var code: String
    var name: String
    var location: String
    var kind: String
    var capacity: String
    var createdAt: Date

    @Relationship(deleteRule: .cascade, inverse: \Inspection.extinguisher)
    var inspections: [Inspection] = []

    init(code: String, name: String, location: String, kind: String, capacity: String, createdAt: Date = .now) {
        self.code = code
        self.name = name
        self.location = location
        self.kind = kind
        self.capacity = capacity
        self.createdAt = createdAt
    }

    /// Newest first.
    var sortedInspections: [Inspection] { inspections.sorted { $0.timestamp > $1.timestamp } }
    var lastInspection: Inspection? { inspections.max { $0.timestamp < $1.timestamp } }
}

/// One survey of an extinguisher by an operator.
@Model
final class Inspection {
    var timestamp: Date
    var inspector: String
    /// `InspectionStatus.rawValue`
    var statusRaw: String
    /// Comma-separated `Issue.rawValue`s; empty when status is OK.
    var issuesRaw: String
    var notes: String
    var extinguisher: Extinguisher?

    init(timestamp: Date = .now, inspector: String, status: InspectionStatus, issues: Set<Issue>, notes: String) {
        self.timestamp = timestamp
        self.inspector = inspector
        self.statusRaw = status.rawValue
        self.issuesRaw = status == .ok ? "" : Issue.join(issues)
        self.notes = notes
    }

    var status: InspectionStatus { InspectionStatus(rawValue: statusRaw) ?? .notOK }
    var issues: [Issue] { Issue.parse(issuesRaw) }
}

enum InspectionStatus: String, CaseIterable {
    case ok = "OK"
    case notOK = "NOT_OK"

    var label: String { self == .ok ? "OK" : "Not OK" }
}

/// Conditions an operator can report when an extinguisher is not OK.
enum Issue: String, CaseIterable, Identifiable {
    case corroded = "CORRODED"
    case damaged = "DAMAGED"
    case needsCasing = "NEEDS_CASING"
    case lowPressure = "LOW_PRESSURE"
    case pinSeal = "PIN_SEAL"
    case hoseNozzle = "HOSE_NOZZLE"
    case label = "LABEL"
    case obstructed = "OBSTRUCTED"
    case needsRefill = "NEEDS_REFILL"
    case serviceDue = "SERVICE_DUE"
    case mounting = "MOUNTING"
    case missing = "MISSING"
    case other = "OTHER"

    var id: String { rawValue }

    var title: String {
        switch self {
        case .corroded: "Corroded / rusted"
        case .damaged: "Physically damaged / dented"
        case .needsCasing: "Needs casing / cabinet"
        case .lowPressure: "Low pressure (gauge not in green)"
        case .pinSeal: "Safety pin or tamper seal missing/broken"
        case .hoseNozzle: "Hose or nozzle damaged / blocked"
        case .label: "Label / instructions missing or unreadable"
        case .obstructed: "Access obstructed / not visible"
        case .needsRefill: "Used or needs refill / recharge"
        case .serviceDue: "Service / hydrostatic test overdue"
        case .mounting: "Bracket / mounting damaged"
        case .missing: "Extinguisher missing from location"
        case .other: "Other (describe in notes)"
        }
    }

    static func parse(_ raw: String) -> [Issue] {
        raw.split(separator: ",").compactMap { Issue(rawValue: $0.trimmingCharacters(in: .whitespaces)) }
    }

    static func join(_ issues: Set<Issue>) -> String {
        allCases.filter(issues.contains).map(\.rawValue).joined(separator: ",")
    }
}

let extinguisherTypes = [
    "Dry chemical powder (ABC)",
    "CO2",
    "Foam (AFFF)",
    "Water",
    "Wet chemical",
    "Clean agent (FM-200 / Halotron)",
    "Other",
]
