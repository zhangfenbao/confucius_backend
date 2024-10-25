import SwiftUI

@main
struct OpenSesameApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

    @StateObject var model = OpenSesameModel.shared
    
    init() {
        setAppLogLevel(.info)
    }

    var body: some Scene {
        WindowGroup {
            if self.model.needsOnboard {
                OnboardingView().environmentObject(self.model)
            } else {
                ContentView().environmentObject(self.model)
            }
        }
    }

}
