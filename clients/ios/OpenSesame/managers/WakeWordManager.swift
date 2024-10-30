import ios_voice_processor
import Porcupine
import SwiftySound

final class WakeWordManager {
    let keywordPath = Bundle.main.path(forResource: "open-sesame", ofType: "ppn")

    private var porcupineManager: PorcupineManager?
    private var isListening = false

    static let shared = WakeWordManager()

    func startListening() {
        let settings = SettingsManager.shared.getSettings()
        if !settings.enableWakeWord {
            Logger.shared.debug("WakeWordManager - The wake word setting is disabled.")
            return
        }
        
        if self.isListening {
            self.stopListening()
        }
        
        // Obtained from Picovoice Console (https://console.picovoice.ai)
        let accessKey = settings.picovoiceApiKey
        guard VoiceProcessor.hasRecordAudioPermission else {
            VoiceProcessor.requestRecordAudioPermission { isGranted in
                guard isGranted else {
                    DispatchQueue.main.async {
                        Logger.shared.error("WakeWordManager - App requires microphone permission.")
                    }
                    return
                }

                DispatchQueue.main.async {
                    self.startListening()
                }
            }
            return
        }

        guard let customKeyWord = self.keywordPath else {
            Logger.shared.error("WakeWordManager - Failed to retrieve custom keyword!")
            return
        }

        let errorCallback: ((Error) -> Void) = {error in
            Logger.shared.error("WakeWordManager - Received error: \(error)")
        }

        do {
            Sound.category = .playAndRecord
            let keywordCallback: ((Int32) -> Void) = { keywordIndex in
                Task {
                    let openSesameModel = OpenSesameModel.shared
                    if !openSesameModel.isInCall {
                        Sound.play(file: "beep.wav")
                        openSesameModel.connect(automaticallyDisconnect: true)
                    }
                }
            }

            self.porcupineManager = try PorcupineManager(
                accessKey: accessKey,
                keywordPath: customKeyWord,
                onDetection: keywordCallback,
                errorCallback: errorCallback)

            try self.porcupineManager?.start()
            self.isListening = true
        } catch {
            Logger.shared.error("WakeWordManager - Received error: \(error)")
        }

    }

    func stopListening() {
        do {
            try self.porcupineManager?.stop()
        } catch {
            Logger.shared.error("WakeWordManager - Received error: \(error)")
            return
        }
        self.isListening = false
    }
}
