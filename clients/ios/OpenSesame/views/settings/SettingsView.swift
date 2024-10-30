import SwiftUI
import RTVIClientIOS

struct SettingsView: View {
    // prod
    @EnvironmentObject private var model: OpenSesameModel
    
    @Binding var showingSettings: Bool
    
    @State private var selectedMic: MediaDeviceId? = nil
    @State private var isMicEnabled: Bool = true
    @State private var openSesameURL: String = ""
    @State private var isWakeWordEnabled: Bool = false
    
    @State private var showDeleteConfirmation: Bool = false
    @State private var showLogoutConfirmation: Bool = false
    
    // Note: In a production environment, it is recommended to avoid calling Daily's API endpoint directly.
    // Instead, you should route requests through your own server to handle authentication, validation,
    // and any other necessary logic. Therefore, the baseUrl should be set to the URL of your own server.
    @State private var dailyApiKey: String = ""
    @State private var openApiKey: String = ""
    @State private var picovoiceApiKey: String = ""
    
    @State private var openSesameSecret: String = ""
    @State private var cartesiaApiKey: String = ""
    @State private var deepgramApiKey: String = ""
    @State private var togetherApiKey: String = ""
    @State private var anthropicApiKey: String = ""
    
    var body: some View {
        let microphones = self.model.getAllMics() ?? []
        ZStack {
            NavigationView {
                Form {
                    Section(header: Text("Open Sesame")) {
                        VStack {
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
                            Divider()
                                .background(Color.gray)
                            HStack {
                                Text("Log out")
                                    .foregroundColor(Color.black)
                                    .frame(maxWidth: .infinity)
                                    .padding(10)
                            }
                            .frame(maxWidth: .infinity)
                            .background(.white.opacity(0.8))
                            .onTapGesture {
                                self.showLogoutConfirmation = true
                            }
                            .cornerRadius(12)
                        }
                    }
                    .listRowBackground(Color.settingsSectionBackground)
                    Section(header: Text("Services Credentials")) {
                        VStack {
                            HStack {
                                Text("Daily:").frame(minWidth: 100, alignment: .leading)
                                SecureFieldWithPlaceholder(placeholder: "Daily API Key", text: self.$dailyApiKey, foregroundColor: Color.settingsItemForeground)
                            }
                            Divider()
                                .background(Color.gray)
                            HStack {
                                Text("Cartesia:").frame(minWidth: 100, alignment: .leading)
                                SecureFieldWithPlaceholder(placeholder: "Cartesia API Key", text: self.$cartesiaApiKey, foregroundColor: Color.settingsItemForeground)
                            }
                            Divider()
                                .background(Color.gray)
                            HStack {
                                Text("Deepgram:").frame(minWidth: 100, alignment: .leading)
                                SecureFieldWithPlaceholder(placeholder: "Deepgram API Key", text: self.$deepgramApiKey, foregroundColor: Color.settingsItemForeground)
                            }
                            Divider()
                                .background(Color.gray)
                            HStack {
                                Text("Together:").frame(minWidth: 100, alignment: .leading)
                                SecureFieldWithPlaceholder(placeholder: "Together API Key", text: self.$togetherApiKey, foregroundColor: Color.settingsItemForeground)
                            }
                            Divider()
                                .background(Color.gray)
                            HStack {
                                Text("Anthropic:").frame(minWidth: 100, alignment: .leading)
                                SecureFieldWithPlaceholder(placeholder: "Anthropic API Key", text: self.$anthropicApiKey, foregroundColor: Color.settingsItemForeground)
                            }
                            Divider()
                                .background(Color.gray)
                            HStack {
                                Text("OpenAI:").frame(minWidth: 100, alignment: .leading)
                                SecureFieldWithPlaceholder(placeholder: "Open API Key", text: self.$openApiKey, foregroundColor: Color.settingsItemForeground)
                            }
                        }
                    }
                    .listRowBackground(Color.settingsSectionBackground)
                    .foregroundColor(Color.settingsForeground)
                    
                    if (!microphones.isEmpty) {
                        Section(header: Text("Audio Settings")) {
                            VStack {
                                ForEach(Array(microphones.enumerated()), id: \.element.id.id) { index, mic in
                                    HStack {
                                        Text(mic.name)
                                        Spacer()
                                        if mic.id == self.selectedMic {
                                            Image(systemName: "checkmark")
                                        }
                                    }
                                    .frame(maxWidth: .infinity)
                                    .contentShape(Rectangle())
                                    .onTapGesture {
                                        self.selectMic(mic.id)
                                    }
                                    if index < microphones.count - 1 {
                                        Divider()
                                            .background(Color.gray)
                                    }
                                }
                            }
                        }
                        .listRowBackground(Color.settingsSectionBackground)
                    }
                    
                    Section(header: Text("Start options")) {
                        VStack {
                            Toggle("Enable Microphone", isOn: self.$isMicEnabled)
                        }
                    }
                    .listRowBackground(Color.settingsSectionBackground)
                    
                    // TODO: leaving this section disabled for now
                    // We need to implement the tool calling so we can disconnect
                    // the bot before making it available
                    Section(header: Text("Wake word")) {
                        VStack {
                            Toggle("Enable the wake word to start Daily Assistant", isOn: self.$isWakeWordEnabled)
                            Divider()
                                .background(Color.gray)
                            HStack {
                                Text("Picovoice:").frame(minWidth: 100, alignment: .leading)
                                SecureFieldWithPlaceholder(placeholder: "Picovoice API Key", text: self.$picovoiceApiKey, foregroundColor: Color.settingsItemForeground)
                            }
                        }
                    }
                    .listRowBackground(Color.settingsSectionBackground)
                    
                    // Delete Conversation Button
                    if (self.model.selectedConversation != nil) {
                        Section {
                            Button(action: {
                                self.showDeleteConfirmation = true
                            }) {
                                Text("Delete Conversation")
                                    .foregroundColor(Color.deleteForeground)
                            }
                            .frame(maxWidth: .infinity)
                        }
                        .listRowBackground(Color.deleteBackground.opacity(0.2))
                    }
                }
                .toolbar {
                    ToolbarItem(placement: .navigationBarLeading) {
                        Text("Settings")
                            .font(.headline)
                    }
                    ToolbarItem(placement: .navigationBarTrailing) {
                        Button("Close") {
                            self.saveSettings()
                        }
                    }
                }
                .onAppear {
                    self.loadSettings()
                }
                .scrollContentBackground(.hidden) // Hide the default form background
                .background(Color.settingsBackground)
                .foregroundColor(Color.settingsForeground)
            }
            .background(Color.settingsBackground)
            .foregroundColor(Color.settingsForeground)
            
            if self.showDeleteConfirmation {
                ConfirmationView(
                    title: "Confirm Deletion",
                    message: "Are you sure you want to delete this conversation?",
                    onConfirm: {
                        self.showDeleteConfirmation = false
                        self.model.deleteCurrentChat()
                        self.showingSettings = false
                    },
                    onCancel: {
                        self.showDeleteConfirmation = false
                    }
                )
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .transition(.scale)
                .zIndex(1)
            }
            
            if self.showLogoutConfirmation {
                ConfirmationView(
                    title: "Confirm log out",
                    message: "Are you sure you want to log out?",
                    onConfirm: {
                        self.showLogoutConfirmation = false
                        self.openSesameSecret = ""
                        self.saveSettings()
                    },
                    onCancel: {
                        self.showLogoutConfirmation = false
                    }
                )
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .transition(.scale)
                .zIndex(1)
            }
        }
    }
    
    private func selectMic(_ mic: MediaDeviceId) {
        self.selectedMic = mic
        self.model.updateMic(micId: mic)
    }
    
    private func saveSettings() {
        let newSettings = SettingsPreference(
            selectedMic: self.selectedMic?.id,
            enableMic: self.isMicEnabled,
            openSesameURL: self.openSesameURL,
            openSesameSecret: self.openSesameSecret,
            dailyApiKey: self.dailyApiKey,
            openApiKey: self.openApiKey,
            cartesiaApiKey: self.cartesiaApiKey,
            deepgramApiKey: self.deepgramApiKey,
            togetherApiKey: self.togetherApiKey,
            anthropicApiKey: self.anthropicApiKey,
            picovoiceApiKey: self.picovoiceApiKey,
            enableWakeWord: self.isWakeWordEnabled
        )
        SettingsManager.shared.updateSettings(settings: newSettings)
        self.showingSettings = false
        self.model.refreshSettingsAndLoadWorkspaces()
    }
    
    private func loadSettings() {
        let savedSettings = SettingsManager.shared.getSettings()
        if let selectedMic = savedSettings.selectedMic {
            self.selectedMic = MediaDeviceId(id: selectedMic)
        } else {
            self.selectedMic = nil
        }
        self.isMicEnabled = savedSettings.enableMic
        self.openSesameURL = savedSettings.openSesameURL
        self.dailyApiKey = savedSettings.dailyApiKey
        self.openApiKey = savedSettings.openApiKey
        self.picovoiceApiKey = savedSettings.picovoiceApiKey
        self.isWakeWordEnabled = savedSettings.enableWakeWord
        
        self.openSesameSecret = savedSettings.openSesameSecret
        self.cartesiaApiKey = savedSettings.cartesiaApiKey
        self.deepgramApiKey = savedSettings.deepgramApiKey
        self.togetherApiKey = savedSettings.togetherApiKey
        self.anthropicApiKey = savedSettings.anthropicApiKey
    }
}

#Preview {
    let mockModel = OpenSesameMockModel()
    let result = SettingsView(showingSettings: .constant(true))
        .environmentObject(mockModel as OpenSesameModel)
    mockModel.startAudioLevelSimulation()
    return result
}
