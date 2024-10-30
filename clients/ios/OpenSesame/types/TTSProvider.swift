import Foundation

struct TTSProvider: Identifiable, Equatable {
    
    let id: String
    let label: String
    let languages: [Language]
    
    static func == (lhs: TTSProvider, rhs: TTSProvider) -> Bool {
        lhs.id == rhs.id
    }
}
