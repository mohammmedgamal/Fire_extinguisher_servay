import AVFoundation
import SwiftUI
import UIKit

/// Full-screen QR scanner with torch toggle and manual code entry.
struct ScannerScreen: View {
    let onCode: (String) -> Void

    @Environment(\.dismiss) private var dismiss
    @Environment(\.openURL) private var openURL
    @State private var authorization = AVCaptureDevice.authorizationStatus(for: .video)
    @State private var torchOn = false
    @State private var showManual = false
    @State private var manualCode = ""
    @State private var handled = false

    var body: some View {
        NavigationStack {
            ZStack {
                Color.black.ignoresSafeArea()
                if authorization == .authorized {
                    QRScannerView(torchOn: torchOn) { deliver($0) }
                        .ignoresSafeArea()
                    RoundedRectangle(cornerRadius: 16)
                        .stroke(.white, lineWidth: 3)
                        .frame(width: 260, height: 260)
                    VStack {
                        Spacer()
                        Text("Point the camera at the QR label on the extinguisher")
                            .multilineTextAlignment(.center)
                            .foregroundStyle(.white)
                            .padding()
                            .frame(maxWidth: .infinity)
                            .background(.black.opacity(0.5))
                    }
                } else if authorization == .notDetermined {
                    ProgressView().tint(.white)
                } else {
                    VStack(spacing: 16) {
                        Text("Camera access is needed to scan QR codes.")
                            .foregroundStyle(.white)
                            .multilineTextAlignment(.center)
                        Button("Open Settings") {
                            if let url = URL(string: UIApplication.openSettingsURLString) { openURL(url) }
                        }
                        .buttonStyle(.borderedProminent)
                        Button("Enter code manually") { showManual = true }
                            .buttonStyle(.bordered)
                    }
                    .padding()
                }
            }
            .navigationTitle("Scan QR")
            .navigationBarTitleDisplayMode(.inline)
            .toolbarBackground(.visible, for: .navigationBar)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } }
                ToolbarItemGroup(placement: .primaryAction) {
                    if authorization == .authorized {
                        Button("Flashlight", systemImage: torchOn ? "flashlight.on.fill" : "flashlight.off.fill") { torchOn.toggle() }
                    }
                    Button("Enter code", systemImage: "keyboard") { showManual = true }
                }
            }
            .alert("Enter extinguisher code", isPresented: $showManual) {
                TextField("Code printed under the QR", text: $manualCode)
                    .textInputAutocapitalization(.characters)
                    .autocorrectionDisabled()
                Button("Open") { deliver(manualCode) }
                Button("Cancel", role: .cancel) {}
            }
        }
        .task {
            if authorization == .notDetermined {
                let granted = await AVCaptureDevice.requestAccess(for: .video)
                authorization = granted ? .authorized : .denied
            }
        }
    }

    /// A QR code stays in view for many frames; only act on the first one.
    private func deliver(_ raw: String) {
        let code = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !handled, !code.isEmpty else { return }
        handled = true
        onCode(code)
    }
}

/// Camera preview that reports decoded QR code strings.
struct QRScannerView: UIViewControllerRepresentable {
    let torchOn: Bool
    let onCode: (String) -> Void

    func makeUIViewController(context: Context) -> ScannerViewController {
        let vc = ScannerViewController()
        vc.onCode = onCode
        return vc
    }

    func updateUIViewController(_ vc: ScannerViewController, context: Context) {
        vc.onCode = onCode
        vc.setTorch(torchOn)
    }
}

final class ScannerViewController: UIViewController, AVCaptureMetadataOutputObjectsDelegate {
    var onCode: ((String) -> Void)?

    private let session = AVCaptureSession()
    private let sessionQueue = DispatchQueue(label: "scanner.session")
    private var previewLayer: AVCaptureVideoPreviewLayer?
    private var device: AVCaptureDevice?
    private var found = false

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .black

        guard let device = AVCaptureDevice.default(for: .video),
              let input = try? AVCaptureDeviceInput(device: device),
              session.canAddInput(input) else { return }
        self.device = device
        session.addInput(input)

        let output = AVCaptureMetadataOutput()
        guard session.canAddOutput(output) else { return }
        session.addOutput(output)
        output.setMetadataObjectsDelegate(self, queue: .main)
        output.metadataObjectTypes = [.qr]

        let layer = AVCaptureVideoPreviewLayer(session: session)
        layer.videoGravity = .resizeAspectFill
        view.layer.addSublayer(layer)
        previewLayer = layer
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        previewLayer?.frame = view.bounds
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        let session = session
        sessionQueue.async { if !session.isRunning { session.startRunning() } }
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        let session = session
        sessionQueue.async { if session.isRunning { session.stopRunning() } }
    }

    func setTorch(_ on: Bool) {
        guard let device, device.hasTorch, device.isTorchAvailable else { return }
        guard (device.torchMode == .on) != on else { return }
        do {
            try device.lockForConfiguration()
            device.torchMode = on ? .on : .off
            device.unlockForConfiguration()
        } catch {}
    }

    func metadataOutput(_ output: AVCaptureMetadataOutput, didOutput metadataObjects: [AVMetadataObject], from connection: AVCaptureConnection) {
        guard let value = metadataObjects
            .compactMap({ ($0 as? AVMetadataMachineReadableCodeObject)?.stringValue })
            .first(where: { !$0.isEmpty }), !found else { return }
        found = true
        UINotificationFeedbackGenerator().notificationOccurred(.success)
        onCode?(value)
    }
}
