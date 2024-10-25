import RTVIClientIOS
import Foundation

extension OpenSesameAPI {
    
    func createSessionToken(username: String, password: String, completion: @escaping (Result<SessionTokenResponse, Error>) -> Void) {
        let bodyJson: Value = Value.object([
            "username" : Value.string(username),
            "password" : Value.string(password)
        ])
        guard let body = try? JSONEncoder().encode(bodyJson) else {
            completion(.failure(NSError(domain: "EncodingError", code: -1, userInfo: nil)))
            return
        }
        let request = createRequest(path: "users/session", method: "POST", body: body)
        perform(request, completion: completion)
    }
    
    func createSessionToken(username: String, password: String) async throws -> SessionTokenResponse {
        return try await withCheckedThrowingContinuation { continuation in
            self.createSessionToken(username: username, password: password) { result in
                switch result {
                case .success(let sessionToken):
                    continuation.resume(returning: sessionToken)
                case .failure(let error):
                    continuation.resume(throwing: error)
                }
            }
        }
    }
    
}
