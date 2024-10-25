import Foundation

struct SettingsPreference: Codable {
    // general settings
    var selectedMic: String?
    var enableMic: Bool
    var openSesameURL: String
    var defaultWorkspace: UUID?
    
    // open sesame keys
    var openSesameSecret: String
    var dailyApiKey: String
    var openApiKey: String
    var cartesiaApiKey: String
    var deepgramApiKey: String
    var togetherApiKey: String
    var anthropicApiKey: String
    
    // Settings for enable wake word
    var picovoiceApiKey: String
    var enableWakeWord: Bool
}
