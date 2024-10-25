import SwiftUI

struct FloatingComponent: View {

    @EnvironmentObject private var model: OpenSesameModel

    var body: some View {
        VStack {
            Spacer()
            HStack {
                if self.model.isBotSpeaking {
                    Spacer()
                    Button(action: {
                        self.model.interrupt()
                    }) {
                        Image(systemName: "pause.fill")
                            .font(.system(size: 40))
                            .foregroundColor(Color.appForeground)
                            .frame(width: 70, height: 70)
                            .background(Color.settingsBackground)
                            .clipShape(Circle())
                            .overlay(
                                Circle()
                                    .stroke(Color.white, lineWidth: 2)
                            )
                            .shadow(radius: 10)
                            .opacity(0.5)
                    }
                } else {
                    // Gray circle with loading icon when not connected
                    HStack {
                        ProgressView()
                            .progressViewStyle(CircularProgressViewStyle(tint: .white))
                            .scaleEffect(2) // Adjust size of the loading spinner
                            .padding()
                        Text(self.model.voiceClientStatus)
                            .foregroundColor(.white)
                            .font(.headline)
                    }
                    .frame(maxWidth: .infinity)
                    .background(Color.settingsBackground)
                    .cornerRadius(15)
                }
            }
            .padding([.horizontal])
        }
        .ignoresSafeArea()
    }
}

#Preview {
    let mockModel = OpenSesameMockModel()
    let result = FloatingComponent()
        .background(Color.appBackground)
        .environmentObject(mockModel as OpenSesameModel)
    mockModel.startAudioLevelSimulation()
    return result
}
