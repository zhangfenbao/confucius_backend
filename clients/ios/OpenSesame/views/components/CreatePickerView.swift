import SwiftUI

struct CreatePickerView: View {
    // Customizable parameters
    let title: String
    let value: String
    let leadingIcon: String = OpenSesameIcons.configWorkspace
    let trailingIcon: String = OpenSesameIcons.rightArrow
    
    var body: some View {
        HStack {
            Image(self.leadingIcon)
            Text(self.title)
                .frame(minWidth: 100, alignment: .leading)
            Text(self.value)
                .foregroundColor(Color.settingsItemForeground)
                .frame(maxWidth: .infinity, alignment: .trailing)
            Image(self.trailingIcon)
        }
        .contentShape(Rectangle())
    }
}
