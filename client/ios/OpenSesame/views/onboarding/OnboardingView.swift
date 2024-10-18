import SwiftUI

struct OnboardingView: View {
    
    @EnvironmentObject private var model: OpenSesameModel
    @State private var currentStep = 1
    
    // Open Sesame
    @State private var selectedAuthMethod: AuthMethod = .qrcode
    @State private var openSesameURL = ""
    @State private var openSesameSecret = ""
    @State private var openSesameUsername = ""
    @State private var openSesamePassword = ""
    
    // API keys
    @State private var dailyApiKey: String = ""
    @State private var openApiKey: String = ""
    @State private var cartesiaApiKey: String = ""
    @State private var deepgramApiKey: String = ""
    @State private var togetherApiKey: String = ""
    @State private var anthropicApiKey: String = ""

    var body: some View {
        VStack {
            Text("Open Sesame")
                .font(.largeTitle)
                .fontWeight(.bold)
                .multilineTextAlignment(.center)
                .padding(.top, 40)
                .padding(.bottom, 20)
            
            let description = AttributedString("Open Sesame requires a hosted backend instance. For more details, see the ")
                    
            var link: AttributedString {
                var linkText = AttributedString("documentation.")
                linkText.link = URL(string: "https://github.com/pipecat-ai/open-sesame/")!
                linkText.foregroundColor = .blue
                linkText.underlineStyle = .single
                return linkText
            }
            
            Text(description + link)
                .font(.callout)
                .padding(.bottom, 40)
            
            if self.currentStep == 1 {
                OnboardingServerSettings(selectedAuthMethod: $selectedAuthMethod, openSesameURL: $openSesameURL, openSesameSecret: $openSesameSecret, openSesameUsername: $openSesameUsername, openSesamePassword: $openSesamePassword)
            } else {
                OnboardingApiKeys(dailyApiKey: $dailyApiKey, openApiKey: $openApiKey, cartesiaApiKey: $cartesiaApiKey, deepgramApiKey: $deepgramApiKey, togetherApiKey: $togetherApiKey, anthropicApiKey: $anthropicApiKey)
            }
            
            // Navigation buttons
            HStack {
                Button(action: {
                    if currentStep == 1 {
                        Task {
                            await self.authenticateAndProceed()
                        }
                    } else if currentStep == 2 {
                        self.finishOnboarding()
                    }
                }) {
                    Text(currentStep == 1 ? "Save and Continue" : "Continue")
                        .font(.headline)
                        .padding()
                        .frame(maxWidth: .infinity)
                        .foregroundColor(.black)
                        .background(Color.white)
                        .cornerRadius(10)
                }
            }
            .padding(.horizontal)
            
            Spacer()
        }
        .onAppear {
            self.loadSettings()
        }
        .preferredColorScheme(.dark)
        .background(Color.settingsBackground)
        .foregroundColor(Color.appForeground)
        .toast(message: self.model.toastMessage, isShowing: self.model.showToast)
    }
    
    private func authenticateAndProceed() async {
        if self.selectedAuthMethod == .login {
            self.openSesameSecret = await self.model.signIn(baseUrl: self.openSesameURL, username: self.openSesameUsername, password: self.openSesamePassword)?.token ?? ""
        }
        let connectionSucceded = await self.model.testServerConnection(baseUrl: self.openSesameURL, apiKey: self.openSesameSecret)
        if connectionSucceded {
            self.currentStep = 2
        }
    }
    
    private func finishOnboarding() {
        self.saveSettings()
    }
    
    private func saveSettings() {
        var settings = SettingsManager.shared.getSettings()
        settings.openSesameURL = self.openSesameURL
        settings.openSesameSecret = self.openSesameSecret
        
        settings.dailyApiKey = self.dailyApiKey
        settings.openApiKey = self.openApiKey
        settings.cartesiaApiKey = self.cartesiaApiKey
        settings.deepgramApiKey = self.deepgramApiKey
        settings.togetherApiKey = self.togetherApiKey
        settings.anthropicApiKey = self.anthropicApiKey
        
        SettingsManager.shared.updateSettings(settings: settings)
        
        self.model.refreshSettingsAndLoadWorkspaces()
    }

    private func loadSettings() {
        let savedSettings = SettingsManager.shared.getSettings()
        
        self.openSesameURL = savedSettings.openSesameURL
        self.openSesameSecret = savedSettings.openSesameSecret
        
        self.dailyApiKey = savedSettings.dailyApiKey
        self.openApiKey = savedSettings.openApiKey
        self.cartesiaApiKey = savedSettings.cartesiaApiKey
        self.deepgramApiKey = savedSettings.deepgramApiKey
        self.togetherApiKey = savedSettings.togetherApiKey
        self.anthropicApiKey = savedSettings.anthropicApiKey
    }
}

#Preview {
    let mockModel = OpenSesameMockModel()
    let result = OnboardingView().environmentObject(mockModel as OpenSesameModel)
    return result
}
