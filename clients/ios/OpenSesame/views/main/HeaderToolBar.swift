import SwiftUI

struct HeaderToolBar: View {
    @State private var showingSettings = false
    @State private var showingWorkspaces = false
    @State private var showingTray = false

    @EnvironmentObject private var model: OpenSesameModel

    var body: some View {
        HStack {
            Button(action: {
                self.showingTray = true
            }) {
                Image(OpenSesameIcons.tray)
                    .resizable()
                    .frame(width: 24, height: 24)
            }
            .sheet(isPresented: self.$showingTray) {
                TrayView(showingTray: self.$showingTray, showingWorkspaces: self.$showingWorkspaces)
            }

            Spacer()

            Button(action: {
                self.showingWorkspaces = true
            }) {
                HStack {
                    Text(self.model.selectedWorkspaceName)
                    Image(OpenSesameIcons.rightArrow)
                        .resizable()
                        .frame(width: 5, height: 10)
                }
                .fontWeight(.bold)
                .padding()
            }
            .sheet(isPresented: self.$showingWorkspaces) {
                WorkspacesView(showingWorkspaces: self.$showingWorkspaces)
            }

            Spacer()

            Button(action: {
                self.showingSettings = true
            }) {
                Image(OpenSesameIcons.settings)
                    .resizable()
                    .frame(width: 24, height: 24)
            }
            .sheet(isPresented: self.$showingSettings) {
                SettingsView(showingSettings: self.$showingSettings)
            }
        }
        .padding()
    }
}

#Preview {
    let mockModel = OpenSesameMockModel()
    let result = HeaderToolBar()
        .background(Color.appBackground)
    .environmentObject(mockModel as OpenSesameModel)
    return result
}
