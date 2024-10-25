import SwiftUI

struct SelectVoice: View {
    
    @Binding var selectedVoice: Voice?
    let language: Language?
    
    var body: some View {
        SelectPickerView(
            options: language?.voices ?? [],
            selectedOption: self.$selectedVoice,
            titleKeyPath: \Voice.name, 
            title: "Default Voice"
        )
    }
}

#Preview {
    let ttsProvider = RTVIDefaultData.supportedTtsProviders[0]
    let language = ttsProvider.languages[0]
    let result = SelectVoice(
        selectedVoice: .constant(language.voices[0]),
        language: language
    )
    .background(Color.appBackground)
    return result
}
