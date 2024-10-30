import ActivityKit
import RTVIClientIOS
import Foundation

final class ActivityManager {
    private(set) var activityID: String?

    static let shared = ActivityManager()
    private let initialContentState: ActivityContent<OpenSesameStatusAttributes.ContentState>

    init() {
        self.initialContentState = ActivityContent(
            state: OpenSesameStatusAttributes.ContentState(
                connectionStatus: TransportState.disconnected.description),
            staleDate: nil,
            relevanceScore: 0)
    }

    func start() async {
        await self.cancelAllRunningActivities()
        await self.startNewLiveActivity()
    }

    @MainActor
    private func startNewLiveActivity() async {
        let attributes = OpenSesameStatusAttributes(assistantName: "Open Sesame")

        let activity = try? Activity.request(
            attributes: attributes,
            content: self.initialContentState,
            pushType: .token
        )

        guard let activity = activity else { return }
        self.activityID = activity.id
    }

    func updateActivity(connectionStatus: String) {
        guard let activityID = self.activityID,
              let runningActivity = Activity<OpenSesameStatusAttributes>.activities.first(where: { $0.id == activityID }) else {
            return
        }
        let newContent = OpenSesameStatusAttributes.ContentState(connectionStatus: connectionStatus)
        DispatchQueue.main.async {
            Task {
                let activityContent = ActivityContent(state: newContent, staleDate: nil)
                await runningActivity.update(activityContent)
            }
        }
    }

    @MainActor
    func endActivity() async {
        guard let activityID = self.activityID,
              let runningActivity = Activity<OpenSesameStatusAttributes>.activities.first(where: { $0.id == activityID }) else {
            return
        }
        await runningActivity.end(self.initialContentState)
        self.activityID = nil
    }

    @MainActor
    func cancelAllRunningActivities() async {
        for activity in Activity<OpenSesameStatusAttributes>.activities {
            await activity.end(nil, dismissalPolicy: .immediate)
        }
        self.activityID = nil
    }

}
