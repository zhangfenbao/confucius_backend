import SwiftUI

struct WaveformView: View {
    var autoIncreaseAudioLevel: Bool = true
    var audioLevelMinThreshold: Float = 0.0
    var audioLevel: Float
    
    @State
    private var audioLevels: [Float] = Array(repeating: 0, count: 5)
    private let dotCount = 5
    
    var body: some View {
        GeometryReader { geometry in
            HStack(spacing:2) {
                if self.audioLevel > self.audioLevelMinThreshold {
                    ForEach(0..<self.dotCount, id: \.self) { index in
                        Rectangle()
                            .fill(Color.white)
                            .frame(height: CGFloat(self.audioLevels[index]) * (geometry.size.height))
                            .cornerRadius(12)
                            .animation(.easeInOut(duration: 0.2), value: self.audioLevels[index])
                    }
                }else {
                    ForEach(0..<self.dotCount, id: \.self) { _ in
                        Circle()
                            .fill(Color.white)
                    }
                }
            }
            .frame(width: geometry.size.width * 0.7)
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .center)
        }
        .onChange(of: self.audioLevel) { oldLevel, newLevel in
            // The audio level that we receive from the bot is usually too low
            // so just increasing it so we can see a better graph but
            // making sure that it is not higher than the maximum 1
            var audioLevel = self.autoIncreaseAudioLevel ? self.audioLevel + 0.4 : self.audioLevel
            if audioLevel > 1 {
                audioLevel = 1
            }
            // Update the array and shift values
            self.audioLevels.removeFirst()
            self.audioLevels.append(audioLevel)
        }
    }
}

#Preview {
    WaveformView(audioLevel: 0.001)
        .background(Color.appBackground)
        .foregroundColor(Color.appForeground)
        .frame(width: 100, height: 100)
}
