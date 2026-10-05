import SwiftData
import SwiftUI

struct DetailView: View {
    let code: String
    @Query private var matches: [Extinguisher]
    @State private var showLabel = false

    init(code: String) {
        self.code = code
        _matches = Query(filter: #Predicate<Extinguisher> { $0.code == code })
    }

    var body: some View {
        if let e = matches.first {
            content(e)
        } else {
            ContentUnavailableView {
                Label("Unknown extinguisher", systemImage: "exclamationmark.triangle.fill")
            } description: {
                Text("No extinguisher is registered with code:\n\"\(code)\"")
            } actions: {
                NavigationLink("Register this extinguisher", value: Route.newExtinguisher(prefilledCode: code))
                    .buttonStyle(.borderedProminent)
            }
            .navigationTitle("Extinguisher")
        }
    }

    private func content(_ e: Extinguisher) -> some View {
        let history = e.sortedInspections
        let last = history.first
        return List {
            Section {
                VStack(alignment: .leading, spacing: 10) {
                    Text(e.name).font(.title2.bold())
                    HStack(alignment: .top) {
                        LabeledValue(label: "Code", value: e.code)
                        LabeledValue(label: "Location", value: e.location)
                    }
                    HStack(alignment: .top) {
                        LabeledValue(label: "Type", value: e.kind)
                        LabeledValue(label: "Capacity", value: e.capacity)
                    }
                }
                .padding(.vertical, 4)
            }

            Section {
                LastCheckView(last: last)
            } header: {
                HStack {
                    Text("Last check")
                    Spacer()
                    StatusBadge(status: last?.status, overdue: Formats.isOverdue(last?.timestamp))
                }
            }
            .listRowBackground(lastCheckBackground(last))

            Section {
                NavigationLink(value: Route.survey(code: e.code)) {
                    Label("Start survey", systemImage: "checklist")
                        .font(.headline)
                        .foregroundStyle(Color.fireRed)
                }
            }

            if history.count > 1 {
                Section("Inspection history") {
                    ForEach(history.dropFirst()) { HistoryRow(inspection: $0) }
                }
            }
        }
        .navigationTitle(e.name)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            Button("QR label", systemImage: "qrcode") { showLabel = true }
            NavigationLink(value: Route.editExtinguisher(code: e.code)) { Label("Edit", systemImage: "pencil") }
        }
        .sheet(isPresented: $showLabel) { QRLabelSheet(extinguisher: e) }
    }

    private func lastCheckBackground(_ last: Inspection?) -> Color {
        switch last?.status {
        case .ok?: Color.okGreen.opacity(0.10)
        case .notOK?: Color.fireRed.opacity(0.10)
        case nil: Color(.secondarySystemGroupedBackground)
        }
    }
}

private struct LastCheckView: View {
    let last: Inspection?

    var body: some View {
        if let last {
            VStack(alignment: .leading, spacing: 10) {
                HStack(alignment: .top) {
                    LabeledValue(label: "Date", value: "\(Formats.dateTime(last.timestamp))\n(\(Formats.relative(last.timestamp)))")
                    LabeledValue(label: "Checked by", value: last.inspector)
                }
                LabeledValue(
                    label: "Condition",
                    value: last.status == .ok ? "OK – no defects found" : last.issues.map { "• \($0.title)" }.joined(separator: "\n"),
                    color: last.status == .ok ? .okGreen : .fireRed
                )
                if !last.notes.isEmpty { LabeledValue(label: "Notes", value: last.notes) }
                if Formats.isOverdue(last.timestamp) {
                    Text("Monthly check is due (last check over \(Formats.inspectionIntervalDays) days ago).")
                        .font(.caption).foregroundStyle(Color.fireRed)
                }
            }
            .padding(.vertical, 4)
        } else {
            Text("This extinguisher has never been surveyed.")
        }
    }
}

private struct HistoryRow: View {
    let inspection: Inspection

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                VStack(alignment: .leading) {
                    Text(Formats.dateTime(inspection.timestamp)).font(.subheadline.weight(.medium))
                    Text(inspection.inspector.isEmpty ? "—" : inspection.inspector).font(.caption).foregroundStyle(.secondary)
                }
                Spacer()
                StatusBadge(status: inspection.status)
            }
            if !inspection.issues.isEmpty {
                Text(inspection.issues.map(\.title).joined(separator: ", ")).font(.caption).foregroundStyle(Color.fireRed)
            }
            if !inspection.notes.isEmpty { Text(inspection.notes).font(.caption) }
        }
    }
}

private struct QRLabelSheet: View {
    let extinguisher: Extinguisher
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        let label = QRGenerator.labelImage(code: extinguisher.code, name: extinguisher.name, location: extinguisher.location)
        NavigationStack {
            VStack(spacing: 20) {
                Image(uiImage: label)
                    .resizable()
                    .interpolation(.none)
                    .scaledToFit()
                    .frame(maxWidth: 300)
                    .border(Color.secondary.opacity(0.3))
                ShareLink(
                    item: Image(uiImage: label),
                    preview: SharePreview("QR label – \(extinguisher.name)", image: Image(uiImage: label))
                ) {
                    Label("Share / print label", systemImage: "printer")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
            }
            .padding()
            .navigationTitle("QR label")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { Button("Done") { dismiss() } }
        }
        .presentationDetents([.large])
    }
}
