import UIKit
import ActivityKit

class AppDelegate: NSObject, UIApplicationDelegate {

    func applicationWillTerminate(_ application: UIApplication) {
        let semaphore = DispatchSemaphore(value: 0)
        // Forcing to close all live activities when we destroy the app
        Task.detached {
            for activity in Activity<OpenSesameStatusAttributes>.activities {
                await activity.end(nil, dismissalPolicy: .immediate)
            }
            semaphore.signal()
        }
        semaphore.wait()
    }

}
