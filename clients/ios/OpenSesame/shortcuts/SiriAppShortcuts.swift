import AppIntents

struct SiriAppShortcuts: AppShortcutsProvider {
    @AppShortcutsBuilder
    static var appShortcuts: [AppShortcut] {
        AppShortcut(
            intent: StartDailyBotIntent(),
            phrases: ["connect me with \(.applicationName)", "start \(.applicationName)"],
            shortTitle: "Start Open Sesame",
            systemImageName: "person"
        )
    }
}
