import SwiftUI

struct OnboardingApiKeys: View {
    
    // Note: In a production environment, it is recommended to avoid calling Daily's API endpoint directly.
    // Instead, you should route requests through your own server to handle authentication, validation,
    // and any other necessary logic. Therefore, the baseUrl should be set to the URL of your own server.
    @Binding var dailyApiKey: String
    @Binding var openApiKey: String
    @Binding var cartesiaApiKey: String
    @Binding var deepgramApiKey: String
    @Binding var togetherApiKey: String
    @Binding var anthropicApiKey: String
    
    var body: some View {
        VStack {
            Text("Credentials")
                .font(.title2)
                .fontWeight(.semibold)
                .frame(maxWidth: .infinity, alignment: .center)
            Form {
                Section() {
                    VStack {
                        HStack {
                            Text("Daily:").frame(minWidth: 100, alignment: .leading)
                            SecureFieldWithPlaceholder(placeholder: "Daily API Key", text: self.$dailyApiKey, foregroundColor: Color.settingsItemForeground)
                        }
                        Divider()
                            .background(Color.gray)
                        HStack {
                            Text("Cartesia:").frame(minWidth: 100, alignment: .leading)
                            SecureFieldWithPlaceholder(placeholder: "Cartesia API Key", text: self.$cartesiaApiKey, foregroundColor: Color.settingsItemForeground)
                        }
                        Divider()
                            .background(Color.gray)
                        HStack {
                            Text("Deepgram:").frame(minWidth: 100, alignment: .leading)
                            SecureFieldWithPlaceholder(placeholder: "Deepgram API Key", text: self.$deepgramApiKey, foregroundColor: Color.settingsItemForeground)
                        }
                        Divider()
                            .background(Color.gray)
                        HStack {
                            Text("Together:").frame(minWidth: 100, alignment: .leading)
                            SecureFieldWithPlaceholder(placeholder: "Together API Key", text: self.$togetherApiKey, foregroundColor: Color.settingsItemForeground)
                        }
                        Divider()
                            .background(Color.gray)
                        HStack {
                            Text("Anthropic:").frame(minWidth: 100, alignment: .leading)
                            SecureFieldWithPlaceholder(placeholder: "Anthropic API Key", text: self.$anthropicApiKey, foregroundColor: Color.settingsItemForeground)
                        }
                        Divider()
                            .background(Color.gray)
                        HStack {
                            Text("OpenAI:").frame(minWidth: 100, alignment: .leading)
                            SecureFieldWithPlaceholder(placeholder: "Open API Key", text: self.$openApiKey, foregroundColor: Color.settingsItemForeground)
                        }
                    }
                }
                .listRowBackground(Color.settingsSectionBackground)
            }
            .scrollContentBackground(.hidden)
        }
    }
}

#Preview {
    let result = OnboardingApiKeys(
        dailyApiKey: .constant(""),
        openApiKey: .constant(""),
        cartesiaApiKey: .constant(""),
        deepgramApiKey: .constant(""), 
        togetherApiKey: .constant(""),
        anthropicApiKey: .constant("")
    )
    .background(Color.settingsBackground)
    .foregroundColor(Color.settingsForeground)
    return result
}
