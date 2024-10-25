import SwiftUI

struct SelectPickerView<T: Identifiable>: View {
    let options: [T]
    @Binding var selectedOption: T?
    
    // KeyPath to specify which property of the object to display as text
    let titleKeyPath: KeyPath<T, String>
    
    @State private var showDialog: Bool = false
    
    // Customizable parameters like title and icons
    let title: String
    let leadingIcon: String = OpenSesameIcons.configWorkspace
    let trailingIcon: String = OpenSesameIcons.selectDropDown
    
    var body: some View {
        HStack {
            Image(self.leadingIcon)
            Text(self.title)
                .frame(minWidth: 100, alignment: .leading)
            Text(self.selectedOption?[keyPath: self.titleKeyPath] ?? "")
                .foregroundColor(Color.settingsItemForeground)
                .frame(maxWidth: .infinity, alignment: .trailing)
            Image(self.trailingIcon)
        }
        .onTapGesture {
            self.showDialog = true
        }
        .confirmationDialog("Select an Option", isPresented: self.$showDialog, titleVisibility: .visible) {
            ForEach(self.options) { option in
                Button(option[keyPath: self.titleKeyPath]) {
                    self.showDialog = false
                    self.selectedOption = option
                }
            }
            Button("Cancel", role: .cancel) {}
        }
    }
}
