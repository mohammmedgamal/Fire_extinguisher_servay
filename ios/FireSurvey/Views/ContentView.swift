import SwiftData
import SwiftUI

private enum Filter: String, CaseIterable, Identifiable {
    case all = "All"
    case notOK = "Not OK"
    case due = "Due / never"
    var id: String { rawValue }
}

struct ContentView: View {
    @Environment(Router.self) private var router
    @Query(sort: \Extinguisher.name) private var extinguishers: [Extinguisher]
    @AppStorage("inspectorName") private var inspectorName = ""

    @State private var search = ""
    @State private var filter = Filter.all
    @State private var showScanner = false
    @State private var showNamePrompt = false
    @State private var nameDraft = ""
    @State private var exportFile: ShareFile?
    @State private var exportError: String?

    var body: some View {
        @Bindable var router = router
        NavigationStack(path: $router.path) {
            List {
                Section {
                    HStack(spacing: 8) {
                        StatTile(label: "Total", value: extinguishers.count)
                        StatTile(label: "Not OK", value: extinguishers.filter { $0.lastInspection?.status == .notOK }.count)
                        StatTile(label: "Due", value: extinguishers.filter {
                            $0.lastInspection?.status != .notOK && Formats.isOverdue($0.lastInspection?.timestamp)
                        }.count)
                    }
                    .listRowInsets(EdgeInsets())
                    .listRowBackground(Color.clear)

                    Picker("Filter", selection: $filter) {
                        ForEach(Filter.allCases) { Text($0.rawValue).tag($0) }
                    }
                    .pickerStyle(.segmented)
                    .listRowInsets(EdgeInsets())
                    .listRowBackground(Color.clear)
                }

                if extinguishers.isEmpty {
                    ContentUnavailableView(
                        "No extinguishers yet",
                        systemImage: "fire.extinguisher",
                        description: Text("Scan an extinguisher's QR code to register it, or tap + to add one and print its QR label.")
                    )
                } else if filtered.isEmpty {
                    ContentUnavailableView.search
                } else {
                    Section {
                        ForEach(filtered) { e in
                            NavigationLink(value: Route.detail(code: e.code)) { ExtinguisherRow(extinguisher: e) }
                        }
                    }
                }
            }
            .searchable(text: $search, prompt: "Search name, code or location")
            .navigationTitle("Extinguishers")
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button {
                        nameDraft = inspectorName
                        showNamePrompt = true
                    } label: {
                        Label(inspectorName.isEmpty ? "Set name" : inspectorName, systemImage: "person.crop.circle")
                            .labelStyle(.titleAndIcon)
                    }
                }
                ToolbarItemGroup(placement: .topBarTrailing) {
                    Button("Export CSV", systemImage: "square.and.arrow.up") { export() }
                    NavigationLink(value: Route.newExtinguisher(prefilledCode: "")) {
                        Label("Register extinguisher", systemImage: "plus")
                    }
                }
            }
            .safeAreaInset(edge: .bottom) {
                Button {
                    showScanner = true
                } label: {
                    Label("Scan QR code", systemImage: "qrcode.viewfinder")
                        .font(.title3.weight(.semibold))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 8)
                }
                .buttonStyle(.borderedProminent)
                .padding()
                .background(.bar)
            }
            .navigationDestination(for: Route.self) { route in
                switch route {
                case .detail(let code): DetailView(code: code)
                case .survey(let code): SurveyView(code: code)
                case .newExtinguisher(let code): EditExtinguisherView(code: code, isNew: true)
                case .editExtinguisher(let code): EditExtinguisherView(code: code, isNew: false)
                }
            }
        }
        .fullScreenCover(isPresented: $showScanner) {
            ScannerScreen { code in
                showScanner = false
                router.path.append(.detail(code: code))
            }
        }
        .alert("Operator / inspector name", isPresented: $showNamePrompt) {
            TextField("Your name or staff ID", text: $nameDraft)
            Button("Save") { inspectorName = nameDraft.trimmingCharacters(in: .whitespaces) }
            Button("Cancel", role: .cancel) {}
        }
        .sheet(item: $exportFile) { file in
            ActivityView(items: [file.url]).ignoresSafeArea()
        }
        .alert("Export failed", isPresented: Binding(get: { exportError != nil }, set: { if !$0 { exportError = nil } })) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(exportError ?? "")
        }
    }

    private var filtered: [Extinguisher] {
        let q = search.trimmingCharacters(in: .whitespaces)
        return extinguishers.filter { e in
            let matchesQuery = q.isEmpty || [e.code, e.name, e.location, e.kind].contains { $0.localizedCaseInsensitiveContains(q) }
            let last = e.lastInspection
            let matchesFilter = switch filter {
            case .all: true
            case .notOK: last?.status == .notOK
            case .due: Formats.isOverdue(last?.timestamp)
            }
            return matchesQuery && matchesFilter
        }
    }

    private func export() {
        do {
            exportFile = ShareFile(url: try CSVExporter.export(extinguishers: extinguishers))
        } catch {
            exportError = error.localizedDescription
        }
    }
}

private struct StatTile: View {
    let label: String
    let value: Int

    var body: some View {
        VStack {
            Text("\(value)").font(.title2.bold())
            Text(label).font(.caption)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 10)
        .background(Color(.secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 12))
    }
}

private struct ExtinguisherRow: View {
    let extinguisher: Extinguisher

    var body: some View {
        let last = extinguisher.lastInspection
        VStack(alignment: .leading, spacing: 4) {
            HStack(alignment: .firstTextBaseline) {
                VStack(alignment: .leading) {
                    Text(extinguisher.name).font(.headline).lineLimit(1)
                    Text([extinguisher.code, extinguisher.location].filter { !$0.isEmpty }.joined(separator: " · "))
                        .font(.caption).foregroundStyle(.secondary).lineLimit(1)
                }
                Spacer()
                StatusBadge(status: last?.status, overdue: Formats.isOverdue(last?.timestamp))
            }
            if let last {
                Text("Last check: \(Formats.date(last.timestamp)) (\(Formats.relative(last.timestamp)))").font(.caption)
                if last.status == .notOK, !last.issues.isEmpty {
                    Text(last.issues.map(\.title).joined(separator: ", "))
                        .font(.caption).foregroundStyle(Color.fireRed).lineLimit(2)
                }
            } else {
                Text("Last check: never").font(.caption)
            }
        }
        .padding(.vertical, 2)
    }
}
