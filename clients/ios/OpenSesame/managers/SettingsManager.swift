import Foundation

class SettingsManager {
    
    static let shared = SettingsManager()
    
    private let preferencesKey = "settingsPreference"
    private var settings: SettingsPreference?
    
    private func loadSettings() -> SettingsPreference {
        if let data = UserDefaults.standard.data(forKey: preferencesKey),
           let settings = try? JSONDecoder().decode(SettingsPreference.self, from: data) {
            return settings
        } else {
            // default values in case we don't have any settings
            return SettingsPreference(
                enableMic: true,
                openSesameURL: "",
                openSesameSecret: "",
                dailyApiKey: "",
                openApiKey: "",
                cartesiaApiKey: "",
                deepgramApiKey: "",
                togetherApiKey: "",
                anthropicApiKey: "",
                picovoiceApiKey: "",
                enableWakeWord: false
            )
        }
    }

    func getSettings() -> SettingsPreference {
        guard let settings = self.settings else {
            let loadedSettings = self.loadSettings()
            self.settings = loadedSettings
            return loadedSettings
        }
        return settings
    }

    func updateSettings(settings: SettingsPreference) {
        if let data = try? JSONEncoder().encode(settings) {
            UserDefaults.standard.set(data, forKey: preferencesKey)
        }
        self.settings = settings
    }
    
    func updateDefaultWorkspace(workspaceId: UUID?) {
        var settings = self.getSettings()
        settings.defaultWorkspace = workspaceId
        self.updateSettings(settings: settings)
    }
    
}
