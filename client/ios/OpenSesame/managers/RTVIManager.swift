import Foundation
import RTVIClientIOSDaily
import RTVIClientIOS
import SwiftySound

class RTVIManager: ObservableObject {
    
    private var interactionMode: InteractionMode = .informational
    
    private var rtviClientIOS: RTVIClient?
    var rtviDelegate: RTVIDelegate?
    
    private var automaticallyDisconnect: Bool = false
    private var automaticallyDisconnectTimeout: TimeInterval
    private var automaticallyDisconnectTimer: Timer?
    private var lastMessageReceived = Date()

    init() {
        RTVIClientIOS.setLogLevel(.warn)
        self.automaticallyDisconnectTimeout = 8.0 // seconds
    }
    
    deinit {
        self.stopAutomaticallyDisconnectTimer()
    }
    
    private func startAutomaticallyDisconnectTimer() {
        if self.automaticallyDisconnect {
            self.automaticallyDisconnectTimer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { [weak self] _ in
                self?.maybDisconnect()
            }
        }
    }
    
    private func maybDisconnect() {
        DispatchQueue.main.async {
            let currentTime = Date()
            let timeElapsed = currentTime.timeIntervalSince(self.lastMessageReceived)
            Logger.shared.debug("RTVIManager, timeElapsed: \(timeElapsed)")
            if timeElapsed >= self.automaticallyDisconnectTimeout && self.rtviClientIOS?.state == .ready {
                self.disconnect()
            }
        }
    }

    private func stopAutomaticallyDisconnectTimer() {
        self.automaticallyDisconnectTimer?.invalidate()
        self.automaticallyDisconnectTimer = nil
    }
    
    @MainActor
    func initialize(conversation: ConversationModel) throws {
        if self.rtviClientIOS != nil {
            return
        }
        
        let currentSettings = SettingsManager.shared.getSettings()

        let baseUrl = currentSettings.openSesameURL.trimmingCharacters(in: .whitespacesAndNewlines)
        if baseUrl.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            throw MissingAPIError(message: "Need to fill the backendURL. For more info visit: https://bots.daily.co")
        }
        
        // Appending the RTVI conext to the baseURL
        let rtviURL = baseUrl.hasSuffix("/api") ? baseUrl + "/rtvi" : baseUrl + "/api/rtvi"
        self.rtviClientIOS = DailyVoiceClient.init(
            options: RTVIHelper.createOptions(conversation: conversation, baseUrl: rtviURL, enableMic: currentSettings.enableMic, apiKey: currentSettings.openSesameSecret)
        )
        
        // Registering the llm helper
        let llmHelper = try? self.rtviClientIOS?.registerHelper(service: "llm", helper: LLMHelper.self)
        llmHelper?.delegate = self
        self.rtviClientIOS?.delegate = self
    }

    @MainActor
    func connect(automaticallyDisconnect: Bool = false, interactionMode: InteractionMode?) async throws {
        self.interactionMode = interactionMode ?? .conversational
        self.automaticallyDisconnect = automaticallyDisconnect
        let currentSettings = SettingsManager.shared.getSettings()
        
        do {
            try await self.rtviClientIOS?.start()
        } catch {
            self.rtviClientIOS = nil
            throw RTVIError(message: "Error while trying to connect: \(error.localizedDescription)")
        }
        
        // Selecting the mic based on the preferences
        if let selectedMic = currentSettings.selectedMic {
            self.rtviClientIOS?.updateMic(micId: MediaDeviceId(id: selectedMic), completion: nil)
        }
    }

    @MainActor
    func disconnect() {
        self.rtviClientIOS?.disconnect(completion: nil)
    }

    @MainActor
    func enableMic(enable: Bool, completion: ((Result<Void, AsyncExecutionError>) -> Void)?) {
        self.rtviClientIOS?.enableMic(enable: enable, completion: completion)
    }
    
    @MainActor
    func isMicEnabled() -> Bool {
        self.rtviClientIOS?.isMicEnabled ?? false
    }

    @MainActor
    func enableCam(enable: Bool, completion: ((Result<Void, AsyncExecutionError>) -> Void)?) {
        self.rtviClientIOS?.enableCam(enable: enable, completion: completion)
    }
    
    @MainActor
    func isCamEnabled() -> Bool {
        self.rtviClientIOS?.isCamEnabled ?? false
    }
    
    @MainActor
    func getAllMics() -> [MediaDeviceInfo]? {
        self.rtviClientIOS?.getAllMics()
    }
    
    @MainActor
    public func updateMic(micId: MediaDeviceId) {
        self.rtviClientIOS?.updateMic(micId: micId, completion: nil)
    }
    
    @MainActor
    public func interrupt() {
        self.rtviClientIOS?.action(action: ActionRequest.init(
            service: "tts",
            action: "interrupt"
        ), completion: nil)
    }
    
    @MainActor
    public func say(text: String) {
        self.rtviClientIOS?.action(action: ActionRequest.init(
            service: "tts",
            action: "say",
            arguments: [
                Option(name: "text", value: .string(text))
            ]
        ), completion: nil)
    }
    
    @MainActor
    public func appendTextMessage(textMessage:String) throws {
        let llmHelper: LLMHelper? = try self.rtviClientIOS?.getHelper(service: "llm")
        llmHelper?.appendToMessages(message: LLMContextMessage(role: "user", content: textMessage), runImmediately: true, completion: nil)
    }

}

extension RTVIManager: RTVIClientDelegate, LLMHelperDelegate {
    func onTransportStateChanged(state: TransportState) {
        self.rtviDelegate?.onTransportStateChanged(state: state)
        self.lastMessageReceived = Date()
    }

    @MainActor
    func onBotReady(botReadyData: BotReadyData) {
        self.say(text: "Uh-huh  ")
        self.rtviDelegate?.onBotReady(botReadyData: botReadyData)
    }

    func onConnected() {
        self.rtviDelegate?.onConnected()
        self.startAutomaticallyDisconnectTimer()
    }

    @MainActor
    func onDisconnected() {
        self.interactionMode = .informational
        self.rtviClientIOS?.release()
        self.rtviClientIOS = nil
        // forwarding the method for our external delegate
        self.rtviDelegate?.onDisconnected()
        self.stopAutomaticallyDisconnectTimer()
        if self.automaticallyDisconnect {
            Sound.play(file: "beep.wav")
        }
    }

    func onRemoteAudioLevel(level: Float, participant: Participant) {
        self.rtviDelegate?.onRemoteAudioLevel(level: level, participant: participant)
    }

    func onUserAudioLevel(level: Float) {
        self.rtviDelegate?.onUserAudioLevel(level: level)
    }

    func onUserTranscript(data: Transcript) {
        self.rtviDelegate?.onUserTranscript(data: data)
        self.lastMessageReceived = Date()
    }

    func onBotTTSStarted() {
        if self.interactionMode == .conversational {
            self.rtviDelegate?.onBotTextStarted()
        }
    }
    
    func onBotTTSText(data: BotTTSText) {
        self.lastMessageReceived = Date()
        if self.interactionMode == .conversational {
            self.rtviDelegate?.onBotPartialText(text: data.text + " ")
        }
    }
    
    func onBotLLMStarted() {
        if self.interactionMode == .informational {
            self.rtviDelegate?.onBotTextStarted()
        }
    }
    
    func onBotLLMText(data: BotLLMText) {
        if self.interactionMode == .informational {
            self.rtviDelegate?.onBotPartialText(text: data.text)
        }
    }
    
    func onBotStartedSpeaking(participant: Participant) {
        self.rtviDelegate?.onBotStartedSpeaking()
    }
    
    func onBotStoppedSpeaking(participant: Participant) {
        self.rtviDelegate?.onBotStoppedSpeaking()
    }
    
    func onStorageItemStored(data: StorageItemStoredData) {
        Logger.shared.debug("onStorageItemStored \(data)")
    }

    func onError(message: String) {
        self.rtviDelegate?.onError(message: message)
    }

    func onTracksUpdated(tracks: Tracks) {
        self.rtviDelegate?.onTracksUpdated(tracks: tracks)
    }
}

public protocol RTVIDelegate {
   func onTransportStateChanged(state: TransportState)

    func onBotReady(botReadyData: BotReadyData)

    func onConnected()

    func onDisconnected()

    func onRemoteAudioLevel(level: Float, participant: Participant)

    func onUserAudioLevel(level: Float)

    func onUserTranscript(data: Transcript)

    func onBotTextStarted()
    
    func onBotPartialText(text: String)
    
    func onBotStartedSpeaking()
    
    func onBotStoppedSpeaking()
    
    func onError(message: String)

    func onTracksUpdated(tracks: Tracks)
}




