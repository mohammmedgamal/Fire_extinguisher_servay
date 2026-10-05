import Foundation

enum CSVExporter {
    /// Writes all inspections to a CSV file in the temporary directory and returns its URL.
    static func export(extinguishers: [Extinguisher]) throws -> URL {
        var rows = ["Date,Extinguisher code,Name,Location,Type,Inspector,Result,Conditions,Notes"]
        let all = extinguishers
            .flatMap { e in e.inspections.map { (e, $0) } }
            .sorted { $0.1.timestamp > $1.1.timestamp }
        for (e, i) in all {
            rows.append([
                Formats.dateTime(i.timestamp),
                e.code,
                e.name,
                e.location,
                e.kind,
                i.inspector,
                i.status.label,
                i.issues.map(\.title).joined(separator: "; "),
                i.notes,
            ].map(field).joined(separator: ","))
        }
        let url = FileManager.default.temporaryDirectory
            .appendingPathComponent("extinguisher_survey_\(Formats.date(.now)).csv")
        // BOM so Excel opens UTF-8 (e.g. Arabic names) correctly
        try ("\u{FEFF}" + rows.joined(separator: "\n") + "\n").write(to: url, atomically: true, encoding: .utf8)
        return url
    }

    private static func field(_ value: String) -> String {
        if value.contains(where: { $0 == "," || $0 == "\"" || $0 == "\n" || $0 == "\r" }) {
            return "\"" + value.replacingOccurrences(of: "\"", with: "\"\"") + "\""
        }
        return value
    }
}
