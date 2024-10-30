import SwiftUI
import MarkdownUI

struct ChatView: View {
    @EnvironmentObject private var model: OpenSesameModel
    let inProgress: Bool

    var body: some View {
        VStack {
            if self.model.messages.isEmpty {
                let label = self.inProgress ? "No messages available!" : "Press connect to start a new chat!"
                VStack {
                    Spacer()
                    Image(OpenSesameIcons.openSesame)
                        .resizable()
                        .frame(width: 24, height: 24)
                    Text(label)
                        .padding()
                    Spacer()
                }
                .frame(maxHeight: .infinity)
            } else {
                ScrollViewReader { scrollViewProxy in
                    ScrollView {
                        VStack(spacing: 16) {
                            ForEach(self.model.messages) { message in
                                MessageView(message: message) {
                                    // This closure gets called when the message content changes
                                    scrollToLastMessage(scrollViewProxy)
                                }
                                .frame(maxWidth: .infinity, alignment: !message.fromBot ? .trailing : .leading)
                                .padding(.horizontal)
                                .id(message.id)
                            }
                        }
                        .onChange(of: self.model.messages) { _, _ in
                            scrollToLastMessage(scrollViewProxy)
                        }
                    }
                    .onAppear {
                        scrollToLastMessage(scrollViewProxy)
                    }
                }
            }
        }
        .edgesIgnoringSafeArea(.bottom)
    }

    private func scrollToLastMessage(_ scrollViewProxy: ScrollViewProxy) {
        if let lastMessageId = self.model.messages.last?.id {
            withAnimation {
                scrollViewProxy.scrollTo(lastMessageId, anchor: .bottom)
            }
        }
    }
}


struct MessageView: View {
    @ObservedObject var message: LiveMessage
    var onContentChange: () -> Void

    var body: some View {
        HStack {
            if self.message.fromBot {
                if self.message.isBotSpeaking {
                    BotSpeakingView()
                } else {
                    Image(OpenSesameIcons.openSesame)
                        .resizable()
                        .frame(width: 24, height: 24)
                }
            }
            Markdown(self.message.content)
                .padding()
                .markdownTextStyle(\.text) {
                    ForegroundColor(.white)
                }
                .background(self.message.fromBot ? Color.appBackground : Color.settingsBackground)
                .cornerRadius(15)
                .overlay(
                    RoundedRectangle(cornerRadius: 15)
                        .stroke(Color.gray.opacity(0.5), lineWidth: 1)
                )
        }
        .padding(!self.message.fromBot ? .leading : .trailing, 40)
        .onChange(of: self.message.content) { _, _ in
            onContentChange()
        }
    }
}



#Preview {
    let mockModel = OpenSesameMockModel()
    let result = 
        ChatView(inProgress: false)
        .background(Color.appBackground)
        .environmentObject(mockModel as OpenSesameModel)
    return result
}
