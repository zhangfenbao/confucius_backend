import SwiftUI

struct SelectStorage: View {
    
    @Binding var selectedStorage: Storage?
    
    var body: some View {
        SelectPickerView<Storage>(
            options: RTVIDefaultData.supportedStorages,
            selectedOption: self.$selectedStorage,
            titleKeyPath: \Storage.name,
            title: "Storage"
        )
    }
}

#Preview {
    let result = SelectStorage(
        selectedStorage: .constant(RTVIDefaultData.supportedStorages[0])
    ).background(Color.appBackground)
    return result
}
