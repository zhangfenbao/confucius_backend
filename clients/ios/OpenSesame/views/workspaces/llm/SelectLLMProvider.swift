import SwiftUI

struct SelectLLMProvider: View {
    
    @State var showingLLM: Bool = false
    @Binding var selectedLLMProvider: LLMProvider?
    @Binding var selectedLLMModel: LLMModel?
    
    var body: some View {
        CreatePickerView(
            title: "Model",
            value: self.selectedLLMModel?.label ?? ""
        )
        .onTapGesture {
            self.showingLLM = true
        }
        .sheet(isPresented: self.$showingLLM) {
            LLMView(
                showingLLM: $showingLLM,
                selectedLLMProvider: $selectedLLMProvider,
                selectedLLMModel: $selectedLLMModel
            )
        }
    }
}

#Preview {
    let llmProvider = RTVIDefaultData.defaultLLMModels[0]
    let result = SelectLLMProvider(
        selectedLLMProvider: .constant(llmProvider),
        selectedLLMModel: .constant(llmProvider.models[0])
    ).background(Color.appBackground)
    return result
}
