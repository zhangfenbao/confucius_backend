import SwiftUI

public enum AuthMethod: String, CaseIterable, Identifiable {
    case qrcode = "QR Code"
    case apiKey = "API Key"
    case login = "Login"
    public var id: String { self.rawValue }
}

struct OnboardingServerSettings: View {
    
    @Binding var selectedAuthMethod: AuthMethod
    @Binding var openSesameURL: String
    @Binding var openSesameSecret: String
    @Binding var openSesameUsername: String
    @Binding var openSesamePassword: String
    
    @State private var isCameraPresented: Bool = false
    @State private var scannedCode: String = ""
    
    func handleNewQrcode(newScannedCode: String) {
        if self.selectedAuthMethod == .apiKey {
            return
        }
        Logger.shared.info("Scanned QR Code: \(newScannedCode)")
        self.scannedCode = newScannedCode
        guard let qrCodeInfo = try? JSONDecoder().decode(QrcodeInfo.self, from: Data(newScannedCode.utf8)) else {
            Logger.shared.warn("Failed to parse qrcode!")
            return
        }
        self.openSesameURL = qrCodeInfo.baseUrl
        self.openSesameSecret = qrCodeInfo.token
        self.isCameraPresented = false
        self.selectedAuthMethod = .apiKey
    }
    
    var body: some View {
        VStack {
            Text("Sign in")
                .font(.title2)
                .fontWeight(.semibold)
                .frame(maxWidth: .infinity, alignment: .center)
                .padding()
            
            // Picker for selecting the authentication method
            Picker("Authentication Method", selection: $selectedAuthMethod) {
                ForEach(AuthMethod.allCases) { method in
                    Text(method.rawValue).tag(method)
                }
            }
            .pickerStyle(SegmentedPickerStyle())
            .padding(.horizontal)
            
            Form {
                Section() {
                    VStack(alignment: .center, spacing: 20) {
                        switch selectedAuthMethod {
                        case .apiKey:
                            HStack {
                                Text("Base URL:").frame(minWidth: 100, alignment: .leading)
                                TextField("Backend URL", text: self.$openSesameURL)
                                    .keyboardType(.URL)
                            }
                            Divider()
                                .background(Color.gray)
                            HStack {
                                Text("API Key:").frame(minWidth: 100, alignment: .leading)
                                SecureFieldWithPlaceholder(placeholder: "OpenSesame Secret", text: self.$openSesameSecret, foregroundColor: Color.settingsItemForeground)
                            }
                        case .login:
                            HStack {
                                Text("Base URL:").frame(minWidth: 100, alignment: .leading)
                                TextField("Backend URL", text: self.$openSesameURL)
                                    .keyboardType(.URL)
                            }
                            Divider()
                                .background(Color.gray)
                            HStack {
                                Text("Username:").frame(minWidth: 100, alignment: .leading)
                                TextField("Username", text: self.$openSesameUsername)
                                    .keyboardType(.default)
                            }
                            HStack {
                                Text("Password:").frame(minWidth: 100, alignment: .leading)
                                SecureField("Password", text: self.$openSesamePassword)
                            }                            
                        case .qrcode:
                            Button(action: {
                                isCameraPresented = true
                            }) {
                                HStack {
                                    Spacer()
                                    VStack {
                                        Image(systemName: "qrcode")
                                            .resizable()
                                            .frame(width: 48, height: 48)
                                        Text("Scan QR Code")
                                            .foregroundColor(.blue)
                                            .underline()
                                    }
                                    Spacer()
                                }
                            }
                        }
                    }
                }
                .listRowBackground(Color.settingsSectionBackground)
            }
            .scrollContentBackground(.hidden)
        }
        .sheet(isPresented: $isCameraPresented) {
            QRCodeScannerContainer (isCameraPresented: self.$isCameraPresented){ scannedCode in
                self.handleNewQrcode(newScannedCode: scannedCode)
            }
        }
    }
}


#Preview {
    let result = OnboardingServerSettings(selectedAuthMethod: .constant(AuthMethod.qrcode), openSesameURL: .constant("serverURL"), openSesameSecret: .constant("secret"), openSesameUsername: .constant(""), openSesamePassword: .constant(""))
        .background(Color.settingsBackground)
        .foregroundColor(Color.settingsForeground)
    return result
}
