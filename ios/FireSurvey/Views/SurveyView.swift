import SwiftData
import SwiftUI

struct SurveyView: View {
    let code: String
    @Query private var matches: [Extinguisher]
    @Environment(\.modelContext) private var context
    @Environment(\.dismiss) private var dismiss
    @AppStorage("inspectorName") private var savedInspector = ""

    @State private var status: InspectionStatus?
    @State private var issues = Set<Issue>()
    @State private var notes = ""
    @State private var inspector = ""
    @State private var saveError: String?

    init(code: String) {
        self.code = code
        _matches = Query(filter: #Predicate<Extinguisher> { $0.code == code })
    }

    private var needsNotes: Bool {
        status == .notOK && issues.contains(.other) && notes.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    private var canSave: Bool {
        guard matches.first != nil, !inspector.trimmingCharacters(in: .whitespaces).isEmpty else { return false }
        switch status {
        case .ok?: return true
        case .notOK?: return !issues.isEmpty && !needsNotes
        case nil: return false
        }
    }

    var body: some View {
        Form {
            if let e = matches.first {
                Section {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(e.name).font(.title3.bold())
                        Text([e.code, e.location].filter { !$0.isEmpty }.joined(separator: " · "))
                            .font(.subheadline).foregroundStyle(.secondary)
                        if let last = e.lastInspection {
                            let issueText = last.issues.isEmpty ? "" : " (\(last.issues.map(\.title).joined(separator: ", ")))"
                            Text("Last check: \(Formats.dateTime(last.timestamp)) – \(last.status.label)\(issueText)").font(.caption)
                        } else {
                            Text("Last check: never").font(.caption)
                        }
                    }
                }
            }

            Section("Condition of the extinguisher") {
                HStack(spacing: 12) {
                    StatusChoice(title: "OK", icon: "checkmark.circle.fill", color: .okGreen, selected: status == .ok) { status = .ok }
                    StatusChoice(title: "NOT OK", icon: "xmark.octagon.fill", color: .fireRed, selected: status == .notOK) { status = .notOK }
                }
                .listRowInsets(EdgeInsets(top: 8, leading: 8, bottom: 8, trailing: 8))
            }

            if status == .notOK {
                Section("What is wrong? (select all that apply)") {
                    ForEach(Issue.allCases) { issue in
                        Button {
                            if issues.contains(issue) { issues.remove(issue) } else { issues.insert(issue) }
                        } label: {
                            HStack {
                                Image(systemName: issues.contains(issue) ? "checkmark.square.fill" : "square")
                                    .foregroundStyle(issues.contains(issue) ? Color.fireRed : .secondary)
                                    .font(.title3)
                                Text(issue.title).foregroundStyle(.primary)
                            }
                        }
                    }
                }
            }

            Section {
                TextField(needsNotes || (status == .notOK && issues.contains(.other)) ? "Notes (required for \"Other\")" : "Notes (optional)",
                          text: $notes, axis: .vertical)
                    .lineLimit(3...6)
                TextField("Inspected by (name or staff ID)", text: $inspector)
                    .textContentType(.name)
            } footer: {
                if inspector.trimmingCharacters(in: .whitespaces).isEmpty {
                    Text("Enter your name or staff ID").foregroundStyle(Color.fireRed)
                }
            }

            Section {
                Button(action: save) {
                    Text("Save survey").font(.headline).frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .disabled(!canSave)
                .listRowInsets(EdgeInsets())
                .listRowBackground(Color.clear)
            }
        }
        .navigationTitle("Survey")
        .navigationBarTitleDisplayMode(.inline)
        .animation(.default, value: status)
        .onAppear { if inspector.isEmpty { inspector = savedInspector } }
        .alert("Could not save", isPresented: Binding(get: { saveError != nil }, set: { if !$0 { saveError = nil } })) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(saveError ?? "")
        }
    }

    private func save() {
        guard let e = matches.first, let status else { return }
        let name = inspector.trimmingCharacters(in: .whitespaces)
        let inspection = Inspection(
            inspector: name,
            status: status,
            issues: issues,
            notes: notes.trimmingCharacters(in: .whitespacesAndNewlines)
        )
        context.insert(inspection)
        inspection.extinguisher = e
        do {
            try context.save()
            savedInspector = name
            UINotificationFeedbackGenerator().notificationOccurred(.success)
            dismiss()
        } catch {
            saveError = error.localizedDescription
        }
    }
}

private struct StatusChoice: View {
    let title: String
    let icon: String
    let color: Color
    let selected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Label(title, systemImage: icon)
                .font(.headline)
                .frame(maxWidth: .infinity, minHeight: 56)
                .foregroundStyle(selected ? .white : color)
                .background(selected ? color : .clear, in: RoundedRectangle(cornerRadius: 12))
                .overlay(RoundedRectangle(cornerRadius: 12).stroke(color, lineWidth: 2))
        }
        .buttonStyle(.plain)
    }
}
