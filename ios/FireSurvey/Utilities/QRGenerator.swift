import CoreImage.CIFilterBuiltins
import UIKit

enum QRGenerator {
    private static let context = CIContext()

    static func qrImage(for content: String, size: CGFloat = 600) -> UIImage {
        let filter = CIFilter.qrCodeGenerator()
        filter.message = Data(content.utf8)
        // High error correction: labels in a plant get dirty and scratched.
        filter.correctionLevel = "H"
        guard let output = filter.outputImage else { return UIImage() }
        let scale = size / output.extent.width
        let scaled = output.transformed(by: CGAffineTransform(scaleX: scale, y: scale))
        guard let cg = context.createCGImage(scaled, from: scaled.extent) else { return UIImage() }
        return UIImage(cgImage: cg)
    }

    /// A printable label: QR code with the extinguisher name, code and location underneath.
    static func labelImage(code: String, name: String, location: String) -> UIImage {
        let qrSize: CGFloat = 600
        let padding: CGFloat = 40
        let lines: [(String, UIFont)] = [
            (name, .boldSystemFont(ofSize: 44)),
            (code, .systemFont(ofSize: 36)),
            (location, .systemFont(ofSize: 32)),
        ].filter { !$0.0.trimmingCharacters(in: .whitespaces).isEmpty }
        let textHeight = lines.reduce(0) { $0 + $1.1.pointSize * 1.4 }
        let width = qrSize + padding * 2
        let size = CGSize(width: width, height: qrSize + padding * 2 + textHeight)

        let format = UIGraphicsImageRendererFormat()
        format.scale = 1
        return UIGraphicsImageRenderer(size: size, format: format).image { ctx in
            UIColor.white.setFill()
            ctx.fill(CGRect(origin: .zero, size: size))
            let qr = qrImage(for: code, size: qrSize)
            // Nearest-neighbour keeps the QR modules sharp.
            ctx.cgContext.interpolationQuality = .none
            qr.draw(in: CGRect(x: padding, y: padding, width: qrSize, height: qrSize))

            let paragraph = NSMutableParagraphStyle()
            paragraph.alignment = .center
            paragraph.lineBreakMode = .byTruncatingTail
            var y = padding + qrSize
            for (text, font) in lines {
                let h = font.pointSize * 1.4
                let attrs: [NSAttributedString.Key: Any] = [.font: font, .foregroundColor: UIColor.black, .paragraphStyle: paragraph]
                text.draw(in: CGRect(x: padding, y: y, width: width - padding * 2, height: h), withAttributes: attrs)
                y += h
            }
        }
    }
}
