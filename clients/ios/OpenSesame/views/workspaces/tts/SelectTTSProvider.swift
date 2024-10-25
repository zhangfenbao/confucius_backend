import SwiftUI

struct SelectTTSProvider: View {
    
    @Binding var selectedTTSProvider: TTSProvider?
    
    var body: some View {
        SelectPickerView(
            options: RTVIDefaultData.supportedTtsProviders,
            selectedOption: self.$selectedTTSProvider,
            titleKeyPath: \TTSProvider.label,
            title: "TTS Provider"
        )
    }
}

#Preview {
    let result = SelectTTSProvider(
        selectedTTSProvider: .constant(RTVIDefaultData.supportedTtsProviders[0])
    ).background(Color.appBackground)
    return result
}
