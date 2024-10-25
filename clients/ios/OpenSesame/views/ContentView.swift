import SwiftUI

struct ContentView: View {
    @EnvironmentObject private var model: OpenSesameModel

    var body: some View {
        VStack {
            // Header Toolbar
            HeaderToolBar()

            ZStack {
                // Main Panel (ChatView is already a separate view)
                if(!self.model.showInfoPanel) {
                    ChatView(inProgress: self.model.selectedConversation != nil)
                        .frame(maxHeight: .infinity)
               }else {
                   InfoComponent(messageType: self.model.infoMessageType, message: self.model.infoMessage, messageDetails: self.model.infoMessageDetails)
                }

                // Floating Component
                if(self.model.isInCall && (!self.model.isBotReady || self.model.isBotSpeaking)){
                    FloatingComponent()
                    .frame(maxHeight: .infinity)
                }
            }
            .frame(maxHeight: .infinity)

            // Bottom Panel
            BottomPanel()
        }
        .preferredColorScheme(.dark)
        .background(Color.appBackground)
        .foregroundColor(Color.appForeground)
        .toast(message: self.model.toastMessage, isShowing: self.model.showToast)
    }
}

#Preview {
    let mockModel = OpenSesameMockModel()
    let result = ContentView().environmentObject(mockModel as OpenSesameModel)
    mockModel.startAudioLevelSimulation()
    return result
}
