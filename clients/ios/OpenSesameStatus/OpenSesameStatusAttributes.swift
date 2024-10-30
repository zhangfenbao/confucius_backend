import ActivityKit

struct OpenSesameStatusAttributes: ActivityAttributes {
    public struct ContentState: Codable, Hashable {
        var connectionStatus: String
    }

    var assistantName: String

}
