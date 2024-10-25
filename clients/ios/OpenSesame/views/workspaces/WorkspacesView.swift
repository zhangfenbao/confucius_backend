import SwiftUI

struct WorkspacesView: View {

    @EnvironmentObject private var model: OpenSesameModel

    @Binding var showingWorkspaces: Bool

    @State var showingWorkspace: Bool = false
    @State private var editedWorkspace: WorkspaceModel? = nil

    var body: some View {
        NavigationView {
            VStack {
                Text("Your Workspaces")
                .font(.title)
                .fontWeight(.bold)
                .padding()
                .frame(maxWidth: .infinity, alignment: .leading)

                ScrollView {
                    VStack {
                        // List of workspaces
                        ForEach(self.model.workspaces) { workspace in
                            WorkspaceItem(
                                workspace: workspace,
                                showingWorkspace: self.$showingWorkspace,
                                showingWorkspaces: self.$showingWorkspaces,
                                editedWorkspace: self.$editedWorkspace
                            )
                            .padding(.horizontal)
                        }
                    }
                    Button(action: {
                        self.editedWorkspace = nil
                        self.showingWorkspace = true
                    }) {
                        HStack {
                            Image(systemName: "plus.circle.fill")  // Icon for the button
                            Text("Create New Workspace")
                        }
                        .padding()
                        .font(.headline)
                        .cornerRadius(10)
                    }
                    .sheet(isPresented: self.$showingWorkspace) {
                        WorkspaceView(
                            workspace: self.$editedWorkspace,
                            showingWorkspace: self.$showingWorkspace
                        )
                    }
                    .padding(.top, 20)
                }
            }
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Text("Workspaces")
                        .font(.headline)
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Close") {
                        self.showingWorkspaces = false
                    }
                }
            }
            .onAppear {
                self.model.loadWorkspaces()
            }
            .frame(maxWidth:.infinity, maxHeight: .infinity)
            .background(Color.settingsBackground)
            .foregroundColor(Color.settingsForeground)
        }
        .background(Color.settingsBackground)
        .foregroundColor(Color.settingsForeground)
        .toast(message: self.model.toastMessage, isShowing: self.model.showToast)
    }

}

#Preview {
    let mockModel = OpenSesameMockModel()
    let result = WorkspacesView(showingWorkspaces: .constant(true))
        .environmentObject(mockModel as OpenSesameModel)
    return result
}
