import SwiftUI

public extension Color {
    static let appBackground = Color(hex: "#000000")
    static let appForeground = Color(hex: "#FFFFFF")
    
    static let bottomPanelBorder = Color(hex: "#525252")
    
    static let prejoinBackground = Color(hex: "#F9FAFB")
    
    static let settingsBackground = Color(hex: "#262626")
    static let settingsSectionBackground = Color(hex: "#404040")
    static let settingsForeground = Color(hex: "#FFFFFF")
    static let settingsItemForeground = Color(hex: "A3A3A3")
    
    static let toastMessage = Color(hex: "e3f2fa")
    
    static let deleteBackground = Color(hex: "DC2626")
    static let deleteForeground = Color(hex: "FEE2E2")
    
    // TODO refactor all these colors and rename them
    static let micVolume = Color(hex: "#86EFAC")
    static let disabledMic = Color(hex: "#ee6b6e")

    init(hex: String) {
        let scanner = Scanner(string: hex)
        _ = scanner.scanString("#")

        var rgb: UInt64 = 0
        scanner.scanHexInt64(&rgb)

        let red = Double((rgb >> 16) & 0xFF) / 255.0
        let green = Double((rgb >> 8) & 0xFF) / 255.0
        let blue = Double(rgb & 0xFF) / 255.0

        self.init(red: red, green: green, blue: blue)
    }
}
