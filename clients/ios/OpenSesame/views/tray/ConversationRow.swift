import SwiftUI

// Row for displaying each conversation
struct ConversationRow: View {
    @EnvironmentObject private var model: OpenSesameModel
    @Binding var showingTray: Bool

    var conversation: ConversationModel

    var body: some View {
        Button(action: {
            self.model.selectConversation(conversation: conversation)
            self.showingTray = false
        }) {
            HStack {
                Image(systemName: "message.fill")
                    .resizable()
                    .frame(width: 24, height: 24)
                Text(self.conversation.title ?? "")
                    .padding(.leading, 8)
                Spacer()
            }
        }
        .frame(maxWidth: .infinity)
        .padding(12)
    }
}

#Preview {
    let mockModel = OpenSesameMockModel()
    let result = ConversationRow(
        showingTray: .constant(true), conversation: OpenSesameMockModel.mockConversation
    )
    .environmentObject(mockModel as OpenSesameModel)
    .background(Color.appBackground)
    return result
}
