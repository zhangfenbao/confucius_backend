import AppIntents

struct StartDailyBotIntent: LiveActivityIntent {
    
    static var openAppWhenRun: Bool = true
    static var title: LocalizedStringResource = "Start Open Sesame"
    static var authenticationPolicy = IntentAuthenticationPolicy.alwaysAllowed

    @MainActor
    func perform() async throws -> some IntentResult {
        return .result()
    }

}
