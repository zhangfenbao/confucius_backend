import Foundation

extension OpenSesameAPI {

    func getConversationsByWorkspace(workspaceID: UUID, limit: Int = 20, offset: Int = 0, completion: @escaping (Result<[ConversationModel], Error>) -> Void) {
        let request = createRequest(path: "conversations/\(workspaceID.uuidString)?limit=\(limit)&offset=\(offset)", method: "GET")
        perform(request, completion: completion)
    }
    
    func getAllConversationsByWorkspace(workspaceID: UUID, completion: @escaping (Result<[ConversationModel], Error>) -> Void) {
        let request = createRequest(path: "conversations/\(workspaceID.uuidString)", method: "GET")
        perform(request, completion: completion)
    }
    
    func getAllConversationsByWorkspace(workspaceID: UUID) async throws -> [ConversationModel] {
        return try await withCheckedThrowingContinuation { continuation in
            self.getAllConversationsByWorkspace(workspaceID: workspaceID) { result in
                switch result {
                case .success(let conversations):
                    continuation.resume(returning: conversations)
                case .failure(let error):
                    continuation.resume(throwing: error)
                }
            }
        }
    }

    func createConversation(conversation: ConversationCreateModel, completion: @escaping (Result<ConversationModel, Error>) -> Void) {
        guard let body = try? JSONEncoder().encode(conversation) else {
            completion(.failure(NSError(domain: "EncodingError", code: -1, userInfo: nil)))
            return
        }
        let request = createRequest(path: "conversations", method: "POST", body: body)
        perform(request, completion: completion)
    }
    
    func createConversation(conversation: ConversationCreateModel) async throws -> ConversationModel {
        return try await withCheckedThrowingContinuation { continuation in
            self.createConversation(conversation: conversation) { result in
                switch result {
                case .success(let conversation):
                    continuation.resume(returning: conversation)
                case .failure(let error):
                    continuation.resume(throwing: error)
                }
            }
        }
    }

    func deleteConversation(conversationID: UUID, completion: @escaping (Result<Void, Error>) -> Void) {
        let request = createRequest(path: "conversations/\(conversationID)", method: "DELETE")
        perform(request) { (result: Result<Data, Error>) in
            switch result {
            case .success(_):
                completion(.success(()))
            case .failure(let error):
                completion(.failure(error))
            }
        }
    }
    
    func deleteConversation(conversationID: UUID) async throws -> Void {
        return try await withCheckedThrowingContinuation { continuation in
            self.deleteConversation(conversationID: conversationID) { result in
                switch result {
                case .success():
                    continuation.resume()
                case .failure(let error):
                    continuation.resume(throwing: error)
                }
            }
        }
    }

    func getConversationMessages(conversationID: UUID, completion: @escaping (Result<ConversationMessagesModel, Error>) -> Void) {
        let request = createRequest(path: "conversations/\(conversationID.uuidString)/messages", method: "GET")
        perform(request, completion: completion)
    }
    
    func getConversationMessages(conversationID: UUID) async throws -> ConversationMessagesModel {
        return try await withCheckedThrowingContinuation { continuation in
            self.getConversationMessages(conversationID: conversationID) { result in
                switch result {
                case .success(let conversations):
                    continuation.resume(returning: conversations)
                case .failure(let error):
                    continuation.resume(throwing: error)
                }
            }
        }
    }
    
}
