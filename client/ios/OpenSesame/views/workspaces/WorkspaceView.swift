import SwiftUI

struct WorkspaceView: View {

    @EnvironmentObject private var model: OpenSesameModel

    @Binding var workspace: WorkspaceModel?
    @State private var showDeleteConfirmation: Bool = false

    @Binding var showingWorkspace: Bool

    @State private var selectedLLMProvider: LLMProvider?
    @State private var selectedLLMModel: LLMModel?

    @State var selectedTTSProvider: TTSProvider? = RTVIDefaultData.defaultTTSProvider
    @State var selectedLanguage: Language? = RTVIDefaultData.defaultLanguage
    @State var selectedVoice: Voice? = RTVIDefaultData.defaultTTSVoice
    @State var selectedStorage: Storage? = RTVIDefaultData.supportedStorages[0]
    @State var systemPrompt: String = RTVIDefaultData.defaultPrompt
    
    @State var workspaceName: String = ""
    @State var isConversationalWorkspace: Bool = true

    var body: some View {
        ZStack {
            
            NavigationView {
                Form {
                    // Configuration Section
                    Section(header: Text("Configuration")) {
                        VStack {
                            SelectLLMProvider(
                                selectedLLMProvider: self.$selectedLLMProvider,
                                selectedLLMModel: self.$selectedLLMModel
                            )
                            Divider().background(Color.gray)
                            SelectPrompt(systemPrompt: self.$systemPrompt)
                            Divider().background(Color.gray)
                            SelectStorage(selectedStorage: self.$selectedStorage)
                            
                        }
                    }
                    .listRowBackground(Color.settingsSectionBackground)
                    .foregroundColor(Color.settingsForeground)
                    
                    // Voice Settings Section
                    Section(header: Text("Voice Settings")) {
                        VStack {
                            SelectTTSProvider(selectedTTSProvider: self.$selectedTTSProvider)
                                .onChange(of: selectedTTSProvider) { old, new in
                                    self.selectedLanguage = new?.languages[0]
                                    self.selectedVoice = self.selectedLanguage?.voices[0]
                                }
                            Divider().background(Color.gray)
                            SelectLanguage(selectedLanguage: self.$selectedLanguage, ttsProvider: self.selectedTTSProvider)
                                .onChange(of: selectedLanguage) { old, new in
                                    self.selectedVoice = new?.voices[0]
                                }
                            Divider().background(Color.gray)
                            SelectVoice(selectedVoice: self.$selectedVoice, language: self.selectedLanguage)
                        }
                    }
                    .listRowBackground(Color.settingsSectionBackground)
                    .foregroundColor(Color.settingsForeground)
                    
                    // Workspace Options Section
                    Section(header: Text("Workspace Options")) {
                        VStack {
                            HStack {
                                Image(OpenSesameIcons.configWorkspace)
                                Text("Name").frame(minWidth: 100, alignment: .leading)
                                TextField("Workspace Name", text: self.$workspaceName)
                                    .multilineTextAlignment(.trailing)
                                    .disableAutocorrection(true)
                            }
                            Divider()
                                .background(Color.gray)
                            HStack {
                                Image(OpenSesameIcons.configWorkspace)
                                Toggle("Conversational", isOn: self.$isConversationalWorkspace)
                            }
                            Text("A conversational workspace displays words as the bot speaks. If not selected, the full LLM output is shown all at once.")
                                .font(.footnote)
                                .foregroundColor(.gray)
                                .padding(.top, 4)
                        }
                    }
                    .listRowBackground(Color.settingsSectionBackground)
                    .foregroundColor(Color.settingsForeground)
                    
                    // Save Workspace Button
                    Section {
                        Button(action: {
                            self.saveWorkspace()
                        }) {
                            Text("Save")
                                .foregroundColor(Color.black)
                        }
                        .frame(maxWidth: .infinity)
                    }
                    .listRowBackground(Color.white.opacity(0.8))
                    
                    // Delete Workspace Button
                    if (self.workspace?.id != nil) {
                        Section {
                            Button(action: {
                                self.showDeleteConfirmation = true
                            }) {
                                Text("Delete Workspace")
                                    .foregroundColor(Color.deleteForeground)
                            }
                            .frame(maxWidth: .infinity)
                        }
                        .listRowBackground(Color.deleteBackground.opacity(0.2))
                    }
                }
                .toolbar {
                    ToolbarItem(placement: .navigationBarLeading) {
                        Text(self.workspace?.id != nil ? "Edit Workspace": "Create Workspace")
                            .font(.headline)
                    }
                    ToolbarItem(placement: .navigationBarTrailing) {
                        Button("Close") {
                            self.showingWorkspace = false
                        }
                    }
                }
                .onAppear {
                    self.loadWorkspaceSettings()
                }
                .scrollContentBackground(.hidden)
                .background(Color.settingsBackground)
                .foregroundColor(Color.settingsForeground)
            }
            
            if self.showDeleteConfirmation {
                ConfirmationView(
                    title: "Confirm Deletion",
                    message: "Are you sure you want to delete this workspace?",
                    onConfirm: {
                        self.showDeleteConfirmation = false
                        self.deleteWorkspace()
                    },
                    onCancel: {
                        self.showDeleteConfirmation = false
                    }
                )
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .transition(.scale)
                .zIndex(1)
            }
        }
    }


    private func loadWorkspaceSettings() {
        guard let workspace = self.workspace else {
            return
        }
        self.workspaceName = workspace.title
        self.selectedLLMProvider = workspace.getLLMProvider()
        self.selectedLLMModel = workspace.getLLMModel()
        //TODO: need to improve this, should not be a single value
        self.systemPrompt = workspace.getSystemPrompt()
        self.selectedTTSProvider = workspace.getTTSProvider()
        self.selectedLanguage = workspace.getLanguage()
        self.selectedVoice = workspace.getVoice()
        self.isConversationalWorkspace = workspace.config.interactionMode == .informational ? false : true
    }

    private func deleteWorkspace() {
        guard let workspaceId = self.workspace?.id else {
            return
        }
        self.model.deleteWorkspace(workspaceId: workspaceId)
        self.showingWorkspace = false
    }

    private func saveWorkspace() {
        guard let selectedLLMProvider = self.selectedLLMProvider,
              let selectedLLMModel = self.selectedLLMModel else {
            self.model.showToast(message: "Select the model!")
            return
        }
        
        let newWorkspace = RTVIHelper.createWorkspaceModel(
            title: self.workspaceName,
            prompt: self.systemPrompt,
            llmProvider: selectedLLMProvider,
            llmModel: selectedLLMModel,
            ttsProvider: self.selectedTTSProvider!,
            ttsLanguage: self.selectedLanguage!,
            ttsVoice: self.selectedVoice!,
            interactionMode: self.isConversationalWorkspace ? .conversational : .informational
        )
        if self.workspace != nil {
            // Updating the workspace
            self.model.updateWorkspace(workspaceId: self.workspace!.id, workspace: newWorkspace)
        } else {
            // Creating a new workspace
            self.model.createWorkspace(newWorkspace: newWorkspace)
        }
        self.showingWorkspace = false
    }

}

#Preview {
    let mockModel = OpenSesameMockModel()
    let result = WorkspaceView(workspace: .constant(OpenSesameMockModel.mockWorkspace), showingWorkspace: .constant(true))
        .environmentObject(mockModel as OpenSesameModel)
    return result
}
