import Foundation

extension OpenSesameAPI {
    
    func getAllMessages(conversationID: UUID) async throws -> [MessageModel] {
        let conversationMessages = try await self.getConversationMessages(conversationID: conversationID)
        if (!conversationMessages.messages.isEmpty) {
            // Ignoring the first message that it is from the system.
            return Array(conversationMessages.messages.dropFirst())
        }
        return []
    }
        
}
