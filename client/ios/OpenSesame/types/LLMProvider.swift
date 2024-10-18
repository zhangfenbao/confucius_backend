struct LLMProvider: Identifiable {
    let id: String
    let label: String
    let models: [LLMModel]
}
