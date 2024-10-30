import SwiftUI

// Enum for different message types
enum MessageType {
    case info
    case warning
    case error
}

struct InfoComponent: View {
    var messageType: MessageType
    var message: String
    var messageDetails: String? = nil

    // Helper function to determine icon and colors based on the message type
    private func getMessageDetails(type: MessageType) -> (icon: String, foregroundColor: Color, backgroundColor: Color) {
        switch type {
        case .info:
            return ("info.circle.fill", .blue, Color.blue.opacity(0.2))
        case .warning:
            return ("exclamationmark.triangle.fill", .yellow, Color.yellow.opacity(0.2))
        case .error:
            return ("xmark.octagon.fill", .red, Color.red.opacity(0.2))
        }
    }

    var body: some View {
        // Get the icon and colors based on the message type
        let details = getMessageDetails(type: messageType)
        
        HStack {
            // Icon for the message type
            Image(systemName: details.icon)
                .resizable()
                .scaledToFit()
                .frame(width: 40, height: 40)
                .foregroundColor(details.foregroundColor)
            
            VStack(alignment: .leading) {
                // Primary message (bold)
                Text(message)
                    .font(.subheadline)
                    .fontWeight(.bold)
                    .foregroundColor(.gray)
                
                // Optional secondary message (details)
                if let messageDetails = self.messageDetails {
                    Text(messageDetails)
                        .font(.footnote)
                        .foregroundColor(.gray)
                        .padding(.top, 2)
                }
            }
        }
        .padding()
        .background(details.backgroundColor)
        .cornerRadius(10)
        .padding(.horizontal)
    }
}

#Preview {
    VStack(spacing: 20) {
        InfoComponent(messageType: .info, message: "Info message", messageDetails: "This is an informational message.")
        InfoComponent(messageType: .warning, message: "Warning message", messageDetails: "This is a warning message.")
        InfoComponent(messageType: .error, message: "Error message", messageDetails: "This is an error message with status code 500.")
    }
    .padding()
}
