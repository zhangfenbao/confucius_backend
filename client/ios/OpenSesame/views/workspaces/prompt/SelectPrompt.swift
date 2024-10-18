import SwiftUI

struct SelectPrompt: View {
    
    @State var showingPrompt: Bool = false
    @Binding var systemPrompt: String
    
    var body: some View {
        CreatePickerView(
            title: "Prompt",
            value: ""
        )
        .onTapGesture {
            self.showingPrompt = true
        }
        .sheet(isPresented: self.$showingPrompt) {
            PromptView(showingPrompt: self.$showingPrompt, systemPrompt: self.$systemPrompt)
        }
    }
}

#Preview {
    let result = SelectPrompt(systemPrompt: .constant(""))
        .background(Color.appBackground)
    return result
}
