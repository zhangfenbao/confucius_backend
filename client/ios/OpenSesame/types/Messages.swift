import Foundation

struct MessageModel : Identifiable, Codable, Equatable{
    var id: UUID {
        messageID
    }
    let messageID: UUID
    let conversationID: UUID
    let messageNumber: Int32
    let content: MessageContent
    let languageCode: String?
    let createdAt: Date
    let updatedAt: Date
    let extraMetadata: [String: String]?

    enum CodingKeys: String, CodingKey {
        case messageID = "message_id"
        case conversationID = "conversation_id"
        case messageNumber = "message_number"
        case content
        case languageCode = "language_code"
        case createdAt = "created_at"
        case updatedAt = "updated_at"
        case extraMetadata = "extra_metadata"
    }

    init(messageID: UUID, conversationID: UUID, messageNumber: Int32, content: MessageContent, languageCode: String? = nil, createdAt: Date, updatedAt: Date, extraMetadata: [String : String]? = nil) {
        self.messageID = messageID
        self.conversationID = conversationID
        self.messageNumber = messageNumber
        self.content = content
        self.languageCode = languageCode
        self.createdAt = createdAt
        self.updatedAt = updatedAt
        self.extraMetadata = extraMetadata
    }

    init(from decoder: any Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        self.messageID = try container.decode(UUID.self, forKey: .messageID)
        self.conversationID = try container.decode(UUID.self, forKey: .conversationID)
        self.messageNumber = try container.decode(Int32.self, forKey: .messageNumber)
        self.content = try container.decode(MessageContent.self, forKey: .content)
        self.languageCode = try container.decodeIfPresent(String.self, forKey: .languageCode)
        self.extraMetadata = try container.decodeIfPresent([String : String].self, forKey: .extraMetadata)

        let createdAtString = try container.decode(String.self, forKey: .createdAt)
        guard let createdAtDate = OpenSesameHelper.parseStringAsDate(dateAsString: createdAtString) else {
            throw DecodingError.dataCorruptedError(forKey: .createdAt, in: container, debugDescription: "Date string does not match format expected by formatter.")
        }
        self.createdAt = createdAtDate

        let updatedAtString = try container.decode(String.self, forKey: .updatedAt)
        guard let updatedAtDate = OpenSesameHelper.parseStringAsDate(dateAsString: updatedAtString) else {
            throw DecodingError.dataCorruptedError(forKey: .updatedAt, in: container, debugDescription: "Date string does not match format expected by formatter.")
        }
        self.updatedAt = updatedAtDate
    }

    func isFromBot() -> Bool {
        self.content.role == "assistant"
    }

    static func == (lhs: MessageModel, rhs: MessageModel) -> Bool {
        lhs.id == rhs.id
    }
}

struct MessageContent : Codable {
    let role: String
    let content: String
}
