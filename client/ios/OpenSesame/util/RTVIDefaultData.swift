import Foundation

struct RTVIDefaultData {
    
    static let defaultLLMModels: [LLMProvider] = [
        LLMProvider(
            id: "together",
            label: "together.ai",
            models: [
                LLMModel(
                    id: "meta-llama/Meta-Llama-3.1-70B-Instruct-Turbo",
                    label: "Llama 3.1 70B"
                ),
                LLMModel(
                    id: "meta-llama/Meta-Llama-3.1-8B-Instruct-Turbo",
                    label: "Llama 3.1 8B"
                ),
                LLMModel(
                    id: "meta-llama/Meta-Llama-3.1-405B-Instruct-Turbo",
                    label: "Llama 3.1 405B"
                )
            ]
        ),
        LLMProvider(
            id: "anthropic",
            label: "ANTHROP\\C",
            models: [
                LLMModel(
                    id: "claude-3-5-sonnet-20240620",
                    label: "Claude 3.5 Sonnet"
                )
            ]
        ),
        LLMProvider(
            id: "openai",
            label: "Open AI",
            models: [
                LLMModel(
                    id: "gpt-4o",
                    label: "GPT-4o"
                ),
                LLMModel(
                    id: "gpt-4o-mini",
                    label: "GPT-4o Mini"
                )
            ]
        )
    ]
    
    static func getLLMProvider(by id: String?) -> LLMProvider? {
        return defaultLLMModels.first { $0.id == id }
    }
    
    static func getLLMModel(by id: String?, providerId: String?) -> LLMModel? {
        return defaultLLMModels.first { $0.id == providerId }?.models.first { $0.id == id}
    }
    
    static let supportedTtsProviders: [TTSProvider] = [
        TTSProvider(
            id: "cartesia",
            label: "Cartesia",
            languages: [
                Language(
                    id: "en",
                    providerId: "cartesia",
                    name: "English",
                    voices: [
                        Voice(id: "79a125e8-cd45-4c13-8a67-188112f4dd22", name: "British Lady", ttsModel: "sonic-english", sttModel: "nova-2-conversationalai"),
                        Voice(id: "b7d50908-b17c-442d-ad8d-810c63997ed9", name: "California Girl", ttsModel: "sonic-english", sttModel: "nova-2-conversationalai"),
                        Voice(id: "2ee87190-8f84-4925-97da-e52547f9462c", name: "Child", ttsModel: "sonic-english", sttModel: "nova-2-conversationalai"),
                        Voice(id: "95856005-0332-41b0-935f-352e296aa0df", name: "Classy British Man", ttsModel: "sonic-english", sttModel: "nova-2-conversationalai"),
                        Voice(id: "63ff761f-c1e8-414b-b969-d1833d1c870c", name: "Confident British Man", ttsModel: "sonic-english", sttModel: "nova-2-conversationalai"),
                        Voice(id: "fb26447f-308b-471e-8b00-8e9f04284eb5", name: "Doctor Mischief", ttsModel: "sonic-english", sttModel: "nova-2-conversationalai"),
                        Voice(id: "5c42302c-194b-4d0c-ba1a-8cb485c84ab9", name: "Female Nurse", ttsModel: "sonic-english", sttModel: "nova-2-conversationalai"),
                        Voice(id: "69267136-1bdc-412f-ad78-0caad210fb40", name: "Friendly Reading Man", ttsModel: "sonic-english", sttModel: "nova-2-conversationalai"),
                        Voice(id: "156fb8d2-335b-4950-9cb3-a2d33befec77", name: "Helpful Woman", ttsModel: "sonic-english", sttModel: "nova-2-conversationalai"),
                        Voice(id: "726d5ae5-055f-4c3d-8355-d9677de68937", name: "Kentucky Man", ttsModel: "sonic-english", sttModel: "nova-2-conversationalai"),
                        Voice(id: "e13cae5c-ec59-4f71-b0a6-266df3c9bb8e", name: "Madame Mischief", ttsModel: "sonic-english", sttModel: "nova-2-conversationalai"),
                        Voice(id: "c45bc5ec-dc68-4feb-8829-6e6b2748095d", name: "Movieman", ttsModel: "sonic-english", sttModel: "nova-2-conversationalai"),
                        Voice(id: "d46abd1d-2d02-43e8-819f-51fb652c1c61", name: "Newsman", ttsModel: "sonic-english", sttModel: "nova-2-conversationalai"),
                        Voice(id: "ee7ea9f8-c0c1-498c-9279-764d6b56d189", name: "Polite Man", ttsModel: "sonic-english", sttModel: "nova-2-conversationalai"),
                        Voice(id: "820a3788-2b37-4d21-847a-b65d8a68c99a", name: "Salesman", ttsModel: "sonic-english", sttModel: "nova-2-conversationalai"),
                        Voice(id: "f9836c6e-a0bd-460e-9d3c-f7299fa60f94", name: "Southern Woman", ttsModel: "sonic-english", sttModel: "nova-2-conversationalai"),
                        Voice(id: "996a8b96-4804-46f0-8e05-3fd4ef1a87cd", name: "Storyteller Lady", ttsModel: "sonic-english", sttModel: "nova-2-conversationalai"),
                        Voice(id: "50d6beb4-80ea-4802-8387-6c948fe84208", name: "The Merchant", ttsModel: "sonic-english", sttModel: "nova-2-conversationalai"),
                    ]
                ),
                Language(
                    id: "de",
                    providerId: "cartesia",
                    name: "German",
                    voices: [
                        Voice(id: "fb9fcab6-aba5-49ec-8d7e-3f1100296dde", name: "Friendly German Man", ttsModel: "sonic-multilingual", sttModel: "nova-2-general"),
                        Voice(id: "384b625b-da5d-49e8-a76d-a2855d4f31eb", name: "German Conversation Man", ttsModel: "sonic-multilingual", sttModel: "nova-2-general"),
                        Voice(id: "3f4ade23-6eb4-4279-ab05-6a144947c4d5", name: "German Conversational Woman", ttsModel: "sonic-multilingual", sttModel: "nova-2-general"),
                        Voice(id: "3f6e78a8-5283-42aa-b5e7-af82e8bb310c", name: "German Reporter Man", ttsModel: "sonic-multilingual", sttModel: "nova-2-general"),
                        Voice(id: "119e03e4-0705-43c9-b3ac-a658ce2b6639", name: "German Reporter Woman", ttsModel: "sonic-multilingual", sttModel: "nova-2-general"),
                        Voice(id: "db229dfe-f5de-4be4-91fd-7b077c158578", name: "German Storyteller Man", ttsModel: "sonic-multilingual", sttModel: "nova-2-general"),
                        Voice(id: "b9de4a89-2257-424b-94c2-db18ba68c81a", name: "German Woman", ttsModel: "sonic-multilingual", sttModel: "nova-2-general"),
                    ]
                ),
                Language(
                    id: "fr",
                    providerId: "cartesia",
                    name: "French",
                    voices: [
                        Voice(id: "a8a1eb38-5f15-4c1d-8722-7ac0f329727d", name: "Calm French Woman", ttsModel: "sonic-multilingual", sttModel: "nova-2-general"),
                        Voice(id: "a249eaff-1e96-4d2c-b23b-12efa4f66f41", name: "French Conversational Lady", ttsModel: "sonic-multilingual", sttModel: "nova-2-general"),
                        Voice(id: "8832a0b5-47b2-4751-bb22-6a8e2149303d", name: "French Narrator Lady", ttsModel: "sonic-multilingual", sttModel: "nova-2-general"),
                        Voice(id: "5c3c89e5-535f-43ef-b14d-f8ffe148c1f0", name: "French Narrator Man", ttsModel: "sonic-multilingual", sttModel: "nova-2-general"),
                        Voice(id: "ab7c61f5-3daa-47dd-a23b-4ac0aac5f5c3", name: "Friendly French Man", ttsModel: "sonic-multilingual", sttModel: "nova-2-general"),
                        Voice(id: "65b25c5d-ff07-4687-a04c-da2f43ef6fa9", name: "Helpful French Lady", ttsModel: "sonic-multilingual", sttModel: "nova-2-general"),
                        Voice(id: "0418348a-0ca2-4e90-9986-800fb8b3bbc0", name: "Stern French Man", ttsModel: "sonic-multilingual", sttModel: "nova-2-general"),
                    ]
                ),
                Language(
                    id: "es",
                    providerId: "cartesia",
                    name: "Spanish",
                    voices: [
                        Voice(id: "15d0c2e2-8d29-44c3-be23-d585d5f154a1", name: "Mexican Man", ttsModel: "sonic-multilingual", sttModel: "nova-2-general"),
                        Voice(id: "5c5ad5e7-1020-476b-8b91-fdcbe9cc313c", name: "Mexican Woman", ttsModel: "sonic-multilingual", sttModel: "nova-2-general"),
                        Voice(id: "2deb3edf-b9d8-4d06-8db9-5742fb8a3cb2", name: "Spanish Narrator Lady", ttsModel: "sonic-multilingual", sttModel: "nova-2-general"),
                        Voice(id: "a67e0421-22e0-4d5b-b586-bd4a64aee41d", name: "Spanish Narrator Man", ttsModel: "sonic-multilingual", sttModel: "nova-2-general"),
                        Voice(id: "846d6cb0-2301-48b6-9683-48f5618ea2f6", name: "Spanish Speaking Lady", ttsModel: "sonic-multilingual", sttModel: "nova-2-general"),
                        Voice(id: "34dbb662-8e98-413c-a1ef-1a3407675fe7", name: "Spanish Speaking Man", ttsModel: "sonic-multilingual", sttModel: "nova-2-general"),
                        Voice(id: "2695b6b5-5543-4be1-96d9-3967fb5e7fec", name: "Spanish Speaking Reporter Man", ttsModel: "sonic-multilingual", sttModel: "nova-2-general"),
                        Voice(id: "db832ebd-3cb6-42e7-9d47-912b425adbaa", name: "Young Spanish Speaking Woman", ttsModel: "sonic-multilingual", sttModel: "nova-2-general"),
                        Voice(id: "846fa30b-6e1a-49b9-b7df-6be47092a09a", name: "Spanish Speaking Storyteller Man", ttsModel: "sonic-multilingual", sttModel: "nova-2-general"),
                    ]
                )
            ]
        ),
        TTSProvider(
            id: "elevenlabs",
            label: "ElevenLabs",
            languages: [
                Language(
                    id: "en",
                    providerId: "elevenlabs",
                    name: "English",
                    voices: [
                        Voice(id: "Xb7hH8MSUJpSbSDYk0k2", name: "Alice", ttsModel: "eleven_turbo_v2_5", sttModel: "nova-2-conversationalai"),
                        Voice(id: "pqHfZKP75CvOlQylNhV4", name: "Bill", ttsModel: "eleven_turbo_v2_5", sttModel: "nova-2-conversationalai"),
                        Voice(id: "nPczCjzI2devNBz1zQrb", name: "Brian", ttsModel: "eleven_turbo_v2_5", sttModel: "nova-2-conversationalai"),
                        Voice(id: "N2lVS1w4EtoT3dr4eOWO", name: "Callum", ttsModel: "eleven_turbo_v2_5", sttModel: "nova-2-conversationalai"),
                        Voice(id: "IKne3meq5aSn9XLyUdCD", name: "Charlie", ttsModel: "eleven_turbo_v2_5", sttModel: "nova-2-conversationalai")
                    ]
                ),
            ]
        )
    ]
    
    static func getLanguage(ttsProviderId: String, languageId: String) -> Language? {
        return supportedTtsProviders.first{ $0.id == ttsProviderId }?.languages.first { $0.id == languageId }
    }
    
    static let supportedStorages = [
        //Storage(id:"local", name: "On Device"),
        Storage(id:"supabase", name: "Supabase")
    ]
    
    static func getStorage(by id: String) -> Storage? {
        return supportedStorages.first { $0.id == id }
    }
    
    // default data
    static let defaultTTSProvider = supportedTtsProviders[0]
    static let defaultLanguage = defaultTTSProvider.languages[0]
    static let defaultTTSVoice = defaultLanguage.voices[0]
    static let defaultPrompt = "You are Sesame, a friendly assistant. Keep your responses brief, when possible or not requested differently. Avoid bold and italic text formatting (**bold** and *italic*) in your responses."
    
}
