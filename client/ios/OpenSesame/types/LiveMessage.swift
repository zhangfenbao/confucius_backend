import Foundation

class LiveMessage: ObservableObject, Identifiable, Equatable {
    
    @Published var content: String
    
    let fromBot: Bool
    let updatedAt: Date
    @Published var isBotSpeaking: Bool
    
    init(content: String, fromBot: Bool, updatedAt: Date, isInProgress: Bool = false) {
        self.content = content
        self.fromBot = fromBot
        self.updatedAt = updatedAt
        self.isBotSpeaking = isInProgress
    }
    
    static func == (lhs: LiveMessage, rhs: LiveMessage) -> Bool {
        lhs.updatedAt == rhs.updatedAt
    }
    
}
