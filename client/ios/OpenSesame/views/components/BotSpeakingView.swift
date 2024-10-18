import SwiftUI

struct BotSpeakingView: View {
    
    @EnvironmentObject private var model: OpenSesameModel

    var body: some View {
        WaveformView(audioLevel: self.model.remoteAudioLevel)
        .overlay(
            Circle()
                .stroke(Color.white, lineWidth: 2)
        )
        .frame(width: 72, height: 72)
    }
}

#Preview {
    let mockModel = OpenSesameMockModel()
    let result = BotSpeakingView()
        .background(Color.appBackground)
        .environmentObject(mockModel as OpenSesameModel)
    mockModel.startAudioLevelSimulation()
    return result
}
