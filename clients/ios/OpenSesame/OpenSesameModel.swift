import RTVIClientIOS
import SwiftUI

class OpenSesameModel: ObservableObject {

    static let shared = OpenSesameModel()

    private var rtviManager: RTVIManager = RTVIManager.init()
    private var activityManager = ActivityManager.shared
    private var wakeWordManager = WakeWordManager.shared
    private var storageManager = OpenSesameAPI.shared

    @Published var voiceClientStatus: String = TransportState.disconnected.description
    @Published var isInCall: Bool = false
    @Published var isBotReady: Bool = false
    @Published var isBotSpeaking: Bool = false

    @Published var isMicEnabled: Bool = false
    @Published var isCamEnabled: Bool = false
    @Published var localCamId: MediaTrackId? = nil

    @Published var toastMessage: String? = nil
    @Published var showToast: Bool = false
    
    @Published var needsOnboard: Bool = false
    
    @Published var showInfoPanel: Bool = false
    @Published var infoMessageType: MessageType = .info
    @Published var infoMessage: String = ""
    @Published var infoMessageDetails: String?

    @Published
    var remoteAudioLevel: Float = 0
    @Published
    var localAudioLevel: Float = 0

    var selectedWorkspace: WorkspaceModel?
    var selectedConversation: ConversationModel?

    @Published var workspaces: [WorkspaceModel] = []
    @Published var selectedWorkspaceName: String = ""
    @Published var conversations: [ConversationModel] = []
    @Published var messages: [LiveMessage] = []
    @Published var liveBotMessage: LiveMessage?

    init() {
        self.rtviManager.rtviDelegate = self
        self.refreshSettingsAndLoadWorkspaces()
    }

    func refreshSettingsAndLoadWorkspaces() {
        let settings = SettingsManager.shared.getSettings()
        OpenSesameAPI.shared.setBearerToken(settings.openSesameSecret)
        if self.hasPendingSettings(settingsPreferences: settings) {
            self.needsOnboard = true
        } else {
            self.needsOnboard = false
            if(self.workspaces.isEmpty) {
                self.loadWorkspaces()
            }
        }
        
        Task {
            if settings.enableWakeWord {
                await self.activityManager.start()
                self.wakeWordManager.startListening()
            } else {
                await self.activityManager.cancelAllRunningActivities()
                self.wakeWordManager.stopListening()
            }
        }
    }
    
    func testServerConnection(baseUrl: String, apiKey: String) async -> Bool {
        // This will invoke the API so we can test the connection
        do {
            self.storageManager.setBaseURL(baseUrl)
            self.storageManager.setBearerToken(apiKey)
            _ = try await self.storageManager.getAllWorkspaces()
            return true
        } catch {
            self.showError(message: "Test connection failed: \(error.localizedDescription)")
        }
        return false
    }
    
    func signIn(baseUrl: String, username: String, password: String) async -> SessionTokenResponse? {
        do {
            self.storageManager.setBaseURL(baseUrl)
            return try await self.storageManager.createSessionToken(username: username, password: password)
        } catch {
            self.showError(message: "Sign in failed: \(error.localizedDescription)")
        }
        return nil
    }

    private func hasPendingSettings(settingsPreferences: SettingsPreference) -> Bool {
        settingsPreferences.openSesameURL.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        || settingsPreferences.openSesameSecret.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    func connect(automaticallyDisconnect: Bool = false) {
        guard let conversation = self.selectedConversation else {
            self.showError(message: "There is no conversation in progress!")
            return
        }
        Task {
            do {
                try await self.rtviManager.initialize(conversation: conversation)
                try await self.rtviManager.connect(automaticallyDisconnect: automaticallyDisconnect, interactionMode: self.selectedWorkspace?.config.interactionMode)
            } catch {
                self.showError(message: error.localizedDescription)
            }
        }
    }

    func sendTextMessage(textMessage: String) {
        let sanitizedMessage = textMessage.trimmingCharacters(in: .whitespacesAndNewlines)
        if(sanitizedMessage.isEmpty) {
            return
        }
        guard let conversation = self.selectedConversation else {
            self.showError(message: "There is no conversation in progress!")
            return
        }
        Task {
            do {
                self.createLiveMessage(content: sanitizedMessage, fromBot: false)
                try await self.rtviManager.initialize(conversation: conversation)
                try await self.rtviManager.appendTextMessage(textMessage: sanitizedMessage)
            } catch {
                self.showError(message: error.localizedDescription)
            }
        }
    }

    @MainActor
    func disconnect() {
        self.rtviManager.disconnect()
    }

    func showError(message: String) {
        // TODO: probably need to pass a different color os something different to the toas
        self.showToast(message: message)
    }

    func showToast(message: String) {
        DispatchQueue.main.async {
            self.toastMessage = message
            self.showToast = true
        }
        // Hide the toast after 5 seconds
        DispatchQueue.main.asyncAfter(deadline: .now() + 5) {
            self.showToast = false
            self.toastMessage = nil
        }
    }
    
    func showInfoPanel(message: String, messageType: MessageType = .info, details: String? = nil) {
        DispatchQueue.main.async {
            self.showInfoPanel = true
            self.infoMessageType = messageType
            self.infoMessage = message
            self.infoMessageDetails = details
        }
    }
    
    func closeInfoPanel() {
        DispatchQueue.main.async {
            self.showInfoPanel = false
            self.infoMessage = ""
            self.infoMessageDetails = nil
        }
    }

    @MainActor
    func toggleMicInput() {
        self.rtviManager.enableMic(enable: !self.isMicEnabled) { result in
            switch result {
            case .success():
                self.isMicEnabled = self.rtviManager.isMicEnabled()
            case .failure(let error):
                self.showError(message: error.localizedDescription)
            }
        }
    }

    @MainActor
    func toggleCamInput() {
        self.rtviManager.enableCam(enable: !self.isCamEnabled) { result in
            switch result {
            case .success():
                self.isCamEnabled = self.rtviManager.isCamEnabled()
            case .failure(let error):
                self.showError(message: error.localizedDescription)
            }
        }
    }

    @MainActor
    func getAllMics() -> [MediaDeviceInfo]? {
        self.rtviManager.getAllMics()
    }

    @MainActor
    public func updateMic(micId: MediaDeviceId) {
        self.rtviManager.updateMic(micId: micId)
    }

    @MainActor
    public func interrupt() {
        self.rtviManager.interrupt()
    }

    func createWorkspace(newWorkspace: WorkspaceUpdateModel) {
        Task {
            do {
                _ = try await self.storageManager.createWorkspace(workspace: newWorkspace)
                // refreshing the current workspaces
                self.loadWorkspaces()
            } catch {
                self.showError(message: "Failed to create new workspace: \(error.localizedDescription)")
            }
        }
    }
    
    func updateWorkspace(workspaceId: UUID, workspace: WorkspaceUpdateModel) {
        Task {
            do {
                _ = try await self.storageManager.updateWorkspace(workspaceID: workspaceId, workspace: workspace)
                // refreshing the current workspaces
                self.loadWorkspaces()
            } catch {
                self.showError(message: "Failed to update workspace: \(error.localizedDescription)")
            }
        }
    }

    func loadWorkspaces() {
        Task {
            do {
                let workspaces = try await self.storageManager.getAllWorkspaces()
                DispatchQueue.main.async {
                    self.workspaces = workspaces
                    if(workspaces.isEmpty){
                        self.showInfoPanel(message: "No workspaces available.", messageType: .info, details: "Create a new workspace using the button above.")
                        self.selectedWorkspaceName = "New workspace"
                    } else if self.selectedWorkspace == nil {
                        self.closeInfoPanel()
                        let defaultWorkspaceId = SettingsManager.shared.getSettings().defaultWorkspace
                        let defaultWorkspace = workspaces.first(where: { $0.id == defaultWorkspaceId }) ?? workspaces[0]
                        self.selectWorkspace(workspace: defaultWorkspace)
                    }
                }
            } catch {
                self.showError(message: "Failed to load workspaces: \(error.localizedDescription)")
            }
        }
    }

    func selectWorkspace(workspace: WorkspaceModel) {
        self.selectedWorkspace = workspace
        self.selectedWorkspaceName = workspace.title
        self.messages = []
        self.selectedConversation = nil
        self.loadAllConversations()
        // Updating the preferred workspace
        SettingsManager.shared.updateDefaultWorkspace(workspaceId: workspace.id)
    }

    func loadAllConversations() {
        guard let selectedWorkspace = self.selectedWorkspace else {
            return
        }
        Task {
            do {
                let conversations = try await self.storageManager.getAllConversationsByWorkspace(workspaceID: selectedWorkspace.id)
                DispatchQueue.main.async {
                    self.conversations = conversations
                    if( self.conversations.isEmpty ) {
                        self.startNewConversation()
                    } else {
                        self.selectConversation(conversation: self.conversations[0])
                    }
                }
            } catch {
                self.showError(message: "Failed to load conversations: \(error.localizedDescription)")
            }
        }
    }

    func deleteWorkspace(workspaceId: UUID) {
        Task {
            do {
                if (self.isInCall && self.selectedWorkspace?.id == workspaceId) {
                    await self.disconnect()
                }
                try await self.storageManager.deleteWorkspace(workspaceID: workspaceId)
                if (self.selectedWorkspace?.id == workspaceId) {
                    DispatchQueue.main.async {
                        self.selectedWorkspace = nil
                    }
                }
                // refreshing the current workspaces
                self.loadWorkspaces()
            } catch {
                self.showError(message: "Failed to delete workspace: \(error.localizedDescription)")
            }
        }
    }

    func startNewConversation(resetContext: Bool = true) {
        guard let selectedWorkspace = self.selectedWorkspace else {
            return
        }
        Task {
            let dateFormatter = DateFormatter()
            dateFormatter.dateFormat = "MM/dd/yyyy HH:mm"
            // It should be updated later automatically based in the content of the conversation.
            let title = dateFormatter.string(from: Date())
            do {
                let newConversation = ConversationCreateModel(
                    workspaceID: selectedWorkspace.id,
                    title: title,
                    languageCode: selectedWorkspace.getLanguage()?.id
                )
                let createdConversation = try await self.storageManager.createConversation(conversation: newConversation)
                DispatchQueue.main.async {
                    self.conversations.insert(createdConversation, at: 0)
                    self.selectConversation(conversation: createdConversation)
                }
            } catch {
                self.showError(message: "Failed to start new chat: \(error.localizedDescription)")
            }
        }
    }

    func selectConversation(conversation: ConversationModel) {
        Task {
            do {
                let messages = try await self.storageManager.getAllMessages(conversationID: conversation.id)
                DispatchQueue.main.async {
                    // Disconnecting the bot in case it is in connected in voice mode
                    self.disconnect()
                    // Setting the new conversation
                    self.selectedConversation = conversation
                    let liveMessages: [LiveMessage] = messages.map { messageModel in
                        LiveMessage(content: messageModel.content.content, fromBot: messageModel.isFromBot(), updatedAt: messageModel.updatedAt)
                    }
                    self.messages = liveMessages
                }
            } catch {
                print("Failed to load the chat \(error.localizedDescription)")
            }
        }
    }

    func deleteCurrentChat() {
        guard let selectedConversation = self.selectedConversation else {
            return
        }
        Task {
            if self.isInCall {
                await self.disconnect()
            }
            do {
                try await self.storageManager.deleteConversation(conversationID: selectedConversation.id)
                DispatchQueue.main.async {
                    self.messages = []
                    self.selectedConversation = nil
                    self.loadAllConversations()
                }
            } catch {
                print("Failed to delete chat \(error.localizedDescription)")
            }
        }
    }

}

extension OpenSesameModel: RTVIDelegate {
    
    private func handleEvent(eventName: String, eventValue: Any? = nil) {
        if let value = eventValue {
            Logger.shared.debug("RTVI Demo, received event:\(eventName), value:\(value)")
        } else {
            Logger.shared.debug("RTVI Demo, received event: \(eventName)")
        }
    }

    private func createLiveMessage(content:String = "", fromBot:Bool, isInProgress: Bool = false) {
        // Closing the previous message
        self.closeLiveMessage()
        // Creating a new one
        DispatchQueue.main.async {
            let liveMessage = LiveMessage(content: content, fromBot: fromBot, updatedAt: Date(), isInProgress: isInProgress)
            self.messages.append(liveMessage)
            if fromBot && isInProgress {
                self.liveBotMessage = liveMessage
                self.liveBotMessage?.isBotSpeaking = true
            }
        }
    }
    
    private func appendTextToLiveMessage(content:String) {
        DispatchQueue.main.async {
            // Updating the last message with the new content
            self.liveBotMessage?.content += content
        }
    }
    
    private func closeLiveMessage() {
        DispatchQueue.main.async {
            self.liveBotMessage?.isBotSpeaking = false
            self.liveBotMessage = nil
        }
    }

    func onTransportStateChanged(state: TransportState) {
        self.handleEvent(eventName: "onTransportStateChanged", eventValue: state)
        self.voiceClientStatus = state.description
        self.isInCall = ( state == .connecting || state == .connected || state == .ready || state == .authenticating || state == .disconnecting )
        self.activityManager.updateActivity(connectionStatus: self.voiceClientStatus)
    }

    func onBotReady(botReadyData: BotReadyData) {
        self.handleEvent(eventName: "onBotReady.")
        self.isBotReady = true
        // stops the wake word while we are in a call
        self.wakeWordManager.stopListening()
    }

    func onConnected() {
        DispatchQueue.main.async {
            self.isMicEnabled = self.rtviManager.isMicEnabled()
            self.isCamEnabled = self.rtviManager.isCamEnabled()
        }
    }

    func onDisconnected() {
        self.isBotReady = false
        self.isBotSpeaking = false
        // ensure that we are still listening after we disconnect
        self.wakeWordManager.startListening()
    }

    func onRemoteAudioLevel(level: Float, participant: Participant) {
        if (self.remoteAudioLevel != level) {
            DispatchQueue.main.async {
                self.isBotSpeaking = level > 0
                self.remoteAudioLevel = level
            }
        }
    }

    func onUserAudioLevel(level: Float) {
        if (self.localAudioLevel != level) {
            DispatchQueue.main.async {
                self.localAudioLevel = level
            }
        }
    }

    func onUserTranscript(data: Transcript) {
        if data.final ?? false {
            self.handleEvent(eventName: "onUserTranscript", eventValue: data.text)
            self.createLiveMessage(content: data.text, fromBot: false)
        }
    }
    
    func onBotTextStarted() {
        self.createLiveMessage(fromBot: true, isInProgress: true)
    }
    
    func onBotPartialText(text: String) {
        self.appendTextToLiveMessage(content: text)
    }
    
    func onBotStartedSpeaking() {
        self.liveBotMessage?.isBotSpeaking = true
    }
    
    func onBotStoppedSpeaking() {
        self.liveBotMessage?.isBotSpeaking = false
    }

    func onError(message: String) {
        self.handleEvent(eventName: "onError", eventValue: message)
        self.showError(message: message)
    }

    func onTracksUpdated(tracks: Tracks) {
        self.handleEvent(eventName: "onTracksUpdated", eventValue: tracks)
        self.localCamId = tracks.local.video
    }

}
