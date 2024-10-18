struct Language: Identifiable, Equatable {
    
    let id: String
    let providerId: String
    let name: String
    let voices: [Voice]
    
    static func == (lhs: Language, rhs: Language) -> Bool {
        lhs.id == rhs.id && lhs.providerId == rhs.providerId
    }
}
