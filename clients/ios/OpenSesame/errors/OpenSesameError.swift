import Foundation

/// A protocol representing a base error occurring during an operation.
public protocol OpenSesameError: LocalizedError {
    /// A human-readable description of the error.
     var message: String { get }
}

public extension OpenSesameError {
    /// Provides a detailed description of the error, including any underlying error.
    var errorDescription: String? {
        return self.message
    }
}

/// Missing API Key error occurred..
public struct MissingAPIError: OpenSesameError {
    public let message: String

    public init(message: String) {
        self.message = message
    }
}

/// Error occurred inside RTVI
public struct RTVIError: OpenSesameError {
    public let message: String

    public init(message: String) {
        self.message = message
    }
}

/// Connection Error
public struct ConnectionError: OpenSesameError {
    public let message: String

    public init(message: String) {
        self.message = message
    }
}
