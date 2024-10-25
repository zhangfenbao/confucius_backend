import SwiftUI

struct ToastModifier: ViewModifier {
    var message: String?
    var isShowing: Bool

    func body(content: Content) -> some View {
        ZStack {
            content
            if self.isShowing, let message = self.message {
                VStack {
                    Text(message)
                        .padding()
                        .background(Color.toastMessage.opacity(0.95))
                        .foregroundColor(.black)
                        .cornerRadius(8)
                        .transition(.slide)
                        .padding(.top, 50)
                    Spacer()
                }
                .animation(.easeInOut(duration: 0.5), value: self.isShowing)
            }
        }
    }
}

extension View {
    func toast(message: String?, isShowing: Bool) -> some View {
        self.modifier(ToastModifier(message: message, isShowing: isShowing))
    }
}
