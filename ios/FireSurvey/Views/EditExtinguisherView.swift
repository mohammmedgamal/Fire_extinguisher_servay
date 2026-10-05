import SwiftData
import SwiftUI

struct EditExtinguisherView: View {
    let isNew: Bool
    private let originalCode: String

    @Environment(\.modelContext) private var context
    @Environment(\.dismiss) private var dismiss
    @Environment(Router.self) private var router
    @Query private var matches: [Extinguisher]

    @State private var code: String
    @State private var name = ""
    @State private var location = ""
    @State private var kind = extinguisherTypes[0]
    @State private var capacity = ""
    @State private var loaded = false
    @State private var error: String?
    @State private var confirmDelete = false

    init(code: String, isNew: Bool) {
        self.isNew = isNew
        self.originalCode = code
        _code = State(initialValue: code)
        _matches = Query(filter: #Predicate<Extinguisher> { $0.code == code })
    }

    private var existing: Extinguisher? { isNew ? nil : matches.first }

    private var canSave: Bool {
        !code.trimmingCharacters(in: .whitespaces).isEmpty && !name.trimmingCharacters(in: .whitespaces).isEmpty
    }

    var body: some View {
        Form {
            Section {
                TextField("QR code / ID", text: $code)
                    .textInputAutocapitalization(.characters)
                    .autocorrectionDisabled()
                    .disabled(!isNew)
            } header: {
                Text("QR code / ID *")
            } footer: {
                Text(isNew ? "Text encoded in the QR label, e.g. FE-TURB-001" : "The QR code cannot be changed")
            }

            Section("Details") {
                TextField("Name * (e.g. Turbine hall – extinguisher 3)", text: $name)
                TextField("Location (e.g. Unit 2, Level 0, near switchgear)", text: $location)
                Picker("Type", selection: $kind) {
                    ForEach(extinguisherTypes, id: \.self) { Text($0) }
                }
                TextField("Capacity (e.g. 6 kg)", text: $capacity)
            }

            if let error {
                Section { Text(error).foregroundStyle(Color.fireRed) }
            }

            Section {
                Button(action: save) { Text("Save").font(.headline).frame(maxWidth: .infinity) }
                    .buttonStyle(.borderedProminent)
                    .disabled(!canSave)
                    .listRowInsets(EdgeInsets())
                    .listRowBackground(Color.clear)
            } footer: {
                if isNew {
                    Text("After saving, open the extinguisher and tap the QR icon to share or print its label.")
                }
            }

            if existing != nil {
                Section {
                    Button("Delete extinguisher", role: .destructive) { confirmDelete = true }
                }
            }
        }
        .navigationTitle(isNew ? "Register extinguisher" : "Edit extinguisher")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear(perform: loadExisting)
        .confirmationDialog("Delete extinguisher?", isPresented: $confirmDelete, titleVisibility: .visible) {
            Button("Delete", role: .destructive, action: delete)
        } message: {
            Text("This also deletes its whole inspection history. This cannot be undone.")
        }
    }

    private func loadExisting() {
        guard !loaded, let e = existing else { return }
        name = e.name
        location = e.location
        kind = extinguisherTypes.contains(e.kind) ? e.kind : extinguisherTypes.last!
        capacity = e.capacity
        loaded = true
    }

    private func save() {
        let trimmedCode = code.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedName = name.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedLocation = location.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedCapacity = capacity.trimmingCharacters(in: .whitespacesAndNewlines)

        if let e = existing {
            e.name = trimmedName
            e.location = trimmedLocation
            e.kind = kind
            e.capacity = trimmedCapacity
        } else {
            // Codes are unique; SwiftData would silently overwrite a duplicate, so check first.
            let descriptor = FetchDescriptor<Extinguisher>(predicate: #Predicate { $0.code == trimmedCode })
            if ((try? context.fetchCount(descriptor)) ?? 0) > 0 {
                error = "An extinguisher with code \"\(trimmedCode)\" already exists"
                return
            }
            context.insert(Extinguisher(code: trimmedCode, name: trimmedName, location: trimmedLocation, kind: kind, capacity: trimmedCapacity))
        }

        do {
            try context.save()
        } catch {
            self.error = error.localizedDescription
            return
        }

        if isNew && originalCode != trimmedCode {
            // Registered from "+": replace this screen with the new extinguisher's page.
            router.path.removeLast()
            router.path.append(.detail(code: trimmedCode))
        } else {
            // Edited, or registered from an unknown scan: the previous page now shows the extinguisher.
            dismiss()
        }
    }

    private func delete() {
        guard let e = existing else { return }
        context.delete(e)
        try? context.save()
        router.popToRoot()
    }
}
