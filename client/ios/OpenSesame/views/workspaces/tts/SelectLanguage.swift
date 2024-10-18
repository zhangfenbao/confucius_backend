import SwiftUI

struct SelectLanguage: View {
    
    @Binding var selectedLanguage: Language?
    let ttsProvider: TTSProvider?
    
    var body: some View {
        SelectPickerView(
            options: ttsProvider?.languages ?? [],
            selectedOption: self.$selectedLanguage,
            titleKeyPath: \Language.name,
            title: "Main language"
        )
    }
}

#Preview {
    let ttsProvider = RTVIDefaultData.supportedTtsProviders[0]
    let languages = ttsProvider.languages
    let result = SelectLanguage(
        selectedLanguage: .constant(languages[0]),
        ttsProvider: ttsProvider
    ).background(Color.appBackground)
    return result
}
