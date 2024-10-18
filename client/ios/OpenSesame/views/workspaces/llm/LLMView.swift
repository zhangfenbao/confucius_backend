import SwiftUI

struct LLMView: View {
    
    @Binding var showingLLM: Bool
    @Binding var selectedLLMProvider: LLMProvider?
    @Binding var selectedLLMModel: LLMModel?
    
    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("LLM PROVIDER")) {
                    List(RTVIDefaultData.defaultLLMModels) { llmProvider in
                        Button(action: {
                            self.selectedLLMProvider = llmProvider
                            self.selectedLLMModel = llmProvider.models[0]
                        }) {
                            Text(llmProvider.label)
                                .frame(maxWidth: .infinity)
                                .padding()
                                .background(Color.settingsSectionBackground)
                                .cornerRadius(12)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 12)
                                    .stroke(llmProvider.id == self.selectedLLMProvider?.id ? Color.green : Color.clear, lineWidth: 2)
                                )
                        }
                    }
                }
                .listRowBackground(Color.settingsBackground)
                .foregroundColor(Color.settingsForeground)
                
                Section(header: Text("Model")) {
                    SelectLLMModel(
                        llmModelOptions: self.selectedLLMProvider?.models ?? [],
                        selectedLLMModel: self.$selectedLLMModel
                    )
                }
                .listRowBackground(Color.settingsBackground)
                .foregroundColor(Color.settingsForeground)
            }
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Text("Model")
                        .font(.headline)
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Close") {
                        self.showingLLM = false
                    }
                }
            }
            .scrollContentBackground(.hidden)
            .background(Color.settingsBackground)
            .foregroundColor(Color.settingsForeground)
        }
    }
}

#Preview {
    let llmProvider = RTVIDefaultData.defaultLLMModels[0]
    let result = LLMView(
        showingLLM: .constant(true),
        selectedLLMProvider: .constant(llmProvider),
        selectedLLMModel: .constant(llmProvider.models[0])
    )
    return result
}
