import SwiftUI

struct WorkspaceItem: View {

    @EnvironmentObject private var model: OpenSesameModel

    let workspace: WorkspaceModel

    @Binding var showingWorkspace: Bool
    @Binding var showingWorkspaces: Bool

    @Binding var editedWorkspace: WorkspaceModel?

    var body: some View {
        HStack {
            Button(action: {
                self.model.selectWorkspace(workspace: workspace)
                self.showingWorkspaces = false
            }) {
                HStack {
                    Image(OpenSesameIcons.workspace)  // Workspace icon on the left
                        .resizable()
                        .frame(width: 40, height: 40)
                        .padding(.trailing, 10)

                    VStack(alignment: .leading) {
                        Text(self.workspace.title)
                            .font(.headline)
                        Text(self.workspace.getLLMModel()?.label ?? "")
                            .font(.subheadline)
                            .foregroundColor(.gray)
                    }
                    Spacer()  // Pushes everything to the left
                }
            }
            .padding()
            .background(Color.settingsSectionBackground)
            .cornerRadius(12)
            Button(action: {
                self.editedWorkspace = workspace
                self.showingWorkspace = true
            }) {
                Image(OpenSesameIcons.editWorkspace)
                    .resizable()
                    .frame(width: 24, height: 24)
                    .padding()
            }
        }
    }
}

#Preview {
    let mockModel = OpenSesameMockModel()
    let result = WorkspaceItem(
        workspace: OpenSesameMockModel.mockWorkspace,
        showingWorkspace: .constant(false),
        showingWorkspaces: .constant(true),
        editedWorkspace: .constant(nil)
    ).environmentObject(mockModel as OpenSesameModel)
    return result
}
