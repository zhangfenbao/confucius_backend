import SwiftUI

struct BottomPanel: View {
    // prod
    @EnvironmentObject private var model: OpenSesameModel

    @State private var message = ""

    var body: some View {
        HStack {
            if !self.model.isInCall {
                ZStack {
                    RoundedRectangle(cornerRadius: 8)
                        .fill(Color.black)
                    TextField("", text: $message)
                        .placeholder(when: message.isEmpty) {
                            Text("Type a message...").foregroundColor(.gray)
                        }
                        .padding(10)
                        .onSubmit {
                            self.model.sendTextMessage(textMessage: self.message)
                            self.message = ""
                        }
                }
                .frame(height: 38)
            } else {
                MicrophoneView(audioLevel: self.model.localAudioLevel, isMuted: !self.model.isMicEnabled)
                    .padding([.horizontal])
                    .onTapGesture {
                        self.model.toggleMicInput()
                    }
                Spacer()
            }

            // Custom Toggle for Keyboard and Voice Icons
            HStack {
                Button(action: {
                    self.model.disconnect()
                }) {
                    Image("keyboard")
                        .padding(.horizontal, 12)
                        .padding(.vertical, 5)
                }
                .background(self.model.isInCall ? Color.settingsBackground : .white)
                .cornerRadius(15)
                .padding(.vertical, 5)

                Button(action: {
                    self.model.connect()
                }) {
                    Image("speech")
                        .padding(.horizontal, 12)
                        .padding(.vertical, 5)
                }
                .background(self.model.isInCall ? .white : Color.settingsBackground)
                .cornerRadius(15)
                .padding(.vertical, 5)
            }
            .background(Color.settingsBackground)
            .cornerRadius(15)
            .padding(.horizontal, 5)
        }
        .padding(5)
        .background(
            RoundedRectangle(cornerRadius: 15)
                .stroke(Color.bottomPanelBorder, lineWidth: 2) // Use stroke to create the border with rounded corners
        )
        .cornerRadius(15)
        .padding()
    }

}

#Preview {
    let mockModel = OpenSesameMockModel()
    let result =
    BottomPanel()
        .background(Color.appBackground)
        .foregroundColor(Color.appForeground)
        .environmentObject(mockModel as OpenSesameModel)
    return result
}
