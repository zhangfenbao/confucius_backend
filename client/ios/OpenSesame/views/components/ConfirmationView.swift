import SwiftUI

struct ConfirmationView: View {
    var title: String
    var message: String
    var onConfirm: () -> Void
    var onCancel: () -> Void
    
    var body: some View {
        VStack {
            Spacer()
            VStack(spacing: 20) {
                Text(title)
                    .font(.headline)
                Text(message)
                    .font(.subheadline)
                HStack {
                    Button("Cancel") {
                        onCancel()
                    }
                    .padding()
                    .background(Color.gray.opacity(0.2))
                    .cornerRadius(10)
                    Button("Confirm") {
                        onConfirm()
                    }
                    .padding()
                    .background(Color.deleteBackground.opacity(0.5))
                    .cornerRadius(10)
                    .foregroundColor(.white)
                }
            }
            .padding(40)
            .background(Color.settingsBackground)
            .foregroundColor(Color.settingsForeground)
            .cornerRadius(20)
            .shadow(radius: 10)
            Spacer()
        }
        .frame(maxWidth: .infinity)
        .background(Color.white.opacity(0.4))
    }
}

#Preview {
    ConfirmationView(
        title: "Confirm Deletion",
        message: "Are you sure you want to delete this conversation?",
        onConfirm: {},
        onCancel: {}
    )
}
