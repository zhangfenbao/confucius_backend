import SwiftUI

struct SelectLLMModel: View {
    
    let llmModelOptions: [LLMModel]
    @Binding var selectedLLMModel: LLMModel?
    
    var body: some View {
        SelectPickerView<LLMModel>(
            options: self.llmModelOptions,
            selectedOption: self.$selectedLLMModel,
            titleKeyPath: \LLMModel.label,
            title: "Model"
        )
    }
}

#Preview {
    let llmModels = RTVIDefaultData.defaultLLMModels[0].models
    let result = SelectLLMModel(
        llmModelOptions: llmModels,
        selectedLLMModel: .constant(llmModels[0])
    )
    .background(Color.appBackground)
    return result
}
