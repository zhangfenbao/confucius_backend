import Foundation

class OpenSesameAPI {
    
    static let shared = OpenSesameAPI()
    
    private var baseURL: URL?
    private var bearerToken: String?
    
    init() {
        let currentSettings = SettingsManager.shared.getSettings()
        self.setBaseURL(currentSettings.openSesameURL)
        self.bearerToken = currentSettings.openSesameSecret
    }
    
    func setBaseURL(_ url: String) {
        let apiURL = url.hasSuffix("/api") ? url : url + "/api"
        self.baseURL = URL(string: apiURL )
    }
    
    func setBearerToken(_ token: String) {
        self.bearerToken = token
    }
    
    func createRequest(path: String, method: String, body: Data? = nil) -> URLRequest? {
        guard let url = baseURL?.appendingPathComponent(path) else {
            return nil
        }
        var request = URLRequest(url: url)
        request.httpMethod = method
        if let token = bearerToken {
            request.addValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        if let body = body {
            request.httpBody = body
            request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        }
        return request
    }

    func perform<T: Codable>(_ request: URLRequest?, completion: @escaping (Result<T, Error>) -> Void) {
        guard let request = request else {
            return
        }
        Logger.shared.info("OpenSesameAPI - Will request URL: \(String(describing: request.url))")
        let task = URLSession.shared.dataTask(with: request) { data, response, error in
            guard let httpResponse = response as? HTTPURLResponse else {
                completion(.failure(ConnectionError(message: "Not received an HTTP response!")))
                return
            }
            let validStatusCodes: Set<Int> = [200, 201, 404]
            if !validStatusCodes.contains(httpResponse.statusCode) {
                completion(.failure(ConnectionError(message: "Request failed with status \(httpResponse.statusCode)")))
                return
            }

            guard let data = data, error == nil else {
                completion(.failure(error!))
                return
            }
            
            do {
                if(T.self == Data.self) {
                    completion(.success(data as! T))
                } else {
                    Logger.shared.debug("OpenSesameAPI - received data: \(String(describing: String(data: data, encoding: .utf8)))")
                    let result = try JSONDecoder().decode(T.self, from: data)
                    Logger.shared.debug("OpenSesameAPI - json result: \(result)")
                    completion(.success(result))
                }
            } catch {
                Logger.shared.error("OpenSesameAPI - received error: \(error)")
                completion(.failure(error))
            }
        }
        task.resume()
    }
    
}
