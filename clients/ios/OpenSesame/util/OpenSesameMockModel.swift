import Foundation

// Mock class to make it possible to use the Preview when developing the Views.
class OpenSesameMockModel: OpenSesameModel {

    public static let mockWorkspace: WorkspaceModel = {
        let ttsProvider = RTVIDefaultData.supportedTtsProviders[0]
        let llmProvider = RTVIDefaultData.defaultLLMModels[0]
        let workspaceModel = RTVIHelper.createWorkspaceModel(
            title: "mock",
            prompt: "MockPrompt",
            llmProvider: llmProvider,
            llmModel: llmProvider.models[0],
            ttsProvider: ttsProvider,
            ttsLanguage: ttsProvider.languages[0],
            ttsVoice: ttsProvider.languages[0].voices[0],
            interactionMode: .conversational
        )
        return WorkspaceModel(
            workspaceID: UUID(),
            title: workspaceModel.title!,
            config: workspaceModel.config!,
            createdAt: Date(),
            updatedAt: Date()
        )
    }()

    public static let mockConversation: ConversationModel = ConversationModel(
        conversationID: UUID(),
        workspaceID: UUID(),
        title: "Today Conversation",
        archived: false,
        languageCode: "en",
        createdAt: Date(),
        updatedAt: Date()
    )

    public static let mockMessage: MessageModel = MessageModel(
        messageID: UUID(),
        conversationID: UUID(),
        messageNumber: 1,
        content: MessageContent(
            role: "system",
            content: "You are a helpfull assistant"
        ),
        createdAt: Date(),
        updatedAt: Date()
    )

    override init() {
        super.init()
        let messageModel = OpenSesameMockModel.mockMessage
        let liveMessage = LiveMessage(content: messageModel.content.content, fromBot: messageModel.isFromBot(), updatedAt: messageModel.updatedAt)
        let markdownMessageFromBot = LiveMessage(
            content: "This is ~~normal~~ **markdown** text.",
            fromBot: true,
            updatedAt: Date(),
            isInProgress: false
        )
        let liveMessageFromBot = LiveMessage(
            content: "Message from bot",
            fromBot: true,
            updatedAt: Date(),
            isInProgress: true
        )
        self.messages = [ liveMessage, markdownMessageFromBot, liveMessageFromBot ]
        self.isBotReady = true
        self.isInCall = true
        self.showInfoPanel = false
        self.isBotSpeaking = true
        self.infoMessage = "Some important settings are missing."
        self.infoMessageDetails = "Please open the settings from the top-right menu to complete them."
        self.selectedConversation = OpenSesameMockModel.mockConversation
    }

    override func connect(automaticallyDisconnect: Bool = false) {
        print("connect")
    }

    override func disconnect() {
        print("disconnect")
    }

    override func showError(message: String) {
        print("show error \(message)")
    }

    override func loadWorkspaces() {
        self.workspaces = [ OpenSesameMockModel.mockWorkspace ]
    }

    override func loadAllConversations() {
        let calendar = Calendar.current
        let today = calendar.startOfDay(for: Date())
        let yesterday = calendar.date(byAdding: .day, value: -1, to: today)!
        let previous = calendar.date(byAdding: .day, value: -2, to: today)!
        self.conversations = [
            ConversationModel(
                conversationID: UUID(),
                workspaceID: UUID(),
                title: "Today Conversation",
                archived: false,
                languageCode: "en",
                createdAt: today,
                updatedAt: today
            ),
            ConversationModel(
                conversationID: UUID(),
                workspaceID: UUID(),
                title: "Today Conversation 2",
                archived: false,
                languageCode: "en",
                createdAt: today,
                updatedAt: today
            ),
            ConversationModel(
                conversationID: UUID(),
                workspaceID: UUID(),
                title: "Yesterday Conversation",
                archived: false,
                languageCode: "en",
                createdAt: yesterday,
                updatedAt: yesterday
            ),
            ConversationModel(
                conversationID: UUID(),
                workspaceID: UUID(),
                title: "Previous Conversation",
                archived: false,
                languageCode: "en",
                createdAt: previous,
                updatedAt: previous
            )
        ]
    }

    // -------  helpers functions  -----------

    func startAudioLevelSimulation() {
        // Simulate audio level changes
        Timer.scheduledTimer(withTimeInterval: 0.1, repeats: true) { _ in
            let newLevel = Float.random(in: 0...0.2)
            self.remoteAudioLevel = newLevel
            self.localAudioLevel = newLevel
        }
    }

}
