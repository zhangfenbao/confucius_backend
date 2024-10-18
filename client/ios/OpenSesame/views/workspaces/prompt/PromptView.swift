import SwiftUI

struct PromptView: View {
    
    @Binding var showingPrompt: Bool
    @Binding var systemPrompt: String
    
    var body: some View {
        NavigationView {
            Form {
                // Configuration Section
                Section(header: Text("System")) {
                    TextEditor(text: self.$systemPrompt)
                    .frame(height: 200)
                }
                .listRowBackground(Color.settingsSectionBackground)
                .foregroundColor(Color.settingsForeground)
            }
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Text("Prompt")
                        .font(.headline)
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Close") {
                        self.showingPrompt = false
                    }
                }
            }
            .scrollContentBackground(.hidden)
            .background(Color.settingsBackground)
            .foregroundColor(Color.settingsForeground)
        }
    }
}

#Preview {
    let result = PromptView(showingPrompt: .constant(true), systemPrompt: .constant(""))
    return result
}
