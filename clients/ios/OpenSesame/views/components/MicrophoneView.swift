import SwiftUI

struct MicrophoneView: View {
    var audioLevel: Float // Current audio level
    var isMuted: Bool // Muted state

    var body: some View {
        HStack {
            Image(systemName: self.isMuted ? "mic.slash.fill" : "mic.fill")
                .frame(width: 24, height: 24)
            if !self.isMuted {
                WaveformView(autoIncreaseAudioLevel: true, audioLevelMinThreshold: 0.1, audioLevel: self.audioLevel)
            }
        }
        .frame(width: 100, height: 38)
        .background(self.isMuted ? Color.disabledMic : Color.settingsBackground)
        .cornerRadius(15)
    }
}

#Preview {
    //TODO: refactor this component so it looks like the picture
    MicrophoneView(audioLevel: 0.001, isMuted: false)
        .background(Color.appBackground)
        .foregroundColor(Color.appForeground)
}
