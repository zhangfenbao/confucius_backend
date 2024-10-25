import WidgetKit
import SwiftUI

struct OpenSesameStatusLiveActivity: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: OpenSesameStatusAttributes.self) { context in
            // Lock screen/banner UI
            HStack {
                Spacer()
                Text(context.attributes.assistantName)
                    .font(.headline)
                    .fontWeight(.bold)
                    .foregroundColor(.black)
                Text(context.state.connectionStatus)
                    .font(.subheadline)
                    .foregroundColor(.gray)
                Spacer()
            }
            .padding(10) // Padding around the HStack
            .background(Color.white)
            .cornerRadius(15)
            .activityBackgroundTint(Color.white)
            .activitySystemActionForegroundColor(Color.black)
        } dynamicIsland: { context in
            DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(context.attributes.assistantName)
                            .font(.headline)
                            .foregroundColor(.black)

                        Text(context.state.connectionStatus)
                            .font(.subheadline)
                            .foregroundColor(.gray)
                    }
                    .padding()
                    .background(Color.white)
                    .cornerRadius(10)
                    .shadow(radius: 5)
                }
            } compactLeading: {
                Text(context.attributes.assistantName)
                    .font(.headline)
                    .padding(.leading, 8) // Padding in the compact leading
            } compactTrailing: {
                Text(context.state.connectionStatus)
                    .font(.subheadline)
                    .foregroundColor(.gray)
                    .padding(.trailing, 8) // Padding in the compact trailing
            } minimal: {
                Text("🤖")
                    .font(.title)
                    .padding(8) // Padding around the minimal icon
            }
        }
    }
}
