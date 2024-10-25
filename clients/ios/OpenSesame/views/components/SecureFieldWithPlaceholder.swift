import SwiftUI

// SwiftUI doesn't have a built-in way to directly change the placeholder text color in SecureField.
struct SecureFieldWithPlaceholder: View {
    var placeholder: String
    @Binding var text: String
    var foregroundColor: Color = .white
    
    var body: some View {
        ZStack(alignment: .trailing) {
            if self.text.isEmpty {
                Text(self.placeholder)
                    .foregroundColor(self.foregroundColor) // Placeholder color
            }
            SecureField("", text: self.$text)
                .foregroundColor(self.foregroundColor) // Actual text color
        }
    }
}
