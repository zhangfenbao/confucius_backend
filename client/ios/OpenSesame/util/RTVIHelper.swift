import Foundation
import RTVIClientIOS

public enum ToolsFunctions: String {
    case endBot = "end_bot"
}

class RTVIHelper {
    
    static func createOptions(conversation: ConversationModel, baseUrl: String, enableMic: Bool, apiKey: String) -> RTVIClientOptions {
        let headers = [["Authorization": "Bearer \(apiKey)"]]
        let requestData = Value.object([
            "conversation_id": .string(conversation.id.uuidString)
        ])

        return RTVIClientOptions.init(
            enableMic: enableMic,
            enableCam: false, 
            params: RTVIClientParams(
                baseUrl: baseUrl,
                headers: headers,
                requestData: requestData
            )
        )
    }
    
    // Define a method that creates a WorkspaceModel based on inputs.
    public static func createWorkspaceModel(
        title: String,
        prompt: String,
        llmProvider: LLMProvider,
        llmModel: LLMModel,
        ttsProvider: TTSProvider,
        ttsLanguage: Language,
        ttsVoice: Voice,
        interactionMode: InteractionMode
    ) -> WorkspaceUpdateModel {

        let vadConfig = RTVIServiceConfig(
            service: "vad",
            options: [
                RTVIServiceOptionConfig(
                    name: "params",
                    value: Value.object([
                        "stop_secs": Value.number(0.8)
                    ])
                )
            ]
        )
        
        // Dynamic TTS config based on the provided language
        var ttsConfig = RTVIServiceConfig(
            service: "tts",
            options: [
                RTVIServiceOptionConfig(name: "voice", value: Value.string(ttsVoice.id)),
                RTVIServiceOptionConfig(name:"model", value: Value.string(ttsVoice.ttsModel)),
                RTVIServiceOptionConfig(name:"language", value: Value.string(ttsLanguage.id))
            ]
        )
        if interactionMode == .informational {
            ttsConfig.options.append(
                RTVIServiceOptionConfig(name: "text_filter", value: Value.object([
                    "filter_code" : .boolean(true),
                    "filter_tables": .boolean(true)
                ])
            ))
        }

        // Dynamic LLM config based on the provided model
        let llmConfig = RTVIServiceConfig(
            service: "llm",
            options: [
                RTVIServiceOptionConfig(name:"model", value:Value.string(llmModel.id)),
                RTVIServiceOptionConfig(name:"run_on_config", value:Value.boolean(false))
            ]
        )
        
        let sttConfig = RTVIServiceConfig(
            service: "stt",
            options: [
                RTVIServiceOptionConfig(name:"model", value:Value.string(ttsVoice.sttModel)),
                RTVIServiceOptionConfig(name:"language", value:Value.string(ttsLanguage.id))
            ]
        )

        let services = [
            "stt": "deepgram",
            "tts": ttsProvider.id,
            "llm": llmProvider.id,
        ]

        let defaultLlmContext = [
            MessageCreateModel(
                content: [
                    "role": "system",
                    "content": prompt
                ],
                extraMetadata: [:]
            )
        ]
        
        let settings = SettingsManager.shared.getSettings()
        let apiKeys: [String:String] = [
            "daily": settings.dailyApiKey,
            "openai": settings.openApiKey,
            "cartesia": settings.cartesiaApiKey,
            "deepgram": settings.deepgramApiKey,
            "together": settings.togetherApiKey,
            "anthropic": settings.anthropicApiKey,
        ]

        let workspaceConfig = WorkspaceConfig(
            config: [
                vadConfig, ttsConfig, llmConfig, sttConfig
            ],
            apiKeys: apiKeys,
            services: services,
            defaultLlmContext: defaultLlmContext,
            interactionMode: interactionMode
        )

        return WorkspaceUpdateModel(
            title: title,
            config: workspaceConfig
        )
    }


}
