import Foundation

extension OpenSesameAPI {
    
    func getWorkspaces(completion: @escaping (Result<[WorkspaceModel], Error>) -> Void) {
        let request = self.createRequest(path: "workspaces", method: "GET")
        self.perform(request, completion: completion)
    }
    
    func getAllWorkspaces() async throws -> [WorkspaceModel] {
        return try await withCheckedThrowingContinuation { continuation in
            self.getWorkspaces { result in
                switch result {
                case .success(let workspaces):
                    continuation.resume(returning: workspaces)
                case .failure(let error):
                    continuation.resume(throwing: error)
                }
            }
        }
    }
    
    func createWorkspace(workspace: WorkspaceUpdateModel, completion: @escaping (Result<WorkspaceModel, Error>) -> Void) {
        let encoder = JSONEncoder()
        guard let body = try? encoder.encode(workspace) else {
            completion(.failure(NSError(domain: "EncodingError", code: -1, userInfo: nil)))
            return
        }
        let request = self.createRequest(path: "workspaces", method: "POST", body: body)
        self.perform(request, completion: completion)
    }
    
    func createWorkspace(workspace: WorkspaceUpdateModel) async throws -> WorkspaceModel {
        return try await withCheckedThrowingContinuation { continuation in
            self.createWorkspace(workspace: workspace) { result in
                switch result {
                case .success(let newWorkspace):
                    continuation.resume(returning: newWorkspace)
                case .failure(let error):
                    continuation.resume(throwing: error)
                }
            }
        }
    }
    
    func getWorkspace(workspaceID: String, completion: @escaping (Result<WorkspaceModel, Error>) -> Void) {
        let request = createRequest(path: "workspaces/\(workspaceID)", method: "GET")
        perform(request, completion: completion)
    }

    func updateWorkspace(workspaceID: String, workspace: WorkspaceUpdateModel, completion: @escaping (Result<WorkspaceModel, Error>) -> Void) {
        guard let body = try? JSONEncoder().encode(workspace) else {
            completion(.failure(NSError(domain: "EncodingError", code: -1, userInfo: nil)))
            return
        }
        let request = createRequest(path: "workspaces/\(workspaceID)", method: "PUT", body: body)
        perform(request, completion: completion)
    }
    
    func updateWorkspace(workspaceID: UUID, workspace: WorkspaceUpdateModel) async throws -> WorkspaceModel {
        return try await withCheckedThrowingContinuation { continuation in
            self.updateWorkspace(workspaceID: workspaceID.uuidString, workspace: workspace) { result in
                switch result {
                case .success(let newWorkspace):
                    continuation.resume(returning: newWorkspace)
                case .failure(let error):
                    continuation.resume(throwing: error)
                }
            }
        }
    }

    func deleteWorkspace(workspaceID: UUID, completion: @escaping (Result<Void, Error>) -> Void) {
        let request = createRequest(path: "workspaces/\(workspaceID.uuidString)", method: "DELETE")
        perform(request) { (result: Result<Data, Error>) in
            switch result {
            case .success(_):
                completion(.success(()))
            case .failure(let error):
                completion(.failure(error))
            }
        }
    }
    
    func deleteWorkspace(workspaceID: UUID) async throws -> Void {
        return try await withCheckedThrowingContinuation { continuation in
            self.deleteWorkspace(workspaceID: workspaceID) { result in
                switch result {
                case .success():
                    continuation.resume()
                case .failure(let error):
                    continuation.resume(throwing: error)
                }
            }
        }
    }
    
}
