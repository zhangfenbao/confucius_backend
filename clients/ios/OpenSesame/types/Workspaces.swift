import Foundation
import RTVIClientIOS

enum InteractionMode: String, Codable {
    case conversational
    case informational
}

struct WorkspaceModel: Codable, Identifiable {
    var id: UUID {
        workspaceID
    }
    let workspaceID: UUID
    var title: String
    var config: WorkspaceConfig
    let createdAt: Date
    let updatedAt: Date
    
    enum CodingKeys: String, CodingKey {
        case workspaceID = "workspace_id"
        case title
        case config
        case createdAt = "created_at"
        case updatedAt = "updated_at"
    }
    
    init(workspaceID: UUID, title: String, config: WorkspaceConfig, createdAt: Date, updatedAt: Date) {
        self.workspaceID = workspaceID
        self.title = title
        self.config = config
        self.createdAt = createdAt
        self.updatedAt = updatedAt
    }
    
    init(from decoder: any Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        self.workspaceID = try container.decode(UUID.self, forKey: .workspaceID)
        self.title = try container.decode(String.self, forKey: .title)
        self.config = try container.decode(WorkspaceConfig.self, forKey: .config)
        
        let createdAtString = try container.decode(String.self, forKey: .createdAt)
        guard let createdAtDate = OpenSesameHelper.parseStringAsDate(dateAsString: createdAtString) else {
            throw DecodingError.dataCorruptedError(forKey: .createdAt, in: container, debugDescription: "Date string does not match format expected by formatter.")
        }
        self.createdAt = createdAtDate
        
        let updatedAtString = try container.decode(String.self, forKey: .updatedAt)
        guard let updatedAtDate = OpenSesameHelper.parseStringAsDate(dateAsString: updatedAtString) else {
            throw DecodingError.dataCorruptedError(forKey: .updatedAt, in: container, debugDescription: "Date string does not match format expected by formatter.")
        }
        self.updatedAt = updatedAtDate
    }
    
    // Helper functions
    
    func getLLMProvider() -> LLMProvider? {
        guard let services = self.config.services else {
            return nil
        }
        return RTVIDefaultData.getLLMProvider(by: services["llm"])
    }
    
    func getLLMModel() -> LLMModel? {
        guard let services = self.config.services else {
            return nil
        }
        guard let serviceConfig = self.config.config?.first(where: { $0.service == "llm" }) else {
            return nil
        }
        guard let modelOption = serviceConfig.options.first(where: { $0.name == "model" }) else {
            return nil
        }
        if case .string(let modelId) = modelOption.value {
            return RTVIDefaultData.getLLMModel(by: modelId, providerId: services["llm"])
        }
        return nil
    }
    
    func getTTSProvider() -> TTSProvider? {
        guard let services = self.config.services else {
            return nil
        }
        guard let ttsProviderId = services["tts"] else {
            return nil
        }
        return RTVIDefaultData.supportedTtsProviders.first{ $0.id == ttsProviderId }
    }
    
    func getLanguage() -> Language? {
        guard let services = self.config.services else {
            return nil
        }
        guard let ttsProviderId = services["tts"] else {
            return nil
        }
        guard let serviceConfig = self.config.config?.first(where: { $0.service == "tts" }) else {
            return nil
        }
        guard let languageOption = serviceConfig.options.first(where: { $0.name == "language" }) else {
            return nil
        }
        if case .string(let languageCode) = languageOption.value {
            return RTVIDefaultData.getLanguage(ttsProviderId: ttsProviderId, languageId: languageCode)
        }
        return nil
    }
    
    func getVoice() -> Voice? {
        guard let serviceConfig = self.config.config?.first(where: { $0.service == "tts" }) else {
            return nil
        }
        guard let voiceOption = serviceConfig.options.first(where: { $0.name == "voice" }) else {
            return nil
        }
        if case .string(let voiceId) = voiceOption.value {
            let language = self.getLanguage()
            return language?.voices.first{ $0.id == voiceId }
        }
        return nil
    }
    
    // TODO: need to improve our interface to be able to render all the prompt messages, instead of only the first one
    func getSystemPrompt() -> String {
        guard let defaultLlmContext = self.config.defaultLlmContext else {
            return ""
        }
        if(defaultLlmContext.isEmpty) {
            return ""
        }
        return defaultLlmContext[0].content["content"] ?? ""
    }
}

struct WorkspaceUpdateModel: Codable {
    let title: String?
    let config: WorkspaceConfig?
}

struct WorkspaceConfig: Codable {
    let config: [RTVIServiceConfig]?
    let apiKeys: [String: String]?
    let services: [String: String]?
    let defaultLlmContext: [MessageCreateModel]?
    let interactionMode: InteractionMode?
    
    enum CodingKeys: String, CodingKey {
        case config
        case apiKeys = "api_keys"
        case services
        case defaultLlmContext = "default_llm_context"
        case interactionMode = "interaction_mode"
    }
}

struct RTVIServiceConfig: Codable {
    let service: String
    var options: [RTVIServiceOptionConfig]
}

struct RTVIServiceOptionConfig: Codable {
    let name: String
    let value: Value
}

struct MessageCreateModel: Codable {
    //TODO: check if we should to change so content can receive anything.
    let content: [String: String]  // Assumed content as a flexible type, adjust based on schema
    let extraMetadata: [String: String]?
    
    enum CodingKeys: String, CodingKey {
        case content
        case extraMetadata = "extra_metadata"
    }
}
