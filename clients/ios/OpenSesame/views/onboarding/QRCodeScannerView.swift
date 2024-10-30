import SwiftUI
import AVFoundation

struct QRCodeScannerView: UIViewControllerRepresentable {
    
    class Coordinator: NSObject, AVCaptureMetadataOutputObjectsDelegate {
        var parent: QRCodeScannerView

        init(parent: QRCodeScannerView) {
            self.parent = parent
        }
        
        func metadataOutput(_ output: AVCaptureMetadataOutput, didOutput metadataObjects: [AVMetadataObject], from connection: AVCaptureConnection) {
            if let metadataObject = metadataObjects.first {
                guard let readableObject = metadataObject as? AVMetadataMachineReadableCodeObject else { return }
                guard let stringValue = readableObject.stringValue else { return }
                AudioServicesPlaySystemSound(SystemSoundID(kSystemSoundID_Vibrate))
                parent.didFindCode(stringValue)
            }
        }
    }

    var didFindCode: (String) -> Void

    func makeCoordinator() -> Coordinator {
        return Coordinator(parent: self)
    }

    func makeUIViewController(context: Context) -> UIViewController {
        let viewController = UIViewController()
        let captureSession = AVCaptureSession()

        guard let videoCaptureDevice = AVCaptureDevice.default(for: .video) else { return viewController }
        let videoInput: AVCaptureDeviceInput

        do {
            videoInput = try AVCaptureDeviceInput(device: videoCaptureDevice)
        } catch {
            return viewController
        }

        if (captureSession.canAddInput(videoInput)) {
            captureSession.addInput(videoInput)
        } else {
            return viewController
        }

        let metadataOutput = AVCaptureMetadataOutput()

        if (captureSession.canAddOutput(metadataOutput)) {
            captureSession.addOutput(metadataOutput)

            metadataOutput.setMetadataObjectsDelegate(context.coordinator, queue: DispatchQueue.main)
            metadataOutput.metadataObjectTypes = [.qr]
        } else {
            return viewController
        }

        let previewLayer = AVCaptureVideoPreviewLayer(session: captureSession)
        previewLayer.frame = viewController.view.layer.bounds
        previewLayer.videoGravity = .resizeAspectFill
        viewController.view.layer.addSublayer(previewLayer)

        captureSession.startRunning()

        return viewController
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct QRCodeScannerOverlay: View {
    @Binding var isCameraPresented: Bool
    var body: some View {
        VStack {
            Spacer()
            // Rectangle overlay to show where to point the camera
            Rectangle()
                .stroke(Color.blue, lineWidth: 3)
                .frame(width: 250, height: 250)
                .cornerRadius(12)
                .opacity(0.5)
                .overlay(
                    Rectangle()
                        .foregroundColor(Color.black.opacity(0.2))
                        .cornerRadius(12)
                )
            Text("Point to the QR code in your web app.")
                .padding(10)
                .background(Color.white.opacity(0.7))
                .foregroundColor(.black)
                .cornerRadius(8)
                .multilineTextAlignment(.center)
                .frame(maxWidth: 300)
            Button("Close") {
                self.isCameraPresented = false
            }
            .fontWeight(.bold)
            .foregroundColor(.black)
            .padding()
            .frame(width: 250)
            Spacer()
        }
    }
}

struct QRCodeScannerContainer: View {
    @Binding var isCameraPresented: Bool
    var didFindCode: (String) -> Void
    
    var body: some View {
        NavigationView {
            ZStack {
                QRCodeScannerView(didFindCode: { scannedCode in
                    self.didFindCode(scannedCode) // Pass the scanned code back to the parent view
                    self.isCameraPresented = false
                })
                QRCodeScannerOverlay(isCameraPresented: self.$isCameraPresented) // Overlay with instructions and square
            }
        }
    }
}

#Preview {
    QRCodeScannerContainer(isCameraPresented: .constant(true)) { scannedCode in }
}
