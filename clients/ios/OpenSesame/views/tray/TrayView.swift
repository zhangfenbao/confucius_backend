import SwiftUI

struct TrayView: View {

    @EnvironmentObject private var model: OpenSesameModel

    @Binding var showingTray: Bool
    @Binding var showingWorkspaces: Bool

    var body: some View {
        NavigationView {
            VStack {
                // Header with title and icon
                HStack {
                    Image(OpenSesameIcons.workspace)
                        .resizable()
                        .frame(width: 48, height: 48)
                    Text(self.model.selectedWorkspaceName)
                        .font(.title2)
                        .bold()
                    Spacer()
                }
                .padding(.horizontal)

                // Section 1: Buttons (New Chat and Workspaces)
                VStack() {
                    Button(action: {
                        self.model.startNewConversation()
                        self.showingTray = false
                    }) {
                        HStack {
                            Image(OpenSesameIcons.newChat)
                                .resizable()
                                .frame(width: 24, height: 24)
                            Text("New Chat")
                            Spacer()
                        }
                        .padding()
                    }
                    .frame(maxWidth: .infinity)

                    Button(action: {
                        self.showingWorkspaces = true
                    }) {
                        HStack {
                            Image(OpenSesameIcons.workspaces)
                                .resizable()
                                .frame(width: 24, height: 24)
                            Text("Workspaces")
                            Spacer()
                        }
                        .padding()
                    }
                    .frame(maxWidth: .infinity)
                    .sheet(isPresented: self.$showingWorkspaces) {
                        WorkspacesView(showingWorkspaces: self.$showingWorkspaces)
                    }
                }
                .padding()

                // Separator Line
                Divider()
                    .background(Color.gray)
                    .padding(.horizontal)

                // Section 2: Conversations List
                ScrollView {
                    VStack(alignment: .leading) {
                        let groupedConversations = OpenSesameHelper.getGroupedConversations(conversations: self.model.conversations)
                        Group {
                            // Today
                            if let todayConversations = groupedConversations["Today"], !todayConversations.isEmpty {
                                Text("Today")
                                    .font(.headline)
                                    .padding(.vertical, 8)
                                ForEach(todayConversations) { conversation in
                                    ConversationRow(showingTray:$showingTray, conversation: conversation)
                                }
                            }

                            // Yesterday
                            if let yesterdayConversations = groupedConversations["Yesterday"], !yesterdayConversations.isEmpty {
                                Divider()
                                Text("Yesterday")
                                    .font(.headline)
                                    .padding(.vertical, 8)
                                ForEach(yesterdayConversations) { conversation in
                                    ConversationRow(showingTray:$showingTray, conversation: conversation)
                                }
                            }

                            // Previous
                            if let previousConversations = groupedConversations["Previous"], !previousConversations.isEmpty {
                                Divider()
                                Text("Previous")
                                    .font(.headline)
                                    .padding(.vertical, 8)
                                ForEach(previousConversations) { conversation in
                                    ConversationRow(showingTray:$showingTray, conversation: conversation)
                                }
                            }
                        }
                    }
                    .padding()
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Color.settingsBackground)
            .foregroundColor(Color.settingsForeground)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Close") {
                        self.showingTray = false
                    }
                }
            }
        }
    }
}

#Preview {
    let mockModel = OpenSesameMockModel()
    let result = TrayView(
        showingTray: .constant(false),
        showingWorkspaces: .constant(false)
    )
    .environmentObject(mockModel as OpenSesameModel)
    .background(Color.appBackground)
    return result
}
